/**
 * useDashboardGrid.js
 *
 * Pano düzeninin geometrisi ve düzenleme etkileşimleri.
 *
 * Neden hazır bir ızgara kütüphanesi değil: vue-grid-layout ailesi serbest x/y
 * yerleşimi ve çakışma çözümü getirir; bunun bedeli, widget'ların birbirini
 * itmesi ve kullanıcının "kaydırdım ama başka bir yere oturdu" hissidir. Buradaki
 * model daha dar ve tahmin edilebilir: 12 sütunluk akış ızgarası, sıra dizinin
 * kendi sırası, genişlik sütun cinsinden, yükseklik isteğe bağlı.
 *
 * Yükseklik varsayılan olarak YOKTUR (widget kendi doğal boyunda kalır). Sabit
 * bir yükseklik dayatmak, mevcut widget'ların içeriğini kırpardı; kullanıcı
 * dikey olarak sürüklediği an o widget için açık bir yükseklik yazılır.
 */

import { ref } from 'vue'

/** Izgara sütun sayısı — Tailwind'in 12'lik düzeniyle aynı. */
export const COLUMNS = 12

/** Sütunlar arası boşluk (px) — şablondaki gap-4 ile aynı olmalı. */
export const GAP = 16

export const MIN_SPAN = 3
export const MAX_SPAN = COLUMNS
export const MIN_HEIGHT = 140
export const MAX_HEIGHT = 900

/**
 * Tipe göre varsayılan genişlik. Sayaç ve oran göstergesi tek bir sayı
 * gösterdiği için dar; liste, ısı haritası ve zaman serisi geniş olmadan
 * okunmuyor. Bilinmeyen tip üçte bire düşer — eski üç sütunlu düzenle aynı.
 */
const DEFAULT_SPANS = {
  RF_STAT: 3,
  RF_RATIO: 3,
  RF_MULTI_STAT: 6,
  RF_CONTROLLER: 4,
  RF_CHART: 4,
  RF_QUEUE: 6,
  RF_RESULTS: 6,
  RF_TIME_SERIES: 8,
  RF_HEATMAP: 8,
  SUMMARY: 4,
  BURNDOWN: 8,
  VELOCITY: 6,
  WORKLOAD: 6,
  CREATED_VS_RESOLVED: 8,
  OVERDUE: 4,
}

export function defaultSpan(type) {
  return DEFAULT_SPANS[type] ?? 4
}

export function clampSpan(value, fallback) {
  const number = Math.round(Number(value))
  if (!Number.isFinite(number)) return fallback
  return Math.min(Math.max(number, MIN_SPAN), MAX_SPAN)
}

export function clampHeight(value) {
  const number = Math.round(Number(value))
  if (!Number.isFinite(number)) return null
  return Math.min(Math.max(number, MIN_HEIGHT), MAX_HEIGHT)
}

/**
 * @param {import('vue').Ref<Array>} widgets  düzenlenen widget dizisi (yerinde değişir)
 * @param {Function} onChange                 her değişiklikten sonra çağrılır (kirli işareti)
 */
