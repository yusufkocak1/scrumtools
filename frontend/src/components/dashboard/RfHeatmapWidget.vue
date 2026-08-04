<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <OrphanRichFilter v-if="error === 'missing'" />

    <template v-else>
      <div class="flex items-start justify-between gap-2 group/head">
        <div class="min-w-0">
          <h3 class="text-sm font-semibold text-gray-700 truncate">
            {{ title || definition?.name || 'Isı haritası' }}
          </h3>
          <p class="text-[11px] text-gray-400 truncate">{{ rowLabel }} × {{ splitBy }}</p>
        </div>

        <button
          class="shrink-0 text-[11px] text-gray-400 hover:text-purple-600 opacity-0 group-hover/head:opacity-100 transition-opacity"
          title="CSV indir"
          @click="exportCsv"
        >CSV</button>
      </div>

      <div v-if="loading" class="h-48 flex items-center justify-center text-xs text-gray-400">
        Hesaplanıyor…
      </div>

      <div v-else-if="!rows.length || !columns.length" class="h-48 flex items-center justify-center text-xs text-gray-400">
        Bu seçimle eşleşen görev yok.
      </div>

      <!-- Matris: geniş olabilir, kendi içinde kayar; sayfa yatay kaymaz -->
      <div v-else class="overflow-x-auto -mx-1 px-1">
        <table class="border-separate border-spacing-0.5 text-[11px]">
          <thead>
            <tr>
              <th class="sticky left-0 z-10 bg-white"></th>
              <th
                v-for="column in columns"
                :key="column.key || 'empty-col'"
                class="px-1.5 py-1 font-medium text-gray-500 align-bottom whitespace-nowrap"
              >
                {{ column.label }}
              </th>
              <th class="px-1.5 py-1 font-semibold text-gray-400 whitespace-nowrap">Σ</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.key || 'empty-row'">
              <th class="sticky left-0 z-10 bg-white pr-2 py-1 text-left font-medium text-gray-600 whitespace-nowrap max-w-[9rem] truncate">
                <span class="inline-flex items-center gap-1.5">
                  <span v-if="row.color" class="w-2 h-2 rounded-sm shrink-0" :style="{ backgroundColor: row.color }"></span>
                  <span class="truncate">{{ row.label }}</span>
                </span>
              </th>

              <td
                v-for="column in columns"
                :key="column.key || 'empty-col'"
                class="p-0"
              >
                <button
                  class="w-full min-w-[2.5rem] h-7 rounded transition-transform hover:scale-105 hover:ring-1 hover:ring-purple-300 tabular-nums"
                  :style="cellStyle(row, column)"
                  :title="`${row.label} · ${column.label}: ${valueAt(row, column)}`"
                  @click="onCellClick(row)"
                >
                  {{ valueAt(row, column) || '' }}
                </button>
              </td>

              <td class="px-1.5 py-1 text-gray-400 tabular-nums text-right">{{ rowTotal(row) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="truncated" class="text-[11px] text-amber-600">
        Eksenler kırpıldı; en kalabalık kategoriler gösteriliyor.
      </p>
    </template>
  </div>
</template>

<script setup>
/**
 * İki boyutlu dağılım — "atanan × öncelik", "akıllı filtre × sprint".
 *
 * Grafiğin cevaplayamadığı soruyu cevaplar: tek eksende "kim kaç iş taşıyor" ve
 * "kaç iş kritik" ayrı ayrı görünür ama <b>kimin üzerinde kaç kritik iş olduğu</b>
 * ancak kesişimde belli olur. Yığılmış çubuk da bunu gösterebilirdi; matris,
 * kategoriler çoğaldığında okunur kalan tek biçim.
 *
 * Renk yoğunluğu satırın kendi içinde değil, <b>matrisin tamamına</b> göre
 * hesaplanır: satır bazlı normalizasyon, iki görevlik bir satırı yüz görevlik bir
 * satırla aynı koyulukta gösterip karşılaştırmayı bozardı.
 */
import { ref, computed, watch, onMounted } from 'vue'
import OrphanRichFilter from './OrphanRichFilter.vue'
import { useRichFilterContext } from '../../composables/useRichFilterContext.js'
import { useAutoRefresh } from '../../composables/useAutoRefresh.js'
import { matrixRichFilter, smartField } from '../../api/RichFilterApi.js'
import { downloadCsv } from '../../utils/widgetExport.js'

const props = defineProps({
  teamId: { type: String, required: true },
  richFilterId: { type: String, required: true },
  title: { type: String, default: '' },
  /** Satır ekseni; boşsa zengin filtrenin akıllı filtreleri. */
  groupBy: { type: String, default: '' },
  /** Sütun ekseni — zorunlu. */
  splitBy: { type: String, default: 'priority' },
  metric: { type: String, default: 'count' },
  refreshInterval: { type: Number, default: 0 },
})

const { definition, error, payload, signature, loadDefinition, toggleSmart } =
  useRichFilterContext(() => props.richFilterId)

const rows = ref([])
const columns = ref([])
const cells = ref([])
const max = ref(0)
const truncated = ref(false)
const loading = ref(false)

const isSmartRows = computed(() =>
  !props.groupBy || props.groupBy === smartField(definition.value?.name || '')
)

const rowLabel = computed(() => (isSmartRows.value ? 'Akıllı filtreler' : props.groupBy))

/**
 * Hücre değerlerinin hızlı erişimi: satır → (sütun → değer).
 * İki anahtarı tek metinde birleştirmek, "To Do" gibi boşluklu değerlerde çakışırdı.
 */
const byCell = computed(() => {
  const map = new Map()
  cells.value.forEach(c => {
    if (!map.has(c.row)) map.set(c.row, new Map())
    map.get(c.row).set(c.column, Number(c.value) || 0)
  })
  return map
})

const valueAt = (row, column) => byCell.value.get(row.key)?.get(column.key) || 0

const rowTotal = (row) =>
  columns.value.reduce((sum, column) => sum + valueAt(row, column), 0)

/**
 * Hücre rengi: mor tonun yoğunluğu değere orantılı.
 * Sıfır hücreler boş bırakılır — sıfırı soluk bir renkle boyamak, matriste
 * "az" ile "hiç"i birbirine karıştırır.
 */
function cellStyle(row, column) {
  const value = valueAt(row, column)
  if (!value) return { backgroundColor: '#F8FAFC', color: '#CBD5E1' }

  const intensity = max.value ? value / max.value : 0
  // Alt sınır 0.12: en küçük dolu hücre de beyazdan ayırt edilebilsin.
  const alpha = 0.12 + intensity * 0.78
  return {
    backgroundColor: `rgba(99, 102, 241, ${alpha.toFixed(3)})`,
    color: intensity > 0.55 ? '#FFFFFF' : '#334155',
  }
}

/** Satır ekseni sınıflandırmaysa hücreye tıklamak panoyu o kategoriye daraltır. */
function onCellClick(row) {
  if (isSmartRows.value && row.key) toggleSmart(row.key)
}

function exportCsv() {
  const headers = [rowLabel.value, ...columns.value.map(c => c.label), 'Toplam']
  const data = rows.value.map(row => [
    row.label,
    ...columns.value.map(column => valueAt(row, column)),
    rowTotal(row),
  ])
  downloadCsv(props.title || definition.value?.name || 'isi-haritasi', headers, data)
}

async function load() {
  if (!props.teamId || !props.richFilterId || !props.splitBy) return
  loading.value = true
  try {
    const result = await matrixRichFilter(props.teamId, props.richFilterId, payload.value, {
      groupBy: props.groupBy || null,
      splitBy: props.splitBy,
      metric: props.metric || 'count',
    })
    rows.value = result.rows || []
    columns.value = result.columns || []
    cells.value = result.cells || []
    max.value = Number(result.max) || 0
    truncated.value = !!result.truncated
  } catch {
    rows.value = []
    columns.value = []
    cells.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadDefinition(props.teamId)
  load()
})

watch(signature, load)
watch(() => [props.groupBy, props.splitBy, props.metric], load)
useAutoRefresh(load, () => props.refreshInterval)
</script>
