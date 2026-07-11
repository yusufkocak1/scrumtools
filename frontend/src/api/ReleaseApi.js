/**
 * ReleaseApi.js
 *
 * Proje seviyesinde sürüm (release) yönetimi REST API'si.
 * Release yaşam döngüsü: OPEN → CODE_FREEZE → REGRESSION → APPROVED → RELEASED (+ CANCELLED)
 * RELEASED geçişinde dağıtım tarihçesi backend'de snapshot'lanır.
 */

import apiClient from './axios.js'

// ─── Proje bazlı release CRUD ─────────────────────────────────────────────────

export const getProjectReleases = async (projectId) => {
    const { data } = await apiClient.get(`/api/projects/${projectId}/releases`)
    return data
}

export const createRelease = async (projectId, releaseData) => {
    const { data } = await apiClient.post(`/api/projects/${projectId}/releases`, releaseData)
    return data
}

export const updateRelease = async (projectId, releaseId, releaseData) => {
    const { data } = await apiClient.put(`/api/projects/${projectId}/releases/${releaseId}`, releaseData)
    return data
}

export const deleteRelease = async (projectId, releaseId) => {
    await apiClient.delete(`/api/projects/${projectId}/releases/${releaseId}`)
}

export const updateReleaseStatus = async (projectId, releaseId, status) => {
    const { data } = await apiClient.patch(`/api/projects/${projectId}/releases/${releaseId}/status`, { status })
    return data
}

export const getReleaseTasks = async (projectId, releaseId) => {
    const { data } = await apiClient.get(`/api/projects/${projectId}/releases/${releaseId}/tasks`)
    return data
}

export const getReleaseDeployments = async (projectId, releaseId) => {
    const { data } = await apiClient.get(`/api/projects/${projectId}/releases/${releaseId}/deployments`)
    return data
}

// ─── Takım bazlı kolaylık endpoint'leri ───────────────────────────────────────

/**
 * Takımın bağlı olduğu projenin release'leri.
 * Takım bir projeye bağlı değilse boş liste döner.
 */
export const getTeamReleases = async (teamId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/releases`)
    return data
}

/**
 * Task'ın dağıtım tarihçesi — hangi release ile ne zaman yayınlanmış.
 */
export const getTaskDeployments = async (teamId, taskId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/tasks/${taskId}/deployments`)
    return data
}
