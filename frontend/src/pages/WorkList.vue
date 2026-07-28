<template>
  <div class="flex flex-row w-full min-h-screen bg-gray-50">
    <!-- Modül sidebar'ı: proje context'i, görünümler ve board kontrolleri -->
    <WorkSidebar
      :open="showMobileSidebar"
      v-model="activeView"
      :projects="projects"
      :project-id="projectId"
      :active-project="activeProject"
      :has-projects="hasProjects"
      :all-projects-value="ALL_PROJECTS"
      :boards="visibleBoards"
      v-model:selected-board-id="selectedBoardId"
      v-model:group-by="boardGroupBy"
      @select-project="selectProject"
      @manage-projects="showTeamProjects = true"
      @create-board="showCreateBoard = true"
      @board-settings="showBoardSettings = true"
      @close="showMobileSidebar = false"
    />

    <div class="flex-1 min-w-0 flex flex-col overflow-hidden">
      <!-- Üst Bar -->
      <div class="bg-white border-b border-gray-200 px-3 sm:px-6 py-2 sm:py-3 flex items-center gap-3">
        <!-- Mobilde sidebar'ı açar; masaüstünde sidebar zaten sabit -->
        <button
          class="lg:hidden shrink-0 p-1.5 -ml-1 rounded-md text-gray-600 hover:bg-gray-100 transition-colors"
          @click="showMobileSidebar = true"
          title="Menü"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
          </svg>
        </button>

        <h1 class="text-lg font-semibold text-gray-900 truncate">
          {{ currentViewLabel }}
        </h1>
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

    <!-- Takım Projeleri Modal -->
    <TeamProjectsModal
      v-if="showTeamProjects"
      :team-id="teamId"
      :organization-id="organizationId"
      :team-projects="projects"
      @close="showTeamProjects = false"
      @changed="handleTeamProjectsChanged"
    />

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

import WorkSidebar    from '../components/work/WorkSidebar.vue'
import BoardView      from '../components/work/BoardView.vue'
import ScrumBoardView from '../components/work/ScrumBoardView.vue'
import ListView       from '../components/work/ListView.vue'
import Backlog        from '../components/work/Backlog.vue'
import ReleasesView   from '../components/work/ReleasesView.vue'
import ActivityFeed   from '../components/ActivityFeed.vue'
import BoardSettings  from '../components/work/BoardSettings.vue'
import TeamProjectsModal from '../components/work/TeamProjectsModal.vue'

import { getBoards, createBoard as apiCreateBoard } from '../api/BoardApi.js'
import { getTeamActivity } from '../api/NotificationApi.js'
import { useProjectContext } from '../composables/useProjectContext.js'
import { useTeamContext } from '../composables/useTeamContext.js'

const props = defineProps({
  teamId: String
})

const route  = useRoute()
const router = useRouter()

// URL'deki takım merkezi context'e adopte edilir: board her zaman URL'deki
// takımı gösterir, context de ona hizalanır (organizasyon dahil) — böylece
// sonraki gezinmeler ve diğer modüller aynı takımda devam eder.
const { adoptTeam } = useTeamContext()

// ─── Aktif proje context'i ────────────────────────────────────────────────────
// Takım birden fazla projede çalışabilir; bu seçim tüm alt görünümlerin kapsamı.
const {
  projects,
  projectId,
  organizationId,
  activeProject,
  hasProjects,
  selectProject,
  loadProjects,
  ALL_PROJECTS,
} = useProjectContext(() => props.teamId)

const showTeamProjects = ref(false)
/** Mobilde sidebar off-canvas açılır; lg ve üstünde her zaman görünür olduğu için yok sayılır. */
const showMobileSidebar = ref(false)

/**
 * Takıma proje eklendi/çıkarıldı. Proje listesi tazelenir; board'lar da yeniden
 * çekilir çünkü görünür board kümesi aktif projeye göre filtreleniyor.
 */
async function handleTeamProjectsChanged() {
  await loadProjects()
  await loadBoards()
}

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
  // Mobilde görünüm seçimi drawer'dan yapılır; seçimden sonra kapanmalı
  showMobileSidebar.value = false
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
  adoptTeam(props.teamId)
  await loadProjects()
  newBoardProjectId.value = projectId.value
  await loadBoards()
})
// teamId route seviyesinde değişebilir; composable proje listesini kendi
// watcher'ıyla tazeliyor, burada yalnızca board'ları yeniliyoruz.
watch(() => props.teamId, (teamId) => {
  adoptTeam(teamId)
  selectedBoardId.value = null
  loadBoards()
})
</script>
