/**
 * CiApi.js
 *
 * CI/CD (Jenkins) entegrasyonu REST API'si.
 * - Org bağlantıları: sadece ORG_OWNER/ORG_ADMIN
 * - Job eşleme: PROJECT_MANAGE_SETTINGS
 * - Task deploy tetikleme: proje üyesi; release pipeline: release manager/org admin
 * Token'lar hiçbir yanıtta yer almaz; sadece tokenHint (****abcd) döner.
 */

import apiClient from './axios.js'

// ─── Org bağlantı yönetimi ────────────────────────────────────────────────────

export const getConnections = async (orgId, opts = {}) => {
    const { data } = await apiClient.get(`/api/organizations/${orgId}/ci/connections`, opts)
    return data
}

export const createConnection = async (orgId, connection) => {
    const { data } = await apiClient.post(`/api/organizations/${orgId}/ci/connections`, connection)
    return data
}

export const updateConnection = async (orgId, connectionId, connection) => {
    const { data } = await apiClient.put(`/api/organizations/${orgId}/ci/connections/${connectionId}`, connection)
    return data
}

export const deleteConnection = async (orgId, connectionId) => {
    await apiClient.delete(`/api/organizations/${orgId}/ci/connections/${connectionId}`)
}

/** Bağlantıyı canlı test eder — başarısızlıkta da 200: { ok, message, serverVersion, user, crumbRequired } */
export const testConnection = async (orgId, connectionId) => {
    const { data } = await apiClient.post(`/api/organizations/${orgId}/ci/connections/${connectionId}/test`)
    return data
}

/** Sunucudan CANLI job listesi (eşleme ekranı araması için) — folder'lar recursive açılır. */
export const getJobs = async (orgId, connectionId, search = '') => {
    const { data } = await apiClient.get(
        `/api/organizations/${orgId}/ci/connections/${connectionId}/jobs`,
        { params: { search } }
    )
    return data
}

/** Seçilen job'ın tanımlı parametreleri — şablon editörünü ön doldurmak için. */
export const getJobParameters = async (orgId, connectionId, jobFullName) => {
    const { data } = await apiClient.get(
        `/api/organizations/${orgId}/ci/connections/${connectionId}/jobs/params`,
        { params: { jobFullName } }
    )
    return data
}

// ─── Job–proje eşleme ─────────────────────────────────────────────────────────

export const getJobMappings = async (projectId) => {
    const { data } = await apiClient.get(`/api/projects/${projectId}/ci/job-mappings`)
    return data
}

export const createJobMapping = async (projectId, mapping) => {
    const { data } = await apiClient.post(`/api/projects/${projectId}/ci/job-mappings`, mapping)
    return data
}

export const updateJobMapping = async (projectId, mappingId, mapping) => {
    const { data } = await apiClient.put(`/api/projects/${projectId}/ci/job-mappings/${mappingId}`, mapping)
    return data
}

export const deleteJobMapping = async (projectId, mappingId) => {
    await apiClient.delete(`/api/projects/${projectId}/ci/job-mappings/${mappingId}`)
}

// ─── Task deploy ──────────────────────────────────────────────────────────────

/** Task deploy paneli tek çağrı: { featureEnabled, projectId, canDeploy, mappings, builds } */
export const getTaskDeployView = async (taskId) => {
    const { data } = await apiClient.get(`/api/tasks/${taskId}/ci`)
    return data
}

/** Aktif build'leri tazelemek için (polling) — sadece tarihçe döner. */
export const getTaskBuilds = async (taskId) => {
    const { data } = await apiClient.get(`/api/tasks/${taskId}/ci/builds`)
    return data
}

/** Deploy tetikler: { mappingId, branch, overrides? } */
export const deployTask = async (taskId, payload) => {
    const { data } = await apiClient.post(`/api/tasks/${taskId}/ci/deploy`, payload)
    return data
}

// ─── Release pipeline ─────────────────────────────────────────────────────────

/** Release pipeline paneli tek çağrı: { featureEnabled, releaseStatus, canRun, blockedReason, mappings, builds } */
export const getReleasePipelineView = async (releaseId) => {
    const { data } = await apiClient.get(`/api/releases/${releaseId}/ci`)
    return data
}

export const getReleaseBuilds = async (releaseId) => {
    const { data } = await apiClient.get(`/api/releases/${releaseId}/ci/builds`)
    return data
}

/** Pipeline tetikler: { mappingId, overrides? } */
export const runReleasePipeline = async (releaseId, payload) => {
    const { data } = await apiClient.post(`/api/releases/${releaseId}/ci/run`, payload)
    return data
}

// ─── Proje geneli tarihçe + manuel yenileme ───────────────────────────────────

export const getProjectBuilds = async (projectId, status = '', page = 0) => {
    const { data } = await apiClient.get(`/api/projects/${projectId}/ci/builds`, { params: { status, page } })
    return data
}

/** Tek build'i Jenkins'ten anında senkronize eder. */
export const refreshBuild = async (buildId) => {
    const { data } = await apiClient.post(`/api/ci/builds/${buildId}/refresh`)
    return data
}
