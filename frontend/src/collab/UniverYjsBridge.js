import * as Y from 'yjs'
import { cellKey, parseCellKey } from './cellAddress.js'
import { CommandType } from '@univerjs/core'
import {
  SetRangeValuesMutation,
  SetWorksheetRowHeightMutation,
  SetWorksheetColWidthMutation,
  AddWorksheetMergeMutation,
  RemoveWorksheetMergeMutation,
  SetWorksheetNameMutation,
  SetWorksheetOrderMutation,
  InsertSheetMutation,
  RemoveSheetMutation,
  SetSelectionsOperation,
  IMarkSelectionService
} from '@univerjs/preset-sheets-core'

/**
 * Univer ↔ Yjs iki yönlü köprüsü (COLLAB_WORKSPACE_PLAN.md K4 / §5, Faz 3).
 *
 * Planın en riskli işi buydu; tasarımın dayandığı üç karar:
 *
 * 1. **CRDT'de Univer'in mutation'ları değil, kendi hücre modelimiz durur.**
 *    Ham mutation akışını kaydetmek, doğruluk kaynağını Univer'in iç
 *    sözleşmesine bağlar; sürüm yükseltmesi geçmişi okunamaz hâle getirirdi
 *    (R1). Burada CRDT §5'teki `"R{satır}C{sütun}"` sözlüğüdür, Univer ise
 *    yalnızca bir görüntüleyici/düzenleyici.
 *
 * 2. **Hücre başına düz nesne, iç içe `Y.Map` değil.** §5'in taslağı hücreyi
 *    `Y.Map` olarak çiziyordu; bu, alan bazlı birleşme kazandırır ama 200 bin
 *    hücrede 200 bin CRDT tipi demektir — tarayıcı belleği buna dayanmaz.
 *    Karşılığında hücre düzeyinde "son yazan kazanır" davranışı kabul edildi;
 *    hesap tablolarında beklenen davranış zaten budur.
 *
 * 3. **Yoğun akış mutation'la, seyrek yapı değişikliği Facade ile.** Hücre
 *    yazımı saniyede onlarca kez olur ve doğrudan `SetRangeValuesMutation`
 *    uygulanır (komut değil mutation: geri alma yığınına girmez). Sayfa
 *    ekleme/silme/yeniden adlandırma nadirdir ve belgelenmiş Facade API'siyle
 *    yapılır — iç anlık görüntü nakletmekten çok daha dayanıklı.
 */

/** Kendi yazdığımız Yjs işlemlerini gözlemcide tanımak için. */
const LOCAL_ORIGIN = 'univer-bridge'

/** CRDT boşken her istemcinin ürettiği sayfa kimliği — sabit olması şart. */
const DEFAULT_SHEET_ID = 'default'

const DEFAULT_ROWS = 200
const DEFAULT_COLS = 40

/** Univer `ICellData` içinden CRDT'ye taşıdığımız alanlar. */
const CELL_FIELDS = ['v', 'f', 'si', 't', 's']

// Hücre anahtarı yardımcıları ayrı bir modülde: bu dosya Univer'i statik import
// ediyor, dolayısıyla buradan bir fonksiyon almak Univer'in tamamını çağıranın
// paketine sokar (bkz. cellAddress.js). Yeniden dışa aktarılıyorlar ki mevcut
// çağıranlar için bu dosyanın API'si değişmesin.
export { cellKey, parseCellKey }

export class UniverYjsBridge {
  constructor({ ydoc, univer, univerAPI, awareness, unitId }) {
    this.ydoc = ydoc
    this.univer = univer
    this.univerAPI = univerAPI
    this.awareness = awareness
    this.unitId = unitId

    this.meta = ydoc.getMap('meta')

    /** Uzaktan gelen değişiklik uygulanırken yerel dinleyici susar. */
    this.applyingRemote = false
    this.disposers = []
    this.sheetObservers = new Map()
    this.remoteCursorShapes = new Map()
    this.destroyed = false
  }

  // ─── Kurulum ───────────────────────────────────────────────────────────────

