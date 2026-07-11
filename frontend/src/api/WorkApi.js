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

export const updateSprint = async (teamId, sprintId, sprintData) => {
    const { data } = await apiClient.put(`/api/teams/${teamId}/sprints/${sprintId}`, sprintData)
    return data
}

// ─── Attachments (MinIO) ──────────────────────────────────────────────────────

/**
 * Task'a dosya yükle.
 * @param {string} teamId
 * @param {string} taskId
 * @param {File} file — tarayıcı File nesnesi
 * @param {function} onProgress — yükleme progress callback (opsiyonel)
 * @returns {Promise<{id, fileName, fileSize, mimeType, uploadedBy, downloadUrl, createdAt}>}
 */
export const uploadAttachment = async (teamId, taskId, file, onProgress) => {
    const formData = new FormData()
    formData.append('file', file)

    const { data } = await apiClient.post(
        `/api/teams/${teamId}/tasks/${taskId}/attachments`,
        formData,
        {
            headers: { 'Content-Type': 'multipart/form-data' },
            onUploadProgress: onProgress
                ? (e) => onProgress(Math.round((e.loaded * 100) / e.total))
                : undefined
        }
    )
    return data
}

/**
 * Task'ın dosyalarını listele (pre-signed URL ile).
 */
export const getAttachments = async (teamId, taskId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/tasks/${taskId}/attachments`)
    return data
}

/**
 * Dosya indirme URL'i oluştur (backend proxy).
 */
export const getAttachmentDownloadUrl = (teamId, taskId, attachmentId) => {
    // baseURL '/' ise sondaki slash'i at; yoksa '//api/...' protokol-göreli URL oluşur
    const base = (apiClient.defaults.baseURL || '').replace(/\/+$/, '')
    return `${base}/api/teams/${teamId}/tasks/${taskId}/attachments/${attachmentId}/download`
}

/**
 * Dosya sil.
 */
export const deleteAttachment = async (teamId, taskId, attachmentId) => {
    await apiClient.delete(`/api/teams/${teamId}/tasks/${taskId}/attachments/${attachmentId}`)
}

// ─── Subtasks (Faz 3) ─────────────────────────────────────────────────────────

export const getSubtasks = async (teamId, taskId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/tasks/${taskId}/subtasks`)
    return data
}

export const createSubtask = async (teamId, parentTaskId, taskData) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/tasks`, {
        ...taskData,
        parentTaskId
    })
    return data
}

// ─── Task Links (Faz 3) ───────────────────────────────────────────────────────

export const getLinks = async (teamId, taskId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/tasks/${taskId}/links`)
    return data
}

export const createLink = async (teamId, taskId, targetTaskId, linkType) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/tasks/${taskId}/links`, {
        targetTaskId,
        linkType
    })
    return data
}

export const deleteLink = async (teamId, taskId, linkId) => {
    await apiClient.delete(`/api/teams/${teamId}/tasks/${taskId}/links/${linkId}`)
}

// ─── Task History (Faz 3) ─────────────────────────────────────────────────────

export const getHistory = async (teamId, taskId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/tasks/${taskId}/history`)
    return data
}

// ─── Watchers (Faz 3) ─────────────────────────────────────────────────────────

export const addWatcher = async (teamId, taskId, email) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/tasks/${taskId}/watchers`, { email })
    return data
}

export const removeWatcher = async (teamId, taskId, email) => {
    const { data } = await apiClient.delete(`/api/teams/${teamId}/tasks/${taskId}/watchers/${email}`)
    return data
}

// ─── Custom Fields (Faz 3) ────────────────────────────────────────────────────

export const getCustomFields = async (teamId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/custom-fields`)
    return data
}

export const createCustomField = async (teamId, fieldData) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/custom-fields`, fieldData)
    return data
}

export const updateCustomField = async (fieldId, fieldData) => {
    const { data } = await apiClient.put(`/api/custom-fields/${fieldId}`, fieldData)
    return data
}

export const deleteCustomField = async (fieldId) => {
    await apiClient.delete(`/api/custom-fields/${fieldId}`)
}

// ─── Faz 4: Task Filter ───────────────────────────────────────────────────────

/**
 * Dinamik task filtresi (Board, ListView için)
 * @param {string} teamId
 * @param {Object} filterRequest - { filters, sortBy, sortDir, page, size }
 */
export const filterTasks = async (teamId, filterRequest) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/tasks/filter`, filterRequest)
    return data
}

/**
 * Takım içi görev arama (typeahead) — customId veya başlık ile.
 * @returns [{ id, customId, title, status, issueType, hasParent }]
 */
export const searchTeamTasks = async (teamId, q) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/tasks/search`, { params: { q } })
    return data
}
