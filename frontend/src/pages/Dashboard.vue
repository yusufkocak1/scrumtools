<template>
  <div class="flex flex-row w-full min-h-screen bg-gray-50">
    <div class="flex-1 min-w-0 flex flex-col overflow-hidden">
      <!-- Üst Bar -->
      <div class="bg-white border-b border-gray-200 px-4 sm:px-6 pt-3">
        <div class="flex flex-wrap items-center justify-between gap-2 sm:gap-4">
          <h1 class="text-lg font-semibold text-gray-900">Panolar</h1>

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

            <template v-if="activeDashboard">
              <!-- Widget ekle: yalnız düzenleme yetkisi olanda -->
              <button v-if="canEditActive" @click="openAddWidget"
                      class="flex items-center gap-1.5 text-sm bg-purple-600 hover:bg-purple-700 text-white px-3 py-1.5 rounded-lg transition-colors">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
                Widget Ekle
              </button>

              <!-- Düzen kipi: sürükleme ve boyutlandırma yalnız burada açılır -->
              <button v-if="canEditActive" @click="toggleEditing"
                      class="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-lg border transition-colors"
                      :class="editing
                        ? 'border-purple-300 bg-purple-50 text-purple-700'
                        : 'border-gray-300 text-gray-700 hover:bg-gray-50'">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M4 8V6a2 2 0 012-2h2M4 16v2a2 2 0 002 2h2m8-16h2a2 2 0 012 2v2m-4 12h2a2 2 0 002-2v-2"/>
                </svg>
                {{ editing ? (saving ? 'Kaydediliyor…' : 'Düzenlemeyi bitir') : 'Düzeni düzenle' }}
              </button>

              <!-- Kirli düzen uyarısı + elle kaydet -->
              <button v-if="dirty" @click="persistLayout"
                      :disabled="saving"
                      class="flex items-center gap-1.5 text-sm border border-amber-300 bg-amber-50 text-amber-800 px-3 py-1.5 rounded-lg transition-colors disabled:opacity-50">
                <span class="w-1.5 h-1.5 rounded-full bg-amber-500"></span>
                Kaydedilmemiş değişiklik
              </button>
            </template>
          </div>
        </div>

        <!-- Pano sekmeleri -->
        <div v-if="selectedTeamId" class="mt-2 -mb-px">
          <DashboardTabs
            :dashboards="dashboards"
            :active-id="activeId"
            @select="selectDashboard"
            @create="openCreate"
            @rename="openRename"
            @duplicate="openDuplicate"
            @toggle-visibility="toggleVisibility"
            @delete="confirmDelete"
          />
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

        <div v-else-if="loading" class="flex items-center justify-center h-64 text-sm text-gray-400">
          Panolar yükleniyor…
        </div>

        <div v-else-if="loadError" class="flex flex-col items-center justify-center h-64 gap-3">
          <p class="text-sm text-red-600">{{ loadError }}</p>
          <button class="text-sm text-purple-600 hover:underline" @click="reloadDashboards">Yeniden dene</button>
        </div>

        <template v-else-if="activeDashboard">
          <!-- Düzen kipi şeridi -->
          <div v-if="editing"
               class="mb-3 flex flex-wrap items-center gap-2 rounded-xl border border-purple-200 bg-purple-50 px-3 py-2 text-xs text-purple-800">
            <span class="font-medium">Düzen kipi</span>
            <span class="text-purple-700/80">
              Kartları sürükleyerek sıralayın, sağ alt köşeden boyutlandırın. Bu kipte
              widget'ların içi tıklanmaz — çapraz filtreleme kapalıdır.
            </span>
          </div>

          <!-- Widget Izgarası -->
          <div ref="gridRef" class="dash-grid gap-4 items-start">
            <div
              v-for="(widget, index) in activeWidgets"
              :key="widget.id"
              :ref="el => setCellRef(widget.id, el)"
              class="dash-cell relative group transition-shadow"
              :class="[
                editing ? 'cursor-grab rounded-xl outline-dashed outline-1 outline-offset-2 outline-purple-300' : '',
                dragIndex === index ? 'opacity-40' : '',
                overIndex === index ? 'ring-2 ring-purple-400' : '',
                widget.h ? 'overflow-hidden' : '',
              ]"
              :style="cellStyle(widget)"
              :draggable="editing"
              @dragstart="startDrag(index, $event)"
              @dragend="endDrag"
              @dragover.prevent="dragOver(index)"
              @drop.prevent="drop(index)"
            >
              <!-- Widget işlemleri -->
              <div v-if="canEditActive"
                   class="absolute top-2 right-2 z-10 flex items-center gap-1 transition-opacity"
                   :class="editing ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'">
                <!-- Düzenle: yalnız yapılandırılabilir widget'larda -->
                <button v-if="isConfigurable(widget)"
                        @click.stop="openEditWidget(widget)"
                        title="Widget ayarlarını düzenle"
                        class="p-1 rounded-full bg-white border border-gray-200 shadow-sm hover:bg-purple-50 hover:border-purple-200">
                  <svg class="w-3.5 h-3.5 text-gray-400 hover:text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                  </svg>
                </button>

                <!-- Yüksekliği sıfırla: elle boyutlandırılmışsa -->
                <button v-if="editing && widget.h"
                        @click.stop="resetHeight(index)"
                        title="Doğal yüksekliğe dön"
                        class="p-1 rounded-full bg-white border border-gray-200 shadow-sm hover:bg-gray-50">
                  <svg class="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                  </svg>
                </button>

                <button @click.stop="removeWidget(widget)"
                        title="Widget'ı kaldır"
                        class="p-1 rounded-full bg-white border border-gray-200 shadow-sm hover:bg-red-50 hover:border-red-200">
                  <svg class="w-3.5 h-3.5 text-gray-400 hover:text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                  </svg>
                </button>
              </div>

              <!-- Widget içeriği -->
              <div :class="editing ? 'pointer-events-none select-none h-full' : 'h-full'">
                <component
                  :is="widgetComponent(widget.type)"
                  v-bind="widgetProps(widget)"
                  class="h-full"
                  @task-click="goToTask"
                  @config-change="updateWidgetConfig(widget.id, $event)"
                />
              </div>

              <!-- Boyutlandırma tutamağı -->
              <div
                v-if="editing"
                class="absolute bottom-0 right-0 z-20 w-5 h-5 cursor-nwse-resize flex items-end justify-end p-0.5"
                title="Sürükleyerek boyutlandır"
                @pointerdown="onResizeStart($event, index, widget)"
              >
                <svg class="w-3 h-3 text-purple-400" fill="currentColor" viewBox="0 0 10 10">
                  <path d="M9 1v8H1v-1h7V1h1zM9 5v4H5v-1h3V5h1z"/>
                </svg>
              </div>

              <!-- Genişlik rozeti -->
              <span v-if="editing"
                    class="absolute bottom-1 left-2 z-10 text-[10px] font-medium text-purple-500 bg-white/90 rounded px-1">
                {{ span(widget) }}/12
              </span>
            </div>

            <!-- Boş durum -->
            <div v-if="activeWidgets.length === 0"
                 class="col-span-full flex flex-col items-center justify-center py-16 gap-4 text-gray-400">
              <svg class="w-16 h-16 text-gray-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                      d="M4 5a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM14 5a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1V5zM4 15a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1v-4zM14 15a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z"/>
              </svg>
              <p class="text-sm">
                {{ canEditActive ? 'Henüz widget yok. "Widget Ekle" ile başlayın.' : 'Bu pano boş.' }}
              </p>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- Widget ekle / düzenle -->
    <div v-if="showWidgetModal"
         class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
         @click.self="closeWidgetModal">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
        <!-- 3. adım (ya da düzenlemede tek adım): tipe göre yapılandırma -->
        <RfWidgetConfigModal
          v-if="configDraft"
          :type="configDraft.type"
          :type-label="configDraft.typeLabel"
          :rich-filter="configDraft.richFilter"
          :groupable-fields="groupableFields"
          :summable-fields="summableFields"
          :initial="configDraft.initial"
          :submit-label="configDraft.widgetId ? 'Kaydet' : 'Ekle'"
          @save="applyWidgetConfig"
          @back="backFromConfig"
        />

        <!-- 2. adım: zengin filtre seçimi -->
        <template v-else-if="pendingType">
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

        <!-- 1. adım: widget tipi -->
        <template v-else>
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

        <button v-if="!configDraft" @click="closeWidgetModal"
                class="mt-2 w-full text-sm text-gray-500 hover:text-gray-700">
          İptal
        </button>
      </div>
    </div>

    <!-- Pano oluştur / yeniden adlandır / kopyala -->
    <DashboardFormModal
      v-if="dashboardForm"
      :mode="dashboardForm.mode"
      :initial-name="dashboardForm.name"
      :initial-visibility="dashboardForm.visibility"
      :saving="dashboardForm.saving"
      :error="dashboardForm.error"
      @save="submitDashboardForm"
      @close="dashboardForm = null"
    />
  </div>
