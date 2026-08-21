/**
 * turnAlert.js
 *
 * "Sıra sende" uyarısı — kısa bir zil sesi ve sekme başlığı flaşı.
 *
 * Neden var: sırasını bekleyen oyuncu başka sekmeye geçtiğinde sırası geldiğini
 * fark etmiyor ve süre boşa doluyor. Uyarı, beklemeyi kısaltmıyor ama kaçırmayı
 * engelliyor.
 *
 * Ses dosya bağımlılığı olmadan Web Audio ile üretilir: iki notalık kısa bir
 * "ding-dong". Ses ikincil bir sinyal — tarayıcı izin vermezse (otomatik oynatma
 * politikası, AudioContext yok) sessizce vazgeçilir, hata gösterilmez.
 */

let audioCtx = null

/** Tarayıcı otomatik oynatmayı engellediyse bağlam askıda kalır; her çalmada uyandırılır. */
const context = () => {
    const Ctx = window.AudioContext || window.webkitAudioContext
    if (!Ctx) return null
    if (!audioCtx) audioCtx = new Ctx()
    if (audioCtx.state === 'suspended') audioCtx.resume()
    return audioCtx
}

/** İki notalık kısa zil. Sesi kısık tutuldu (0.18) — ofiste oynanıyor. */
export const playTurnChime = () => {
    try {
        const ctx = context()
        if (!ctx) return

        const now = ctx.currentTime
        for (const [freq, offset] of [[880, 0], [1320, 0.13]]) {
            const osc = ctx.createOscillator()
            const gain = ctx.createGain()
            osc.type = 'sine'
            osc.frequency.value = freq
            // Ani başlangıç/bitiş "klik" sesi yapar; hızlı rampalarla yumuşatılır.
            gain.gain.setValueAtTime(0.0001, now + offset)
            gain.gain.exponentialRampToValueAtTime(0.18, now + offset + 0.02)
            gain.gain.exponentialRampToValueAtTime(0.0001, now + offset + 0.26)
            osc.connect(gain).connect(ctx.destination)
            osc.start(now + offset)
            osc.stop(now + offset + 0.3)
        }
    } catch (e) {
        // Ses kritik değil — görsel uyarı zaten var.
    }
}

/**
 * Sekme başlığını verilen metinle dönüşümlü flaşlatır.
 *
 * @returns {Function} flaşı durduran ve özgün başlığı geri koyan fonksiyon.
 *                     Çağıran, iş bitince (sekmeye dönüldüğünde / bileşen kapanırken)
 *                     bunu çağırmak zorunda — aksi hâlde başlık takılı kalır.
 */
export const flashTabTitle = (text, intervalMs = 900) => {
    const original = document.title
    let showingAlert = false

    const timer = setInterval(() => {
        showingAlert = !showingAlert
        document.title = showingAlert ? text : original
    }, intervalMs)

    return () => {
        clearInterval(timer)
        document.title = original
    }
}
