<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <div>
        <h3 class="text-lg font-semibold text-gray-800 dark:text-gray-100">Ortak Çalışma Kaynakları</h3>
        <p class="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
          Tek örnek çalışıyor (Redis yok). Sayılar bu örneğe aittir.
        </p>
      </div>
      <label class="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-300">
        <input type="checkbox" v-model="autoRefresh" class="rounded">
        5 sn'de bir yenile
      </label>
    </div>

    <div v-if="error" class="mb-4 px-3 py-2 rounded-lg bg-red-50 text-red-700 text-sm">
      {{ error }}
    </div>

    <div v-if="!metrics && loading" class="text-sm text-gray-500">Yükleniyor…</div>

    <template v-else-if="metrics">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-5">
        <StatCard label="Açık bağlantı" :value="metrics.openConnections"
                  hint="Eşzamanlı düzenleyici sayısı" />
        <StatCard label="Açık doküman" :value="metrics.openDocuments" />
        <StatCard label="Güncelleme/sn" :value="updatesPerSecond"
                  hint="İki ölçüm arasındaki fark" />
        <StatCard label="Bekleyen tampon" :value="formatBytes(metrics.bufferedBytes)"
                  hint="Henüz diske yazılmamış" />
        <StatCard label="collab_updates satırı" :value="metrics.totalUpdateRows.toLocaleString('tr-TR')"
                  hint="Sıkıştırma bunu düşürür" />
        <StatCard label="Makro kuyruğu" :value="`${metrics.macroRunning} / ${metrics.macroQueueDepth}`"
                  hint="Koşan / bekleyen" />
        <StatCard label="Heap" :value="formatBytes(metrics.heapUsedBytes)"
                  :hint="`Üst sınır ${formatBytes(metrics.heapMaxBytes)}`"
                  :warn="heapRatio > 0.85" />
        <StatCard label="Heap doluluk" :value="`%${Math.round(heapRatio * 100)}`"
                  :warn="heapRatio > 0.85" />
      </div>

      <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-200 mb-2">
        En büyük dokümanlar
      </h4>
      <p class="text-xs text-gray-500 dark:text-gray-400 mb-2">
        Sıkıştırılmamış delta yüküne göre. Disk şişmesi genelde tek bir uzun ömürlü
        dokümandan gelir ve toplamda görünmez.
      </p>
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="text-xs text-gray-500 dark:text-gray-400 border-b dark:border-gray-700">
            <tr>
              <th class="text-left py-2 font-medium">Doküman</th>
              <th class="text-right py-2 font-medium">Güncelleme</th>
              <th class="text-right py-2 font-medium">Yük</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="doc in metrics.topDocuments" :key="doc.documentId"
                class="border-b dark:border-gray-700 last:border-0">
              <td class="py-2 text-gray-700 dark:text-gray-200 truncate max-w-xs">{{ doc.title }}</td>
              <td class="py-2 text-right text-gray-600 dark:text-gray-300">
                {{ doc.updateCount.toLocaleString('tr-TR') }}
              </td>
              <td class="py-2 text-right text-gray-600 dark:text-gray-300">
                {{ formatBytes(doc.payloadBytes) }}
              </td>
            </tr>
            <tr v-if="!metrics.topDocuments.length">
              <td colspan="3" class="py-4 text-center text-gray-400 text-xs">
                Henüz sıkıştırılmamış güncelleme yok.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, h } from 'vue'
import axios from '../../api/axios'

/**
 * Collab kaynak izleme paneli (COLLAB_WORKSPACE_PLAN.md §12).
 *
 * Yenileme varsayılan olarak **kapalı**: açık bırakılan bir yönetici sekmesi,
 * ölçmeye çalıştığı sunucuya sürekli sorgu atan bir yüke dönüşürdü (D3).
 */
const metrics = ref(null)
const loading = ref(false)
const error = ref('')
const autoRefresh = ref(false)

// Oran sunucuda değil burada hesaplanıyor: sunucunun kayan pencere tutması
// gerekirdi, oysa iki ölçüm arasındaki fark hem ucuz hem yeterli.
const previous = ref(null)
const updatesPerSecond = computed(() => {
  if (!previous.value || !metrics.value) return '—'
  const deltaCount = metrics.value.acceptedUpdates - previous.value.acceptedUpdates
  const deltaMs = metrics.value.at - previous.value.at
  if (deltaMs <= 0 || deltaCount < 0) return '—'
  return (deltaCount / (deltaMs / 1000)).toFixed(1)
})

const heapRatio = computed(() => {
  if (!metrics.value?.heapMaxBytes) return 0
  return metrics.value.heapUsedBytes / metrics.value.heapMaxBytes
})

let timer = null

async function load() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/admin/collab/metrics')
    previous.value = metrics.value
    metrics.value = { ...data, at: Date.now() }
    error.value = ''
  } catch (e) {
    error.value = e?.response?.data?.error || 'Göstergeler alınamadı.'
  } finally {
    loading.value = false
  }
}

function formatBytes(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit++
  }
  return `${value.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`
}

watch(autoRefresh, (on) => {
  clearInterval(timer)
  if (on) timer = setInterval(load, 5000)
})

onMounted(load)
onBeforeUnmount(() => clearInterval(timer))

/** Küçük gösterge kartı — ayrı dosya açmaya değmeyecek kadar basit. */
const StatCard = (props) => h('div', {
  class: ['rounded-xl border p-3',
    props.warn
      ? 'border-amber-300 bg-amber-50 dark:bg-amber-900/20'
      : 'border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900/40']
}, [
  h('div', { class: 'text-[11px] text-gray-500 dark:text-gray-400' }, props.label),
  h('div', { class: 'text-lg font-semibold text-gray-800 dark:text-gray-100 mt-0.5' },
    String(props.value)),
  props.hint
    ? h('div', { class: 'text-[10px] text-gray-400 mt-0.5' }, props.hint)
    : null
])
StatCard.props = ['label', 'value', 'hint', 'warn']
</script>
