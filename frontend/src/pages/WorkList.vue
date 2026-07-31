<template>
  <div class="flex flex-row w-full min-h-screen bg-gray-50">
    <!-- Modül sidebar'ı: proje context'i, görünümler ve board kontrolleri -->
    <WorkSidebar
      :open="showMobileSidebar"
      v-model="activeView"
      :team-id="teamId"
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

    <!-- Takım Projeleri Modal -->
    <TeamProjectsModal
      v-if="showTeamProjects"
      :team-id="teamId"
      :organization-id="organizationId"
      :team-projects="projects"
      @close="showTeamProjects = false"
      @changed="handleTeamProjectsChanged"
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
import TeamProjectsModal from '../components/work/TeamProjectsModal.vue'

import { getBoards } from '../api/BoardApi.js'
import { getTeamActivity } from '../api/NotificationApi.js'
import { useProjectContext } from '../composables/useProjectContext.js'
import { useTeamContext } from '../composables/useTeamContext.js'
import { useTaskStatuses } from '../composables/useTaskStatuses.js'

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

// Board sütunları yapılandırılmamışsa iş akışındaki durumlardan türetilir.
const { statuses: workflowStatuses } = useTaskStatuses(() => props.teamId, projectId)

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

// ─── Board seçimi ─────────────────────────────────────────────────────────
// Board'ların yapılandırması (sütunlar, durum eşlemesi, oluşturma/silme)
// /workspace-settings sayfasına taşındı; burada yalnızca görünüm durumu kalır.
const boards          = ref([])
const selectedBoardId = ref(null)
const boardGroupBy    = ref('status')

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

/**
 * Board'un sütunları. Sabit varsayılan liste kaldırıldı: board yoksa ya da
 * sütunları tanımsızsa takımın iş akışındaki durumlardan bir sütun üretilir —
 * böylece kolonlar hiçbir zaman "To Do / In Progress / Done" sanılmaz.
 */
const activeBoardColumns = computed(() => {
  const configured = selectedBoard.value?.columnConfig?.columns
  if (configured?.length) return configured

  return workflowStatuses.value
    .filter(s => !s.isCancellation)
    .map(s => ({
      name: s.name,
      color: s.color || '#6B7280',
      wipLimit: 0,
      statuses: [s.name],
    }))
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

// Proje değişince board seçimi de o projeye taşınır.
watch(projectId, () => {
  ensureBoardInProject()
})

onMounted(async () => {
  adoptTeam(props.teamId)
  await loadProjects()
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
