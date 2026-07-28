<template>
  <div class="flex flex-col h-full">
    <!-- Sorgu çubuğu: görsel filtre + STQL -->
    <QueryBar
      v-model:query="query"
      :team-id="teamId"
      :project-id="projectId"
      :filters="filters"
      :builder-compatible="builderCompatible"
      :builder-incompatible-reason="builderIncompatibleReason"
      :active-filter-count="activeFilterCount"
      :error="error"
      @run="loadTasks"
      @validated="onValidated"
      @add-filter="onAddFilter"
      @remove-filter="onRemoveFilter"
      @clear-filters="onClearFilters"
      @apply-filters="onApplyFilters"
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

  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BoardColumn   from './BoardColumn.vue'
import BoardSwimlane from './BoardSwimlane.vue'
import QueryBar      from './QueryBar.vue'
import { getTasks, updateTask } from '../../api/WorkApi.js'
import { useTaskQuery } from '../../composables/useTaskQuery.js'

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
const allTasks  = ref([])
const isLoading = ref(false)

// Board tüm sütunları aynı anda gösterdiği için sorgu sayfası üst sınırda tutulur.
const {
  query, filters, builderCompatible, builderIncompatibleReason, activeFilterCount,
  error, tasks: queryTasks, hasQuery,
  addFilter, removeFilter, setFilters, clearAll, onValidated, run, restoreFromUrl,
} = useTaskQuery({
  teamId: computed(() => props.teamId),
  projectId: computed(() => props.projectId),
  pageSize: 200,
})

// ─── Görevleri Yükle ──────────────────────────────────────────────────────────
async function loadTasks() {
  isLoading.value = true
  try {
    if (hasQuery.value) {
      await run()
      allTasks.value = queryTasks.value
    } else {
      allTasks.value = await getTasks(props.teamId, false, props.projectId)
    }
  } catch (e) {
    console.error('Board yükleme hatası:', e)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  // Paylaşılan linkteki sorgu (?q=) varsa onunla açılır.
  restoreFromUrl()
  loadTasks()
})
watch(() => [props.teamId, props.projectId], loadTasks)

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

// ─── Filtre olayları ──────────────────────────────────────────────────────────
function onAddFilter(filter) {
  addFilter(filter)
  loadTasks()
}

function onRemoveFilter(field) {
  removeFilter(field)
  loadTasks()
}

function onClearFilters() {
  clearAll()
  loadTasks()
}

function onApplyFilters(list) {
  setFilters(list)
  loadTasks()
}
</script>

