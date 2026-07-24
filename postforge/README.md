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
```

`MAIL_PROVIDER` seçenekleri: `log` (varsayılan, mail gönderilmez), `postforge`, `smtp`.
Gönderim hataları loglanır ve iş akışını (üye oluşturma, ödeme aktivasyonu vb.) durdurmaz.
