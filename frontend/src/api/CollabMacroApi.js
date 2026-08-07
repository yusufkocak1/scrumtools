import axios from './axios'

/**
 * Makro REST istemcisi (COLLAB_WORKSPACE_PLAN.md §6 / §9).
 *
 * Yürütme burada yok: makro tarayıcıdaki worker'da koşuyor (K8). Sunucudan
 * alınan tek şey **izin** ve kaynak; sonuç sonradan raporlanıyor.
 */
const BASE = (projectId) => `/api/projects/${projectId}/collab/macros`

export default {
    list(projectId, documentId) {
        return axios.get(BASE(projectId), {
            params: { documentId: documentId || undefined }
        })
    },

    create(projectId, data) {
        return axios.post(BASE(projectId), data)
    },

    update(projectId, macroId, data) {
        return axios.put(`${BASE(projectId)}/${macroId}`, data)
    },

    remove(projectId, macroId) {
        return axios.delete(`${BASE(projectId)}/${macroId}`)
    },

    /** Onay kaynağın özetine verilir; kaynak değişince kendiliğinden düşer. */
    approve(projectId, macroId) {
        return axios.post(`${BASE(projectId)}/${macroId}/approve`)
    },

    revoke(projectId, macroId) {
        return axios.post(`${BASE(projectId)}/${macroId}/revoke`)
    },

    /** İzin ister; reddedilirse 403 döner ve deneme sunucuda DENIED olarak kaydedilir. */
    run(projectId, macroId, trigger) {
        return axios.post(`${BASE(projectId)}/${macroId}/run`, null, {
            params: { trigger: trigger || undefined }
        })
    },

    completeRun(projectId, runId, report) {
        return axios.post(`${BASE(projectId)}/runs/${runId}/complete`, report)
    },

    runs(projectId, macroId) {
        return axios.get(`${BASE(projectId)}/${macroId}/runs`)
    }
}
