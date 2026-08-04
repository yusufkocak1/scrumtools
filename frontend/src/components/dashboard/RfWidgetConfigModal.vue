<template>
  <div>
    <h2 class="text-base font-semibold text-gray-800">{{ typeLabel }} — yapılandır</h2>
    <p class="text-xs text-gray-500 mt-1 mb-4">{{ richFilter?.name }}</p>

    <div class="space-y-4 max-h-[60vh] overflow-y-auto pr-1">
      <!-- Başlık: her widget tipinde -->
      <div>
        <label class="block text-xs font-medium text-gray-600 mb-1.5">Başlık</label>
        <input
          v-model="draft.title"
          type="text"
          maxlength="80"
          :placeholder="richFilter?.name"
          class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-500/20"
        />
      </div>

      <!-- ── Grafik ───────────────────────────────────────────────────── -->
      <template v-if="type === 'RF_CHART'">
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Grup ekseni</label>
          <select v-model="draft.groupBy" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
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
          <select v-model="draft.metric" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
            <option value="count">Görev sayısı</option>
            <option v-for="field in summableFields" :key="field.name" :value="field.name">
              {{ field.label }} toplamı
            </option>
          </select>
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Görünüm</label>
          <div class="flex gap-2">
            <button
              v-for="option in CHART_TYPES"
              :key="option.value"
              class="flex-1 py-2 rounded-lg border text-sm transition-colors"
              :class="draft.chart === option.value
                ? 'border-purple-400 bg-purple-50 text-purple-700'
                : 'border-gray-200 text-gray-600 hover:border-gray-300'"
              @click="draft.chart = option.value"
            >
              {{ option.icon }} {{ option.label }}
            </button>
          </div>
        </div>
      </template>

      <!-- ── Sayaç eşikleri ───────────────────────────────────────────── -->
      <template v-else-if="type === 'RF_STAT'">
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Uyarı eşiği</label>
            <input v-model.number="draft.warn" type="number" min="0" placeholder="—"
                   class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Kritik eşik</label>
            <input v-model.number="draft.danger" type="number" min="0" placeholder="—"
                   class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400" />
          </div>
        </div>
        <p class="text-[11px] text-gray-400 -mt-2">
          Sayı eşiği aştığında kart sarıya, kritik eşikte kırmızıya döner. Boş bırakılırsa renk değişmez.
        </p>
      </template>

      <!-- ── Liste ────────────────────────────────────────────────────── -->
      <template v-else-if="type === 'RF_RESULTS'">
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Gösterilecek görev</label>
          <input v-model.number="draft.limit" type="number" min="3" max="25"
                 class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400" />
        </div>
      </template>

      <!-- ── Kuyruk ───────────────────────────────────────────────────── -->
      <template v-else-if="type === 'RF_QUEUE'">
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Kuyruklar</label>
          <p v-if="!smartFilters.length" class="text-[11px] text-amber-600">
            Bu zengin filtrede akıllı filtre yok; kuyruk paneli boş kalır.
          </p>
          <div v-else class="space-y-1 max-h-40 overflow-y-auto">
            <label v-for="element in smartFilters" :key="element.id"
                   class="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-gray-50 cursor-pointer">
              <input type="checkbox" :value="element.id" v-model="draft.smartIds" class="accent-purple-600" />
              <span class="w-2.5 h-2.5 rounded-sm" :style="{ backgroundColor: element.color || '#94A3B8' }"></span>
              <span class="text-xs text-gray-700 truncate">{{ element.name }}</span>
            </label>
          </div>
          <p class="mt-1 text-[11px] text-gray-400">
            Hiçbiri seçilmezse hepsi gösterilir — sonradan eklenen akıllı filtreler de kendiliğinden görünür.
          </p>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Açılan listedeki görev</label>
            <input v-model.number="draft.taskLimit" type="number" min="3" max="20"
                   class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400" />
          </div>
          <label class="flex items-end gap-2 pb-2 cursor-pointer">
            <input type="checkbox" v-model="draft.hideEmpty" class="accent-purple-600" />
            <span class="text-xs text-gray-600">Boş kuyrukları gizle</span>
          </label>
        </div>
      </template>

      <!-- ── Oran ─────────────────────────────────────────────────────── -->
      <template v-else-if="type === 'RF_RATIO'">
        <p v-if="!smartFilters.length" class="text-[11px] text-amber-600">
          Oran, akıllı filtreler üzerinden hesaplanır. Önce en az bir akıllı filtre tanımlayın.
        </p>

        <template v-else>
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Pay</label>
            <select v-model="draft.numeratorId" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
              <option value="" disabled>Akıllı filtre seçin…</option>
              <option v-for="element in smartFilters" :key="element.id" :value="element.id">
                {{ element.name }}
              </option>
            </select>
          </div>

          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Payda</label>
            <select v-model="draft.denominatorId" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
              <option value="">Tüm sonuçlar</option>
              <option v-for="element in smartFilters" :key="element.id" :value="element.id"
                      :disabled="element.id === draft.numeratorId">
                {{ element.name }}
              </option>
            </select>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1.5">Hedef (%)</label>
              <input v-model.number="draft.targetPercent" type="number" min="0" max="100" placeholder="—"
                     class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400" />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1.5">Yön</label>
              <select v-model="draft.direction" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
                <option value="higher_better">Yüksek olması iyi</option>
                <option value="lower_better">Düşük olması iyi</option>
              </select>
            </div>
          </div>
          <p class="text-[11px] text-gray-400 -mt-2">
            Hedef boş bırakılırsa gösterge renk değiştirmez, yalnız oranı yazar.
          </p>
        </template>
      </template>

      <!-- ── Çoklu ölçü ───────────────────────────────────────────────── -->
      <template v-else-if="type === 'RF_MULTI_STAT'">
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Ölçüler</label>
          <p v-if="!smartFilters.length" class="text-[11px] text-amber-600">
            Bu zengin filtrede akıllı filtre yok; kartta yalnız toplam görünür.
          </p>
          <div v-else class="space-y-1 max-h-40 overflow-y-auto">
            <label v-for="element in smartFilters" :key="element.id"
                   class="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-gray-50 cursor-pointer">
              <input type="checkbox" :value="element.id" v-model="draft.measureIds" class="accent-purple-600" />
              <span class="w-2.5 h-2.5 rounded-sm" :style="{ backgroundColor: element.color || '#94A3B8' }"></span>
              <span class="text-xs text-gray-700 truncate">{{ element.name }}</span>
            </label>
          </div>
        </div>

        <div class="flex items-center gap-4">
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="draft.showTotal" class="accent-purple-600" />
            <span class="text-xs text-gray-600">Toplam kutusu</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="draft.showShare" class="accent-purple-600" />
            <span class="text-xs text-gray-600">Yüzde payı</span>
          </label>
        </div>
      </template>

      <!-- ── Isı haritası ─────────────────────────────────────────────── -->
      <template v-else-if="type === 'RF_HEATMAP'">
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Satır ekseni</label>
          <select v-model="draft.groupBy" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
            <option value="">Akıllı filtreler (varsayılan)</option>
            <option v-for="field in groupableFields" :key="field.name" :value="field.name">
              {{ field.label }}
            </option>
          </select>
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Sütun ekseni</label>
          <select v-model="draft.splitBy" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
            <option v-for="field in groupableFields" :key="field.name" :value="field.name">
              {{ field.label }}
            </option>
          </select>
          <p class="mt-1 text-[11px] text-gray-400">
            Kesişim tek eksende görünmeyeni gösterir: "kimin üzerinde kaç kritik iş var".
          </p>
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Ölçü</label>
          <select v-model="draft.metric" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
            <option value="count">Görev sayısı</option>
            <option v-for="field in summableFields" :key="field.name" :value="field.name">
              {{ field.label }} toplamı
            </option>
          </select>
        </div>
      </template>

      <!-- ── Zaman serisi ─────────────────────────────────────────────── -->
      <template v-else-if="type === 'RF_TIME_SERIES'">
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Seriler</label>
          <p v-if="!timeSeries.length" class="text-[11px] text-amber-600">
            Bu zengin filtrede zaman serisi tanımlı değil. Önce editörde seri tanımlayın;
            geçmiş orada kurgulanır.
          </p>
          <div v-else class="space-y-1 max-h-40 overflow-y-auto">
            <label v-for="element in timeSeries" :key="element.id"
                   class="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-gray-50 cursor-pointer">
              <input type="checkbox" :value="element.id" v-model="draft.elementIds" class="accent-purple-600" />
              <span class="w-2.5 h-2.5 rounded-sm" :style="{ backgroundColor: element.color || '#94A3B8' }"></span>
              <span class="text-xs text-gray-700 truncate">{{ element.name }}</span>
            </label>
          </div>
          <p class="mt-1 text-[11px] text-gray-400">
            Hiçbiri seçilmezse hepsi çizilir.
          </p>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Pencere</label>
            <select v-model.number="draft.days" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
              <option :value="30">Son 30 gün</option>
              <option :value="90">Son 90 gün</option>
              <option :value="180">Son 180 gün</option>
              <option :value="365">Son 1 yıl</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Aralık</label>
            <select v-model="draft.interval" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
              <option value="day">Günlük</option>
              <option value="week">Haftalık</option>
            </select>
          </div>
        </div>
        <p class="text-[11px] text-gray-400 -mt-2">
          Haftalık gösterimde haftanın son değeri alınır; günlük sayılar toplanmaz —
          seri o an açık olan görev sayısıdır, bir akış değil.
        </p>
      </template>

      <!-- Tazeleme: bütün zengin filtre widget'larında ortak -->
      <div>
        <label class="block text-xs font-medium text-gray-600 mb-1.5">Kendiliğinden tazele</label>
        <select v-model.number="draft.refreshInterval" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
          <option v-for="option in REFRESH_OPTIONS" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
        <p class="mt-1 text-[11px] text-gray-400">
          Sekme arka plandayken tazeleme durur, geri dönüldüğünde bir kez çalışır.
        </p>
      </div>
    </div>

    <button
      class="mt-5 w-full text-sm bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white px-4 py-2 rounded-lg transition-colors"
      :disabled="!canSave"
      @click="submit"
    >
      {{ submitLabel }}
    </button>
    <button class="mt-2 w-full text-sm text-gray-500 hover:text-gray-700" @click="emit('back')">
      {{ initial ? 'İptal' : 'Geri' }}
    </button>
  </div>
