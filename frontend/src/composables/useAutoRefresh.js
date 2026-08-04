/**
 * useAutoRefresh.js
 *
 * Widget'ların kendi kendine tazelenmesi (`refreshInterval`).
 *
 * Duvara asılı bir pano için gereklidir: kimse sayfayı yenilemeyecekse sayaçlar
 * saatler sonra hâlâ sabah 9'un verisini gösterir. Buna karşılık her widget'ın
 * kendi zamanlayıcısını kurması iki tuzağı beraberinde getirir:
 *
 *  1. **Görünmeyen sekmede boşa istek.** Tarayıcı arka plandaki sekmelerde
 *     zamanlayıcıları kısar ama durdurmaz; sekme açık unutulmuş bir dashboard
 *     saatlerce sunucuya gider. Burada sekme gizliyken tur atlanır, geri
 *     dönüldüğünde <b>bir kez</b> tazelenir — kullanıcı baktığı anda güncel veri
 *     görür, bakmadığı sürece istek gitmez.
 *  2. **Çok sık tazeleme.** Alt sınır 15 sn: bunun altındaki bir değer, veri
 *     tazeliğine katkı vermeden aynı sorguyu tekrar tekrar çalıştırır.
 *
 * Seçim değiştiğinde yeniden yükleme widget'ın kendi `watch(signature)`'ıyla
 * olur; bu composable yalnız <b>zamana bağlı</b> tazelemeden sorumludur.
 */

import { computed, unref, watch, onMounted, onUnmounted } from 'vue'

/** Bundan sık tazeleme kabul edilmez. */
export const MIN_REFRESH_SECONDS = 15

/** Yapılandırma ekranlarının ortak seçenekleri. */
export const REFRESH_OPTIONS = [
    { value: 0, label: 'Kapalı' },
    { value: 60, label: '1 dakika' },
    { value: 300, label: '5 dakika' },
    { value: 900, label: '15 dakika' },
    { value: 3600, label: '1 saat' },
]

/**
 * @param {() => any} reload — widget'ın yükleme fonksiyonu
 * @param {import('vue').Ref<number>|(() => number)|number} seconds — 0/boş: kapalı
 */
export function useAutoRefresh(reload, seconds) {
    const interval = computed(() => {
        const raw = Number(typeof seconds === 'function' ? seconds() : unref(seconds)) || 0
        return raw > 0 ? Math.max(raw, MIN_REFRESH_SECONDS) : 0
    })

    let timer = null
    /** Sekme gizliyken atlanan bir tur oldu mu? */
    let missed = false

    function tick() {
        if (document.hidden) {
            missed = true
            return
        }
        reload()
    }

    function start() {
        stop()
        if (interval.value > 0) timer = setInterval(tick, interval.value * 1000)
    }

    function stop() {
        if (timer) clearInterval(timer)
        timer = null
    }

    function onVisibilityChange() {
        if (document.hidden || !missed) return
        missed = false
        reload()
    }

    watch(interval, start, { immediate: true })

    onMounted(() => document.addEventListener('visibilitychange', onVisibilityChange))
    onUnmounted(() => {
        stop()
        document.removeEventListener('visibilitychange', onVisibilityChange)
    })

    return { intervalSeconds: interval, stop }
}
