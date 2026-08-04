<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col justify-between"
       :class="thresholdClass">
    <OrphanRichFilter v-if="error === 'missing'" />

    <template v-else>
      <div class="flex items-start justify-between gap-2">
        <h3 class="text-sm font-semibold text-gray-700 truncate">
          {{ title || definition?.name || 'Sayaç' }}
        </h3>
        <span v-if="isFiltered" class="shrink-0 text-[10px] px-1.5 py-0.5 rounded bg-purple-50 text-purple-600 border border-purple-100">
          daraltıldı
        </span>
      </div>

      <button class="text-left mt-3 group" @click="openInWorkList">
        <span class="text-3xl font-semibold tabular-nums" :class="valueClass">
          {{ loading ? '…' : count }}
        </span>
        <span class="block text-[11px] text-gray-400 group-hover:text-purple-600 transition-colors">
          görev · listede aç
        </span>
      </button>
    </template>
  </div>
</template>

<script setup>
/**
 * Zengin filtreye bağlı sayaç.
 *
 * Eşik aşılınca kart rengi değişir — "bu sayı büyüdüyse bir sorun var" sinyali.
 * Eşikler widget yapılandırmasında taşınır (`threshold: { warn, danger }`).
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import OrphanRichFilter from './OrphanRichFilter.vue'
import { useRichFilterContext } from '../../composables/useRichFilterContext.js'
import { useAutoRefresh } from '../../composables/useAutoRefresh.js'
import { countRichFilter, resolveRichFilter } from '../../api/RichFilterApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  richFilterId: { type: String, required: true },
  title: { type: String, default: '' },
  threshold: { type: Object, default: () => ({}) },
  refreshInterval: { type: Number, default: 0 },
})

const router = useRouter()
const { definition, error, payload, signature, isFiltered, loadDefinition } =
  useRichFilterContext(() => props.richFilterId)

const count = ref(0)
const loading = ref(false)

const level = computed(() => {
  const { warn, danger } = props.threshold || {}
  if (danger != null && count.value >= danger) return 'danger'
  if (warn != null && count.value >= warn) return 'warn'
  return 'normal'
})

const thresholdClass = computed(() => ({
  danger: 'border-red-200 bg-red-50/40',
  warn: 'border-amber-200 bg-amber-50/40',
  normal: '',
}[level.value]))

const valueClass = computed(() => ({
  danger: 'text-red-600',
  warn: 'text-amber-600',
  normal: 'text-gray-800',
}[level.value]))

async function load() {
  if (!props.teamId || !props.richFilterId) return
  loading.value = true
  try {
    count.value = await countRichFilter(props.teamId, props.richFilterId, payload.value)
  } catch {
    count.value = 0
  } finally {
    loading.value = false
  }
}

async function openInWorkList() {
  try {
    const { stql } = await resolveRichFilter(props.teamId, props.richFilterId, payload.value)
    router.push({ path: `/workList/${props.teamId}`, query: stql ? { q: stql } : {} })
  } catch {
    router.push(`/workList/${props.teamId}`)
  }
}

onMounted(async () => {
  await loadDefinition(props.teamId)
  load()
})

watch(signature, load)
useAutoRefresh(load, () => props.refreshInterval)
</script>
