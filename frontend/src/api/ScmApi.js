/**
 * ScmApi.js
 *
 * Git / SCM entegrasyonu REST API'si (GitHub, GitLab; Bitbucket/Gitea Faz 3'te).
 * - Org bağlantıları: sadece ORG_OWNER/ORG_ADMIN
 * - Repo eşleme: PROJECT_MANAGE_SETTINGS
 * - Task dev paneli: takım üyeliği
 * Token'lar hiçbir yanıtta yer almaz; sadece tokenHint (****abcd) döner.
 */

import apiClient from './axios.js'

// ─── Org bağlantı yönetimi ────────────────────────────────────────────────────

export const getConnections = async (orgId, opts = {}) => {
    const { data } = await apiClient.get(`/api/organizations/${orgId}/scm/connections`, opts)
    return data
}

export const createConnection = async (orgId, connection) => {
    const { data } = await apiClient.post(`/api/organizations/${orgId}/scm/connections`, connection)
    return data
}

export const updateConnection = async (orgId, connectionId, connection) => {
    const { data } = await apiClient.put(`/api/organizations/${orgId}/scm/connections/${connectionId}`, connection)
    return data
}

export const deleteConnection = async (orgId, connectionId) => {
    await apiClient.delete(`/api/organizations/${orgId}/scm/connections/${connectionId}`)
}

/** Token'ı canlı test eder — başarısızlıkta da 200 döner: { ok, message, username } */
export const testConnection = async (orgId, connectionId) => {
    const { data } = await apiClient.post(`/api/organizations/${orgId}/scm/connections/${connectionId}/test`)
    return data
}

/** Sağlayıcıdan CANLI repo listesi (eşleme ekranı araması için). */
export const getRemoteRepos = async (orgId, connectionId, search = '', page = 1) => {
    const { data } = await apiClient.get(
        `/api/organizations/${orgId}/scm/connections/${connectionId}/repos`,
        { params: { search, page } }
    )
    return data
}

// ─── Repo–proje eşleme ────────────────────────────────────────────────────────

export const getProjectRepos = async (projectId) => {
    const { data } = await apiClient.get(`/api/projects/${projectId}/scm/repos`)
    return data
}

export const mapRepo = async (projectId, connectionId, externalId) => {
    const { data } = await apiClient.post(`/api/projects/${projectId}/scm/repos`, { connectionId, externalId })
    return data
}

export const unmapRepo = async (projectId, repoId) => {
    await apiClient.delete(`/api/projects/${projectId}/scm/repos/${repoId}`)
}

/** FAILED durumundaki webhook'u yeniden kurmayı dener. */
export const rewebhookRepo = async (projectId, repoId) => {
    const { data } = await apiClient.post(`/api/projects/${projectId}/scm/repos/${repoId}/rewebhook`)
    return data
}

/** Canlı branch listesi — branch açarken base seçimi için. */
export const getRepoBranches = async (projectId, repoId, search = '') => {
    const { data } = await apiClient.get(
        `/api/projects/${projectId}/scm/repos/${repoId}/branches`,
        { params: { search } }
    )
    return data
}

// ─── Task dev paneli ──────────────────────────────────────────────────────────

/**
 * Task'ın geliştirme verisi:
 * { featureEnabled, projectLinked, projectId, canCreateBranch, canManageRepos,
 *   hasUserAccount, repos, branches, commits }
 */
export const getTaskScm = async (teamId, taskId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/tasks/${taskId}/scm`)
    return data
}