  /** CRDT'den çalışma kitabı verisi üretir; boşsa varsayılan tek sayfa döner. */
  buildWorkbookData() {
    const order = this.sheetOrder()
    const sheets = {}
    const sheetOrder = []

    if (order.length === 0) {
      sheets[DEFAULT_SHEET_ID] = this.emptySheetData(DEFAULT_SHEET_ID, 'Sayfa1')
      sheetOrder.push(DEFAULT_SHEET_ID)
    } else {
      for (const id of order) {
        sheets[id] = this.readSheetData(id)
        sheetOrder.push(id)
      }
    }

    return {
      id: this.unitId,
      name: this.meta.get('title') || 'Tablo',
      sheetOrder,
      sheets,
      // Stil havuzu bilinçli olarak boş: stiller hücrenin içinde satır içi
      // nesne olarak taşınıyor (aşağıdaki `styleToUniver` notuna bakın).
      styles: {}
    }
  }

  /** Köprüyü çalıştırır: önce CRDT→Univer, sonra iki yönlü dinleyiciler. */
  start(workbook) {
    this.workbook = workbook
    this.attachUniverListener()
    this.attachYjsListeners()
  }

  destroy() {
    this.destroyed = true
    this.disposers.forEach((dispose) => {
      try { dispose() } catch { /* kapanışta hata yutulur */ }
    })
    this.disposers = []
    this.sheetObservers.clear()
    this.clearRemoteCursors()
  }

  // ─── Univer → Yjs ─────────────────────────────────────────────────────────

  attachUniverListener() {
    const subscription = this.univerAPI.onCommandExecuted((command) => {
      if (this.applyingRemote || this.destroyed) return

      // Seçim bir mutation değil operation; imleç paylaşımı için ayrı ele alınıyor.
      if (command.id === SetSelectionsOperation.id) {
        this.publishSelection(command.params)
        return
      }
      if (command.type !== CommandType.MUTATION) return
      if (command.params?.unitId && command.params.unitId !== this.unitId) return

      switch (command.id) {
        case SetRangeValuesMutation.id:
          this.onLocalCellValues(command.params)
          break
        case SetWorksheetRowHeightMutation.id:
          this.onLocalSize(command.params, 'rows', 'h', command.params?.rowHeight)
          break
        case SetWorksheetColWidthMutation.id:
          this.onLocalSize(command.params, 'cols', 'w', command.params?.colWidth)
          break
        case AddWorksheetMergeMutation.id:
          this.onLocalMerge(command.params, true)
          break
        case RemoveWorksheetMergeMutation.id:
          this.onLocalMerge(command.params, false)
          break
        case SetWorksheetNameMutation.id:
          this.onLocalSheetRename(command.params)
          break
        case InsertSheetMutation.id:
          this.onLocalSheetInsert(command.params)
          break
        case RemoveSheetMutation.id:
          this.onLocalSheetRemove(command.params)
          break
        case SetWorksheetOrderMutation.id:
          this.onLocalSheetOrder(command.params)
          break
        default:
          break
      }
    })
    this.disposers.push(() => subscription?.dispose?.())
  }

  onLocalCellValues(params) {
    const { subUnitId, cellValue } = params || {}
    if (!subUnitId || !cellValue) return
    const cells = this.sheetCells(subUnitId)

    this.transact(() => {
      for (const rowKey of Object.keys(cellValue)) {
        const row = Number(rowKey)
        const columns = cellValue[rowKey]
        if (!columns) continue
        for (const colKey of Object.keys(columns)) {
          const col = Number(colKey)
          const cell = columns[colKey]
          const key = cellKey(row, col)
          const normalized = normalizeCell(cell)
          if (normalized === null) {
            cells.delete(key)
          } else {
            cells.set(key, normalized)
          }
        }
      }
    })
  }

  onLocalSize(params, field, property, size) {
    const { subUnitId, ranges } = params || {}
    if (!subUnitId || !Array.isArray(ranges)) return
    const target = this.sheetChild(subUnitId, field)

    this.transact(() => {
      for (const range of ranges) {
        const from = field === 'rows' ? range.startRow : range.startColumn
        const to = field === 'rows' ? range.endRow : range.endColumn
        for (let index = from; index <= to; index++) {
          const value = typeof size === 'number' ? size : size?.[index]
          if (value == null) continue
          const current = target.get(String(index)) || {}
          target.set(String(index), { ...current, [property]: Math.round(value) })
        }
      }
    })
  }

