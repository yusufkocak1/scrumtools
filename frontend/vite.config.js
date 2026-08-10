import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { readFileSync } from 'node:fs'

const pkg = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf-8'))

// https://vitejs.dev/config/
export default defineConfig({
  base: process.env.VITE_APP_BASE_PATH || '/',
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version),
    __BUILD_TIME__: JSON.stringify(new Date().toLocaleString('tr-TR', {
      timeZone: 'Europe/Istanbul',
      dateStyle: 'short',
      timeStyle: 'short'
    }))
  },
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },

  /**
   * Geliştirme sunucusu vekili.
   *
   * <b>Neden gerekliydi:</b> uygulama `/api` ve `/ws` adreslerine *göreli*
   * istek atıyor (üretimde nginx bunları backend'e yönlendiriyor). Geliştirmede
   * vekil yoktu, dolayısıyla:
   *
   * - `/api/...` istekleri Vite'ın SPA geri dönüşüne düşüp **200 ile
   *   `index.html`** dönüyordu. Yani istek "başarılı" görünüyor ama gövde HTML
   *   olduğu için `data.canWrite` gibi alanlar `undefined` kalıyordu — hata
   *   yerine sessiz yanlış davranış.
   * - `/ws/collab` yükseltmesini karşılayan kimse olmadığından tarayıcı
   *   soketi CONNECTING durumunda asılı bırakıyordu: ne `onopen` ne `onclose`.
   *   Ortak çalışma ekranındaki "Bağlanılıyor…" şeridinin hiç geçmemesinin
   *   sebebi buydu.
   *
   * Hedef `VITE_DEV_API_TARGET` ile değiştirilebilir (uzak bir backend'e
   * bağlanmak için).
   */
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_DEV_API_TARGET || 'http://localhost:8080',
        changeOrigin: true
      },
      // `ws: true` şart: hem STOMP/SockJS ucu hem de ortak çalışmanın ham
      // WebSocket'i bu önek altında yaşıyor.
      '/ws': {
        target: process.env.VITE_DEV_API_TARGET || 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
