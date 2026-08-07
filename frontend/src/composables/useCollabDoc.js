import { ref, shallowRef, onBeforeUnmount } from 'vue'
import * as Y from 'yjs'
import { Awareness, applyAwarenessUpdate, encodeAwarenessUpdate, removeAwarenessStates } from 'y-protocols/awareness'
import CollabApi from '../api/CollabApi.js'

/**
 * Bir ortak çalışma dokümanının yaşam döngüsü (COLLAB_WORKSPACE_PLAN.md §7).
 *
 * Üç editör tipi de (TEXT / CODE / SHEET) bu sözleşmeyi paylaşır; aralarındaki
 * tek fark `ydoc` içindeki hangi paylaşımlı tipe bağlandıklarıdır.
 *
 * Mimarinin özeti: sunucu **aptal röledir** (plan K2). Birleştirmeyi Yjs burada,
 * tarayıcıda yapar. Sunucudan gelen tek "akıl" yetki kararları ve yazar
 * atamasıdır.
 */

// Çerçeve tipleri — backend'deki CollabProtocol ile birebir aynı olmalı.
const MESSAGE_SYNC = 0
const MESSAGE_AWARENESS = 1
const MESSAGE_CONTROL = 2

const CLOSE_FORBIDDEN = 4403
const CLOSE_QUOTA_EXCEEDED = 4429

/** Yjs güncellemelerinin biriktirildiği pencere (§12 madde 1). */
const UPDATE_BATCH_MS = 200
/** Yazar bu kadar sessizlikten sonra anlık görüntü gönderir (plan K6). */
const SNAPSHOT_IDLE_MS = 30_000
/** Hiç susmayan dokümanda üst sınır — bu kadar sonra yine kaydedilir. */
const SNAPSHOT_MAX_INTERVAL_MS = 5 * 60_000

const RECONNECT_MIN_MS = 1_000
const RECONNECT_MAX_MS = 30_000

