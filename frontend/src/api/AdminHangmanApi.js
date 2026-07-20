import api from './axios.js'

/** Superadmin Adam Asmaca kelime havuzu yönetimi API'si (backend: /api/admin/hangman). */
export const AdminHangmanApi = {
  getWords: (language) => api.get('/api/admin/hangman/words', { params: { language } }),
  addWords: (language, words) => api.post('/api/admin/hangman/words', { language, words }),
  deleteWord: (wordId) => api.delete(`/api/admin/hangman/words/${wordId}`),
}

export default AdminHangmanApi
