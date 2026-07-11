# ScrumTools

Çevik (Scrum) takımlar için hepsi bir arada, self-hosted takım yönetimi platformu. Sprint ve görev yönetiminden planlama pokerine, retrospektif panolarından takım dokümantasyonuna kadar bir Scrum takımının günlük ihtiyaç duyduğu araçları tek uygulamada toplar.

Canlı ortam: [scrumtools.kocak.net.tr](https://scrumtools.kocak.net.tr)

## Özellikler

### İş Yönetimi
- **Proje ve Sprint yönetimi** — backlog, sprint planlama, sprint detayları
- **Görev (task) yönetimi** — alt görev hiyerarşisi, yorumlar, dosya ekleri, özel alanlar (custom fields)
- **Board** — sürükle-bırak destekli görev panosu, özelleştirilebilir iş akışları (workflow)
- **Raporlar ve Dashboard** — grafiklerle sprint/proje durumu takibi (Chart.js)

### Gerçek Zamanlı Takım Araçları (WebSocket/STOMP)
- **Scrum Poker** — planlama pokeri ile eforlama oturumları
- **Retro Board** — retrospektif panoları; madde sıralama, oylama ve yorumlar
- **Quiz** — takım içi bilgi yarışmaları
- **Code Share** — Monaco Editor tabanlı canlı kod paylaşımı

### Organizasyon ve İşbirliği
- **Organizasyon / Takım yapısı** — çoklu takım, davet sistemi, rol tabanlı yetkilendirme
- **Dokümantasyon (Docs)** — TipTap tabanlı zengin metin editörüyle takım wiki'si (tablo, kod bloğu, görsel desteği)
- **Bildirimler** — uygulama içi bildirim paneli ve HTML e-posta bildirimleri (Thymeleaf şablonları)
- **Aktivite akışı** — proje/görev üzerindeki değişikliklerin takibi

### SaaS / Faturalandırma
- **FREE / PRO / MAX** planları
- iyzico (iyzilink) ile online ödeme; kapalıyken süperadmin panelden manuel aktivasyon
- Süperadmin yönetim paneli

## Teknoloji Yığını

| Katman | Teknolojiler |
|---|---|
| Backend | Java 25, Spring Boot 3.5 (Web, Security, Data JPA, WebSocket/STOMP, Validation, Mail, Thymeleaf), JWT (jjwt) |
| Frontend | Vue 3, Vite, Tailwind CSS, Vue Router, TipTap, Monaco Editor, Chart.js, SockJS + STOMP |
| Veritabanı | PostgreSQL 16 |
| Dosya depolama | MinIO (S3 uyumlu, presigned URL ile) |
| Altyapı | Docker Compose, nginx, nginx-proxy-manager, Jenkins CI |

## Proje Yapısı

```
ScrumTools/
├── backend/          # Spring Boot REST API + WebSocket
│   └── src/main/java/com/scrumtools/
│       ├── controller/   # REST endpoint'leri (Auth, Task, Sprint, Poker, Retro, Billing, ...)
│       ├── service/      # İş mantığı (mail, payment dahil)
│       ├── entity/       # JPA entity'leri
│       ├── repository/   # Spring Data repository'leri
│       ├── security/     # JWT filtreleri ve güvenlik yapılandırması
│       └── config/       # Uygulama yapılandırması
├── frontend/         # Vue 3 SPA
│   └── src/
│       ├── pages/        # Sayfalar (Dashboard, WorkList, ScrumPoker, RetroBoard, ...)
│       └── components/   # Bileşenler (work, poker, retro, docs, billing, admin, ...)
├── db/               # Veritabanı init script'leri
├── docker-compose.yml
├── DEPLOYMENT.md     # Sunucu kurulum ve deploy dokümanı
└── Jenkinsfile       # CI pipeline
```

## Hızlı Başlangıç (Docker Compose)

1. Ortam değişkenlerini hazırlayın:
   ```bash
   cp backend/.env.example .env
   # DB_PASSWORD, MINIO_ROOT_PASSWORD ve JWT_SECRET (min 32 karakter) değerlerini doldurun
   ```
2. Servisleri ayağa kaldırın:
   ```bash
   docker compose up -d --build
   ```

Compose; PostgreSQL, MinIO, backend ve frontend (nginx) container'larını başlatır. Dışarıya yalnızca frontend açılır; backend, veritabanı ve MinIO kapalı `internal` network'te kalır. Üretim ortamı mimarisi ve nginx-proxy-manager ayarları için [DEPLOYMENT.md](DEPLOYMENT.md) dosyasına bakın.

## Geliştirme Ortamı

### Backend
```bash
cd backend
./mvnw.cmd spring-boot:run     # Windows
```
Gereksinimler: JDK 25, çalışan bir PostgreSQL ve MinIO örneği. Bağlantı bilgileri ortam değişkenleriyle verilir (bkz. `backend/.env.example`). API varsayılan olarak `8080` portunda çalışır.

### Frontend
```bash
cd frontend
npm install
npm run dev                    # http://localhost:3000
```

### Önemli Ortam Değişkenleri

| Değişken | Açıklama |
|---|---|
| `DB_*` | PostgreSQL bağlantı bilgileri |
| `JWT_SECRET` | JWT imza anahtarı (en az 32 karakter) |
| `MINIO_*` | Dosya ekleri için MinIO erişimi; `MINIO_PUBLIC_ENDPOINT` presigned URL'lerin public adresi |
| `MAIL_ENABLED` | `false` iken e-postalar gönderilmez, log'a yazılır (dev/test için güvenli varsayılan) |
| `IYZICO_ENABLED` | `false` iken online ödeme kapalıdır; manuel aktivasyon çalışır |
| `VITE_*` | Frontend **build-time** değerleri; değişirse frontend imajı yeniden build edilmelidir |
| `TZ` | Abonelik dönem hesapları ve zamanlanmış görevler için sabit kalmalıdır (`Europe/Istanbul`) |

Tam liste için [backend/.env.example](backend/.env.example) dosyasına bakın.

## Mimari Notlar

- Kimlik doğrulama JWT tabanlıdır; şifre kurulum/sıfırlama akışları e-posta linkiyle çalışır.
- Gerçek zamanlı özellikler (poker, retro, quiz, code share, bildirimler) SockJS + STOMP üzerinden yürür — reverse proxy'de WebSocket desteği zorunludur.
- Dosya ekleri MinIO'ya presigned URL ile yüklenir/indirilir; backend URL'leri public domain'e çevirir, frontend nginx bucket path'ini MinIO'ya proxy'ler.
