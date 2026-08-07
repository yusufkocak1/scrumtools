/**
 * /ws/collab yük testi — COLLAB_WORKSPACE_PLAN.md Faz 0 iskeleti.
 *
 * Ölçtüğü üç şey (plan §12 / Faz 1 kabul kriterleri):
 *   • kayıp    — gönderilen paketlerin kaçı diğer istemcilere ulaşmadı
 *   • gecikme  — bir istemcinin yazdığını diğerlerinin görme süresi (p50/p95/max)
 *   • RAM      — backend container'ının bellek kullanımı (docker stats)
 *
 * Faz 0'da röle henüz yok ve DenyAllCollabDocumentAccessResolver her bağlantıyı
 * 4403 ile kapatıyor. Bu bekleniyor: bugün çalıştırıldığında betik uç noktanın
 * ayakta olduğunu ve yetkilendirmenin uygulandığını doğrular. Faz 1'de aynı betik
 * gerçek kayıp/gecikme sayılarını üretir.
 *
 * Kullanım:
 *   npm install
 *   COLLAB_DOC=<documentId> COLLAB_TOKEN=<jwt> node loadtest.js
 *
 * Token yerine kimlik bilgisi de verilebilir:
 *   COLLAB_EMAIL=... COLLAB_PASSWORD=... COLLAB_DOC=... node loadtest.js
 */

import { WebSocket } from 'ws'
import { execFile } from 'node:child_process'
import { promisify } from 'node:util'

const execFileAsync = promisify(execFile)

const CONFIG = {
    baseUrl: process.env.COLLAB_BASE_URL || 'http://localhost:8080',
    wsUrl: process.env.COLLAB_WS_URL || 'ws://localhost:8080/ws/collab',
    documentId: process.env.COLLAB_DOC || '',
    token: process.env.COLLAB_TOKEN || '',
    email: process.env.COLLAB_EMAIL || '',
    password: process.env.COLLAB_PASSWORD || '',
    clients: Number(process.env.COLLAB_CLIENTS || 20),
    durationMs: Number(process.env.COLLAB_DURATION_MS || 10 * 60 * 1000),
    // Her istemci bu aralıkla yazar. 200 ms, plan §12'deki istemci tarafı
    // toplama penceresiyle aynı — gerçek yazma temposunu taklit eder.
    sendIntervalMs: Number(process.env.COLLAB_SEND_INTERVAL_MS || 200),
    ramSampleMs: Number(process.env.COLLAB_RAM_SAMPLE_MS || 15000),
    backendContainer: process.env.COLLAB_BACKEND_CONTAINER || 'scrumtools-backend',
}

const stats = {
    sent: 0,
    received: 0,
    latencies: [],
    ramSamples: [],
    connected: 0,
    rejected: new Map(),   // kapanış kodu -> adet
}

/** Paket biçimi: [magic, clientId(1B), seq(4B), timestamp(8B)] — 14 bayt. */
const MAGIC = 0x7f

function encode(clientId, seq) {
    const buf = Buffer.alloc(14)
    buf.writeUInt8(MAGIC, 0)
    buf.writeUInt8(clientId, 1)
    buf.writeUInt32BE(seq, 2)
    buf.writeBigUInt64BE(BigInt(Date.now()), 6)
    return buf
}

function decode(buf) {
    if (buf.length < 14 || buf.readUInt8(0) !== MAGIC) return null
    return {
        clientId: buf.readUInt8(1),
        seq: buf.readUInt32BE(2),
        sentAt: Number(buf.readBigUInt64BE(6)),
    }
}

