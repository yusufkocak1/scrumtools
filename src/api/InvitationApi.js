import api from './axios.js'

export const InvitationApi = {
  // Davet gönder
  send: (data) => api.post('/api/invitations', data),

  // Bekleyen davetleri listele
  getPending: () => api.get('/api/invitations/pending'),

  // Daveti kabul et
  accept: (token) => api.post(`/api/invitations/${token}/accept`),

  // Daveti reddet
  decline: (token) => api.post(`/api/invitations/${token}/decline`),
}

export default InvitationApi

