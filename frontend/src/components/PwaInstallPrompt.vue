<template>
  <!-- Ana ekrana kurulum önerisi. Android/Chrome'da tek dokunuşla kurar,
       iOS'ta (beforeinstallprompt desteklenmez) manuel adımları anlatır. -->
  <transition
    enter-active-class="transition duration-300 ease-out"
    enter-from-class="translate-y-full opacity-0"
    enter-to-class="translate-y-0 opacity-100"
    leave-active-class="transition duration-200 ease-in"
    leave-from-class="translate-y-0 opacity-100"
    leave-to-class="translate-y-full opacity-0"
  >
    <div
      v-if="visible"
      class="fixed inset-x-0 bottom-0 z-[9997] p-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))]
             sm:inset-x-auto sm:right-4 sm:bottom-4 sm:w-96 sm:p-0"
    >
      <div class="bg-white rounded-2xl shadow-2xl border border-gray-200 p-4">
        <div class="flex items-start gap-3">
          <img
            src="/icons/icon-192.png"
            alt=""
            width="48"
            height="48"
            class="w-12 h-12 rounded-xl shrink-0"
          />

          <div class="flex-1 min-w-0">
            <p class="text-sm font-semibold text-gray-900">ScrumTools'u yükle</p>
            <p class="text-xs text-gray-500 mt-0.5 leading-relaxed">
              {{ isIos
                ? 'Paylaş menüsünden “Ana Ekrana Ekle” ile uygulama gibi kullan.'
                : 'Ana ekranına ekle; tam ekran açılsın, daha hızlı çalışsın.' }}
            </p>
          </div>

          <button
            @click="dismiss"
            class="shrink-0 -mt-1 -mr-1 p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
            aria-label="Kapat"
            type="button"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <!-- iOS: kurulum tarayıcı menüsünden yapılır, buton gösterilemez -->
        <div v-if="isIos" class="mt-3 flex items-center gap-2 text-xs text-gray-600 bg-gray-50 rounded-lg px-3 py-2">
          <svg class="w-4 h-4 text-blue-600 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 16V4m0 0L8 8m4-4l4 4M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2"/>
          </svg>
          <span>Paylaş <span class="text-gray-400">›</span> Ana Ekrana Ekle</span>
        </div>

        <div v-else class="mt-3 flex items-center gap-2">
          <button
            @click="install"
            class="flex-1 px-4 py-2.5 text-sm font-medium text-white bg-blue-600 rounded-xl hover:bg-blue-700 active:bg-blue-800 transition-colors"
            type="button"
          >
            Yükle
          </button>
          <button
            @click="dismiss"
            class="px-4 py-2.5 text-sm font-medium text-gray-600 hover:text-gray-900 transition-colors"
            type="button"
          >
            Şimdi değil
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

/** Kapatıldıktan sonra bu kadar süre tekrar gösterilmez. */
const DISMISS_KEY = 'pwa_install_dismissed_at'
const DISMISS_DAYS = 14

const visible = ref(false)
const isIos = ref(false)

/** Kurulu uygulamanın içinde açıldıysak öneri anlamsız. */
function isStandalone() {
  return window.matchMedia?.('(display-mode: standalone)').matches
    || window.navigator.standalone === true
}

function recentlyDismissed() {
  try {
    const at = Number(localStorage.getItem(DISMISS_KEY) || 0)
    return Date.now() - at < DISMISS_DAYS * 24 * 60 * 60 * 1000
  } catch {
    return false
  }
}

// Chrome kurulum olayını tutar; prompt() yalnızca bu event üzerinden çağrılabilir.
let deferredPrompt = null

function onBeforeInstallPrompt(e) {
  // Tarayıcının kendi mini-infobar'ı yerine kendi bannerımızı gösteriyoruz
  e.preventDefault()
  deferredPrompt = e
  if (!isStandalone() && !recentlyDismissed()) visible.value = true
}

function onAppInstalled() {
  visible.value = false
  deferredPrompt = null
}

async function install() {
  if (!deferredPrompt) {
    visible.value = false
    return
  }
  deferredPrompt.prompt()
  try {
    await deferredPrompt.userChoice
  } finally {
    // prompt() bir kez kullanılabilir; sonuç ne olursa olsun event tüketildi
    deferredPrompt = null
    visible.value = false
  }
}

function dismiss() {
  visible.value = false
  try {
    localStorage.setItem(DISMISS_KEY, String(Date.now()))
  } catch {
    // Private mode — bu oturumda gizlemek yeterli
  }
}

onMounted(() => {
  if (isStandalone()) return

  const ua = window.navigator.userAgent
  // iPadOS 13+ kendini Mac gibi tanıtır; dokunmatik varlığıyla ayrıştırılır
  isIos.value = /iPhone|iPad|iPod/i.test(ua)
    || (/Macintosh/.test(ua) && navigator.maxTouchPoints > 1)

  window.addEventListener('beforeinstallprompt', onBeforeInstallPrompt)
  window.addEventListener('appinstalled', onAppInstalled)

  // iOS'ta beforeinstallprompt hiç tetiklenmez — banner'ı kısa bir gecikmeyle
  // kendimiz açıyoruz ki sayfa açılışının önüne geçmesin.
  if (isIos.value && !recentlyDismissed()) {
    setTimeout(() => { if (!isStandalone()) visible.value = true }, 3000)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeinstallprompt', onBeforeInstallPrompt)
  window.removeEventListener('appinstalled', onAppInstalled)
})
</script>
