/**
 * SavedFilterApi.js
 *
 * Kayıtlı filtreler — kaydedilmiş STQL sorguları.
 * Görünürlük: PRIVATE (yalnız sahibi) | TEAM (takım) | PROJECT (projedeki takımlar).
 * Düzenleme/silme her durumda yalnız sahibine açıktır.
 */

import apiClient from './axios.js'

/** Kullanıcının görebildiği filtreler — favoriler üstte döner. */
export const getSavedFilters = async (teamId, projectId = null) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/filters`, {
        params: projectId ? { projectId } : {}
    })
    return data
}

export const getSavedFilter = async (teamId, filterId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/filters/${filterId}`)
    return data
}

/**
 * @param {Object} filter
 * @param {string} filter.name
 * @param {string} [filter.description]
 * @param {string} filter.query — STQL metni
 * @param {'PRIVATE'|'TEAM'|'PROJECT'} [filter.visibility]
 * @param {string|null} [filter.projectId] — PROJECT görünürlüğünde zorunlu
 */
export const createSavedFilter = async (teamId, filter) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/filters`, filter)
    return data
}

export const updateSavedFilter = async (teamId, filterId, filter) => {
    const { data } = await apiClient.put(`/api/teams/${teamId}/filters/${filterId}`, filter)
    return data
}

export const deleteSavedFilter = async (teamId, filterId) => {
    await apiClient.delete(`/api/teams/${teamId}/filters/${filterId}`)
}

/** Yıldızlar veya yıldızı kaldırır; güncellenmiş filtre döner. */
export const setSavedFilterFavorite = async (teamId, filterId, favorite) => {
    const url = `/api/teams/${teamId}/filters/${filterId}/favorite`
    const { data } = favorite
        ? await apiClient.post(url)
        : await apiClient.delete(url)
    return data
}

/** Kayıtlı filtreyi çalıştırır — sorgu metnini istemciye taşımadan. */
export const runSavedFilter = async (teamId, filterId, { projectId = null, page = 0, size = 50 } = {}) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/filters/${filterId}/run`, {
        projectId, page, size
    })
    return data
}