export function useCollabDoc(projectId, documentId) {
    const ydoc = new Y.Doc()
    const awareness = new Awareness(ydoc)

    const socket = shallowRef(null)
    /** 'connecting' | 'synced' | 'offline' | 'forbidden' */
    const status = ref('connecting')
    const canWrite = ref(false)
    const isWriter = ref(false)
    const participants = ref([])
    const lastSavedAt = ref(null)
    const pendingChanges = ref(false)

    let destroyed = false
    let lastSeq = 0
    let reconnectDelay = RECONNECT_MIN_MS
    let reconnectTimer = null

    let pendingUpdates = []
    let batchTimer = null

    let snapshotIdleTimer = null
    let snapshotMaxTimer = null
    let snapshotTextProvider = () => ''

    // ─── Bağlantı ────────────────────────────────────────────────────────────

    /**
     * Her bağlanışta önce REST'ten tam durum çekilir, sonra WS açılır.
     *
     * Yeniden bağlanmada da böyle: kopukken başkalarının yazdıklarını yakalamanın
     * güvenilir tek yolu budur ve `since` değeri tazelenir. Yjs güncellemeleri
     * etkisiz-tekrarlanabilir olduğundan aynı paketi ikinci kez uygulamak zararsız.
     */
    async function connect() {
        if (destroyed) return
        status.value = 'connecting'
        try {
            await loadState()
        } catch (e) {
            // Yetki hatasında yeniden denemek anlamsız; ağ hatasında geri çekil.
            if (e?.response?.status === 403) {
                status.value = 'forbidden'
                return
            }
            scheduleReconnect()
            return
        }
        openSocket()
    }

    async function loadState() {
        const { data } = await CollabApi.getState(projectId, documentId)
        canWrite.value = !!data.canWrite
        lastSeq = data.lastSeq ?? 0

        // Tek bir transaction: editör her paket için ayrı ayrı yeniden çizmesin.
        ydoc.transact(() => {
            if (data.state) Y.applyUpdate(ydoc, base64ToBytes(data.state), 'remote')
            for (const update of data.updates || []) {
                Y.applyUpdate(ydoc, base64ToBytes(update), 'remote')
            }
        }, 'remote')
    }

    function openSocket() {
        const ws = new WebSocket(buildWsUrl(documentId, lastSeq))
        ws.binaryType = 'arraybuffer'
        socket.value = ws

        ws.onopen = () => {
            reconnectDelay = RECONNECT_MIN_MS
            status.value = 'synced'
            // Bağlantı kurulur kurulmaz kendi varlığımızı duyur; aksi hâlde
            // diğerleri bizi ancak ilk imleç hareketinde görür.
            broadcastAwareness([ydoc.clientID])
        }

        ws.onmessage = (event) => handleFrame(new Uint8Array(event.data))

        ws.onclose = (event) => {
            socket.value = null
            if (destroyed) return
            if (event.code === CLOSE_FORBIDDEN) {
                status.value = 'forbidden'
                return
            }
            status.value = 'offline'
            if (event.code === CLOSE_QUOTA_EXCEEDED) {
                // Kota geçici bir durumdur (biri çıkınca yer açılır), ama hızlı
                // yeniden denemek sunucuyu döver — uzun aralıkla bekle.
                reconnectDelay = RECONNECT_MAX_MS
            }
            scheduleReconnect()
        }

        ws.onerror = () => { /* onclose zaten devreye girecek */ }
    }

    function scheduleReconnect() {
        if (destroyed || reconnectTimer) return
        status.value = 'offline'
        // Üstel geri çekilme + jitter: yeniden başlatmadan sonra tüm istemcilerin
        // aynı anda geri gelmesi, dar sunucuyu (plan D3) ikinci kez düşürür.
        const jitter = Math.random() * 0.3 * reconnectDelay
        const delay = reconnectDelay + jitter
        reconnectTimer = setTimeout(() => {
            reconnectTimer = null
            connect()
        }, delay)
        reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_MS)
    }

    // ─── Çerçeve işleme ──────────────────────────────────────────────────────

    function handleFrame(frame) {
        if (frame.length < 1) return
        const body = frame.subarray(1)
        switch (frame[0]) {
            case MESSAGE_SYNC:
                // 'remote' kaynağı, kendi observer'ımızın bu güncellemeyi geri
                // göndermesini engeller (sonsuz yankı).
                Y.applyUpdate(ydoc, body, 'remote')
                break
            case MESSAGE_AWARENESS:
                applyAwarenessUpdate(awareness, body, 'remote')
                break
            case MESSAGE_CONTROL:
                handleControl(body)
                break
        }
    }

    function handleControl(body) {
        let message
        try {
            message = JSON.parse(new TextDecoder().decode(body))
        } catch {
            return
        }
        if (message.type === 'hello') {
            canWrite.value = !!message.canWrite
            isWriter.value = !!message.isWriter
            // Renk sunucudan gelir: imleç etiketi ile katılımcı rozeti aynı
            // rengi kullansın diye tek kaynaktan dağıtılıyor.
            awareness.setLocalStateField('user', {
                name: message.name,
                email: message.email,
                color: message.color
            })
            broadcastAwareness([ydoc.clientID])
            scheduleSnapshotTimers()
        } else if (message.type === 'writer') {
            isWriter.value = !!message.isWriter
            scheduleSnapshotTimers()
        } else if (message.type === 'snapshotRequest') {
            sendSnapshot()
        }
    }

    function send(type, payload) {
        const ws = socket.value
        if (!ws || ws.readyState !== WebSocket.OPEN) return
        const frame = new Uint8Array(payload.length + 1)
        frame[0] = type
        frame.set(payload, 1)
        ws.send(frame)
    }

    // ─── Giden güncellemeler ─────────────────────────────────────────────────

    ydoc.on('update', (update, origin) => {
        if (origin === 'remote') return
        pendingChanges.value = true
        pendingUpdates.push(update)
        if (!batchTimer) {
            batchTimer = setTimeout(flushUpdates, UPDATE_BATCH_MS)
        }
        restartSnapshotIdleTimer()
    })

    /**
     * Tuş başına paket göndermek yerine 200 ms'lik pencerede biriktirip
     * `Y.mergeUpdates` ile **tek** pakete indirger (§12 madde 1). Sunucudaki
     * append gruplaması bunun üstüne biner.
     */
    function flushUpdates() {
        batchTimer = null
        if (pendingUpdates.length === 0) return
        const merged = pendingUpdates.length === 1
            ? pendingUpdates[0]
            : Y.mergeUpdates(pendingUpdates)
        pendingUpdates = []
        send(MESSAGE_SYNC, merged)
    }

    awareness.on('update', ({ added, updated, removed }) => {
        broadcastAwareness([...added, ...updated, ...removed])
        participants.value = collectParticipants()
    })

    function broadcastAwareness(clients) {
        if (clients.length === 0) return
        send(MESSAGE_AWARENESS, encodeAwarenessUpdate(awareness, clients))
    }

    function collectParticipants() {
        const seen = new Map()
        awareness.getStates().forEach((state, clientId) => {
            const user = state?.user
            if (!user?.email) return
            // Aynı kişinin iki sekmesi tek rozet olsun.
            if (!seen.has(user.email)) {
                seen.set(user.email, { ...user, clientId, self: clientId === ydoc.clientID })
            }
        })
        return [...seen.values()]
    }

    // ─── Anlık görüntü (plan K6) ─────────────────────────────────────────────

    /**
     * Editör, okunabilir çıktıyı üreten fonksiyonu buraya kaydeder
     * (TEXT → HTML, CODE → düz metin). Sunucu bunu üretemez: orada Yjs yok.
     */
    function setSnapshotTextProvider(fn) {
        snapshotTextProvider = fn
    }

    async function sendSnapshot() {
        if (!isWriter.value || !canWrite.value) return
        // Bekleyen paketler önce gitsin; yoksa sunucudaki lastSeq anlık
        // görüntünün gerisinde kalır ve sıkıştırma erken budama yapar.
        flushUpdates()
        try {
            await CollabApi.saveSnapshot(projectId, documentId, {
                state: bytesToBase64(Y.encodeStateAsUpdate(ydoc)),
                stateVector: bytesToBase64(Y.encodeStateVector(ydoc)),
                snapshotText: snapshotTextProvider() ?? '',
                seq: lastSeq
            })
            pendingChanges.value = false
            lastSavedAt.value = new Date()
        } catch {
            // Kaydedilemedi ama içerik kaybolmadı: ham güncellemeler zaten
            // sunucudaki append log'unda. Bir sonraki tetikte tekrar denenir.
        }
    }

    function restartSnapshotIdleTimer() {
        if (!isWriter.value) return
        clearTimeout(snapshotIdleTimer)
        snapshotIdleTimer = setTimeout(sendSnapshot, SNAPSHOT_IDLE_MS)
    }

    function scheduleSnapshotTimers() {
        clearInterval(snapshotMaxTimer)
        if (!isWriter.value) return
        // Sürekli yazılan dokümanda boşta kalma hiç oluşmaz; üst sınır zamanlayıcısı
        // o durumda tek kayıt garantisidir.
        snapshotMaxTimer = setInterval(() => {
            if (pendingChanges.value) sendSnapshot()
        }, SNAPSHOT_MAX_INTERVAL_MS)
    }

    // ─── Temizlik ────────────────────────────────────────────────────────────

    async function destroy() {
        if (destroyed) return
        destroyed = true
        clearTimeout(reconnectTimer)
        clearTimeout(batchTimer)
        clearTimeout(snapshotIdleTimer)
        clearInterval(snapshotMaxTimer)

        // Sekme kapanırken son pencereyi ve (yazarsak) anlık görüntüyü kurtar.
        flushUpdates()
        if (isWriter.value && pendingChanges.value) {
            await sendSnapshot()
        }

        removeAwarenessStates(awareness, [ydoc.clientID], 'unmount')
        socket.value?.close(1000)
        awareness.destroy()
        ydoc.destroy()
    }

    onBeforeUnmount(destroy)
    connect()

    return {
        ydoc,
        awareness,
        status,
        canWrite,
        isWriter,
        participants,
        pendingChanges,
        lastSavedAt,
        setSnapshotTextProvider,
        requestSnapshot: sendSnapshot,
        reconnect: () => { reconnectDelay = RECONNECT_MIN_MS; connect() },
        destroy
    }
}

