# PostForge E-posta Şablonları

ScrumTools'un uygulama e-postaları **PostForge** (https://postforge.kocak.net.tr) üzerinden
gönderilir. Konu ve HTML gövde PostForge'da tutulur; ScrumTools yalnızca `templateCode` +
`params` gönderir:

```bash
curl -X POST https://postforge.kocak.net.tr/api/v1/emails \
  -H "X-Api-Key: pf_live_..." \
  -H "Content-Type: application/json" \
  -d '{"templateCode":"scrumtools-member-invite","to":"alici@ornek.com",
       "params":{"name":"Ahmet","orgName":"Acme","setupUrl":"https://..."}}'
```

Bu klasör şablonların **kaynak kodudur** — PostForge'daki kayıtlar buradan üretilir.
Şablon metnini değiştirdiğinizde import script'ini tekrar çalıştırın.

## Dosyalar

| Dosya | Açıklama |
|---|---|
| `templates/*.html` | Mustache şablonları (`{{degisken}}`) |
| `templates.json` | Kod, ad, konu şablonu ve örnek parametreler |
| `import-templates.ps1` | Şablonları PostForge'a yükler/günceller (Windows, idempotent) |
| `import-templates.sh` | Aynısının bash + `jq` sürümü (Linux/CI) |

## Şablonlar

| Kod | Tetikleyen akış | Parametreler |
|---|---|---|
| `scrumtools-member-invite` | Org sahibi yeni üye oluşturur | `name`, `orgName`, `setupUrl` |
| `scrumtools-password-reset` | Şifremi unuttum | `name`, `resetUrl` |
| `scrumtools-trial-expiring` | Trial bitişine X gün kala | `name`, `orgName`, `daysLeft` |
| `scrumtools-trial-expired` | Trial doldu, FREE'ye düşüldü | `name`, `orgName` |
| `scrumtools-payment-link` | Ödeme linki oluşturuldu | `name`, `orgName`, `planName`, `cycleLabel`, `paymentUrl` |
| `scrumtools-payment-received` | Ödeme alındı, abonelik aktif | `name`, `orgName`, `planName`, `periodEnd` |
| `scrumtools-subscription-expiring` | Abonelik bitişine X gün kala | `name`, `orgName`, `daysLeft`, `hasRenewUrl`, `renewUrl` |
| `scrumtools-subscription-expired` | Abonelik doldu, FREE'ye düşüldü | `name`, `orgName` |

Kodlar [`PostForgeMailService`](../backend/src/main/java/com/scrumtools/service/mail/PostForgeMailService.java)
içindeki sabitlerle birebir aynı olmalıdır.

> **Not:** `hasRenewUrl` bilerek `boolean`'dır. jmustache'te boş string varsayılan olarak
> *truthy* sayıldığı için `{{#renewUrl}}` koşulu güvenilir değildir; koşullu blok
> `{{#hasRenewUrl}}` / `{{^hasRenewUrl}}` üzerinden çalışır.

## Yükleme

**Windows (PowerShell)** — ek bağımlılık yok:

```powershell
$env:PF_EMAIL="you@example.com"; $env:PF_PASSWORD="..."
.\postforge\import-templates.ps1
```

**Linux / CI (bash)** — `jq` gerekir:

```bash
PF_EMAIL=you@example.com PF_PASSWORD=... ./postforge/import-templates.sh
```

Hazır bir JWT varsa login atlanır (`PF_TOKEN`), farklı sunucu için `PF_BASE_URL`
kullanılır. Her iki script de mevcut şablonu `code` ile bulur; varsa `PUT` ile
günceller, yoksa `POST` ile oluşturur.

## ScrumTools tarafı yapılandırma

```bash
MAIL_PROVIDER=postforge
POSTFORGE_BASE_URL=https://postforge.kocak.net.tr
POSTFORGE_API_KEY=pf_live_...
POSTFORGE_SENDER_CODE=          # boşsa PostForge'daki varsayılan gönderici
POSTFORGE_TRACK_CLICKS=true     # tıklama takibi (varsayılan: açık)
POSTFORGE_TRACK_OPENS=true      # açılma takibi (varsayılan: açık)
```

`MAIL_PROVIDER` seçenekleri: `log` (varsayılan, mail gönderilmez), `postforge`, `smtp`.
Gönderim hataları loglanır ve iş akışını (üye oluşturma, ödeme aktivasyonu vb.) durdurmaz.

## Takip (tıklama / açılma)

Takip PostForge'da **şablona değil gönderime** bağlıdır: `POST /api/v1/emails` gövdesine
`trackClicks` / `trackOpens` konmazsa takip kapalı sayılır. `PostForgeMailService` bu iki
alanı her istekte açıkça gönderir.

Bilinmesi gerekenler:

- Tıklama takibi açıkken linkler gönderim anında `…/t/c/<token>` ile değiştirilir; alıcı
  tıklayınca kayıt düşülür ve orijinal adrese yönlendirilir. **Davet ve şifre sıfırlama
  linkleri de bu yönlendirmeden geçer** — tek kullanımlık token PostForge'un tıklama
  kaydına girer. İstenmiyorsa `POSTFORGE_TRACK_CLICKS=false`.
- Takip yalnızca şemalı (`https://…`) adreslerde çalışır; `mailto:`, `tel:`, göreli yollar
  (`/fiyatlar`) ve `www.ornek.com` gibi şemasız adresler yeniden yazılmaz.
- PostForge paketi takibi desteklemiyorsa alanlar gönderilse de yok sayılır; hata dönmez,
  sayaç sıfır kalır (Paket & Fatura ekranından kontrol edin).
- Doğrulama: Gönderimler → mesaj detayında HTML gövdede `/t/c/` geçmiyorsa o mail takipsiz
  gitmiştir. Açılma 1x1 pikselle ölçüldüğü için her zaman eksiktir; takip linkine tıklanan
  mail açılmış da sayılır.

## Webhook — davet durumlarının güncellenmesi

Gönderilen **davet** maillerinin durumu (`email_messages` tablosu) PostForge bildirimleriyle
güncellenir ve organizasyon ekranındaki **Gönderilen Davetler** listesinde görünür.
Yalnızca `scrumtools-member-invite` takip edilir; diğer şablonların olayları yok sayılır.

PostForge > Webhook'lar ekranından:

| Alan | Değer |
|---|---|
| Adres | `https://scrumtools.kocak.net.tr/api/webhooks/postforge` |
| Kapsam | ScrumTools uygulaması |
| Olaylar | `message.sent`, `message.failed`, `message.opened`, `message.clicked` |

Oluşan gizli anahtarı ScrumTools'a `POSTFORGE_WEBHOOK_SECRET` olarak verin. Tanımlı değilse
gelen bildirimler **401 ile reddedilir** ve davet durumları `Kuyrukta` kalır.

Doğrulama `X-PostForge-Signature: t=<zaman>,v1=<hex>` başlığı üzerinden yapılır: `v1`,
`"<t>.<ham gövde>"` metninin HMAC-SHA256 özetidir. Tekrarlanan olaylar `X-PostForge-Delivery`
kimliğiyle yutulur (aksi halde açılma/tıklama sayaçları her retry'da şişerdi), 5 dakikadan
eski zaman damgaları replay sayılıp reddedilir.
