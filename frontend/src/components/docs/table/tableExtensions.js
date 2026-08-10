/**
 * Docs tablosunun zenginleştirilmiş TipTap düğümleri ve komutları
 * (DOCS_TABLE_PLAN.md Faz 1–2).
 *
 * <b>Neden yeni bir düğüm tipi değil, mevcutların genişletilmesi:</b> ayrı bir
 * "veri tablosu" düğümü yazmak, eski sayfalardaki tabloları geride bırakırdı ve
 * iki tablo tipi arasında dönüştürme zorunluluğu doğururdu. Öznitelik
 * genişletmesiyle <b>her eski tablo kendiliğinden yeni yeteneklere sahip</b>
 * oluyor; özniteliksiz tablo ise bugünküyle birebir aynı davranıyor (plan R7).
 *
 * Kalıcı durum HTML özniteliklerinde saklanıyor (plan T3): sürüm geçmişi,
 * izinler, arama ve dışa aktarma bedavaya geliyor.
 */

import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { TableHeader } from '@tiptap/extension-table-header'
import { Extension } from '@tiptap/core'
import { Plugin, PluginKey, TextSelection } from '@tiptap/pm/state'
import { Decoration, DecorationSet } from '@tiptap/pm/view'

import {
  columnType,
  computeAggregate,
  cellRawValue,
  parseColumnFormat,
  AGGREGATIONS
} from './tableSchema.js'

import {
  findTable,
  findCell,
  selectedCells,
  selectedColumnIndexes,
  selectedRowIndexes,
  splitRows,
  hasMergedCells,
  cellAt,
  cellText,
  setCellText
} from './tableUtils.js'

// ─── Öznitelik tanımları ─────────────────────────────────────────────────────

/**
 * `data-*` özniteliğini TipTap öznitelik tanımına çevirir.
 *
 * Anahtar açıkça veriliyor, HTML adından türetilmiyor: `data-v` → `rawValue`
 * gibi eşlemeler türetilemez ve sessizce yanlış anahtara bakan bir `renderHTML`
 * özniteliği hiç yazmaz — fark edilmesi zor bir veri kaybı.
 */
function dataAttribute(key, attributeName, { defaultValue = null, parse = (v) => v } = {}) {
  return {
    default: defaultValue,
    parseHTML: (element) => parse(element.getAttribute(attributeName)),
    renderHTML: (attributes) => {
      const value = attributes[key]
      return value == null || value === '' ? {} : { [attributeName]: String(value) }
    }
  }
}

/** Hem `td` hem `th` üzerinde bulunan ortak öznitelikler. */
function sharedCellAttributes() {
  return {
    align: dataAttribute('align', 'data-align'),
    bg: dataAttribute('bg', 'data-bg'),
    // Sütun tipi başlıkta tanımlı ama her hücrede tekrar ediyor: CSS "başlığı
    // şu olan sütun" diye bir seçici sunmuyor, hizalama ve rozet görünümü ise
    // hücrenin kendisinde uygulanmak zorunda.
    colType: dataAttribute('colType', 'data-col-type'),
    // Ham değer: yalnızca yazılır, okunmaz (bkz. tableSchema.cellRawValue).
    rawValue: dataAttribute('rawValue', 'data-v')
  }
}

export const DocTableCell = TableCell.extend({
  addAttributes() {
    return { ...this.parent?.(), ...sharedCellAttributes() }
  }
})

export const DocTableHeader = TableHeader.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      ...sharedCellAttributes(),
      colFormat: dataAttribute('colFormat', 'data-col-format'),
      colAgg: dataAttribute('colAgg', 'data-col-agg'),
      sorted: dataAttribute('sorted', 'data-sorted')
    }
  }
})

export const DocTableRow = TableRow.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      totalRow: {
        default: false,
        parseHTML: (element) => element.hasAttribute('data-total-row'),
        renderHTML: (attributes) => (attributes.totalRow ? { 'data-total-row': '' } : {})
      }
    }
  }
})

// ─── Tablo düğümü + komutlar ─────────────────────────────────────────────────

