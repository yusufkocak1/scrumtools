import api from './axios.js'

export const UserApi = {
  // Profilimi getir
  getProfile: () => api.get('/api/users/profile'),

  // Profili güncelle
  updateProfile: (data) => api.put('/api/users/profile', data),

  // --- Admin endpoints ---

  // Tüm kullanıcıları listele (SUPER_ADMIN)
  getAllUsers: () => api.get('/api/admin/users'),

  // Kullanıcı durumunu değiştir (SUPER_ADMIN)
  updateUserStatus: (userId, status) =>
    api.put(`/api/admin/users/${userId}/status`, null, { params: { status } }),

  // Sistem rolünü değiştir (SUPER_ADMIN)
  updateSystemRole: (userId, role) =>
    api.put(`/api/admin/users/${userId}/system-role`, null, { params: { role } }),
}

export default UserApi
