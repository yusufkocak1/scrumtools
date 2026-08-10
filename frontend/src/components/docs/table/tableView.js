/**
 * Okuma görünümlerinde tablo hazırlığı (DOCS_TABLE_PLAN.md Faz 1).
 *
 * Editörde kaydırma sarmalayıcısını prosemirror-tables üretiyor
 * (`div.tableWrapper`); okuma görünümünde ise kaydedilen HTML olduğu gibi
 * basıldığı için sarmalayıcı yok. Sonuç: 12 sütunlu bir tablo sayfa düzenini
 * kırıyor, mobilde ise yatay kaydırma yerine tüm sayfayı esnetiyordu.
 *
 * <b>Neden CSS ile değil:</b> `table { display: block; overflow-x: auto }` hilesi
 * sarmalayıcısız çalışırdı ama tabloyu blok kutusuna çevirdiği için
 * `table-layout: fixed` ve `<col>` genişlikleri devre dışı kalır — yani
 * kullanıcının sürükleyerek ayarladığı sütun genişlikleri okuma modunda
 * kaybolurdu. Gerçek bir sarmalayıcı bu bedeli ödemiyor.
 */

/** Sarmalayıcının kendi dikey kaydırmasına geçtiği satır eşiği. */
const TALL_ROW_THRESHOLD = 20

/**
 * Kökün altındaki tabloları kaydırma sarmalayıcısına alır ve sütun
 * genişliklerini uygular. Yeniden çağrılabilir.
 */
export function wrapTables(root) {
  if (!root) return

  for (const table of root.querySelectorAll('table')) {
    const parent = table.parentElement
    const alreadyWrapped = parent?.classList?.contains('doc-table-wrapper')
        || parent?.classList?.contains('tableWrapper')

    if (!alreadyWrapped) {
      const wrapper = document.createElement('div')
      wrapper.className = 'doc-table-wrapper'
      table.replaceWith(wrapper)
      wrapper.appendChild(table)
    }

    applyColumnWidths(table)
    markTall(table.parentElement, table)
  }
}

/**
 * `colwidth` özniteliğinden `<colgroup>` üretir.
 *
 * <b>Bulunan hata:</b> TipTap sütun genişliğini hücrenin `colwidth`
 * <i>özniteliğinde</i> saklıyor ve `getHTML()` çıktısına `<colgroup>` koymuyor —
 * `<colgroup>`'u yalnızca editördeki NodeView çiziyor. `colwidth` ise standart
 * bir HTML özniteliği değil, tarayıcı onu yok sayıyor. Sonuç: kullanıcının
 * sürükleyerek ayarladığı genişlikler <b>okuma modunda hiç uygulanmıyordu</b> —
 * sanitize izin listelerinde `colwidth` yıllardır duruyor olmasına rağmen.
 *
 * Burada `<colgroup>` DOM'da üretiliyor, kaydedilen HTML'e yazılmıyor: doğruluk
 * kaynağı hücrenin özniteliği olarak kalsın ve iki yerde saklanan bir genişlik
 * ayrışmasın.
 */
function applyColumnWidths(table) {
  const firstRow = table.querySelector('tr')
  if (!firstRow) return

  const widths = []
  let hasWidth = false
  for (const cell of firstRow.children) {
    const colspan = Number(cell.getAttribute('colspan') || 1)
    // `colwidth` birleşmiş hücrede virgülle ayrılmış birden çok değer taşır.
    const declared = (cell.getAttribute('colwidth') || '').split(',')
    for (let index = 0; index < colspan; index++) {
      const width = Number(declared[index])
      if (Number.isFinite(width) && width > 0) hasWidth = true
      widths.push(Number.isFinite(width) && width > 0 ? width : null)
    }
  }

  const existing = table.querySelector('colgroup[data-generated]')
  if (!hasWidth) {
    existing?.remove()
    return
  }

  const colgroup = document.createElement('colgroup')
  colgroup.setAttribute('data-generated', '')
  for (const width of widths) {
    const col = document.createElement('col')
    if (width) col.style.width = `${width}px`
    colgroup.appendChild(col)
  }

  if (existing) existing.replaceWith(colgroup)
  else table.insertBefore(colgroup, table.firstChild)
}

/**
 * Uzun tabloda sarmalayıcıya dikey kaydırma ve yapışkan başlık verilir.
 *
 * Kısa tabloya verilmiyor: `overflow-x: auto` dikey ekseni de kaydırma kabına
 * çevirdiği için `max-height` olmadan yapışkan başlık zaten çalışmaz, ve kısa
 * tabloyu kendi kutusuna hapsetmek sayfa akışını gereksizce bozar.
 */
function markTall(wrapper, table) {
  const rowCount = table.querySelectorAll('tr').length
  if (rowCount > TALL_ROW_THRESHOLD) wrapper.setAttribute('data-tall', '')
  else wrapper.removeAttribute('data-tall')
}
