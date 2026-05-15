import api from './axios.js'

export const ProjectApi = {
  // Organizasyon altında proje oluştur
  create: (orgId, data) => api.post(`/api/organizations/${orgId}/projects`, data),

  // Organizasyonun projelerini listele
  getByOrg: (orgId) => api.get(`/api/organizations/${orgId}/projects`),

  // Proje detayı
  getById: (projectId) => api.get(`/api/projects/${projectId}`),

  // Proje güncelle
  update: (projectId, data) => api.put(`/api/projects/${projectId}`, data),

  // Proje sil (soft delete)
  delete: (projectId) => api.delete(`/api/projects/${projectId}`),

  // Üyeleri listele
  getMembers: (projectId) => api.get(`/api/projects/${projectId}/members`),

  // Üye ekle (memberType: 'MEMBER' | 'OBSERVER')
  addMember: (projectId, email, roleIds, memberType) =>
    api.post(`/api/projects/${projectId}/members`, null, { params: { email, roleIds, memberType } }),

  // Takımı toplu ekle (memberType: 'MEMBER' | 'OBSERVER')
  addTeam: (projectId, teamId, roleIds, memberType) =>
    api.post(`/api/projects/${projectId}/teams/${teamId}/members`, null, { params: { roleIds, memberType } }),

  // Üye rollerini güncelle (roleIds: UUID array)
  updateMemberRoles: (projectId, userId, roleIds) =>
    api.put(`/api/projects/${projectId}/members/${userId}/roles`, roleIds),

  // Üyeyi projeden çıkar
  removeMember: (projectId, userId) =>
    api.delete(`/api/projects/${projectId}/members/${userId}`),
}

export default ProjectApi
