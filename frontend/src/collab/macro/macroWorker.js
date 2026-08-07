/* eslint-disable no-restricted-globals */
/**
 * Makro sandbox'ı — Web Worker (COLLAB_WORKSPACE_PLAN.md K8 / §9.2).
 *
 * Model: **oku-anlık-görüntü / yaz-toplu.** Worker'a canlı doküman verilmez;
 * ana iş parçacığı okunabilir bir kopya gönderir, makro onun üzerinde çalışır ve
 * geri dönen işlem listesi tek bir Yjs transaction'ında uygulanır. Bunun
 * alternatifi `SharedArrayBuffer` + COOP/COEP başlıklarıyla eşzamanlı RPC'ydi;
 * o yol hem sunucu yapılandırmasını hem de tüm sayfayı izole moda sokardı.
 *
 * <b>Neden bu bir güvenlik sınırı sayılıyor:</b> worker aynı origin'de koşar ama
 * ağa ve depolamaya açılan tüm tutamaklar aşağıda siliniyor. Buna rağmen tek
 * gerçek koruma bu değil — `ScrumTools.tasks` gibi veri çağrıları ana iş
 * parçacığına gidiyor ve orada **çalıştıran kullanıcının kendi oturumuyla**
 * yapılıyor. Yani makro, kullanıcının zaten yapabileceğinden fazlasını yapamaz.
 */

// ─── Sertleştirme ──────────────────────────────────────────────────────────
// Silme sırası önemli: kullanıcı kodu çalışmadan önce bitmiş olmalı.
for (const name of [
  'fetch', 'XMLHttpRequest', 'importScripts', 'WebSocket', 'EventSource',
  'indexedDB', 'caches', 'Worker', 'SharedWorker', 'BroadcastChannel'
]) {
  try { delete self[name] } catch { /* bazı tutamaklar yapılandırılamaz */ }
  try { self[name] = undefined } catch { /* silinemediyse en azından boşalt */ }
}

const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor

/** Bekleyen ana iş parçacığı çağrıları: id → { resolve, reject }. */
const pending = new Map()
let callSequence = 0
let logLines = []
let ops = []

function hostCall(scope, method, args) {
  const id = ++callSequence
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject })
    self.postMessage({ type: 'call', id, scope, method, args })
  })
}

function log(...values) {
  if (logLines.length >= 500) return
  logLines.push(values.map(stringify).join(' '))
}

function stringify(value) {
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value)
  } catch {
    return String(value)
  }
}

// ─── A1 gösterimi ──────────────────────────────────────────────────────────

function columnToIndex(letters) {
  let index = 0
  for (const char of letters.toUpperCase()) {
    index = index * 26 + (char.charCodeAt(0) - 64)
  }
  return index - 1
}

function indexToColumn(index) {
  let result = ''
  let n = index + 1
  while (n > 0) {
    const remainder = (n - 1) % 26
    result = String.fromCharCode(65 + remainder) + result
    n = Math.floor((n - 1) / 26)
  }
  return result
}

function parseA1(notation) {
  const match = /^([A-Za-z]+)(\d+)(?::([A-Za-z]+)(\d+))?$/.exec(String(notation).trim())
  if (!match) throw new Error(`Geçersiz aralık: ${notation}`)
  const startColumn = columnToIndex(match[1])
  const startRow = Number(match[2]) - 1
  const endColumn = match[3] ? columnToIndex(match[3]) : startColumn
  const endRow = match[4] ? Number(match[4]) - 1 : startRow
  return {
    startRow: Math.min(startRow, endRow),
    startColumn: Math.min(startColumn, endColumn),
    endRow: Math.max(startRow, endRow),
    endColumn: Math.max(startColumn, endColumn)
  }
}

// ─── Doküman API'si (§9.1) ─────────────────────────────────────────────────

function cellKey(row, column) {
  return `R${row}C${column}`
}

