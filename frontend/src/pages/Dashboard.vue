<template>
  <div class="flex flex-row w-full min-h-screen bg-gray-50 pb-20 lg:pb-0">
    <SideBar :team-id="selectedTeamId" />

    <div class="flex-1 min-w-0 flex flex-col overflow-hidden">
      <!-- Üst Bar -->
      <div class="bg-white border-b border-gray-200 px-4 sm:px-6 py-3 flex flex-wrap items-center justify-between gap-2 sm:gap-4">
        <h1 class="text-lg font-semibold text-gray-900">Dashboard</h1>

        <div class="flex items-center gap-3 ml-auto">
          <!-- Takım seçici -->
          <select v-model="selectedTeamId" @change="onTeamChange"
                  class="text-sm border border-gray-300 rounded-lg px-3 py-1.5 bg-white focus:outline-none focus:ring-2 focus:ring-purple-400">
            <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.teamName }}</option>
          </select>

          <!-- Widget ekle -->
          <button @click="showAddWidget = true"
                  class="flex items-center gap-1.5 text-sm bg-purple-600 hover:bg-purple-700 text-white px-3 py-1.5 rounded-lg transition-colors">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
            Widget Ekle
          </button>

          <!-- Kaydet -->
          <button @click="saveLayout"
                  :disabled="saving"
                  class="text-sm border border-gray-300 hover:bg-gray-50 text-gray-700 px-3 py-1.5 rounded-lg transition-colors disabled:opacity-50">
            {{ saving ? 'Kaydediliyor…' : 'Düzeni Kaydet' }}
          </button>
        </div>
      </div>

      <!-- İçerik -->
      <div class="flex-1 overflow-y-auto p-4">
        <!-- Takım seçilmedi uyarısı -->
        <div v-if="!selectedTeamId" class="flex flex-col items-center justify-center h-64 gap-4 text-gray-400">
          <svg class="w-16 h-16 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                  d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
          </svg>
          <p class="text-sm">Rapor görüntülemek için bir takım seçin.</p>
        </div>

        <!-- Widget Grid -->
        <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 auto-rows-min">
          <template v-for="widget in activeWidgets" :key="widget.id">
            <!-- Kapat butonu sarmalayıcı -->
            <div class="relative group">
              <!-- Remove button -->
              <button @click="removeWidget(widget.id)"
                      class="absolute top-2 right-2 z-10 opacity-0 group-hover:opacity-100 transition-opacity
                             p-1 rounded-full bg-white border border-gray-200 shadow-sm hover:bg-red-50 hover:border-red-200">
                <svg class="w-3.5 h-3.5 text-gray-400 hover:text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>

              <!-- Widget içeriği -->
              <component
                :is="widgetComponent(widget.type)"
                :team-id="widget.teamId || selectedTeamId"
                class="h-full"
                @task-click="goToTask"
              />
            </div>
          </template>

          <!-- Boş durum -->
          <div v-if="activeWidgets.length === 0"
               class="col-span-full flex flex-col items-center justify-center py-16 gap-4 text-gray-400">
            <svg class="w-16 h-16 text-gray-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                    d="M4 5a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM14 5a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1V5zM4 15a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1v-4zM14 15a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z"/>
            </svg>
            <p class="text-sm">Henüz widget yok. "Widget Ekle" ile başlayın.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Add Widget Modal -->
    <div v-if="showAddWidget"
         class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
         @click.self="showAddWidget = false">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
        <h2 class="text-base font-semibold text-gray-800 mb-4">Widget Ekle</h2>
        <div class="grid grid-cols-2 gap-3">
          <button v-for="wt in availableWidgetTypes" :key="wt.type"
                  @click="addWidget(wt.type)"
                  class="flex flex-col items-center gap-2 p-4 rounded-xl border border-gray-200 hover:border-purple-400 hover:bg-purple-50 text-sm text-gray-700 transition-colors">
            <span class="text-2xl">{{ wt.icon }}</span>
            <span class="font-medium text-xs text-center leading-tight">{{ wt.label }}</span>
          </button>
        </div>
        <button @click="showAddWidget = false"
                class="mt-4 w-full text-sm text-gray-500 hover:text-gray-700">
          İptal
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SideBar from '../components/SideBar.vue'
import SummaryWidget from '../components/dashboard/SummaryWidget.vue'
import BurndownWidget from '../components/dashboard/BurndownWidget.vue'
import VelocityWidget from '../components/dashboard/VelocityWidget.vue'
import WorkloadWidget from '../components/dashboard/WorkloadWidget.vue'
import CreatedVsResolvedWidget from '../components/dashboard/CreatedVsResolvedWidget.vue'
import OverdueWidget from '../components/dashboard/OverdueWidget.vue'
import { getMyTeams } from '../api/TeamApi.js'
import { getDashboardLayout, saveDashboardLayout } from '../api/DashboardApi.js'

