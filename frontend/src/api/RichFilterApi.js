/**
 * RichFilterApi.js
 *
 * Zengin filtreler — temel sorgu + adlandırılmış parçalar (akıllı filtreler,
 * dinamik/sabit filtreler, görünümler…). Plan: RICH_FILTER_PLAN.md
 *
 * Görünürlük kayıtlı filtrelerle aynı kuralı izler:
 * PRIVATE (yalnız sahibi) | TEAM | PROJECT. Düzenleme her durumda yalnız sahibine açıktır.
 */

import apiClient from './axios.js'

const base = (teamId) => `/api/teams/${teamId}/rich-filters`

/** Kullanıcının görebildiği zengin filtreler. */
export const getRichFilters = async (teamId, projectId = null) => {
    const { data } = await apiClient.get(base(teamId), {
        params: projectId ? { projectId } : {}
    })
    return data
}

export const getRichFilter = async (teamId, richFilterId) => {
    const { data } = await apiClient.get(`${base(teamId)}/${richFilterId}`)
    return data
}

/**
 * @param {Object} filter
 * @param {string} filter.name — takım içinde benzersiz; STQL'de smart["ad"] ile kullanılır
 * @param {string} [filter.description]
 * @param {string|null} [filter.baseFilterId] — temel sorgu olarak kayıtlı filtre
 * @param {string} [filter.baseQuery] — ya da doğrudan STQL
 * @param {'PRIVATE'|'TEAM'|'PROJECT'} [filter.visibility]
 * @param {string|null} [filter.projectId]
 */
export const createRichFilter = async (teamId, filter) => {
    const { data } = await apiClient.post(base(teamId), filter)
    return data
}

export const updateRichFilter = async (teamId, richFilterId, filter) => {
    const { data } = await apiClient.put(`${base(teamId)}/${richFilterId}`, filter)
    return data
}

export const deleteRichFilter = async (teamId, richFilterId) => {
    await apiClient.delete(`${base(teamId)}/${richFilterId}`)
}

/** Öğeleriyle birlikte kopyalar; kopya kopyalayanın PRIVATE kaydı olur. */
export const duplicateRichFilter = async (teamId, richFilterId) => {
    const { data } = await apiClient.post(`${base(teamId)}/${richFilterId}/duplicate`)
    return data
}

// ─── Çalıştırma ──────────────────────────────────────────────────────────────
//
// Hepsi aynı **seçim nesnesini** alır — istemci ham sorgu göndermez:
//   { projectId, smart: [elementId], text }
// Sunucu id'leri kendi kayıtlarından çözer.

/** Sayfalı görev listesi + `smartTags`: { taskId: {id,name,color} }. */
export const searchRichFilter = async (teamId, richFilterId, selection = {}, { page = 0, size = 25 } = {}) => {
    const { data } = await apiClient.post(`${base(teamId)}/${richFilterId}/search`, {
        ...selection, page, size
    })
    return data
}

/** Eşleşen kayıt sayısı — sayaç widget'ı için tek çağrı. */
export const countRichFilter = async (teamId, richFilterId, selection = {}) => {
    const { data } = await apiClient.post(`${base(teamId)}/${richFilterId}/count`, selection)
    return data.count
}

/**
 * Gruplama. `groupBy` verilmezse zengin filtrenin kendi akıllı filtreleri eksen olur.
 * @returns {Promise<Array<{key: string, label: string, value: number, color: string|null, filter: string|null}>>}
 */
export const aggregateRichFilter = async (teamId, richFilterId, selection = {}, { groupBy = null, metric = 'count', limit = null } = {}) => {
    const { data } = await apiClient.post(`${base(teamId)}/${richFilterId}/aggregate`, {
        ...selection, groupBy, metric, limit
    })
    return data
}

/**
 * Dinamik filtrelerin güncel seçenekleri — hepsi tek istekte.
 * Her kontrolün seçenekleri kendi seçimi dışlanarak hesaplanır: bir değer
 * seçtikten sonra aynı listeden ikinci bir değer eklenebilsin diye.
 * @returns {Promise<Array<{elementId: string, name: string, field: string, options: Array}>>}
 */
export const optionsRichFilter = async (teamId, richFilterId, selection = {}) => {
    const { data } = await apiClient.post(`${base(teamId)}/${richFilterId}/options`, selection)
    return data
}

/** Seçimlerin STQL karşılığı + sayısı — görev listesine geçiş linki için. */
export const resolveRichFilter = async (teamId, richFilterId, selection = {}) => {
    const { data } = await apiClient.post(`${base(teamId)}/${richFilterId}/resolve`, selection)
    return data
}

// ─── Öğeler ──────────────────────────────────────────────────────────────────

/**
 * @param {Object} element
 * @param {'SMART_FILTER'|'STATIC_FILTER'|'DYNAMIC_FILTER'|'VIEW'|'QUEUE'|'CUSTOM_VALUE'|'RATIO'|'TIME_SERIES'} element.kind
 * @param {string} element.name
 * @param {string} [element.query] — akıllı filtrede zorunlu (STQL)
 * @param {string} [element.color] — #RRGGBB
 * @param {Object} [element.config]
 */
export const addRichFilterElement = async (teamId, richFilterId, element) => {
    const { data } = await apiClient.post(`${base(teamId)}/${richFilterId}/elements`, element)
    return data
}

export const updateRichFilterElement = async (teamId, richFilterId, elementId, element) => {
    const { data } = await apiClient.put(`${base(teamId)}/${richFilterId}/elements/${elementId}`, element)
    return data
}

export const deleteRichFilterElement = async (teamId, richFilterId, elementId) => {
    await apiClient.delete(`${base(teamId)}/${richFilterId}/elements/${elementId}`)
}

/**
 * Öğe sırasını değiştirir.
 * Akıllı filtrelerde sıra sonucu belirler: bir görev, kendisine uyan **ilk**
 * akıllı filtrenin rengini ve etiketini alır.
 */
export const reorderRichFilterElements = async (teamId, richFilterId, elementIds) => {
    const { data } = await apiClient.put(`${base(teamId)}/${richFilterId}/elements/reorder`, { elementIds })
    return data
}

/**
 * STQL'de bir zengin filtrenin sınıflandırmasına atıfta bulunan alan adı.
 * Ad tırnaklanır: boşluklu ve Türkçe karakterli adlar tırnaksız çözümlenemez.
 */
export const smartField = (richFilterName) =>
    `smart["${String(richFilterName ?? '').replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"]`
