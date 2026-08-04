<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <OrphanRichFilter v-if="error === 'missing'" />

    <template v-else>
      <div class="flex items-start justify-between gap-2">
        <div class="min-w-0">
          <h3 class="text-sm font-semibold text-gray-700 truncate">
            {{ title || definition?.name || 'Kuyruk' }}
          </h3>
          <p class="text-[11px] text-gray-400">
            {{ loading ? 'Hesaplanıyor…' : `${total} görev` }}
          </p>
        </div>
        <button
          v-if="selectedSmart.length"
          class="shrink-0 text-[11px] text-purple-600 hover:text-purple-700"
          @click="setSmart([])"
        >
          Seçimi kaldır
        </button>
      </div>

      <p v-if="!loading && !rows.length" class="py-6 text-center text-xs text-gray-400">
        Gösterilecek kuyruk yok.
      </p>

      <ul v-else class="divide-y divide-gray-50">
        <li v-for="row in rows" :key="row.key || 'unclassified'" class="py-1.5">
          <div
            class="flex items-center gap-2 rounded-lg px-1.5 py-1 transition-colors"
            :class="isSelected(row) ? 'bg-purple-50/70' : 'hover:bg-gray-50/70'"
          >
            <!-- Satırın kendisi: seçimi paylaşılan duruma yazar -->
            <button
              class="flex items-center gap-2 min-w-0 flex-1 text-left disabled:cursor-default"
              :disabled="!row.key"
              @click="toggleSmart(row.key)"
            >
              <span class="w-1.5 h-6 rounded-full shrink-0" :style="{ backgroundColor: colorOf(row) }"></span>
              <span class="min-w-0 flex-1">
                <span class="block text-xs text-gray-700 truncate">{{ row.label }}</span>
                <!-- Payı gösteren ince şerit: sayıları okumadan önce dağılım görünür -->
                <span class="mt-1 block h-1 rounded-full bg-gray-100 overflow-hidden">
                  <span
                    class="block h-full rounded-full transition-all"
                    :style="{ width: share(row) + '%', backgroundColor: colorOf(row) }"
                  ></span>
                </span>
              </span>
              <span class="text-sm font-semibold tabular-nums shrink-0"
                    :class="isSelected(row) ? 'text-purple-700' : 'text-gray-700'">
                {{ row.value }}
              </span>
            </button>

            <button
              v-if="row.key"
              class="shrink-0 w-5 h-5 text-gray-300 hover:text-purple-600 transition-transform"
              :class="expanded === row.key ? 'rotate-90 text-purple-600' : ''"
              :title="expanded === row.key ? 'Kapat' : 'Görevleri göster'"
              @click="toggleExpand(row)"
            >›</button>

            <button
              class="shrink-0 text-[10px] text-gray-300 hover:text-purple-600"
              title="Görev listesinde aç"
              @click="openRow(row)"
            >↗</button>
          </div>

          <!-- Açılır liste: kuyruğun ilk görevleri, triage için -->
          <div v-if="expanded === row.key" class="pl-5 pr-1 pb-1">
            <p v-if="loadingTasks" class="py-2 text-[11px] text-gray-400">Yükleniyor…</p>
            <p v-else-if="!tasks.length" class="py-2 text-[11px] text-gray-400">
              Bu seçimde görev yok.
            </p>
            <ul v-else class="space-y-0.5">
              <li v-for="task in tasks" :key="task.id">
                <button
                  class="w-full flex items-center gap-2 px-1 py-1 rounded text-left hover:bg-gray-50"
                  @click="$emit('task-click', task)"
                >
                  <span class="text-[10px] font-mono text-gray-400 shrink-0 w-14 truncate">
                    {{ task.customId }}
                  </span>
                  <span class="text-[11px] text-gray-600 truncate">{{ task.title }}</span>
                </button>
              </li>
            </ul>
            <button
              v-if="row.value > tasks.length"
              class="mt-1 text-[10px] text-purple-600 hover:text-purple-700"
              @click="openRow(row)"
            >
              Tümünü gör ({{ row.value }})
            </button>
          </div>
        </li>
      </ul>
    </template>
  </div>
</template>