// ─── Yardımcılar ─────────────────────────────────────────────────────────────

/**
 * Tarayıcı WebSocket handshake'ine başlık ekleyemediği için JWT query string'de
 * gider. nginx bu yol için erişim log'unu kapatıyor (bkz. frontend/nginx.conf).
 */
function buildWsUrl(documentId, since) {
    const base = import.meta.env.VITE_WS_BASE_URL || '/ws'
    let root
    if (/^wss?:\/\//i.test(base)) {
        root = base
    } else if (/^https?:\/\//i.test(base)) {
        root = base.replace(/^http/i, 'ws')
    } else {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
        const path = base.startsWith('/') ? base : `/${base}`
        root = `${protocol}//${window.location.host}${path}`
    }
    const token = localStorage.getItem('jwt') || ''
    return `${root.replace(/\/$/, '')}/collab`
        + `?doc=${encodeURIComponent(documentId)}`
        + `&since=${encodeURIComponent(since)}`
        + `&token=${encodeURIComponent(token)}`
}

function base64ToBytes(base64) {
    const binary = atob(base64)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
    return bytes
}

function bytesToBase64(bytes) {
    // Parça parça: büyük dokümanlarda tek seferlik String.fromCharCode(...bytes)
    // çağrı yığınını taşırır.
    let binary = ''
    const CHUNK = 0x8000
    for (let i = 0; i < bytes.length; i += CHUNK) {
        binary += String.fromCharCode.apply(null, bytes.subarray(i, i + CHUNK))
    }
    return btoa(binary)
}
