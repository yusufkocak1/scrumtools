import axios from 'axios'

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

// 401 gelirse token geçersiz/süresi dolmuş → login'e yönlendir
instance.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('jwt')
            localStorage.removeItem('user')
            window.location.href = '/login'
        }
        return Promise.reject(error)
    }
)

export default instance

