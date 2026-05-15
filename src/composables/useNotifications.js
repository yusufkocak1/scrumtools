/**
 * useNotifications.js
 *
 * Bildirim durumunu yönetir:
 *  - İlk yükleme: REST API ile unread count
 *  - Gerçek zamanlı: WebSocket STOMP push
 *  - Sayfa odağa gelince: otomatik yenileme
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuth } from './useAuth.js'
import {
    getNotifications,
    getUnreadCount,
    markNotificationRead,
    markAllNotificationsRead
} from '../api/NotificationApi.js'
import { connect, subscribe, unsubscribe } from '../api/websocket.js'

export function useNotifications() {
    const auth = useAuth()
    const notifications = ref([])
    const unreadCount = ref(0)
    const loading = ref(false)
    const hasMore = ref(true)
    const currentPage = ref(0)
    const PAGE_SIZE = 20
    let wsTopic = null

    const userEmail = computed(() => auth.userEmail?.value)

    // ─── REST yükleme ──────────────────────────────────────────────────────────

    async function fetchUnreadCount() {
        try {
            unreadCount.value = await getUnreadCount()
        } catch (e) {
            console.warn('[useNotifications] unreadCount fetch failed:', e.message)
        }
    }

    async function loadNotifications(reset = false) {
        if (loading.value) return
        loading.value = true
        try {
            if (reset) {
                currentPage.value = 0
                hasMore.value = true
                notifications.value = []
            }
            const page = await getNotifications(currentPage.value, PAGE_SIZE)
            const items = page.content ?? []
            notifications.value = reset ? items : [...notifications.value, ...items]
            hasMore.value = !page.last
            currentPage.value++
            unreadCount.value = notifications.value.filter(n => !n.isRead).length
        } catch (e) {
            console.warn('[useNotifications] load failed:', e.message)
        } finally {
            loading.value = false
        }
    }

    // ─── Aksiyonlar ────────────────────────────────────────────────────────────

    async function markRead(notificationId) {
        await markNotificationRead(notificationId)
        const n = notifications.value.find(n => n.id === notificationId)
        if (n && !n.isRead) {
            n.isRead = true
            unreadCount.value = Math.max(0, unreadCount.value - 1)
        }
    }

    async function markAllRead() {
        await markAllNotificationsRead()
        notifications.value.forEach(n => { n.isRead = true })
        unreadCount.value = 0
    }

    // ─── WebSocket ─────────────────────────────────────────────────────────────

    function subscribeToWS() {
        const email = userEmail.value
        if (!email) return

        connect(() => {
            wsTopic = `/topic/user/${email}/notifications`
            subscribe(wsTopic, (newNotification) => {
                // Yeni bildirimi listenin başına ekle
                notifications.value.unshift(newNotification)
                if (!newNotification.isRead) {
                    unreadCount.value++
                }
            })
        })
    }

    function unsubscribeFromWS() {
        if (wsTopic) {
            unsubscribe(wsTopic)
            wsTopic = null
        }
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    onMounted(async () => {
        await fetchUnreadCount()
        subscribeToWS()

        // Sayfa odağa gelince unread count'u yenile
        window.addEventListener('focus', fetchUnreadCount)
    })

    onUnmounted(() => {
        unsubscribeFromWS()
        window.removeEventListener('focus', fetchUnreadCount)
    })

    return {
        notifications,
        unreadCount,
        loading,
        hasMore,
        loadNotifications,
        markRead,
        markAllRead,
        fetchUnreadCount
    }
}