export const DocTable = Table.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      sortCol: dataAttribute('sortCol', 'data-sort-col', {
        parse: (value) => (value == null ? null : Number(value))
      }),
      sortDir: dataAttribute('sortDir', 'data-sort-dir'),
      // Boşlukla ayrılmış bayraklar: "freeze-first striped compact"
      layout: dataAttribute('layout', 'data-table-layout')
    }
  },

  /**
   * Tablo düzeyi öznitelikleri editörde de DOM'a yazar.
   *
   * <b>Neden gerekli:</b> prosemirror-tables tabloyu kendi NodeView'ıyla
   * (`TableView`) çiziyor ve o view `<table>` elemanını <i>elle</i> oluşturuyor —
   * düğüm öznitelikleri DOM'a hiç geçmiyor. Sonuç şu olurdu: kullanıcı
   * "Çizgili" ya da "İlk sütunu dondur" diyor, <b>editörde hiçbir şey
   * değişmiyor</b>, ama sayfa kaydedilip okuma moduna geçildiğinde ayar
   * uygulanmış görünüyor. `renderHTML` doğru çalıştığı için kaydedilen HTML
   * zaten doğruydu; eksik olan yalnızca editörün canlı görüntüsüydü.
   *
   * Öznitelikler `view.dom.querySelector('table')` üzerinden yazılıyor,
   * `TableView`'ın iç alan adları üzerinden değil: iç alanlar sürümler arasında
   * değişebilir, DOM yapısı ise sözleşmenin görünen kısmı (R1 dersi).
   */
  addNodeView() {
    const parentNodeView = this.parent?.()
    if (!parentNodeView) return null

    return (props) => {
      const view = parentNodeView(props)

      const syncAttributes = (node) => {
        const table = view.dom?.querySelector?.('table')
        if (!table) return
        for (const [attribute, value] of [
          ['data-table-layout', node.attrs.layout],
          ['data-sort-col', node.attrs.sortCol],
          ['data-sort-dir', node.attrs.sortDir]
        ]) {
          if (value == null || value === '') table.removeAttribute(attribute)
          else table.setAttribute(attribute, String(value))
        }
      }

      syncAttributes(props.node)

      const parentUpdate = view.update?.bind(view)
      view.update = (node, ...rest) => {
        const accepted = parentUpdate ? parentUpdate(node, ...rest) : false
        if (accepted) syncAttributes(node)
        return accepted
      }

      return view
    }
  },

  addKeyboardShortcuts() {
    return {
      ...this.parent?.(),

      /**
       * Tablonun son hücresinde `Enter` yeni satır açar (Excel refleksi).
       *
       * `Tab` bunu zaten yapıyordu ama kullanıcıların çoğu satır sonunda
       * `Enter`'a basıyor ve hücrenin içinde ikinci bir paragraf açıyordu.
       * Koşullar dar tutuldu — yalnızca son hücrenin son paragrafının sonunda —
       * ki hücre içinde bilerek paragraf açmak hâlâ mümkün olsun.
       */
      Enter: () => {
        const { state } = this.editor
        const { selection } = state
        if (!selection.empty) return false

        const table = findTable(selection.$from)
        const cell = findCell(selection.$from)
        if (!table || !cell) return false

        const { data } = splitRows(table)
        const lastRow = data[data.length - 1]
        if (!lastRow || cell.node !== lastRow.node.lastChild) return false

        const paragraph = selection.$from.parent
        const atEnd = selection.$from.parentOffset === paragraph.content.size
        if (!atEnd || cell.node.lastChild !== paragraph || paragraph.content.size === 0) return false

        return this.editor.chain().addRowAfter().goToNextCell(1).run()
      }
    }
  },

  addCommands() {
    return {
      ...this.parent?.(),

      /** Seçili hücrelere bir öznitelik uygular (hizalama, arka plan…). */
      setDocCellAttribute: (name, value) => ({ state, tr, dispatch }) => {
        const cells = selectedCells(state)
        if (!cells.length) return false
        if (dispatch) {
          for (const cell of cells) {
            tr.setNodeMarkup(cell.pos, undefined, { ...cell.node.attrs, [name]: value })
          }
          dispatch(tr)
        }
        return true
      },

      /**
       * Sütun tipini değiştirir ve sütundaki tüm veri hücrelerini yeniden
       * biçimlendirir.
       */
      setColumnType: (typeKey) => ({ state, tr, dispatch }) => {
        const table = findTable(state.selection.$from)
        if (!table) return false
        const columns = selectedColumnIndexes(state)
        if (!columns.length) return false

        if (dispatch) {
          applyToColumns(tr, table, columns, ({ cell, pos, isHeader, headerCell }) => {
            if (isHeader) {
              const attrs = { ...cell.attrs, colType: typeKey }
              // Tip değişince eski toplayıcı anlamsız kalabilir (metin sütununda
              // "toplam" gibi); sayısal olmayana geçerken sıfırlanıyor.
              if (!columnType(typeKey).aggregatable && attrs.colAgg
                  && !['count', 'filled'].includes(attrs.colAgg)) {
                attrs.colAgg = null
              }
              tr.setNodeMarkup(pos, undefined, attrs)
              return
            }
            const format = parseColumnFormat(headerCell?.attrs?.colFormat)
            rewriteCell(tr, state.schema, pos, cell, typeKey, format)
          })
          dispatch(tr)
        }
        return true
      },

      /** Sütun biçim seçeneklerini (ondalık, simge, gruplama) günceller. */
      setColumnFormat: (formatString) => ({ state, tr, dispatch }) => {
        const table = findTable(state.selection.$from)
        if (!table) return false
        const columns = selectedColumnIndexes(state)
        if (!columns.length) return false

        if (dispatch) {
          const format = parseColumnFormat(formatString)
          applyToColumns(tr, table, columns, ({ cell, pos, isHeader, headerCell }) => {
            if (isHeader) {
              tr.setNodeMarkup(pos, undefined, { ...cell.attrs, colFormat: formatString })
              return
            }
            const typeKey = headerCell?.attrs?.colType || cell.attrs.colType || 'text'
            rewriteCell(tr, state.schema, pos, cell, typeKey, format)
          })
          dispatch(tr)
        }
        return true
      },

      /**
       * Sütuna toplayıcı atar. Toplam satırı yoksa <b>aynı işlemde</b> açılır —
       * ayrı bir komutla yapılsaydı geri alma (undo) iki adım sürerdi ve
       * kullanıcı ilk Ctrl+Z'de yarım bir durum görürdü.
       */
      setColumnAggregate: (aggKey) => ({ state, tr, dispatch }) => {
        const table = findTable(state.selection.$from)
        if (!table) return false
        const columns = selectedColumnIndexes(state)
        if (!columns.length) return false

        if (dispatch) {
          applyToColumns(tr, table, columns, ({ cell, pos, isHeader }) => {
            if (!isHeader) return
            tr.setNodeMarkup(pos, undefined, {
              ...cell.attrs,
              colAgg: aggKey === 'none' ? null : aggKey
            })
          })
          if (aggKey !== 'none' && !splitRows(table).total.length) {
            appendTotalRow(tr, state.schema, table)
          }
          dispatch(tr)
        }
        return true
      },

      /** Toplam satırını ekler ya da kaldırır. */
      toggleTotalRow: () => ({ state, tr, dispatch }) => {
        const table = findTable(state.selection.$from)
        if (!table) return false
        const { total } = splitRows(table)

        if (dispatch) {
          if (total.length) {
            // Sondan başa silinir; aksi hâlde ilk silme sonrakinin konumunu kaydırır.
            for (const row of [...total].reverse()) {
              tr.delete(row.pos, row.pos + row.node.nodeSize)
            }
          } else if (!appendTotalRow(tr, state.schema, table)) {
            return false
          }
          dispatch(tr)
        }
        return true
      },

      /**
       * Sütuna göre sıralar. `direction`: 'asc' | 'desc' | null (işareti kaldırır).
       *
       * Sıralama <b>belge sırasını değiştirir</b> (plan T4): kaydedilir ve
       * sayfayı okuyan da aynı sırayı görür. Görüntü sıralaması olsaydı, okuyanın
       * gördüğü sıra ile dışa aktarılan sıra ayrışırdı.
       */
      sortByColumn: (direction) => ({ state, tr, dispatch }) => {
        const table = findTable(state.selection.$from)
        if (!table || hasMergedCells(table.node)) return false

        const column = selectedColumnIndexes(state)[0]
        if (column == null) return false

        const { header, data, total } = splitRows(table)
        if (data.length < 2) return false

        const headerCell = header[0] ? cellAt(header[0].node, column) : null
        const type = columnType(headerCell?.attrs?.colType || 'text')

        const ordered = data
            .map((row, index) => ({
              row,
              index,
              key: type.sortValue(type.parse(cellText(cellAt(row.node, column))))
            }))
            .sort((a, b) => {
              // Boş hücreler yönden bağımsız olarak <b>her zaman sonda</b>:
              // azalan sıralamada boşların başa gelmesi tabloyu okunmaz yapar.
              if (a.key == null && b.key == null) return a.index - b.index
              if (a.key == null) return 1
              if (b.key == null) return -1
              if (a.key === b.key) return a.index - b.index
              const comparison = a.key < b.key ? -1 : 1
              return direction === 'desc' ? -comparison : comparison
            })

        if (dispatch) {
          replaceRows(tr, table, [
            ...header.map((row) => withSortMarker(row.node, column, direction)),
            ...ordered.map((entry) => entry.row.node),
            ...total.map((row) => row.node)
          ])
          tr.setNodeMarkup(table.pos, undefined, {
            ...table.node.attrs,
            sortCol: direction ? column : null,
            sortDir: direction
          })
          dispatch(tr)
        }
        return true
      },

      /** Seçili satırı yukarı (-1) / aşağı (+1) taşır. */
      moveTableRow: (delta) => ({ state, tr, dispatch }) => {
        const table = findTable(state.selection.$from)
        if (!table || hasMergedCells(table.node)) return false

        const { header, data, total } = splitRows(table)
        const mapRow = selectedRowIndexes(state)[0]
        if (mapRow == null) return false

        const from = mapRow - header.length
        const to = from + delta
        if (from < 0 || from >= data.length || to < 0 || to >= data.length) return false

        if (dispatch) {
          const nodes = data.map((row) => row.node)
          const [moved] = nodes.splice(from, 1)
          nodes.splice(to, 0, moved)
          replaceRows(tr, table, [
            ...header.map((row) => row.node),
            ...nodes,
            ...total.map((row) => row.node)
          ])
          dispatch(tr)
        }
        return true
      },

      /** Seçili sütunu sola (-1) / sağa (+1) taşır. */
      moveTableColumn: (delta) => ({ state, tr, dispatch }) => {
        const table = findTable(state.selection.$from)
        if (!table || hasMergedCells(table.node)) return false

        const from = selectedColumnIndexes(state)[0]
        if (from == null) return false
        const to = from + delta

        const width = table.node.firstChild?.childCount ?? 0
        if (from < 0 || from >= width || to < 0 || to >= width) return false

        if (dispatch) {
          const rows = []
          table.node.forEach((row) => {
            const cells = []
            row.forEach((cell) => cells.push(cell))
            const [moved] = cells.splice(from, 1)
            cells.splice(to, 0, moved)
            rows.push(row.type.create(row.attrs, cells, row.marks))
          })
          replaceRows(tr, table, rows)
          dispatch(tr)
        }
        return true
      },

      /** Tablo düzeni bayrağını açar/kapatır: freeze-first | striped | compact. */
      toggleTableLayoutFlag: (flag) => ({ state, tr, dispatch }) => {
        const table = findTable(state.selection.$from)
        if (!table) return false
        const flags = new Set((table.node.attrs.layout || '').split(/\s+/).filter(Boolean))
        if (flags.has(flag)) flags.delete(flag)
        else flags.add(flag)

        if (dispatch) {
          tr.setNodeMarkup(table.pos, undefined, {
            ...table.node.attrs,
            layout: flags.size ? [...flags].join(' ') : null
          })
          dispatch(tr)
        }
        return true
      },

      /** Sütundaki tüm hücreleri mevcut tip/biçime göre yeniden yazar. */
      reformatColumn: () => ({ state, tr, dispatch }) => {
        const table = findTable(state.selection.$from)
        if (!table) return false
        const columns = selectedColumnIndexes(state)
        if (!columns.length) return false

        if (dispatch) {
          applyToColumns(tr, table, columns, ({ cell, pos, isHeader, headerCell }) => {
            if (isHeader) return
            const typeKey = headerCell?.attrs?.colType || cell.attrs.colType || 'text'
            rewriteCell(tr, state.schema, pos, cell, typeKey,
                parseColumnFormat(headerCell?.attrs?.colFormat))
          })
          dispatch(tr)
        }
        return true
      },

      /**
       * Satır/sütun ızgarasını (TSV, CSV) imlecin bulunduğu yere tablo olarak
       * yerleştirir. Tablo içindeyse mevcut tabloyu <b>büyütür</b>.
       */
      insertGrid: (grid, { withHeaderRow = true } = {}) => ({ state, tr, dispatch }) => {
        if (!grid?.length) return false
        const table = findTable(state.selection.$from)
        if (table) return pasteIntoTable({ state, tr, dispatch, table, grid })

        if (dispatch) {
          const { schema } = state
          const rows = grid.map((cells, rowIndex) => {
            const cellType = withHeaderRow && rowIndex === 0
                ? schema.nodes.tableHeader
                : schema.nodes.tableCell
            return schema.nodes.tableRow.create(null, cells.map((text) => cellType.create(
                { colspan: 1, rowspan: 1, colwidth: null },
                schema.nodes.paragraph.create(null, text ? schema.text(text) : null)
            )))
          })
          tr.replaceSelectionWith(schema.nodes.table.create(null, rows))
          dispatch(tr)
        }
        return true
      }
    }
  }
})

