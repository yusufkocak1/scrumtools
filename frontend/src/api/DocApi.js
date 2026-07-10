import axios from './axios'

const BASE = (projectId) => `/api/projects/${projectId}/docs`

export default {
    // ─── Spaces ─────────────────────────────────────────────────────────────────
    getSpaces(projectId) {
        return axios.get(`${BASE(projectId)}/spaces`)
    },
    getSpace(projectId, spaceId) {
        return axios.get(`${BASE(projectId)}/spaces/${spaceId}`)
    },
    createSpace(projectId, data) {
        return axios.post(`${BASE(projectId)}/spaces`, data)
    },
    updateSpace(projectId, spaceId, data) {
        return axios.put(`${BASE(projectId)}/spaces/${spaceId}`, data)
    },
    deleteSpace(projectId, spaceId) {
        return axios.delete(`${BASE(projectId)}/spaces/${spaceId}`)
    },

    // ─── Pages ──────────────────────────────────────────────────────────────────
    getPageTree(projectId, spaceId) {
        return axios.get(`${BASE(projectId)}/spaces/${spaceId}/pages`)
    },
    getPage(projectId, spaceId, pageId) {
        return axios.get(`${BASE(projectId)}/spaces/${spaceId}/pages/${pageId}`)
    },
    createPage(projectId, spaceId, data) {
        return axios.post(`${BASE(projectId)}/spaces/${spaceId}/pages`, data)
    },
    updatePage(projectId, spaceId, pageId, data) {
        return axios.put(`${BASE(projectId)}/spaces/${spaceId}/pages/${pageId}`, data)
    },
    deletePage(projectId, spaceId, pageId) {
        return axios.delete(`${BASE(projectId)}/spaces/${spaceId}/pages/${pageId}`)
    },

    // ─── Versions ───────────────────────────────────────────────────────────────
    getVersions(projectId, spaceId, pageId) {
        return axios.get(`${BASE(projectId)}/spaces/${spaceId}/pages/${pageId}/versions`)
    },
    getVersion(projectId, spaceId, pageId, versionNumber) {
        return axios.get(`${BASE(projectId)}/spaces/${spaceId}/pages/${pageId}/versions/${versionNumber}`)
    },
    restoreVersion(projectId, spaceId, pageId, versionNumber) {
        return axios.post(`${BASE(projectId)}/spaces/${spaceId}/pages/${pageId}/versions/${versionNumber}/restore`)
    },

    // ─── Permissions ────────────────────────────────────────────────────────────
    getPermissions(projectId, { spaceId, pageId }) {
        const params = {}
        if (spaceId) params.spaceId = spaceId
        if (pageId) params.pageId = pageId
        return axios.get(`${BASE(projectId)}/permissions`, { params })
    },
    grantPermission(projectId, data) {
        return axios.post(`${BASE(projectId)}/permissions`, data)
    },
    delegatePermission(projectId, data) {
        return axios.post(`${BASE(projectId)}/permissions/delegate`, data)
    },
    revokePermission(projectId, permissionId) {
        return axios.delete(`${BASE(projectId)}/permissions/${permissionId}`)
    },
    searchPermissionTargets(projectId, type, q) {
        return axios.get(`${BASE(projectId)}/permissions/targets`, { params: { type, q } })
    },

    // ─── Attachments ────────────────────────────────────────────────────────────
    getAttachments(projectId, spaceId, pageId) {
        return axios.get(`${BASE(projectId)}/spaces/${spaceId}/pages/${pageId}/attachments`)
    },
    uploadAttachment(projectId, spaceId, pageId, file) {
        const formData = new FormData()
        formData.append('file', file)
        return axios.post(
            `${BASE(projectId)}/spaces/${spaceId}/pages/${pageId}/attachments`,
            formData,
            { headers: { 'Content-Type': 'multipart/form-data' } }
        )
    },
    downloadAttachment(projectId, attachmentId) {
        return axios.get(`${BASE(projectId)}/attachments/${attachmentId}/download`, {
            responseType: 'blob'
        })
    },
    deleteAttachment(projectId, attachmentId) {
        return axios.delete(`${BASE(projectId)}/attachments/${attachmentId}`)
    }
}

