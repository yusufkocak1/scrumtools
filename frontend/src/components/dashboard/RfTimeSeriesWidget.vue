<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <OrphanRichFilter v-if="error === 'missing'" />

    <template v-else>
      <div class="flex items-start justify-between gap-2 group/head">
        <div class="min-w-0">
          <h3 class="text-sm font-semibold text-gray-700 truncate">
            {{ title || definition?.name || 'Zaman serisi' }}
          </h3>
          <p class="text-[11px] text-gray-400">
            Son {{ days }} gün · {{ interval === 'week' ? 'haftalık' : 'günlük' }}
          </p>
        </div>

        <div class="flex items-center gap-1 opacity-0 group-hover/head:opacity-100 transition-opacity">
          <button
            v-for="option in WINDOWS"
            :key="option.value"
            class="px-1.5 h-6 rounded text-[10px] transition-colors"
            :class="days === option.value ? 'bg-purple-100 text-purple-700' : 'text-gray-400 hover:bg-gray-100'"
            @click="emit('config-change', { days: option.value })"
          >{{ option.label }}</button>
        </div>
      </div>

      <div v-if="loading" class="h-48 flex items-center justify-center text-xs text-gray-400">
        Yükleniyor…
      </div>

      <div v-else-if="!series.length" class="h-48 flex flex-col items-center justify-center gap-1 text-center">
        <p class="text-xs text-gray-400">Bu zengin filtrede zaman serisi tanımlı değil.</p>
        <router-link :to="`/rich-filters/${richFilterId}`" class="text-[11px] text-purple-600 hover:underline">
          Seri tanımla
        </router-link>
      </div>

      <div v-else-if="!labels.length" class="h-48 flex items-center justify-center px-6 text-center">
        <p class="text-xs text-gray-400">
          Henüz veri yok. Geçmiş kurgulanamadıysa seri bugünden itibaren birikmeye başlar.
        </p>
      </div>

      <template v-else>
        <div class="relative h-48">
          <Line :data="chartData" :options="chartOptions" />
        </div>

        <!-- Efsane: tıklanınca panodaki seçim o kategoriye geçer -->
        <div class="flex flex-wrap gap-x-3 gap-y-1.5">
          <button
            v-for="(item, index) in series"
            :key="item.elementId"
            class="inline-flex items-center gap-1.5 text-[11px] transition-opacity"
            :class="isDimmed(item) ? 'opacity-40' : ''"
            @click="onSeriesClick(item)"
          >
            <span class="w-3 h-0.5 rounded-full" :style="{ backgroundColor: colorOf(item, index) }"></span>
            <span class="text-gray-600">{{ item.name }}</span>
            <span class="text-gray-400 tabular-nums">{{ lastValue(item) }}</span>
          </button>
        </div>

        <p v-if="hasReplayed" class="text-[11px] text-gray-400">
          Kesikli bölüm geçmişten kurgulandı; düz bölüm o gün ölçüldü.
        </p>
      </template>
    </template>
  </div>
</template>

<script setup>
/**
 * Zaman serisi grafiği — bir akıllı filtrenin sayısının gün gün seyri.
 *
 * Diğer zengin filtre widget'larından bir farkı var: <b>seçimden etkilenmez</b>.
 * Noktalar öğe başına önceden ölçülmüştür; geçmişteki satır artık olmadığı için
 * çapraz filtreleme geçmişi yeniden hesaplayamaz (bkz. RICH_FILTER_PLAN.md — K23).
 * Seçim yine de anlamlıdır: seçili kategorinin çizgisi öne çıkar, diğerleri solar —
 * pano daraltıldığında grafik "ilgisiz" değil, "odaklanmış" görünür.
 *
 * Kurgulanmış geçmiş kesikli çizilir. Ölçülen değerle kurgulanan değeri aynı
 * çizgide birleştirmek, bakan kişinin güvenilirlik farkını görmesini engellerdi.
 */
import { ref, computed, watch, onMounted } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS, LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Filler,
} from 'chart.js'
import OrphanRichFilter from './OrphanRichFilter.vue'
import { useRichFilterContext } from '../../composables/useRichFilterContext.js'
import { useAutoRefresh } from '../../composables/useAutoRefresh.js'
import { getRichFilterSeries } from '../../api/RichFilterApi.js'
import { colorFor, fade } from '../../utils/chartPalette.js'

ChartJS.register(LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Filler)