// ─── Ortak yürütücüler ───────────────────────────────────────────────────────

/**
 * Verilen sütunlardaki her hücre için geri çağrıyı çalıştırır.
 *
 * <b>Ters sırada yürüyor</b> (tablonun sonundan başına): geri çağrı hücre
 * metnini değiştirebiliyor ve bu, kendisinden <i>sonraki</i> konumları kaydırır.
 * Sondan başa gidildiğinde henüz işlenmemiş konumlar hep değişmemiş bölgede
 * kalıyor ve konum eşlemeye (mapping) hiç ihtiyaç kalmıyor.
 *
 * Birleştirilmiş hücre içeren tabloda hücrenin satır içi sırası ile görsel
 * sütun indeksi ayrışır; tip/biçim komutları bu durumda yanlış sütuna
 * dokunabilir. Sıralama ve taşıma zaten engelli (R3), bunlar ise düzeltilebilir
 * olduğu için serbest bırakıldı.
 */
function applyToColumns(tr, table, columns, callback) {
  const columnSet = new Set(columns)
  const headerRow = splitRows(table).header[0]?.node

  const entries = []
  let rowPos = table.start
  table.node.forEach((row) => {
    let cellPos = rowPos + 1
    row.forEach((cell, _offset, index) => {
      if (columnSet.has(index)) {
        entries.push({
          cell,
          pos: cellPos,
          columnIndex: index,
          isHeader: cell.type.spec.tableRole === 'header_cell',
          headerCell: headerRow ? cellAt(headerRow, index) : null
        })
      }
      cellPos += cell.nodeSize
    })
    rowPos += row.nodeSize
  })

  for (const entry of entries.reverse()) callback(entry)
}