function createRange(sheet, range) {
  const rows = range.endRow - range.startRow + 1
  const columns = range.endColumn - range.startColumn + 1

  const api = {
    getA1Notation() {
      return `${indexToColumn(range.startColumn)}${range.startRow + 1}:`
        + `${indexToColumn(range.endColumn)}${range.endRow + 1}`
    },

    getValues() {
      const result = []
      for (let r = 0; r < rows; r++) {
        const line = []
        for (let c = 0; c < columns; c++) {
          const cell = sheet.cells[cellKey(range.startRow + r, range.startColumn + c)]
          line.push(cell ? (cell.v ?? null) : null)
        }
        result.push(line)
      }
      return result
    },

    getValue() {
      return api.getValues()[0][0]
    },

    setValues(matrix) {
      if (!Array.isArray(matrix)) throw new Error('setValues bir matris bekler')
      matrix.forEach((line, r) => {
        if (!Array.isArray(line)) return
        line.forEach((value, c) => {
          // Anlık görüntü de güncelleniyor: makronun bir sonraki getValues()
          // çağrısı kendi yazdığını görmeli, yoksa iki adımlı betikler
          // sessizce eski değerle çalışır.
          const key = cellKey(range.startRow + r, range.startColumn + c)
          sheet.cells[key] = { ...(sheet.cells[key] || {}), v: value, f: undefined }
          ops.push({
            op: 'setCell', sheetId: sheet.id,
            row: range.startRow + r, column: range.startColumn + c,
            cell: { v: value }
          })
        })
      })
      return api
    },

    setValue(value) {
      return api.setValues([[value]])
    },

    setFormula(formula) {
      const key = cellKey(range.startRow, range.startColumn)
      sheet.cells[key] = { ...(sheet.cells[key] || {}), f: formula }
      ops.push({
        op: 'setCell', sheetId: sheet.id,
        row: range.startRow, column: range.startColumn,
        cell: { f: formula }
      })
      return api
    },

    setStyle(style) {
      for (let r = 0; r < rows; r++) {
        for (let c = 0; c < columns; c++) {
          ops.push({
            op: 'setStyle', sheetId: sheet.id,
            row: range.startRow + r, column: range.startColumn + c,
            style
          })
        }
      }
      return api
    }
  }
  return api
}

function createSheet(sheet) {
  return {
    getName: () => sheet.name,
    getSheetId: () => sheet.id,
    getLastRow: () => sheet.rowCount,
    getLastColumn: () => sheet.colCount,

    getRange(a1OrRow, column, numRows, numColumns) {
      const range = typeof a1OrRow === 'string'
        ? parseA1(a1OrRow)
        : {
            startRow: a1OrRow,
            startColumn: column,
            endRow: a1OrRow + (numRows || 1) - 1,
            endColumn: column + (numColumns || 1) - 1
          }
      return createRange(sheet, range)
    },

    insertRows(rowIndex, count = 1) {
      ops.push({ op: 'insertRows', sheetId: sheet.id, rowIndex, count })
    },

    deleteRows(rowIndex, count = 1) {
      ops.push({ op: 'deleteRows', sheetId: sheet.id, rowIndex, count })
    },

    insertColumns(columnIndex, count = 1) {
      ops.push({ op: 'insertColumns', sheetId: sheet.id, columnIndex, count })
    },

    deleteColumns(columnIndex, count = 1) {
      ops.push({ op: 'deleteColumns', sheetId: sheet.id, columnIndex, count })
    },

    /**
     * Sıralama sunucuda ya da ızgarada değil **burada** yapılıyor: sonuç zaten
     * hücre yazımına dönüşüyor, ayrı bir sıralama işlemi tanımlamak köprüde
     * ikinci bir yol açardı.
     */
    sort({ column, ascending = true, range: target } = {}) {
      const area = target ? parseA1(target) : {
        startRow: 0, startColumn: 0,
        endRow: sheet.rowCount - 1, endColumn: sheet.colCount - 1
      }
      const rangeApi = createRange(sheet, area)
      const values = rangeApi.getValues()
      const keyIndex = (column ?? area.startColumn) - area.startColumn

      values.sort((a, b) => {
        const left = a[keyIndex]
        const right = b[keyIndex]
        if (left === right) return 0
        if (left == null) return 1
        if (right == null) return -1
        const result = left > right ? 1 : -1
        return ascending ? result : -result
      })
      rangeApi.setValues(values)
    }
  }
}

