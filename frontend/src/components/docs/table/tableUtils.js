/**
 * Tablo konum yardımcıları (DOCS_TABLE_PLAN.md Faz 1–2).
 *
 * prosemirror-tables'ın kendi yardımcılarının çoğu dışa aktarılmıyor ya da
 * sürümler arası değişiyor; burada yalnızca <b>belgelenmiş iki API</b>ye
 * yaslanıyoruz: `TableMap` ve `CellSelection`. Gerisi düz ProseMirror konum
 * aritmetiği — bu, `IMPROVEMENT_ANALYSIS` §2'nin "doğrulanmamış iç sözleşmeye
 * yaslanma" uyarısına verilen cevap.
 */

import { TableMap, CellSelection } from '@tiptap/pm/tables'

/** İmlecin içinde bulunduğu tabloyu bulur. */
export function findTable($pos) {
  for (let depth = $pos.depth; depth > 0; depth--) {
    const node = $pos.node(depth)
    if (node.type.spec.tableRole === 'table') {
      return { node, pos: $pos.before(depth), start: $pos.start(depth), depth }
    }
  }
  return null
}

/** İmlecin içinde bulunduğu hücreyi bulur (başlık hücresi de olabilir). */
export function findCell($pos) {
  for (let depth = $pos.depth; depth > 0; depth--) {
    const node = $pos.node(depth)
    const role = node.type.spec.tableRole
    if (role === 'cell' || role === 'header_cell') {
      return { node, pos: $pos.before(depth), start: $pos.start(depth), depth }
    }
  }
  return null
}

/**
 * Seçili hücreler. Tek imleçte tek hücre, `CellSelection`'da hepsi.
 * Konumlar belge (mutlak) konumudur.
 */
export function selectedCells(state) {
  const { selection } = state
  if (selection instanceof CellSelection) {
    const cells = []
    selection.forEachCell((node, pos) => cells.push({ node, pos }))
    return cells
  }
  const cell = findCell(selection.$from)
  return cell ? [{ node: cell.node, pos: cell.pos }] : []
}

/** Seçimin dokunduğu sütun indeksleri (soldan sağa, tekrarsız). */
export function selectedColumnIndexes(state) {
  const table = findTable(state.selection.$from)
  if (!table) return []
  const map = TableMap.get(table.node)
  const columns = new Set()
  for (const cell of selectedCells(state)) {
    const rect = safeFindCell(map, cell.pos - table.start)
    if (!rect) continue
    for (let column = rect.left; column < rect.right; column++) columns.add(column)
  }
  return [...columns].sort((a, b) => a - b)
}

/** Seçimin dokunduğu satır indeksleri. */
export function selectedRowIndexes(state) {
  const table = findTable(state.selection.$from)
  if (!table) return []
  const map = TableMap.get(table.node)
  const rows = new Set()
  for (const cell of selectedCells(state)) {
    const rect = safeFindCell(map, cell.pos - table.start)
    if (!rect) continue
    for (let row = rect.top; row < rect.bottom; row++) rows.add(row)
  }
  return [...rows].sort((a, b) => a - b)
}

/**
 * `TableMap.findCell` bilinmeyen konumda <b>fırlatır</b>, `null` dönmez.
 * Seçim tablo dışına taştığında (örneğin iki tabloyu kapsayan bir seçimde) bu
 * hata editörü kilitlerdi; sarmalanıyor.
 */
function safeFindCell(map, relativePos) {
  try {
    return map.findCell(relativePos)
  } catch {
    return null
  }
}

/** Tablonun satır düğümleri, belge konumlarıyla. */
export function tableRows(table) {
  const rows = []
  let pos = table.start
  table.node.forEach((row) => {
    rows.push({ node: row, pos })
    pos += row.nodeSize
  })
  return rows
}

/** Satırın tamamı başlık hücresi mi (yani başlık satırı mı). */
export function isHeaderRow(row) {
  if (row.childCount === 0) return false
  let allHeader = true
  row.forEach((cell) => {
    if (cell.type.spec.tableRole !== 'header_cell') allHeader = false
  })
  return allHeader
}

/** Toplam satırı işaretli mi. */
export function isTotalRow(row) {
  return row.attrs?.totalRow === true || row.attrs?.totalRow === 'true'
}

