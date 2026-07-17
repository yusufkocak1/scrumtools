/**
 * ScrumPokerApi.js
 *
 * Spring Boot ScrumPoker REST API ile iletişim kurar.
 * Firebase ScrumPokerService.js'in REST kısmının yerini alır.
 *
 * WebSocket stratejisi: Data-Carrying
 *   Yazma işlemleri (join/leave/vote/visibility) buradaki REST fonksiyonları ile yapılır.
 *   Okuma/dinleme → websocket.js üzerinden subscribe edilir:
 *     /topic/poker/{teamId}/votes       → tam oy listesi
 *     /topic/poker/{teamId}/visibility  → { votesVisible: true/false }
 */

import apiClient from './axios.js'

const base = (teamId) => `/api/teams/${teamId}/poker`

/**
 * Oturum bilgisini ve mevcut oyları döndürür (ilk yükleme için).
 */
export const getSession = async (teamId) => {
    const { data } = await apiClient.get(base(teamId))
    return data
}

/**
 * Mevcut oy listesini döndürür.
 */
export const getVotes = async (teamId) => {
    const { data } = await apiClient.get(`${base(teamId)}/votes`)
    return data
}

/**
 * Kullanıcıyı oturuma katar — oy kaydı oluşturulur (vote="-").
 * displayName backend tarafından TeamMember tablosundan otomatik alınır.
 */
export const joinPoker = async (teamId) => {
    const { data } = await apiClient.post(`${base(teamId)}/join`)
    return data
}

/**
 * Kullanıcıyı oturumdan çıkarır — oy kaydı silinir.
 */
export const leavePoker = async (teamId) => {
    await apiClient.post(`${base(teamId)}/leave`)
}

/**
 * Kullanıcının oyunu günceller.
 * @param {string} teamId
 * @param {string} vote  - Örn: "1", "2", "5", "?", "-"
 */
export const vote = async (teamId, voteValue) => {
    const { data } = await apiClient.post(`${base(teamId)}/vote`, { vote: voteValue })
    return data
}

/**
 * Oyların görünürlüğünü günceller.
 * @param {boolean} votesVisible
 */
export const setVotesVisible = async (teamId, votesVisible) => {
    await apiClient.patch(`${base(teamId)}/visibility`, { votesVisible })
}

/**
 * Kart tipini günceller.
 * @param {string} cardType - Örn: "fibonacci", "tshirt"
 */
export const updateCardType = async (teamId, cardType) => {
    await apiClient.patch(`${base(teamId)}/card-type`, { cardType })
}

/**
 * Yeni tur başlatır: tüm oylar "-" yapılır, votesVisible=false yapılır.
 * Backend WebSocket üzerinden broadcast eder.
 */
export const newRound = async (teamId) => {
    await apiClient.post(`${base(teamId)}/new-round`)
}

// ─── Work Modülü Entegrasyonu ────────────────────────────────────────────────

/**
 * Work modülündeki bir görevi poker oturumuna bağlar (puanlama için).
 * Backend yeni tur başlatır ve görevi /topic/poker/{teamId}/task üzerinden broadcast eder.
 * @returns {Promise<{taskId, customId, title, issueType, storyPoints}>}
 */
export const setPokerTask = async (teamId, taskId) => {
    const { data } = await apiClient.patch(`${base(teamId)}/task`, { taskId })
    return data
}

/**
 * Görev bağını kaldırır — oturum bağımsız moda döner.
 */
export const clearPokerTask = async (teamId) => {
    await apiClient.patch(`${base(teamId)}/task`, { taskId: '' })
}

/**
 * Tartışma sonrası seçilen puanı bağlı göreve işler.
 * Backend görev bağını kaldırır ve yeni tur başlatır.
 * @returns {Promise<{taskId, customId, title, issueType, storyPoints}>} — customId ile göreve dönülür
 */
export const applyScore = async (teamId, points) => {
    const { data } = await apiClient.post(`${base(teamId)}/apply-score`, { points })
    return data
}

