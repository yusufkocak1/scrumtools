/**
 * BoardApi.js — Faz 4
 * Board CRUD + Task filtre API çağrıları
 */

import apiClient from './axios.js'

// ─── Boards ───────────────────────────────────────────────────────────────────

export const getBoards = async (teamId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/boards`)
    return data
}

export const getBoard = async (teamId, boardId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/boards/${boardId}`)
    return data
}

export const createBoard = async (teamId, boardData) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/boards`, boardData)
    return data
}

export const updateBoard = async (teamId, boardId, boardData) => {
    const { data } = await apiClient.put(`/api/teams/${teamId}/boards/${boardId}`, boardData)
    return data
}

export const deleteBoard = async (teamId, boardId) => {
    await apiClient.delete(`/api/teams/${teamId}/boards/${boardId}`)
}

// ─── Task Filter ──────────────────────────────────────────────────────────────

/**
 * Dinamik task filtresi.
 * @param {string} teamId
 * @param {Object} filterRequest - { filters, sortBy, sortDir, page, size }
 * @returns {{ content, totalElements, totalPages, page, size }}
 */
export const filterTasks = async (teamId, filterRequest) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/tasks/filter`, filterRequest)
    return data
}