</template>

<script setup>
/**
 * Pano ekranı.
 *
 * Bir takımda birden çok pano tutulur; sekme şeridi bunlar arasında geçer.
 * Düzen (sıra, genişlik, yükseklik) panonun JSON'unda saklanır.
 *
 * Kaydetme kuralı ikiye ayrılmıştır ve bu bilinçlidir:
 *  - Widget ekleme / ayar değiştirme / kaldırma kesikli ve onaylı işlemlerdir;
 *    hemen yazılır. Kullanıcı bir modalda "Kaydet"e bastıktan sonra ayrıca
 *    "düzeni de kaydet" demek zorunda kalmamalı.
 *  - Sürükleme ve boyutlandırma sürekli hareketlerdir; her piksel için istek
 *    atmak yerine kirli işaretlenir, "Düzenlemeyi bitir" ya da uyarı düğmesiyle
 *    tek seferde yazılır.
 *
 * Düzen kipi ayrı bir kip olmak zorunda: widget'ların içi tıklanabilir
 * (grafik diliminde daraltma, kuyrukta açma) ve sürükleme aynı tıklamayı
 * yakalasaydı çapraz filtreleme kullanılamaz hâle gelirdi.
 */
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
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
import RfQueueWidget from '../components/dashboard/RfQueueWidget.vue'
import RfRatioWidget from '../components/dashboard/RfRatioWidget.vue'
import RfMultiStatWidget from '../components/dashboard/RfMultiStatWidget.vue'
import RfTimeSeriesWidget from '../components/dashboard/RfTimeSeriesWidget.vue'
import RfHeatmapWidget from '../components/dashboard/RfHeatmapWidget.vue'
import RfWidgetConfigModal from '../components/dashboard/RfWidgetConfigModal.vue'
import DashboardTabs from '../components/dashboard/DashboardTabs.vue'
import DashboardFormModal from '../components/dashboard/DashboardFormModal.vue'
import {
  getDashboards, createDashboard, updateDashboard,
  saveDashboardLayout, duplicateDashboard, deleteDashboard,
} from '../api/DashboardApi.js'
import { getRichFilters, getRichFilter } from '../api/RichFilterApi.js'
import { getQueryFields } from '../api/QueryApi.js'
import { useTeamContext } from '../composables/useTeamContext.js'
import { applySelectionFromQuery, selectionToQuery } from '../composables/useRichFilterContext.js'
import { useDashboardGrid, defaultSpan, clampSpan } from '../composables/useDashboardGrid.js'