/** Bir veri hücresini sütun tipine göre yeniden yazar ve ham değeri işler. */
function rewriteCell(tr, schema, pos, cell, typeKey, format) {
  const text = cellText(cell)
  const raw = cellRawValue(typeKey, text)
  const formatted = columnType(typeKey).format(raw, format)
  tr.setNodeMarkup(pos, undefined, {
    ...cell.attrs,
    colType: typeKey,
    rawValue: raw == null ? null : String(raw)
  })
  if (formatted !== text) setCellText(tr, pos, cell, formatted, schema)
}

/** Tablonun tüm satırlarını verilen dizilimle değiştirir. */
function replaceRows(tr, table, rows) {
  tr.replaceWith(table.start, table.start + table.node.content.size, rows)
  // Seçim yeniden düzenlenen içeriğin içinde kalırsa beklenmedik bir hücreye
  // düşer; tablonun ilk hücresine alınıyor.
  tr.setSelection(TextSelection.near(tr.doc.resolve(table.start + 2)))
}

/** Tablonun sonuna toplam satırı ekler. */
function appendTotalRow(tr, schema, table) {
  const { header, data } = splitRows(table)
  const template = data[0]?.node || header[0]?.node
  if (!template) return false

  const headerRow = header[0]?.node
  const cells = []
  for (let index = 0; index < template.childCount; index++) {
    const headerCell = headerRow ? cellAt(headerRow, index) : null
    cells.push(schema.nodes.tableCell.create({
      colspan: 1,
      rowspan: 1,
      colwidth: template.child(index)?.attrs?.colwidth ?? null,
      colType: headerCell?.attrs?.colType ?? null
    }, schema.nodes.paragraph.create()))
  }
  tr.insert(table.start + table.node.content.size,
      schema.nodes.tableRow.create({ totalRow: true }, cells))
  return true
}

