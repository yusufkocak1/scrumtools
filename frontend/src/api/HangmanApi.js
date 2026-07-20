/**
 * HangmanApi.js
 *
 * Adam Asmaca global kelime havuzunu okur (oyun içi kullanım).
 * Kelime ekleme/silme SUPER_ADMIN'e özeldir — bkz. AdminHangmanApi.js.
 */

import apiClient from './axios.js'

export const getHangmanWords = async (language) => {
    const { data } = await apiClient.get('/api/hangman/words', { params: { language } })
    return data
}
