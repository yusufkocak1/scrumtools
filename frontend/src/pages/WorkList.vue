<template>
  <div class="flex flex-row w-full min-h-screen bg-gray-50 pb-20 lg:pb-0">
    <SideBar :team-id="teamId"/>

    <div class="flex-1 min-w-0 flex flex-col overflow-hidden">
      <!-- Üst Bar -->
      <div class="bg-white border-b border-gray-200 px-3 sm:px-6 py-2 sm:py-3 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 sm:gap-4">
        <h1 class="text-lg font-semibold text-gray-900 hidden sm:block shrink-0">
          {{ currentViewLabel }}
        </h1>

        <div class="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-3 w-full sm:w-auto sm:ml-auto min-w-0">
          <!-- Aktif proje context'i: görev/backlog/sürüm görünümlerinin tamamını daraltır.
               Sprintler takım bazlı kaldığı için bu seçim sprintleri gizlemez, içindeki
               görevleri filtreler. -->
          <div v-if="hasProjects" class="flex items-center gap-1.5 shrink-0">
            <span
              v-if="activeProject"
              class="w-2 h-2 rounded-full shrink-0"
              :style="{ backgroundColor: activeProject.color || '#3B82F6' }"
            ></span>
            <select
              :value="projectId ?? ALL_PROJECTS"
              @change="selectProject($event.target.value === ALL_PROJECTS ? null : $event.target.value)"
              class="min-w-0 text-xs font-medium rounded-md border border-gray-300 px-2 py-1.5 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
              title="Aktif proje"
            >
              <option v-for="p in projects" :key="p.id" :value="p.id">
                {{ p.name }} ({{ p.key }})
              </option>
              <option v-if="projects.length > 1" :value="ALL_PROJECTS">Tüm projeler</option>
            </select>
          </div>

          <!-- Görünüm seçici: mobilde tam genişlik, sığmazsa yatay kaydırma -->
          <div class="w-full sm:w-auto overflow-x-auto no-scrollbar">
            <ViewSwitcher v-model="activeView" />
          </div>

          <!-- Board kontrolleri (sadece Board modunda) -->
          <div v-if="activeView === 'board'" class="flex items-center gap-2 sm:gap-3 min-w-0">
            <!-- Board seçici — yalnızca aktif projenin (ve takım geneli) board'ları -->
            <select
              v-if="visibleBoards.length > 1"
              v-model="selectedBoardId"
              class="flex-1 sm:flex-none min-w-0 text-xs rounded-md border border-gray-300 px-2 py-1.5 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
            >
              <option v-for="b in visibleBoards" :key="b.id" :value="b.id">
                {{ b.name }} ({{ b.boardType === 'SCRUM' ? 'Scrum' : 'Kanban' }})
              </option>
            </select>

            <!-- Gruplama seçici -->
            <select
              v-model="boardGroupBy"
              class="flex-1 sm:flex-none min-w-0 text-xs rounded-md border border-gray-300 px-2 py-1.5 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
            >
              <option value="status">Grupla: Status</option>
              <option value="assignee">Grupla: Kişi</option>
            </select>

            <!-- Board yönetim butonu -->
            <button
              class="shrink-0 text-xs text-gray-500 hover:text-gray-700 border border-gray-300 hover:border-gray-400 rounded-md px-2 py-1.5 transition"
              @click="showBoardSettings = true"
              title="Board Yönetimi"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
              </svg>
            </button>

            <!-- Yeni board oluştur -->
            <button
              class="shrink-0 text-xs text-gray-500 hover:text-blue-600 border border-gray-300 hover:border-blue-400 rounded-md px-2 py-1.5 whitespace-nowrap transition"
              @click="showCreateBoard = true"
            >
              + Board
            </button>
          </div>
        </div>
      </div>

      <!-- İçerik alanı -->
      <div class="flex-1 overflow-hidden p-4">
        <!-- Board görünümü — SCRUM vs KANBAN -->
        <template v-if="activeView === 'board'">
          <ScrumBoardView
            v-if="selectedBoard?.boardType === 'SCRUM'"
            :key="boardViewKey"
            :team-id="teamId"
            :project-id="projectId"
            :projects="projects"
            :columns="activeBoardColumns"
            :group-by="boardGroupBy"
          />
          <BoardView
            v-else
            :key="boardViewKey"
            :team-id="teamId"
            :project-id="projectId"
            :columns="activeBoardColumns"
            :group-by="boardGroupBy"
          />
        </template>

        <!-- Liste Görünümü -->
        <ListView
          v-else-if="activeView === 'list'"
          :key="boardViewKey"
          :team-id="teamId"
          :project-id="projectId"
        />

        <!-- Backlog -->
        <Backlog
          v-else-if="activeView === 'backlog'"
          :key="boardViewKey"
          :team-id="teamId"
          :project-id="projectId"
          :projects="projects"
        />

        <!-- Sürümler -->
        <ReleasesView
          v-else-if="activeView === 'releases'"
          :key="boardViewKey"
          :team-id="teamId"
          :project-id="projectId"
        />

        <!-- Aktivite Akışı -->
        <div v-else-if="activeView === 'activity'" class="max-w-2xl mx-auto">
          <ActivityFeed
            :events="activityEvents"
            :loading="activityLoading"
            :has-more="activityHasMore"
            @load-more="loadActivity(false)"
          />
        </div>
      </div>
    </div>

    <!-- Board Oluşturma Modal -->
    <div v-if="showCreateBoard" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="bg-white rounded-xl shadow-2xl w-full max-w-md mx-4 p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Yeni Board Oluştur</h2>
        <div class="space-y-3 mb-5">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Board Adı</label>
            <input
              v-model="newBoardName"
              placeholder="ör: Sprint Board"
              class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Tür</label>
            <select
              v-model="newBoardType"
              class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
            >
              <option value="KANBAN">Kanban</option>
              <option value="SCRUM">Scrum</option>
            </select>
            <p class="text-xs text-gray-400 mt-1">
              {{ newBoardType === 'SCRUM'
                ? 'Scrum board aktif sprint üzerinden çalışır. Sprint başlatıldığında görevler görünür.'
                : 'Kanban board tüm görevleri durumlarına göre sütunlarda gösterir.' }}
            </p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Proje (Opsiyonel)</label>
            <select
              v-model="newBoardProjectId"
              class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
            >
              <option :value="null">— Proje seçilmedi —</option>
              <option v-for="p in projects" :key="p.id" :value="p.id">
                {{ p.name }} ({{ p.key }})
              </option>
            </select>
          </div>
        </div>
        <div class="flex justify-end gap-3">
          <button @click="showCreateBoard = false" class="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">
            İptal
          </button>
          <button
            @click="handleCreateBoard"
            :disabled="!newBoardName.trim()"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            Oluştur
          </button>
        </div>
      </div>
    </div>

    <!-- Board Yönetim Modal -->
    <BoardSettings
      v-if="showBoardSettings"
      :team-id="teamId"
      :boards="boards"
      @close="showBoardSettings = false"
      @updated="handleBoardUpdated"
      @deleted="handleBoardDeleted"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import SideBar        from '../components/SideBar.vue'
