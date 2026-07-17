import { createApp } from 'vue'
import './style.css'
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
