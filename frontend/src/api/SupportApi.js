import api from './axios.js'

// ─── Destek Talepleri (kullanıcı tarafı) ─────────────────────────────────────

export default {
    /** Yeni destek talebi oluştur. */
    async createTicket({ subject, description, category, errorTrackingCode }) {
        const { data } = await api.post('/api/support/tickets', {
            subject, description, category,
            errorTrackingCode: errorTrackingCode || null,
        })
        return data
    },

    /** Kendi taleplerimi listele. */
    async getMyTickets() {
        const { data } = await api.get('/api/support/tickets')
        return data
    },

    /** Talep detayı (mesajlar + ekler). */
    async getTicketDetail(ticketId) {
        const { data } = await api.get(`/api/support/tickets/${ticketId}`)
        return data
    },

    /** Talebe mesaj ekle. */
    async addMessage(ticketId, content) {
        const { data } = await api.post(`/api/support/tickets/${ticketId}/messages`, { content })
        return data
    },

    /** Talebe dosya yükle (multipart). */
    async uploadAttachment(ticketId, file, onProgress) {
        const formData = new FormData()
        formData.append('file', file)

        const { data } = await api.post(
            `/api/support/tickets/${ticketId}/attachments`,
            formData,
            {
                headers: { 'Content-Type': 'multipart/form-data' },
                onUploadProgress: onProgress
                    ? (e) => onProgress(Math.round((e.loaded * 100) / e.total))
                    : undefined,
            }
        )
        return data
    },

    /** Dosya sil. */
    async deleteAttachment(ticketId, attachmentId) {
        await api.delete(`/api/support/tickets/${ticketId}/attachments/${attachmentId}`)
    },

    /** Dosya indirme URL'i (backend proxy — presigned downloadUrl yoksa yedek). */
    getAttachmentDownloadUrl(ticketId, attachmentId) {
        const base = (api.defaults.baseURL || '').replace(/\/+$/, '')
        return `${base}/api/support/tickets/${ticketId}/attachments/${attachmentId}/download`
    },
}
