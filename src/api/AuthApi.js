/**
 * AuthApi.js
 *
 * Spring Boot auth servisiyle iletişim kurar.
 * useAuth composable'ı ile entegredir — tüm auth state buradan beslenir.
 */

import apiClient from './axios.js'
import { useAuth } from '../composables/useAuth.js'

// ─── Login ──────────────────────────────────────────────────────────────────

export const login = async (email, password) => {
    const { data } = await apiClient.post('/api/auth/login', { email, password })
    const auth = useAuth()

    // jwt token'ı ve kullanıcı bilgisini reaktif store'a yaz
    auth.setToken(data.jwt)
    auth.setUser({
        name: data.name,
        email: data.email,
        // Profil henüz tam değil — systemRole vb. /users/profile'dan gelecek
        // Şimdilik defaults ile çalış, fetchProfile ile zenginleştirilebilir
        systemRole: 'USER',
        status: 'ACTIVE',
    })

    return data
}

// ─── Register ────────────────────────────────────────────────────────────────

export const register = async (email, password, name) => {
    const { data } = await apiClient.post('/api/auth/register', { email, password, name })
    const auth = useAuth()

    auth.setToken(data.jwt)
    auth.setUser({
        name: data.name,
        email: data.email,
        systemRole: 'USER',
        status: 'ACTIVE',
    })

    return data
}

// ─── Logout ──────────────────────────────────────────────────────────────────

export const logout = () => {
    const auth = useAuth()
    auth.logout() // jwt + user + selectedTeam temizle
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

    // Mevcut kullanıcı adını güncelle
    const auth = useAuth()
    if (auth.user.value) {
        auth.setUser({ ...auth.user.value, name: data.name })
    }

    return data
}