import ViewSwitcher   from '../components/work/ViewSwitcher.vue'
import BoardView      from '../components/work/BoardView.vue'
import ScrumBoardView from '../components/work/ScrumBoardView.vue'
import ListView       from '../components/work/ListView.vue'
import Backlog        from '../components/work/Backlog.vue'
import ReleasesView   from '../components/work/ReleasesView.vue'
import ActivityFeed   from '../components/ActivityFeed.vue'
import BoardSettings  from '../components/work/BoardSettings.vue'

import { getBoards, createBoard as apiCreateBoard } from '../api/BoardApi.js'
import { getTeamActivity } from '../api/NotificationApi.js'
import { useProjectContext } from '../composables/useProjectContext.js'

const props = defineProps({
  teamId: String
})

const route  = useRoute()
const router = useRouter()

// ─── Aktif proje context'i ────────────────────────────────────────────────────
// Takım birden fazla projede çalışabilir; bu seçim tüm alt görünümlerin kapsamı.
const {
  projects,
  projectId,
  activeProject,
  hasProjects,
  selectProject,
  loadProjects,
  ALL_PROJECTS,
} = useProjectContext(() => props.teamId)

// ─── Görünüm ──────────────────────────────────────────────────────────────────
const validViews = ['board', 'list', 'backlog', 'releases', 'activity']
const initialView = validViews.includes(route.query.view) ? route.query.view : 'board'
const activeView = ref(initialView)

const currentViewLabel = computed(() => {
  if (activeView.value === 'board') {
    const board = selectedBoard.value
    if (board?.boardType === 'SCRUM') return 'Scrum Board'
    return 'Kanban Board'
  }
  const labels = { list: 'Liste', backlog: 'Backlog', releases: 'Sürümler', activity: 'Aktivite' }
  return labels[activeView.value] || ''
})

// ─── Board yönetimi ───────────────────────────────────────────────────────
const boards           = ref([])
const selectedBoardId  = ref(null)
const showCreateBoard  = ref(false)
const showBoardSettings = ref(false)
const newBoardName     = ref('')
const newBoardType     = ref('KANBAN')
const newBoardProjectId = ref(null)
const boardGroupBy     = ref('status')

/**
 * Aktif projenin board'ları + projeye bağlanmamış (takım geneli) board'lar.
 * "Tüm projeler" seçiliyken hiçbir board gizlenmez.
 */
const visibleBoards = computed(() => {
  if (!projectId.value) return boards.value
  return boards.value.filter(b => !b.projectId || b.projectId === projectId.value)
})

const selectedBoard = computed(() =>
  visibleBoards.value.find(b => b.id === selectedBoardId.value) || visibleBoards.value[0] || null
)