const route = useRoute()
const router = useRouter()

// Takım seçimi merkezi context'ten okunur (Ayarlar > Çalışma Alanı)
const { activeTeamId: selectedTeamId, activeTeam, loadTeams } = useTeamContext()

// ─── Pano listesi ─────────────────────────────────────────────────────────
const dashboards = ref([])
const activeId = ref('')
const loading = ref(false)
const loadError = ref('')

/** Aktif panonun çalışma kopyası — kaydedilene kadar sunucudakinden ayrışabilir. */
const activeWidgets = ref([])
const dirty = ref(false)
const saving = ref(false)
const editing = ref(false)

const activeDashboard = computed(() => dashboards.value.find(d => d.id === activeId.value) || null)
const canEditActive = computed(() => !!activeDashboard.value?.canEdit)

// ─── Widget kataloğu ──────────────────────────────────────────────────────

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
  { type: 'RF_MULTI_STAT', label: 'Çoklu Ölçü Kartı', icon: '🧮', richFilter: true },
  { type: 'RF_RESULTS', label: 'Zengin Filtre Listesi', icon: '📋', richFilter: true },
  { type: 'RF_CHART', label: 'Zengin Filtre Grafiği', icon: '🍩', richFilter: true },
  { type: 'RF_QUEUE', label: 'Kuyruk Paneli', icon: '📥', richFilter: true },
  { type: 'RF_RATIO', label: 'Oran Göstergesi', icon: '🎯', richFilter: true },
  { type: 'RF_TIME_SERIES', label: 'Zaman Serisi', icon: '📈', richFilter: true },
  { type: 'RF_HEATMAP', label: 'Isı Haritası', icon: '▩', richFilter: true },
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
  RF_MULTI_STAT: RfMultiStatWidget,
  RF_RESULTS: RfResultsWidget,
  RF_CHART: RfChartWidget,
  RF_QUEUE: RfQueueWidget,
  RF_RATIO: RfRatioWidget,
  RF_TIME_SERIES: RfTimeSeriesWidget,
  RF_HEATMAP: RfHeatmapWidget,
}

