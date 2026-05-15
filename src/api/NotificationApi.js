/**
 * NotificationApi.js
 *
 * Bildirim ve aktivite akışı API çağrıları.
 */

import apiClient from './axios.js'

// ─── Bildirimler ──────────────────────────────────────────────────────────────

export const getNotifications = async (page = 0, size = 20) => {
    const { data } = await apiClient.get('/api/notifications', { params: { page, size } })
    return data
}

export const getUnreadCount = async () => {
    const { data } = await apiClient.get('/api/notifications/unread-count')
    return data.count ?? 0
}

export const markNotificationRead = async (notificationId) => {
    await apiClient.patch(`/api/notifications/${notificationId}/read`)
}

export const markAllNotificationsRead = async () => {
    await apiClient.post('/api/notifications/mark-all-read')
}

// ─── Aktivite Akışı ───────────────────────────────────────────────────────────

export const getTeamActivity = async (teamId, page = 0, size = 20) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/activity`, { params: { page, size } })
    return data
}

