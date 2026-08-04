<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-2"
       :class="cardClass">
    <OrphanRichFilter v-if="error === 'missing'" />

    <template v-else>
      <div class="flex items-start justify-between gap-2">
        <h3 class="text-sm font-semibold text-gray-700 truncate">
          {{ title || definition?.name || 'Oran' }}
        </h3>
        <span v-if="isFiltered" class="shrink-0 text-[10px] px-1.5 py-0.5 rounded bg-purple-50 text-purple-600 border border-purple-100">
          daraltıldı
        </span>
      </div>

      <p v-if="!numeratorReady" class="py-8 text-center text-xs text-gray-400">
        Pay olarak seçilen akıllı filtre artık yok.
        <router-link :to="`/rich-filters/${richFilterId}`" class="text-purple-600 hover:underline">
          Düzenle
        </router-link>
      </p>

      <template v-else>
        <!-- Gösterge: yarım daire + hedef çentiği -->
        <button class="self-center relative" :title="numeratorId ? 'Bu kategoriye daralt' : ''" @click="focusNumerator">
          <svg viewBox="0 0 140 84" class="w-40 h-24">
            <path :d="ARC" fill="none" stroke="#F1F5F9" :stroke-width="STROKE" stroke-linecap="round" />
            <!-- Sıfır oranda hiç çizilmez: yuvarlak uç, sıfırı bir nokta gibi gösterirdi -->
            <path
              v-if="clamped > 0"
              :d="ARC"
              fill="none"
              :stroke="statusColor"
              :stroke-width="STROKE"
              stroke-linecap="round"
              :stroke-dasharray="`${ARC_LENGTH * clamped} ${ARC_LENGTH}`"
              class="transition-all duration-500"
            />
            <!-- Hedef çentiği: "iyi" sayılan sınır göz kararı değil, çizili olsun -->
            <line
              v-if="target != null"
              :x1="tick.x1" :y1="tick.y1" :x2="tick.x2" :y2="tick.y2"
              stroke="#475569" stroke-width="2" stroke-linecap="round"
            />
          </svg>

          <span class="absolute inset-x-0 bottom-1 flex flex-col items-center">
            <span class="text-2xl font-semibold tabular-nums" :style="{ color: statusColor }">
              {{ loading ? '…' : percent }}
            </span>
            <span v-if="target != null" class="text-[10px] text-gray-400">
              hedef {{ Math.round(target * 100) }}%
            </span>
          </span>
        </button>

        <p class="text-center text-[11px] text-gray-500">
          <span class="font-medium text-gray-700 tabular-nums">{{ numerator }}</span>
          {{ numeratorLabel }}
          <span class="text-gray-300 mx-1">/</span>
          <span class="font-medium text-gray-700 tabular-nums">{{ denominator }}</span>
          {{ denominatorLabel }}
        </p>

        <p v-if="target != null" class="text-center text-[11px]" :style="{ color: statusColor }">
          {{ statusText }}
        </p>

        <button class="self-center text-[11px] text-purple-600 hover:text-purple-700" @click="openInWorkList">
          Görevlerde aç
        </button>
      </template>
    </template>
  </div>
</template>

<script setup>
/**
 * İki ölçünün oranı — hedef çizgili gösterge.
 *
 * Pay ve payda, zengin filtrenin <b>kendi akıllı filtrelerinden</b> seçilir;
 * payda boş bırakılırsa o anki sonuç kümesinin tamamıdır. İkisi de tek bir
 * gruplama isteğinden okunur: sınıflandırma zaten bütün kovaları tek
 * {@code CASE WHEN} sorgusunda üretiyor, ayrı bir oran ucuna gerek yok
 * (bkz. RICH_FILTER_PLAN.md — K21).
 *
 * Bu, oranın grafikteki dilimle her zaman aynı sayıyı vermesini de garanti eder:
 * iki widget aynı kovadan besleniyor.
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import OrphanRichFilter from './OrphanRichFilter.vue'
import { useRichFilterContext } from '../../composables/useRichFilterContext.js'
import { useAutoRefresh } from '../../composables/useAutoRefresh.js'
import { aggregateRichFilter, resolveRichFilter } from '../../api/RichFilterApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  richFilterId: { type: String, required: true },
  title: { type: String, default: '' },
  /** Payı oluşturan akıllı filtre. */
  numeratorId: { type: String, default: '' },
  /** Payda: boşsa sonuç kümesinin tamamı. */
  denominatorId: { type: String, default: '' },
  /** 0–1 arası hedef; null ise gösterge nötr renkte kalır. */
  target: { type: Number, default: null },
  /** 'higher_better' | 'lower_better' */
  direction: { type: String, default: 'higher_better' },
  refreshInterval: { type: Number, default: 0 },
})

