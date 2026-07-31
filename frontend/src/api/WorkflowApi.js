import axios from './axios.js'

export default {
  // ─── Durum kaydı (status registry) ─────────────────────────────────────────

  /**
   * Takım (ve varsa aktif proje) kapsamında geçerli durumlar.
   * Yanıt: { workflowId, workflowName, scope, statuses[], unmapped[] }
   * Workflow yoksa sunucu ilk çağrıda varsayılanı üretir — boş liste dönmez.
   */
  getStatuses: (teamId, projectId = null) =>
    axios.get(`/api/teams/${teamId}/statuses`, {
      params: projectId ? { projectId } : {},
    }),

  /**
   * Durum adı (küçük harf) → görev sayısı.
   * Sütun eşleme ekranı bir durumu board dışına almanın kaç işi etkilediğini
   * gösterebilsin diye ayrı uçtan gelir — katalog çağrısını ağırlaştırmaz.
   */
  getStatusCounts: (teamId, projectId = null) =>
    axios.get(`/api/teams/${teamId}/statuses/counts`, {
      params: projectId ? { projectId } : {},
    }),

  /** Ayarlar ekranının düzenlediği workflow (durumlar + geçişler). */
  getEffective: (teamId, projectId = null) =>
    axios.get(`/api/teams/${teamId}/workflows/effective`, {
      params: projectId ? { projectId } : {},
    }),

  /** Projeye takımdan bağımsız kendi durum setini verir. */
  provisionForProject: (projectId) =>
    axios.post(`/api/projects/${projectId}/workflows/provision`),

  // ─── Team Workflows ────────────────────────────────────────────────────────
  getByTeam: (teamId) =>
    axios.get(`/api/teams/${teamId}/workflows`),

  createForTeam: (teamId, data) =>
    axios.post(`/api/teams/${teamId}/workflows`, data),

  // ─── Project Workflows ─────────────────────────────────────────────────────
  getByProject: (projectId) =>
    axios.get(`/api/projects/${projectId}/workflows`),

  createForProject: (projectId, data) =>
    axios.post(`/api/projects/${projectId}/workflows`, data),

  // ─── Workflow CRUD ─────────────────────────────────────────────────────────
  get: (workflowId) =>
    axios.get(`/api/workflows/${workflowId}`),

  update: (workflowId, data) =>
    axios.put(`/api/workflows/${workflowId}`, data),

  delete: (workflowId) =>
    axios.delete(`/api/workflows/${workflowId}`),

  // ─── Status CRUD ───────────────────────────────────────────────────────────
  addStatus: (workflowId, data) =>
    axios.post(`/api/workflows/${workflowId}/statuses`, data),

  updateStatus: (workflowId, statusId, data) =>
    axios.put(`/api/workflows/${workflowId}/statuses/${statusId}`, data),

  /**
   * @param migrateToStatusId bu durumdaki görevlerin taşınacağı durum.
   *   Durumda görev varsa zorunlu; verilmezse sunucu 400 ile reddeder.
   */
  deleteStatus: (workflowId, statusId, migrateToStatusId = null) =>
    axios.delete(`/api/workflows/${workflowId}/statuses/${statusId}`, {
      params: migrateToStatusId ? { migrateTo: migrateToStatusId } : {},
    }),

  // ─── Transition CRUD ───────────────────────────────────────────────────────
  addTransition: (workflowId, data) =>
    axios.post(`/api/workflows/${workflowId}/transitions`, data),

  deleteTransition: (workflowId, transitionId) =>
    axios.delete(`/api/workflows/${workflowId}/transitions/${transitionId}`),
}

