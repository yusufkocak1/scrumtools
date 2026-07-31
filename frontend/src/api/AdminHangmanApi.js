import api from './axios.js'

/** Superadmin Adam Asmaca kelime havuzu yönetimi API'si (backend: /api/admin/hangman). */
export const AdminHangmanApi = {
  getWords: (language, category = null) => api.get('/api/admin/hangman/words', {
    params: category ? { language, category } : { language }
  }),
  addWords: (language, category, words) => api.post('/api/admin/hangman/words', { language, category, words }),
  deleteWord: (wordId) => api.delete(`/api/admin/hangman/words/${wordId}`),
}

export default AdminHangmanApi
