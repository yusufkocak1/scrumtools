<template>
  <teleport to="body">
    <div v-if="visible" ref="bar" :style="style"
         class="doc-table-toolbar fixed z-40 bg-white border border-slate-200 rounded-xl shadow-lg shadow-slate-900/5"
         @mousedown.prevent>
      <!-- Ana satır -->
      <div class="flex items-center gap-0.5 px-1.5 py-1 flex-wrap max-w-[min(94vw,44rem)]">
        <span class="text-[10px] font-semibold uppercase tracking-wider text-slate-400 px-1.5 hidden sm:inline">
          Tablo
        </span>

        <!-- Sık kullanılan yapı işlemleri -->
        <button :class="btn" title="Alta satır ekle" @click="run('addRowAfter')">
          <svg class="w-4 h-4" v-html="icons.rowAfter"></svg>
        </button>
        <button :class="btn" title="Sağa sütun ekle" @click="run('addColumnAfter')">
          <svg class="w-4 h-4" v-html="icons.columnAfter"></svg>
        </button>
        <button :class="btn" title="Satırı sil" @click="run('deleteRow')">
          <span class="text-rose-600 text-xs font-medium">−Satır</span>
        </button>
        <button :class="btn" title="Sütunu sil" @click="run('deleteColumn')">
          <span class="text-rose-600 text-xs font-medium">−Sütun</span>
        </button>

        <span :class="divider"></span>

        <!-- Menüler -->
        <button :class="menuBtn('structure')" @click="toggleMenu('structure')">Yapı</button>
        <button :class="menuBtn('appearance')" @click="toggleMenu('appearance')">Görünüm</button>
        <button :class="menuBtn('data')" @click="toggleMenu('data')">Veri</button>

        <span :class="divider"></span>

        <!-- Sıralama: en sık istenen tek işlem, menüye gömülmedi -->
        <button :class="btn" :disabled="!canSort" title="Artan sırala"
                @click="run('sortByColumn', 'asc')">▲</button>
        <button :class="btn" :disabled="!canSort" title="Azalan sırala"
                @click="run('sortByColumn', 'desc')">▼</button>

        <span :class="divider"></span>

        <button :class="btn" title="Tabloyu sil" @click="confirmingDelete = true">
          <svg class="w-4 h-4 text-rose-600" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"/>
          </svg>
        </button>
      </div>

      <!-- Yapı menüsü -->
      <div v-if="menu === 'structure'" class="border-t border-slate-100 px-2 py-2 space-y-2">
        <div class="flex flex-wrap items-center gap-1">
          <span :class="label">Ekle</span>
          <button :class="chip" @click="run('addRowBefore')">↑ Satır</button>
          <button :class="chip" @click="run('addRowAfter')">↓ Satır</button>
          <button :class="chip" @click="run('addColumnBefore')">← Sütun</button>
          <button :class="chip" @click="run('addColumnAfter')">→ Sütun</button>
        </div>
        <div class="flex flex-wrap items-center gap-1">
          <span :class="label">Taşı</span>
          <button :class="chip" :disabled="merged" @click="run('moveTableRow', -1)">↑ Satır</button>
          <button :class="chip" :disabled="merged" @click="run('moveTableRow', 1)">↓ Satır</button>
          <button :class="chip" :disabled="merged" @click="run('moveTableColumn', -1)">← Sütun</button>
          <button :class="chip" :disabled="merged" @click="run('moveTableColumn', 1)">→ Sütun</button>
        </div>
        <div class="flex flex-wrap items-center gap-1">
          <span :class="label">Hücre</span>
          <button :class="chip" @click="run('mergeCells')">⊞ Birleştir</button>
          <button :class="chip" @click="run('splitCell')">⊟ Ayır</button>
          <button :class="chip" @click="run('toggleHeaderRow')">▤ Başlık satırı</button>
          <button :class="chip" @click="run('toggleHeaderColumn')">▥ Başlık sütunu</button>
        </div>
        <p v-if="merged" class="text-[11px] text-amber-700 bg-amber-50 rounded-lg px-2 py-1">
          Bu tabloda birleştirilmiş hücre var — sıralama ve taşıma kapalı, çünkü
          satırları yeniden dizmek birleşmeleri geri döndürülemez biçimde bozar.
        </p>
      </div>

      <!-- Görünüm menüsü -->
      <div v-else-if="menu === 'appearance'" class="border-t border-slate-100 px-2 py-2 space-y-2">
        <div class="flex flex-wrap items-center gap-1">
          <span :class="label">Hizala</span>
          <button v-for="option in ALIGNMENTS" :key="option.key"
                  :class="chipActive(activeAlign === option.key)"
                  :title="option.label"
                  @click="setAlign(option.key)">{{ option.icon }}</button>
          <button :class="chip" title="Varsayılana dön" @click="setAlign(null)">✕</button>
        </div>
        <div class="flex flex-wrap items-center gap-1">
          <span :class="label">Renk</span>
          <button v-for="color in CELL_BACKGROUNDS" :key="color.key ?? 'none'"
                  :title="color.label"
                  class="w-6 h-6 rounded-md border transition"
                  :class="activeBg === color.key ? 'border-indigo-500 ring-2 ring-indigo-200' : 'border-slate-200 hover:border-slate-300'"
                  :style="{ backgroundColor: color.swatch }"
                  @click="run('setDocCellAttribute', 'bg', color.key)">
            <span v-if="!color.key" class="text-[10px] text-slate-400">✕</span>
          </button>
        </div>
        <div class="flex flex-wrap items-center gap-1">
          <span :class="label">Düzen</span>
          <button :class="chipActive(hasFlag('freeze-first'))"
                  title="Geniş tabloda ilk sütun kaydırılırken yerinde kalır"
                  @click="run('toggleTableLayoutFlag', 'freeze-first')">İlk sütunu dondur</button>
          <button :class="chipActive(hasFlag('striped'))"
                  @click="run('toggleTableLayoutFlag', 'striped')">Çizgili</button>
          <button :class="chipActive(hasFlag('compact'))"
                  @click="run('toggleTableLayoutFlag', 'compact')">Sık</button>
        </div>
      </div>

      <!-- Veri menüsü -->
      <div v-else-if="menu === 'data'" class="border-t border-slate-100 px-2 py-2 space-y-2">
        <div class="flex flex-wrap items-center gap-1">
          <span :class="label">Sütun tipi</span>
          <button v-for="type in columnTypeList" :key="type.key"
                  :class="chipActive(activeColumnType === type.key)"
                  @click="run('setColumnType', type.key)">{{ type.label }}</button>
        </div>

        <div v-if="numericColumn" class="flex flex-wrap items-center gap-1">
          <span :class="label">Biçim</span>
          <label class="text-[11px] text-slate-500 flex items-center gap-1">
            Ondalık
            <select :value="formatOptions.decimals ?? ''" @change="setDecimals($event.target.value)"
                    @mousedown.stop
                    class="border border-slate-200 rounded-md text-xs px-1 py-0.5 outline-none focus:border-indigo-300">
              <option value="">otomatik</option>
              <option v-for="n in [0,1,2,3,4]" :key="n" :value="n">{{ n }}</option>
            </select>
          </label>
          <label v-if="activeColumnType === 'currency'" class="text-[11px] text-slate-500 flex items-center gap-1">
            Simge
            <input :value="formatOptions.symbol ?? '₺'" @change="setSymbol($event.target.value)"
                   maxlength="4" @mousedown.stop
                   class="w-12 border border-slate-200 rounded-md text-xs px-1 py-0.5 outline-none focus:border-indigo-300"/>
          </label>
        </div>

        <div class="flex flex-wrap items-center gap-1">
          <span :class="label">Toplam</span>
          <button v-for="aggregation in aggregationList" :key="aggregation.key"
                  :class="chipActive(activeAggregate === aggregation.key)"
                  @click="run('setColumnAggregate', aggregation.key)">{{ aggregation.label }}</button>
          <button :class="chip" @click="run('toggleTotalRow')">
            {{ hasTotalRow ? 'Toplam satırını kaldır' : 'Toplam satırı ekle' }}
          </button>
        </div>

        <div class="flex flex-wrap items-center gap-1">
          <span :class="label">Filtre</span>
          <input v-model="filterQuery" type="text" placeholder="Satırlarda ara…"
                 class="flex-1 min-w-[8rem] border border-slate-200 rounded-lg text-xs px-2 py-1 outline-none focus:border-indigo-300"
                 @mousedown.stop/>
          <button v-if="filterQuery" :class="chip" @click="filterQuery = ''">Temizle</button>
        </div>
        <p v-if="filterQuery" class="text-[11px] text-amber-700 bg-amber-50 rounded-lg px-2 py-1">
          {{ hiddenRowCount }} satır gizli — filtre yalnızca sizin ekranınızda,
          kaydedilen sayfada tüm satırlar duruyor.
        </p>

        <div class="flex flex-wrap items-center gap-1 pt-1 border-t border-slate-100">
          <span :class="label">Aktar</span>
          <button :class="chip" @click="importCsv">CSV içe aktar</button>
          <button :class="chip" @click="exportCsv">CSV dışa aktar</button>
        </div>
      </div>
    </div>

    <input ref="csvInput" type="file" accept=".csv,.tsv,.txt,text/csv" class="hidden"
           @change="onCsvSelected"/>
  </teleport>

  <ConfirmDialog v-if="confirmingDelete"
                 title="Tablo silinsin mi?"
                 message="Tablo ve içindeki tüm veriler kaldırılacak. Ctrl+Z ile geri alabilirsiniz."
                 confirm-text="Sil"
                 @confirm="deleteTable"
                 @cancel="confirmingDelete = false"/>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import {
  ALIGNMENTS,
  CELL_BACKGROUNDS,
  COLUMN_TYPES,
  AGGREGATIONS,
  parseColumnFormat,
  serializeColumnFormat
} from './tableSchema.js'
import {
  findTable,
  selectedColumnIndexes,
  selectedCells,
  splitRows,
  hasMergedCells,
  cellAt,
  tableToGrid
} from './tableUtils.js'
import { filterKey, countFilteredRows } from './tableExtensions.js'
import { parseDelimited, detectDelimiter, gridToCsv } from './tableClipboard.js'
import ConfirmDialog from '../../common/ConfirmDialog.vue'

