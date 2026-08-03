<template>
  <div class="flex flex-row w-full min-h-screen bg-gray-50">
    <div class="flex-1 min-w-0 flex flex-col overflow-hidden">
      <!-- Üst Bar -->
      <div class="bg-white border-b border-gray-200 px-4 sm:px-6 py-3 flex flex-wrap items-center justify-between gap-2 sm:gap-4">
        <h1 class="text-lg font-semibold text-gray-900">Dashboard</h1>

        <div class="flex items-center gap-3 ml-auto">
          <!-- Aktif takım — merkezi context'ten (Ayarlar > Çalışma Alanı) -->
          <router-link
            to="/settings"
            class="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border border-gray-200 bg-white text-sm text-gray-700 hover:border-purple-300 hover:text-purple-700 transition"
            title="Aktif takımı Ayarlar'dan değiştir"
          >
            <span class="w-2 h-2 rounded-full bg-green-500"></span>
            {{ activeTeam?.teamName || 'Takım seç' }}
            <span class="text-xs text-gray-400">Değiştir</span>
          </router-link>

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
                v-bind="widgetProps(widget)"
                class="h-full"
                @task-click="goToTask"
                @config-change="updateWidgetConfig(widget.id, $event)"
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
         @click.self="closeAddWidget">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
        <!-- 1. adım: widget tipi -->
        <template v-if="!pendingType">
          <h2 class="text-base font-semibold text-gray-800 mb-4">Widget Ekle</h2>

          <p class="text-[11px] font-semibold tracking-wider text-gray-400 uppercase mb-2">Hazır raporlar</p>
          <div class="grid grid-cols-2 gap-3">
            <button v-for="wt in availableWidgetTypes" :key="wt.type"
                    @click="chooseType(wt)"
                    class="flex flex-col items-center gap-2 p-4 rounded-xl border border-gray-200 hover:border-purple-400 hover:bg-purple-50 text-sm text-gray-700 transition-colors">
              <span class="text-2xl">{{ wt.icon }}</span>
              <span class="font-medium text-xs text-center leading-tight">{{ wt.label }}</span>
            </button>
          </div>

          <p class="text-[11px] font-semibold tracking-wider text-gray-400 uppercase mt-5 mb-2">
            Zengin filtre widget'ları
          </p>
          <div class="grid grid-cols-2 gap-3">
            <button v-for="wt in richFilterWidgetTypes" :key="wt.type"
                    @click="chooseType(wt)"
                    class="flex flex-col items-center gap-2 p-4 rounded-xl border border-gray-200 hover:border-purple-400 hover:bg-purple-50 text-sm text-gray-700 transition-colors">
              <span class="text-2xl">{{ wt.icon }}</span>
              <span class="font-medium text-xs text-center leading-tight">{{ wt.label }}</span>
            </button>
          </div>
        </template>

        <!-- 3. adım: grafik yapılandırması -->
        <template v-else-if="chartDraft">
          <h2 class="text-base font-semibold text-gray-800">Grafiği yapılandır</h2>
          <p class="text-xs text-gray-500 mt-1 mb-4">
            {{ chartDraft.richFilterName }} — hangi eksende, hangi ölçüyle?
          </p>

          <div class="space-y-4">
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1.5">Grup ekseni</label>
              <select v-model="chartDraft.groupBy"
                      class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
                <option value="">Akıllı filtreler (varsayılan)</option>
                <option v-for="field in groupableFields" :key="field.name" :value="field.name">
                  {{ field.label }}
                </option>
              </select>
              <p class="mt-1 text-[11px] text-gray-400">
                Akıllı filtre ekseninde dilime tıklamak panodaki tüm widget'ları daraltır.
              </p>
            </div>

            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1.5">Ölçü</label>
              <select v-model="chartDraft.metric"
                      class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
                <option value="count">Görev sayısı</option>
                <option v-for="field in summableFields" :key="field.name" :value="field.name">
                  {{ field.label }} toplamı
                </option>
              </select>
            </div>

            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1.5">Görünüm</label>
              <div class="flex gap-2">
                <button v-for="option in CHART_TYPES" :key="option.value"
                        class="flex-1 py-2 rounded-lg border text-sm transition-colors"
                        :class="chartDraft.chart === option.value
                          ? 'border-purple-400 bg-purple-50 text-purple-700'
                          : 'border-gray-200 text-gray-600 hover:border-gray-300'"
                        @click="chartDraft.chart = option.value">
                  {{ option.icon }} {{ option.label }}
                </button>
              </div>
            </div>
          </div>

          <button class="mt-5 w-full text-sm bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors"
                  @click="addChartWidget">
            Ekle
          </button>
          <button @click="chartDraft = null" class="mt-2 w-full text-sm text-gray-500 hover:text-gray-700">
            Geri
          </button>
        </template>

        <!-- 2. adım: zengin filtre seçimi -->
        <template v-else>
          <h2 class="text-base font-semibold text-gray-800">Zengin filtre seç</h2>
          <p class="text-xs text-gray-500 mt-1 mb-4">
            Widget bu filtreye bağlanır; aynı filtreye bağlı widget'lar seçimleri paylaşır.
          </p>

          <div v-if="richFilters.length" class="space-y-2 max-h-72 overflow-y-auto">
            <button v-for="rf in richFilters" :key="rf.id"
                    @click="addRichFilterWidget(rf)"
                    class="w-full flex items-center gap-3 p-3 rounded-xl border border-gray-200 hover:border-purple-400 hover:bg-purple-50 text-left transition-colors">
              <div class="flex items-center gap-1 shrink-0">
                <span v-for="element in smartOf(rf).slice(0, 4)" :key="element.id"
                      class="w-3 h-3 rounded"
                      :style="{ backgroundColor: element.color || '#94A3B8' }"></span>
              </div>
              <div class="min-w-0">
                <p class="text-sm text-gray-800 truncate">{{ rf.name }}</p>
                <p class="text-[11px] text-gray-400">{{ smartOf(rf).length }} akıllı filtre</p>
              </div>
            </button>
          </div>

          <div v-else class="py-8 text-center">
            <p class="text-sm text-gray-500">Bu takımda zengin filtre yok.</p>
            <router-link to="/rich-filters" class="text-xs text-purple-600 hover:underline">
              Zengin filtre oluştur
            </router-link>
          </div>

          <button @click="pendingType = null" class="mt-4 w-full text-sm text-gray-500 hover:text-gray-700">
            Geri
          </button>
        </template>

        <button @click="closeAddWidget"
                class="mt-2 w-full text-sm text-gray-500 hover:text-gray-700">
          İptal
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SummaryWidget from '../components/dashboard/SummaryWidget.vue'
import BurndownWidget from '../components/dashboard/BurndownWidget.vue'
import VelocityWidget from '../components/dashboard/VelocityWidget.vue'
import WorkloadWidget from '../components/dashboard/WorkloadWidget.vue'
import CreatedVsResolvedWidget from '../components/dashboard/CreatedVsResolvedWidget.vue'
import OverdueWidget from '../components/dashboard/OverdueWidget.vue'
import RfControllerWidget from '../components/dashboard/RfControllerWidget.vue'
import RfStatWidget from '../components/dashboard/RfStatWidget.vue'
import RfResultsWidget from '../components/dashboard/RfResultsWidget.vue'
import RfChartWidget from '../components/dashboard/RfChartWidget.vue'
import { getDashboardLayout, saveDashboardLayout } from '../api/DashboardApi.js'
import { getRichFilters } from '../api/RichFilterApi.js'
import { getQueryFields } from '../api/QueryApi.js'
import { useTeamContext } from '../composables/useTeamContext.js'
import { applySelectionFromQuery, selectionToQuery } from '../composables/useRichFilterContext.js'