const router = useRouter()

const teams = ref([])
const selectedTeamId = ref(localStorage.getItem('selectedTeam') || null)
const activeWidgets = ref([])
const showAddWidget = ref(false)
const saving = ref(false)

const availableWidgetTypes = [
  { type: 'SUMMARY', label: 'Takım Özeti', icon: '📊' },
  { type: 'BURNDOWN', label: 'Sprint Burndown', icon: '📉' },
  { type: 'VELOCITY', label: 'Sprint Velocity', icon: '⚡' },
  { type: 'WORKLOAD', label: 'Üye İş Yükü', icon: '👥' },
  { type: 'CREATED_VS_RESOLVED', label: 'Oluşturulan vs Çözülen', icon: '📈' },
  { type: 'OVERDUE', label: 'Vadesi Geçmiş', icon: '⏰' },
]

const WIDGET_COMPONENT_MAP = {
  SUMMARY: SummaryWidget,
  BURNDOWN: BurndownWidget,
  VELOCITY: VelocityWidget,
  WORKLOAD: WorkloadWidget,
  CREATED_VS_RESOLVED: CreatedVsResolvedWidget,
  OVERDUE: OverdueWidget,
}

function widgetComponent(type) {
  return WIDGET_COMPONENT_MAP[type] || SummaryWidget
}

onMounted(async () => {
  try {
    teams.value = await getMyTeams()
    if (!selectedTeamId.value && teams.value.length > 0) {
      selectedTeamId.value = teams.value[0].id
    }
    // Kayıtlı layout'u yükle
    const savedLayout = await getDashboardLayout()
    if (savedLayout?.length) {
      activeWidgets.value = savedLayout
    } else if (selectedTeamId.value) {
      // İlk açılış: varsayılan widgetları koy
      setDefaultWidgets()
    }
  } catch (e) {
    console.error('Dashboard yüklenemedi', e)
    if (selectedTeamId.value) setDefaultWidgets()
  }
})

function setDefaultWidgets() {
  activeWidgets.value = [
    { id: 'default-summary', type: 'SUMMARY', teamId: selectedTeamId.value },
    { id: 'default-burndown', type: 'BURNDOWN', teamId: selectedTeamId.value },
    { id: 'default-velocity', type: 'VELOCITY', teamId: selectedTeamId.value },
    { id: 'default-overdue', type: 'OVERDUE', teamId: selectedTeamId.value },
  ]
}

function onTeamChange() {
  localStorage.setItem('selectedTeam', selectedTeamId.value)
  // Mevcut widgetların teamId'sini güncelle
  activeWidgets.value = activeWidgets.value.map(w => ({
    ...w,
    teamId: selectedTeamId.value
  }))
}

function addWidget(type) {
  const id = `${type}-${Date.now()}`
  activeWidgets.value.push({ id, type, teamId: selectedTeamId.value })
  showAddWidget.value = false
}

function removeWidget(widgetId) {
  activeWidgets.value = activeWidgets.value.filter(w => w.id !== widgetId)
}

async function saveLayout() {
  saving.value = true
  try {
    await saveDashboardLayout(activeWidgets.value)
  } catch (e) {
    console.error('Layout kaydedilemedi', e)
  } finally {
    saving.value = false
  }
}

function goToTask(task) {
  if (task?.customId || task?.id) router.push(`/task/${task.customId || task.id}`)
}
</script>

