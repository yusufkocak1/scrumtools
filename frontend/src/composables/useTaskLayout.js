/**
 * useTaskLayout.js
 *
 * Görev detay sayfasının panel düzeni. Jira'daki gibi her bölüm bağımsız bir
 * paneldir: kullanıcı panelleri sürükleyerek sıralayabilir, ana kolon ile yan
 * kolon arasında taşıyabilir ve ilgilenmediklerini katlayabilir.
 *
 * Düzen sunucuda tutulan bir tercih değil, ekran alışkanlığıdır — bu yüzden
 * localStorage'da saklanır ve okuma/yazma best-effort'tur (private mode'da
 * sessizce oturum içi kalır).
 *
 * Panel görünürlüğü düzenin parçası değildir: veri/yetki yoksa panel kendini
 * gizler ama sıradaki yerini korur — veri geldiğinde eski yerinde açılır.
 */

import { reactive, ref, watch } from 'vue'

const STORAGE_KEY = 'taskdetail_layout_v1'

export const COLUMNS = ['main', 'side']

/**
 * Varsayılan yerleşim. Geliştirme/Deploy panelleri Jira'daki gibi yan kolonda
 * durur; ana kolon okuma-yazma akışına (açıklama → ekler → kırılım → aktivite)
 * ayrılmıştır.
 */
export const DEFAULT_LAYOUT = Object.freeze({
  main: ['description', 'attachments', 'relations', 'activity'],
  side: ['details', 'development', 'deploy', 'deployments', 'watchers'],
})

const ALL_KEYS = [...DEFAULT_LAYOUT.main, ...DEFAULT_LAYOUT.side]

export function useTaskLayout() {
  const layout = reactive({ main: [], side: [] })
  const collapsedKeys = ref([])
  const editing = ref(false)
  const draggingKey = ref(null)

  // ── Kalıcılık ──────────────────────────────────────────────────────────────

  function persist() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        main: layout.main,
        side: layout.side,
        collapsed: collapsedKeys.value,
      }))
    } catch {
      // Kota/private mode — düzen oturum içinde yine çalışır
    }
  }

  /**
   * Kayıtlı düzeni doğrulayarak uygular: tanınmayan anahtarlar atılır, yeni
   * eklenen paneller varsayılan kolonlarının sonuna iliştirilir. Böylece eski
   * bir kayıt yüzünden yeni bir panel hiç görünmez kalmaz.
   */
  function applyLayout(raw) {
    const next = { main: [], side: [] }
    const seen = new Set()

    for (const col of COLUMNS) {
      for (const key of Array.isArray(raw?.[col]) ? raw[col] : []) {
        if (ALL_KEYS.includes(key) && !seen.has(key)) {
          next[col].push(key)
          seen.add(key)
        }
      }
    }
    for (const col of COLUMNS) {
      for (const key of DEFAULT_LAYOUT[col]) {
        if (!seen.has(key)) {
          next[col].push(key)
          seen.add(key)
        }
      }
    }

    layout.main = next.main
    layout.side = next.side
  }

  function load() {
    let raw = null
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) raw = JSON.parse(stored)
    } catch {
      raw = null
    }
    applyLayout(raw || DEFAULT_LAYOUT)
    collapsedKeys.value = (raw?.collapsed || []).filter(k => ALL_KEYS.includes(k))
  }

  load()

  watch([() => layout.main, () => layout.side, collapsedKeys], persist, { deep: true })

  // ── Sorgular ───────────────────────────────────────────────────────────────

  function columnOf(key) {
    return COLUMNS.find(col => layout[col].includes(key)) || null
  }

  function isCollapsed(key) {
    return collapsedKeys.value.includes(key)
  }

  // ── Düzen değişiklikleri ───────────────────────────────────────────────────

  function toggleCollapsed(key) {
    collapsedKeys.value = isCollapsed(key)
      ? collapsedKeys.value.filter(k => k !== key)
      : [...collapsedKeys.value, key]
  }

  function setAllCollapsed(value) {
    collapsedKeys.value = value ? [...ALL_KEYS] : []
  }

  /**
   * Paneli hedef kolonun verilen sırasına taşır. Sıra, taşınan panel hâlâ
   * listedeyken hesaplanan hedef indeksidir: üstteki bir panelin üstüne
   * bırakınca öne, alttakinin üstüne bırakınca arkasına geçer.
   */
  function relocate(key, column, index) {
    const from = columnOf(key)
    if (!from || !COLUMNS.includes(column)) return
    const current = layout[from].indexOf(key)
    if (from === column && current === index) return

    layout[from].splice(current, 1)
    const target = layout[column]
    target.splice(Math.max(0, Math.min(index, target.length)), 0, key)
  }

  /** Dokunmatik/klavye için sürüklemesiz alternatif: bir sıra yukarı/aşağı. */
  function moveBy(key, delta) {
    const col = columnOf(key)
    if (!col) return
    const from = layout[col].indexOf(key)
    const to = from + delta
    if (to < 0 || to >= layout[col].length) return
    layout[col].splice(from, 1)
    layout[col].splice(to, 0, key)
  }

  /** Paneli diğer kolonun sonuna taşır. */
  function moveToOtherColumn(key) {
    const from = columnOf(key)
    if (!from) return
    const to = from === 'main' ? 'side' : 'main'
    relocate(key, to, layout[to].length)
  }

  function resetLayout() {
    applyLayout(DEFAULT_LAYOUT)
    collapsedKeys.value = []
  }

  function toggleEditing() {
    editing.value = !editing.value
    if (!editing.value) draggingKey.value = null
  }

  // ── Sürükleme ──────────────────────────────────────────────────────────────

  function startDrag(key) {
    draggingKey.value = key
  }

  function endDrag() {
    draggingKey.value = null
  }

  /** Sürüklenen panel bir panelin üstüne girdiğinde canlı olarak yer değiştirir. */
  function dragOverPanel(key) {
    const dragged = draggingKey.value
    if (!dragged || dragged === key) return
    const col = columnOf(key)
    if (!col) return
    relocate(dragged, col, layout[col].indexOf(key))
  }

  /** Kolonun boş alanına girildiğinde panel o kolonun sonuna alınır. */
  function dragOverColumn(column) {
    const dragged = draggingKey.value
    if (!dragged || !COLUMNS.includes(column)) return
    if (columnOf(dragged) === column && layout[column].at(-1) === dragged) return
    relocate(dragged, column, layout[column].length)
  }

  return {
    layout,
    editing,
    draggingKey,
    columnOf,
    isCollapsed,
    toggleCollapsed,
    setAllCollapsed,
    relocate,
    moveBy,
    moveToOtherColumn,
    resetLayout,
    toggleEditing,
    startDrag,
    endDrag,
    dragOverPanel,
    dragOverColumn,
  }
}