/**
 * Tabloyu satır rollerine göre böler.
 *
 * Sıralama ve toplama yalnızca <b>veri</b> satırlarında çalışır; başlık satırı
 * yukarıda, toplam satırı aşağıda sabit kalmalı — aksi hâlde artan sıralamada
 * toplam satırı tablonun ortasına düşerdi.
 */
export function splitRows(table) {
  const rows = tableRows(table)
  const header = []
  const data = []
  const total = []
  for (const row of rows) {
    if (isTotalRow(row.node)) total.push(row)
    else if (isHeaderRow(row.node) && data.length === 0) header.push(row)
    else data.push(row)
  }
  return { header, data, total }
}

/**
 * Tabloda birleştirilmiş hücre var mı.
 *
 * Sıralama ve satır/sütun taşıma bu durumda kapatılıyor (plan R3): birleşmiş
 * bir hücreyi kapsayan satırları yeniden sıralamak tabloyu geri döndürülemez
 * biçimde bozar. Kullanıcıya "neden kapalı" bilgisi arayüzde veriliyor.
 */
export function hasMergedCells(tableNode) {
  let merged = false
  tableNode.forEach((row) => {
    row.forEach((cell) => {
      if ((cell.attrs.colspan || 1) > 1 || (cell.attrs.rowspan || 1) > 1) merged = true
    })
  })
  return merged
}

/** Satırın `index`'inci hücresi (birleşme yoksa sütun indeksiyle aynıdır). */
export function cellAt(rowNode, index) {
  if (index < 0 || index >= rowNode.childCount) return null
  return rowNode.child(index)
}

/** Hücrenin düz metni. */
export function cellText(cellNode) {
  return cellNode?.textContent ?? ''
}

/**
 * Hücrenin içeriğini tek paragraflık metinle değiştirir.
 *
 * Doğrudan `insertText` kullanılmıyor: hücrede birden çok paragraf ya da liste
 * olabilir ve metni "yazmak" eskisini bırakırdı. Tam değişim, biçimlendirmenin
 * öngörülebilir olmasını sağlıyor.
 */
export function setCellText(tr, cellPos, cellNode, text, schema) {
  const from = cellPos + 1
  const to = cellPos + 1 + cellNode.content.size
  const paragraph = schema.nodes.paragraph.create(
      null,
      text ? schema.text(text) : null
  )
  tr.replaceWith(from, to, paragraph)
  return tr
}

/** Bir tablo düğümündeki sütun sayısı (birleşmeler dâhil). */
export function columnCount(tableNode) {
  return TableMap.get(tableNode).width
}

/**
 * İmlecin bulunduğu hücreden tablonun sağ-alt köşesine kalan yer.
 *
 * Yapıştırmanın hedefe sığıp sığmadığına buradan bakılıyor: sığıyorsa
 * ProseMirror'ın zengin yapıştırması çalışır (biçimlendirme korunur), sığmıyorsa
 * tabloyu büyüten düz metin yolu devreye girer.
 */
export function capacityFromCursor(state) {
  const table = findTable(state.selection.$from)
  const cell = findCell(state.selection.$from)
  if (!table || !cell) return null

  const { header, data } = splitRows(table)
  const rows = [...header, ...data]
  let rowIndex = -1
  let columnIndex = -1
  rows.forEach((row, index) => {
    row.node.forEach((child, _offset, position) => {
      if (child === cell.node) {
        rowIndex = index
        columnIndex = position
      }
    })
  })
  if (rowIndex < 0) return null

  return {
    rows: rows.length - rowIndex,
    columns: (rows[rowIndex]?.node.childCount ?? 0) - columnIndex
  }
}

/**
 * Tabloyu düz metin ızgarasına çevirir (dışa aktarma ve dönüştürme için).
 * Toplam satırı da dâhil edilir: dışa aktarılan dosyada görünen tablonun aynısı
 * olmalı.
 */
export function tableToGrid(tableNode) {
  const grid = []
  tableNode.forEach((row) => {
    const cells = []
    row.forEach((cell) => {
      cells.push(cellText(cell))
      const colspan = cell.attrs.colspan || 1
      for (let extra = 1; extra < colspan; extra++) cells.push('')
    })
    grid.push(cells)
  })
  return grid
}

export { TableMap, CellSelection }