/** Başlık satırının sıralama okunu günceller (yalnız bir sütunda kalır). */
function withSortMarker(rowNode, column, direction) {
  const cells = []
  rowNode.forEach((cell, _offset, index) => {
    cells.push(cell.type.create(
        { ...cell.attrs, sorted: index === column ? direction : null },
        cell.content,
        cell.marks
    ))
  })
  return rowNode.type.create(rowNode.attrs, cells, rowNode.marks)
}

/**
 * Izgarayı mevcut tablonun içine, imlecin bulunduğu hücreden başlayarak yazar.
 *
 * Eksik satır/sütun varsa tablo <b>büyütülür</b>. Excel'den 30 satır kopyalayıp
 * 3 satırlık tabloya yapıştıran kullanıcının 27 satırı sessizce kaybetmesi,
 * bugünkü davranışın en can sıkıcı yanıydı.
 */
function pasteIntoTable({ state, tr, dispatch, table, grid }) {
  const cell = findCell(state.selection.$from)
  if (!cell) return false

  const { schema } = state
  const { header, data, total } = splitRows(table)
  // Toplam satırı hedef alanın dışında: yapıştırma onun üzerine yazsaydı,
  // hesaplayıcı bir sonraki işlemde değeri geri yazar ve kullanıcı verisinin
  // "kaybolduğunu" görürdü.
  const targets = [...header, ...data].map((row) => row.node)

  let startRow = -1
  let startColumn = -1
  targets.forEach((row, rowIndex) => {
    row.forEach((child, _offset, index) => {
      if (child === cell.node) {
        startRow = rowIndex
        startColumn = index
      }
    })
  })
  if (startRow < 0) return false
  if (!dispatch) return true

  const pastedWidth = Math.max(...grid.map((line) => line.length))
  const width = Math.max(targets[0]?.childCount ?? 0, startColumn + pastedWidth)
  const rowCount = Math.max(targets.length, startRow + grid.length)

  const newRows = []
  for (let rowIndex = 0; rowIndex < rowCount; rowIndex++) {
    const source = targets[rowIndex]
    const isHeaderRow = source
        ? source.firstChild?.type?.spec?.tableRole === 'header_cell'
        : false
    const cellType = isHeaderRow ? schema.nodes.tableHeader : schema.nodes.tableCell
    const cells = []

    for (let column = 0; column < width; column++) {
      const pasted = grid[rowIndex - startRow]?.[column - startColumn]
      const existing = source && column < source.childCount ? source.child(column) : null

      if (pasted != null) {
        // Var olan hücrenin biçim öznitelikleri korunur (sütun tipi, renk),
        // yalnızca içerik ve ham değer değişir.
        const base = existing
        cells.push((base?.type || cellType).create(
            { ...(base?.attrs ?? { colspan: 1, rowspan: 1, colwidth: null }), rawValue: null },
            schema.nodes.paragraph.create(null, pasted ? schema.text(pasted) : null)
        ))
      } else if (existing) {
        cells.push(existing)
      } else {
        cells.push(cellType.create({ colspan: 1, rowspan: 1, colwidth: null },
            schema.nodes.paragraph.create()))
      }
    }
    newRows.push((source?.type ?? schema.nodes.tableRow).create(source?.attrs ?? null, cells))
  }

  replaceRows(tr, table, [...newRows, ...total.map((row) => row.node)])
  dispatch(tr)
  return true
}