/**
 * Tablo bağlam araç çubuğu (DOCS_TABLE_PLAN.md Faz 1–2).
 *
 * <b>Neden yüzen, sabit değil:</b> önceki sürümde araç çubuğu editörün üstünde
 * sabit bir şeritti; imleç tabloya girdiğinde beliriyor ve <i>tüm içeriği aşağı
 * itiyordu</i> — kullanıcı yazarken sayfa zıplıyordu. Yüzen çubuk yerleşimi
 * hiç etkilemiyor.
 *
 * <b>`@mousedown.prevent` her düğmede şart:</b> aksi hâlde tıklama editörden
 * odağı alır, ProseMirror hücre seçimini kaybeder ve komut "seçili hücre yok"
 * diye sessizce başarısız olur.
 */
const props = defineProps({
  editor: { type: Object, default: null }
})

const bar = ref(null)
const csvInput = ref(null)
const visible = ref(false)
const style = ref({})
const menu = ref(null)
const filterQuery = ref('')
const confirmingDelete = ref(false)
/** Editör değişimlerinde türetilmiş değerleri yeniden hesaplatan sayaç. */
const revision = ref(0)

const btn = 'h-7 min-w-[1.75rem] px-1.5 inline-flex items-center justify-center rounded-lg text-sm ' +
    'text-slate-600 hover:bg-slate-100 hover:text-slate-800 transition disabled:opacity-40 ' +
    'disabled:cursor-not-allowed'
