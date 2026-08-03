/**
 * QueryApi.js
 *
 * STQL (ScrumTools Query Language) uçları — Jira JQL benzeri görev sorgulama.
 * Dil referansı: TASK_QUERY_LANGUAGE.md
 */

import apiClient from './axios.js'

/**
 * Sorgu uçlarında global hata toast'ı kapalıdır: yazım hâlindeki bir sorgunun
 * geçersiz olması beklenen bir durumdur, hata mesajı editörün içinde konumuyla
 * birlikte gösterilir. Toast burada yalnızca gürültü olurdu.
 */
const SILENT = { _skipErrorToast: true }

/**
 * Sorguyu çalıştırır.
 * @param {string} teamId
 * @param {Object} params
 * @param {string} params.query — STQL metni; boş bırakılırsa kapsamdaki tüm görevler
 * @param {string|null} [params.projectId] — aktif proje kapsamı
 * @param {number} [params.page]
 * @param {number} [params.size]
 * @returns {Promise<{content: Array, totalElements: number, totalPages: number, page: number, size: number}>}
 */
export const runQuery = async (teamId, { query, projectId = null, page = 0, size = 50 }) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/tasks/query`, {
        query, projectId, page, size
    }, SILENT)
    return data
}

/**
 * Sorguyu doğrular ve geçerliyse eşleşen kayıt sayısını da döner.
 * Doğrulama ile sayaç tek istekte birleştirilmiştir — editör her duraklamada
 * bunu çağırdığı için iki ayrı gidiş-dönüş gereksiz yük olurdu.
 *
 * @returns {Promise<{valid: boolean, count?: number, error?: {message: string, position: number, length: number}}>}
 */
export const validateQuery = async (teamId, query, projectId = null) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/tasks/query/validate`, {
        query, projectId
    }, SILENT)
    return data
}

/** Eşleşen kayıt sayısı. Editör validateQuery kullanır; bu uç program içi kullanım içindir. */
export const countQuery = async (teamId, query, projectId = null) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/tasks/query/count`, {
        query, projectId
    }, SILENT)
    return data.count
}

/**
 * Sorgu sonucunu bir alana göre gruplar — grafik widget'larının veri kaynağı.
 *
 * Dönen her kova, kendisine daraltan STQL parçasını `filter` alanında taşır;
 * grafik diliminden görev listesine geçerken bu parça temel sorguya AND'lenir.
 *
 * @param {string} teamId
 * @param {Object} params
 * @param {string} params.query — STQL metni
 * @param {string} params.groupBy — status, priority, assignee, labels, sprint…
 * @param {string} [params.metric] — 'count' (varsayılan) ya da sayısal alan adı
 * @param {number} [params.limit] — kova sayısı; aşanlar "Diğer" kovasında toplanır
 * @param {string|null} [params.projectId]
 * @returns {Promise<Array<{key: string, label: string, value: number, color: string|null, filter: string|null}>>}
 */
export const aggregateQuery = async (teamId, { query, groupBy, metric = 'count', limit = null, projectId = null }) => {
    const { data } = await apiClient.post(`/api/teams/${teamId}/tasks/query/aggregate`, {
        query, groupBy, metric, limit, projectId
    }, SILENT)
    return data
}

/**
 * Sorgulanabilir alanlar, operatörleri ve fonksiyon kataloğu.
 * Her alan `groupable` / `summable` bayraklarını taşır — grafik yapılandırması
 * grup ekseni ve ölçü listelerini bu bayraklarla süzer.
 * @returns {Promise<{fields: Array, functions: Array, keywords: Array, customFieldSyntax: string}>}
 */
export const getQueryFields = async (teamId) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/tasks/query/fields`, SILENT)
    return data
}

/**
 * Bir alan için değer önerileri (otomatik tamamlama).
 * @returns {Promise<Array<{value: string, label: string}>>}
 */
export const suggestValues = async (teamId, field, prefix = '', projectId = null) => {
    const { data } = await apiClient.get(`/api/teams/${teamId}/tasks/query/suggest`, {
        params: { field, prefix, ...(projectId ? { projectId } : {}) },
        ...SILENT,
    })
    return data
}