const route = useRoute()
const router = useRouter()

// Takım seçimi merkezi context'ten okunur (Ayarlar > Çalışma Alanı)
const { activeTeamId: selectedTeamId, activeTeam, loadTeams } = useTeamContext()

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

/**
 * Zengin filtreye bağlanan widget'lar. Bunlar bir `richFilterId` taşır ve aynı
 * filtreye bağlı olanlar seçim durumunu paylaşır — birinde daraltma yapmak
 * diğerlerini de daraltır (bkz. RICH_FILTER_PLAN.md — K10).
 */
const richFilterWidgetTypes = [
  { type: 'RF_CONTROLLER', label: 'Filtre Kontrolcüsü', icon: '🎛', richFilter: true },
  { type: 'RF_STAT', label: 'Zengin Filtre Sayacı', icon: '🔢', richFilter: true },
  { type: 'RF_RESULTS', label: 'Zengin Filtre Listesi', icon: '📋', richFilter: true },
  { type: 'RF_CHART', label: 'Zengin Filtre Grafiği', icon: '🍩', richFilter: true, configurable: true },
]

const CHART_TYPES = [
  { value: 'donut', label: 'Halka', icon: '◍' },
  { value: 'pie', label: 'Pasta', icon: '◕' },
  { value: 'bar', label: 'Çubuk', icon: '▥' },
]

const WIDGET_COMPONENT_MAP = {
  SUMMARY: SummaryWidget,
  BURNDOWN: BurndownWidget,
  VELOCITY: VelocityWidget,
  WORKLOAD: WorkloadWidget,
  CREATED_VS_RESOLVED: CreatedVsResolvedWidget,
  OVERDUE: OverdueWidget,
  RF_CONTROLLER: RfControllerWidget,
  RF_STAT: RfStatWidget,
  RF_RESULTS: RfResultsWidget,
  RF_CHART: RfChartWidget,
}

function widgetComponent(type) {
  return WIDGET_COMPONENT_MAP[type] || SummaryWidget
}

const isRichFilterWidget = (type) => String(type || '').startsWith('RF_')

/**
 * Widget'a geçilecek özellikler. Hazır rapor widget'ları yalnız takımı bilir;
 * zengin filtre widget'ları ayrıca bağlı oldukları filtreyi ve kendi ayarlarını alır.
 */