<script setup>
/**
 * Kuyruk paneli — akıllı filtre başına sayaç ve açılır görev listesi.
 *
 * Grafiğin cevapladığı soru "dağılım nasıl?", kuyruğun cevapladığı soru
 * "sırada ne var?". Bu yüzden kovalar burada daire dilimi değil, sıralı bir
 * çalışma listesidir: satıra tıklamak panoyu o kategoriye daraltır, ok ise
 * kategorinin ilk görevlerini yerinde açar — triage için dashboard'dan
 * çıkmak gerekmez.
 *
 * Satır sırası akıllı filtrelerin kendi sırasıdır (en büyükten değil): "ilk
 * eşleşen kazanır" mantığında sıra zaten yazarın önceliklendirmesidir.
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import OrphanRichFilter from './OrphanRichFilter.vue'
import { useRichFilterContext } from '../../composables/useRichFilterContext.js'
import { useAutoRefresh } from '../../composables/useAutoRefresh.js'
import { aggregateRichFilter, searchRichFilter, resolveRichFilter } from '../../api/RichFilterApi.js'
import { colorFor } from '../../utils/chartPalette.js'

const props = defineProps({
  teamId: { type: String, required: true },
  richFilterId: { type: String, required: true },
  title: { type: String, default: '' },
  /** Gösterilecek akıllı filtreler; boşsa hepsi. */
  smartIds: { type: Array, default: () => [] },
  /** Sıfır sayılı kuyruklar gizlensin mi? */
  hideEmpty: { type: Boolean, default: false },
  /** Bir kuyruk açıldığında listelenecek görev sayısı. */
  taskLimit: { type: Number, default: 5 },
  refreshInterval: { type: Number, default: 0 },
})

defineEmits(['task-click'])

const router = useRouter()
const { definition, error, selectedSmart, payload, signature, loadDefinition, toggleSmart, setSmart } =
  useRichFilterContext(() => props.richFilterId)

const buckets = ref([])
const loading = ref(false)

/** Açık olan kuyruğun anahtarı — aynı anda bir tanesi açılır. */
const expanded = ref(null)
const tasks = ref([])
const loadingTasks = ref(false)

/** Toplam, seçilen alt küme değil <b>bütün</b> kovalar üzerinden: paylar kaymasın. */
const total = computed(() => buckets.value.reduce((sum, b) => sum + (Number(b.value) || 0), 0))

const rows = computed(() => {
  let list = buckets.value

  // Alt küme seçilmişse "Sınıflandırılmamış" da dışarıda kalır: kullanıcı
  // hangi kuyrukları istediğini açıkça söylemiştir.
  if (props.smartIds?.length) {
    const wanted = new Set(props.smartIds)
    list = list.filter(b => wanted.has(b.key))
  }
  if (props.hideEmpty) list = list.filter(b => Number(b.value) > 0)

  return list
})

const isSelected = (row) => !!row.key && selectedSmart.value.includes(row.key)

const colorOf = (row) => colorFor(row, rows.value.indexOf(row))

function share(row) {
  if (!total.value) return 0
  return Math.round((Number(row.value) || 0) / total.value * 100)
}

async function toggleExpand(row) {
  if (expanded.value === row.key) {
    expanded.value = null
    return
  }
  expanded.value = row.key
  await loadTasks(row)
}

/**
 * Açılan kuyruğun görevleri: mevcut daraltmalar korunur, yalnız akıllı filtre
 * seçimi bu satıra sabitlenir — panodaki başka bir seçim varken açılan kuyruk
 * o seçimin içinden okunur.
 */
async function loadTasks(row) {
  loadingTasks.value = true
  try {
    const result = await searchRichFilter(
      props.teamId, props.richFilterId,
      { ...payload.value, smart: [row.key] },
      { page: 0, size: props.taskLimit },
    )
    tasks.value = result.content || []
  } catch {
    tasks.value = []
  } finally {
    loadingTasks.value = false
  }
}

/** Satırı görev listesi ekranında açar (bkz. RfChartWidget — aynı köprü). */
async function openRow(row) {
  let stql = row.filter || ''
  try {
    const resolved = await resolveRichFilter(props.teamId, props.richFilterId, payload.value)
    if (resolved.stql && row.filter) stql = `(${resolved.stql}) AND (${row.filter})`
    else if (resolved.stql) stql = resolved.stql
  } catch {
    // Çözümleme başarısızsa yalnız satır koşuluyla açmak, hiç açmamaktan iyidir.
  }
  router.push({ path: `/workList/${props.teamId}`, query: stql ? { q: stql } : {} })
}

async function load() {
  if (!props.teamId || !props.richFilterId) return
  loading.value = true
  try {
    buckets.value = await aggregateRichFilter(props.teamId, props.richFilterId, payload.value)
  } catch {
    buckets.value = []
  } finally {
    loading.value = false
  }

  // Açık kuyruğun listesi de seçimle birlikte tazelenir; aksi hâlde başlıktaki
  // sayı değişirken altındaki liste eski kalırdı.
  const open = rows.value.find(r => r.key === expanded.value)
  if (open) await loadTasks(open)
  else expanded.value = null
}

onMounted(async () => {
  await loadDefinition(props.teamId)
  load()
})

watch(signature, load)
useAutoRefresh(load, () => props.refreshInterval)
</script>
