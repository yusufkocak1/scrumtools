<template>
  <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col gap-3">
    <OrphanRichFilter v-if="error === 'missing'" />

    <template v-else>
      <div class="flex items-start justify-between gap-2">
        <h3 class="text-sm font-semibold text-gray-700 truncate">
          {{ title || definition?.name || 'Görevler' }}
        </h3>
        <span class="shrink-0 text-[11px] text-gray-400">{{ total }} görev</span>
      </div>

      <div v-if="loading" class="py-8 text-center text-xs text-gray-400">Yükleniyor…</div>

      <div v-else-if="!tasks.length" class="py-8 text-center text-xs text-gray-400">
        Bu seçimle eşleşen görev yok.
      </div>

      <ul v-else class="divide-y divide-gray-50 -mx-1">
        <li v-for="task in tasks" :key="task.id">
          <button
            class="w-full flex items-center gap-2 px-1 py-2 text-left hover:bg-gray-50/70 rounded transition-colors"
            @click="$emit('task-click', task)"
          >
            <!-- Akıllı filtre etiketi: rengi sınıflandırmadan gelir -->
            <span
              class="w-1.5 h-8 rounded-full shrink-0"
              :style="{ backgroundColor: tagOf(task)?.color || '#E2E8F0' }"
              :title="tagOf(task)?.name || 'Sınıflandırılmamış'"
            ></span>

            <span class="text-[11px] font-mono text-gray-400 shrink-0 w-16 truncate">
              {{ task.customId }}
            </span>

            <span class="text-xs text-gray-700 truncate flex-1">{{ task.title }}</span>

            <span
              v-if="tagOf(task)"
              class="shrink-0 text-[10px] px-1.5 py-0.5 rounded-full text-white"
              :style="{ backgroundColor: tagOf(task).color || '#94A3B8' }"
            >
              {{ tagOf(task).name }}
            </span>
          </button>
        </li>
      </ul>

      <button
        v-if="total > tasks.length"
        class="text-[11px] text-purple-600 hover:text-purple-700 self-start"
        @click="openInWorkList"
      >
        Tümünü gör ({{ total }})
      </button>
    </template>
  </div>
</template>

<script setup>
/**
 * Zengin filtrenin ilk N görevi — satırlar akıllı filtre rengiyle etiketli.
 *
 * Etiketler sunucudan `smartTags` haritasıyla gelir (görev id → kategori);
 * sınıflandırma istemcide yeniden hesaplanmaz, grafiklerle aynı kaynağı kullanır.
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import OrphanRichFilter from './OrphanRichFilter.vue'
import { useRichFilterContext } from '../../composables/useRichFilterContext.js'
import { useAutoRefresh } from '../../composables/useAutoRefresh.js'
import { searchRichFilter, resolveRichFilter } from '../../api/RichFilterApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  richFilterId: { type: String, required: true },
  title: { type: String, default: '' },
  limit: { type: Number, default: 8 },
  refreshInterval: { type: Number, default: 0 },
})

defineEmits(['task-click'])

const router = useRouter()
const { definition, error, payload, signature, loadDefinition } =
  useRichFilterContext(() => props.richFilterId)

const tasks = ref([])
const tags = ref({})
const total = ref(0)
const loading = ref(false)

const tagOf = computed(() => (task) => tags.value[task.id] || null)

async function load() {
  if (!props.teamId || !props.richFilterId) return
  loading.value = true
  try {
    const result = await searchRichFilter(props.teamId, props.richFilterId, payload.value, {
      page: 0, size: props.limit,
    })
    tasks.value = result.content || []
    tags.value = result.smartTags || {}
    total.value = result.totalElements || 0
  } catch {
    tasks.value = []
    tags.value = {}
    total.value = 0
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
watch(() => props.limit, load)
useAutoRefresh(load, () => props.refreshInterval)
</script>