function widgetProps(widget) {
  const teamId = widget.teamId || selectedTeamId.value
  if (!isRichFilterWidget(widget.type)) return { teamId }

  return {
    teamId,
    richFilterId: widget.richFilterId,
    title: widget.title || '',
    ...(widget.type === 'RF_STAT' ? { threshold: widget.threshold || {} } : {}),
    ...(widget.type === 'RF_RESULTS' ? { limit: widget.limit || 8 } : {}),
    ...(widget.type === 'RF_CHART' ? {
      groupBy: widget.groupBy || '',
      metric: widget.metric || 'count',
      chart: widget.chart || 'donut',
    } : {}),
  }
}

/**
 * Widget'ın kendi başlığından değiştirdiği ayarlar (grafik türü gibi).
 * Düzen kaydedilene kadar yereldedir — "Düzeni Kaydet" ile kalıcı olur.
 */
function updateWidgetConfig(widgetId, changes) {
  activeWidgets.value = activeWidgets.value.map(w =>
    w.id === widgetId ? { ...w, ...changes } : w
  )
}

onMounted(async () => {
  try {
    await loadTeams()
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

// Aktif takım (Ayarlar'dan) değişince mevcut widget'lar yeni takıma taşınır
watch(selectedTeamId, (teamId) => {
  if (!teamId) return
  if (activeWidgets.value.length === 0) {
    setDefaultWidgets()
    return
  }
  activeWidgets.value = activeWidgets.value.map(w => ({
    ...w,
    teamId
  }))
})

// ─── Widget ekleme ────────────────────────────────────────────────────────

/** Zengin filtre widget'ı seçildiğinde ikinci adımda hangi tip bekliyor. */
const pendingType = ref(null)
const richFilters = ref([])
/** Grafik widget'ının üçüncü adımdaki taslağı (eksen, ölçü, görünüm). */
const chartDraft = ref(null)

const smartOf = (rf) => (rf.elements || []).filter(e => e.kind === 'SMART_FILTER')

async function chooseType(widgetType) {
  if (!widgetType.richFilter) {
    addWidget(widgetType.type)
    return
  }
  pendingType.value = widgetType.type
  try {
    richFilters.value = await getRichFilters(selectedTeamId.value)
  } catch (e) {
    console.error('Zengin filtreler yüklenemedi', e)
    richFilters.value = []
  }
}

function addWidget(type) {
  const id = `${type}-${Date.now()}`
  activeWidgets.value.push({ id, type, teamId: selectedTeamId.value })
  closeAddWidget()
}

async function addRichFilterWidget(richFilter) {
  const type = pendingType.value

  // Grafik bir adım daha ister: eksen ve ölçü seçilmeden eklenen grafik,
  // kullanıcının silip yeniden eklemesi gereken bir tahmin olurdu.
  if (type === 'RF_CHART') {
    chartDraft.value = {
      richFilterId: richFilter.id,
      richFilterName: richFilter.name,
      groupBy: '',
      metric: 'count',
      chart: 'donut',
    }
    await loadFieldCatalog()
    return
  }

  activeWidgets.value.push({
    id: `${type}-${Date.now()}`,
    type,
    teamId: selectedTeamId.value,
    richFilterId: richFilter.id,
    title: richFilter.name,
  })
  closeAddWidget()
}

function addChartWidget() {
  const draft = chartDraft.value
  activeWidgets.value.push({
    id: `RF_CHART-${Date.now()}`,
    type: 'RF_CHART',
    teamId: selectedTeamId.value,
    richFilterId: draft.richFilterId,
    title: draft.richFilterName,
    groupBy: draft.groupBy,
    metric: draft.metric,
    chart: draft.chart,
  })
  closeAddWidget()
}

// ─── Alan kataloğu (grafik ekseni ve ölçüsü) ──────────────────────────────

const groupableFields = ref([])
const summableFields = ref([])

/** Katalog bir kez çekilir; alan listesi sorgu diliyle aynı kaynaktan gelir. */
async function loadFieldCatalog() {
  if (groupableFields.value.length || !selectedTeamId.value) return
  try {
    const catalog = await getQueryFields(selectedTeamId.value)
    groupableFields.value = (catalog.fields || []).filter(f => f.groupable)
    summableFields.value = (catalog.fields || []).filter(f => f.summable)
  } catch (e) {
    console.error('Alan kataloğu alınamadı', e)
  }
}

function closeAddWidget() {
  showAddWidget.value = false
  pendingType.value = null
  chartDraft.value = null
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

// ─── Zengin filtre seçiminin URL senkronu ─────────────────────────────────
//
// Daraltılmış bir dashboard olduğu gibi paylaşılabilsin diye seçim URL'ye
// yazılır (K11) — sorgu çubuğunun mevcut `?q=` davranışının devamı.

/** Açılışta linkteki seçimi geri yükle. */
applySelectionFromQuery(route.query)

watch(() => JSON.stringify(selectionToQuery()), (encoded) => {
  const selection = JSON.parse(encoded)
  const query = { ...route.query }
  delete query.rf
  delete query.smart
  delete query.rfq

  router.replace({ query: { ...query, ...selection } }).catch(() => {})
})
</script>