export function useDashboardGrid(widgets, onChange = () => {}) {
  /** Sürüklenen widget'ın dizideki yeri; sürükleme yokken null. */
  const dragIndex = ref(null)
  /** Üzerine gelinen hedef — bırakma çizgisini göstermek için. */
  const overIndex = ref(null)
  /** Boyutlandırılan widget'ın kimliği; imleç ve seçim engelini açar. */
  const resizingId = ref(null)

  // ─── Sürükle-sırala ────────────────────────────────────────────────────────

  function startDrag(index, event) {
    dragIndex.value = index
    // Firefox sürüklemeyi ancak veri yazılırsa başlatır; taşınan bilgi dizin.
    event?.dataTransfer?.setData('text/plain', String(index))
    if (event?.dataTransfer) event.dataTransfer.effectAllowed = 'move'
  }

  function dragOver(index) {
    if (dragIndex.value === null || index === dragIndex.value) {
      overIndex.value = null
      return
    }
    overIndex.value = index
  }

  function drop(index) {
    const from = dragIndex.value
    endDrag()
    if (from === null || index === from) return

    const list = [...widgets.value]
    const [moved] = list.splice(from, 1)
    list.splice(index, 0, moved)
    widgets.value = list
    onChange()
  }

  function endDrag() {
    dragIndex.value = null
    overIndex.value = null
  }

  // ─── Boyutlandırma ─────────────────────────────────────────────────────────

  /**
   * Sağ-alt köşedeki tutamaktan boyutlandırma. Pointer olayları kullanılır
   * (fare + dokunmatik tek yol) ve pointer capture ile imleç widget'ın dışına
   * çıktığında da olaylar tutamağa gelmeye devam eder.
   *
   * @param {PointerEvent} event
   * @param {number} index      widget'ın dizideki yeri
   * @param {HTMLElement} cell  widget sarmalayıcısı — sol/üst köşesi ölçüm kaynağı
   * @param {HTMLElement} grid  ızgara kabı — sütun genişliği buradan çıkar
   */
  function startResize(event, index, cell, grid) {
    if (!cell || !grid) return
    event.preventDefault()
    event.stopPropagation()

    const widget = widgets.value[index]
    if (!widget) return

    const handle = event.currentTarget
    handle.setPointerCapture?.(event.pointerId)
    resizingId.value = widget.id

    const cellRect = cell.getBoundingClientRect()
    const gridWidth = grid.getBoundingClientRect().width
    // Sütun genişliği: toplam genişlikten aradaki boşluklar düşülür.
    const columnWidth = (gridWidth - GAP * (COLUMNS - 1)) / COLUMNS

    function onMove(moveEvent) {
      // Güncel değer her seferinde diziden okunur: `widget` ilk yakalanan
      // nesnedir ve dizi her güncellemede yeniden kurulduğu için eskir —
      // ona bakılsaydı karşılaştırma hep "değişti" der, her piksel yeni bir
      // dizi üretirdi.
      const current = widgets.value[index]
      if (!current) return

      const width = moveEvent.clientX - cellRect.left
      const height = moveEvent.clientY - cellRect.top

      // Genişlik sütuna oturtulur: n sütun = n*col + (n-1)*gap
      const span = clampSpan((width + GAP) / (columnWidth + GAP), current.w ?? defaultSpan(current.type))
      const nextHeight = clampHeight(height)

      if (span === current.w && nextHeight === (current.h ?? null)) return

      widgets.value = widgets.value.map((w, i) =>
        i === index ? { ...w, w: span, h: nextHeight } : w
      )
      onChange()
    }

    function onUp() {
      handle.releasePointerCapture?.(event.pointerId)
      handle.removeEventListener('pointermove', onMove)
      handle.removeEventListener('pointerup', onUp)
      handle.removeEventListener('pointercancel', onUp)
      resizingId.value = null
    }

    handle.addEventListener('pointermove', onMove)
    handle.addEventListener('pointerup', onUp)
    handle.addEventListener('pointercancel', onUp)
  }

  /** Genişliği tek tıkla değiştirmek için — tutamağa erişemeyen kullanıcı da ayarlayabilsin. */
  function setSpan(index, span) {
    widgets.value = widgets.value.map((w, i) =>
      i === index ? { ...w, w: clampSpan(span, defaultSpan(w.type)) } : w
    )
    onChange()
  }

  /** Elle verilmiş yüksekliği kaldırır; widget doğal boyuna döner. */
  function resetHeight(index) {
    widgets.value = widgets.value.map((w, i) => {
      if (i !== index) return w
      const { h, ...rest } = w
      return rest
    })
    onChange()
  }

  return {
    dragIndex,
    overIndex,
    resizingId,
    startDrag,
    dragOver,
    drop,
    endDrag,
    startResize,
    setSpan,
    resetHeight,
  }
}

export default useDashboardGrid