const divider = 'w-px h-4 bg-slate-200 mx-1'
const label = 'text-[10px] font-semibold uppercase tracking-wider text-slate-400 w-16 shrink-0'
const chip = 'px-2 py-0.5 rounded-lg text-[11px] font-medium text-slate-600 bg-slate-100 ' +
    'hover:bg-slate-200 transition disabled:opacity-40 disabled:cursor-not-allowed'

function chipActive(active) {
  return active
      ? 'px-2 py-0.5 rounded-lg text-[11px] font-medium bg-indigo-600 text-white transition'
      : chip
}

function menuBtn(name) {
  return [
    'px-2 h-7 rounded-lg text-xs font-medium transition',
    menu.value === name ? 'bg-indigo-100 text-indigo-700' : 'text-slate-600 hover:bg-slate-100'
  ]
}

// ─── Türetilmiş tablo durumu ─────────────────────────────────────────────────

/**
 * `revision`'a bilerek bağımlı: ProseMirror durumu Vue'nun reaktif sistemi
 * dışında yaşıyor, dolayısıyla editör her değiştiğinde sayacı artırıp bu
 * computed'ları yeniden koşturuyoruz.
 */
const context = computed(() => {
  revision.value
  const editor = props.editor
  if (!editor || !editor.isActive('table')) return null

  const state = editor.state
  const table = findTable(state.selection.$from)
  if (!table) return null

  const { header, data, total } = splitRows(table)
  const column = selectedColumnIndexes(state)[0] ?? null
  const headerCell = header[0] && column != null ? cellAt(header[0].node, column) : null

  return {
    table,
    headerCell,
    hasTotalRow: total.length > 0,
    dataRowCount: data.length,
    firstCellAttrs: selectedCells(state)[0]?.node?.attrs ?? {},
    merged: hasMergedCells(table.node)
  }
})