function widgetComponent(type) {
  return WIDGET_COMPONENT_MAP[type] || SummaryWidget
}

const isRichFilterWidget = (type) => String(type || '').startsWith('RF_')

/**
 * Düzenlenebilirlik widget'ın ayarı olup olmamasına bakar. Hazır raporlar
 * (Takım Özeti, Burndown…) yalnız takımı bilir; onlarda bir "ayarlar" kutusu
 * açmak boş bir form göstermek olurdu — konumu ve boyutu düzen kipinden ayarlanır.
 */
function isConfigurable(widget) {
  return isRichFilterWidget(widget.type) && !!widget.richFilterId
}

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
    refreshInterval: widget.refreshInterval || 0,
    ...(RF_WIDGET_PROPS[widget.type]?.(widget) || {}),
  }
}

/** Tipe özel widget ayarları — kaydedilen düzen JSON'undan okunur. */
const RF_WIDGET_PROPS = {
  RF_STAT: (w) => ({ threshold: w.threshold || {} }),
  RF_RESULTS: (w) => ({ limit: w.limit || 8 }),
  RF_CHART: (w) => ({
    groupBy: w.groupBy || '',
    metric: w.metric || 'count',
    chart: w.chart || 'donut',
  }),
  RF_QUEUE: (w) => ({
    smartIds: w.smartIds || [],
    hideEmpty: !!w.hideEmpty,
    taskLimit: w.taskLimit || 5,
  }),
  RF_RATIO: (w) => ({
    numeratorId: w.numeratorId || '',
    denominatorId: w.denominatorId || '',
    target: w.target ?? null,
    direction: w.direction || 'higher_better',
  }),
  RF_MULTI_STAT: (w) => ({
    measureIds: w.measureIds || [],
    showTotal: w.showTotal !== false,
    showShare: !!w.showShare,
  }),
  RF_TIME_SERIES: (w) => ({
    elementIds: w.elementIds || [],
    days: w.days || 90,
    interval: w.interval || 'day',
  }),
  RF_HEATMAP: (w) => ({
    groupBy: w.groupBy || '',
    splitBy: w.splitBy || 'priority',
    metric: w.metric || 'count',
  }),
}

/**
 * Widget'ın kendi başlığından değiştirdiği ayarlar (grafik türü gibi).
 * Bunlar kirli işaretlenir; sürükleme/boyutlandırma ile aynı yoldan kaydedilir.
 */
function updateWidgetConfig(widgetId, changes) {
  activeWidgets.value = activeWidgets.value.map(w =>
    w.id === widgetId ? { ...w, ...changes } : w
  )
  markDirty()
}

// ─── Izgara düzeni ────────────────────────────────────────────────────────

const gridRef = ref(null)
const cellRefs = new Map()

function setCellRef(widgetId, el) {
  if (el) cellRefs.set(widgetId, el)
  else cellRefs.delete(widgetId)
}

const {
  dragIndex, overIndex,
  startDrag, dragOver, drop, endDrag, startResize, resetHeight,
} = useDashboardGrid(activeWidgets, markDirty)

const span = (widget) => clampSpan(widget.w, defaultSpan(widget.type))

/**
 * Genişlik CSS değişkeniyle verilir: `grid-column: span var(--w)` yalnız md ve
 * üstünde uygulanır (bkz. scoped style), dar ekranda her kart tam genişlik olur.
 * Sınıf adına gömülü Tailwind span'ları (col-span-4 gibi) kullanılamazdı —
 * değer çalışma anında belli oluyor ve Tailwind sınıfları derleme anında tarar.
 */
function cellStyle(widget) {
  return {
    '--w': span(widget),
    height: widget.h ? `${widget.h}px` : null,
  }
}