</template>

<script setup>
/**
 * Zengin filtre widget'larının ortak yapılandırma adımı.
 *
 * Faz 4'e kadar yalnız grafik yapılandırılıyordu ve bu, Dashboard'un içine
 * gömülü bir adımdı. Kuyruk/oran/çoklu ölçü gelince her tip için ayrı bir adım
 * yazmak yerine tek panel: ortak alanlar (başlık, tazeleme) bir kez, tipe özel
 * alanlar bir dalda tanımlanır.
 *
 * Çıktı düz bir nesnedir; dashboard düzeni JSONB'de bu alanlarla saklanır
 * (bkz. RICH_FILTER_PLAN.md — §7).
 *
 * Aynı panel var olan bir widget'ı düzenlemek için de kullanılır: `initial`
 * verildiğinde taslak o widget'tan doldurulur. Ayrı bir düzenleme paneli yazmak,
 * iki formun zamanla ayrışması demekti — yeni bir ayar eklendiğinde biri
 * güncellenip diğeri unutulur, kullanıcı ayarı yalnız eklerken görürdü.
 */
import { ref, computed } from 'vue'
import { REFRESH_OPTIONS } from '../../composables/useAutoRefresh.js'

const props = defineProps({
  type: { type: String, required: true },
  typeLabel: { type: String, default: 'Widget' },
  richFilter: { type: Object, required: true },
  groupableFields: { type: Array, default: () => [] },
  summableFields: { type: Array, default: () => [] },
  /** Düzenleme kipinde mevcut widget ayarları; ekleme kipinde null. */
  initial: { type: Object, default: null },
  submitLabel: { type: String, default: 'Ekle' },
})

