/**
 * RetroBoardApi.js
 *
 * Spring Boot RetroBoard REST API ile iletişim kurar.
 * Firebase RetroBoardService.js'in yerini alır.
 *
 * WebSocket stratejisi: Notification-only
 *   Backend değişiklik olduğunda /topic/retro/{teamId}/{boardId} topic'ine
 *   { event, columnName, itemId } mesajı gönderir.
 *   Frontend bu mesajı alınca ilgili REST endpoint'i çağırarak güncel veriyi çeker.
 */

import apiClient from './axios.js'

const base = (teamId, boardId) =>
    `/api/teams/${teamId}/retro-boards${boardId ? '/' + boardId : ''}`

// ─── Boards ───────────────────────────────────────────────────────────────────

export const getBoards = async (teamId) => {
    const { data } = await apiClient.get(base(teamId))
    return data
}

export const getBoard = async (teamId, boardId) => {
    const { data } = await apiClient.get(base(teamId, boardId))
    return data
}

export const createBoard = async (teamId, retroBoardName, columns) => {
    const { data } = await apiClient.post(base(teamId), { retroBoardName, columns })
    return data
}

export const renameBoard = async (teamId, boardId, retroBoardName) => {
    const { data } = await apiClient.put(base(teamId, boardId), { retroBoardName })
    return data
}

export const deleteBoard = async (teamId, boardId) => {
    await apiClient.delete(base(teamId, boardId))
}

// ─── Items ────────────────────────────────────────────────────────────────────

export const getItems = async (teamId, boardId, columnName) => {
    const { data } = await apiClient.get(`${base(teamId, boardId)}/columns/${encodeURIComponent(columnName)}/items`)
    return data
}

export const createItem = async (teamId, boardId, columnName, value, owner) => {
    const { data } = await apiClient.post(
        `${base(teamId, boardId)}/columns/${encodeURIComponent(columnName)}/items`,
        { value, owner }
    )
    return data
}

export const updateItem = async (teamId, boardId, columnName, itemId, value) => {
    const { data } = await apiClient.put(
        `${base(teamId, boardId)}/columns/${encodeURIComponent(columnName)}/items/${itemId}`,
        { value }
    )
    return data
}

export const updateItemStatus = async (teamId, boardId, columnName, itemId, status) => {
    const { data } = await apiClient.patch(
        `${base(teamId, boardId)}/columns/${encodeURIComponent(columnName)}/items/${itemId}/status`,
        { status }
    )
    return data
}

export const deleteItem = async (teamId, boardId, columnName, itemId) => {
    await apiClient.delete(
        `${base(teamId, boardId)}/columns/${encodeURIComponent(columnName)}/items/${itemId}`
    )
}

// ─── Votes ────────────────────────────────────────────────────────────────────

/**
 * Oy toggle — aynı oya tekrar basınca kaldırır, farklı yönde basınca günceller.
 * @param {number} voteValue +1 veya -1
 */
export const toggleVote = async (teamId, boardId, columnName, itemId, voteValue) => {
    const { data } = await apiClient.post(
        `${base(teamId, boardId)}/columns/${encodeURIComponent(columnName)}/items/${itemId}/votes`,
        { voteValue }
    )
    return data
}

// ─── Comments ─────────────────────────────────────────────────────────────────

export const addComment = async (teamId, boardId, columnName, itemId, value) => {
    const { data } = await apiClient.post(
        `${base(teamId, boardId)}/columns/${encodeURIComponent(columnName)}/items/${itemId}/comments`,
        { value }
    )
    return data
}

export const getComments = async (teamId, boardId, columnName, itemId) => {
    const { data } = await apiClient.get(
        `${base(teamId, boardId)}/columns/${encodeURIComponent(columnName)}/items/${itemId}/comments`
    )
    return data
}

export const deleteComment = async (teamId, boardId, columnName, itemId, commentId) => {
    await apiClient.delete(
        `${base(teamId, boardId)}/columns/${encodeURIComponent(columnName)}/items/${itemId}/comments/${commentId}`
    )
}

