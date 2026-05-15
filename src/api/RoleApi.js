import api from './axios.js'

export const RoleApi = {
  // Tüm rolleri getir
  getAll: (scope) => api.get('/api/roles', { params: scope ? { scope } : {} }),

  // Varsayılan rolleri getir
  getDefaults: () => api.get('/api/roles/defaults'),

  // Tüm izinleri listele
  getPermissions: () => api.get('/api/roles/permissions'),

  // Özel rol oluştur
  create: (data) => api.post('/api/roles', data),

  // Rol güncelle
  update: (roleId, data) => api.put(`/api/roles/${roleId}`, data),

  // Rol sil
  delete: (roleId) => api.delete(`/api/roles/${roleId}`),
}

export default RoleApi