const emit = defineEmits(['save', 'back'])

const CHART_TYPES = [
  { value: 'donut', label: 'Halka', icon: '◍' },
  { value: 'pie', label: 'Pasta', icon: '◕' },
  { value: 'bar', label: 'Çubuk', icon: '▥' },
]

const smartFilters = computed(() =>
  (props.richFilter?.elements || []).filter(e => e.kind === 'SMART_FILTER')
)

const timeSeries = computed(() =>
  (props.richFilter?.elements || []).filter(e => e.kind === 'TIME_SERIES')
)

/**
 * Taslak: önce her tip için makul varsayılanlar, sonra düzenleme kipinde
 * mevcut widget'ın değerleri. Kaydedilen düzen yalnız tipin kullandığı alanları
 * taşıdığı için (bkz. submit), eksik alanlar varsayılanlarıyla kalır.
 */
function initialDraft() {
  const base = {
    title: props.richFilter?.name || '',
    refreshInterval: 0,
    // grafik / ısı haritası
    groupBy: '',
    splitBy: 'priority',
    metric: 'count',
    chart: 'donut',
    // sayaç
    warn: null,
    danger: null,
    // liste
    limit: 8,
    // kuyruk
    smartIds: [],
    hideEmpty: false,
    taskLimit: 5,
    // oran
    numeratorId: smartFilters.value[0]?.id || '',
    denominatorId: '',
    targetPercent: 80,
    direction: 'higher_better',
    // çoklu ölçü
    measureIds: [],
    showTotal: true,
    showShare: false,
    // zaman serisi
    elementIds: [],
    days: 90,
    interval: 'day',
  }

  const w = props.initial
  if (!w) return base

  // Depolanan biçim ile form biçimi iki yerde ayrışır: eşikler bir nesne
  // içinde, hedef 0–1 arası oran olarak saklanır. Dönüşüm burada tersine çevrilir.
  return {
    ...base,
    title: w.title ?? base.title,
    refreshInterval: Number(w.refreshInterval) || 0,
    groupBy: w.groupBy ?? base.groupBy,
    splitBy: w.splitBy ?? base.splitBy,
    metric: w.metric ?? base.metric,
    chart: w.chart ?? base.chart,
    warn: Number.isFinite(w.threshold?.warn) ? w.threshold.warn : null,
    danger: Number.isFinite(w.threshold?.danger) ? w.threshold.danger : null,
    limit: w.limit ?? base.limit,
    smartIds: [...(w.smartIds || [])],
    hideEmpty: !!w.hideEmpty,
    taskLimit: w.taskLimit ?? base.taskLimit,
    numeratorId: w.numeratorId || base.numeratorId,
    denominatorId: w.denominatorId || '',
    targetPercent: Number.isFinite(w.target) ? Math.round(w.target * 100) : null,
    direction: w.direction ?? base.direction,
    measureIds: [...(w.measureIds || [])],
    showTotal: w.showTotal !== false,
    showShare: !!w.showShare,
    elementIds: [...(w.elementIds || [])],
    days: w.days ?? base.days,
    interval: w.interval ?? base.interval,
  }
}

