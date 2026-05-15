/**
 * CodeShareApi.js
 *
 * Spring Boot CodeShare REST API ile iletişim kurar.
 * Firebase codeShareService.js'in yerini alır.
 */

import apiClient from './axios.js'

// ─── Save or Update Code Share ────────────────────────────────────────────────

export const saveCodeShare = async (teamId, data, tag) => {
    const response = await apiClient.put(`/api/teams/${teamId}/code-shares/${encodeURIComponent(tag)}`, {
        data
    })
    return response.data
}

// ─── Get Code Share by Tag ────────────────────────────────────────────────────

export const getCodeShare = async (teamId, tag) => {
    const response = await apiClient.get(`/api/teams/${teamId}/code-shares/${encodeURIComponent(tag)}`)
    // 204 No Content durumunda null döner
    if (response.status === 204) {
        return null
    }
    return response.data
}

