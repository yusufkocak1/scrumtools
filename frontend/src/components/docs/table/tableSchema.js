/**
 * Docs tablo şeması — öznitelikler, sütun tipleri, toplayıcılar
 * (DOCS_TABLE_PLAN.md Faz 1–2).
 *
 * <b>Bu dosya tek doğruluk kaynağıdır.</b> Tabloya eklenen bir öznitelik dört
 * ayrı yerde birden beyaz listeye girmek zorunda (planın §6'sı):
 *
 *   1. `DocPage.vue` → `renderedContent` (Docs okuma modu)
 *   2. `RichContentViewer.vue` (görev açıklaması/yorum görüntüleyicisi)
 *   3. `CollabHtmlSanitizer.java` (sunucu; Y1/Y2 aynalama yolu)
 *   4. `CollabTextEditor.vue` düğüm tanımları (ortak düzenleme turu)
 *
 * Biri unutulursa öznitelik <i>kaydedilir ama gösterilmez</i>; hata kullanıcıya
 * "bazen çalışmıyor" diye döner ve teşhisi pahalıdır. Üç istemci noktası
 * aşağıdaki `TABLE_HTML_ATTRIBUTES`'ı içe aktarır; Java tarafı elle eşlenir ve
 * orada bu dosyaya atıf yapan bir yorum vardır.
 */

// ─── Hizalama ────────────────────────────────────────────────────────────────

export const ALIGNMENTS = [
  { key: 'left', label: 'Sola yasla', icon: '⬅' },
  { key: 'center', label: 'Ortala', icon: '↔' },
  { key: 'right', label: 'Sağa yasla', icon: '➡' }
]

// ─── Hücre arka planı ────────────────────────────────────────────────────────

/**
 * Renkler `data-bg` anahtarı olarak saklanıp CSS'ten boyanıyor, satır içi
 * `style` olarak değil. İki sebebi var: (a) sanitize katmanlarından geçmesi
 * kolay — bir anahtar kelime, keyfi CSS değil; (b) tema değişirse tek yerden
 * güncellenir, yüzlerce hücrenin içine yazılmış hex kodları değil.
 */
export const CELL_BACKGROUNDS = [
  { key: null, label: 'Yok', swatch: 'transparent' },
  { key: 'slate', label: 'Gri', swatch: '#e2e8f0' },
  { key: 'red', label: 'Kırmızı', swatch: '#fecaca' },
  { key: 'amber', label: 'Turuncu', swatch: '#fde68a' },
  { key: 'green', label: 'Yeşil', swatch: '#bbf7d0' },
  { key: 'blue', label: 'Mavi', swatch: '#bfdbfe' },
  { key: 'violet', label: 'Mor', swatch: '#ddd6fe' },
  { key: 'pink', label: 'Pembe', swatch: '#fbcfe8' }
]

export const BACKGROUND_KEYS = CELL_BACKGROUNDS.map((c) => c.key).filter(Boolean)

// ─── Sayı / tarih ayrıştırma ─────────────────────────────────────────────────

/**
 * Metinden sayı okur.
 *
 * <b>Neden bu kadar uğraşıyor:</b> "1.234,56" (TR) ile "1,234.56" (EN) aynı
 * sayıdır ama `parseFloat` ikisini de yanlış okur — birincisi 1.234, ikincisi 1
 * çıkar. Tablo sıralaması ve toplamı bu ayrıştırıcının doğruluğuna bağlı.
 *
 * Belirsiz tek durum tek ayraçlı üç haneli gruptur ("1,234"): TR'de 1.234
 * ondalığı, EN'de bin dört yüz otuz dört demektir. Uygulamanın dili Türkçe
 * olduğu için <b>TR yorumu seçiliyor</b> (virgül = ondalık, nokta = binlik).
 * Bu belirsizlik zaten yalnızca kullanıcının <i>yeni yazdığı</i> metinde
 * yaşanır: biçimlendirme bir kez uygulandığında ham değer hücrenin `data-v`
 * özniteliğine yazılır ve sonraki okumalar oradan gelir.
 */
