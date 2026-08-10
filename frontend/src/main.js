import { createApp } from 'vue'
import './style.css'
// Docs/görev tablolarının ortak görünümü. `style.css`'ten sonra: Tailwind'in
// `prose` tablo kuralları ezilebilsin (DOCS_TABLE_PLAN.md Faz 1). CSS `@import`
// yerine buradan alınıyor — @import'un kural dosyasının başında olma zorunluluğu
// Tailwind direktifleriyle çakışıyor.
import './assets/doc-table.css'
import App from './App.vue'
import router from "./router.js"
import 'mosha-vue-toastify/dist/style.css';
import '@material-tailwind/html/scripts/ripple.js';
import { vPermission } from './directives/v-permission.js'
import { installErrorReporting } from './utils/errorReporter.js'

// Deploy sonrası eski sekmelerde bayat chunk isteği 404 verdiğinde sayfayı yenile.
// Art arda tetiklenirse (chunk gerçekten erişilemezse) döngüye girmemek için 30 sn'de bir yeniler.
window.addEventListener('vite:preloadError', () => {
    const lastReload = Number(sessionStorage.getItem('chunk_reload_at') || 0)
    if (Date.now() - lastReload > 30_000) {
        sessionStorage.setItem('chunk_reload_at', String(Date.now()))
        window.location.reload()
    }
})

const app = createApp(App)
    .use(router)
    .directive('permission', vPermission)

installErrorReporting(app)

app.mount('#app')

// PWA service worker — ana ekrana kurulabilirlik ve çevrimdışı kabuk için.
// Sürüm query'de taşınır: yeni sürümde dosya URL'i değişir, tarayıcı SW'yi
// günceller ve eski cache'ler temizlenir (bkz. public/sw.js).
if ('serviceWorker' in navigator && import.meta.env.PROD) {
    window.addEventListener('load', () => {
        navigator.serviceWorker
            .register(`/sw.js?v=${__APP_VERSION__}`)
            .catch(err => console.warn('[pwa] service worker kaydedilemedi:', err))
    })

    // Yeni SW kontrolü devraldığında sayfayı bir kez tazele ki kullanıcı
    // yarısı eski yarısı yeni bir uygulamada kalmasın. İlk kurulumda controller
    // henüz yok — o durumda yenileme yapılmaz, aksi halde her yeni ziyaretçi
    // sayfayı bir kez boşuna yeniden yüklerdi.
    const hadController = Boolean(navigator.serviceWorker.controller)
    let refreshing = false
    navigator.serviceWorker.addEventListener('controllerchange', () => {
        if (!hadController || refreshing) return
        refreshing = true
        window.location.reload()
    })
}
