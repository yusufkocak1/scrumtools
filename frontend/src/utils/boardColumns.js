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

/**
 * Sütunun topladığı durum adları.
 *
 * `statuses` alanı hiç yoksa (eşleme özelliğinden önce oluşturulmuş board)
 * sütun kendi adıyla eşleşir. Boş dizi ise bu bilinçli bir seçimdir —
 * "bu sütun hiçbir durumu toplamıyor" — ve sütun adına geri düşülmez; aksi
 * halde kullanıcının kaldırdığı eşleme sessizce geri gelirdi.
 */
export function columnStatuses(column) {
    const explicit = column?.statuses
    if (Array.isArray(explicit)) return explicit
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

// Eşlenmemiş durumları bulan ve aynı durumun iki sütuna verilmesini yakalayan
// yardımcılar buradan kaldırıldı: ColumnMapEditor eşlemeyi tek bir düzen üzerinde
// yürütüyor, eşlenmemişler zaten kendi panelinde duruyor ve bir durum aynı anda
// yalnızca tek sütunda olabiliyor.