export function parseNumber(input) {
  if (input == null) return null
  if (typeof input === 'number') return Number.isFinite(input) ? input : null

  let text = String(input).trim()
  if (!text) return null

  // Para birimi simgeleri, yüzde işareti, boşluklar ve binlik ayracı olarak
  // kullanılan dar boşluk (U+00A0 / U+202F) temizlenir.
  text = text.replace(/[₺$€£%\s  ]/g, '')

  // Muhasebe gösterimi: (1.234) = negatif
  let negative = false
  if (/^\((.*)\)$/.test(text)) {
    negative = true
    text = text.slice(1, -1)
  }

  const hasDot = text.includes('.')
  const hasComma = text.includes(',')

  if (hasDot && hasComma) {
    // İkisi de varsa sonuncusu ondalık ayracıdır; diğeri binliktir.
    const decimalSeparator = text.lastIndexOf(',') > text.lastIndexOf('.') ? ',' : '.'
    const thousandSeparator = decimalSeparator === ',' ? '.' : ','
    text = text.split(thousandSeparator).join('')
    text = text.replace(decimalSeparator, '.')
  } else if (hasComma) {
    // Virgül tek ayraç: TR'de ondalıktır. Birden fazlaysa binliktir.
    text = text.split(',').length > 2 ? text.split(',').join('') : text.replace(',', '.')
  } else if (hasDot) {
    // Nokta tek ayraç: TR'de binliktir — ama yalnızca üçer haneli gruplandıysa.
    // "3.14" gibi bir değer binlik olamaz, ondalık kabul edilir.
    const parts = text.split('.')
    const looksGrouped = parts.length > 1 && parts.slice(1).every((p) => p.length === 3)
    if (looksGrouped) text = parts.join('')
  }

  if (!/^[+-]?\d*\.?\d+(e[+-]?\d+)?$/i.test(text)) return null
  const value = Number(text)
  if (!Number.isFinite(value)) return null
  return negative ? -value : value
}

/** ISO (`2026-08-10`), `10.08.2026` ve `10/08/2026` biçimlerini okur. */
export function parseDate(input) {
  if (!input) return null
  const text = String(input).trim()
  if (!text) return null

  const iso = /^(\d{4})-(\d{2})-(\d{2})$/.exec(text)
  if (iso) return toUtcDate(+iso[1], +iso[2], +iso[3])

  const local = /^(\d{1,2})[./-](\d{1,2})[./-](\d{4})$/.exec(text)
  if (local) return toUtcDate(+local[3], +local[2], +local[1])

  return null
}

/**
 * Tarihler UTC gece yarısına sabitleniyor. Yerel saatle kurulsaydı, sunucu ile
 * istemci farklı saat dilimlerindeyken "10 Ağustos" bir günde 9 Ağustos'a
 * kayabilirdi — tablo sıralamasında fark etmesi güç, açıklaması zor bir hata.
 */
function toUtcDate(year, month, day) {
  const date = new Date(Date.UTC(year, month - 1, day))
  if (Number.isNaN(date.getTime())) return null
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1) return null
  return date
}

const numberFormatCache = new Map()

function formatNumber(value, { decimals = null, grouping = true } = {}) {
  const key = `${decimals}|${grouping}`
  let formatter = numberFormatCache.get(key)
  if (!formatter) {
    formatter = new Intl.NumberFormat('tr-TR', {
      useGrouping: grouping,
      minimumFractionDigits: decimals ?? 0,
      // `decimals` verilmemişse kullanıcının yazdığı hassasiyet korunur:
      // 3,14159 yazan biri 3 görmemeli.
      maximumFractionDigits: decimals ?? 6
    })
    numberFormatCache.set(key, formatter)
  }
  return formatter.format(value)
}

// ─── Sütun tipleri ───────────────────────────────────────────────────────────

/**
 * Her tip üç şey tanımlar: metni ham değere çeviren `parse`, ham değeri
 * gösterime çeviren `format`, ve karşılaştırma için `sortValue`.
 *
 * `sortValue` ayrı olmak zorunda: onay kutusunda gösterim "✓" ama sıralamada
 * 1/0 gerekir, tarihte gösterim "10.08.2026" ama sıralamada zaman damgası.
 */
