<template>
  <div>
    <!-- Seçili görev -->
    <div
      v-if="selected"
      class="flex items-center justify-between border border-indigo-200 bg-indigo-50 rounded-lg px-3 py-1.5"
    >
      <div class="min-w-0 flex items-center gap-2">
        <span class="font-mono text-xs text-gray-500 shrink-0">{{ selected.customId }}</span>
        <span class="text-sm text-gray-800 truncate">{{ selected.title }}</span>
      </div>
      <button type="button" @click="clearSelection" class="text-gray-400 hover:text-gray-600 ml-2 shrink-0">✕</button>
    </div>

    <!-- Arama kutusu + sonuçlar -->
    <div v-else class="relative">
      <input
        v-model="query"
        type="text"
        :placeholder="placeholder"
        @focus="onFocus"
        @blur="showResults = false"
        class="w-full text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-blue-500"
      />
      <div
        v-if="showResults"
        class="absolute z-20 mt-1 w-full bg-white border rounded-lg shadow-lg max-h-56 overflow-y-auto"
      >
        <div v-if="searching" class="px-3 py-2 text-sm text-gray-400">Aranıyor...</div>
        <div v-else-if="results.length === 0" class="px-3 py-2 text-sm text-gray-400">Sonuç bulunamadı</div>
        <button
          v-else
          v-for="result in results"
          :key="result.id"
          type="button"
          :disabled="isDisabled(result)"
          @mousedown.prevent="selectTask(result)"
          class="w-full text-left px-3 py-2 flex items-center gap-2"
          :class="isDisabled(result) ? 'opacity-50 cursor-not-allowed' : 'hover:bg-indigo-50'"
        >
          <span class="font-mono text-xs text-gray-500 shrink-0">{{ result.customId }}</span>
          <span class="text-sm text-gray-800 truncate flex-1">{{ result.title }}</span>
          <span v-if="isDisabled(result)" class="text-[10px] text-amber-600 shrink-0">Alt görev seçilemez</span>
          <StatusBadge v-else :status="result.status" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import StatusBadge from '@/components/workflow/StatusBadge.vue'
import { searchTeamTasks, searchByCustomId } from '@/api/WorkApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  // Seçili görevin UUID'si
  modelValue: { type: String, default: null },
  // Kendisiyle ilişki kurulmasın diye sonuçlardan çıkarılacak görev
  excludeTaskId: { type: String, default: null },
  // true ise parent seçimi yapılıyor demektir: subtask'lar seçilemez (tek seviye hiyerarşi)
  subtaskTarget: { type: Boolean, default: false },
  placeholder: { type: String, default: 'Görev ara (ID veya başlık)...' },
})

const emit = defineEmits(['update:modelValue', 'select'])

const query = ref('')
const results = ref([])
const selected = ref(null)
const searching = ref(false)
const showResults = ref(false)
let searchTimer = null

watch(query, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(search, 300)
})

// Edit modunda dışarıdan gelen UUID'nin etiketini (customId · başlık) çöz
watch(() => props.modelValue, async (val) => {
  if (!val) {
    if (selected.value) resetState()
    return
  }
  if (selected.value?.id === val) return
  try {
    const task = await searchByCustomId(val)
    if (task) selected.value = { id: task.id, customId: task.customId, title: task.title, status: task.status }
  } catch (e) {
    // Etiket çözülemedi — seçim değeri yine de geçerli
  }
}, { immediate: true })

async function search() {
  searching.value = true
  showResults.value = true
  try {
    const res = await searchTeamTasks(props.teamId, query.value)
    results.value = res.filter(t => t.id !== props.excludeTaskId)
  } catch (e) {
    console.error(e)
    results.value = []
  } finally {
    searching.value = false
  }
}

function onFocus() {
  search()
}

function isDisabled(result) {
  return props.subtaskTarget && result.hasParent
}

function selectTask(result) {
  if (isDisabled(result)) return
  selected.value = result
  showResults.value = false
  emit('update:modelValue', result.id)
  emit('select', result)
}

function resetState() {
  selected.value = null
  query.value = ''
  results.value = []
  showResults.value = false
}

function clearSelection() {
  resetState()
  emit('update:modelValue', null)
}

onUnmounted(() => clearTimeout(searchTimer))
</script>
