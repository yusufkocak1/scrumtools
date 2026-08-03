/**
 * chartPalette.js
 *
 * Grafiklerin ortak renk sözlüğü. Aynı kategori her widget'ta aynı renkte çıksın
 * diye renk seçimi tek yerde toplandı (bkz. RICH_FILTER_PLAN.md — K12).
 *
 * Öncelik sırası:
 *  1. Sunucudan gelen renk — akıllı filtrenin yazarı seçmiş ya da durum renginin
 *     kendisi (workflow'da tanımlı). En güvenilir kaynak budur.
 *  2. Öncelik / tür gibi sabit kümeler için uygulama sözlüğü.
 *  3. Kalanlar için anahtardan türetilen deterministik renk: aynı etiket her
 *     grafikte, her oturumda aynı rengi alır.
 *
 * Sıraya göre renk atamak (3. maddenin alternatifi) kabul edilmedi: bir hafta
 * mavi olan "ödeme" etiketi, sıralama değişince ertesi hafta yeşile döner ve
 * kullanıcı grafikleri karşılaştıramaz.
 */

/** Genel amaçlı palet — birbirinden ayırt edilebilir, koyu zeminde de okunur. */
export const CHART_PALETTE = [
    '#6366F1', '#0EA5E9', '#10B981', '#F59E0B',
    '#EF4444', '#8B5CF6', '#EC4899', '#14B8A6',
    '#F97316', '#64748B', '#84CC16', '#A855F7',
]

/** Öncelik: acilden sakine doğru sıcaktan soğuğa. */
export const PRIORITY_COLORS = {
    critical: '#DC2626',
    high: '#F97316',
    medium: '#F59E0B',
    low: '#10B981',
}

export const TYPE_COLORS = {
    bug: '#EF4444',
    story: '#10B981',
    task: '#6366F1',
    epic: '#8B5CF6',
    subtask: '#94A3B8',
}

/** Değeri olmayan kova — nötr gri, veriyle karışmasın. */
export const EMPTY_COLOR = '#CBD5E1'
/** "Diğer" kovası — tek tek gösterilmeyen artıklar. */
export const OTHER_COLOR = '#94A3B8'

/**
 * Bir kovanın rengi.
 * @param {{key?: string, label?: string, color?: string|null}} bucket
 * @param {number} index — sözlükte karşılığı olmayanlarda son çare
 */
export function colorFor(bucket, index = 0) {
    if (bucket?.color) return bucket.color

    const key = String(bucket?.key ?? '')
    if (!key) return EMPTY_COLOR
    if (key === '__other__') return OTHER_COLOR

    const normalized = key.toLowerCase()
    if (PRIORITY_COLORS[normalized]) return PRIORITY_COLORS[normalized]
    if (TYPE_COLORS[normalized]) return TYPE_COLORS[normalized]

    return hashColor(key, index)
}

/** Kova listesinin renkleri — grafik veri kümesi için. */
export function colorsFor(buckets) {
    return (buckets || []).map((bucket, index) => colorFor(bucket, index))
}

/**
 * Anahtardan deterministik renk. Basit bir toplama hash'i yeterli: amaç
 * kriptografik dağılım değil, aynı adın hep aynı rengi almasıdır.
 */
export function hashColor(key, fallbackIndex = 0) {
    if (!key) return CHART_PALETTE[fallbackIndex % CHART_PALETTE.length]

    let hash = 0
    for (let i = 0; i < key.length; i++) {
        hash = (hash * 31 + key.charCodeAt(i)) | 0
    }
    return CHART_PALETTE[Math.abs(hash) % CHART_PALETTE.length]
}

/** Seçili olmayan dilimleri soluklaştırmak için — çapraz filtrelemede odak vurgusu. */
export function fade(color, alpha = 0.25) {
    if (!color?.startsWith('#') || (color.length !== 7 && color.length !== 4)) return color

    const hex = color.length === 4
        ? color.slice(1).split('').map(c => c + c).join('')
        : color.slice(1)

    const r = parseInt(hex.slice(0, 2), 16)
    const g = parseInt(hex.slice(2, 4), 16)
    const b = parseInt(hex.slice(4, 6), 16)
    return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
