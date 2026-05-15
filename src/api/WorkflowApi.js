import axios from './axios.js'

export default {
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

  deleteStatus: (workflowId, statusId) =>
    axios.delete(`/api/workflows/${workflowId}/statuses/${statusId}`),

  // ─── Transition CRUD ───────────────────────────────────────────────────────
  addTransition: (workflowId, data) =>
    axios.post(`/api/workflows/${workflowId}/transitions`, data),

  deleteTransition: (workflowId, transitionId) =>
    axios.delete(`/api/workflows/${workflowId}/transitions/${transitionId}`),
}

