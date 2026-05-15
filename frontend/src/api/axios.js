import axios from 'axios'
import { createToast } from 'mosha-vue-toastify'

const instance = axios.create({
    baseURL: 'http://localhost:8080',
    headers: {
        'Content-Type': 'application/json'
    }
})

// Her istekte localStorage'daki JWT'yi Authorization header'ına ekle
instance.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('jwt')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => Promise.reject(error)
)

// HTTP durum kodlarına göre varsayılan hata mesajları
const STATUS_MESSAGES = {
    400: 'Geçersiz istek.',
    403: 'Bu işlem için yetkiniz yok.',
    404: 'Kaynak bulunamadı.',
    409: 'Çakışma: Bu kayıt zaten mevcut.',
    422: 'Gönderilen veriler işlenemedi.',
    429: 'Çok fazla istek gönderildi. Lütfen biraz bekleyin.',
    500: 'Sunucu hatası. Lütfen tekrar deneyin.',
    502: 'Sunucu geçici olarak kullanılamıyor.',
    503: 'Sunucu bakımda. Lütfen daha sonra tekrar deneyin.',
}

/**
 * Hata yanıtlarından kullanıcıya gösterilecek mesajı çıkarır.
 */
function extractErrorMessage(error) {
    // Sunucudan yanıt yoksa (ağ hatası / timeout)
    if (!error.response) {
        return 'Sunucuya bağlanılamıyor. Lütfen internet bağlantınızı kontrol edin.'
    }

    const data = error.response.data
    const status = error.response.status

    // Backend'den gelen hata mesajını al (error veya message alanı)
    if (data) {
        if (typeof data === 'string') return data
        if (data.error) return data.error
        if (data.message) return data.message
    }

    // Durum koduna göre varsayılan mesaj
    return STATUS_MESSAGES[status] || `Beklenmeyen bir hata oluştu. (HTTP ${status})`
}

// Response interceptor: 401 → login yönlendirme, diğer hatalar → toast
instance.interceptors.response.use(
    (response) => response,
    (error) => {
        // 401 → token geçersiz/süresi dolmuş → login'e yönlendir (toast gösterme)
        if (error.response?.status === 401) {
            localStorage.removeItem('jwt')
            localStorage.removeItem('user')
            window.location.href = '/login'
            return Promise.reject(error)
        }

        // İstek bazında toast'ı devre dışı bırakma desteği
        // Kullanım: axios.get('/api/...', { _skipErrorToast: true })
        if (!error.config?._skipErrorToast) {
            const message = extractErrorMessage(error)
            createToast(message, {
                type: 'danger',
                position: 'top-center',
                showCloseButton: true,
                timeout: 5000,
            })
        }

        return Promise.reject(error)
    }
)

export default instance