const props = defineProps({
  teamId: { type: String, required: true },
  richFilterId: { type: String, required: true },
  title: { type: String, default: '' },
  /** Gösterilecek seriler; boşsa hepsi. */
  elementIds: { type: Array, default: () => [] },
  days: { type: Number, default: 90 },
  interval: { type: String, default: 'day' },
  refreshInterval: { type: Number, default: 0 },
})

const emit = defineEmits(['config-change'])

const { definition, error, selectedSmart, loadDefinition, toggleSmart } =
  useRichFilterContext(() => props.richFilterId)

const WINDOWS = [
  { value: 30, label: '30g' },
  { value: 90, label: '90g' },
  { value: 180, label: '180g' },
]

const all = ref([])
const loading = ref(false)

const series = computed(() => {
  if (!props.elementIds?.length) return all.value
  const wanted = new Set(props.elementIds)
  return all.value.filter(s => wanted.has(s.elementId))
})

/** Serilerin tarihleri farklı başlayabilir; eksen hepsinin birleşimidir. */
const labels = computed(() => {
  const dates = new Set()
  series.value.forEach(s => (s.points || []).forEach(p => dates.add(p.date)))
  return [...dates].sort()
})

const hasReplayed = computed(() =>
  series.value.some(s => (s.points || []).some(p => p.source === 'REPLAY'))
)

const colorOf = (item, index) => colorFor({ key: item.elementId, color: item.color }, index)

/** Seçim varken, seçili kategoriye bağlı olmayan seriler solar. */
function isDimmed(item) {
  return selectedSmart.value.length > 0
    && (!item.smartFilterId || !selectedSmart.value.includes(item.smartFilterId))
}

function lastValue(item) {
  const points = item.points || []
  return points.length ? Number(points[points.length - 1].value) : '—'
}

/** Tarih → değer eşlemesi; eksik günlerde çizgi kesilmesin diye null bırakılır. */
function valuesOf(item) {
  const byDate = new Map((item.points || []).map(p => [p.date, Number(p.value)]))
  return labels.value.map(date => (byDate.has(date) ? byDate.get(date) : null))
}

function sourcesOf(item) {
  const byDate = new Map((item.points || []).map(p => [p.date, p.source]))
  return labels.value.map(date => byDate.get(date) || null)
}

const chartData = computed(() => ({
  labels: labels.value.map(shortDate),
  datasets: series.value.map((item, index) => {
    const color = colorOf(item, index)
    const dimmed = isDimmed(item)
    const sources = sourcesOf(item)

    return {
      label: item.name,
      data: valuesOf(item),
      // Soluklaştırma rengin kendisinde yapılır: Chart.js veri kümesi bazında
      // saydamlık tanımıyor, çizgiyi gizlemek ise karşılaştırmayı bozardı.
      borderColor: dimmed ? fade(color, 0.35) : color,
      backgroundColor: dimmed ? fade(color, 0.35) : color,
      borderWidth: dimmed ? 1 : 2,
      pointRadius: 0,
      pointHoverRadius: 3,
      tension: 0.25,
      spanGaps: true,
      // Kurgulanmış bölüm kesikli: ucu kurguya dayanan parça tahmindir.
      segment: {
        borderDash: (seg) => (sources[seg.p1DataIndex] === 'REPLAY' ? [4, 3] : undefined),
      },
    }
  }),
}))

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'index', intersect: false },
  plugins: {
    legend: { display: false },
    tooltip: { callbacks: { title: (items) => items[0]?.label ?? '' } },
  },
  scales: {
    x: {
      ticks: { font: { size: 10 }, maxTicksLimit: 8, autoSkip: true },
      grid: { display: false },
    },
    y: {
      beginAtZero: true,
      ticks: { font: { size: 10 }, precision: 0 },
      grid: { color: '#F1F5F9' },
    },
  },
}))

/** "2026-08-04" → "4 Ağu" */
function shortDate(iso) {
  const date = new Date(`${iso}T00:00:00`)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' })
}

function onSeriesClick(item) {
  if (item.smartFilterId) toggleSmart(item.smartFilterId)
}

async function load() {
  if (!props.teamId || !props.richFilterId) return
  loading.value = true
  try {
    all.value = await getRichFilterSeries(props.teamId, props.richFilterId, {
      days: props.days, interval: props.interval,
    })
  } catch {
    all.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadDefinition(props.teamId)
  load()
})

watch(() => [props.days, props.interval], load)
useAutoRefresh(load, () => props.refreshInterval)
</script>