function onResizeStart(event, index, widget) {
  startResize(event, index, cellRefs.get(widget.id), gridRef.value)
}

/**
 * Düzenleme yetkisi olmayanda değişiklik yerel kalır: paylaşılan bir panoda
 * grafik türünü çevirmek serbesttir, ama "kaydedilmemiş değişiklik" uyarısı
 * göstermek kullanıcıya asla basamayacağı bir kaydet düğmesi vaat ederdi.
 */
function markDirty() {
  if (!canEditActive.value) return
  dirty.value = true
}

// ─── Yükleme ──────────────────────────────────────────────────────────────

onMounted(() => { loadTeams() })

/**
 * Eskimiş yanıtın yeni takımın listesini ezmesini engelleyen sıra numarası.
 *
 * İzleyiciden ÖNCE tanımlı olmalı: aşağıdaki `immediate` izleyici setup içinde
 * senkron çalışıp `reloadDashboards`'a giriyor, o da ilk satırında bu sayacı
 * artırıyor. Bildirim izleyicinin altında kalırsa değişken TDZ'de olur ve sayfa
 * "Cannot access 'reloadToken' before initialization" ile açılmaz.
 */
let reloadToken = 0

/**
 * Panolar takıma bağlıdır; tek yükleme kaynağı bu izleyicidir.
 *
 * `immediate` ile açılışta da çalışır — ayrıca onMounted'da çağırmak takım
 * localStorage'dan geldiğinde iki eşzamanlı listeleme demekti ve sunucu ilk
 * girişte varsayılan panoyu KURDUĞU için iki kurulum yarışırdı.
 */
watch(selectedTeamId, (teamId, previous) => {
  if (teamId === previous) return
  activeId.value = ''
  reloadDashboards()
}, { immediate: true })

async function reloadDashboards() {
  const token = ++reloadToken
  const teamId = selectedTeamId.value
  if (!teamId) {
    dashboards.value = []
    activeWidgets.value = []
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const list = await getDashboards(teamId)
    if (token !== reloadToken) return
    dashboards.value = list || []

    // Açılacak pano: linkteki (?d=) → önceki seçim → ilk pano
    const wanted = route.query.d || activeId.value
    activeId.value = dashboards.value.find(d => d.id === wanted)?.id
      || dashboards.value[0]?.id
      || ''
    loadWorkingCopy()
  } catch (e) {
    if (token !== reloadToken) return
    console.error('Panolar yüklenemedi', e)
    loadError.value = 'Panolar yüklenemedi.'
  } finally {
    if (token === reloadToken) loading.value = false
  }
}

/** Sunucudaki düzenin kopyası alınır; düzenleme sunucu nesnesini kirletmesin. */
function loadWorkingCopy() {
  const layout = activeDashboard.value?.layout || []
  activeWidgets.value = layout.map(w => ({ ...w }))
  dirty.value = false
  editing.value = false
}

async function selectDashboard(id) {
  if (id === activeId.value) return
  if (!await confirmDiscard()) return
  activeId.value = id
  loadWorkingCopy()
}

/** Kirli düzenle sayfadan/panodan çıkarken sorulur — düzen sessizce kaybolmasın. */
async function confirmDiscard() {
  if (!dirty.value) return true
  return window.confirm('Kaydedilmemiş düzen değişiklikleri var. Yine de devam edilsin mi?')
}

// ─── Kaydetme ─────────────────────────────────────────────────────────────

async function persistLayout() {
  if (!activeDashboard.value?.canEdit) return
  saving.value = true
  try {
    const saved = await saveDashboardLayout(activeId.value, activeWidgets.value)
    replaceDashboard(saved)
    dirty.value = false
  } catch (e) {
    console.error('Düzen kaydedilemedi', e)
    window.alert(errorText(e, 'Düzen kaydedilemedi.'))
  } finally {
    saving.value = false
  }
}

async function toggleEditing() {
  if (editing.value) {
    if (dirty.value) await persistLayout()
    editing.value = false
    return
  }
  editing.value = true
}

function replaceDashboard(saved) {
  if (!saved) return
  dashboards.value = dashboards.value.map(d => (d.id === saved.id ? saved : d))
}

// ─── Widget ekleme / düzenleme ────────────────────────────────────────────

