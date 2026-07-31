import api from './axios.js'

/** Superadmin Adam Asmaca kelime havuzu yönetimi API'si (backend: /api/admin/hangman). */
export const AdminHangmanApi = {
  /**
   * Dahili havuz + eklenen kelimeler, sayfalı.
   * @param {object} opts { language, category, search, page, size }
   */
  getWords: ({ language, category = null, search = '', page = 0, size = 50 }) => {
    const params = { language, page, size }
    if (category) params.category = category
    if (search) params.search = search
    return api.get('/api/admin/hangman/words', { params })
  },
  /** Tek kategoriye iki dilin kelimeleri birlikte eklenir; biri boş bırakılabilir. */
  addWords: (category, trWords, enWords) => api.post('/api/admin/hangman/words', { category, trWords, enWords }),
  deleteWord: (wordId) => api.delete(`/api/admin/hangman/words/${wordId}`),
}

export default AdminHangmanApi