// ─── Toplam satırı hesaplayıcısı ─────────────────────────────────────────────

const computeKey = new PluginKey('docTableCompute')

/**
 * Toplam satırlarını belge her değiştiğinde yeniden hesaplar.
 *
 * <b>Neden `appendTransaction`:</b> sonuç belgenin içine yazılıyor, dolayısıyla
 * okuma görünümü hiçbir JS çalıştırmadan doğru toplamı gösteriyor — sayfayı
 * okuyan kullanıcı için sıfır maliyet. Alternatif olan "dekorasyonla göster",
 * kaydedilen HTML'de toplamı olmayan bir tablo bırakırdı.
 *
 * `addToHistory: false`: kullanıcı Ctrl+Z yaptığında yalnızca kendi
 * değişikliğini geri almalı, türetilmiş toplamı değil.
 */
export const DocTableCompute = Extension.create({
  name: 'docTableCompute',

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: computeKey,

        appendTransaction(transactions, oldState, newState) {
          if (!transactions.some((transaction) => transaction.docChanged)) return null
          if (transactions.some((transaction) => transaction.getMeta(computeKey))) return null

          const operations = []
          newState.doc.descendants((node, pos) => {
            if (node.type.spec.tableRole !== 'table') return true
            collectTotals(node, pos, operations)
            return false
          })
          if (!operations.length) return null

          const tr = newState.tr
          // Sondan başa: metin uzunluğu değiştiği için önceki konumlar korunur.
          for (const operation of operations.reverse()) {
            setCellText(tr, operation.pos, operation.cell, operation.text, newState.schema)
          }
          tr.setMeta(computeKey, true)
          tr.setMeta('addToHistory', false)
          return tr
        }
      })
    ]
  }
})

