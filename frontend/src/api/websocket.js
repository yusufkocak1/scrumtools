import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

const WS_URL = import.meta.env.VITE_WS_BASE_URL || '/ws'

let stompClient = null
let isActivating = false
const subscriptions = new Map()
const pendingConnectCallbacks = []

/**
 * WebSocket bağlantısını başlatır.
 * JWT token localStorage'dan okunur.
 * Otomatik reconnect aktiftir.
 *
 * @param {Function} [onConnect] - Bağlantı kurulduğunda çağrılır
 * @param {Function} [onError]   - Hata olduğunda çağrılır
 */
export function connect(onConnect, onError) {
    if (stompClient && stompClient.connected) {
        onConnect?.()
        return
    }

    // Zaten bağlanma sürecindeyse sadece callback'i kuyruğa ekle
    if (isActivating) {
        if (onConnect) pendingConnectCallbacks.push(onConnect)
        return
    }

    const token = localStorage.getItem('jwt')
    isActivating = true

    stompClient = new Client({
        // SockJS factory — tarayıcı uyumluluğu için
        webSocketFactory: () => new SockJS(WS_URL),

        // JWT ile authentication
        connectHeaders: {
            Authorization: token ? `Bearer ${token}` : ''
        },

        // Otomatik reconnect (5 saniye aralıkla)
        reconnectDelay: 5000,

        // Heartbeat
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,

        onConnect: () => {
            console.log('[WS] Connected')
            isActivating = false
            // İlk callback'i çağır
            onConnect?.()
            // Kuyruktaki tüm bekleyen callback'leri çağır
            while (pendingConnectCallbacks.length > 0) {
                const cb = pendingConnectCallbacks.shift()
                try { cb() } catch (e) { console.error('[WS] Pending callback error:', e) }
            }
        },

        onStompError: (frame) => {
            console.error('[WS] STOMP error:', frame.headers?.message)
            isActivating = false
            onError?.(frame)
        },

        onWebSocketClose: () => {
            console.warn('[WS] Connection closed, will reconnect...')
            isActivating = false
        }
    })

    stompClient.activate()
}

/**
 * Belirtilen topic'e subscribe olur.
 *
 * Notification modu (RetroBoard):
 *   Callback'e gelen mesajı parse edip ilgili REST fetch'i yapabilirsiniz.
 *
 * Data modu (ScrumPoker):
 *   Callback'e gelen veri doğrudan kullanılabilir.
 *
 * @param {string}   topic    - Örn: '/topic/retro/teamId/boardId' veya '/topic/poker/teamId/votes'
 * @param {Function} callback - Mesaj geldiğinde çağrılır, parametre olarak parse edilmiş JSON alır
 * @returns {string} subscriptionId - Unsubscribe için kullanılır
 */
export function subscribe(topic, callback) {
    if (!stompClient || !stompClient.connected) {
        console.warn('[WS] Not connected. Attempting to connect first...')
        connect(() => {
            _doSubscribe(topic, callback)
        })
        return topic // topic'i ID olarak kullan, bağlantı sonrası subscribe edilecek
    }
    return _doSubscribe(topic, callback)
}

function _doSubscribe(topic, callback) {
    // Zaten subscribe edilmişse önce unsubscribe et
    if (subscriptions.has(topic)) {
        subscriptions.get(topic).unsubscribe()
    }

    const sub = stompClient.subscribe(topic, (message) => {
        try {
            const body = JSON.parse(message.body)
            callback(body)
        } catch {
            callback(message.body)
        }
    })

    subscriptions.set(topic, sub)
    return topic
}

/**
 * Belirtilen topic'ten unsubscribe olur.
 *
 * @param {string} topic - Subscribe olurken kullanılan topic
 */
export function unsubscribe(topic) {
    if (subscriptions.has(topic)) {
        subscriptions.get(topic).unsubscribe()
        subscriptions.delete(topic)
    }
}

/**
 * Belirtilen destination'a mesaj gönderir.
 *
 * @param {string} destination - Örn: '/app/poker/teamId/vote'
 * @param {object} body        - Gönderilecek veri
 */
export function send(destination, body) {
    if (!stompClient || !stompClient.connected) {
        console.error('[WS] Cannot send — not connected')
        return
    }
    stompClient.publish({
        destination,
        body: JSON.stringify(body)
    })
}

/**
 * WebSocket bağlantısını kapatır ve tüm subscription'ları temizler.
 */
export function disconnect() {
    if (stompClient) {
        subscriptions.forEach((sub) => sub.unsubscribe())
        subscriptions.clear()
        pendingConnectCallbacks.length = 0
        isActivating = false
        stompClient.deactivate()
        stompClient = null
        console.log('[WS] Disconnected')
    }
}

/**
 * Bağlantı durumunu döndürür.
 *
 * @returns {boolean}
 */
export function isConnected() {
    return stompClient?.connected ?? false
}

export default {
    connect,
    disconnect,
    subscribe,
    unsubscribe,
    send,
    isConnected
}

