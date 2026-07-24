# Referans Mimari — kocak.net.tr Uygulama Ailesi

Bu döküman, **ScrumTools**'un mimarisini yeni uygulamalara (öncelikle **QR Menü**,
ardından **CRM** ve sonraki ürünler) kopyalanabilir bir **referans mimari** olarak
tanımlar. Amaç: her yeni ürünün aynı teknoloji yığını, aynı klasör yapısı, aynı
deploy modeli ve aynı konvansiyonlarla açılması; CRM'in de tüm ürünlerin
müşterilerini tek ekrandan yönetebilmesi.

> Kaynak referans: ScrumTools (canlı: https://scrumtools.kocak.net.tr).
> Bu dökümanda "ürün" = bu mimariyle kurulmuş her bağımsız uygulama
> (ScrumTools, QR Menü, CRM, ...).

---

## 1. Genel Mimari

Her ürün; tek sunucuda, kendi `docker compose` projesi içinde, ortak
**nginx-proxy-manager (NPM)** arkasında, kendi subdomain'inde çalışan
**self-contained bir stack**'tir:

```
Internet ──HTTPS──> nginx-proxy-manager  (ortak "proxy" docker network'ü, SSL burada biter)
                        │
     ┌──────────────────┼──────────────────────┐
     │                  │                      │
scrumtools.kocak…   qrmenu.kocak…          crm.kocak…
     │                  │                      │
     ▼                  ▼                      ▼
┌─ compose: scrumtools ─┐   (her ürün için aynı şablon)
│  frontend (nginx:80)  │ ← yalnızca bu container "proxy" network'üne bağlı
│    ├ /            → Vue SPA (static)
│    ├ /api/        → backend:8080
│    ├ /ws          → backend:8080 (SockJS/STOMP)
│    └ /<bucket>/   → minio:9000 (presigned URL passthrough)
│  backend  (Spring Boot, :8080)  ┐
│  postgres (16-alpine)           ├─ dışa kapalı "internal" network
│  minio    (S3 uyumlu, :9000)    ┘
└──────────────────────────────────┘
```

Temel ilkeler:

1. **Ürün başına tam izolasyon** — her ürünün kendi postgres'i, minio'su,
   backend'i, frontend'i ve compose projesi vardır. Ürünler birbirinin
   veritabanına asla doğrudan erişmez.
2. **Tek giriş kapısı** — dışarıya yalnızca frontend nginx açılır; backend, DB
   ve MinIO `internal` network'te kalır. SSL sonlandırma NPM'de yapılır.
3. **Aynı origin** — SPA, API (`/api`), WebSocket (`/ws`) ve dosyalar
   (`/<bucket>/`) aynı domain'den servis edilir; CORS derdi minimuma iner.
4. **Ürünler arası iletişim yalnızca HTTP API ile** (bkz. §10 CRM).

## 2. Teknoloji Yığını

| Katman | Teknoloji | Not |
|---|---|---|
| Backend | Java 25, Spring Boot 3.5 (Web, Security, Data JPA, WebSocket/STOMP, Validation, Mail, Thymeleaf) | Maven, Lombok |
| Kimlik | JWT (jjwt 0.12), BCrypt | Stateless; token `Authorization: Bearer` |
| Frontend | Vue 3, Vite 5, Vue Router 4, Tailwind CSS 3, Axios | SPA; state için Pinia yerine hafif composable'lar |
| Gerçek zamanlı | SockJS + STOMP (`@stomp/stompjs`) | Reverse proxy'de WebSocket desteği zorunlu |
| Veritabanı | PostgreSQL 16 (alpine) | Ürün başına ayrı container + ayrı schema |
| Dosya | MinIO (S3 uyumlu) | Presigned URL pattern'i (bkz. §6) |
| Ödeme | iyzico / iyzilink | Feature flag ile kapatılabilir; manuel aktivasyon fallback |
| E-posta | PostForge API (fallback: SMTP + Thymeleaf) | `MAIL_PROVIDER=log` iken log'a yazar |
| Altyapı | Docker Compose, nginx, nginx-proxy-manager, Jenkins CI | Tek sunucu, ortak `proxy` network |

Yeni üründe farklı bir kütüphane seçmeden önce buradaki karşılığı kullanılır;
yığın bilinçli olarak tüm ürünlerde **aynı** tutulur.

## 3. Repo ve Proje Yapısı

Ürün başına tek repo (monorepo değil, "mono-ürün" repo): backend + frontend +
altyapı dosyaları birlikte versiyonlanır ve birlikte deploy edilir.

```
<urun>/
├── backend/
│   ├── Dockerfile                # multi-stage: temurin-jdk build → temurin-jre run
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/<urun>/
│       │   ├── controller/       # REST endpoint'leri (+ GlobalExceptionHandler)
│       │   ├── service/          # iş mantığı (mail/, payment/ gibi alt paketler)
│       │   ├── repository/       # Spring Data JPA
│       │   ├── entity/           # JPA entity'leri (+ enums/)
│       │   ├── dto/              # request/response modelleri
│       │   ├── security/         # JwtUtil, JwtAuthFilter, *SecurityEvaluator
│       │   ├── config/           # SecurityConfig, WebSocketConfig, MinioConfig, DataInitializer
│       │   └── exception/        # domain exception'ları
│       └── resources/
│           ├── application.yml   # tüm ayarlar ${ENV:default} formatında
│           └── templates/mail/   # Thymeleaf HTML e-posta şablonları
├── frontend/
│   ├── Dockerfile                # multi-stage: node:20-alpine build → nginx:1.27-alpine
│   ├── nginx.conf                # SPA + /api + /ws + /<bucket>/ proxy (bkz. §7)
│   └── src/
│       ├── api/                  # axios.js (ortak instance) + alan başına XxxApi.js + websocket.js
│       ├── pages/                # route başına sayfa
│       ├── components/<alan>/    # alan bazlı bileşen klasörleri (work/, billing/, admin/ ...)
│       ├── composables/          # useAuth, useEntitlements, usePermissions ...
│       ├── utils/
│       └── router.js
├── db/init/01-create-schema.sh   # boş volume'de schema oluşturur (docker-entrypoint-initdb.d)
├── docker-compose.yml
├── Jenkinsfile
├── DEPLOYMENT.md
└── README.md
```

## 4. Backend Mimarisi

### Katmanlar

Klasik üç katman: `Controller → Service → Repository`. Controller'lar ince
tutulur (validasyon + DTO dönüşümü), iş mantığı service'lerde yaşar. Entity'ler
API'ye sızdırılmaz; dışarıya DTO döner.

### Kimlik doğrulama ve yetkilendirme

- **JWT tabanlı, stateless.** `JwtAuthFilter` her istekte `Bearer` token'ı
  doğrular; `SecurityConfig` public endpoint'leri (auth, webhook, public sayfa
  API'leri) whitelist'ler. Şifreler BCrypt ile saklanır.
- Şifre kurulum/sıfırlama **e-posta linkiyle** çalışır (`PasswordSetupToken`
  entity + token'lı link → frontend `SetPassword` sayfası).
- Yetkilendirme iki seviyelidir: global roller (user/superadmin) + kaynak bazlı
  kontrol (`ProjectSecurityEvaluator` gibi evaluator sınıfları; "bu kullanıcı bu
  organizasyonun/projenin üyesi mi?").
- **QR Menü notu:** menünün müşteri tarafı (QR'ı okutan son kullanıcı) auth'suz
  public endpoint'lerden beslenir; restoran sahibi paneli JWT ile korunur. Aynı
  whitelist mekanizması kullanılır.

### Hata sözleşmesi

`GlobalExceptionHandler` tüm hataları tek formata çevirir; frontend interceptor
bu sözleşmeye göre yazılmıştır (bkz. §5):

```json
{ "error": "insan okur mesaj", "code": "PLAN_LIMIT", "limitType": "...", "trackingCode": "ERR-..." }
```

- `401` → oturum geçersiz (frontend login'e atar)
- `402 + code=PLAN_LIMIT` → paket limiti (frontend upgrade modal'ı açar)
- `trackingCode` → sunucu log'uyla eşleşen hata takip numarası; toast'ta gösterilir

### Gerçek zamanlı katman (opsiyonel modül)

WebSocket/STOMP `WebSocketConfig` ile `/ws` endpoint'inde (SockJS fallback'li)
açılır. Sunucu → istemci yayınları topic bazlıdır. Bu modül üründe gerçek
zamanlı ihtiyaç varsa eklenir; **QR Menü'de sipariş/garson çağırma bildirimleri
için birebir aynı pattern kullanılır.**

### E-posta

`MailService` arayüzü + üç implementasyon, `MAIL_PROVIDER` ile seçilir:

- `postforge` → `PostForgeMailService`: konu/gövde PostForge'da tutulur, uygulama
  yalnızca `templateCode` + `params` gönderir (`POST /api/v1/emails`, `X-Api-Key`).
  Şablon kaynakları ve import script'i repo kökündeki `postforge/` klasöründedir.
- `smtp` → `SmtpMailService`: doğrudan SMTP, şablonlar Thymeleaf HTML.
- `log` (dev/test varsayılanı) → `LoggingMailService`: mail gönderilmez, log'a yazılır.

Linkler her durumda `APP_FRONTEND_BASE_URL` ile üretilir. Gönderim `@Async`'tir ve
hatalar yalnızca loglanır — mail gidemedi diye iş akışı geri alınmaz.

**Yeni üründe:** merkezî e-posta altyapısı PostForge'dur; her ürün kendi
uygulama API anahtarını alır ve şablonlarını kendi repo'sunda `postforge/`
klasöründe versiyonlar.

### Konfigürasyon ve feature flag'ler

Tüm ayarlar `application.yml`'de `${ENV_VAR:guvenli_varsayilan}` formatındadır;
prod değerleri compose `.env`'den gelir. Kritik konvansiyonlar:

- Entegrasyonlar **flag ile kapatılabilir** ve kapalıyken uygulama çalışmaya
  devam eder: `MAIL_PROVIDER=log`, `IYZICO_ENABLED` (kapalıyken manuel aktivasyon).
- Sırlar (`JWT_SECRET` min 32 karakter, DB/MinIO şifreleri) repo'ya girmez;
  compose'da `:?` ile zorunlu kılınır.
- `TZ=Europe/Istanbul` sabittir — abonelik dönem hesapları ve zamanlanmış
  görevler bu zone'a göre çalışır.

### Veritabanı şema yönetimi

Mevcut konvansiyon: `db/init/01-create-schema.sh` boş volume'de schema'yı
oluşturur, Hibernate `ddl-auto: update` tabloları günceller (Flyway kapalı).
Tek instance'lı ürünler için yeterlidir; elle migration gerektiren durumlar
`config/` altındaki `*Runner` sınıflarıyla (ApplicationRunner) çözülür.

## 5. Frontend Mimarisi

### API katmanı — tek axios instance

`src/api/axios.js` ortak instance'tır; **tüm** API dosyaları bunu kullanır.
İçindeki sözleşme her üründe aynen kopyalanır:

- Request interceptor: `localStorage`'daki JWT'yi `Authorization` header'ına ekler.
- Response interceptor:
  - `401` → token temizle, login'e yönlendir (toast yok)
  - `402 + code=PLAN_LIMIT` → `window` event'i (`<urun>:plan-limit`) → App.vue
    dinler, UpgradeModal açar
  - Diğer hatalar → backend mesajı (+ `trackingCode`) ile toast; istek bazında
    `_skipErrorToast: true` ile susturulabilir

Alan başına bir API modülü yazılır (`WorkApi.js`, `BillingApi.js`, ...); sayfa ve
bileşenler axios'u doğrudan değil bu modüller üzerinden çağırır.

### State ve composable'lar

Global store (Pinia/Vuex) yoktur; oturum, yetki ve plan bilgisi hafif
composable'larla taşınır: `useAuth`, `usePermissions`,
`useEntitlements` (plan limitlerine göre UI'da özellik açma/kapama).
Yeni üründe de aynı yaklaşım kullanılır; store ancak gerçekten gerekirse eklenir.

### WebSocket istemcisi

`src/api/websocket.js` SockJS + STOMP bağlantısını yönetir (connect, topic
subscribe, reconnect). Gerçek zamanlı sayfalar bu wrapper'ı kullanır.

### Build-time env

`VITE_API_BASE_URL`, `VITE_WS_BASE_URL`, `VITE_APP_BASE_PATH` **build
zamanında** gömülür (Dockerfile `ARG`). Değişirlerse frontend imajı yeniden
build edilmelidir. Varsayılanlar relative (`/`, `/ws`) olduğundan aynı-origin
deploy'da genelde dokunmak gerekmez.

## 6. Dosya Depolama — MinIO Presigned URL Pattern'i

Dosyalar (görev ekleri; QR Menü'de ürün/menü fotoğrafları) backend üzerinden
akmaz; tarayıcı MinIO ile doğrudan konuşur:

```
1. Frontend → backend: "yükleme URL'i ver" (/api/.../attachments)
2. Backend → MinIO SDK: presigned PUT/GET URL üretir (internal: http://minio:9000/...)
3. Backend URL'deki host'u MINIO_PUBLIC_ENDPOINT (https://<urun>.kocak.net.tr) ile değiştirir
4. Tarayıcı bu URL'e PUT/GET yapar → istek frontend nginx'e düşer
5. nginx /<bucket>/ location'ı isteği minio:9000'e proxy'ler ve
   Host header'ını "minio:9000"e GERİ yazar → SigV4 imzası geçerli kalır
```

Kritik detaylar:
- nginx location adı **bucket adıyla aynı** olmalıdır (`MINIO_BUCKET` değişirse
  `nginx.conf` da değişir).
- MinIO console (9001) dışarı açılmaz; gerekirse SSH tüneli.
- Upload limiti üç yerde tutarlı olmalıdır: NPM Advanced
  (`client_max_body_size 25M`), frontend nginx, Spring multipart ayarı.

## 7. Frontend nginx Sözleşmesi

Her ürünün `frontend/nginx.conf`'u aynı şablondur:

| Location | Hedef | Not |
|---|---|---|
| `/` | SPA static | `try_files ... /index.html` (SPA fallback) |
| `= /index.html` | — | `no-cache` (deploy anında yeni sürüm görünsün) |
| `/assets/` | — | `immutable, max-age=1y` (hash'li dosyalar) |
| `/api/` | `backend:8080` | X-Forwarded-* header'ları ile |
| `/ws` | `backend:8080/ws` | `Upgrade/Connection` header + uzun timeout |
| `/<bucket>/` | `minio:9000` | Host header'ı geri yazılır (bkz. §6) |

## 8. SaaS / Abonelik Modeli

Faturalandırma modeli üründen bağımsız tasarlanmıştır ve **her ücretli üründe
aynı varlıklar ve adlarla** kurulur — CRM'in tek ekrandan yönetebilmesinin
temeli budur:

| Varlık | Alanlar (özet) | Görev |
|---|---|---|
| `Plan` | code (FREE/PRO/MAX), limitler (maxMembers, maxProjects, features), monthlyPriceTry, yearlyPriceTry, trialDays, isDefault, isPublic, active | Satılabilir paket tanımı (ürüne göre limit alanları değişir; ör. QR Menü: maxBranches, maxProducts) |
| `Subscription` | organization, plan, status, billingCycle, currentPeriodStart/End, trialEndsAt, canceledAt, **source** (IYZICO/MANUAL), activatedBy, notes | Müşterinin aktif aboneliği |
| `PaymentTransaction` | abonelik, tutar, iyzilink referansı, durum | Ödeme kayıtları |

Akışlar:

- **Online ödeme:** `POST /api/organizations/{orgId}/billing/checkout` →
  iyzilink ödeme linki üretilir → `PaymentWebhookController` iyzico callback'i
  ile aboneliği aktive eder.
- **Manuel aktivasyon:** iyzico kapalıyken (`IYZICO_ENABLED=false`) süperadmin
  panelden abonelik elle açılır (`source=MANUAL`).
- **Limit uygulama:** backend limit aşımında `402 + PLAN_LIMIT` döner; frontend
  `useEntitlements` + UpgradeModal ile karşılar. Limit kontrolü **her zaman
  backend'dedir**, frontend yalnızca UX içindir.
- **Süperadmin API'si** (CRM'in tüketeceği yüzey, bkz. §10):

  ```
  GET  /api/admin/billing/organizations                     # müşteri + abonelik listesi
  POST /api/admin/billing/organizations/{orgId}/subscription # manuel aktivasyon / plan değişikliği
  POST /api/admin/billing/organizations/{orgId}/payment-link # iyzilink üret
  GET  /api/admin/billing/organizations/{orgId}/transactions
  PUT  /api/admin/billing/organizations/{orgId}/suspend
  GET/POST/PUT /api/admin/billing/plans
  ```

## 9. Deployment ve CI

### docker-compose şablonu

ScrumTools `docker-compose.yml`'i şablondur. Ürün başına değişenler yalnızca
**isimler**: container adları (`<urun>-postgres`, `<urun>-backend`, ...),
volume adları, `COMPOSE_PROJECT_NAME`, DB/schema adı, bucket adı, domain.
Değişmeyenler: healthcheck'ler, `internal` + external `proxy` network modeli,
`:?` ile zorunlu kılınan sırlar, restart policy'ler.

### NPM proxy host (ürün başına bir kez)

| Alan | Değer |
|---|---|
| Domain | `<urun>.kocak.net.tr` (DNS A kaydı → sunucu IP) |
| Forward | `http` → `<urun>-frontend:80` |
| Websockets Support | **Açık** (STOMP kullanan ürünlerde zorunlu) |
| SSL | Let's Encrypt, Force SSL + HTTP/2 |
| Advanced | `client_max_body_size 25M;` |

### Jenkins pipeline (ürün başına bir job)

Aşamalar her üründe aynıdır:
`.env`'i Secret file credential'dan kopyala (`<urun>-env`) → `proxy` network'ünü
garanti et → `docker compose build --pull` → `up -d --remove-orphans` →
backend health check (frontend container'ı içinden `/api/...` HTTP cevabı bekle)
→ `docker image prune`. `.env` her koşuda workspace'ten silinir.

### Yeni ürün açılış checklist'i (QR Menü örneği)

1. Repo'yu ScrumTools iskeletinden türet; paket adı `com.qrmenu`, compose
   adları `qrmenu-*`, schema `qrmenu`, bucket `qrmenu-assets` yap.
2. `frontend/nginx.conf`'ta bucket location'ını yeni bucket adıyla güncelle.
3. DNS: `qrmenu.kocak.net.tr` A kaydı; NPM'de proxy host aç (üstteki tablo).
4. Jenkins: `qrmenu-env` Secret file credential + pipeline job.
5. `.env`: `MINIO_PUBLIC_ENDPOINT`, `APP_CORS_ALLOWED_ORIGINS`,
   `APP_FRONTEND_BASE_URL` → `https://qrmenu.kocak.net.tr`; yeni `JWT_SECRET`
   ve şifreler üret (ürünler arası sır paylaşımı yok).
6. Billing modülünü (§8) aynı varlık adlarıyla kur; plan limitlerini ürüne göre
   tanımla; admin billing API'sini aynı path sözleşmesiyle aç.
7. CRM entegrasyonu için servis token'ı tanımla (bkz. §10).

## 10. CRM — Çoklu Ürün Müşteri Yönetimi

CRM, bu referans mimariyle kurulmuş **ayrı bir üründür** (`crm.kocak.net.tr`,
kendi compose stack'i, kendi DB'si). Diğer ürünlerin müşterilerini **tek
ekrandan** yönetir ama onların veritabanlarına asla doğrudan bağlanmaz —
tek entegrasyon yüzeyi, her ürünün zaten sunduğu **admin billing API'sidir**.

```
                      ┌────────────── CRM (crm.kocak.net.tr) ──────────────┐
                      │  Customer  (email = birleşik anahtar)              │
                      │  CustomerProductLink (product, externalOrgId,      │
                      │        plan, status, periodEnd, lastSyncAt)        │
                      │  Not/Görüşme kayıtları, satış boru hattı, raporlar │
                      └───────┬───────────────────────┬────────────────────┘
                    HTTPS + servis token       HTTPS + servis token
                              │                       │
             ScrumTools admin API              QR Menü admin API
        /api/admin/billing/organizations   /api/admin/billing/organizations
        /...(§8'deki standart sözleşme)    /...(aynı sözleşme)
```

### Entegrasyon kuralları

1. **Standart sözleşme:** Her ürün §8'deki admin billing endpoint setini aynı
   path ve aynı response şekliyle sunar. CRM tarafında ürün başına özel kod
   değil, tek bir "product adapter" konfigürasyonu olur:
   `{ productCode, baseUrl, serviceToken }`.
2. **Kimlik doğrulama:** Ürünlere CRM için süperadmin JWT'si yerine **servis
   token'ı** eklenir — `X-Service-Token` header'ını doğrulayan basit bir filter,
   yalnızca `/api/admin/**` path'lerinde geçerli. Token ürün başına ayrıdır ve
   `.env`'de tutulur. (ScrumTools'a eklenecek küçük bir geliştirmedir;
   yeni ürünlerde baştan konur.)
3. **Erişim yolu:** Public HTTPS üzerinden (`https://<urun>.kocak.net.tr/api/admin/...`).
   Bu, ürünler ileride farklı sunuculara dağılsa bile çalışır. Aynı sunucuda
   kaldıkça istenirse ek sıkılaştırma: NPM'de `/api/admin` path'ine IP kısıtı.
4. **Senkronizasyon:** İki yönlü değil, **CRM çeker + ürün iter**:
   - *Pull:* CRM zamanlanmış görevle (ör. saatlik) her ürünün müşteri/abonelik
     listesini çekip `CustomerProductLink`'leri günceller (`lastSyncAt`).
   - *Push (opsiyonel iyileştirme):* Ürünler kritik olaylarda
     (`customer.created`, `subscription.activated`, `payment.received`) CRM'in
     webhook endpoint'ine POST atar; pull mekanizması kaçanları telafi eder.
5. **Müşteri eşleştirme:** Birleşik anahtar **e-postadır** (organizasyon sahibi
   e-postası). Aynı e-posta iki üründe müşteriyse CRM'de tek `Customer` +
   iki `CustomerProductLink` oluşur.
6. **Yazma işlemleri:** CRM'den yapılan manuel aktivasyon / askıya alma /
   ödeme linki üretme, ilgili ürünün admin API'sine yapılan çağrıdır; kayıt
   sahibi (source of truth) her zaman üründür, CRM kopyayı senkronla günceller.
7. **Ödeme:** iyzilink entegrasyonu ürün içinde kalır (webhook'lar ürüne
   gelir); CRM konsolide raporlar ve ödeme linki üretimini tetikler.

### Bilinçli olarak v1'e alınmayanlar

- **Merkezi SSO / ortak kullanıcı hesabı:** Her ürün kendi auth'unu tutar.
  Ortak kimlik servisi ancak ürünler arası oturum paylaşımı gerçek bir ihtiyaç
  olursa, ayrı bir proje olarak ele alınır.
- **Ortak veritabanı / event bus:** Tek geliştiricili, tek sunuculu bu ölçekte
  HTTP API + zamanlanmış senkron yeterlidir; Kafka/RabbitMQ eklenmez.

## 11. Konvansiyon Özeti (hızlı referans)

| Konu | Konvansiyon |
|---|---|
| Domain | `<urun>.kocak.net.tr`, ürün başına bir subdomain |
| Java paketi | `com.<urun>` |
| Container/volume adları | `<urun>-postgres`, `<urun>-backend`, `<urun>-frontend`, `<urun>-minio`, `<urun>-postgres-data` |
| DB | Ürün başına postgres container'ı; DB/schema/user adı = ürün adı |
| Bucket | `<urun>-<amaç>` (ör. `scrumtools-attachments`); nginx location ile birebir aynı |
| Auth | JWT + BCrypt; şifre akışları e-posta linkiyle |
| Hata gövdesi | `{ error, code?, trackingCode? }`; 401=oturum, 402+PLAN_LIMIT=paket limiti |
| Feature flag | Entegrasyonlar `*_ENABLED=false` iken uygulama çalışmaya devam eder |
| Frontend env | `VITE_*` build-time'dır; relative varsayılanlar kullanılır |
| Saat dilimi | `TZ=Europe/Istanbul`, tüm ürünlerde sabit |
| CI | Ürün başına Jenkins job + `<urun>-env` Secret file credential |
| Admin API | `/api/admin/billing/*` standart sözleşmesi + `X-Service-Token` (CRM erişimi) |
| Sırlar | Ürün başına ayrı üretilir; repo'da asla tutulmaz; compose'da `:?` ile zorunlu |