  onLocalMerge(params, add) {
    const { subUnitId, ranges } = params || {}
    if (!subUnitId || !Array.isArray(ranges)) return
    const merges = this.sheetMerges(subUnitId)

    this.transact(() => {
      for (const range of ranges) {
        const entry = {
          r: range.startRow,
          c: range.startColumn,
          rs: range.endRow - range.startRow + 1,
          cs: range.endColumn - range.startColumn + 1
        }
        const existing = merges.toArray().findIndex((m) => m.r === entry.r && m.c === entry.c)
        if (add) {
          if (existing < 0) merges.push([entry])
        } else if (existing >= 0) {
          merges.delete(existing, 1)
        }
      }
    })
  }

  onLocalSheetRename(params) {
    const { subUnitId, name } = params || {}
    if (!subUnitId) return
    this.transact(() => {
      const entry = this.sheetMeta(subUnitId)
      entry.set('name', name || 'Sayfa')
    })
  }

  onLocalSheetInsert(params) {
    const sheet = params?.sheet
    if (!sheet?.id) return
    this.transact(() => {
      const entry = this.sheetMeta(sheet.id)
      entry.set('name', sheet.name || 'Sayfa')
      entry.set('rowCount', sheet.rowCount || DEFAULT_ROWS)
      entry.set('colCount', sheet.columnCount || DEFAULT_COLS)

      const order = this.orderArray()
      if (!order.toArray().includes(sheet.id)) {
        const index = Math.min(Math.max(params.index ?? order.length, 0), order.length)
        order.insert(index, [sheet.id])
      }
    })
  }

  onLocalSheetRemove(params) {
    const { subUnitId } = params || {}
    if (!subUnitId) return
    this.transact(() => {
      const order = this.orderArray()
      const index = order.toArray().indexOf(subUnitId)
      if (index >= 0) order.delete(index, 1)
      this.sheetsMeta().delete(subUnitId)
      // Hücre verisi de temizlenir; bırakılırsa doküman sonsuza dek büyür (R3).
      this.sheetCells(subUnitId).clear()
    })
  }

  onLocalSheetOrder(params) {
    const { fromOrder, toOrder } = params || {}
    if (fromOrder == null || toOrder == null) return
    this.transact(() => {
      const order = this.orderArray()
      const values = order.toArray()
      if (fromOrder < 0 || fromOrder >= values.length) return
      const [moved] = values.splice(fromOrder, 1)
      values.splice(Math.min(toOrder, values.length), 0, moved)
      order.delete(0, order.length)
      order.insert(0, values)
    })
  }

  // ─── Yjs → Univer ─────────────────────────────────────────────────────────

  attachYjsListeners() {
    const onOrder = (event, transaction) => {
      if (transaction.origin === LOCAL_ORIGIN) return
      this.syncSheetList()
    }
    this.orderArray().observe(onOrder)
    this.disposers.push(() => this.orderArray().unobserve(onOrder))

    const onSheetsMeta = (events, transaction) => {
      if (transaction.origin === LOCAL_ORIGIN) return
      this.syncSheetNames()
    }
    this.sheetsMeta().observeDeep(onSheetsMeta)
    this.disposers.push(() => this.sheetsMeta().unobserveDeep(onSheetsMeta))

    for (const id of this.sheetOrder()) this.observeSheet(id)
    if (this.sheetOrder().length === 0) this.observeSheet(DEFAULT_SHEET_ID)
  }

  observeSheet(sheetId) {
    if (this.sheetObservers.has(sheetId)) return

    const cells = this.sheetCells(sheetId)
    const onCells = (event, transaction) => {
      if (transaction.origin === LOCAL_ORIGIN || this.destroyed) return
      this.applyRemoteCells(sheetId, event.changes.keys)
    }
    cells.observe(onCells)

    const rows = this.sheetChild(sheetId, 'rows')
    const onRows = (event, transaction) => {
      if (transaction.origin === LOCAL_ORIGIN || this.destroyed) return
      this.applyRemoteSizes(sheetId, rows, event.changes.keys, 'rows')
    }
    rows.observe(onRows)

    const cols = this.sheetChild(sheetId, 'cols')
    const onCols = (event, transaction) => {
      if (transaction.origin === LOCAL_ORIGIN || this.destroyed) return
      this.applyRemoteSizes(sheetId, cols, event.changes.keys, 'cols')
    }
    cols.observe(onCols)

    const merges = this.sheetMerges(sheetId)
    const onMerges = (event, transaction) => {
      if (transaction.origin === LOCAL_ORIGIN || this.destroyed) return
      this.applyRemoteMerges(sheetId, event)
    }
    merges.observe(onMerges)

    const dispose = () => {
      cells.unobserve(onCells)
      rows.unobserve(onRows)
      cols.unobserve(onCols)
      merges.unobserve(onMerges)
    }
    this.sheetObservers.set(sheetId, dispose)
    this.disposers.push(dispose)
  }

