import api from './axios.js'

// ─── Admin: Destek Talepleri + Hata Kayıtları (sadece SUPER_ADMIN) ───────────

export default {
    // Destek talepleri
    async getTickets({ status, category, organizationId, page = 0, size = 20 } = {}) {
        const { data } = await api.get('/api/admin/support/tickets', {
            params: {
                status: status || undefined,
                category: category || undefined,
                organizationId: organizationId || undefined,
                page, size,
            },
        })
        return data
    },

    async getTicketDetail(ticketId) {
        const { data } = await api.get(`/api/admin/support/tickets/${ticketId}`)
        return data
    },

    async reply(ticketId, content, setWaitingUser = true) {
        const { data } = await api.post(
            `/api/admin/support/tickets/${ticketId}/messages`,
            { content },
            { params: { setWaitingUser } }
        )
        return data
    },

    async updateTicketStatus(ticketId, status) {
        const { data } = await api.put(`/api/admin/support/tickets/${ticketId}/status`, null, {
            params: { status },
        })
        return data
    },

    // Hata grupları
    async getErrorGroups({ status, page = 0, size = 20 } = {}) {
        const { data } = await api.get('/api/admin/errors/groups', {
            params: { status: status || undefined, page, size },
        })
        return data
    },

    async getErrorGroupDetail(groupId, page = 0, size = 10) {
        const { data } = await api.get(`/api/admin/errors/groups/${groupId}`, {
            params: { page, size },
        })
        return data
    },

    async updateErrorGroupStatus(groupId, status) {
        const { data } = await api.put(`/api/admin/errors/groups/${groupId}/status`, null, {
            params: { status },
        })
        return data
    },

    async getErrorEventByCode(trackingCode) {
        const { data } = await api.get(`/api/admin/errors/events/by-code/${trackingCode}`)
        return data
    },
}
