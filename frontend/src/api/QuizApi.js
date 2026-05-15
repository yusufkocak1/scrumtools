/**
 * QuizApi.js
 *
 * Quiz REST API ile iletişim kurar.
 *
 * WebSocket stratejisi: Data-Carrying
 *   /topic/quiz/{teamId}/state     → oturum durumu (lobby, aktif soru, leaderboard, bitiş)
 *   /topic/quiz/{teamId}/answered  → cevaplayan sayısı güncellemesi
 */

import apiClient from './axios.js'

const base = (teamId) => `/api/teams/${teamId}/quiz`

// ─── Template CRUD ──────────────────────────────────────────────────────────

export const getTemplates = async (teamId) => {
    const { data } = await apiClient.get(`${base(teamId)}/templates`)
    return data
}

export const getTemplate = async (teamId, templateId) => {
    const { data } = await apiClient.get(`${base(teamId)}/templates/${templateId}`)
    return data
}

export const createTemplate = async (teamId, payload) => {
    const { data } = await apiClient.post(`${base(teamId)}/templates`, payload)
    return data
}

export const updateTemplate = async (teamId, templateId, payload) => {
    const { data } = await apiClient.put(`${base(teamId)}/templates/${templateId}`, payload)
    return data
}

export const deleteTemplate = async (teamId, templateId) => {
    await apiClient.delete(`${base(teamId)}/templates/${templateId}`)
}

// ─── Session ────────────────────────────────────────────────────────────────

export const startSession = async (teamId, templateId) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions`, { templateId })
    return data
}

export const getActiveSession = async (teamId) => {
    const response = await apiClient.get(`${base(teamId)}/sessions/active`)
    // 204 No Content → aktif oturum yok
    if (response.status === 204 || !response.data) return null
    return response.data
}

export const getSessionHistory = async (teamId) => {
    const { data } = await apiClient.get(`${base(teamId)}/sessions/history`)
    return data
}

export const getSession = async (teamId, sessionId) => {
    const { data } = await apiClient.get(`${base(teamId)}/sessions/${sessionId}`)
    return data
}

export const joinSession = async (teamId, sessionId) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions/${sessionId}/join`)
    return data
}

export const nextQuestion = async (teamId, sessionId) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions/${sessionId}/next`)
    return data
}

export const submitAnswer = async (teamId, sessionId, payload) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions/${sessionId}/answer`, payload)
    return data
}

export const showQuestionResult = async (teamId, sessionId) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions/${sessionId}/show-result`)
    return data
}

export const finishSession = async (teamId, sessionId) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions/${sessionId}/finish`)
    return data
}

export const getSessionReport = async (teamId, sessionId) => {
    const { data } = await apiClient.get(`${base(teamId)}/sessions/${sessionId}/report`)
    return data
}

