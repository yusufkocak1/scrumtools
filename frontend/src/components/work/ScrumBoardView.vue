<template>
  <div class="flex flex-col h-full">
    <!-- Sprint yoksa veya aktif sprint yoksa uyarı -->
    <div v-if="isLoading" class="flex-1 flex items-center justify-center">
      <div class="text-gray-400 text-sm">Yükleniyor…</div>
    </div>

    <div v-else-if="activeSprints.length === 0" class="flex-1 flex items-center justify-center">
      <div class="text-center space-y-4">
        <div class="w-16 h-16 mx-auto bg-yellow-50 rounded-full flex items-center justify-center">
          <svg class="w-8 h-8 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z"/>
          </svg>
        </div>
        <div>
          <h3 class="text-lg font-semibold text-gray-900">Aktif Sprint Bulunamadı</h3>
          <p class="text-sm text-gray-500 mt-1">
            Scrum board'u kullanabilmek için önce bir sprint başlatmalısınız.
          </p>
          <p class="text-xs text-gray-400 mt-1">
            Backlog görünümüne geçerek yeni sprint oluşturabilir ve başlatabilirsiniz.
          </p>
        </div>
        <div v-if="sprints.length > 0" class="pt-2">
          <p class="text-xs text-gray-500 mb-2">Mevcut sprintler:</p>
          <div class="flex flex-wrap justify-center gap-2">
            <span
              v-for="s in sprints"
              :key="s.id"
              class="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs"
              :class="{
                'bg-blue-50 text-blue-700': s.status === 'backlog',
                'bg-green-50 text-green-700': s.status === 'open',
                'bg-gray-100 text-gray-600': s.status === 'done'
              }"
            >
              {{ s.name }}
              <span class="opacity-60">({{ getStatusLabel(s.status) }})</span>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Scrum Board: Aktif sprint(ler) var -->
    <template v-else>
      <!-- ═══ Sprint Sekme Barı ═══ -->
      <div class="bg-white border-b border-gray-200 flex-shrink-0">
        <div class="flex items-center gap-1 px-3 sm:px-4 pt-2 overflow-x-auto no-scrollbar">
          <button
            v-for="sprint in activeSprints"
            :key="sprint.id"
            class="relative flex items-center gap-2 px-3 sm:px-4 py-2.5 text-sm font-medium rounded-t-lg border border-b-0 transition-all whitespace-nowrap shrink-0"
            :class="selectedSprintId === sprint.id
              ? 'bg-white text-blue-700 border-gray-200 shadow-sm z-10 -mb-px'
              : 'bg-gray-50 text-gray-500 border-transparent hover:text-gray-700 hover:bg-gray-100'"
            @click="selectedSprintId = sprint.id"
          >
            <span class="w-2 h-2 rounded-full flex-shrink-0"
              :class="selectedSprintId === sprint.id ? 'bg-blue-500 animate-pulse' : 'bg-green-400'"
            ></span>
            <span class="truncate max-w-[160px]">{{ sprint.name }}</span>
            <span v-if="sprint.startDate" class="text-[10px] text-gray-400 hidden sm:inline">
              {{ formatDate(sprint.startDate) }} — {{ formatDate(sprint.endDate) }}
            </span>
            <span v-if="getRemainingDays(sprint) !== null"
              class="text-[10px] font-semibold px-1.5 py-0.5 rounded-full hidden md:inline"
              :class="getRemainingDays(sprint) <= 2 ? 'text-red-700 bg-red-100' : 'text-gray-600 bg-gray-100'"
            >
              {{ getRemainingDays(sprint) > 0 ? getRemainingDays(sprint) + 'g' : 'Bugün!' }}
            </span>
          </button>
        </div>
      </div>

      <!-- ═══ Seçili Sprint Bilgi Barı ═══ -->
      <div v-if="selectedSprint" class="bg-gradient-to-r from-blue-50 via-indigo-50 to-purple-50 border-b border-blue-200/60 px-3 sm:px-5 py-2.5 sm:py-3 flex-shrink-0">
        <div class="flex items-center justify-between flex-wrap gap-2">
          <div class="flex items-center gap-2 sm:gap-3 flex-wrap">
            <div class="flex items-center gap-2">
              <div class="w-2 h-2 rounded-full bg-blue-500 animate-pulse"></div>
              <span class="text-sm font-bold text-gray-900">{{ selectedSprint.name }}</span>
            </div>
            <span class="text-[11px] text-blue-700 bg-blue-100/80 px-2.5 py-0.5 rounded-full font-semibold border border-blue-200/50">
              Aktif Sprint
            </span>
            <span v-if="selectedSprint.startDate" class="text-xs text-gray-500 bg-white/60 px-2 py-0.5 rounded-md">
              {{ formatDate(selectedSprint.startDate) }} — {{ formatDate(selectedSprint.endDate) }}
            </span>
            <span v-if="selectedRemainingDays !== null" class="text-[11px] font-semibold px-2 py-0.5 rounded-full"
              :class="selectedRemainingDays <= 2 ? 'text-red-700 bg-red-100 border border-red-200' : 'text-gray-600 bg-gray-100 border border-gray-200'"
            >
              {{ selectedRemainingDays > 0 ? selectedRemainingDays + ' gün kaldı' : 'Bugün bitiyor!' }}
            </span>
          </div>
          <div class="flex items-center gap-3 sm:gap-4 flex-wrap">
            <!-- Sprint istatistikleri -->
            <div class="flex items-center gap-3 text-xs">
              <span class="text-gray-600"><span class="font-bold text-gray-900">{{ sprintTasks.length }}</span> görev</span>
              <span class="text-gray-400">•</span>
              <span class="text-green-700"><span class="font-bold">{{ doneCount }}</span> tamamlandı</span>
            </div>
            <!-- Completion yüzdesi -->
            <div class="flex items-center gap-2">
              <div class="w-24 h-2 bg-gray-200/80 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full transition-all duration-500 ease-out"
                  :class="completionPercent === 100 ? 'bg-green-500' : 'bg-blue-500'"
                  :style="{ width: completionPercent + '%' }"
                ></div>
              </div>
              <span class="text-[11px] font-bold" :class="completionPercent === 100 ? 'text-green-700' : 'text-blue-700'">
                %{{ completionPercent }}
              </span>
            </div>
          </div>
        </div>

        <!-- Sprint Hedefi -->
        <div v-if="selectedSprint.goal" class="mt-2 flex items-start gap-2">
          <svg class="w-4 h-4 text-indigo-400 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M13 10V3L4 14h7v7l9-11h-7z"/>
          </svg>
          <p class="text-xs text-indigo-700 italic leading-relaxed">
            <span class="font-semibold not-italic text-indigo-900">Hedef:</span> {{ selectedSprint.goal }}
          </p>
        </div>
      </div>

      <!-- Filtre çubuğu -->
      <FilterBar
        :active-filters="activeFilters"
        @add-filter="addFilter"
        @remove-filter="removeFilter"
        @clear-filters="clearFilters"
        @open-builder="showBuilder = true"
      />

      <!-- Board sütunları -->
      <!-- Swimlane modu (assignee veya priority gruplaması) -->
      <div v-if="groupBy && groupBy !== 'status'" class="flex-1 overflow-auto pb-4 px-2 pt-2">
        <BoardSwimlane
          :columns="columns"
          :tasks="sprintTasks"
          :group-by="groupBy"
          @task-click="openTask"
          @task-drop="handleDrop"
        />
      </div>

      <!-- Normal status-based board -->
      <div v-else class="flex-1 overflow-x-auto pb-4 bg-gradient-to-b from-gray-50/50 to-white">
        <div class="flex gap-3 px-4 pt-4 min-w-max">
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

      <!-- Filter builder modal -->
      <FilterBuilder
        :is-open="showBuilder"
        :initial-filters="activeFilters"
        @close="showBuilder = false"
        @apply="applyBuilderFilters"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BoardColumn   from './BoardColumn.vue'