const showWidgetModal = ref(false)
const pendingType = ref(null)
const richFilters = ref([])
/** Yapılandırma adımı: { type, typeLabel, richFilter, initial, widgetId }. */
const configDraft = ref(null)

const smartOf = (rf) => (rf.elements || []).filter(e => e.kind === 'SMART_FILTER')

function openAddWidget() {
  configDraft.value = null
  pendingType.value = null
  showWidgetModal.value = true
}

function closeWidgetModal() {
  showWidgetModal.value = false
  pendingType.value = null
  configDraft.value = null
}

/**
 * Yapılandırma adımından çıkış. Eklemede bir önceki adıma (filtre seçimi)
 * dönülür; düzenlemede geri dönülecek adım olmadığı için modal kapanır.
 */
function backFromConfig() {
  if (configDraft.value?.widgetId) closeWidgetModal()
  else configDraft.value = null
}

async function chooseType(widgetType) {
  if (!widgetType.richFilter) {
    await addWidget(widgetType.type)
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

/** Hazır rapor widget'ı: yapılandırması yok, doğrudan eklenir. */
async function addWidget(type) {
  activeWidgets.value = [...activeWidgets.value, {
    id: `${type}-${Date.now()}`,
    type,
    teamId: selectedTeamId.value,
    w: defaultSpan(type),
  }]
  closeWidgetModal()
  await persistLayout()
}

/**
 * Zengin filtre seçildi → yapılandırma adımı.
 *
 * Her tip yapılandırılır: eksen/eşik/hedef seçilmeden eklenen bir widget,
 * kullanıcının silip yeniden eklemesi gereken bir tahmin olurdu.
 */
async function addRichFilterWidget(richFilter) {
  const type = pendingType.value
  configDraft.value = {
    type,
    typeLabel: labelOf(type),
    richFilter,
    initial: null,
    widgetId: null,
  }
  await loadFieldCatalog()
}

/**
 * Var olan widget'ın ayarlarını açar. Zengin filtre tanımı widget'ta yalnız id
 * olarak duruyor; form akıllı filtre/seri listesine ihtiyaç duyduğu için tanım
 * çekilir.
 */
async function openEditWidget(widget) {
  if (!canEditActive.value || !isConfigurable(widget)) return
  try {
    const richFilter = await getRichFilter(widget.teamId || selectedTeamId.value, widget.richFilterId)
    configDraft.value = {
      type: widget.type,
      typeLabel: labelOf(widget.type),
      richFilter,
      initial: widget,
      widgetId: widget.id,
    }
    pendingType.value = null
    showWidgetModal.value = true
    await loadFieldCatalog()
  } catch (e) {
    console.error('Widget ayarları açılamadı', e)
    window.alert('Widget\'ın bağlı olduğu zengin filtre açılamadı. Filtre silinmiş olabilir.')
  }
}

const labelOf = (type) =>
  richFilterWidgetTypes.find(t => t.type === type)?.label
  || availableWidgetTypes.find(t => t.type === type)?.label
  || 'Widget'

/** Yapılandırma adımının çıktısı: yeni widget eklenir ya da mevcut olan güncellenir. */
async function applyWidgetConfig(config) {
  const draft = configDraft.value
  if (!draft) return

  if (draft.widgetId) {
    activeWidgets.value = activeWidgets.value.map(w =>
      w.id === draft.widgetId ? { ...w, ...config } : w
    )
  } else {
    activeWidgets.value = [...activeWidgets.value, {
      id: `${draft.type}-${Date.now()}`,
      type: draft.type,
      teamId: selectedTeamId.value,
      richFilterId: draft.richFilter.id,
      w: defaultSpan(draft.type),
      ...config,
    }]
  }
  closeWidgetModal()
  await persistLayout()
}

async function removeWidget(widget) {
  if (!canEditActive.value) return
  if (!window.confirm(`"${widget.title || labelOf(widget.type)}" widget'ı kaldırılsın mı?`)) return
  activeWidgets.value = activeWidgets.value.filter(w => w.id !== widget.id)
  await persistLayout()
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

// ─── Pano işlemleri ───────────────────────────────────────────────────────

/** { mode, name, visibility, target, saving, error } */
const dashboardForm = ref(null)

function openCreate() {
  dashboardForm.value = { mode: 'create', name: '', visibility: 'PRIVATE', target: null, saving: false, error: '' }
}

function openRename(dashboard) {
  dashboardForm.value = {
    mode: 'rename',
    name: dashboard.name,
    visibility: dashboard.visibility,
    target: dashboard,
    saving: false,
    error: '',
  }
}

function openDuplicate(dashboard) {
  dashboardForm.value = {
    mode: 'duplicate',
    name: `${dashboard.name} kopyası`,
    visibility: 'PRIVATE',
    target: dashboard,
    saving: false,
    error: '',
  }
}

async function submitDashboardForm({ name, visibility }) {
  const form = dashboardForm.value
  if (!form) return
  form.saving = true
  form.error = ''
  try {
    if (form.mode === 'create') {
      const created = await createDashboard(selectedTeamId.value, { name, visibility, layout: [] })
      dashboards.value = [...dashboards.value, created]
      activeId.value = created.id
      loadWorkingCopy()
    } else if (form.mode === 'rename') {
      const saved = await updateDashboard(form.target.id, { name, visibility })
      replaceDashboard(saved)
    } else {
      const copy = await duplicateDashboard(form.target.id, name)
      dashboards.value = [...dashboards.value, copy]
      activeId.value = copy.id
      loadWorkingCopy()
    }
    dashboardForm.value = null
  } catch (e) {
    form.error = errorText(e, 'Pano kaydedilemedi.')
    form.saving = false
  }
}

async function toggleVisibility(dashboard) {
  const next = dashboard.visibility === 'TEAM' ? 'PRIVATE' : 'TEAM'
  try {
    replaceDashboard(await updateDashboard(dashboard.id, { visibility: next }))
  } catch (e) {
    window.alert(errorText(e, 'Görünürlük değiştirilemedi.'))
  }
}

async function confirmDelete(dashboard) {
  if (!window.confirm(`"${dashboard.name}" panosu silinsin mi? Bu işlem geri alınamaz.`)) return
  try {
    await deleteDashboard(dashboard.id)
    dashboards.value = dashboards.value.filter(d => d.id !== dashboard.id)
    if (activeId.value === dashboard.id) {
      dirty.value = false
      activeId.value = dashboards.value[0]?.id || ''
      // Son pano da silindiyse sunucu bir sonraki listelemede varsayılanı kurar.
      if (activeId.value) loadWorkingCopy()
      else await reloadDashboards()
    }
  } catch (e) {
    window.alert(errorText(e, 'Pano silinemedi.'))
  }
}

/** Sunucu iş kuralı hatalarını (ad çakışması, tavan) olduğu gibi gösterir. */
function errorText(error, fallback) {
  const data = error?.response?.data
  return data?.message || data?.error || fallback
}

function goToTask(task) {
  if (task?.customId || task?.id) router.push(`/task/${task.customId || task.id}`)
}

// ─── Kaybolan değişiklik koruması ─────────────────────────────────────────

onBeforeRouteLeave(async () => await confirmDiscard())

function onBeforeUnload(event) {
  if (!dirty.value) return
  event.preventDefault()
  event.returnValue = ''
}
onMounted(() => window.addEventListener('beforeunload', onBeforeUnload))
onBeforeUnmount(() => window.removeEventListener('beforeunload', onBeforeUnload))

// ─── URL senkronu ─────────────────────────────────────────────────────────
//
// Daraltılmış bir pano olduğu gibi paylaşılabilsin diye hem seçim hem de açık
// pano URL'ye yazılır (K11) — sorgu çubuğunun mevcut `?q=` davranışının devamı.

/** Açılışta linkteki seçimi geri yükle. */
applySelectionFromQuery(route.query)

watch(
  () => [JSON.stringify(selectionToQuery()), activeId.value],
  ([encoded, dashboardId]) => {
    const selection = JSON.parse(encoded)
    const query = { ...route.query }
    delete query.rf
    delete query.rfsel
    delete query.d

    const next = { ...query, ...selection }
    if (dashboardId) next.d = dashboardId

    router.replace({ query: next }).catch(() => {})
  }
)
</script>

<style scoped>
/**
 * Izgara: dar ekranda tek sütun (her kart tam genişlik), md ve üstünde 12
 * sütunluk akış. Kartın kapladığı sütun sayısı `--w` değişkeninden gelir.
 */
.dash-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
}

.dash-cell {
  grid-column: span 12 / span 12;
}

@media (min-width: 768px) {
  .dash-cell {
    grid-column: span var(--w) / span var(--w);
  }
}
</style>