  applyRemoteCells(sheetId, changedKeys) {
    const cells = this.sheetCells(sheetId)
    const cellValue = {}
    let count = 0

    changedKeys.forEach((_change, key) => {
      const position = parseCellKey(key)
      if (!position) return
      const value = cells.get(key)
      cellValue[position.row] = cellValue[position.row] || {}
      // Silinen hücre `null` olarak gider: Univer'de null "hücreyi temizle" demek.
      cellValue[position.row][position.col] = value ? { ...value } : null
      count++
    })

    if (count === 0) return
    this.applyLocally(SetRangeValuesMutation.id, {
      unitId: this.unitId,
      subUnitId: sheetId,
      cellValue
    })
  }

  applyRemoteSizes(sheetId, source, changedKeys, kind) {
    changedKeys.forEach((_change, key) => {
      const index = Number(key)
      if (!Number.isFinite(index)) return
      const meta = source.get(key)
      if (!meta) return

      const range = kind === 'rows'
        ? { startRow: index, endRow: index, startColumn: 0, endColumn: 0 }
        : { startRow: 0, endRow: 0, startColumn: index, endColumn: index }

      if (kind === 'rows' && meta.h != null) {
        this.applyLocally(SetWorksheetRowHeightMutation.id, {
          unitId: this.unitId, subUnitId: sheetId, ranges: [range], rowHeight: meta.h
        })
      } else if (kind === 'cols' && meta.w != null) {
        this.applyLocally(SetWorksheetColWidthMutation.id, {
          unitId: this.unitId, subUnitId: sheetId, ranges: [range], colWidth: meta.w
        })
      }
    })
  }

  applyRemoteMerges(sheetId, event) {
    // Birleşimler seyrek değişir; farkı hesaplamak yerine hedef durumu kuruyoruz:
    // önce mevcut birleşimler kaldırılıyor, sonra CRDT'deki liste uygulanıyor.
    if (!event.changes.added.size && !event.changes.deleted.size) return
    const merges = this.sheetMerges(sheetId).toArray()
    const sheet = this.worksheet(sheetId)
    if (!sheet) return

    // getMergeData() Facade nesnesi (FRange) döndürür; mutation ham IRange ister.
    const existing = (sheet.getMergeData?.() || []).map((range) => range.getRange())
    if (existing.length) {
      this.applyLocally(RemoveWorksheetMergeMutation.id, {
        unitId: this.unitId, subUnitId: sheetId, ranges: existing
      })
    }
    if (merges.length) {
      this.applyLocally(AddWorksheetMergeMutation.id, {
        unitId: this.unitId,
        subUnitId: sheetId,
        ranges: merges.map((m) => ({
          startRow: m.r, endRow: m.r + m.rs - 1,
          startColumn: m.c, endColumn: m.c + m.cs - 1
        }))
      })
    }
  }

  /**
   * Sayfa listesini CRDT'ye göre hizalar.
   *
   * Facade kullanılıyor (mutation değil): sayfa oluşturmak, Univer'in iç
   * anlık görüntü biçimini elle üretmeyi gerektirir ve o biçim sürümler arası
   * değişebilir (R1). `create()` ise belgelenmiş, sürüm garantili API.
   */
  syncSheetList() {
    if (!this.workbook || this.destroyed) return
    const desired = this.sheetOrder()
    if (desired.length === 0) return

    this.applyingRemote = true
    try {
      const existing = new Set(this.workbook.getSheets().map((sheet) => sheet.getSheetId()))

      for (const id of desired) {
        if (existing.has(id)) continue
        const meta = this.sheetsMeta().get(id)
        const name = meta?.get?.('name') || 'Sayfa'
        this.workbook.create(
          name,
          meta?.get?.('rowCount') || DEFAULT_ROWS,
          meta?.get?.('colCount') || DEFAULT_COLS,
          { index: desired.indexOf(id), sheet: { id } })
        this.observeSheet(id)
      }

      for (const sheet of this.workbook.getSheets()) {
        if (!desired.includes(sheet.getSheetId())) {
          this.workbook.deleteSheet(sheet)
        }
      }
    } catch (error) {
      console.warn('[collab] sayfa listesi eşitlenemedi', error)
    } finally {
      this.applyingRemote = false
    }
  }

