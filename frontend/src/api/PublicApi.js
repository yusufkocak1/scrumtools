import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/'

/**
 * Oturum gerektirmeyen uçlar için interceptor'sız axios örneği.
 *
 * Ana axios örneği ({@link ./axios.js}) 401 aldığında login'e yönlendirir ve
 * her hatada toast basar; tanıtım sayfası oturumsuz açıldığı için ikisi de
 * istenmez — istek sessizce başarısız olup sayfa statik yedeğe düşmelidir.
 */
const publicApi = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
})

export const PublicApi = {
    // Aktif + public paketler (SecurityConfig'de permitAll)
    getPlans: () => publicApi.get('/api/plans'),
}

export default PublicApi
