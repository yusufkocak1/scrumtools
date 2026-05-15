/**
 * DashboardApi.js — Faz 6
 * Kişisel dashboard layout CRUD
 */

import apiClient from './axios.js'

// ─── Layout al ────────────────────────────────────────────────────────────────
export const getDashboardLayout = async () => {
    const { data } = await apiClient.get('/api/dashboards')
    return data
}

// ─── Tüm layout'u kaydet ──────────────────────────────────────────────────────
export const saveDashboardLayout = async (layout) => {
    const { data } = await apiClient.put('/api/dashboards', layout)
    return data
}

// ─── Tek widget ekle/güncelle ─────────────────────────────────────────────────
export const upsertWidget = async (widget) => {
    const { data } = await apiClient.put('/api/dashboards/widgets', widget)
    return data
}

// ─── Widget sil ───────────────────────────────────────────────────────────────
export const removeWidget = async (widgetId) => {
    const { data } = await apiClient.delete(`/api/dashboards/widgets/${widgetId}`)
    return data
}