/** Bir tablodaki toplam satırı hücrelerinden güncellenmesi gerekenleri toplar. */
function collectTotals(tableNode, tablePos, operations) {
  const table = { node: tableNode, pos: tablePos, start: tablePos + 1 }
  const { header, data, total } = splitRows(table)
  if (!total.length || !data.length) return

  const headerRow = header[0]?.node
  const totalRow = total[0]

  let anyAggregate = false
  headerRow?.forEach((cell) => {
    if (cell.attrs.colAgg && cell.attrs.colAgg !== 'none') anyAggregate = true
  })

  let cellPos = totalRow.pos + 1
  totalRow.node.forEach((cell, _offset, index) => {
    const headerCell = headerRow ? cellAt(headerRow, index) : null
    const aggKey = headerCell?.attrs?.colAgg || 'none'

    let text = ''
    if (AGGREGATIONS[aggKey] && aggKey !== 'none') {
      text = computeAggregate(
          aggKey,
          headerCell?.attrs?.colType || 'text',
          data.map((row) => cellText(cellAt(row.node, index))),
          parseColumnFormat(headerCell?.attrs?.colFormat)
      )
    } else if (index === 0 && anyAggregate) {
      // İlk sütunda toplayıcı yoksa satırın ne olduğunu söyleyen bir etiket
      // durur; tamamen boş bir satır kullanıcıya "bozuk" gibi görünüyordu.
      text = 'Toplam'
    }

    if (cellText(cell) !== text) operations.push({ pos: cellPos, cell, text })
    cellPos += cell.nodeSize
  })
}

// ─── Hücreden çıkışta biçimlendirme ──────────────────────────────────────────

const normalizeKey = new PluginKey('docTableNormalize')

/**
 * Kullanıcı biçimli bir sütundaki hücreden ayrıldığında değeri normalize eder:
 * "1234.5" → "1.234,50 ₺".
 *
 * <b>Neden yazarken değil çıkarken:</b> her tuş vuruşunda biçimlendirmek imleci
 * hücrenin sonuna atar ve sayının ortasına rakam eklemeyi imkânsız kılardı.
 * Hücreden çıkmak, kullanıcının "bu değer bitti" dediği andır.
 */
