/**
 * HangmanApi.js
 *
 * Adam Asmaca — global kelime havuzu + takım oturumu.
 * Kelime ekleme/silme SUPER_ADMIN'e özeldir — bkz. AdminHangmanApi.js.
 *
 * WebSocket stratejisi: Data-Carrying
 *   /topic/hangman/{teamId}/state → oturumun tam durumu
 *
 * NOT: Takım oyununda kelime sunucuda tutulur; istemciye sadece maskelenmiş
 * hali (round.maskedWord) gelir. Cevap yalnızca tur bitince round.revealedWord
 * alanında döner.
 */

import apiClient from './axios.js'

const base = (teamId) => `/api/teams/${teamId}/hangman`

// ─── Kelime havuzu (tek kişilik oyun) ───────────────────────────────────────

export const getHangmanWords = async (language) => {
    const { data } = await apiClient.get('/api/hangman/words', { params: { language } })
    return data
}

// ─── Takım oturumu ──────────────────────────────────────────────────────────

/**
 * Oturum açar ve açan kişiyi moderatör yapar.
 * @param {object} payload { language, roundCount, customWords, moderatorPlays }
 */
export const startHangmanSession = async (teamId, payload) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions`, payload)
    return data
}

export const getActiveHangmanSession = async (teamId) => {
    const { data } = await apiClient.get(`${base(teamId)}/sessions/active`)
    return data
}

export const getHangmanHistory = async (teamId) => {
    const { data } = await apiClient.get(`${base(teamId)}/sessions/history`)
    return data
}

export const getHangmanSession = async (teamId, sessionId) => {
    const { data } = await apiClient.get(`${base(teamId)}/sessions/${sessionId}`)
    return data
}

export const joinHangmanSession = async (teamId, sessionId) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions/${sessionId}/join`)
    return data
}

export const beginHangmanGame = async (teamId, sessionId) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions/${sessionId}/begin`)
    return data
}

export const guessHangmanLetter = async (teamId, sessionId, letter) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions/${sessionId}/guess-letter`, { value: letter })
    return data
}

export const guessHangmanWord = async (teamId, sessionId, word) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions/${sessionId}/guess-word`, { value: word })
    return data
}

export const skipHangmanTurn = async (teamId, sessionId) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions/${sessionId}/skip-turn`)
    return data
}

export const finishHangmanSession = async (teamId, sessionId) => {
    const { data } = await apiClient.post(`${base(teamId)}/sessions/${sessionId}/finish`)
    return data
}
