<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <OrphanRichFilter v-if="error === 'missing'" />

    <template v-else>
      <!-- Başlık + hızlı ayarlar -->
      <div class="flex items-start justify-between gap-2 group/head">
        <div class="min-w-0">
          <h3 class="text-sm font-semibold text-gray-700 truncate">
            {{ title || definition?.name || 'Dağılım' }}
          </h3>
          <p class="text-[11px] text-gray-400 truncate">{{ groupLabel }} · {{ metricLabel }}</p>
        </div>

        <div class="flex items-center gap-1 opacity-0 group-hover/head:opacity-100 transition-opacity">
          <button
            v-for="option in CHART_TYPES"
            :key="option.value"
            class="w-6 h-6 rounded text-xs transition-colors"
            :class="chart === option.value ? 'bg-purple-100 text-purple-700' : 'text-gray-400 hover:bg-gray-100'"
            :title="option.label"
            @click="emit('config-change', { chart: option.value })"
          >{{ option.icon }}</button>

          <span class="w-px h-4 bg-gray-200 mx-0.5"></span>
          <button class="px-1 h-6 rounded text-[10px] text-gray-400 hover:bg-gray-100 hover:text-purple-600"
                  title="CSV indir" @click="exportCsv">CSV</button>
          <button class="px-1 h-6 rounded text-[10px] text-gray-400 hover:bg-gray-100 hover:text-purple-600"
                  title="PNG indir" @click="exportPng">PNG</button>
        </div>
      </div>

      <div v-if="loading" class="h-48 flex items-center justify-center text-xs text-gray-400">
        Hesaplanıyor…
      </div>

      <div v-else-if="!buckets.length" class="h-48 flex items-center justify-center text-xs text-gray-400">
        Bu seçimle eşleşen görev yok.
      </div>

      <template v-else>
        <div class="relative h-48">
          <Doughnut v-if="chart === 'donut'" ref="chartRef" :data="chartData" :options="doughnutOptions" />
          <Pie v-else-if="chart === 'pie'" ref="chartRef" :data="chartData" :options="doughnutOptions" />
          <Bar v-else ref="chartRef" :data="chartData" :options="barOptions" />
        </div>

        <!-- Efsane: tıklanabilir; sınıflandırma ekseninde seçim, diğerlerinde drill-down -->
        <div class="flex flex-wrap gap-x-3 gap-y-1.5">
          <button
            v-for="(bucket, index) in buckets"
            :key="bucket.key || 'empty'"
            class="inline-flex items-center gap-1.5 text-[11px] transition-opacity"
            :class="isDimmed(bucket) ? 'opacity-40' : ''"
            @click="onBucketClick(bucket)"
          >
            <span class="w-2.5 h-2.5 rounded-sm" :style="{ backgroundColor: colorFor(bucket, index) }"></span>
            <span class="text-gray-600">{{ bucket.label }}</span>
            <span class="text-gray-400 tabular-nums">{{ bucket.value }}</span>
          </button>
        </div>
      </template>

      <p v-if="unclassified > 0" class="text-[11px] text-amber-600">
        {{ unclassified }} görev hiçbir akıllı filtreye uymuyor.
      </p>
    </template>
  </div>
</template>

<script setup>
/**
 * Zengin filtreye bağlı dağılım grafiği.
 *
 * Dilime tıklamanın anlamı eksene göre değişir:
 *  - Sınıflandırma ekseninde (varsayılan) seçim paylaşılan duruma yazılır ve
 *    aynı filtreye bağlı bütün widget'lar daralır — çapraz filtreleme.
 *  - Alan ekseninde (atanan, öncelik…) seçim durumu bu alanı henüz taşımadığı
 *    için görev listesine geçilir. Dinamik filtreler geldiğinde (Faz 4) bu da
 *    daraltmaya döner.
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Doughnut, Pie, Bar } from 'vue-chartjs'
import {
  Chart as ChartJS, ArcElement, CategoryScale, LinearScale, BarElement, Tooltip, Legend,
} from 'chart.js'
import OrphanRichFilter from './OrphanRichFilter.vue'
import { useRichFilterContext } from '../../composables/useRichFilterContext.js'
import { useAutoRefresh } from '../../composables/useAutoRefresh.js'
import { aggregateRichFilter, resolveRichFilter, smartField } from '../../api/RichFilterApi.js'
import { colorFor, fade } from '../../utils/chartPalette.js'
import { downloadCsv, downloadChartPng } from '../../utils/widgetExport.js'

ChartJS.register(ArcElement, CategoryScale, LinearScale, BarElement, Tooltip, Legend)

const props = defineProps({
  teamId: { type: String, required: true },
  richFilterId: { type: String, required: true },
  title: { type: String, default: '' },
  /** Boş bırakılırsa zengin filtrenin kendi akıllı filtreleri eksen olur. */
  groupBy: { type: String, default: '' },
  metric: { type: String, default: 'count' },
  chart: { type: String, default: 'donut' },
  refreshInterval: { type: Number, default: 0 },
})