export const DocTableNormalize = Extension.create({
  name: 'docTableNormalize',

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: normalizeKey,

        appendTransaction(transactions, oldState, newState) {
          if (transactions.some((transaction) => transaction.getMeta(normalizeKey))) return null

          const previous = findCell(oldState.selection.$from)
          if (!previous) return null

          const current = findCell(newState.selection.$from)
          const mappedPos = transactions.reduce(
              (pos, transaction) => transaction.mapping.map(pos), previous.pos)

          // Hâlâ aynı hücredeyse değer bitmemiştir.
          if (current && current.pos === mappedPos) return null
          if (mappedPos == null || mappedPos >= newState.doc.content.size) return null

          const node = newState.doc.nodeAt(mappedPos)
          if (node?.type?.spec?.tableRole !== 'cell') return null

          const table = findTable(newState.doc.resolve(mappedPos))
          if (!table) return null
          const headerRow = splitRows(table).header[0]?.node
          if (!headerRow) return null

          const columnIndex = cellColumnIndex(table, mappedPos)
          if (columnIndex == null) return null

          const headerCell = cellAt(headerRow, columnIndex)
          const typeKey = headerCell?.attrs?.colType || node.attrs.colType
          // Metin ve etiket sütunlarında normalize edilecek bir şey yok;
          // kullanıcının yazdığına dokunmamak da bir özellik.
          if (!typeKey || typeKey === 'text' || typeKey === 'select') return null

          const format = parseColumnFormat(headerCell?.attrs?.colFormat)
          const text = cellText(node)
          const raw = cellRawValue(typeKey, text)
          const formatted = columnType(typeKey).format(raw, format)
          const rawAttribute = raw == null ? null : String(raw)

          if (formatted === text && node.attrs.rawValue === rawAttribute) return null

          const tr = newState.tr
          tr.setNodeMarkup(mappedPos, undefined, { ...node.attrs, rawValue: rawAttribute })
          if (formatted !== text) setCellText(tr, mappedPos, node, formatted, newState.schema)
          tr.setMeta(normalizeKey, true)
          return tr
        }
      })
    ]
  }
})

/** Hücrenin satır içindeki sırası — birleşme yoksa sütun indeksidir. */
function cellColumnIndex(table, cellPos) {
  let rowPos = table.start
  let found = null
  table.node.forEach((row) => {
    let position = rowPos + 1
    row.forEach((cell, _offset, index) => {
      if (position === cellPos) found = index
      position += cell.nodeSize
    })
    rowPos += row.nodeSize
  })
  return found
}

// ─── Filtre (yalnız görüntü) ─────────────────────────────────────────────────

export const filterKey = new PluginKey('docTableFilter')

/**
 * Satır filtresi — <b>belgeye dokunmaz</b> (plan T4).
 *
 * Kalıcı olsaydı sayfayı okuyan kişi eksik veriyi tam sanardı; bir tabloda
 * yapılabilecek en tehlikeli hata bu. Filtre dekorasyonla uygulanıyor, yalnızca
 * filtreyi kuran kişinin editöründe yaşıyor, ve arayüzde kaç satırın gizlendiğini
 * söyleyen kalıcı bir şerit var.
 */
export const DocTableFilter = Extension.create({
  name: 'docTableFilter',

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: filterKey,

        state: {
          init: () => ({ query: '' }),
          apply(tr, value) {
            const meta = tr.getMeta(filterKey)
            return meta === undefined ? value : { ...value, ...meta }
          }
        },

        props: {
          decorations(state) {
            const query = filterKey.getState(state)?.query
            if (!query) return DecorationSet.empty
            return DecorationSet.create(state.doc, filteredRowDecorations(state.doc, query))
          }
        }
      })
    ]
  }
})

/** Filtreye uymayan veri satırları için dekorasyonlar. */
function filteredRowDecorations(doc, query) {
  const needle = query.toLocaleLowerCase('tr')
  const decorations = []
  doc.descendants((node, pos) => {
    if (node.type.spec.tableRole !== 'table') return true
    for (const row of splitRows({ node, pos, start: pos + 1 }).data) {
      if (!row.node.textContent.toLocaleLowerCase('tr').includes(needle)) {
        decorations.push(Decoration.node(row.pos, row.pos + row.node.nodeSize, {
          class: 'doc-table-filtered'
        }))
      }
    }
    return false
  })
  return decorations
}

/** Arayüzün "kaç satır gizli" şeridi için — dekorasyon sayısı. */
export function countFilteredRows(state, query) {
  if (!query) return 0
  return filteredRowDecorations(state.doc, query).length
}

/** Tüm tablo eklentileri — editörlere tek kalem hâlinde verilir. */
export function docTableExtensions({ resizable = true } = {}) {
  return [
    DocTable.configure({ resizable }),
    DocTableRow,
    DocTableCell,
    DocTableHeader,
    DocTableCompute,
    DocTableNormalize,
    DocTableFilter
  ]
}
