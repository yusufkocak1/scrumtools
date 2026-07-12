import { createToast } from 'mosha-vue-toastify'
import ErrorReportApi from '../api/ErrorReportApi.js'

/**
 * Global hata raporlayıcı — yakalanmayan JS hatalarını backend'e gönderir,
 * dönen takip numarasını kullanıcıya toast ile gösterir.
 *
 * Korumalar:
 *  - Giriş yapılmamışsa (jwt yok) rapor gönderilmez
 *  - Aynı hata 60 sn içinde tekrar raporlanmaz (dedupe)
 *  - Dakikada en fazla 5 rapor gönderilir (istemci tavanı)
 *  - Raporlayıcının kendisi asla hata fırlatmaz
 */

const DEDUPE_WINDOW_MS = 60000
const MAX_REPORTS_PER_MINUTE = 5

const recentErrors = new Map() // key → lastReportedAt
let windowStart = 0
let windowCount = 0

function allowByRateLimit() {
    const now = Date.now()
    if (now - windowStart > 60000) {
        windowStart = now
        windowCount = 0
    }
    windowCount++
    return windowCount <= MAX_REPORTS_PER_MINUTE
}

function dedupeKey(message, stack) {
    const firstStackLine = (stack || '').split('\n').find(l => l.trim()) || ''
    return `${message}|${firstStackLine}`
}

function shouldReport(message, stack) {
    if (!localStorage.getItem('jwt')) return false

    const key = dedupeKey(message, stack)
    const now = Date.now()
    const last = recentErrors.get(key)
    if (last && now - last < DEDUPE_WINDOW_MS) return false

    if (!allowByRateLimit()) return false

    recentErrors.set(key, now)
    // Map büyümesin: eski kayıtları temizle
    if (recentErrors.size > 100) {
        for (const [k, t] of recentErrors) {
            if (now - t > DEDUPE_WINDOW_MS) recentErrors.delete(k)
        }
    }
    return true
}

async function reportError(error, componentName) {
    try {
        const err = error instanceof Error ? error : new Error(String(error))
        const message = err.message || 'Bilinmeyen hata'
        const stack = err.stack || ''

        if (!shouldReport(message, stack)) return

        const { data } = await ErrorReportApi.report({
            errorName: err.name || 'Error',
            message: message.slice(0, 4000),
            stack: stack.slice(0, 20000),
            url: window.location.href.slice(0, 1024),
            componentName: componentName || null,
            userAgent: navigator.userAgent.slice(0, 512),
        })

        // Rate limit aşımında backend trackingCode null döner → sessiz kal
        if (data?.trackingCode) {
            createToast(`Beklenmeyen bir hata oluştu. Takip No: ${data.trackingCode}`, {
                type: 'danger',
                position: 'top-center',
                showCloseButton: true,
                timeout: 8000,
            })
        }
    } catch {
        // Raporlayıcı asla yeniden hata fırlatmamalı
    }
}

export function installErrorReporting(app) {
    app.config.errorHandler = (err, instance, info) => {
        console.error('[Vue Error]', err, info)
        const componentName = instance?.$options?.name || instance?.$?.type?.name || null
        reportError(err, componentName)
    }

    window.addEventListener('error', (event) => {
        // Kaynak yükleme hataları (img/script) raporlanmaz — sadece JS hataları
        if (!event.error) return
        reportError(event.error, null)
    })

    window.addEventListener('unhandledrejection', (event) => {
        // Axios hataları raporlanmaz: backend hataları zaten sunucuda izleniyor
        // ve interceptor toast'ı gösteriyor (errorReporter'ın kendi isteği dahil)
        if (event.reason?.isAxiosError || event.reason?.config?._isErrorReport) return
        reportError(event.reason, null)
    })
}
