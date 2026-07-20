<template>
  <div class="flex flex-col h-full">
    <!-- Filtre çubuğu -->
    <FilterBar
      :active-filters="activeFilters"
      @add-filter="addFilter"
      @remove-filter="removeFilter"
      @clear-filters="clearFilters"
      @open-builder="showBuilder = true"
    />

    <!-- Board içeriği -->
    <div v-if="isLoading" class="flex-1 flex items-center justify-center">
      <div class="text-gray-400 text-sm">Yükleniyor…</div>
    </div>

    <!-- Swimlane modu (assignee veya priority gruplaması) -->
    <div v-else-if="groupBy && groupBy !== 'status'" class="flex-1 overflow-auto pb-4 px-2 pt-2">
      <BoardSwimlane
        :columns="columns"
        :tasks="allTasks"
        :group-by="groupBy"
        @task-click="openTask"
        @task-drop="handleDrop"
      />
    </div>

    <!-- Normal status-based board -->
    <div v-else class="flex-1 overflow-x-auto pb-4">
      <div class="flex gap-4 px-2 pt-2 min-w-max">
        <BoardColumn
          v-for="col in columns"
          :key="col.name"
          :column="col"
          :tasks="tasksByStatus[col.name] || []"
          @task-click="openTask"
          @task-drop="handleDrop"
        />
      </div>
    </div>

    <!-- Filter builder modal -->
    <FilterBuilder
      :is-open="showBuilder"
      :initial-filters="activeFilters"
      @close="showBuilder = false"
      @apply="applyBuilderFilters"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BoardColumn   from './BoardColumn.vue'
import BoardSwimlane from './BoardSwimlane.vue'
import FilterBar     from './FilterBar.vue'
import FilterBuilder from './FilterBuilder.vue'
import { getTasks }  from '../../api/WorkApi.js'
import { filterTasks } from '../../api/WorkApi.js'
import { updateTask }  from '../../api/WorkApi.js'

const props = defineProps({
  teamId:  { type: String, required: true },
  /** Aktif proje context'i — null ise takımın tüm projelerindeki görevler gösterilir. */
  projectId: { type: String, default: null },
  columns: {
    type: Array,
    default: () => [
      { name: 'To Do',       color: '#6B7280', wipLimit: 0 },
      { name: 'In Progress', color: '#3B82F6', wipLimit: 3 },
      { name: 'Done',        color: '#10B981', wipLimit: 0 },
    ]
  },
  groupBy: { type: String, default: 'status' }
})

const router = useRouter()

// ─── State ────────────────────────────────────────────────────────────────────
const allTasks     = ref([])
const isLoading    = ref(false)
const showBuilder  = ref(false)
const activeFilters = ref([])

// ─── Görevleri Yükle ──────────────────────────────────────────────────────────
async function loadTasks() {
  isLoading.value = true
  try {
    if (activeFilters.value.length > 0) {
      const result = await filterTasks(props.teamId, {
        filters:   activeFilters.value,
        projectId: props.projectId || undefined,
        sortBy:  'position',
        sortDir: 'asc',
        size:    200
      })
      allTasks.value = result.content
    } else {
      allTasks.value = await getTasks(props.teamId, false, props.projectId)
    }
  } catch (e) {
    console.error('Board yükleme hatası:', e)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadTasks)
watch(() => [props.teamId, props.projectId], loadTasks)
watch(activeFilters, loadTasks, { deep: true })

// ─── Sütun–görev eşlemesi ─────────────────────────────────────────────────────
const tasksByStatus = computed(() => {
  const map = {}
  for (const col of props.columns) map[col.name] = []
  for (const task of allTasks.value) {
    if (map[task.status]) {
      map[task.status].push(task)
    } else {
      // Bilinmeyen status → ilk sütuna at (geri uyumluluk)
      const first = props.columns[0]
      if (first) (map[first.name] = map[first.name] || []).push(task)
    }
  }
  return map
})

// ─── Sürükle-Bırak ───────────────────────────────────────────────────────────
async function handleDrop({ taskId, toStatus, fromStatus }) {
  // Optimistic update
  const task = allTasks.value.find(t => t.id === taskId)
  if (task) task.status = toStatus
  try {
    await updateTask(props.teamId, taskId, { status: toStatus })
    await loadTasks()
  } catch (e) {
    // Geri al
    if (task) task.status = fromStatus
    console.error('Status güncellenemedi:', e)
  }
}

// ─── Task detay ───────────────────────────────────────────────────────────────
function openTask(task) {
  router.push({ name: 'TaskDetail', params: { taskId: task.customId || task.id } })
}

// ─── Filtreler ────────────────────────────────────────────────────────────────
function addFilter(filter) {
  const idx = activeFilters.value.findIndex(f => f.field === filter.field)
  if (idx >= 0) activeFilters.value[idx] = { ...filter }
  else activeFilters.value.push({ ...filter })
}

function removeFilter(field) {
  activeFilters.value = activeFilters.value.filter(f => f.field !== field)
}

function clearFilters() {
  activeFilters.value = []
}

function applyBuilderFilters(filters) {
  activeFilters.value = filters
}
</script>

