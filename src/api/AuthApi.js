/**
 * AuthApi.js
 *
 * Spring Boot auth servisiyle iletişim kurar.
 */

import apiClient from './axios.js'

// ─── Login ──────────────────────────────────────────────────────────────────

export const login = async (email, password) => {
    const { data } = await apiClient.post('/api/auth/login', { email, password })

    localStorage.setItem('jwt', data.jwt)
    localStorage.setItem('user', data.email)

    return data
}

// ─── Register ────────────────────────────────────────────────────────────────

export const register = async (email, password, name) => {
    const { data } = await apiClient.post('/api/auth/register', { email, password, name })

    localStorage.setItem('jwt', data.jwt)
    localStorage.setItem('user', data.email)

    return data
}

// ─── Logout ──────────────────────────────────────────────────────────────────

export const logout = () => {
    localStorage.removeItem('jwt')
    localStorage.removeItem('user')
    localStorage.removeItem('selectedTeam')
}

// ─── Me (oturum kontrolü) ────────────────────────────────────────────────────

export const me = async () => {
    const { data } = await apiClient.get('/api/auth/me')
    return data
}

// ─── Change Password ─────────────────────────────────────────────────────────

export const changePassword = async (newPassword) => {
    await apiClient.put('/api/auth/change-password', { newPassword })
}

// ─── Update Display Name ──────────────────────────────────────────────────────

export const updateName = async (name) => {
    const { data } = await apiClient.put('/api/auth/update-name', { name })

    return data
}

