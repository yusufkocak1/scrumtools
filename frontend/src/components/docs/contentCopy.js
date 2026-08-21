/**
 * Docs okuma görünümü için kopyalama yardımcıları.
 *
 * <b>Neden var:</b> kaydedilen sayfa metnini başka bir yere taşımanın tek yolu
 * elle seçmekti. Masaüstünde bu katlanılabilir, mobilde ise pratikte imkânsız:
 * uzun basıp tutamaçları sürüklemek uzun sayfalarda çalışmıyor, kaydırılabilir
 * kod bloğu ve tablo içinde ise dokunuş seçim yerine kaydırma olarak
 * yorumlanıyor — yani en çok kopyalanmak istenen iki blok en zor seçilenler.
 *
 * Buradaki üç yetenek bunu karşılıyor: tüm içeriği kopyala, tüm içeriği seç
 * (kullanıcı kendi menüsüyle paylaşmak isterse) ve blok başına kopyala düğmesi.
 *
 * <b>Panoya yazmanın üç yolu deneniyor</b> çünkü hiçbiri tek başına yetmiyor:
 * `clipboard.write` biçimlendirmeyi korur ama `ClipboardItem` her tarayıcıda
 * yok; `writeText` güvenli olmayan bağlamda (http) tanımsız; `execCommand` ise
 * eski ama her yerde var. Sıra: zengin → düz metin → seçim üzerinden
 * `execCommand` (DocShareDialog'daki bağlantı kopyalama da aynı yedeğe düşer).
 */

const BUTTON_CLASS = 'doc-copy-btn'
const BLOCK_CLASS = 'doc-copy-block'
const HIDING_CLASS = 'doc-copy-hiding'
const RESET_DELAY = 1800

const ICON_COPY = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">'
    + '<path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75V6.375c0-.621.504-1.125 1.125-1.125h8.25c.621 0 1.125.504 1.125 1.125v8.25c0 .621-.504 1.125-1.125 1.125H12"/>'
    + '<path stroke-linecap="round" stroke-linejoin="round" d="M5.625 8.25h6.75c.621 0 1.125.504 1.125 1.125v8.25c0 .621-.504 1.125-1.125 1.125h-6.75A1.125 1.125 0 014.5 17.625v-8.25c0-.621.504-1.125 1.125-1.125z"/>'
    + '</svg>'

const ICON_DONE = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true">'
    + '<path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"/>'
    + '</svg>'

/**
 * Kopyala düğmelerini metin ölçümü/seçimi dışında tutar.
 *
 * Düğmeler `.doc-content` ağacının içinde duruyor; hiçbir şey yapılmazsa
 * etiketleri ("Kopyala") kopyalanan metnin ortasına karışırdı. `display: none`
 * hem `innerText`'i hem de `Range` metnini dışarıda bırakıyor. Sınıf aynı
 * senkron blokta geri alındığı için kullanıcı bir titreme görmüyor.
 */
function withoutCopyButtons(el, fn) {
  const host = el?.closest?.('.doc-content') || el
  host?.classList?.add(HIDING_CLASS)
  try {
    return fn()
  } finally {
    host?.classList?.remove(HIDING_CLASS)
  }
}

/**
 * Görünen metin. `textContent` değil `innerText`: ilki blok sınırlarını yok
 * sayıp tüm sayfayı tek satıra indiriyor, tablo hücrelerini de bitiştiriyor.
 */
function visibleTextOf(el) {
  return withoutCopyButtons(el, () => el.innerText ?? el.textContent ?? '').trim()
}

function htmlOf(el) {
  const clone = el.cloneNode(true)
  clone.querySelectorAll('.' + BUTTON_CLASS).forEach((node) => node.remove())
  return clone.innerHTML
}

/** Verilen düğümün içeriğini kullanıcı seçimi hâline getirir. */
export function selectContent(el) {
  const selection = window.getSelection?.()
  if (!el || !selection) return false
  try {
    const range = document.createRange()
    range.selectNodeContents(el)
    selection.removeAllRanges()
    selection.addRange(range)
    return true
  } catch {
    return false
  }
}

/** Seçim üzerinden kopyalama — pano API'si yoksa biçimlendirmeyi koruyan tek yol. */
function copyViaSelection(el) {
  const selection = window.getSelection?.()
  if (!el || !selection) return false
  const previous = selection.rangeCount ? selection.getRangeAt(0).cloneRange() : null
  return withoutCopyButtons(el, () => {
    if (!selectContent(el)) return false
    let ok = false
    try { ok = document.execCommand('copy') } catch { ok = false }
    selection.removeAllRanges()
    if (previous) selection.addRange(previous)
    return ok
  })
}

