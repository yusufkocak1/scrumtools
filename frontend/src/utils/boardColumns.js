/**
 * boardColumns.js
 *
 * Board sütunları ile görev durumları arasındaki eşleme.
 *
 * Sütunlar artık durum adıyla birebir eşleşmek zorunda değil: bir sütun birden
 * fazla durumu toplayabilir (ör. "Doğrulama" sütunu = In Review + Testing) ve bir
 * durum board dışında bırakılabilir. Eşleme sütunun `statuses` dizisinde durur.
 *
 * Geriye dönük uyum: `statuses` tanımlı değilse sütun kendi adıyla eşleşir —
 * eski board'lar (eşleme alanı yokken oluşturulmuş) davranış değiştirmeden çalışır.
 */

const lower = (v) => String(v ?? '').toLowerCase()

/** Sütunun topladığı durum adları. */
export function columnStatuses(column) {
    const explicit = column?.statuses
    if (Array.isArray(explicit) && explicit.length > 0) return explicit
    return column?.name ? [column.name] : []
}

/**
 * Sürükle-bırakta göreve yazılacak durum.
 * Sütunda birden fazla durum varsa ilki "birincil" kabul edilir; görev zaten o
 * sütuna ait bir durumdaysa durum korunur (In Review'daki bir kartı aynı sütun
 * içinde bırakmak onu Testing'e çevirmemeli).
 */
export function targetStatusFor(column, currentStatus) {
    const statuses = columnStatuses(column)
    if (statuses.length === 0) return currentStatus
    if (currentStatus && statuses.some(s => lower(s) === lower(currentStatus))) {
        return currentStatus
    }
    return statuses[0]
}

/**
 * Görevleri sütunlara dağıtır.
 *
 * Hiçbir sütuna eşlenmeyen durumdaki görevler `fallbackToFirst` açıkken ilk
 * sütuna düşer (eski davranış — iş gözden kaybolmasın), kapalıyken `unmapped`
 * listesinde toplanır.
 *
 * @returns {{ byColumn: Record<string, Array>, unmapped: Array }}
 */
export function distributeTasks(columns, tasks, { fallbackToFirst = true } = {}) {
    const byColumn = {}
    for (const col of columns) byColumn[col.name] = []

    // Durum adı → sütun adı. İlk eşleşen sütun kazanır; aynı durumu iki sütuna
    // vermek görev sayılarını ikiye katlardı.
    const statusToColumn = new Map()
    for (const col of columns) {
        for (const status of columnStatuses(col)) {
            const key = lower(status)
            if (!statusToColumn.has(key)) statusToColumn.set(key, col.name)
        }
    }

    const unmapped = []
    for (const task of tasks) {
        const columnName = statusToColumn.get(lower(task.status))
        if (columnName) {
            byColumn[columnName].push(task)
        } else if (fallbackToFirst && columns.length > 0) {
            byColumn[columns[0].name].push(task)
            unmapped.push(task)
        } else {
            unmapped.push(task)
        }
    }

    return { byColumn, unmapped }
}

/**
 * Sütunlarda hiç yer almayan durum adları — ayarlar ekranı bunları "board'da
 * görünmüyor" uyarısı olarak listeler.
 */
export function statusesOutsideBoard(columns, statusNames) {
    const mapped = new Set()
    for (const col of columns) {
        for (const s of columnStatuses(col)) mapped.add(lower(s))
    }
    return statusNames.filter(name => !mapped.has(lower(name)))
}

/**
 * Aynı durumu birden fazla sütuna vermiş yapılandırmalar — kaydetmeden önce
 * uyarı göstermek için.
 */
export function duplicateStatusAssignments(columns) {
    const seen = new Map()
    const duplicates = []
    for (const col of columns) {
        for (const s of columnStatuses(col)) {
            const key = lower(s)
            if (seen.has(key)) {
                duplicates.push({ status: s, columns: [seen.get(key), col.name] })
            } else {
                seen.set(key, col.name)
            }
        }
    }
    return duplicates
}
