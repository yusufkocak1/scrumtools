/**
 * ScrumTools Service Worker
 *
 * Amaç: uygulamanın Android/iOS'ta ana ekrana kurulabilmesi (PWA) ve ağ
 * koptuğunda uygulama kabuğunun açılabilmesi. Kurulabilirlik için tarayıcı bir
 * `fetch` handler'ı şart koşar.
 *
 * Sürümleme: main.js kaydı `/sw.js?v=<app version>` ile yapar. Sürüm değişince
 * dosya baytları değişir → tarayıcı SW'yi günceller, eski cache'ler activate'te
 * silinir. Böylece deploy sonrası kullanıcı eski sürümde takılı kalmaz.
 *
 * Strateji:
 *  - /api, /ws ve GET olmayan istekler          → hiç dokunulmaz (her zaman ağ)
 *  - /assets, /icons (hash'li, değişmez)        → cache-first
 *  - navigasyon (HTML)                          → network-first, çevrimdışıysa cache
 *  - diğer same-origin GET                      → network-first, cache fallback
 */

const VERSION = new URL(self.location.href).searchParams.get('v') || 'dev'
const CACHE = `scrumtools-${VERSION}`

/** Çevrimdışı açılış için minimum kabuk. */
const PRECACHE_URLS = [
  '/',
  '/manifest.webmanifest',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
]

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE)
      // Tek bir dosya 404 verirse tüm kurulum düşmesin diye tek tek eklenir
      .then(cache => Promise.allSettled(PRECACHE_URLS.map(url => cache.add(url))))
      .then(() => self.skipWaiting())
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(
        keys.filter(k => k.startsWith('scrumtools-') && k !== CACHE)
            .map(k => caches.delete(k))
      ))
      .then(() => self.clients.claim())
  )
})

/** Yeni sürüm hazır olduğunda sayfa "hemen geç" diyebilsin. */
self.addEventListener('message', (event) => {
  if (event.data === 'SKIP_WAITING') self.skipWaiting()
})

/** Hash'li build çıktıları — içerik değişince dosya adı değişir, güvenle cache'lenir. */
function isImmutableAsset(url) {
  return url.pathname.startsWith('/assets/') || url.pathname.startsWith('/icons/')
}

/** Backend ve websocket trafiği asla cache'lenmez. */
function isNetworkOnly(url) {
  return url.pathname.startsWith('/api/') || url.pathname.startsWith('/ws')
}

async function cacheFirst(request) {
  const cached = await caches.match(request)
  if (cached) return cached
  const response = await fetch(request)
  if (response.ok) {
    const cache = await caches.open(CACHE)
    cache.put(request, response.clone())
  }
  return response
}

async function networkFirst(request, fallbackUrl) {
  try {
    const response = await fetch(request)
    if (response.ok) {
      const cache = await caches.open(CACHE)
      cache.put(request, response.clone())
    }
    return response
  } catch (err) {
    const cached = await caches.match(request)
    if (cached) return cached
    if (fallbackUrl) {
      const shell = await caches.match(fallbackUrl)
      if (shell) return shell
    }
    throw err
  }
}

self.addEventListener('fetch', (event) => {
  const { request } = event

  if (request.method !== 'GET') return

  const url = new URL(request.url)
  if (url.origin !== self.location.origin) return
  if (isNetworkOnly(url)) return

  if (isImmutableAsset(url)) {
    event.respondWith(cacheFirst(request))
    return
  }

  // SPA: her derin link index.html'e düşer, çevrimdışında kabuk '/' cache'inden gelir
  if (request.mode === 'navigate') {
    event.respondWith(networkFirst(request, '/'))
    return
  }

  event.respondWith(networkFirst(request))
})