function copyViaTextarea(text) {
  const field = document.createElement('textarea')
  field.value = text
  field.setAttribute('readonly', '')
  field.style.cssText = 'position:fixed;top:0;left:-9999px;opacity:0'
  document.body.appendChild(field)
  field.select()
  let ok = false
  try { ok = document.execCommand('copy') } catch { ok = false }
  document.body.removeChild(field)
  return ok
}

/** Düz metni panoya yazar. */
export async function copyText(text) {
  const value = (text || '').trim()
  if (!value) return false
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(value)
      return true
    } catch { /* güvenli olmayan bağlam ya da izin reddi — aşağıya düş */ }
  }
  return copyViaTextarea(value)
}

/**
 * Bir düğümün içeriğini biçimlendirmesiyle birlikte panoya yazar.
 *
 * Hem `text/html` hem `text/plain` konuyor: Word/Confluence gibi hedefler
 * tabloyu tablo olarak alsın, düz metin bekleyen hedefler (terminal, mesajlaşma
 * uygulaması) ise HTML etiketi görmesin.
 */
export async function copyElement(el) {
  if (!el) return false
  const text = visibleTextOf(el)
  if (!text) return false

  if (navigator.clipboard?.write && typeof window.ClipboardItem === 'function') {
    try {
      await navigator.clipboard.write([new window.ClipboardItem({
        'text/html': new Blob([htmlOf(el)], {type: 'text/html'}),
        'text/plain': new Blob([text], {type: 'text/plain'})
      })])
      return true
    } catch { /* biçim desteklenmiyor ya da izin yok — düz metne düş */ }
  }

  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text)
      return true
    } catch { /* aşağıya düş */ }
  }

  return copyViaSelection(el) || copyViaTextarea(text)
}

// ─── Blok başına kopyala düğmesi ─────────────────────────────────────────────

function flash(button, ok) {
  const label = button.querySelector('.' + BUTTON_CLASS + '__label')
  const icon = button.querySelector('.' + BUTTON_CLASS + '__icon')
  button.dataset.state = ok ? 'copied' : 'failed'
  if (icon) icon.innerHTML = ok ? ICON_DONE : ICON_COPY
  if (label) label.textContent = ok ? 'Kopyalandı' : 'Kopyalanamadı'
  clearTimeout(button.resetTimer)
  button.resetTimer = setTimeout(() => {
    delete button.dataset.state
    if (icon) icon.innerHTML = ICON_COPY
    if (label) label.textContent = 'Kopyala'
  }, RESET_DELAY)
}

function createCopyButton(title, onCopy) {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = BUTTON_CLASS
  button.title = title
  button.setAttribute('aria-label', title)
  button.innerHTML = '<span class="' + BUTTON_CLASS + '__icon">' + ICON_COPY + '</span>'
      + '<span class="' + BUTTON_CLASS + '__label">Kopyala</span>'
  button.addEventListener('click', async (event) => {
    event.preventDefault()
    event.stopPropagation()
    flash(button, await onCopy())
  })
  return button
}

/**
 * Bloğu konumlandırma kabına alır ve düğmeyi oraya koyar.
 *
 * Düğme neden bloğun <i>içine</i> değil de dışına: kod bloğu ve tablo
 * sarmalayıcısı yatay kaydırılabilir kaplar; içlerine konan mutlak konumlu bir
 * düğme içerikle birlikte kayıp görüş alanından çıkıyor.
 */
function addCopyBlock(el, title, doCopy) {
  if (!el || el.parentElement?.classList?.contains(BLOCK_CLASS)) return
  const block = document.createElement('div')
  block.className = BLOCK_CLASS
  el.replaceWith(block)
  block.appendChild(el)
  block.appendChild(createCopyButton(title, doCopy))
}

/**
 * Kod bloklarına ve tablolara kopyala düğmesi ekler. Yeniden çağrılabilir;
 * tablo sarmalayıcıları hazır olsun diye `wrapTables`'tan sonra çağrılmalı.
 */
export function attachCopyButtons(root) {
  if (!root) return
  for (const pre of root.querySelectorAll('pre')) {
    // Kod bloğu bilerek <b>düz metin</b> olarak kopyalanıyor: zengin biçim
    // vurgulayıcının renk `span`'lerini de taşır, kod ise gittiği yere
    // (terminal, IDE, sohbet) sade gitmeli.
    addCopyBlock(pre, 'Kod bloğunu kopyala',
        () => copyText(visibleTextOf(pre.querySelector('code') || pre)))
  }
  for (const wrapper of root.querySelectorAll('.doc-table-wrapper, .tableWrapper')) {
    // Tabloda zengin biçim korunuyor: Excel/Word tabloyu hücrelere ayırabilsin.
    addCopyBlock(wrapper, 'Tabloyu kopyala',
        () => copyElement(wrapper.querySelector('table') || wrapper))
  }
}
