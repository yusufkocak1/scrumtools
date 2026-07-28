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
          :tasks="tasksByColumn[col.name] || []"
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
import { distributeTasks, targetStatusFor } from '../../utils/boardColumns.js'

const props = defineProps({
  teamId:  { type: String, required: true },
  /** Aktif proje context'i — null ise takımın tüm projelerindeki görevler gösterilir. */
  projectId: { type: String, default: null },
  /**
   * Sütunlar: [{ name, color, wipLimit, statuses[] }]. `statuses` bir sütunun
   * hangi görev durumlarını topladığını söyler; tanımsızsa sütun kendi adıyla
   * eşleşir (eski board'lar için geri uyum). Sütun listesini WorkList üretir —
   * burada sabit varsayılan tutmuyoruz, aksi halde takımın durumlarını
   * yeniden adlandırması board'ı sessizce boşaltırdı.
   */
  columns: { type: Array, default: () => [] },
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
// Sütun adı = durum adı varsayımı kaldırıldı; eşleme sütunun `statuses` dizisinden
// okunur (bkz. utils/boardColumns.js).
const distribution = computed(() => distributeTasks(props.columns, allTasks.value))
const tasksByColumn = computed(() => distribution.value.byColumn)

// ─── Sürükle-Bırak ───────────────────────────────────────────────────────────
async function handleDrop({ taskId, toColumn, fromStatus }) {
  const task = allTasks.value.find(t => t.id === taskId)
  const column = props.columns.find(c => c.name === toColumn)
  if (!column) return

  // Hedef sütun birden fazla durum topluyorsa görevin durumu sütunun birincil
  // durumuna çekilir; görev zaten o sütuna ait bir durumdaysa korunur.
  const toStatus = targetStatusFor(column, task?.status ?? fromStatus)
  if (!toStatus || toStatus === task?.status) return

  // Optimistic update
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