const merged = computed(() => context.value?.merged ?? false)
const canSort = computed(() => !!context.value && !merged.value && context.value.dataRowCount > 1)
const hasTotalRow = computed(() => context.value?.hasTotalRow ?? false)
const activeAlign = computed(() => context.value?.firstCellAttrs?.align ?? null)
const activeBg = computed(() => context.value?.firstCellAttrs?.bg ?? null)
const activeColumnType = computed(
    () => context.value?.headerCell?.attrs?.colType ?? context.value?.firstCellAttrs?.colType ?? 'text')
const activeAggregate = computed(() => context.value?.headerCell?.attrs?.colAgg ?? 'none')
const formatOptions = computed(
    () => parseColumnFormat(context.value?.headerCell?.attrs?.colFormat))
const numericColumn = computed(
    () => ['number', 'currency', 'percent'].includes(activeColumnType.value))

const columnTypeList = Object.values(COLUMN_TYPES)
const aggregationList = Object.values(AGGREGATIONS)

const hiddenRowCount = computed(() => {
  revision.value
  if (!props.editor || !filterQuery.value) return 0
  return countFilteredRows(props.editor.state, filterQuery.value)
})

function hasFlag(flag) {
  const layout = context.value?.table?.node?.attrs?.layout || ''
  return layout.split(/\s+/).includes(flag)
}

// ─── Komutlar ────────────────────────────────────────────────────────────────

function run(command, ...args) {
  const chain = props.editor?.chain().focus()
  if (!chain?.[command]) return
  chain[command](...args).run()
}

function setAlign(value) {
  run('setDocCellAttribute', 'align', value)
}

function setDecimals(value) {
  const options = { ...formatOptions.value }
  if (value === '') delete options.decimals
  else options.decimals = Number(value)
  run('setColumnFormat', serializeColumnFormat(options))
}

function setSymbol(value) {
  run('setColumnFormat', serializeColumnFormat({ ...formatOptions.value, symbol: value.trim() || '₺' }))
}

function deleteTable() {
  // Tablo silmek geri alınabilir ama kullanıcı çoğu zaman bunu fark etmiyor;
  // onay, yanlışlıkla silinen bir tablodan çok daha ucuz.
  confirmingDelete.value = false
  run('deleteTable')
}

// ─── CSV ─────────────────────────────────────────────────────────────────────

function importCsv() {
  csvInput.value?.click()
}

async function onCsvSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return

  const text = await file.text()
  const delimiter = detectDelimiter(text)
  if (!delimiter) {
    window.alert('Dosyada tablo ayracı (sekme, ; veya ,) bulunamadı.')
    return
  }
  const grid = parseDelimited(text, delimiter)
  if (!grid.length) return
  props.editor?.chain().focus().insertGrid(grid).run()
}