const router = useRouter()
const { definition, error, payload, signature, isFiltered, loadDefinition, setSmart } =
  useRichFilterContext(() => props.richFilterId)

// Yarım daire geometrisi
const CX = 70
const CY = 74
const R = 58
const STROKE = 12
const ARC = `M ${CX - R} ${CY} A ${R} ${R} 0 0 1 ${CX + R} ${CY}`
const ARC_LENGTH = Math.PI * R

const buckets = ref([])
const loading = ref(false)

const bucketOf = (key) => buckets.value.find(b => b.key === key) || null

const numeratorReady = computed(() =>
  !props.numeratorId || !buckets.value.length || !!bucketOf(props.numeratorId)
)

const numerator = computed(() => Number(bucketOf(props.numeratorId)?.value) || 0)

const denominator = computed(() => {
  if (props.denominatorId) return Number(bucketOf(props.denominatorId)?.value) || 0
  return buckets.value.reduce((sum, b) => sum + (Number(b.value) || 0), 0)
})

const numeratorLabel = computed(() => bucketOf(props.numeratorId)?.label || 'seçili')
const denominatorLabel = computed(() =>
  props.denominatorId ? (bucketOf(props.denominatorId)?.label || 'payda') : 'toplam'
)

/** Payda sıfırsa oran tanımsızdır; 0 göstermek "kötü" izlenimi verirdi. */
const value = computed(() => (denominator.value > 0 ? numerator.value / denominator.value : null))
const clamped = computed(() => Math.min(Math.max(value.value ?? 0, 0), 1))
const percent = computed(() => (value.value == null ? '—' : `${Math.round(value.value * 100)}%`))

const NEUTRAL = '#6366F1'
const OK = '#10B981'
const WARN = '#F59E0B'
const DANGER = '#EF4444'

/** Hedefe uzaklık: %10'luk bant "yaklaşıyor" sayılır. */
const level = computed(() => {
  if (props.target == null || value.value == null) return 'neutral'
  const higher = props.direction !== 'lower_better'
  if (higher) {
    if (value.value >= props.target) return 'ok'
    return value.value >= props.target * 0.9 ? 'warn' : 'danger'
  }
  if (value.value <= props.target) return 'ok'
  return value.value <= props.target * 1.1 ? 'warn' : 'danger'
})

const statusColor = computed(() =>
  ({ ok: OK, warn: WARN, danger: DANGER, neutral: NEUTRAL }[level.value])
)

const statusText = computed(() => ({
  ok: 'Hedefte',
  warn: 'Hedefe yakın',
  danger: props.direction === 'lower_better' ? 'Hedefin üzerinde' : 'Hedefin altında',
  neutral: '',
}[level.value]))

const cardClass = computed(() => ({
  danger: 'border-red-200 bg-red-50/40',
  warn: 'border-amber-200 bg-amber-50/40',
  ok: '',
  neutral: '',
}[level.value]))

/** Hedef çentiğinin uçları — yay üzerinde hedefe karşılık gelen açıda. */
const tick = computed(() => {
  const angle = Math.PI * (1 - Math.min(Math.max(props.target ?? 0, 0), 1))
  const point = (radius) => ({
    x: CX + radius * Math.cos(angle),
    y: CY - radius * Math.sin(angle),
  })
  const outer = point(R + STROKE / 2 + 2)
  const inner = point(R - STROKE / 2 - 2)
  return { x1: inner.x, y1: inner.y, x2: outer.x, y2: outer.y }
})

/** Göstergeye tıklamak panoyu payın kategorisine daraltır. */
function focusNumerator() {
  if (props.numeratorId) setSmart([props.numeratorId])
}

async function openInWorkList() {
  try {
    const { stql } = await resolveRichFilter(props.teamId, props.richFilterId, payload.value)
    router.push({ path: `/workList/${props.teamId}`, query: stql ? { q: stql } : {} })
  } catch {
    router.push(`/workList/${props.teamId}`)
  }
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
}

onMounted(async () => {
  await loadDefinition(props.teamId)
  load()
})

watch(signature, load)
useAutoRefresh(load, () => props.refreshInterval)
</script>
