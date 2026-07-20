/**
 * HangmanApi.js
 *
 * Adam Asmaca takım kelime havuzu REST API ile iletişim kurar.
 */

import apiClient from './axios.js'

const base = (teamId) => `/api/teams/${teamId}/hangman`

export const getHangmanWords = async (teamId, language) => {
    const { data } = await apiClient.get(`${base(teamId)}/words`, { params: { language } })
    return data
}

export const addHangmanWords = async (teamId, language, words) => {
    const { data } = await apiClient.post(`${base(teamId)}/words`, { language, words })
    return data
}

export const deleteHangmanWord = async (teamId, wordId) => {
    await apiClient.delete(`${base(teamId)}/words/${wordId}`)
}
