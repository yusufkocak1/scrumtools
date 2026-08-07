# ScrumTools Deployment

Uygulama `scrumtools.kocak.net.tr` subdomain'inde, nginx-proxy-manager (NPM) arkasında
docker compose ile çalışır. SSL sonlandırma NPM'de yapılır.

## Mimari

```
Internet ──HTTPS──> nginx-proxy-manager (proxy network)
                        │
                        └──> scrumtools-frontend:80 (nginx)
                                ├── /            → Vue SPA (static)
                                ├── /api/        → scrumtools-backend:8080
                                ├── /ws          → scrumtools-backend:8080 (SockJS/STOMP)
                                ├── /ws/collab   → scrumtools-backend:8080 (ham WS, CRDT ikili)
                                └── /scrumtools-attachments/ → scrumtools-minio:9000 (presigned URL)
                        (internal network: postgres, minio, backend, frontend)
```

- Sadece **frontend** container'ı `proxy` network'üne bağlıdır; postgres, minio ve
  backend dışarıya kapalı `internal` network'tedir.
- MinIO presigned URL'leri backend tarafından public domain'e çevrilir
  (`MINIO_PUBLIC_ENDPOINT`), frontend nginx bucket path'ini Host header'ını geri
  yazarak MinIO'ya iletir — böylece SigV4 imzası geçerli kalır.

## Sunucu ön koşulları

1. Docker + docker compose kurulu olmalı.
2. `proxy` network'ü mevcut olmalı (NPM bu network'te çalışıyor):
   ```bash
   docker network create proxy   # yoksa
   ```
3. DNS: `scrumtools.kocak.net.tr` → sunucu IP (A kaydı).

## nginx-proxy-manager ayarları

Yeni Proxy Host:

| Alan | Değer |
|---|---|
| Domain Names | `scrumtools.kocak.net.tr` |
| Scheme | `http` |
| Forward Hostname | `scrumtools-frontend` |
| Forward Port | `80` |
| **Websockets Support** | **AÇIK (zorunlu — SockJS/STOMP ve `/ws/collab` için)** |
| Block Common Exploits | Açık |
| SSL | Let's Encrypt, Force SSL + HTTP/2 |

Dosya yükleme limiti için proxy host'un **Advanced** sekmesine ekleyin:
```nginx
client_max_body_size 25M;
```

## Ortam değişkenleri

`.env` dosyası repo'da yoktur (gitignore). Örnek için `backend/.env.example`.
Kritik değerler: `DB_PASSWORD`, `MINIO_ROOT_PASSWORD`, `JWT_SECRET` (min 32 karakter),
`MINIO_PUBLIC_ENDPOINT=https://scrumtools.kocak.net.tr`,
`APP_CORS_ALLOWED_ORIGINS=https://scrumtools.kocak.net.tr`.

`VITE_*` değişkenleri **build-time**'dır; değiştirilirse frontend imajı yeniden
build edilmelidir.

## Jenkins pipeline

`Jenkinsfile` repo kökündedir. Kurulum:

1. Jenkins'te **Secret file** tipinde `scrumtools-env` ID'li credential oluşturun,
   içeriği sunucu `.env` dosyası olsun.
2. Pipeline job'ı GitHub repo'ya bağlayın (webhook veya poll SCM).
3. Jenkins agent'ının docker'a erişimi olmalı (docker.sock mount veya docker grubu).

Pipeline aşamaları: `.env` hazırla → `proxy` network'ünü garanti et →
`docker compose build --pull` → `docker compose up -d --remove-orphans` →
backend health check → eski imajları temizle.

## Manuel deploy

```bash
git pull
docker compose build --pull
docker compose up -d --remove-orphans
docker compose logs -f backend   # kontrol
```

## ⚠️ Backend ölçeklenemez (ortak çalışma alanı)

`docker compose up --scale backend=N` **çalıştırılmamalıdır.**

STOMP broker'ı süreç-içidir (`enableSimpleBroker`) ve ortak çalışma oturumları
(`/ws/collab`) bellekte tutulur — Redis rölesi yoktur (COLLAB_WORKSPACE_PLAN.md D3/K9).
İkinci bir örnek hata vermez, **sessizce yanlış çalışır**: A örneğine bağlı kullanıcı
B örneğine bağlı kullanıcının düzenlemesini görmez ve iki taraf da kendi anlık
görüntüsünü yazar.

- `docker-compose.yml`'daki `container_name: scrumtools-backend` `--scale`'i zaten
  hata verdirir; bu bir güvenlik ağıdır, kural değil.
- Çalışan mod `/actuator/health` içindeki `collab` bileşeninde ve açılış log'unda
  raporlanır (`COLLAB_SINGLE_INSTANCE`).
- İleride ölçek gerekirse doğru yol Redis değil **doküman yapışkanlığı**dır:
  NPM/nginx'te `/ws/collab` yolunu `hash $arg_doc consistent` ile yönlendirmek.

**Yeniden başlatma:** deploy sırasında tüm ortak çalışma oturumları düşer. Veri kaybı
olmaz (güncellemeler Postgres'te append log'dadır), istemciler üstel geri çekilmeyle
yeniden bağlanır; kullanıcı 2–5 sn "yeniden bağlanılıyor" görür.

## Notlar

- Backend container'ına bellek limiti (`mem_limit`) ve `JAVA_TOOL_OPTIONS` ile
  `-XX:MaxRAMPercentage` tanımlıdır. Limit, Postgres ve MinIO'yu sıkıştırmayacak
  şekilde seçilmelidir — aksi hâlde Excel dışa aktarma pikleri (Faz 3) OOM Killer'ı
  yanlış container'da tetikler.
- Postgres verisi `scrumtools-postgres-data`, MinIO verisi `scrumtools-minio-data`
  volume'ünde kalıcıdır. İlk açılışta `db/init/01-create-schema.sh` şemayı oluşturur
  (sadece boş volume ile çalışır).
- MinIO console'a (9001) dışarıdan erişim kapalıdır; gerekirse SSH tüneli kullanın:
  `ssh -L 9001:localhost:9001 user@sunucu` sonrası compose'a geçici port ekleyin.
- `MINIO_BUCKET` değiştirilirse `frontend/nginx.conf` içindeki
  `/scrumtools-attachments/` location'ı da güncellenmelidir.