function createDocument(snapshot) {
  const sheets = (snapshot.sheets || []).map(createSheet)

  return {
    getType: () => snapshot.type,
    getTitle: () => snapshot.title,

    getSheets: () => sheets,
    getActiveSheet: () => sheets[0] || null,
    getSheetByName: (name) => sheets.find((sheet) => sheet.getName() === name) || null,

    getText: () => snapshot.text || '',

    setText(text) {
      snapshot.text = String(text)
      ops.push({ op: 'setText', text: snapshot.text })
    },

    replaceText(pattern, replacement) {
      const next = String(snapshot.text || '').replace(pattern, replacement)
      snapshot.text = next
      ops.push({ op: 'setText', text: next })
      return next
    }
  }
}

// ─── ScrumTools global'i ───────────────────────────────────────────────────

function buildApi(snapshot) {
  const doc = createDocument(snapshot)

  const api = {
    getActiveDocument: () => doc,

    // Veri köprüleri ana iş parçacığına gider ve orada kullanıcının kendi
    // oturumuyla mevcut REST uçlarına çevrilir. Bu yüzden **asenkron**:
    // §9.1'deki taslak senkron gösteriyordu ama K8, eşzamanlı RPC'yi açıkça
    // reddediyor — `await` kullanmak, sayfayı izole moda sokmaktan iyidir.
    tasks: {
      query: (tql, options) => hostCall('tasks', 'query', [tql, options])
    },
    sprints: {
      current: (teamId) => hostCall('sprints', 'current', [teamId])
    },
    docs: {
      getPage: (pageId) => hostCall('docs', 'getPage', [pageId]),
      savePage: (spaceId, title, html) => hostCall('docs', 'savePage', [spaceId, title, html])
    },
    http: {
      fetch: (url, options) => hostCall('http', 'fetch', [url, options])
    },
    ui: {
      toast: (message, type) => hostCall('ui', 'toast', [message, type]),
      alert: (message) => hostCall('ui', 'alert', [message]),
      prompt: (message, defaultValue) => hostCall('ui', 'prompt', [message, defaultValue])
    },
    utils: {
      formatDate(value, pattern = 'dd.MM.yyyy') {
        const date = value instanceof Date ? value : new Date(value)
        if (Number.isNaN(date.getTime())) return ''
        const pad = (n) => String(n).padStart(2, '0')
        return pattern
          .replace('yyyy', date.getFullYear())
          .replace('MM', pad(date.getMonth() + 1))
          .replace('dd', pad(date.getDate()))
          .replace('HH', pad(date.getHours()))
          .replace('mm', pad(date.getMinutes()))
      }
    }
  }

  // Dondurma, kazayla API'yi ezen betiklere karşı: `ScrumTools.tasks = ...`
  // yazan bir makro, sonraki çalıştırmada başka bir makroyu etkileyemesin.
  Object.freeze(api.tasks)
  Object.freeze(api.sprints)
  Object.freeze(api.docs)
  Object.freeze(api.http)
  Object.freeze(api.ui)
  Object.freeze(api.utils)
  return Object.freeze(api)
}

// ─── Mesaj döngüsü ─────────────────────────────────────────────────────────

self.onmessage = async (event) => {
  const message = event.data

  if (message.type === 'callResult') {
    const entry = pending.get(message.id)
    if (!entry) return
    pending.delete(message.id)
    if (message.error) entry.reject(new Error(message.error))
    else entry.resolve(message.value)
    return
  }

  if (message.type !== 'run') return

  logLines = []
  ops = []
  const started = Date.now()

  try {
    const api = buildApi(message.snapshot || {})
    const console = Object.freeze({ log, info: log, warn: log, error: log, debug: log })
    const fn = new AsyncFunction('ScrumTools', 'console', message.source)
    await fn(api, console)

    self.postMessage({
      type: 'result',
      status: 'SUCCESS',
      ops,
      log: logLines.join('\n'),
      durationMs: Date.now() - started
    })
  } catch (error) {
    self.postMessage({
      type: 'result',
      status: 'FAILED',
      // Hata anına kadar biriken işlemler **uygulanmaz**: yarım çalışmış bir
      // makronun dokümanı tutarsız bırakması, hiç çalışmamasından kötüdür.
      ops: [],
      log: logLines.join('\n'),
      error: error?.stack || String(error),
      durationMs: Date.now() - started
    })
  }
}