export const COLUMN_TYPES = {
  text: {
    key: 'text',
    label: 'Metin',
    align: 'left',
    aggregatable: false,
    parse: (text) => (text?.trim() ? text.trim() : null),
    format: (raw) => (raw == null ? '' : String(raw)),
    sortValue: (raw) => (raw == null ? null : String(raw).toLocaleLowerCase('tr'))
  },

  number: {
    key: 'number',
    label: 'Sayı',
    align: 'right',
    aggregatable: true,
    parse: parseNumber,
    format: (raw, options) => (raw == null ? '' : formatNumber(raw, options)),
    sortValue: (raw) => raw
  },

  currency: {
    key: 'currency',
    label: 'Para',
    align: 'right',
    aggregatable: true,
    parse: parseNumber,
    format: (raw, options) => {
      if (raw == null) return ''
      const symbol = options?.symbol || '₺'
      return `${formatNumber(raw, { ...options, decimals: options?.decimals ?? 2 })} ${symbol}`
    },
    sortValue: (raw) => raw
  },

  percent: {
    key: 'percent',
    label: 'Yüzde',
    align: 'right',
    aggregatable: true,
    parse: parseNumber,
    // Ham değer <b>kullanıcının yazdığı sayıdır</b> (45 → "%45"), 0–1 aralığına
    // çevrilmez. Aksi hâlde 45 yazan kullanıcı %4.500 görürdü; toplam satırı da
    // anlaşılmaz hâle gelirdi.
    format: (raw, options) => (raw == null ? '' : `%${formatNumber(raw, options)}`),
    sortValue: (raw) => raw
  },

  date: {
    key: 'date',
    label: 'Tarih',
    align: 'left',
    aggregatable: true,
    parse: (text) => {
      const date = parseDate(text)
      return date ? date.toISOString().slice(0, 10) : null
    },
    format: (raw) => {
      const date = parseDate(raw)
      if (!date) return raw == null ? '' : String(raw)
      return new Intl.DateTimeFormat('tr-TR', { timeZone: 'UTC' }).format(date)
    },
    sortValue: (raw) => {
      const date = parseDate(raw)
      return date ? date.getTime() : null
    }
  },

  checkbox: {
    key: 'checkbox',
    label: 'Onay kutusu',
    align: 'center',
    aggregatable: true,
    parse: (text) => {
      const value = String(text ?? '').trim().toLowerCase()
      if (!value) return null
      return ['✓', '✔', 'x', 'evet', 'true', '1', 'var', 'tamam'].includes(value) ? '1' : '0'
    },
    format: (raw) => (raw === '1' ? '✓' : raw === '0' ? '—' : ''),
    sortValue: (raw) => (raw === '1' ? 1 : raw === '0' ? 0 : null)
  },

  select: {
    key: 'select',
    label: 'Etiket',
    align: 'left',
    aggregatable: false,
    parse: (text) => (text?.trim() ? text.trim() : null),
    format: (raw) => (raw == null ? '' : String(raw)),
    sortValue: (raw) => (raw == null ? null : String(raw).toLocaleLowerCase('tr'))
  }
}

export const COLUMN_TYPE_KEYS = Object.keys(COLUMN_TYPES)

export function columnType(key) {
  return COLUMN_TYPES[key] || COLUMN_TYPES.text
}

/**
 * Hücre metnini ham değere çevirir.
 *
 * <b>Her zaman metinden okur, `data-v`'den değil.</b> İlk tasarımda `data-v`
 * "önbellek" olarak okunuyordu; bunun kaçınılmaz sonucu şuydu: kullanıcı
 * hücreyi düzenlediği anda metin ile `data-v` ayrışıyor ve toplam satırı
 * <i>eski</i> değerle hesaplanıyordu. Tek doğruluk kaynağı görünen metindir;
 * `data-v` yalnızca <b>çıktıdır</b> (dışa aktarma ve makine okuması için
 * yazılır, okunmaz).
 */
export function cellRawValue(typeKey, text) {
  return columnType(typeKey).parse(text)
}

// ─── Sütun biçim seçenekleri ─────────────────────────────────────────────────