  syncSheetNames() {
    if (!this.workbook || this.destroyed) return
    this.applyingRemote = true
    try {
      for (const sheet of this.workbook.getSheets()) {
        const meta = this.sheetsMeta().get(sheet.getSheetId())
        const name = meta?.get?.('name')
        if (name && name !== sheet.getSheetName()) sheet.setName(name)
      }
    } catch (error) {
      console.warn('[collab] sayfa adları eşitlenemedi', error)
    } finally {
      this.applyingRemote = false
    }
  }

  /**
   * Uzaktan gelen değişikliği yerel olarak uygular.
   *
   * `syncExecuteCommand` + `onlyLocal` bilinçli: komut değil mutation
   * çalıştırıldığı için geri alma yığınına girmez — uzaktan gelen bir düzenlemeyi
   * Ctrl+Z ile geri almak, o düzenlemeyi yapan kişinin metnini silmek demekti.
   */
  applyLocally(mutationId, params) {
    this.applyingRemote = true
    try {
      this.univerAPI.syncExecuteCommand(mutationId, params, { onlyLocal: true })
    } catch (error) {
      console.warn('[collab] uzak değişiklik uygulanamadı', mutationId, error)
    } finally {
      this.applyingRemote = false
    }
  }

  /**
   * Makro kaydedicisi için (§9.3): şu an uzak bir değişiklik mi uygulanıyor.
   *
   * Kaydedici bu bayrak açıkken susmak zorunda — aksi hâlde yanınızda çalışan
   * birinin düzenlemeleri sizin makronuza yazılırdı.
   */
  isApplyingRemote() {
    return this.applyingRemote === true
  }

  // ─── Awareness: hücre imleçleri ───────────────────────────────────────────

  publishSelection(params) {
    if (!this.awareness) return
    const selection = params?.selections?.[0]?.range
    if (!selection || !params.subUnitId) return
    this.awareness.setLocalStateField('cell', {
      sheetId: params.subUnitId,
      startRow: selection.startRow,
      startColumn: selection.startColumn,
      endRow: selection.endRow,
      endColumn: selection.endColumn
    })
  }

  /**
   * Uzaktaki imleçleri ızgaraya çizer.
   *
   * `IMarkSelectionService` Univer'in genel API'si değil, DI üzerinden alınan bir
   * servistir — bu yüzden çözülemezse sessizce vazgeçiliyor: bir Univer
   * yükseltmesi bu kozmetik özelliği kapatabilir ama düzenlemeyi bozmamalı (R1).
   */
  renderRemoteCursors(states) {
    const service = this.markSelectionService()
    if (!service) return

    this.clearRemoteCursors()
    const activeSheetId = this.workbook?.getActiveSheet?.()?.getSheetId?.()

    for (const [clientId, state] of states) {
      const cell = state?.cell
      if (!cell || cell.sheetId !== activeSheetId) continue
      try {
        const id = service.addShape({
          range: {
            startRow: cell.startRow,
            endRow: cell.endRow,
            startColumn: cell.startColumn,
            endColumn: cell.endColumn
          },
          primary: null,
          style: {
            strokeWidth: 2,
            stroke: state.user?.color || '#6366f1',
            fill: 'rgba(99, 102, 241, 0.08)',
            widgets: {},
            hasAutoFill: false
          }
        })
        if (id) this.remoteCursorShapes.set(clientId, id)
      } catch {
        // tek bir imleç çizilemezse diğerleri denenmeye devam eder
      }
    }
  }

  clearRemoteCursors() {
    const service = this.markSelectionService()
    if (!service) {
      this.remoteCursorShapes.clear()
      return
    }
    for (const id of this.remoteCursorShapes.values()) {
      try { service.removeShape(id) } catch { /* zaten kaldırılmış olabilir */ }
    }
    this.remoteCursorShapes.clear()
  }

  markSelectionService() {
    if (this._markService !== undefined) return this._markService
    try {
      // __getInjector() Univer'in iç API'si (alt çizgili). Bir yükseltmede
      // kaybolursa uzak imleçler kaybolur, düzenleme çalışmaya devam eder.
      this._markService = this.univer.__getInjector().get(IMarkSelectionService)
    } catch {
      this._markService = null
    }
    return this._markService
  }

