/**
 * retroBoardSync.js
 *
 * Retro Board için debounced WebSocket sync motoru.
 *
 * Strateji:
 *  - Gelen WS notification'ları pendingColumns Set'inde biriktirilir.
 *  - Son bildirimden DEBOUNCE_MS milisaniye sonra (trailing debounce) flush tetiklenir.
 *  - isFetching kilidi sayesinde aynı anda yalnızca bir fetch cycles çalışır.
 *  - Flush sonrası onFlush(columns, events) callback çağrılır; sadece etkilenen
 *    kolonlar yeniden çekilir.
 *
 * Kullanım:
 *   const sync = createBoardSync(teamId, boardId, (columns, events) => { ... })
 *   sync.push({ event, columnName, triggeredBy, timestamp })
 *   sync.destroy()
 */

const DEBOUNCE_MS = 2000

/**
 * @param {string} teamId
 * @param {string} boardId
 * @param {Function} onFlush - (columns: Set<string>, events: Array) => void
 * @returns {{ push: Function, destroy: Function }}
 */
export function createBoardSync(teamId, boardId, onFlush) {
    let debounceTimer = null
    let isFetching = false
    let pendingColumns = new Set()
    let pendingEvents = []
    let destroyed = false

    function flush() {
        if (destroyed || isFetching) return
        if (pendingColumns.size === 0) return

        const columnsToFetch = new Set(pendingColumns)
        const eventsSnapshot = [...pendingEvents]

        // Havuzu temizle
        pendingColumns.clear()
        pendingEvents = []

        isFetching = true
        try {
            onFlush(columnsToFetch, eventsSnapshot)
        } finally {
            isFetching = false
        }
    }

    function push(msg) {
        if (destroyed) return

        const columnName = msg?.columnName || null
        const event = msg?.event || 'UNKNOWN'

        // Kolon adı boşsa tüm kolonları yenile (null = all)
        if (columnName) {
            pendingColumns.add(columnName)
        } else {
            // null → tüm kolonları temsil eden özel değer
            pendingColumns.add('__ALL__')
        }

        pendingEvents.push({
            event,
            columnName,
            triggeredBy: msg?.triggeredBy || null,
            timestamp: msg?.timestamp || new Date().toISOString(),
            itemId: msg?.itemId || null
        })

        // Trailing debounce: sayacı sıfırla
        if (debounceTimer) clearTimeout(debounceTimer)
        debounceTimer = setTimeout(flush, DEBOUNCE_MS)
    }

    function destroy() {
        destroyed = true
        if (debounceTimer) {
            clearTimeout(debounceTimer)
            debounceTimer = null
        }
        pendingColumns.clear()
        pendingEvents = []
    }

    return { push, destroy }
}

export default { createBoardSync }

