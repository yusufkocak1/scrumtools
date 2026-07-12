/**
 * TeamApi.js
 *
 * Spring Boot Team REST API ile iletişim kurar.
 * Takımlar artık organizasyona zorunlu bağlıdır.
 */

import apiClient from './axios.js'

// ─── Get Teams By Organisation (search) ──────────────────────────────────────

export const getTeamsByOrg = async (orgId, query = '') => {
    const { data } = await apiClient.get(`/api/organizations/${orgId}/teams`, {
        params: query ? { q: query } : {}
    })
    return data
}

// ─── Get My Teams ─────────────────────────────────────────────────────────────

export const getMyTeams = async () => {
    const { data } = await apiClient.get('/api/teams')
    return data
}

// ─── Get Team By ID ───────────────────────────────────────────────────────────

export const getTeamById = async (teamId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}`)
    return data
}

// ─── Create Team (org admin/owner only) ───────────────────────────────────────

export const createTeam = async (orgId, teamName, teamCode, displayName) => {
    const { data } = await apiClient.post(`/api/organizations/${orgId}/teams`, {
        teamName,
        teamCode,
        displayName
    })
    return data
}

// ─── Add Member To Team (org üyesi olmak zorunda) ─────────────────────────────

export const addMemberToTeam = async (orgId, teamId, email) => {
    const { data } = await apiClient.post(
        `/api/organizations/${orgId}/teams/${teamId}/members`,
        null,
        { params: { email } }
    )
    return data
}

// ─── Remove User From Team ────────────────────────────────────────────────────

export const removeMember = async (teamId, email) => {
    const { data } = await apiClient.delete(`/api/teams/${teamId}/members/${encodeURIComponent(email)}`)
    return data
}

// ─── Update Member Role & Skills ──────────────────────────────────────────────

export const updateMemberRole = async (teamId, email, role, skills = []) => {
    const { data } = await apiClient.put(
        `/api/teams/${teamId}/members/${encodeURIComponent(email)}/role`,
        { role, skills }
    )
    return data
}

// ─── Update Team Info (name, code) ────────────────────────────────────────────

export const updateTeam = async (teamId, teamName, teamCode) => {
    const { data } = await apiClient.put(`/api/teams/${teamId}`, {
        teamName,
        teamCode,
        displayName: null
    })
    return data
}

// ─── Link Team To Project (release yönetimi için) ─────────────────────────────

export const linkTeamToProject = async (teamId, projectId) => {
    const { data } = await apiClient.put(`/api/teams/${teamId}/project`, { projectId })
    return data
}

// ─── Update Display Name Across All Teams ─────────────────────────────────────

export const updateDisplayNameAcrossTeams = async (displayName) => {
    await apiClient.put('/api/teams/display-name', { displayName })
}
