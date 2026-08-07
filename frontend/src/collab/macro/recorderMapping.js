/**
 * Univer komutu → makro API çağrısı eşlemesi (COLLAB_WORKSPACE_PLAN.md §9.3).
 *
 * **Neden komut kimlikleri dize, köprüdeki gibi içe aktarılmış sabit değil:**
 * `UniverYjsBridge` mutation kimliklerini `@univerjs/preset-sheets-core`'dan
 * sabit olarak alıyor, çünkü orada kaybolan bir sabit sessiz veri bozulması
 * demek — derleme hatası vermesi isteniyor (R1). Burada tam tersi geçerli:
 * eşlemesi bulunamayan komut zaten `// kaydedilemedi:` yorumuna düşüyor, yani
 * güvenli bir davranışı var. Sabit kullanmak, kaydedilemeyen tek bir biçim
 * komutu yüzünden tüm derlemeyi kırardı.
 *
 * **Üretilen kod mutlaktır** (§9.3): aralıklar ve değerler sabit yazılır.
 * Çıktı bitmiş bir araç değil, düzenlenecek bir başlangıçtır.
 */

/** Sütun indeksi → A1 harfi (0 → A, 26 → AA). */
export function columnLetter(index) {
  let value = ''
  let n = index
  while (n >= 0) {
    value = String.fromCharCode((n % 26) + 65) + value
    n = Math.floor(n / 26) - 1
  }
  return value
}

export function a1(range) {
  if (!range) return null
  const { startRow, startColumn, endRow, endColumn } = range
  if (startRow == null || startColumn == null) return null
  const start = `${columnLetter(startColumn)}${startRow + 1}`
  if (endRow === startRow && endColumn === startColumn) return start
  return `${start}:${columnLetter(endColumn ?? startColumn)}${(endRow ?? startRow) + 1}`
}

/** Kaydedilebilir bir komutun ürettiği tek satır. */
function line(code) {
  return { code }
}

/** JS kaynağına gömülebilir hâle getirir. */
function literal(value) {
  return JSON.stringify(value ?? null)
}

/**
 * `SetRangeValuesCommand` parametresindeki değer matrisi, seyrek nesne
 * ({@code {0: {0: {v: 1}}}}) biçiminde gelir. Düz diziye çeviriyoruz ki
 * üretilen kod okunabilir olsun.
 */
function valueMatrix(params, range) {
  const raw = params?.value
  if (!raw || !range) return null

  const rows = (range.endRow ?? range.startRow) - range.startRow + 1
  const cols = (range.endColumn ?? range.startColumn) - range.startColumn + 1

  // Tek hücrelik yazımda Univer doğrudan ICellData verebiliyor.
  if (raw.v !== undefined || raw.f !== undefined) {
    return [[raw.f !== undefined ? raw.f : (raw.v ?? null)]]
  }

  const matrix = []
  for (let r = 0; r < rows; r++) {
    const row = []
    for (let c = 0; c < cols; c++) {
      const cell = raw[r]?.[c] ?? raw[range.startRow + r]?.[range.startColumn + c]
      row.push(cell?.f !== undefined ? cell.f : (cell?.v ?? null))
    }
    matrix.push(row)
  }
  return matrix
}

/**
 * Eşleme tablosu.
 *
 * Her giriş `(params, context) => { code } | null` döner. `null`, "bu komut
 * kaydedilemiyor" demektir ve çağıran taraf yerine bir yorum satırı yazar.
 */
export const COMMAND_MAP = {
  'sheet.command.set-range-values': (params, ctx) => {
    const range = params?.range || ctx.selection
    const address = a1(range)
    if (!address) return null
    const matrix = valueMatrix(params, range)
    if (!matrix) return null
    // Tek hücreye formül yazıldıysa setFormula daha okunur.
    if (matrix.length === 1 && matrix[0].length === 1
        && typeof matrix[0][0] === 'string' && matrix[0][0].startsWith('=')) {
      return line(`sheet.getRange('${address}').setFormula(${literal(matrix[0][0])});`)
    }
    return line(`sheet.getRange('${address}').setValues(${literal(matrix)});`)
  },

  'sheet.command.set-range-bold': styleMapper('bold'),
  'sheet.command.set-range-italic': styleMapper('italic'),
  'sheet.command.set-range-underline': styleMapper('underline'),
  'sheet.command.set-range-text-color': styleMapper('color', (p) => p?.value),
  'sheet.command.set-background-color': styleMapper('bg', (p) => p?.value),
  'sheet.command.set-range-fontsize': styleMapper('fontSize', (p) => p?.value),

  'sheet.command.insert-row': rowColMapper('insertRows', 'row'),
  'sheet.command.insert-row-before': rowColMapper('insertRows', 'row'),
  'sheet.command.insert-row-after': rowColMapper('insertRows', 'row'),
  'sheet.command.remove-row': rowColMapper('deleteRows', 'row'),
  'sheet.command.insert-col': rowColMapper('insertColumns', 'col'),
  'sheet.command.insert-col-before': rowColMapper('insertColumns', 'col'),
  'sheet.command.insert-col-after': rowColMapper('insertColumns', 'col'),
  'sheet.command.remove-col': rowColMapper('deleteColumns', 'col'),

  'sheet.command.sort-range': (params) => {
    const column = params?.orderRules?.[0]?.colIndex ?? params?.colIndex
    if (column == null) return null
    const ascending = (params?.orderRules?.[0]?.type ?? params?.type) !== 'desc'
    return line(`sheet.sort({ column: ${column}, ascending: ${ascending} });`)
  }
}

/**
 * Kaydedilemeyen ama sık kullanılan komutların insan okur adları.
 *
 * Kullanıcıya "bilinmeyen komut sheet.command.add-worksheet-merge" demek yerine
 * "birleştirilmiş hücre" demek için. Listede olmayanlar ham kimlikle görünür —
 * eksik olması bir arıza değil, yalnızca daha az okunur bir mesaj.
 */
export const UNSUPPORTED_LABELS = {
  'sheet.command.add-worksheet-merge': 'hücre birleştirme',
  'sheet.command.remove-worksheet-merge': 'hücre birleştirmeyi kaldırma',
  'sheet.command.set-worksheet-row-height': 'satır yüksekliği',
  'sheet.command.set-worksheet-col-width': 'sütun genişliği',
  'sheet.command.set-frozen': 'bölme dondurma',
  'sheet.command.set-range-border': 'kenarlık',
  'sheet.command.numfmt.set-numfmt': 'sayı biçimi'
}

function styleMapper(key, readValue) {
  return (params, ctx) => {
    const address = a1(params?.range || ctx.selection)
    if (!address) return null
    const value = readValue ? readValue(params) : true
    if (value === undefined || value === null) return null
    return line(`sheet.getRange('${address}').setStyle({ ${key}: ${literal(value)} });`)
  }
}

function rowColMapper(method, axis) {
  return (params) => {
    const range = params?.range
    const start = axis === 'row' ? range?.startRow : range?.startColumn
    const end = axis === 'row' ? range?.endRow : range?.endColumn
    if (start == null) return null
    const count = (end ?? start) - start + 1
    return line(`sheet.${method}(${start}, ${count});`)
  }
}