const activeBoardColumns = computed(() => {
  const board = selectedBoard.value
  if (!board?.columnConfig?.columns) {
    // Board tipine göre varsayılan sütunlar
    if (board?.boardType === 'SCRUM') {
      return [
        { name: 'To Do',        color: '#6B7280', wipLimit: 0 },
        { name: 'In Progress',  color: '#3B82F6', wipLimit: 3 },
        { name: 'In Review',    color: '#F59E0B', wipLimit: 2 },
        { name: 'Done',         color: '#10B981', wipLimit: 0 },
      ]
    }
    return [
      { name: 'To Do',       color: '#6B7280', wipLimit: 0 },
      { name: 'In Progress', color: '#3B82F6', wipLimit: 3 },
      { name: 'Done',        color: '#10B981', wipLimit: 0 },
    ]
  }
  return board.columnConfig.columns
})

// Proje de anahtara dahil: proje değişince alt görünümler remount olup veriyi
// yeni context ile baştan çeker (aksi halde eski projenin görevleri ekranda kalıyordu).
const boardViewKey = computed(() =>
  `${selectedBoardId.value || 'default'}:${projectId.value || 'all'}`
)

async function loadBoards() {
  if (!props.teamId) return
  try {
    boards.value = await getBoards(props.teamId)
    ensureBoardInProject()
  } catch (e) {
    // Board henüz yok olabilir — sessizce devam et
  }
}

/**
 * Seçili board aktif projeye ait değilse (proje değiştirildi ya da ilk yükleme)
 * o projenin varsayılan board'una geçilir. Aksi halde kolonlar bir projeye,
 * görevler başka bir projeye ait olurdu.
 */
function ensureBoardInProject() {
  const list = visibleBoards.value
  if (list.length === 0) {
    selectedBoardId.value = null
    return
  }
  if (!list.some(b => b.id === selectedBoardId.value)) {
    selectedBoardId.value = (list.find(b => b.isDefault) || list[0]).id
  }
}

async function handleCreateBoard() {
  if (!newBoardName.value.trim()) return
  try {
    const board = await apiCreateBoard(props.teamId, {
      name:      newBoardName.value.trim(),
      boardType: newBoardType.value,
      projectId: newBoardProjectId.value || undefined,
    })
    boards.value.push(board)
    selectedBoardId.value = board.id
    showCreateBoard.value = false
    newBoardName.value    = ''
    newBoardType.value    = 'KANBAN'
    newBoardProjectId.value = null
  } catch (e) {
    console.error('Board oluşturma hatası:', e)
  }
}

function handleBoardUpdated(updatedBoard) {
  const idx = boards.value.findIndex(b => b.id === updatedBoard.id)
  if (idx >= 0) {
    // Eğer yeni varsayılan yapıldıysa diğerlerini güncelle
    if (updatedBoard.isDefault) {
      boards.value.forEach(b => { b.isDefault = b.id === updatedBoard.id })
    }
    boards.value[idx] = updatedBoard
  }
  loadBoards() // Listeyi tazele
}

function handleBoardDeleted(boardId) {
  boards.value = boards.value.filter(b => b.id !== boardId)
  if (selectedBoardId.value === boardId) {
    const def = boards.value.find(b => b.isDefault) || boards.value[0] || null
    selectedBoardId.value = def?.id || null
  }
}

// ─── Aktivite Akışı ───────────────────────────────────────────────────────────
const activityEvents  = ref([])
const activityLoading = ref(false)
const activityHasMore = ref(false)
let activityPage = 0

async function loadActivity(reset = true) {
  if (!props.teamId) return
  activityLoading.value = true
  if (reset) { activityPage = 0; activityEvents.value = [] }
  try {
    const page = await getTeamActivity(props.teamId, activityPage, 20)
    const items = page.content ?? []
    activityEvents.value = reset ? items : [...activityEvents.value, ...items]
    activityHasMore.value = !page.last
    activityPage++
  } catch (e) {
    console.warn('[WorkList] activity load error:', e)
  } finally {
    activityLoading.value = false
  }
}

watch(activeView, (v) => {
  // URL query'yi güncelle (sayfa yenilemede korunsun)
  router.replace({ query: { ...route.query, view: v } })
  if (v === 'activity') loadActivity(true)
})

// Route aynı component instance'ı farklı takımlar/görünümler için yeniden
// kullanıyor (path: /workList/:teamId, remount olmuyor). Backlog gibi alt
// bileşenler router.push ile query.view'i değiştirdiğinde (ör. bir sprint'e
// tıklayıp board görünümüne geçmek) bu watcher olmadan activeView hiç
// güncellenmiyor, URL değişse de ekranda hiçbir şey olmuyordu.
watch(() => route.query.view, (v) => {
  if (v && validViews.includes(v) && v !== activeView.value) {
    activeView.value = v
  }
})

// Proje değişince board seçimi de o projeye taşınır ve yeni board oluşturma
// formu aktif projeyi ön seçili getirir.
watch(projectId, (v) => {
  ensureBoardInProject()
  newBoardProjectId.value = v
})

onMounted(async () => {
  await loadProjects()
  newBoardProjectId.value = projectId.value
  await loadBoards()
})
// teamId route seviyesinde değişebilir; composable proje listesini kendi
// watcher'ıyla tazeliyor, burada yalnızca board'ları yeniliyoruz.
watch(() => props.teamId, () => {
  selectedBoardId.value = null
  loadBoards()
})
</script>