import BoardSwimlane from './BoardSwimlane.vue'
import FilterBar     from './FilterBar.vue'
import FilterBuilder from './FilterBuilder.vue'
import { getSprints, getTasks, updateTask } from '../../api/WorkApi.js'

const props = defineProps({
  teamId:  { type: String, required: true },
  columns: {
    type: Array,
    default: () => [
      { name: 'To Do',        color: '#6B7280', wipLimit: 0 },
      { name: 'In Progress',  color: '#3B82F6', wipLimit: 3 },
      { name: 'In Review',    color: '#F59E0B', wipLimit: 2 },
      { name: 'Done',         color: '#10B981', wipLimit: 0 },
    ]
  },
  groupBy: { type: String, default: 'status' }
})

const route  = useRoute()
const router = useRouter()

// ─── State ────────────────────────────────────────────────────────────────────
const sprints          = ref([])
const activeSprints    = ref([])
const selectedSprintId = ref(null)
const allTasks         = ref([])
const isLoading        = ref(false)
const showBuilder      = ref(false)
const activeFilters    = ref([])

// ─── Seçili Sprint ────────────────────────────────────────────────────────────
const selectedSprint = computed(() =>
  activeSprints.value.find(s => s.id === selectedSprintId.value) || null
)

// ─── Sprint & Task yükleme ────────────────────────────────────────────────────
async function loadData() {
  isLoading.value = true
  try {
    const [sprintList, taskList] = await Promise.all([
      getSprints(props.teamId).catch(() => []),
      getTasks(props.teamId, false).catch(() => [])
    ])
    sprints.value = sprintList

    // Birden fazla sprint aynı anda aktif olabilir
    activeSprints.value = sprintList.filter(s => s.status === 'open')

    // Eğer önceki seçim hâlâ aktifse koru, değilse; Backlog'dan bir sprint
    // kartına tıklanarak gelindiyse query'deki sprintId'yi seç, yoksa ilk
    // aktif sprint'e düş
    if (!activeSprints.value.find(s => s.id === selectedSprintId.value)) {
      const requested = activeSprints.value.find(s => s.id === route.query.sprintId)
      selectedSprintId.value = requested
        ? requested.id
        : (activeSprints.value.length > 0 ? activeSprints.value[0].id : null)

      // Query'deki sprintId'yi bir kere tükettikten sonra temizle; aksi halde
      // bu görünüme tekrar dönüldüğünde (ör. ViewSwitcher ile) eski seçim
      // URL'de asılı kalıp yeniden dayatılabilir
      if (route.query.sprintId) {
        const { sprintId, ...rest } = route.query
        router.replace({ query: rest })
      }
    }

    allTasks.value = taskList
  } catch (e) {
    console.error('Scrum board yükleme hatası:', e)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadData)
watch(() => props.teamId, loadData)

// ─── Seçili Sprint'e ait görevler ─────────────────────────────────────────────
const sprintTasks = computed(() => {
  if (!selectedSprintId.value) return []
  return allTasks.value.filter(t => t.sprintId === selectedSprintId.value)
})

const doneCount = computed(() =>
  sprintTasks.value.filter(t => t.status === 'Done').length
)

const completionPercent = computed(() => {
  if (sprintTasks.value.length === 0) return 0
  return Math.round((doneCount.value / sprintTasks.value.length) * 100)
})

const selectedRemainingDays = computed(() => {
  return getRemainingDays(selectedSprint.value)
})

// ─── Sütun–görev eşlemesi ─────────────────────────────────────────────────────
const tasksByColumn = computed(() => {
  const map = {}
  for (const col of props.columns) map[col.name] = []

  for (const task of sprintTasks.value) {
    if (map[task.status]) {
      map[task.status].push(task)
    } else {
      // Bilinmeyen status → ilk sütuna
      const first = props.columns[0]
      if (first) (map[first.name] = map[first.name] || []).push(task)
    }
  }
  return map
})

// ─── Sürükle-Bırak ───────────────────────────────────────────────────────────
async function handleDrop({ taskId, toStatus, fromStatus }) {
  const task = allTasks.value.find(t => t.id === taskId)
  if (task) task.status = toStatus
  try {
    await updateTask(props.teamId, taskId, { status: toStatus })
    await loadData()
  } catch (e) {
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

// ─── Yardımcılar ──────────────────────────────────────────────────────────────
function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('tr-TR')
}

function getRemainingDays(sprint) {
  if (!sprint?.endDate) return null
  const end = new Date(sprint.endDate)
  const now = new Date()
  const diff = Math.ceil((end - now) / (1000 * 60 * 60 * 24))
  return Math.max(0, diff)
}

function getStatusLabel(status) {
  const map = { backlog: 'Backlog', open: 'Aktif', done: 'Tamamlandı' }
  return map[status] || status
}
</script>
