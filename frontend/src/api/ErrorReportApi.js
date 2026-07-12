import api from './axios.js'

export default {
    /**
     * Frontend'de yakalanmayan hatayı backend'e raporlar.
     * _skipErrorToast: rapor isteği başarısız olursa toast gösterilmez.
     * _isErrorReport: errorReporter'ın kendi isteğinden doğan hataları
     * tekrar raporlamasını engelleyen işaret (sonsuz döngü koruması).
     */
    report(payload) {
        return api.post('/api/errors/report', payload, {
            _skipErrorToast: true,
            _isErrorReport: true,
        })
    },
}
