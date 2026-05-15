/**
 * ReportApi.js — Faz 6
 * Raporlama endpoint'leri
 */

import apiClient from './axios.js'

// ─── Takım özet metrikleri ────────────────────────────────────────────────────
export const getTeamSummary = async (teamId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/reports/summary`)
    return data
}

// ─── Sprint burndown ──────────────────────────────────────────────────────────
export const getBurndown = async (teamId, sprintId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/reports/burndown/${sprintId}`)
    return data
}

// ─── Velocity ─────────────────────────────────────────────────────────────────
export const getVelocity = async (teamId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/reports/velocity`)
    return data
}

// ─── Üye iş yükü ─────────────────────────────────────────────────────────────
export const getWorkload = async (teamId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/reports/workload`)
    return data
}

// ─── Oluşturulan vs çözülen ───────────────────────────────────────────────────
export const getCreatedVsResolved = async (teamId, days = 30) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/reports/created-vs-resolved`, {
        params: { days }
    })
    return data
}

// ─── Vadesi geçmiş ────────────────────────────────────────────────────────────
export const getOverdueTasks = async (teamId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/reports/overdue`)
    return data
}

