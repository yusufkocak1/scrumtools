/**
 * DashboardApi.js — pano CRUD
 *
 * Bir takımda birden çok pano tutulur; düzen panonun içinde gelir, ayrı bir
 * istek gerektirmez (sekme değişimi anında olsun diye).
 */

import apiClient from './axios.js'

// ─── Listele ──────────────────────────────────────────────────────────────────
export const getDashboards = async (teamId) => {
    const { data } = await apiClient.get('/api/dashboards', { params: { teamId } })
    return data
}

// ─── Tek pano ─────────────────────────────────────────────────────────────────
export const getDashboard = async (dashboardId) => {
    const { data } = await apiClient.get(`/api/dashboards/${dashboardId}`)
    return data
}

// ─── Oluştur ──────────────────────────────────────────────────────────────────
export const createDashboard = async (teamId, payload) => {
    const { data } = await apiClient.post('/api/dashboards', payload, { params: { teamId } })
    return data
}

// ─── Ad / görünürlük / sıra (ve istenirse düzen) ──────────────────────────────
export const updateDashboard = async (dashboardId, payload) => {
    const { data } = await apiClient.put(`/api/dashboards/${dashboardId}`, payload)
    return data
}

// ─── Yalnız düzen ─────────────────────────────────────────────────────────────
export const saveDashboardLayout = async (dashboardId, layout) => {
    const { data } = await apiClient.put(`/api/dashboards/${dashboardId}/layout`, layout)
    return data
}

// ─── Kendi adına kopyala ──────────────────────────────────────────────────────
export const duplicateDashboard = async (dashboardId, name) => {
    const { data } = await apiClient.post(`/api/dashboards/${dashboardId}/duplicate`, { name })
    return data
}

// ─── Sil ──────────────────────────────────────────────────────────────────────
export const deleteDashboard = async (dashboardId) => {
    await apiClient.delete(`/api/dashboards/${dashboardId}`)
}
