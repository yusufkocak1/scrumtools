import axios from './axios'

/**
 * Ortak Çalışma Alanı REST istemcisi (COLLAB_WORKSPACE_PLAN.md §6).
 *
 * Buradan **içerik yazılmaz**: metin, CRDT deltaları hâlinde ayrı bir ham
 * WebSocket üzerinden akar (bkz. useCollabDoc.js). REST yalnızca üstveri,
 * açılış durumu ve anlık görüntü içindir.
 */
const BASE = (projectId) => `/api/projects/${projectId}/collab`

export default {
    listDocuments(projectId, { type, teamId, query } = {}) {
        return axios.get(`${BASE(projectId)}/documents`, {
            params: { type: type || undefined, teamId: teamId || undefined, query: query || undefined }
        })
    },

    createDocument(projectId, data) {
        return axios.post(`${BASE(projectId)}/documents`, data)
    },

    getDocument(projectId, documentId) {
        return axios.get(`${BASE(projectId)}/documents/${documentId}`)
    },

    /** CRDT anlık görüntüsü + sonrasındaki ham güncellemeler (base64). */
    getState(projectId, documentId) {
        return axios.get(`${BASE(projectId)}/documents/${documentId}/state`)
    },

    /** Yalnızca "yazar" seçilen istemci çağırır (plan K6). */
    saveSnapshot(projectId, documentId, data) {
        return axios.post(`${BASE(projectId)}/documents/${documentId}/snapshot`, data)
    },

    patchDocument(projectId, documentId, data) {
        return axios.patch(`${BASE(projectId)}/documents/${documentId}`, data)
    },

    archiveDocument(projectId, documentId) {
        return axios.delete(`${BASE(projectId)}/documents/${documentId}`)
    },

    // ─── Docs entegrasyonu (plan §8) ────────────────────────────────────────

    /** Y1 — sayfayı ortak düzenlemeye açar. Idempotent: varsa mevcut dokümanı döner. */
    openForDocPage(projectId, pageId) {
        return axios.post(`${BASE(projectId)}/documents/for-doc-page/${pageId}`)
    },

    /** Sayfanın dokümanı var mı — oluşturmaz. Yoksa 204 döner. */
    findForDocPage(projectId, pageId) {
        return axios.get(`${BASE(projectId)}/documents/for-doc-page/${pageId}`, {
            _skipErrorToast: true
        })
    },

    /**
     * Docs içeriğini Y.Doc'a aktarma hakkını ister.
     * `granted=false` gelirse istemci **hiçbir şey yapmamalı** — başka bir sekme
     * aktarımı üstlendi, içerik normal senkron yoluyla gelecek (plan R2).
     */
    claimSeed(projectId, documentId) {
        return axios.post(`${BASE(projectId)}/documents/${documentId}/seed-claim`)
    },

    /** Y2 — ortak çalışma çıktısını yeni bir Docs sayfası olarak kaydeder. */
    publishToDocs(projectId, documentId, data) {
        return axios.post(`${BASE(projectId)}/documents/${documentId}/publish-to-docs`, data)
    },

    unlinkDocPage(projectId, documentId) {
        return axios.delete(`${BASE(projectId)}/documents/${documentId}/link-doc-page`)
    },

    // ─── Geçmiş ─────────────────────────────────────────────────────────────

    getHistory(projectId, documentId) {
        return axios.get(`${BASE(projectId)}/documents/${documentId}/history`)
    },

    getSnapshotText(projectId, documentId, snapshotId) {
        return axios.get(`${BASE(projectId)}/documents/${documentId}/history/${snapshotId}`)
    }
}