/**
 * `data-col-format` içeriği: `decimals=2;symbol=₺;grouping=0`.
 *
 * JSON yerine bu biçim seçildi çünkü öznitelik değeri HTML'e gömülüyor ve
 * JSON'un tırnakları her sanitize/serileştirme turunda kaçış gerektirirdi.
 */
export function parseColumnFormat(text) {
  const options = {}
  if (!text) return options
  for (const part of String(text).split(';')) {
    const [key, value] = part.split('=')
    if (!key || value == null) continue
    if (key === 'decimals') {
      const decimals = Number(value)
      if (Number.isInteger(decimals) && decimals >= 0 && decimals <= 6) options.decimals = decimals
    } else if (key === 'symbol') {
      options.symbol = value.slice(0, 4)
    } else if (key === 'grouping') {
      options.grouping = value !== '0'
    }
  }
  return options
}

export function serializeColumnFormat(options) {
  if (!options) return null
  const parts = []
  if (options.decimals != null) parts.push(`decimals=${options.decimals}`)
  if (options.symbol) parts.push(`symbol=${options.symbol}`)
  if (options.grouping === false) parts.push('grouping=0')
  return parts.length ? parts.join(';') : null
}

// ─── Toplayıcılar ────────────────────────────────────────────────────────────

/**
 * Toplayıcı sonucu belgeye yazılır ama <b>her yüklemede yeniden hesaplanır</b>
 * (plan §2.4): kaydedilmiş değere güvenilmez, çünkü sayfa başka bir istemcide
 * düzenlenmiş ve yalnızca hücreler değişmiş olabilir.
 */
export const AGGREGATIONS = {
  none: { key: 'none', label: 'Yok', compute: () => null },

  sum: {
    key: 'sum',
    label: 'Toplam',
    compute: (values) => (values.length ? values.reduce((a, b) => a + b, 0) : null)
  },

  avg: {
    key: 'avg',
    label: 'Ortalama',
    compute: (values) => (values.length ? values.reduce((a, b) => a + b, 0) / values.length : null)
  },

  min: {
    key: 'min',
    label: 'En düşük',
    compute: (values) => (values.length ? Math.min(...values) : null)
  },

  max: {
    key: 'max',
    label: 'En yüksek',
    compute: (values) => (values.length ? Math.max(...values) : null)
  },

  // Sayısal olmayan sütunlarda da anlamlı olan iki toplayıcı. `compute` yerine
  // `computeAggregate` içinde özel olarak ele alınıyorlar: girdileri sayıya
  // çevirmeden, hücre metnine bakarak sayıyorlar.
  count: { key: 'count', label: 'Satır adedi', numeric: false, compute: null },
  filled: { key: 'filled', label: 'Dolu hücre', numeric: false, compute: null }
}

export const AGGREGATION_KEYS = Object.keys(AGGREGATIONS)

/**
 * Bir sütunun toplayıcı sonucunu üretir.
 *
 * @param aggKey   toplayıcı anahtarı
 * @param typeKey  sütun tipi (biçimlendirme için)
 * @param texts    sütundaki veri hücrelerinin metinleri
 * @returns gösterilecek metin (boşsa `''`)
 */
export function computeAggregate(aggKey, typeKey, texts, formatOptions) {
  const aggregation = AGGREGATIONS[aggKey]
  if (!aggregation || aggKey === 'none') return ''

  if (aggKey === 'count') return String(texts.length)
  if (aggKey === 'filled') return String(texts.filter((text) => text?.trim()).length)

  // Tarihlerin toplamı ve ortalaması anlamsız (iki tarihi toplamak bir tarih
  // vermez); yalnızca en erken/en geç sorusu geçerli.
  if (typeKey === 'date' && !['min', 'max'].includes(aggKey)) return ''

  const type = columnType(typeKey)
  const numbers = texts
      .map((text) => {
        const value = type.sortValue(type.parse(text))
        return typeof value === 'number' && Number.isFinite(value) ? value : null
      })
      .filter((value) => value != null)

  const result = aggregation.compute(numbers)
  if (result == null) return ''

  // Sonuç sütunun kendi biçimiyle gösterilir: para sütununun toplamı da para
  // görünmeli. Tarih sütununda min/max bir zaman damgasıdır, geri çevrilir.
  if (typeKey === 'date') {
    return COLUMN_TYPES.date.format(new Date(result).toISOString().slice(0, 10))
  }
  if (typeKey === 'checkbox') return String(result)
  return type.format(result, formatOptions)
}