  // ─── Anlık görüntü (K6) ───────────────────────────────────────────────────

  /**
   * Sunucuya gönderilen okunabilir JSON'u üretir.
   *
   * Değerler CRDT'den değil **Univer'den** okunur: formül sonuçları CRDT'de
   * saklanmıyor (K5), oysa Excel'e aktarım için hesaplanmış değer gerekiyor.
   */
  toSnapshotJson() {
    const sheets = []
    const order = this.sheetOrder()
    const ids = order.length ? order : [DEFAULT_SHEET_ID]

    // Sunucu sözleşmesi (§5): hücredeki `s` bir stil **anahtarı**dır. CRDT'de
    // stil satır içi nesne olarak durduğu için burada havuza çevriliyor.
    const styles = {}
    const styleKeys = new Map()

    for (const id of ids) {
      const cells = {}
      const source = this.sheetCells(id)
      const worksheet = this.worksheet(id)

      source.forEach((value, key) => {
        const position = parseCellKey(key)
        if (!position) return
        const computed = worksheet
          ? worksheet.getRange(position.row, position.col).getValue()
          : undefined
        cells[key] = {
          ...value,
          // Formül hücresinde CRDT'de değer yok; hesaplanmışı buraya koyuyoruz.
          v: value.f != null && computed != null ? computed : value.v,
          s: styleKey(value.s, styles, styleKeys)
        }
        if (cells[key].s == null) delete cells[key].s
      })

      const meta = this.sheetsMeta().get(id)
      sheets.push({
        id,
        name: meta?.get?.('name') || worksheet?.getSheetName?.() || 'Sayfa1',
        rowCount: meta?.get?.('rowCount') || DEFAULT_ROWS,
        colCount: meta?.get?.('colCount') || DEFAULT_COLS,
        cells,
        rows: mapToObject(this.sheetChild(id, 'rows')),
        cols: mapToObject(this.sheetChild(id, 'cols')),
        merges: this.sheetMerges(id).toArray()
      })
    }

    return JSON.stringify({ version: 1, sheets, styles })
  }

  /**
   * Sunucudan gelen tablo modelini CRDT'ye yazar — içe aktarma tohumlaması (§10).
   *
   * Tek işlem hâlinde: parça parça yazılsaydı diğer istemciler yarım bir tablo
   * görür ve o arada yazan biri henüz gelmemiş hücrelerin üstüne yazardı.
   */
  seedFromModel(model) {
    if (!model || !Array.isArray(model.sheets) || model.sheets.length === 0) return

    this.transact(() => {
      const order = this.orderArray()
      order.delete(0, order.length)

      for (const sheet of model.sheets) {
        const id = sheet.id || DEFAULT_SHEET_ID
        order.push([id])

        const meta = this.sheetMeta(id)
        meta.set('name', sheet.name || 'Sayfa1')
        meta.set('rowCount', sheet.rowCount || DEFAULT_ROWS)
        meta.set('colCount', sheet.colCount || DEFAULT_COLS)

        const cells = this.sheetCells(id)
        cells.clear()
        for (const [key, cell] of Object.entries(sheet.cells || {})) {
          const normalized = normalizeCell(cell)
          if (!normalized) continue
          // Sunucudaki stil anahtarı Univer'in anlayacağı satır içi nesneye
          // çevriliyor; havuz taşımak iki tarafta da eşitlenmesi gereken ikinci
          // bir durum demekti.
          const style = styleToUniver(model.styles?.[cell.s])
          if (style) normalized.s = style
          else delete normalized.s
          cells.set(key, normalized)
        }

        const rows = this.sheetChild(id, 'rows')
        rows.clear()
        for (const [key, value] of Object.entries(sheet.rows || {})) rows.set(key, value)

        const cols = this.sheetChild(id, 'cols')
        cols.clear()
        for (const [key, value] of Object.entries(sheet.cols || {})) cols.set(key, value)

        const merges = this.sheetMerges(id)
        merges.delete(0, merges.length)
        if (Array.isArray(sheet.merges) && sheet.merges.length) merges.push(sheet.merges)
      }
    })
  }

  // ─── Makro işlemleri (plan §9.1 / K8) ─────────────────────────────────────

