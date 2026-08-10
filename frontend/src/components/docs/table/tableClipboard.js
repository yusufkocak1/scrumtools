/**
 * Pano ve dosya ızgaraları — Excel/Sheets'ten yapıştırma, CSV içe/dışa aktarma
 * (DOCS_TABLE_PLAN.md Faz 1 / 2.6).
 *
 * <b>Çözülen sorun:</b> TipTap, Excel'den kopyalanan bir aralığı yalnızca
 * `text/html` biçimiyle tanıyordu; düz metin (TSV) olarak yapıştırıldığında tek
 * bir paragrafa dönüşüyordu. Üstelik HTML yolu da tabloyu <i>büyütmüyordu</i>:
 * prosemirror-tables yapıştırılan aralığı hedef tabloya <b>kırpıyor</b>, yani
 * 3 satırlık tabloya 30 satır yapıştıran kullanıcı 27 satırı sessizce
 * kaybediyordu.
 */

/**
 * Tırnak farkındalıklı ayraçlı metin ayrıştırıcısı.
 *
 * Elle yazıldı çünkü tek ihtiyacımız RFC 4180'in çekirdeği: tırnak içinde ayraç
 * ve satır sonu, iki tırnakla kaçırılmış tırnak. Bunun için bir bağımlılık
 * eklemek, planın T9 kararına (S2 dış bağımlılıksız) aykırı olurdu.
 */
export function parseDelimited(text, delimiter) {
  const grid = []
  let row = []
  let field = ''
  let inQuotes = false

  for (let index = 0; index < text.length; index++) {
    const char = text[index]

    if (inQuotes) {
      if (char === '"') {
        if (text[index + 1] === '"') {
          field += '"'
          index++
        } else {
          inQuotes = false
        }
      } else {
        field += char
      }
      continue
    }

    if (char === '"' && field === '') {
      inQuotes = true
    } else if (char === delimiter) {
      row.push(field)
      field = ''
    } else if (char === '\n' || char === '\r') {
      // \r\n tek satır sonu sayılır
      if (char === '\r' && text[index + 1] === '\n') index++
      row.push(field)
      grid.push(row)
      row = []
      field = ''
    } else {
      field += char
    }
  }

  if (field !== '' || row.length) {
    row.push(field)
    grid.push(row)
  }

  // Sondaki boş satırlar atılır: metin dosyaları genelde satır sonuyla biter ve
  // bu, tabloya her seferinde boş bir satır eklerdi.
  while (grid.length && grid[grid.length - 1].every((cell) => cell === '')) grid.pop()

  return grid
}

/**
 * Ayracı tahmin eder: sekme > noktalı virgül > virgül.
 *
 * Noktalı virgül virgülden önce geliyor çünkü Türkçe yerelde Excel CSV'yi
 * `;` ile yazar — ondalık ayracı zaten virgül olduğu için.
 */
export function detectDelimiter(text) {
  const sample = text.split(/\r\n|\n|\r/).slice(0, 10).join('\n')
  if (sample.includes('\t')) return '\t'
  const semicolons = (sample.match(/;/g) || []).length
  const commas = (sample.match(/,/g) || []).length
  if (semicolons && semicolons >= commas) return ';'
  if (commas) return ','
  return null
}

/**
 * Panodaki düz metinden ızgara üretir; tablo gibi görünmüyorsa `null`.
 *
 * <b>Yalnızca sekme kabul ediliyor.</b> Virgüllü bir cümleyi yapıştıran
 * kullanıcının karşısına tablo çıkması, kazandığından çok kaybettirirdi; CSV
 * için ayrı ve <i>bilinçli</i> bir içe aktarma akışı var.
 */
export function parseClipboardGrid(text) {
  if (!text || !text.includes('\t')) return null
  const grid = parseDelimited(text, '\t')
  if (grid.length < 1) return null
  const width = Math.max(...grid.map((row) => row.length))
  if (width < 2) return null
  // Satırları eşit uzunluğa tamamla — düzensiz ızgara tabloyu bozar.
  return grid.map((row) => {
    const filled = row.slice()
    while (filled.length < width) filled.push('')
    return filled
  })
}

/**
 * HTML panosundaki ilk tablodan ızgara üretir.
 *
 * Hücre içi biçimlendirme (kalın, link) <b>kaybolur</b>: bu yol yalnızca
 * yapıştırılan tablo hedefe sığmadığında, yani alternatifi veri kaybı olduğunda
 * kullanılıyor. Sığdığında ProseMirror'ın kendi zengin yapıştırması çalışır.
 */
export function parseHtmlTableGrid(html) {
  if (!html || !/<table/i.test(html)) return null
  const document = new DOMParser().parseFromString(html, 'text/html')
  const table = document.querySelector('table')
  if (!table) return null

  const grid = []
  for (const row of table.querySelectorAll('tr')) {
    const cells = []
    for (const cell of row.querySelectorAll('th, td')) {
      cells.push((cell.textContent || '').replace(/\s+/g, ' ').trim())
      // Birleştirilmiş hücreler düzleştirilir: kaç sütun kapsıyorsa o kadar
      // boş hücre eklenir, yoksa sonraki satırlarla hizası kayar.
      const colspan = Number(cell.getAttribute('colspan') || 1)
      for (let extra = 1; extra < colspan; extra++) cells.push('')
    }
    if (cells.length) grid.push(cells)
  }
  if (!grid.length) return null

  const width = Math.max(...grid.map((row) => row.length))
  return grid.map((row) => {
    const filled = row.slice()
    while (filled.length < width) filled.push('')
    return filled
  })
}

/** Izgarayı CSV'ye çevirir (dışa aktarma). */
export function gridToCsv(grid, delimiter = ';') {
  return grid
      .map((row) => row
          .map((cell) => {
            const text = cell == null ? '' : String(cell)
            return /["\n\r]|[;,\t]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
          })
          .join(delimiter))
      .join('\r\n')
}