// ─── Sanitize beyaz listesi ──────────────────────────────────────────────────

/** Tablo için gereken HTML etiketleri (DOMPurify `ADD_TAGS`). */
export const TABLE_HTML_TAGS = [
  'table', 'thead', 'tbody', 'tfoot', 'tr', 'th', 'td', 'colgroup', 'col', 'caption'
]

/**
 * Tablo için gereken öznitelikler (DOMPurify `ADD_ATTR`).
 *
 * <b>Bu listeye ekleme yaparken `CollabHtmlSanitizer.java` de güncellenmeli.</b>
 * Sunucu tarafı jsoup kullanıyor ve bu diziyi okuyamıyor; oradaki liste elle
 * eşlenmiş durumda ve bu dosyaya atıf yapan bir yorum taşıyor.
 */
export const TABLE_HTML_ATTRIBUTES = [
  'colspan', 'rowspan', 'colwidth',
  // Hücre görünümü
  'data-align', 'data-bg',
  // Sütun tanımı (hem `th` hem `td` üzerinde — CSS "başlığı şu olan sütun"
  // diye seçemediği için tip her hücrede tekrar eder)
  'data-col-type', 'data-col-format', 'data-col-agg',
  // Ham değer: sıralama ve toplama gösterim metnini yeniden yorumlamak yerine
  // buradan okur
  'data-v',
  // Tablo düzeyi durum
  'data-sort-col', 'data-sort-dir', 'data-table-layout',
  // Toplam satırı işareti
  'data-total-row'
]

/** Y3 gömme kabının öznitelikleri — tablo listesiyle birlikte kullanılır. */
export const EMBED_HTML_ATTRIBUTES = [
  'data-collab-embed', 'data-document-id', 'data-type', 'data-height'
]

/**
 * Görev açıklaması/yorumu için DOMPurify yapılandırması.
 *
 * `data-*` öznitelikleri DOMPurify'da zaten varsayılan olarak serbest
 * (`ALLOW_DATA_ATTR: true`); listede açıkça yer almalarının sebebi belge
 * değeri — hangi özniteliklerin taşınmak <i>zorunda</i> olduğu tek bakışta
 * görünsün ve sunucudaki jsoup listesiyle karşılaştırılabilsin. Asıl kritik
 * olan `colwidth`: standart olmayan bir öznitelik ve listede olmazsa süzülür.
 */
export const RICH_CONTENT_SANITIZE_CONFIG = {
  ADD_TAGS: ['img', ...TABLE_HTML_TAGS],
  ADD_ATTR: [
    'src', 'alt', 'title', 'href', 'target', 'rel',
    ...TABLE_HTML_ATTRIBUTES,
    ...EMBED_HTML_ATTRIBUTES
  ]
}

/**
 * Docs sayfası için yapılandırma — yukarıdakine ek olarak `style`.
 *
 * <b>Neden yalnızca Docs'ta:</b> Docs editörünün bir "HTML kaynak kodu" modu var
 * ve orada yazılan satır içi stiller bugüne kadar korunuyordu; listeyi
 * ortaklaştırırken bunu kaldırmak mevcut sayfalarda sessiz bir gerileme olurdu.
 * Aynı izni görev yorumlarına <i>genişletmek</i> ise ayrı bir karar: yorum
 * yazabilen herkes `position: fixed` ile arayüzün üzerine bindirme yapabilirdi.
 * Bu yüzden `style` Docs'a özgü kaldı.
 */
export const DOC_CONTENT_SANITIZE_CONFIG = {
  ADD_TAGS: RICH_CONTENT_SANITIZE_CONFIG.ADD_TAGS,
  ADD_ATTR: [...RICH_CONTENT_SANITIZE_CONFIG.ADD_ATTR, 'style']
}