  /**
   * Makronun ürettiği işlem listesini uygular.
   *
   * <p>Yazma origin'i {@code LOCAL_ORIGIN} <b>değil</b>: böylece kendi Yjs
   * gözlemcilerimiz de tetiklenir ve değişiklik Univer'e uygulanır. Aksi hâlde
   * veri CRDT'ye girer, ağdaki herkes görür ama makroyu çalıştıran kişinin
   * ekranı güncellenmezdi.
   *
   * <p>Tümü tek transaction: makro yarım uygulanırsa doküman tutarsız kalır ve
   * o arada yazan biri henüz gelmemiş hücrelerin üstüne yazar.
   */
  applyMacroOps(ops) {
    if (!Array.isArray(ops) || ops.length === 0) return
    this.ydoc.transact(() => {
      for (const op of ops) {
        const sheetId = op.sheetId || this.sheetOrder()[0] || DEFAULT_SHEET_ID
        switch (op.op) {
          case 'setCell': {
            const cells = this.sheetCells(sheetId)
            const key = cellKey(op.row, op.column)
            const merged = normalizeCell({ ...(cells.get(key) || {}), ...op.cell })
            if (merged) cells.set(key, merged)
            else cells.delete(key)
            break
          }
          case 'setStyle': {
            const cells = this.sheetCells(sheetId)
            const key = cellKey(op.row, op.column)
            const current = cells.get(key)
            if (!current) break
            cells.set(key, { ...current, s: { ...(current.s || {}), ...macroStyleToUniver(op.style) } })
            break
          }
          case 'insertRows':
            this.shiftCells(sheetId, 'row', op.rowIndex, op.count || 1)
            break
          case 'deleteRows':
            this.shiftCells(sheetId, 'row', op.rowIndex, -(op.count || 1))
            break
          case 'insertColumns':
            this.shiftCells(sheetId, 'column', op.columnIndex, op.count || 1)
            break
          case 'deleteColumns':
            this.shiftCells(sheetId, 'column', op.columnIndex, -(op.count || 1))
            break
          default:
            break
        }
      }
    }, 'macro')
  }

  /**
   * Satır/sütun ekleme-silme: anahtarları yeniden yazar (§5).
   *
   * <p>Hücreler dizi değil {@code "R{r}C{c}"} sözlüğü olduğu için indeks kaydırma
   * otomatik değil; §5 bunu "anahtarları yeniden yazan tek bir işlem" olarak
   * tanımlıyor. Diziyle çalışsaydık her ekleme sonraki tüm indeksleri kaydırır
   * ve eşzamanlı düzenlemede çakışmaları çoğaltırdı.
   */
  shiftCells(sheetId, axis, from, delta) {
    const cells = this.sheetCells(sheetId)
    const entries = []
    cells.forEach((value, key) => {
      const position = parseCellKey(key)
      if (position) entries.push({ key, position, value })
    })

    // Silinen aralıktaki hücreler önce kaldırılır, sonra kalanlar kaydırılır.
    for (const entry of entries) {
      const index = axis === 'row' ? entry.position.row : entry.position.col
      if (index < from) continue
      if (delta < 0 && index < from - delta) {
        cells.delete(entry.key)
        continue
      }
      const row = axis === 'row' ? entry.position.row + delta : entry.position.row
      const col = axis === 'row' ? entry.position.col : entry.position.col + delta
      cells.delete(entry.key)
      cells.set(cellKey(row, col), entry.value)
    }
  }

  // ─── CRDT erişim yardımcıları ─────────────────────────────────────────────

  transact(fn) {
    this.ydoc.transact(fn, LOCAL_ORIGIN)
  }

  /*
   * Kaplar **üst düzey** Yjs tipleridir (`ydoc.getMap` / `ydoc.getArray`).
   *
   * §5'in taslağı hepsini `meta` ve `sheet:<id>` altında iç içe çiziyordu; öyle
   * yapılamadı çünkü iç içe bir tipi oluşturmak CRDT'ye **yazmak** demek.
   * Dokümanı yalnızca görüntülemek için açan istemci bile boş kapları yazar,
   * `last_seq` artar ve sunucudaki tohumlama kilidi (`last_seq = 0`) bir daha
   * asla açılmaz — Excel'den içe aktarılan tablo hiçbir zaman yüklenmezdi.
   * Üst düzey tipe erişmek ise tamamen yereldir, tek bayt üretmez.
   */
  orderArray() {
    return this.ydoc.getArray('sheetOrder')
  }