async function resolveToken() {
    if (CONFIG.token) return CONFIG.token
    if (!CONFIG.email || !CONFIG.password) {
        throw new Error('COLLAB_TOKEN ya da COLLAB_EMAIL + COLLAB_PASSWORD gerekli.')
    }
    const res = await fetch(`${CONFIG.baseUrl}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: CONFIG.email, password: CONFIG.password }),
    })
    if (!res.ok) throw new Error(`Giriş başarısız: HTTP ${res.status}`)
    const body = await res.json()
    const token = body.token || body.accessToken || body.jwt
    if (!token) throw new Error('Giriş yanıtında token alanı bulunamadı.')
    return token
}

function startClient(id, token) {
    const url = `${CONFIG.wsUrl}?doc=${encodeURIComponent(CONFIG.documentId)}&token=${encodeURIComponent(token)}`
    const ws = new WebSocket(url)
    let seq = 0
    let timer = null

    ws.on('open', () => {
        stats.connected++
        timer = setInterval(() => {
            if (ws.readyState !== WebSocket.OPEN) return
            ws.send(encode(id, seq++))
            stats.sent++
        }, CONFIG.sendIntervalMs)
    })

    ws.on('message', (data) => {
        const packet = decode(Buffer.from(data))
        if (!packet || packet.clientId === id) return   // kendi yankısı sayılmaz
        stats.received++
        stats.latencies.push(Date.now() - packet.sentAt)
    })

    ws.on('close', (code) => {
        if (timer) clearInterval(timer)
        if (code !== 1000 && code !== 1001) {
            stats.rejected.set(code, (stats.rejected.get(code) || 0) + 1)
        }
    })

    ws.on('error', () => { /* close olayında zaten sayılıyor */ })
    return ws
}

async function sampleRam() {
    try {
        const { stdout } = await execFileAsync('docker', [
            'stats', '--no-stream', '--format', '{{.MemUsage}}', CONFIG.backendContainer,
        ])
        const mib = parseMemUsage(stdout.trim())
        if (mib != null) stats.ramSamples.push(mib)
    } catch {
        // docker yoksa ya da container uzaktaysa RAM ölçümü atlanır
    }
}

/** "512.3MiB / 1GiB" → 512.3 */
function parseMemUsage(text) {
    const match = /^([\d.]+)\s*([KMG])iB/.exec(text)
    if (!match) return null
    const value = Number(match[1])
    return match[2] === 'G' ? value * 1024 : match[2] === 'K' ? value / 1024 : value
}

function percentile(sorted, p) {
    if (sorted.length === 0) return null
    const idx = Math.min(sorted.length - 1, Math.floor((p / 100) * sorted.length))
    return sorted[idx]
}

function report() {
    const sorted = [...stats.latencies].sort((a, b) => a - b)
    // Her paket, gönderen dışındaki her istemciye ulaşmalı.
    const expected = stats.sent * Math.max(0, CONFIG.clients - 1)
    const lossPct = expected === 0 ? null : (100 * (expected - stats.received)) / expected

    console.log('\n──────── Sonuç ────────')
    console.log(`Bağlanan istemci : ${stats.connected}/${CONFIG.clients}`)
    if (stats.rejected.size > 0) {
        for (const [code, count] of stats.rejected) {
            const note = code === 4403
                ? ' (yetkisiz — Faz 0\'da beklenen, plan K3)'
                : ''
            console.log(`Kapanış kodu ${code}: ${count} bağlantı${note}`)
        }
    }
    console.log(`Gönderilen       : ${stats.sent}`)
    console.log(`Alınan           : ${stats.received} / beklenen ${expected}`)
    console.log(`Kayıp            : ${lossPct == null ? 'ölçülemedi' : lossPct.toFixed(2) + '%'}`)
    console.log(`Gecikme p50/p95/max: ${percentile(sorted, 50) ?? '-'} / ${percentile(sorted, 95) ?? '-'} / ${sorted.at(-1) ?? '-'} ms`)
    if (stats.ramSamples.length > 0) {
        const min = Math.min(...stats.ramSamples)
        const max = Math.max(...stats.ramSamples)
        console.log(`Backend RAM      : ${min.toFixed(1)} → ${max.toFixed(1)} MiB (artış ${(max - min).toFixed(1)} MiB)`)
    } else {
        console.log('Backend RAM      : ölçülemedi (docker stats erişilemedi)')
    }
    console.log('\nKabul eşikleri (plan §15, Faz 1): gecikme p95 < 300 ms, kayıp 0%, RAM artışı < 100 MB')
}

async function main() {
    if (!CONFIG.documentId) {
        console.error('COLLAB_DOC (doküman UUID) tanımlanmalı.')
        process.exit(2)
    }
    const token = await resolveToken()

    console.log(`${CONFIG.clients} istemci ${CONFIG.wsUrl} adresine bağlanıyor, süre ${CONFIG.durationMs / 1000} sn...`)
    const clients = Array.from({ length: CONFIG.clients }, (_, i) => startClient(i, token))
    const ramTimer = setInterval(sampleRam, CONFIG.ramSampleMs)
    await sampleRam()

    await new Promise((resolve) => setTimeout(resolve, CONFIG.durationMs))

    clearInterval(ramTimer)
    await sampleRam()
    clients.forEach((ws) => ws.close())
    report()
}

main().catch((err) => {
    console.error(err.message)
    process.exit(1)
})