const emit = defineEmits(['config-change'])

const router = useRouter()
const {
  definition, error, selectedSmart, payload, signature, loadDefinition, toggleSmart,
} = useRichFilterContext(() => props.richFilterId)

const CHART_TYPES = [
  { value: 'donut', label: 'Halka', icon: '◍' },
  { value: 'pie', label: 'Pasta', icon: '◕' },
  { value: 'bar', label: 'Çubuk', icon: '▥' },
]

const buckets = ref([])
const loading = ref(false)
const chartRef = ref(null)

/** Dosya adı: widget başlığı ya da zengin filtrenin adı. */
const exportName = () => props.title || definition.value?.name || 'dagilim'

function exportCsv() {
  downloadCsv(exportName(), ['Kategori', metricLabel.value], buckets.value.map(b => [b.label, b.value]))
}

function exportPng() {
  downloadChartPng(chartRef, exportName())
}

/** Eksen, zengin filtrenin kendi sınıflandırması mı? */
const isSmartAxis = computed(() =>
  !props.groupBy || props.groupBy === smartField(definition.value?.name || '')
)

const groupLabel = computed(() => (isSmartAxis.value ? 'Akıllı filtreler' : props.groupBy))
const metricLabel = computed(() => (props.metric === 'count' ? 'görev sayısı' : props.metric))

const unclassified = computed(() =>
  isSmartAxis.value ? Number(buckets.value.find(b => !b.key)?.value || 0) : 0
)

/** Sınıflandırma ekseninde seçili olmayan dilimler soluklaşır — odak vurgusu. */
function isDimmed(bucket) {
  return isSmartAxis.value && selectedSmart.value.length > 0 && !selectedSmart.value.includes(bucket.key)
}

const chartData = computed(() => ({
  labels: buckets.value.map(b => b.label),
  datasets: [{
    data: buckets.value.map(b => Number(b.value) || 0),
    backgroundColor: buckets.value.map((b, i) => {
      const color = colorFor(b, i)
      return isDimmed(b) ? fade(color) : color
    }),
    borderWidth: 0,
    borderRadius: props.chart === 'bar' ? 4 : 0,
  }],
}))

const baseOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
}

const doughnutOptions = computed(() => ({
  ...baseOptions,
  cutout: props.chart === 'donut' ? '62%' : 0,
  onClick: onChartClick,
}))

const barOptions = computed(() => ({
  ...baseOptions,
  indexAxis: 'y',
  scales: {
    x: { beginAtZero: true, ticks: { font: { size: 10 } }, grid: { color: '#F1F5F9' } },
    y: { ticks: { font: { size: 10 } }, grid: { display: false } },
  },
  onClick: onChartClick,
}))

function onChartClick(_event, elements) {
  const index = elements?.[0]?.index
  if (index == null) return
  onBucketClick(buckets.value[index])
}

async function onBucketClick(bucket) {
  if (!bucket) return

  // Sınıflandırma ekseni: seçimi paylaşılan duruma yaz → tüm widget'lar daralır.
  if (isSmartAxis.value && bucket.key) {
    toggleSmart(bucket.key)
    return
  }
  await openBucketInWorkList(bucket)
}

/**
 * Kovayı görev listesinde açar.
 *
 * Sunucudan gelen `filter` parçası ile mevcut seçimin STQL karşılığı burada
 * birleştirilir. Metin birleştirme yalnız bu link için yapılır — iki taraf da
 * parantezlenir ve sorgu sunucuda yeniden çözümlenir; kapsam denetimi orada.
 */
async function openBucketInWorkList(bucket) {
  let stql = bucket.filter || ''
  try {
    const resolved = await resolveRichFilter(props.teamId, props.richFilterId, payload.value)
    if (resolved.stql && bucket.filter) stql = `(${resolved.stql}) AND (${bucket.filter})`
    else if (resolved.stql) stql = resolved.stql
  } catch {
    // Çözümleme başarısızsa yalnız kova koşuluyla açmak, hiç açmamaktan iyidir.
  }
  router.push({ path: `/workList/${props.teamId}`, query: stql ? { q: stql } : {} })
}

async function load() {
  if (!props.teamId || !props.richFilterId) return
  loading.value = true
  try {
    buckets.value = await aggregateRichFilter(props.teamId, props.richFilterId, payload.value, {
      groupBy: props.groupBy || null,
      metric: props.metric || 'count',
    })
  } catch {
    buckets.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadDefinition(props.teamId)
  load()
})

watch(signature, load)
watch(() => [props.groupBy, props.metric], load)
useAutoRefresh(load, () => props.refreshInterval)
</script>