  /** Yinelenenler ayıklanır: CRDT boşken iki istemci aynı kimliği ekleyebilir. */
  sheetOrder() {
    return [...new Set(this.orderArray().toArray())]
  }

  sheetsMeta() {
    return this.ydoc.getMap('sheets')
  }

  /** Yalnızca yazma yollarından çağrılır — iç içe tip oluşturmak bir güncellemedir. */
  sheetMeta(sheetId) {
    const sheets = this.sheetsMeta()
    let entry = sheets.get(sheetId)
    if (!(entry instanceof Y.Map)) {
      entry = new Y.Map()
      sheets.set(sheetId, entry)
    }
    return entry
  }

  sheetChild(sheetId, name) {
    return this.ydoc.getMap(`sheet:${sheetId}:${name}`)
  }

  sheetCells(sheetId) {
    return this.sheetChild(sheetId, 'cells')
  }

  sheetMerges(sheetId) {
    return this.ydoc.getArray(`sheet:${sheetId}:merges`)
  }

  readSheetData(sheetId) {
    const meta = this.sheetsMeta().get(sheetId)
    const data = this.emptySheetData(
      sheetId,
      meta?.get?.('name') || 'Sayfa1',
      meta?.get?.('rowCount'),
      meta?.get?.('colCount'))

    this.sheetCells(sheetId).forEach((cell, key) => {
      const position = parseCellKey(key)
      if (!position) return
      data.cellData[position.row] = data.cellData[position.row] || {}
      data.cellData[position.row][position.col] = { ...cell }
    })

    this.sheetChild(sheetId, 'rows').forEach((value, key) => {
      if (value?.h != null) data.rowData[key] = { h: value.h, hd: value.hidden ? 1 : 0 }
    })
    this.sheetChild(sheetId, 'cols').forEach((value, key) => {
      if (value?.w != null) data.columnData[key] = { w: value.w, hd: value.hidden ? 1 : 0 }
    })

    data.mergeData = this.sheetMerges(sheetId).toArray().map((m) => ({
      startRow: m.r, endRow: m.r + m.rs - 1,
      startColumn: m.c, endColumn: m.c + m.cs - 1
    }))

    return data
  }

  emptySheetData(id, name, rowCount, colCount) {
    return {
      id,
      name,
      rowCount: rowCount || DEFAULT_ROWS,
      columnCount: colCount || DEFAULT_COLS,
      cellData: {},
      rowData: {},
      columnData: {},
      mergeData: []
    }
  }

  worksheet(sheetId) {
    try {
      return this.workbook?.getSheetBySheetId?.(sheetId) || null
    } catch {
      return null
    }
  }
}

/** `ICellData` → CRDT hücresi; boş hücre `null` döner (silme anlamına gelir). */
function normalizeCell(cell) {
  if (cell == null) return null
  const result = {}
  for (const field of CELL_FIELDS) {
    const value = cell[field]
    if (value !== undefined && value !== null) result[field] = value
  }
  return Object.keys(result).length ? result : null
}

function mapToObject(map) {
  const result = {}
  map.forEach((value, key) => { result[key] = value })
  return result
}

/**
 * Sunucu modelindeki stil ({@code { numFmt }}) → Univer {@code IStyleData}.
 *
 * v1'de yalnızca sayı biçimi taşınıyor; POI tarafı da (§10) bunu aktarıyor ve
 * kullanıcı uyum raporunda diğerlerinin atlandığını görüyor.
 */
function styleToUniver(style) {
  if (!style || !style.numFmt) return null
  return { n: { pattern: style.numFmt } }
}

/** Makro API'sindeki okunabilir stil → Univer {@code IStyleData}. */
function macroStyleToUniver(style) {
  if (!style) return {}
  const result = {}
  if (style.bold != null) result.bl = style.bold ? 1 : 0
  if (style.italic != null) result.it = style.italic ? 1 : 0
  if (style.bg) result.bg = { rgb: style.bg }
  if (style.color) result.cl = { rgb: style.color }
  return result
}

/** Satır içi Univer stilini sunucunun beklediği havuz anahtarına çevirir. */
function styleKey(style, styles, cache) {
  if (style == null) return null
  if (typeof style === 'string') return style

  const pattern = style?.n?.pattern
  if (!pattern) return null

  let key = cache.get(pattern)
  if (!key) {
    key = `s${cache.size}`
    cache.set(pattern, key)
    styles[key] = { numFmt: pattern }
  }
  return key
}