function exportCsv() {
  const table = context.value?.table
  if (!table) return
  const csv = gridToCsv(tableToGrid(table.node))
  // BOM: Excel UTF-8 CSV'yi BOM olmadan yerel kod sayfasıyla açıyor ve Türkçe
  // karakterler bozuluyor.
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'tablo.csv'
  link.click()
  URL.revokeObjectURL(url)
}

// ─── Filtre ──────────────────────────────────────────────────────────────────

watch(filterQuery, (query) => {
  const editor = props.editor
  if (!editor) return
  editor.view.dispatch(editor.state.tr.setMeta(filterKey, { query }))
})

// ─── Konumlandırma ───────────────────────────────────────────────────────────

/**
 * Çubuk tablonun üst kenarına yapışır; yukarıda yer yoksa altına geçer.
 * Konum `fixed` olduğu için sayfa kaydırıldığında yeniden hesaplanmak zorunda —
 * bu yüzden scroll dinleyicisi `capture` modunda, iç kaydırma kaplarını da
 * yakalasın diye.
 */
function updatePosition() {
  const editor = props.editor
  if (!editor || editor.isDestroyed || !editor.isEditable || !editor.isActive('table')) {
    visible.value = false
    menu.value = null
    return
  }

  const dom = editor.view.domAtPos(editor.state.selection.from)?.node
  const element = dom?.nodeType === 1 ? dom : dom?.parentElement
  const tableElement = element?.closest?.('table')
  if (!tableElement) {
    visible.value = false
    return
  }

  const rect = tableElement.getBoundingClientRect()
  const barHeight = bar.value?.offsetHeight || 40
  const gap = 8
  const above = rect.top - barHeight - gap
  const top = above > 56 ? above : Math.min(rect.bottom + gap, window.innerHeight - barHeight - 8)

  style.value = {
    top: `${Math.max(8, top)}px`,
    left: `${Math.max(8, Math.min(rect.left, window.innerWidth - 32))}px`
  }
  visible.value = true
}

/**
 * Konum hesabı `transaction` başına bir kez değil, kare başına bir kez koşuyor:
 * her tuş vuruşunda `getBoundingClientRect` çağırmak yazma sırasında düzen
 * hesabını (layout) tetikler ve uzun tablolarda hissedilir gecikme yaratır.
 */
let frame = null

function schedulePosition() {
  if (frame) return
  frame = requestAnimationFrame(() => {
    frame = null
    updatePosition()
  })
}

function onEditorUpdate() {
  revision.value++
  schedulePosition()
}

let detach = []

onMounted(() => {
  watch(() => props.editor, (editor) => {
    detach.forEach((off) => off())
    detach = []
    if (!editor) return
    const handlers = ['transaction', 'selectionUpdate', 'focus', 'blur']
    for (const event of handlers) {
      editor.on(event, onEditorUpdate)
      detach.push(() => editor.off(event, onEditorUpdate))
    }
    onEditorUpdate()
  }, { immediate: true })

  window.addEventListener('scroll', schedulePosition, true)
  window.addEventListener('resize', schedulePosition)
})

onBeforeUnmount(() => {
  detach.forEach((off) => off())
  window.removeEventListener('scroll', schedulePosition, true)
  window.removeEventListener('resize', schedulePosition)
  if (frame) cancelAnimationFrame(frame)
  // Filtre dekorasyonla yaşıyor; çubuk kapanırken temizlenmezse satırlar
  // görünmez kalır ve kullanıcı bunu "veri kayboldu" diye okur.
  if (filterQuery.value && props.editor && !props.editor.isDestroyed) {
    props.editor.view.dispatch(props.editor.state.tr.setMeta(filterKey, { query: '' }))
  }
})

const icons = {
  rowAfter: '<path stroke="currentColor" fill="none" stroke-width="1.6" stroke-linecap="round" ' +
      'd="M3 5h18M3 10h18M12 14v6M9 17h6"/>',
  columnAfter: '<path stroke="currentColor" fill="none" stroke-width="1.6" stroke-linecap="round" ' +
      'd="M5 3v18M10 3v18M14 12h6M17 9v6"/>'
}

function toggleMenu(name) {
  menu.value = menu.value === name ? null : name
}
</script>
