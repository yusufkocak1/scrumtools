/**
 * TeamApi.js
 *
 * Spring Boot Team REST API ile iletişim kurar.
 * Firebase TeamService.js'in yerini alır.
 */

import apiClient from './axios.js'

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

// ─── Create Team ──────────────────────────────────────────────────────────────

export const createTeam = async (teamName, teamCode, displayName) => {
    const { data } = await apiClient.post('/api/teams', {
        teamName,
        teamCode,
        displayName
    })
    return data
}

// ─── Join Team (by teamId) ────────────────────────────────────────────────────

export const joinTeam = async (teamId) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/members`)
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

// ─── Update Display Name Across All Teams ─────────────────────────────────────

export const updateDisplayNameAcrossTeams = async (displayName) => {
    await apiClient.put('/api/teams/display-name', { displayName })
}

