import api from './axios.js'

export const OrganizationApi = {
  // Organizasyon oluştur
  create: (data) => api.post('/api/organizations', data),

  // Kullanıcının organizasyonlarını getir
  getMyOrganizations: () => api.get('/api/organizations'),

  // Organizasyon detayı
  getById: (orgId) => api.get(`/api/organizations/${orgId}`),

  // Organizasyon güncelle
  update: (orgId, data) => api.put(`/api/organizations/${orgId}`, data),

  // Üyeleri listele
  getMembers: (orgId) => api.get(`/api/organizations/${orgId}/members`),

  // Üye ekle
  addMember: (orgId, email, role) =>
    api.post(`/api/organizations/${orgId}/members`, null, { params: { email, role } }),

  // Üye rolünü güncelle
  updateMemberRole: (orgId, userId, role) =>
    api.put(`/api/organizations/${orgId}/members/${userId}/role`, null, { params: { role } }),

  // Üye çıkar
  removeMember: (orgId, userId) =>
    api.delete(`/api/organizations/${orgId}/members/${userId}`),
}

export default OrganizationApi
