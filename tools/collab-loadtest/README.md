# Collab yük testi

`/ws/collab` uç noktasının kayıp / gecikme / RAM davranışını ölçer.
Bkz. [COLLAB_WORKSPACE_PLAN.md](../../COLLAB_WORKSPACE_PLAN.md) §12 ve §15.

```bash
npm install
COLLAB_DOC=<documentId> COLLAB_EMAIL=<e-posta> COLLAB_PASSWORD=<şifre> npm start
```

## Ayarlar (ortam değişkeni)

| Değişken | Varsayılan | Açıklama |
|---|---|---|
| `COLLAB_WS_URL` | `ws://localhost:8080/ws/collab` | Ham WS uç noktası |
| `COLLAB_BASE_URL` | `http://localhost:8080` | Giriş için REST kökü |
| `COLLAB_DOC` | — | **Zorunlu.** Doküman UUID'si |
| `COLLAB_TOKEN` | — | JWT; verilirse giriş yapılmaz |
| `COLLAB_EMAIL` / `COLLAB_PASSWORD` | — | Token yoksa giriş bilgisi |
| `COLLAB_CLIENTS` | `20` | Eşzamanlı istemci |
| `COLLAB_DURATION_MS` | `600000` | Süre (10 dk) |
| `COLLAB_SEND_INTERVAL_MS` | `200` | İstemci başına yazma aralığı |
| `COLLAB_BACKEND_CONTAINER` | `scrumtools-backend` | `docker stats` hedefi |

## Faz 0'da ne bekleniyor

Röle Faz 1'de geliyor ve şu an `DenyAllCollabDocumentAccessResolver` yürürlükte —
**tüm bağlantılar `4403` ile kapanır.** Bugün betiğin doğruladığı şey budur:
uç nokta ayakta, handshake çalışıyor, yetkilendirme uygulanıyor.

Faz 1'den sonra aynı komut gerçek sayıları üretir; kabul eşikleri:
gecikme p95 < 300 ms, kayıp %0, 20 istemcide backend RAM artışı < 100 MB.
