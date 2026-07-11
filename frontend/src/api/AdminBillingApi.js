import api from './axios.js'

/** Superadmin müşteri/plan yönetimi API'si (backend: /api/admin/billing). */
export const AdminBillingApi = {
  // Müşteriler
  getOrganizations: () => api.get('/api/admin/billing/organizations'),

  // action: ACTIVATE | EXTEND | CHANGE_PLAN | EXTEND_TRIAL | CANCEL
  applySubscriptionAction: (orgId, payload) =>
    api.post(`/api/admin/billing/organizations/${orgId}/subscription`, payload),

  createPaymentLink: (orgId, { planCode, cycle }) =>
    api.post(`/api/admin/billing/organizations/${orgId}/payment-link`, { planCode, cycle }),

  getTransactions: (orgId) =>
    api.get(`/api/admin/billing/organizations/${orgId}/transactions`),

  setSuspended: (orgId, suspended) =>
    api.put(`/api/admin/billing/organizations/${orgId}/suspend`, null, { params: { suspended } }),

  // Planlar
  getPlans: () => api.get('/api/admin/billing/plans'),
  createPlan: (payload) => api.post('/api/admin/billing/plans', payload),
  updatePlan: (planId, payload) => api.put(`/api/admin/billing/plans/${planId}`, payload),
}

export default AdminBillingApi
