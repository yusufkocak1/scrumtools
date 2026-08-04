<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <OrphanRichFilter v-if="error === 'missing'" />

    <template v-else>
      <div class="flex items-start justify-between gap-2">
        <h3 class="text-sm font-semibold text-gray-700 truncate">
          {{ title || definition?.name || 'Ölçüler' }}
        </h3>
        <button
          v-if="selectedSmart.length"
          class="shrink-0 text-[11px] text-purple-600 hover:text-purple-700"
          @click="setSmart([])"
        >
          Seçimi kaldır
        </button>
      </div>

      <div v-if="loading" class="py-6 text-center text-xs text-gray-400">Hesaplanıyor…</div>

      <div v-else-if="!cells.length" class="py-6 text-center text-xs text-gray-400">
        Gösterilecek ölçü yok.
      </div>

      <div v-else class="grid gap-2" :class="gridClass">
        <button
          v-for="cell in cells"
          :key="cell.key || 'total'"
          class="rounded-lg border px-3 py-2.5 text-left transition-colors"
          :class="cell.selected
            ? 'border-purple-300 bg-purple-50/70'
            : 'border-gray-100 hover:border-gray-200 hover:bg-gray-50/60'"
          @click="onCellClick(cell)"
        >
          <span class="flex items-center gap-1.5">
            <span v-if="cell.color" class="w-2 h-2 rounded-sm shrink-0" :style="{ backgroundColor: cell.color }"></span>
            <span class="text-[11px] text-gray-500 truncate">{{ cell.label }}</span>
          </span>
          <span class="block mt-0.5 text-xl font-semibold tabular-nums text-gray-800">
            {{ cell.value }}
          </span>
          <span v-if="cell.share != null" class="block text-[10px] text-gray-400 tabular-nums">
            %{{ cell.share }}
          </span>
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
/**
 * Tek kartta birden çok ölçü — "Açık 42 · Testte 18 · Geciken 5".
 *
 * Aynı bilgiyi üç ayrı sayaç widget'ıyla da vermek mümkün; fark, üç sayacın üç
 * ayrı sorgu çalıştırması ve panoda üç kutu yer kaplamasıdır. Burada bütün
 * ölçüler tek gruplama isteğinden okunur: sınıflandırma zaten tüm kovaları tek
 * sorguda üretiyor.
 *
 * Ölçüye tıklamak panoyu o kategoriye daraltır — kart hem özet hem kısayoldur.
 */
import { ref, computed, watch, onMounted } from 'vue'
import OrphanRichFilter from './OrphanRichFilter.vue'
import { useRichFilterContext } from '../../composables/useRichFilterContext.js'
import { useAutoRefresh } from '../../composables/useAutoRefresh.js'
import { aggregateRichFilter } from '../../api/RichFilterApi.js'
import { colorFor } from '../../utils/chartPalette.js'

const props = defineProps({
  teamId: { type: String, required: true },
  richFilterId: { type: String, required: true },
  title: { type: String, default: '' },
  /** Gösterilecek akıllı filtreler; boşsa hepsi. */
  measureIds: { type: Array, default: () => [] },
  /** Başa "Toplam" kutusu eklensin mi? */
  showTotal: { type: Boolean, default: true },
  /** Her ölçünün toplam içindeki payı yazılsın mı? */
  showShare: { type: Boolean, default: false },
  refreshInterval: { type: Number, default: 0 },
})

const { definition, error, selectedSmart, payload, signature, loadDefinition, toggleSmart, setSmart } =
  useRichFilterContext(() => props.richFilterId)

const buckets = ref([])
const loading = ref(false)

const total = computed(() => buckets.value.reduce((sum, b) => sum + (Number(b.value) || 0), 0))

const selectedMeasures = computed(() => {
  if (!props.measureIds?.length) return buckets.value.filter(b => b.key)

  // Yapılandırmadaki sıra korunur: kullanıcı kutuları o sırayla düşünmüştür.
  return props.measureIds
    .map(id => buckets.value.find(b => b.key === id))
    .filter(Boolean)
})

const cells = computed(() => {
  const list = selectedMeasures.value.map((bucket, index) => ({
    key: bucket.key,
    label: bucket.label,
    value: Number(bucket.value) || 0,
    color: colorFor(bucket, index),
    selected: selectedSmart.value.includes(bucket.key),
    share: props.showShare && total.value ? Math.round((Number(bucket.value) || 0) / total.value * 100) : null,
  }))

  if (props.showTotal) {
    list.unshift({
      key: '', label: 'Toplam', value: total.value, color: null,
      selected: false, share: null,
    })
  }
  return list
})

/** İki kutuya kadar tek sıra, sonrası ikişerli — dar widget'ta okunur kalsın. */
const gridClass = computed(() => (cells.value.length <= 2 ? 'grid-cols-2' : 'grid-cols-2 sm:grid-cols-3'))

function onCellClick(cell) {
  // "Toplam" bir kategori değil: tıklanınca daraltmayı kaldırır.
  if (!cell.key) setSmart([])
  else toggleSmart(cell.key)
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
