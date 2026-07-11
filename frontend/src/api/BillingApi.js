import api from './axios.js'

export const BillingApi = {
  // Org'un o anki efektif paket hakları (plan, limitler, kullanım)
  getEntitlements: (orgId) => api.get(`/api/organizations/${orgId}/entitlements`),

  // Satın alınabilir paket kartları
  getPlans: () => api.get('/api/plans'),

  // Satın alma başlat → { transactionId, paymentUrl }
  checkout: (orgId, { planCode, cycle }) =>
    api.post(`/api/organizations/${orgId}/billing/checkout`, { planCode, cycle }),

  // Ödeme geçmişi (PENDING ödeme polling'i de bunu kullanır)
  getTransactions: (orgId) =>
    api.get(`/api/organizations/${orgId}/billing/transactions`),
}

export default BillingApi
