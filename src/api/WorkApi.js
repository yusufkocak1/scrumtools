/**
 * WorkApi.js
 *
 * Spring Boot Task & Sprint REST API ile iletişim kurar.
 * Firebase WorkService.js'in yerini alır.
 *
 * Tasklarda customFields (JSONB) ile yatay genişleyebilir yapı desteklenir.
 * İleride dashboard filtresi, arama ve raporlama özelliklerinde kullanılacak.
 */

import apiClient from './axios.js'

// ─── Tasks ────────────────────────────────────────────────────────────────────

/**
 * Takımın task'larını getir.
 * @param {string} teamId
 * @param {boolean} includeCancelled
 */
export const getTasks = async (teamId, includeCancelled = false) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/tasks`, {
        params: { includeCancelled }
    })
    return data
}

/**
 * Task detayını getir (UUID ile).
 */
export const getTask = async (teamId, taskId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/tasks/${taskId}`)
    return data
}

/**
 * customId (örn. "TEAM-5") ile kullanıcının tüm takımlarında arama.
 * @returns {{ id, teamId, customId, ... } | null}
 */
export const searchByCustomId = async (customId) => {
    try {
        const { data } = await apiClient.get('/api/tasks/search', { params: { customId } })
        return data
    } catch (e) {
        if (e.response?.status === 404) return null
        throw e
    }
}

/**
 * Yeni task oluştur.
 * @param {string} teamId
 * @param {Object} taskData — { title, description, status, issueType, priority, assignee,
 *                              developer, analyst, tester, storyPoints, labels, sprintId,
 *                              customFields: {} }
 */
export const createTask = async (teamId, taskData) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/tasks`, taskData)
    return data
}

/**
 * Task güncelle (tam replace).
 */
export const updateTask = async (teamId, taskId, taskData) => {
    const { data } = await apiClient.put(`/api/teams/${teamId}/tasks/${taskId}`, taskData)
    return data
}

/**
 * Sadece status güncelle.
 */
export const updateTaskStatus = async (teamId, taskId, status) => {
    const { data } = await apiClient.patch(`/api/teams/${teamId}/tasks/${taskId}/status`, { status })
    return data
}

/**
 * Task'ı sprint'e taşı veya backlog'a al (sprintId null/empty → backlog).
 */
export const assignTaskToSprint = async (teamId, taskId, sprintId) => {
    const { data } = await apiClient.put(`/api/teams/${teamId}/tasks/${taskId}`, {
        sprintId: sprintId || ''
    })
    return data
}

/**
 * Task sil.
 */
export const deleteTask = async (teamId, taskId) => {
    await apiClient.delete(`/api/teams/${teamId}/tasks/${taskId}`)
}

// ─── Comments ─────────────────────────────────────────────────────────────────

export const addComment = async (teamId, taskId, text) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/tasks/${taskId}/comments`, { text })
    return data
}

export const deleteComment = async (teamId, taskId, commentId) => {
    await apiClient.delete(`/api/teams/${teamId}/tasks/${taskId}/comments/${commentId}`)
}

// ─── Sprints ──────────────────────────────────────────────────────────────────

export const getSprints = async (teamId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/sprints`)
    return data
}

export const createSprint = async (teamId, sprintData) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/sprints`, sprintData)
    return data
}

export const updateSprintStatus = async (teamId, sprintId, status) => {
    const { data } = await apiClient.patch(`/api/teams/${teamId}/sprints/${sprintId}/status`, { status })
    return data
}