const draft = ref(initialDraft())

const canSave = computed(() => props.type !== 'RF_RATIO' || !!draft.value.numeratorId)

/** Yalnız tipin kullandığı alanlar kaydedilir; düzen JSON'u gereksiz alan taşımasın. */
function submit() {
  if (!canSave.value) return
  const d = draft.value
  const common = {
    title: d.title.trim() || props.richFilter?.name || '',
    refreshInterval: Number(d.refreshInterval) || 0,
  }

  const perType = {
    RF_CHART: () => ({ groupBy: d.groupBy, metric: d.metric, chart: d.chart }),
    RF_STAT: () => ({ threshold: threshold(d) }),
    RF_RESULTS: () => ({ limit: clamp(d.limit, 3, 25, 8) }),
    RF_QUEUE: () => ({
      smartIds: [...d.smartIds],
      hideEmpty: !!d.hideEmpty,
      taskLimit: clamp(d.taskLimit, 3, 20, 5),
    }),
    RF_RATIO: () => ({
      numeratorId: d.numeratorId,
      denominatorId: d.denominatorId || '',
      target: percentToRatio(d.targetPercent),
      direction: d.direction,
    }),
    RF_MULTI_STAT: () => ({
      measureIds: [...d.measureIds],
      showTotal: !!d.showTotal,
      showShare: !!d.showShare,
    }),
    RF_HEATMAP: () => ({
      groupBy: d.groupBy,
      splitBy: d.splitBy || 'priority',
      metric: d.metric,
    }),
    RF_TIME_SERIES: () => ({
      elementIds: [...d.elementIds],
      days: clamp(d.days, 7, 365, 90),
      interval: d.interval === 'week' ? 'week' : 'day',
    }),
  }

  emit('save', { ...common, ...(perType[props.type]?.() || {}) })
}

/** Boş bırakılan eşik "0" değil "yok" demektir; alan hiç yazılmaz. */
function threshold(d) {
  const out = {}
  if (Number.isFinite(d.warn)) out.warn = d.warn
  if (Number.isFinite(d.danger)) out.danger = d.danger
  return out
}

function percentToRatio(percent) {
  if (!Number.isFinite(percent)) return null
  return clamp(percent, 0, 100, 0) / 100
}

function clamp(value, min, max, fallback) {
  const number = Number(value)
  if (!Number.isFinite(number)) return fallback
  return Math.min(Math.max(number, min), max)
}
</script>
