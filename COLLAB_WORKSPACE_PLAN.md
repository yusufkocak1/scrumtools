# Collab Workspace — Ortak Çalışma Alanı Planı

> Mevcut **Code Share** modülü **tamamen silinip** yerine Google Docs / Confluence tarzı
> eş zamanlı ortak çalışma alanı yazılacak. Üç içerik tipi: **düz/zengin metin**, **kod**,
> **hesap tablosu (makro destekli)**. Docs modülü ile iki yönlü entegre.
>
> Bu doküman **plan**dır — kod yazılmadı.
>
> İlgili dokümanlar:
> [TASK_QUERY_LANGUAGE.md](TASK_QUERY_LANGUAGE.md) · [RICH_FILTER_PLAN.md](RICH_FILTER_PLAN.md) · [DEPLOYMENT.md](DEPLOYMENT.md)

---

## 0. Alınan Kararlar

Planın tüm geri kalanı bu dört karara göre yazılmıştır:

| # | Karar | Sonucu |
|---|---|---|
| **D1** | **Temiz kesim.** Mevcut Code Share kodu ve tablosu silinir, geriye uyum yok. | Göç betiği yok, `tag`/`team_id` modelden çıkar, ~1 gün tasarruf |
| **D2** | **Univer Pro alınmayacak** (bütçe yok). | Senkron **biz** yazacağız (K4), Excel G/Ç sunucuda Apache POI ile |
| **D3** | **Redis yok** (sunucu kaldırmaz). | Tek backend örneği + süreç-içi oturum kaydı; yatay ölçek yasak (K9) |
| **D4** | **Proje birincil kapsam.** | Dokümanlar `project_id` altında yaşar, Docs ile aynı hizada; takım isteğe bağlı etiket |

D2 ve D3 birlikte mimarinin en önemli kısıtı: **sunucu mümkün olduğunca aptal ve hafif
kalmalı.** Bu yüzden K2 (sunucu CRDT tutmaz) ve K5 (formül sonucu saklanmaz) tercih değil,
zorunluluktur. Bkz. §12 Kaynak Bütçesi.

---

## 1. Amaç

Bugün [CodeShare.vue](frontend/src/pages/CodeShare.vue) şu modelle çalışıyor:
kullanıcı bir `tag` yazar, **Fetch** ile son hâli çeker, **Share** ile üzerine yazar.
Bu **son yazan kazanır** modelidir — iki kişi aynı anda çalışırsa biri diğerinin
işini sessizce siler. Arayüzde "edit collaboratively" yazıyor ama arkada eş zamanlılık yok.

Hedef üç katmanlı:

1. **Gerçek eş zamanlı düzenleme** — karakter düzeyinde birleşme, imleç/seçim paylaşımı,
   kimin nerede olduğunu gösteren varlık (presence) katmanı, çakışmasız birleştirme.
2. **Tek editör, üç içerik tipi** — aynı oturum/yetki/versiyon altyapısı üzerinde
   `TEXT` (zengin metin), `CODE` (Monaco), `SHEET` (hesap tablosu + formül + makro).
3. **Docs ile tek beden** — bir Docs sayfasını ortak düzenleyebilme, ortak çalışma
   çıktısını Docs'a sayfa olarak kaydedebilme, ve bir Docs sayfasının **içine canlı
   tablo gömebilme**.

---

## 2. Bugünkü Envanter

### Yeniden kullanılacaklar

| Katman | Var olan | Dosya |
|---|---|---|
| WebSocket taşıma | STOMP + SockJS, JWT'li CONNECT | [WebSocketConfig.java](backend/src/main/java/com/scrumtools/config/WebSocketConfig.java) |
| WS istemcisi | connect/subscribe/send/unsubscribe sarmalayıcı | [websocket.js](frontend/src/api/websocket.js) |
| Kod editörü | Monaco (~~CDN~~ → paketten, Faz 2.5/R5) | [MonacoEditor.vue](frontend/src/components/collab/editors/MonacoEditor.vue) |
| Zengin metin editörü | TipTap 3 (tablo, kod bloğu, resim, link uzantıları kurulu) | [TiptapEditor.vue](frontend/src/components/docs/TiptapEditor.vue) |
| Versiyonlama | `DocPageVersion` + geri yükleme | [DocPageService.java](backend/src/main/java/com/scrumtools/service/DocPageService.java) |
| İzin modeli | Space/Page bazlı READ/WRITE, rol + üyelik çözümlemesi | [DocPermissionService.java](backend/src/main/java/com/scrumtools/service/DocPermissionService.java) |
| Paket kısıtı | `EntitlementService.assertFeature` | [PlanFeature.java](backend/src/main/java/com/scrumtools/entity/enums/PlanFeature.java) |
| Şema yönetimi | `ddl-auto: update`, Flyway kapalı, göçler `*Runner` sınıflarıyla | [application.yml](backend/src/main/resources/application.yml) |

### Silinecekler (D1)

```
backend/src/main/java/com/scrumtools/controller/CodeShareController.java
backend/src/main/java/com/scrumtools/service/CodeShareService.java
backend/src/main/java/com/scrumtools/entity/CodeShare.java
backend/src/main/java/com/scrumtools/repository/CodeShareRepository.java
backend/src/main/java/com/scrumtools/dto/CodeShareRequest.java
backend/src/main/java/com/scrumtools/dto/CodeShareResponse.java
frontend/src/pages/CodeShare.vue
frontend/src/api/CodeShareApi.js
```

`frontend/src/components/share/MonacoEditor.vue` **kalır** — `components/collab/editors/`
altına taşınır ve CRDT bağlayıcısıyla genişletilir.

Veritabanında `code_shares` tablosu `DROP` edilir; `ddl-auto: update` tabloyu **kendiliğinden
silmez**, bunu yapan bir runner gerekir (§13 Faz 0).

### Mevcut yapıdaki üç engel

**E1 — SockJS ikili (binary) çerçeve taşımaz.**
`registry.addEndpoint("/ws").withSockJS()` protokolü metin tabanlıdır. CRDT güncellemeleri
`Uint8Array`'dir. → **K3 ile ayrı ham WS uç noktası açılarak çözülüyor.**

**E2 — `enableSimpleBroker` süreç-içi bir brokerdır.**
Backend iki örneğe çıkarsa A'ya bağlı kullanıcı B'ye bağlı kullanıcının düzenlemesini
görmez. → **D3 gereği zaten tek örnek kalınacak; K9 bunu bir kural hâline getiriyor.**

**E3 — Abonelik (SUBSCRIBE) yetkilendirmesi yok.** ⚠️
[WebSocketConfig.java:62](backend/src/main/java/com/scrumtools/config/WebSocketConfig.java#L62)
yalnızca CONNECT'te JWT doğruluyor. Kimliği doğrulanmış **herhangi bir** kullanıcı
`/topic/...` altındaki her şeye abone olabilir — retro panoları, poker oyları dahil.
Bugün de açık olan bu delik, ortak dokümanlarda doğrudan **yetkisiz içerik okuma**ya döner.
Faz 0'da kapatılır; mevcut modüller de kazanır.

---

## 3. Kavram Modeli

| Kavram | Karşılığı | Ne işe yarar |
|---|---|---|
| **Ortak doküman** (`CollabDocument`) | Google Docs dosyası | Kalıcı içerik kabı. `TEXT` / `CODE` / `SHEET` tipinde. |
| **Oturum** (session) | Açık sekme kümesi | Bir dokümanda o an aktif bağlantılar; bellekte tutulur. |
| **Katılımcı** (participant) | Sağ üstteki avatarlar | Kullanıcı + renk + imleç/seçim konumu. Kalıcı değil. |
| **Güncelleme** (update) | CRDT deltası | İkili, birleştirilebilir değişiklik paketi. Append-only. |
| **Anlık görüntü** (snapshot) | Kaydedilmiş hâl | Sıkıştırılmış CRDT durumu + okunabilir metin çıktı. |
| **Bağlantı** (link) | Docs eşleşmesi | Dokümanın hangi `DocPage` ile aynalandığı. |
| **Makro** | Apps Script | Doküman üzerinde çalışan JS betiği + tetikleyicisi. |
| **Gömme** (embed) | Confluence macro | Docs sayfası içinde canlı tablo/kod bloğu. |

---

## 4. Mimari Kararlar

### K1 — Senkron omurgası **CRDT (Yjs)**, üç tip için de aynı

Alternatifler OT (Operational Transform) veya sunucu tarafı kilitti.
CRDT'nin seçilme nedeni **sunucunun dönüşüm mantığı bilmesine gerek olmaması**:
birleştirme istemcide, matematiksel olarak çakışmasız yapılır. Backend Java'dır ve
Java tarafında olgun bir OT/CRDT kütüphanesi yoktur — CRDT ile buna ihtiyaç kalmaz.

D3 (Redis yok, sunucu zayıf) altında bu ayrıca **maliyet kararıdır**: hesabı istemciler
yapar, sunucu yalnızca bayt taşır.

Ek kazanç: bağlantı koptuğunda çevrimdışı düzenleme kendiliğinden çalışır, geri
bağlanınca birleşir.

Bağlayıcılar hâlihazırdaki editörlerle birebir örtüşüyor:

| Tip | Editör | Bağlayıcı | Lisans |
|---|---|---|---|
| `TEXT` | TipTap 3 (kurulu) | `@tiptap/extension-collaboration` + `y-prosemirror` | MIT |
| `CODE` | Monaco (kurulu) | `y-monaco` | MIT |
| `SHEET` | Univer ızgarası | `Y.Map` hücre modeli (elle yazılır) | — |

### K2 — Sunucu **aptal röle + kalıcılaştırıcı**, iş mantığı yok

Backend'in yaptığı üç şey: (a) yetkiyi doğrula, (b) güncellemeyi aynı dokümanın diğer
abonelerine aynen ilet, (c) kalıcılaştır. CRDT'yi **açmaz, yorumlamaz, bellekte tutmaz.**

Sonucu doğrudan kaynak bütçesine yansır: doküman başına sunucu RAM'i "birkaç KB bağlantı
durumu" seviyesinde kalır, açık doküman sayısıyla değil eşzamanlı bağlantı sayısıyla ölçeklenir.

Sıkıştırma (compaction) ve okunabilir çıktı üretimi için CRDT'yi çözmek gerekir;
bunu **sunucu değil istemci** yapar (K6).

### K3 — Taşıma: ayrı ham WebSocket uç noktası `/ws/collab` ✅ *karar verildi*

Ortak düzenleme trafiği (saniyede onlarca küçük ikili paket) STOMP/SockJS'in metin
çerçevelerine sıkıştırılmaya uygun değil.

| | A: Mevcut STOMP + base64 | **B: Ayrı ham WS (seçilen)** |
|---|---|---|
| Ek altyapı | Yok | `WebSocketHandler` + `HandshakeInterceptor` |
| Yük şişmesi | %33 | Yok |
| Sunucu CPU | base64 kodlama/çözme her pakette | Yok |
| Yedek (fallback) | SockJS mevcut | Yok — modern tarayıcılarda sorun değil |
| İş | ~0.5 gün | ~2 gün |

**B seçildi.** Nedeni yalnızca zarafet değil: D1 ile zaten sıfırdan yazıyoruz, ve D3'ün
kısıtlı sunucusunda %33 fazla bant genişliği + her pakette base64 CPU'su kalıcı bir vergi olur.
1.5 günlük ekstra iş, tek seferlik.

Varlık/presence ve "doküman kaydedildi" gibi seyrek bildirimler mevcut STOMP kanalında kalır;
yalnızca yoğun CRDT deltası ham WS'e taşınır.

`nginx.conf`'a `/ws/collab` için `Upgrade`/`Connection` başlıkları ve
`proxy_read_timeout 3600s` eklenmeli. NPM (nginx-proxy-manager) tarafında da
WebSocket desteği açık olmalı.

### K4 — Hesap tablosu: **Univer OSS** (Apache-2.0) + **kendi senkronumuz** ✅ *karar verildi*

Sıfırdan ızgara yazmak (sanallaştırılmış çizim, seçim, doldurma tutamacı, birleşik
hücre, dondurulmuş satır, Excel uyumlu kopyala-yapıştır) aylar sürer.

| Aday | Lisans | Durum |
|---|---|---|
| **Univer** | Apache-2.0 (çekirdek) | Formül, pivot, koşullu biçim. **Seçilen.** |
| Handsontable | Ticari | SaaS için ücretli. |
| HyperFormula | GPLv3 / ticari | GPLv3 tüm ScrumTools'u açmaya zorlar → **kullanılamaz**. |
| Fortune-sheet | MIT | Luckysheet çatallaması, React bağımlı. |
| x-spreadsheet | MIT | Çok temel, formül motoru zayıf. |

**D2 gereği:** Univer'in gerçek zamanlı iş birliği, içe/dışa aktarma ve yazdırma
eklentileri **Univer Pro'dadır (ticari lisans)**. Lisans alınmayacağı için:

- Univer OSS **yerel (tek kullanıcı) motor** olarak kullanılır: ızgara, seçim, biçimlendirme,
  formül motoru, kopyala-yapıştır — hepsi bedava gelir.
- **Senkronu biz yazarız:** Univer'in komut/mutation akışını dinleyip `Y.Map`'e yazan,
  Yjs'ten geleni Univer'e uygulayan iki yönlü köprü (`UniverYjsBridge.js`).
- **Excel içe/dışa aktarma sunucuda Apache POI ile** yapılır (§10).

Risk: Univer'in iç mutation sözleşmesi sürümler arası değişebilir → sürüm `package.json`'da
sabitlenir (`~` değil kesin sürüm) ve köprü sözleşme testleriyle korunur. Bkz. R1.

### K5 — Formül sonuçları CRDT'de **saklanmaz**, istemcide türetilir

`=SUM(A1:A100)` yazan hücrede CRDT'ye **yalnızca formül metni** girer. Hesaplanan değer
her istemcide yerel olarak üretilir. Aksi hâlde tek bir hücre değişikliği yüzlerce
türev hücre güncellemesi yayar; ağ trafiği ve `collab_updates` yazma yükü patlar.

D3 altında bu kritik: hesaplama maliyeti istemci CPU'sunda kalır, sunucuya hiç uğramaz.

Hesaplanan değerler yalnızca **dışa aktarma ve anlık görüntü** anında yazılır.

### K6 — Anlık görüntüyü **istemci** üretir, sunucu sanitize eder

Sunucuda Yjs olmadığı için (K2) "kaydet" anında okunabilir çıktıyı (`TEXT` → HTML,
`CODE` → düz metin, `SHEET` → JSON) üreten taraf istemcidir. Kural:

- Oturumdaki **en eski bağlanan** istemci "yazar (writer)" seçilir.
- Yazar, 30 sn boşta kalma veya 5 dk süre dolumunda `POST /snapshot` ile hem
  ikili CRDT durumunu hem okunabilir metni gönderir.
- Yazar düşerse sunucu yeni yazar atar (`/topic/collab/{id}/writer`).
- Sunucu okunabilir metni **doğrulamaz ama sanitize eder** — `TEXT` içeriği Docs'a
  yazılacağı için sunucu tarafı HTML temizleme zorunlu (istemci DOMPurify'ına güvenilmez).

"Hiç kimse kaydetmeden herkes çıktı" durumu da çözülür: ham güncellemeler zaten
`collab_updates` tablosundadır, bir sonraki açılışta yeniden oynatılır. **Veri kaybı
snapshot'a değil, append log'a bağlıdır.**

### K7 — Makro dili **JavaScript**, VBA **değil**

"Excel makrosu" ihtiyacının gerçek karşılığı VBA yorumlayıcısı yazmak değildir —
o tek başına bir ürün. Karşılık **Google Apps Script modeli**: doküman nesne modeli
üzerinde çalışan, sürüm kontrollü, tetikleyicili JS betikleri.

VBA içe aktarma **kapsam dışı**dır ve dokümantasyonda açıkça belirtilmelidir.
`.xlsm` yüklenirse makro kısmı atılır, kullanıcı uyarılır.

### K8 — Makro yürütme **iki yerde**: tarayıcı (etkileşimli) + sunucu (zamanlanmış)

| | Tarayıcı (Web Worker) | Sunucu (GraalJS) |
|---|---|---|
| Tetikleyici | Manuel, `onOpen`, `onEdit` | `onSchedule`, `onWebhook` |
| İzolasyon | Worker + dondurulmuş global, DOM/ağ yok | Polyglot Context, host erişimi kapalı |
| Zaman aşımı | `worker.terminate()` | Bekçi iş parçacığı + `context.close(true)` |
| Faz | Faz 4 | Faz 5 |

**Tarayıcı tarafı veri modeli — "oku-anlık-görüntü / yaz-toplu":** worker'a doküman
API'si canlı verilmez; makronun okuduğu aralıklar kopyalanıp gönderilir, makro
**eşzamanlı** çalışır, dönen değişiklik listesi tek işlemde (transaction) Yjs'e uygulanır.
Bu, `SharedArrayBuffer` + COOP/COEP başlığı gerektiren senkron RPC karmaşasını
tamamen ortadan kaldırır.

D3 notu: sunucu tarafı makro (Faz 5) kısıtlı sunucuda **ciddi bir risk** — kaçak bir
betik tek CPU'yu yer. Faz 5'e girerken eşzamanlı sunucu makro sayısı 1'e sabitlenmeli
(tek iş parçacıklı kuyruk) ve varsayılan 10 sn zaman aşımı konmalı.

### K9 — **Tek backend örneği kuralı** ✅ *karar verildi*

D3 gereği Redis rölesi veya RabbitMQ yok. Backend **tek örnek** çalışır.

`docker-compose.yml`'da `container_name: scrumtools-backend` tanımlı olduğu için
`docker compose up --scale backend=N` zaten **hata verir** — kazara ölçeklemeye karşı
mevcut bir koruma var. Bu tesadüfi korumaya güvenmek yerine açık hâle getirilir:

- `application.yml`'a `app.collab.single-instance: true` bayrağı.
- Başlangıçta uyarı log'u ve `/actuator/health` içinde `collab` bileşeni.
- `DEPLOYMENT.md`'ye "backend ölçeklenemez — ortak düzenleme süreç-içi brokerdır" notu.

**Yeniden başlatma davranışı:** deploy sırasında tüm collab oturumları düşer. Veri
kaybı olmaz (K6 — güncellemeler Postgres'te), istemciler otomatik yeniden bağlanıp
senkronlanır. Kullanıcı yalnızca 2–5 sn "yeniden bağlanılıyor" görür. İstemcide
üstel geri çekilmeli (exponential backoff) yeniden bağlanma şart.

Gelecekte ölçek gerekirse en ucuz yol Redis değil, **doküman yapışkanlığı**dır:
nginx'te `/ws/collab` yolunu `hash $arg_doc consistent` ile yönlendirmek. Bugün gerekmiyor.

### K10 — Temiz kesim, göç yok ✅ *karar verildi*

D1 gereği `code_shares` verisi taşınmaz. Yapılacaklar:

- Tablo `DROP TABLE IF EXISTS code_shares` ile silinir (`CollabCleanupRunner`, tek seferlik).
- `/api/teams/{teamId}/code-shares/**` uç noktaları kaldırılır.
- `/codeShare/:teamId` rotası `/collab`'a **yönlendirilir** (tek satır; eski yer imleri
  404 yerine yeni ekrana düşsün). Veri taşınmadığı için içerik gelmez, bu kabul edildi.

**⚠️ Enum tuzağı:** `PlanFeature.CODE_SHARE` sabiti `plan_features` tablosunda
`@ElementCollection` + `EnumType.STRING` olarak saklanıyor
([Plan.java:52](backend/src/main/java/com/scrumtools/entity/Plan.java#L52)).
Enum'dan `CODE_SHARE` sabitini **doğrudan silerseniz**, o değeri içeren mevcut plan
satırları yüklenirken uygulama patlar. Doğru sıra:

1. `JdbcTemplate` ile **native SQL** çalıştıran, `@Order(HIGHEST_PRECEDENCE)` bir runner:
   `UPDATE plan_features SET feature='COLLAB_WORKSPACE' WHERE feature='CODE_SHARE'`
   — native SQL enum dönüştürücüsünden geçmediği için sabit silinmiş olsa bile çalışır.
2. Ancak o zaman enum'dan `CODE_SHARE` çıkarılır.

Aynı sürümde yapılabilir; sıralama runner'ın önceliğiyle garanti edilir.

---

## 5. Veri Modeli

```
collab_documents
  id                UUID PK
  organization_id   FK organizations      -- entitlement kontrolü için
  project_id        FK projects  NOT NULL -- D4: proje birincil kapsam
  team_id           FK teams     NULL     -- isteğe bağlı etiket / filtre
  type              ENUM(TEXT, CODE, SHEET)
  title             VARCHAR(500)
  language          VARCHAR(50)  NULL     -- CODE için (javascript, java...)
  doc_page_id       FK doc_pages NULL UNIQUE
  link_mode         ENUM(NONE, MIRROR, EMBEDDED)
  state             BYTEA                 -- sıkıştırılmış Yjs durumu
  state_vector      BYTEA
  snapshot_text     TEXT                  -- HTML / kod / JSON — arama ve okuma için
  snapshot_seq      BIGINT
  last_seq          BIGINT
  created_by, updated_by, created_at, updated_at, archived_at

  INDEX (project_id, type, updated_at DESC)
  INDEX (team_id) WHERE team_id IS NOT NULL
```

D1 sayesinde `tag` sütunu ve `UNIQUE(team_id, tag)` kısıtı **yok**. Dokümanlar UUID ile
adreslenir; kullanıcı dostu erişim liste ekranı ve arama üzerinden olur. Bu, eski modeldeki
"aynı tag'i iki kişi farklı amaçla kullanır" çakışmasını da ortadan kaldırır.

```
collab_updates                            -- append-only, sıkıştırmaya kadar
  id            BIGSERIAL PK
  document_id   FK collab_documents (ON DELETE CASCADE)
  seq           BIGINT
  payload       BYTEA
  author_id     FK users NULL
  created_at    TIMESTAMP
  UNIQUE (document_id, seq)
```

> **Sıkıştırma (compaction):** bir doküman için 500 satırı veya toplam 1 MB'ı geçince
> yazar istemciden anlık görüntü istenir; snapshot yazıldıktan sonra
> `seq <= snapshot_seq` satırları silinir. `@Scheduled` görevde döner.
> D3 altında bu **isteğe bağlı değil zorunlu** — yoksa Postgres diski büyür.

```
collab_macros
  id            UUID PK
  document_id   FK collab_documents NULL  -- NULL ise proje kütüphanesi makrosu
  project_id    FK projects
  name, description
  source        TEXT                      -- JS kaynağı
  trigger_type  ENUM(MANUAL, ON_OPEN, ON_EDIT, ON_SCHEDULE, ON_WEBHOOK)
  schedule_cron VARCHAR(100) NULL
  enabled       BOOLEAN
  source_hash   VARCHAR(64)               -- onay sonrası değişti mi
  approved_by   FK users NULL
  approved_at   TIMESTAMP NULL
  created_by, created_at, updated_at

collab_macro_runs
  id, macro_id, triggered_by, trigger_type
  status ENUM(RUNNING, SUCCESS, FAILED, TIMEOUT, DENIED)
  started_at, finished_at, duration_ms
  log TEXT, error TEXT
```

### `SHEET` tipinin Yjs içi yerleşimi

```
Y.Doc
├── meta            Y.Map   { title, sheetOrder: Y.Array<sheetId>, activeSheet }
├── styles          Y.Map   styleId -> { font, bg, align, numFmt, border }
└── sheet:<id>
    ├── cells       Y.Map   "R12C3" -> Y.Map { v, f, t, s }   // ham değer, formül, tip, stil
    ├── rows        Y.Map   "12"    -> { h, hidden }
    ├── cols        Y.Map   "3"     -> { w, hidden }
    └── merges      Y.Array [{ r, c, rs, cs }]
```

Hücre anahtarını `"R{row}C{col}"` düz string yapmak önemlidir: iç içe `Y.Array`
kullanmak satır ekleme/silmede tüm indeksleri kaydırır ve çakışmaları çoğaltır.
Satır ekleme, anahtarları yeniden yazan tek bir işlem (transaction) olur.

---

## 6. API Yüzeyi

### REST — `/api/projects/{projectId}/collab` *(D4: proje altında)*

| Metot | Yol | Açıklama |
|---|---|---|
| `POST` | `/documents` | Yeni doküman (`type`, `title`, opsiyonel `teamId`, `docPageId`) |
| `GET` | `/documents` | Tip + metin araması + takım filtresi ile listeleme |
| `GET` | `/documents/{id}` | Üstveri + `snapshot_text` (salt okuma için yeterli) |
| `GET` | `/documents/{id}/state` | İkili CRDT durumu + sıkıştırılmamış güncellemeler |
| `POST` | `/documents/{id}/snapshot` | Yazar istemciden anlık görüntü (K6) |
| `PATCH` | `/documents/{id}` | Başlık, dil, takım etiketi, arşiv |
| `DELETE` | `/documents/{id}` | Arşivle (yumuşak silme) |
| `POST` | `/documents/{id}/link-doc-page` | Docs sayfasına bağla / bağı çöz |
| `POST` | `/documents/{id}/publish-to-docs` | Space + başlık ile yeni `DocPage` üret |
| `GET` | `/documents/{id}/history` | Anlık görüntü zaman çizelgesi |
| `POST` | `/documents/{id}/export` | `xlsx` / `csv` / `md` / `html` |
| `POST` | `/documents/import` | `xlsx` / `csv` yükle → yeni `SHEET` |
| `GET/POST/PUT/DELETE` | `/macros/**` | Makro CRUD |
| `POST` | `/macros/{id}/run` | Sunucu tarafı yürütme (Faz 5) |
| `GET` | `/macros/{id}/runs` | Çalıştırma günlüğü |

### WebSocket

**Ham WS (K3):** `wss://.../ws/collab?doc={id}&since={seq}&token={jwt}`
İkili çerçeveler, `[0]` mesaj tipi + ham yük:

| Bayt 0 | Anlam |
|---|---|
| `0` | Sync — **ham Yjs güncellemesi**. Röle edilir ve saklanır. |
| `1` | Awareness — imleç, seçim, kullanıcı rengi. Röle edilir, saklanmaz. |
| `2` | Sunucu → istemci kontrol (hello, writer atama, anlık görüntü isteği) |

> **Faz 1'de sadeleştirildi:** ilk tasarımda `y-protocols/sync` mesaj biçimi vardı.
> O protokol sunucunun bir `Y.Doc` tutup durum vektörü karşılaştırması yapmasına
> göre kurulu — K2 ile taban tabana zıt. Yerine: ilk durum REST'ten
> (`GET /state`) alınır, bağlantıda sunucu `since`'ten sonraki paketleri geri
> oynatır, sonrası düz rölededir. Yjs güncellemeleri etkisiz-tekrarlanabilir
> olduğu için geri oynatma ile rölenin çakışması zararsızdır; bu, senkron el
> sıkışmasını ve sunucudaki durum vektörü mantığını tamamen gereksiz kılar.

Handshake'te JWT + doküman yetkisi doğrulanır. **Handshake reddedilmez**, bağlantı
kabul edilip `4403` ile kapatılır: `false` döndürmek HTTP 403 üretir ve tarayıcı
bunu uygulamaya `1006` olarak yansıtır — ağ kesintisinden ayırt edilemez, istemci
"yetkim yok, durma" ile "bağlantı koptu, geri çekilerek dene" arasında karar veremez.

Handshake'te JWT + doküman yetkisi doğrulanır; yetkisiz bağlantı `4403` koduyla kapatılır.
Yazma yetkisi olmayan kullanıcının gönderdiği sync mesajları sunucuda **atılır**
(istemci kısıtına güvenilmez).

**STOMP (mevcut kanal, seyrek olaylar):**

```
/topic/collab/{documentId}/presence   → katılımcı listesi değişti
/topic/collab/{documentId}/saved      → { seq, savedAt, by }
/topic/collab/{documentId}/macro      → { runId, status, log }
/topic/collab/{documentId}/writer     → { writerSessionId }
```

---

## 7. Frontend Mimarisi

```
frontend/src/
├── pages/
│   ├── CollabHome.vue              # doküman listesi/galerisi (CodeShare.vue'nun yerine)
│   └── CollabDocument.vue          # tek doküman kabuğu; tipe göre editör yükler
├── components/collab/
│   ├── CollabShell.vue             # başlık, katılımcı avatarları, kaydet durumu, menü
│   ├── PresenceBar.vue             # renkli avatarlar + "X yazıyor"
│   ├── ConnectionBanner.vue        # "yeniden bağlanılıyor" (K9 restart senaryosu)
│   ├── editors/
│   │   ├── CollabTextEditor.vue    # TiptapEditor.vue + Collaboration uzantısı
│   │   ├── CollabCodeEditor.vue    # MonacoEditor.vue (taşınmış) + y-monaco
│   │   └── CollabSheetEditor.vue   # Univer + Y.Map köprüsü
│   ├── sheet/
│   │   ├── UniverYjsBridge.js      # mutation <-> Y.Map çevirisi (K4'ün kalbi)
│   │   ├── FormulaBar.vue
│   │   └── SheetToolbar.vue
│   ├── macro/
│   │   ├── MacroPanel.vue          # liste, düzenle, çalıştır, günlük
│   │   ├── MacroEditor.vue         # Monaco + API tip tanımları (d.ts) ipucu
│   │   └── macroWorker.js          # Web Worker sandbox (K8)
│   ├── DocsLinkDialog.vue          # "Docs'a kaydet" / "sayfaya bağla"
│   └── HistoryPanel.vue            # anlık görüntü zaman çizelgesi
├── composables/
│   ├── useCollabDoc.js             # Y.Doc yaşam döngüsü, provider, awareness
│   ├── useCollabPresence.js
│   └── useMacroRunner.js
└── api/CollabApi.js
```

**Yönlendirme (router.js):**

```js
{ path: '/projects/:projectId/collab',              name: 'CollabHome' }
{ path: '/projects/:projectId/collab/:documentId',  name: 'CollabDocument' }
{ path: '/codeShare/:teamId', redirect: '/projects' }   // K10 — eski yer imleri 404 olmasın
```

`useCollabDoc.js` sözleşmesi — tüm editörler bunu paylaşır:

```js
const {
  ydoc,          // Y.Doc
  provider,      // WebsocketProvider (özel, ham WS)
  awareness,     // katılımcı durumları
  status,        // 'connecting' | 'synced' | 'offline'
  isWriter,      // bu istemci anlık görüntü yazarı mı (K6)
  canWrite,      // sunucudan gelen yetki — salt-okuma düşürmesi
  participants,  // [{ userId, name, color, cursor }]
  requestSnapshot()
} = useCollabDoc(documentId)
```

---

## 8. Docs Entegrasyonu — Üç Yön

### Y1 — Docs sayfasını ortak düzenle (Docs → Collab)

[DocPage.vue](frontend/src/pages/DocPage.vue) üzerindeki "Düzenle" butonunun yanına
**"Ortak Düzenle"** eklenir.

1. `POST .../collab/documents` — `type=TEXT`, `docPageId=<sayfa>`, `linkMode=MIRROR`.
2. Sayfa için zaten bir doküman varsa sunucu onu döndürür (idempotent).
3. **Tohumlama (seeding) — kritik nokta:** `DocPage.content` (HTML) yalnızca Y.Doc
   **boşken** ProseMirror üzerinden `prosemirrorToYDoc` ile aktarılır. Aksi hâlde her
   açılışta içerik ikilenir. Sunucuda `state IS NULL` koşullu tek işlemle kilitlenir.
4. Kaydetme: yazar istemci anlık görüntü gönderince `DocPageService` üzerinden hem
   `DocPage.content` güncellenir hem **`DocPageVersion`** yazılır —
   `changeSummary = "Ortak düzenleme — {n} katılımcı"`. Mevcut sürüm geçmişi ve geri
   yükleme olduğu gibi çalışmaya devam eder.
5. Yetki tek kaynaktan: `DocPermissionService.checkWriteAccess`. Ayrı izin ağacı yok.

### Y2 — Ortak çalışmayı Docs'a kaydet (Collab → Docs)

Bağımsız açılmış bir dokümanda **"Docs'a Kaydet"**: space + üst sayfa seçilir,
`snapshot_text` içerikle yeni `DocPage` oluşturulur, `doc_page_id` bağlanır ve
`link_mode` `MIRROR`'a döner. `SHEET` tipi Docs'a **HTML tablo** olarak düzleştirilir;
canlı tablo isteniyorsa Y3 kullanılır.

### Y3 — Docs sayfasına canlı tablo göm (embed)

TipTap'e özel bir düğüm (custom node) eklenir:

```html
<div data-collab-embed data-document-id="…" data-type="SHEET" data-height="420"></div>
```

- Docs **okuma** modunda: `snapshot_text`'ten üretilmiş salt-okunur tablo + "Aç" düğmesi.
- Docs **düzenleme** modunda: gömülü canlı Univer örneği (kendi WS bağlantısıyla).
- `DOMPurify` yapılandırmasına `data-collab-embed` izni eklenmeli —
  [DocPage.vue:349](frontend/src/pages/DocPage.vue#L349) `ADD_ATTR` listesi.

Bu, "sprint metriklerini bir makroyla çeken canlı tablo, ekip dokümanının ortasında"
senaryosunu açar — planın en ayırt edici parçası.

---

## 9. Makro Motoru

### 9.1 API yüzeyi (`ScrumTools` global nesnesi)

```js
// ── Doküman ──────────────────────────────────────────────
const doc   = ScrumTools.getActiveDocument()
const sheet = doc.getSheetByName('Sayfa1')      // veya doc.getActiveSheet()

sheet.getRange('A1:C10').getValues()            // [[...], ...]
sheet.getRange('A1:C10').setValues(matrix)
sheet.getRange('D1').setFormula('=SUM(A1:C1)')
sheet.getRange('A1:C1').setStyle({ bold: true, bg: '#eef' })
sheet.insertRows(5, 3);  sheet.deleteColumns(2, 1)
sheet.sort({ column: 2, ascending: false })

doc.getText();  doc.setText(s);  doc.replaceText(/eski/g, 'yeni')   // TEXT tipi

// ── ScrumTools verisi — ayırt edici kısım ────────────────
ScrumTools.tasks.query('project = "ABC" AND status != Done ORDER BY priority')
ScrumTools.sprints.current(teamId)
ScrumTools.docs.getPage(pageId)
ScrumTools.docs.savePage(spaceId, title, html)

// ── Yardımcılar ─────────────────────────────────────────
ScrumTools.ui.toast('Tamamlandı'), .alert(), .prompt()
ScrumTools.utils.formatDate(d, 'dd.MM.yyyy')
ScrumTools.http.fetch(url, opts)     // yalnızca allowlist + PRO paket + sunucu vekili
```

`ScrumTools.tasks.query` doğrudan [TASK_QUERY_LANGUAGE.md](TASK_QUERY_LANGUAGE.md)
motorunu kullanır — ayrı bir sorgu yolu yazılmaz.

### 9.2 Sandbox ve güvenlik

Makro, **kendisini yazan değil, çalıştıran kullanıcının** yetkisiyle koşar. Kötü niyetli
makro, yetkisi yüksek birine çalıştırılırsa yetki yükseltme aracına döner. Önlemler:

- **Onay akışı:** kaynak değişince `source_hash` bozulur, `approved_at = NULL` olur;
  `COLLAB_MANAGE_MACRO` yetkisi olan biri onaylayana kadar yalnızca yazarı çalıştırabilir.
- **İlk çalıştırmada açık rıza:** "Bu makro görevlerinizi okuyacak ve Docs'a yazacak"
  diyalogu; makronun kullandığı API alanları statik taramayla listelenir.
- **Otomatik tetikleyiciler onaysız çalışmaz** — `ON_OPEN` / `ON_EDIT` için onay şart.
- **Sandbox:** Web Worker; `fetch`, `XMLHttpRequest`, `importScripts`, `WebSocket`,
  `indexedDB` silinir; globaller `Object.freeze` ile dondurulur.
- **Kaynak limitleri:** 5 sn duvar saati (PRO 30 sn), 64 MB, çıktı satır sınırı;
  aşımda `worker.terminate()` ve `TIMEOUT` kaydı.
- **`ScrumTools.http`** doğrudan ağa çıkmaz; sunucu vekili üzerinden, alan adı allowlist'i
  ve SSRF koruması (özel IP aralıkları reddedilir) ile. Mevcut `SCM_ALLOW_PRIVATE_HOSTS`
  deseni tekrar kullanılır.
- **Denetim:** her çalıştırma `collab_macro_runs`'a yazılır; yazan makrolar
  `ActivityService` akışına düşer.

### 9.3 Makro kaydedici (recorder)

Kullanıcıların çoğu betik yazmaz. **"Makroyu Kaydet"** modu: kullanıcının yaptığı
işlemler (biçim, sıralama, formül girme) API çağrılarına çevrilip düzenlenebilir bir
betik olarak üretilir.

#### Neden köprünün üstüne "ince bir katman" değil

Planın ilk hâli bunu ince bir katman sayıyordu, çünkü komut akışı zaten dinleniyor.
Faz 3'te köprü yazıldıktan sonra bu varsayım yanlış çıktı:
[UniverYjsBridge.js](frontend/src/collab/UniverYjsBridge.js) aynı akışı dinliyor ama
`command.type !== CommandType.MUTATION` olanı **eliyor** — ve bunu bilerek yapıyor,
CRDT'ye yazılması gereken şey oturmuş düşük seviye değişikliktir.

Kaydedici için mutation yanlış seviye:

- Tek bir kullanıcı işlemi birden çok mutation üretir ("satır sil" = `RemoveRowMutation`
  \+ değer kaydırması + birleşik hücre düzeltmesi).
- Mutation parametreleri dönüşüm **sonrası** ve mutlaktır: "kalın yap", hücre matrisi
  üzerinde tam stil nesnesi olarak gelir, `bold` niyeti olarak değil.
- Mutation'lardan üretilmiş bir betik okunamaz; kaydedicinin bütün değeri
  **düzenlenebilir** çıktı vermesi.

Kaydedici bu yüzden aynı `onCommandExecuted` akışına **`CommandType.COMMAND`** filtresiyle
abone olur — komut, kullanıcının ne yaptığıdır. Aynı kaynak, karşıt filtre; ikisi
birleştirilemez, sonradan "sadeleştirilmesin" diye buraya yazılıyor.

#### Ne kaydedilir, ne kaydedilmez

- **Yalnızca yerel kullanıcı komutları.** Uzak değişiklikler köprünün `applyLocally`'si
  üzerinden gelir; `applyingRemote` açıkken kaydedici susar. Aksi hâlde yanınızda çalışan
  birinin düzenlemeleri sizin makronuza yazılırdı.
- **Makro yazımları hariç** (`'macro'` origin) — çalışan bir makronun yanında kayıt
  yapmak, kendini çağıran bir betik üretirdi.
- Seçim, kaydırma, imleç hariç.
- **Geri al bir tuzak:** kullanıcı bir şey yapar, beğenmez, geri alır. Kaydedici ikisini
  birden yazarsa betik hatayı da tekrarlar. v1 davranışı: `undo` son kaydedilen adımı
  **siler**; yığın kaydın başlangıcından geriye giderse kayıt durdurulur ve kullanıcı
  uyarılır. Geri alma geçmişini betiğe doğru yansıtmaya çalışmak, kazandırdığından çok
  daha karmaşık.

#### Komut → API eşlemesi

Kaydedici yalnızca Faz 4'te **gerçekten var olan** makro API'sini üretebilir
(§9.1 — `setValues`, `setFormula`, `setStyle`, `insertRows/Columns`,
`deleteRows/Columns`, `sort`):

| Univer komutu | Üretilen satır |
|---|---|
| `sheet.command.set-range-values` | `sheet.getRange('A1:C3').setValues([...])` |
| `sheet.command.set-range-bold` / `-italic` / `-font-color` … | `sheet.getRange('A1').setStyle({ bold: true })` |
| `sheet.command.insert-row` / `-col` | `sheet.insertRows(4, 2)` |
| `sheet.command.remove-row` / `-col` | `sheet.deleteRows(4, 2)` |
| `sheet.command.sort-range` | `sheet.sort({ column: 2, ascending: false })` |
| birleştirme, satır yüksekliği, dondurma, filtre, koşullu biçim | **eşleme yok** |

Eşlemesi olmayan komut **sessizce atlanmaz**: yerine
`// kaydedilemedi: birleştirilmiş hücre` yorumu yazılır ve kayıt bitince kaç işlemin
düştüğü söylenir. Sessizce kısalmış bir betik daha kötüdür — kullanıcı çalıştırır, işin
yarısı olmaz ve şüphelenmesi için bir sebep yoktur.

#### Sınırlar

- **Üretilen betik mutlaktır**, göreli değil: aralıklar ve değerler sabit yazılır.
  Excel'in R1C1 tarzı göreli kaydı v1'de yok — göreli kayıt, kullanıcının anlaması
  gereken bir çapa kavramı getirir; oysa çıktı bitmiş bir araç değil, **düzenlenecek bir
  başlangıç**.
- **Yalnızca `SHEET`.** TEXT/CODE'da kayıt, son içeriği yazan tek bir `setText` üretirdi;
  bu makro değil, dokümanın kopyası.
- Kayıt tamamen istemcide tutulur, sunucuda durum yok — sekme kapanırsa kayıt gider.
  Birkaç dakikalık bir iş için ikinci bir doküman-kapsamlı oturum kavramı açmaya değmez.
- Kaydedilen makro **yeni ve onaysız** bir makrodur; §9.2 kuralları aynen işler.
  Kaydedici, onaydan kaçmanın yolu değildir.

---

## 10. Excel İçe/Dışa Aktarma

D2 gereği Univer'in Pro içe/dışa aktarması kullanılamaz → **sunucu tarafında Apache POI**
(Apache-2.0). Backend zaten Java, ek lisans maliyeti yok.

Kısıtlı sunucu (D3) için akış **bellekte tutmayan** biçimde kurulur:

- **Yazma:** `SXSSFWorkbook` (streaming) — satırlar diske akıtılır, heap'te pencere kadar
  satır durur. Klasik `XSSFWorkbook` 200k hücrede yüzlerce MB yer, kullanılmaz.
- **Okuma:** `XSSF + SAX` olay tabanlı okuyucu — DOM ağacı kurulmaz.
- İçe aktarma kapsamı: hücre değeri, formül metni, sayı biçimi, birleşik hücre, temel stil.
  Grafik / pivot / koşullu biçim **v1'de atlanır**, kullanıcı bir uyum raporu görür.
- `.xlsm` yüklenirse VBA atılır ve açıkça bildirilir (K7).
- Dışa aktarma: hesaplanan değerler istemciden gelen anlık görüntüden alınır (K5),
  formüller de yazılır — Excel'de açınca hem değer hem formül görünür.
- **Boyut sınırı paket bazlı:** FREE `SHEET` yok, PRO 200 000 hücre, MAX 500 000.
  (İlk planda MAX için yazılan 1 000 000 hücre, D3'ün sunucusunda POI ile riskli —
  düşürüldü.)
- İçe/dışa aktarma istekleri **tek iş parçacıklı kuyrukta** işlenir; eşzamanlı iki büyük
  dosya sunucuyu düşürmesin.

---

## 11. Yetki ve Paketleme

### İzinler ([Permission.java](backend/src/main/java/com/scrumtools/entity/enums/Permission.java))

```
COLLAB_READ            # dokümanı görüntüle
COLLAB_WRITE           # düzenle
COLLAB_MANAGE          # oluştur, arşivle, Docs'a bağla
COLLAB_RUN_MACRO       # onaylı makro çalıştır
COLLAB_MANAGE_MACRO    # makro yaz ve onayla
```

`doc_page_id` dolu dokümanlarda **`DocPermissionService` kazanır** — çift yetki
kaynağı olmaz (Y1).

### Paket özellikleri ([PlanFeature.java](backend/src/main/java/com/scrumtools/entity/enums/PlanFeature.java))

`CODE_SHARE` **kaldırılır**, yerine:

| Yeni özellik | FREE | PRO | MAX |
|---|---|---|---|
| `COLLAB_WORKSPACE` (metin + kod, eş zamanlı) | ✅ 3 doküman, 3 eşzamanlı kullanıcı | ✅ sınırsız | ✅ |
| `COLLAB_SHEET` | ❌ | ✅ 200k hücre | ✅ 500k hücre |
| `COLLAB_MACRO` | ❌ | ✅ yalnızca manuel | ✅ zamanlanmış + webhook |

Eşzamanlı kullanıcı kotası D3 altında ayrıca bir **koruma mekanizması**dır: FREE
organizasyonların sunucuyu doldurmasını engeller.

> ⚠️ Yeni `PlanFeature` eklerken kontrol listesi: enum + `PlanService` seed +
> `DataInitializer` geriye dolum + **iki arayüz etiket sözlüğü**
> ([PlanManager.vue:123](frontend/src/components/admin/PlanManager.vue#L123) ve
> [BillingTab.vue:152](frontend/src/components/billing/BillingTab.vue#L152)).
> Biri eksik kalırsa kullanıcı "paketinizde yok" hatası alır.
> `CODE_SHARE` **silinirken** ayrıca K10'daki native SQL sırası uygulanmalı.

---

## 12. Kaynak Bütçesi (D3)

Sunucu tek örnek, Redis yok, kaynak dar. Mimarinin buna verdiği cevap:

| Yük kalemi | Nerede harcanır | Sunucu maliyeti |
|---|---|---|
| CRDT birleştirme | İstemci (K1) | **Sıfır** |
| Formül hesaplama | İstemci (K5) | **Sıfır** |
| Izgara çizimi | İstemci (Univer) | **Sıfır** |
| Etkileşimli makro | İstemci Worker (K8) | **Sıfır** |
| Delta rölesi | Sunucu | Bağlantı başına birkaç KB tampon |
| Append yazma | Postgres | Asıl kalem — aşağıya bakın |
| Excel G/Ç | Sunucu | Pik kalem — streaming + kuyrukla sınırlanır (§10) |

**Append yazma yükünü düşüren üç ayar (uygulanması zorunlu):**

1. **İstemci tarafı toplama:** Yjs güncellemeleri 200 ms'lik pencerede biriktirilip tek
   pakette gönderilir. Tuş başına paket gönderilmez.
2. **Sunucu tarafı gruplama:** aynı dokümana gelen güncellemeler 1 sn'lik pencerede
   birleştirilip **tek `INSERT`** olur. Röle bundan bağımsız ve anında yapılır —
   yani gecikme artmaz, yalnızca disk yazımı seyrelir.
3. **Sıkıştırma görevi:** §5'teki eşiklerle `collab_updates` budanır.

**Bellek:** K2 gereği sunucu Y.Doc tutmadığı için RAM açık doküman sayısıyla değil
**eşzamanlı bağlantı sayısıyla** ölçeklenir. 100 eşzamanlı düzenleyici birkaç MB'dir.
Backend container'ına açık bir bellek limiti + `-XX:MaxRAMPercentage=75` konmalı;
POI pikleri OOM Killer'ı tetiklemesin diye limit Postgres/MinIO'yu sıkıştırmayacak
şekilde seçilmeli.

**İzleme:** eşzamanlı collab bağlantısı, saniyedeki güncelleme, `collab_updates` satır
sayısı ve en büyük 10 doküman — basit bir admin ekranı yeterli. Kaynak tükenmeden
görmek gerekir.

---

## 13. Riskler

| # | Konu | Risk | Azaltma |
|---|---|---|---|
| R1 | Univer mutation sözleşmesi (K4) | Sürüm yükseltmede köprü kırılır | `0.25.1` kesin sabitlendi; CRDT Univer'in değil kendi modelimizin biçiminde (Faz 3 karar tablosu), mutation kimlikleri dize değil **dışa aktarılan sabitlerden** okunuyor — kaybolan bir sabit sessiz bozulma değil derleme hatası verir |
| R2 | Docs tohumlama ikilenmesi (Y1) | Sayfa içeriği iki katına çıkar | `state IS NULL` koşullu tek işlem |
| R3 | CRDT büyümesi | Uzun ömürlü doküman diski yer | Sıkıştırma görevi + append gruplama (§12) |
| R4 | Makro yetki yükseltme | Yetkisiz veri erişimi | Onay akışı + çalıştıran yetkisi + denetim (9.2) |
| R5 | ~~Monaco CDN'den yükleniyor~~ ✅ | Kapalı ağ / CDN kesintisinde editör açılmaz; ayrıca `y-monaco` paketi zaten derlemeye soktuğu için **iki ayrı Monaco örneği** oluşuyordu | Faz 2.5'te npm paketine alındı, `loader.config({ monaco })` |
| R6 | POI bellek piki (D3) | Büyük xlsx sunucuyu düşürür | SXSSF + SAX + tek iş parçacıklı kuyruk + hücre kotası |
| R7 | Tek örnek yeniden başlatma (K9) | Deploy'da tüm oturumlar düşer | Append log'dan kurtarma + backoff'lu yeniden bağlanma |
| R8 | Mobil hesap tablosu ✅ | Dokunmatik ızgara zayıf | Faz 3'te uygulandı: `max-width: 767px` altında `SHEET` salt-okunur, banner ile bildiriliyor |

---

## 14. Faz Planı ve İş Listesi

Tahminler adam-gün. Toplam ≈ **64 gün**; Faz 0–2.5 ile kullanılabilir bir ürün çıkar (≈25 gün).

### Faz 0 — Temizlik, altyapı ve güvenlik sertleştirme (7 gün) ✅ *tamamlandı*

- [x] **Code Share'i sil (D1):** §2'deki 8 dosya, `/api/teams/{teamId}/code-shares/**`,
      `/codeShare/:teamId` rotası → `/projects` yönlendirmesi.
      `MonacoEditor.vue` `components/collab/editors/` altına taşındı. *(0.5 g)*
- [x] `CollabCleanupRunner` — `DROP TABLE IF EXISTS code_shares` + K10'daki
      **native SQL `plan_features` güncellemesi** (`@Order(HIGHEST_PRECEDENCE)`),
      ardından enum'dan `CODE_SHARE` çıkarıldı. *(0.5 g)*
- [x] **Abonelik yetkilendirmesi (E3).** `WebSocketSubscriptionAuthorizer` +
      `WebSocketConfig`'te `SUBSCRIBE` süzgeci; retro/poker/quiz/hangman/team
      takım üyeliğine, `/topic/user/**` sahibine, `/topic/collab/**` doküman
      yetkisine bağlandı. *(1.5 g)*
- [x] STOMP mesaj boyutu ve tampon limitleri (512 KB / 1 MB / 20 sn). *(0.5 g)*
- [x] **K3:** `/ws/collab` ham WS uç noktası + handshake'te JWT ve doküman yetkisi +
      `nginx.conf` upgrade/timeout kuralları + NPM notu. *(2 g)*
- [x] **K9:** `app.collab.single-instance` bayrağı, başlangıç uyarısı, health bileşeni,
      `DEPLOYMENT.md` notu. *(0.5 g)*
- [x] **§12 ayarları:** backend container bellek limiti + `MaxRAMPercentage`,
      `CollabUpdateBatcher` append gruplama iskeleti. *(1 g)*
- [x] Yük testi iskeleti: `tools/collab-loadtest/`. *(0.5 g)*

**Faz 0'da verilen üç ek karar:**

| Konu | Karar | Gerekçe |
|---|---|---|
| Yetkisiz SUBSCRIBE | Hata çerçevesi değil, **mesajı düşür** | STOMP'ta ERROR tüm bağlantıyı kapatır; tek hatalı abonelik aynı sekmedeki diğer modülleri de koparırdı. Broker'a ulaşmadığı için sızıntı yine kapalı |
| Geçersiz JWT ile CONNECT | Reddedilmez, kullanıcısız kalır | stomp.js sonsuz yeniden bağlanır; reddetmek 5 sn'lik bir fırtına yaratır. Kullanıcısız oturum hiçbir hedefe abone olamaz |
| `spring-boot-starter-actuator` | Eklendi | K9'un health bileşeni için; yalnızca `health` açık, `diskspace` kapalı, endpoint kimlik doğrulaması arkasında |
| Yetkilendiricinin `WebSocketConfig`'e enjeksiyonu | **`ObjectProvider`** ile geç çözülür | Faz 1'de `CollabDocumentService`, `CollabSessionRegistry`'ye bağlanınca bean döngüsü kapandı ve uygulama açılmadı: `webSocketConfig → …AccessResolver → …DocumentService → …SessionRegistry → SimpMessagingTemplate → webSocketConfig`. Kesim bilerek altyapı ucunda: STOMP ile yayın yapan sekiz servis var ve yetkilendirici uygulamanın yetki ağacını tanımak zorunda, yani bu kenar durdukça ağaca eklenecek her yeni servis döngüyü yeniden kurabilirdi |

**Faz 1'de silinecek Faz 0 yer tutucuları:**
`DenyAllCollabDocumentAccessResolver` (bugün **her** `/ws/collab` bağlantısı 4403
ile kapanır — kasıtlı) ve `NoOpCollabUpdateSink`.

### Faz 1 — Metin ve kod ortak düzenleme (10 gün) ✅ *tamamlandı*

- [x] `CollabDocument`, `CollabUpdate` entity + repository + `CollabDocumentService`. *(1.5 g)*
- [x] `CollabSessionRegistry` — bellekte doküman→bağlantı haritası, yazar seçimi (K6),
      kopan bağlantıda yeniden atama, eşzamanlı kullanıcı kotası. *(1.5 g)*
- [x] `CollabWebSocketHandler` — sync/awareness/kontrol yönlendirmesi, yazma yetkisi
      süzgeci, bağlantıda geri oynatma, append gruplama. *(2 g)*
- [x] `CollabController` REST uçları (§6) + jsoup ile sunucu tarafı HTML sanitize. *(1.5 g)*
- [x] `useCollabDoc.js` + `CollabApi.js` + özel provider (üstel geri çekilmeli). *(1.5 g)*
- [x] `CollabCodeEditor.vue` — Monaco + `y-monaco` + uzaktan imleç dekorasyonları. *(1 g)*
- [x] `CollabTextEditor.vue` — TipTap + `Collaboration` + `CollaborationCaret`. *(1 g)*
- [x] `PresenceBar.vue`, `CollabShell.vue`, `ConnectionBanner.vue`, kaydet göstergesi. *(1 g)*
- [x] `CollabHome.vue` liste ekranı + Navbar girişi. *(1 g)*
- [x] Anlık görüntü + sıkıştırma `@Scheduled` görevi. *(1 g)*
- [x] `PlanFeature.COLLAB_WORKSPACE` — Faz 0'da yapıldı. *(0.5 g)*
- [x] `COLLAB_READ/WRITE/MANAGE` izinleri + rol şablonlarına backfill. *(0.5 g)*

**Faz 1'de verilen ek kararlar:**

| Konu | Karar | Gerekçe |
|---|---|---|
| Wire protokolü | `y-protocols/sync` yerine ham güncelleme + `since` ile geri oynatma | §6'daki nota bakın — sunucunun `Y.Doc` tutmasını gerektiriyordu (K2 ihlali) |
| `GET /state` biçimi | JSON + base64 paket listesi | Sunucu güncellemeleri birleştiremez (K2); tek ikili gövde özel çerçeveleme icat etmeyi gerektirirdi. Doküman başına bir kez işleyen yol, sıcak yol değil |
| Üstveri yazma | Entity `save()` değil hedefli `UPDATE` | Başlık değiştirirken bir başkası yazıyorsa `lastSeq` eski değeriyle geri yazılır, sıra numaraları çakışır |
| `collab_updates` PK | IDENTITY değil SEQUENCE | Identity kolonlar Hibernate'in JDBC toplu eklemesini kapatır; gruplamanın (§12) tek amacı buydu |
| Sıkıştırma anı | Zamanlanmış görev değil, anlık görüntü yazılırken | Tabloyu küçük tutmanın en ucuz anı o an. Zamanlanmış görev yalnızca hiç susmayan doküman ve yarım kalmış budama için |
| FREE eşzamanlı kota | Sekme değil **kişi** bazlı | Sekme bazlı olsaydı FREE kullanıcı ikinci sekmesiyle kendini kilitlerdi |

**Faz 2'ye devredilenler:** `GET /documents/{id}/history` ucu ve `HistoryPanel.vue`
(§6'da listeli, plan Faz 2'de konumlandırıyor). `doc_page_id` ve `link_mode`
sütunları şemada hazır ama akış Faz 2'de yazılacak.

### Faz 2 — Docs entegrasyonu (7 gün) ✅ *tamamlandı*

- [x] `doc_page_id` bağlantısı + `link_mode` + tohumlama kilidi (R2). *(1.5 g)*
- [x] `DocPageService.applyCollaborativeContent` — içerik + `DocPageVersion` yazımı. *(1.5 g)*
- [x] `DocPage.vue`'ye "Ortak Düzenle" + okuma modunda canlı katılımcı rozeti. *(1 g)*
- [x] `DocsLinkDialog.vue` — "Docs'a Kaydet" akışı (Y2). *(1.5 g)*
- [x] Yetki köprüsü: `doc_page_id` doluysa `DocPermissionService`'e devret. *(1 g)*
- [x] `HistoryPanel.vue` + `collab_snapshots` + `/history` uçları. *(1.5 g)*

**Faz 2'de verilen ek kararlar:**

| Konu | Karar | Gerekçe |
|---|---|---|
| Tohumlama kilidi | `seeded_at` sütunu + koşullu `UPDATE ... WHERE seeded_at IS NULL AND last_seq = 0 AND state IS NULL` | Yalnızca "içerik boş mu" bakmak iki istemci arasında yarış penceresi bırakırdı (R2) |
| Tohumlamayı kim yapar | Hakkı sunucu verir, dönüşümü istemci yapar | Sunucuda Yjs yok (K2); HTML→Y.Doc çevirisi ancak tarayıcıda mümkün |
| Dönüşüm yolu | `prosemirrorToYDoc` değil, `editor.commands.setContent` | Collaboration eklentisi her transaction'ı zaten Y.Doc'a yazıyor — içerik normal yazma yolundan girer, ayrı bir şema uyum riski doğmaz |
| "Ortak Düzenle" yetkisi | `COLLAB_MANAGE` değil **Docs yazma** | Sayfayı düzenleyebilen herkes ortak düzenlemeye de açabilmeli; aksi hâlde özellik yalnızca proje yöneticilerinin olurdu |
| Geçmiş kaydı | `collab_snapshots`, yalnızca **okunabilir metin** | Her durağa sıkıştırılmış Y.Doc durumu koymak dar sunucunun diskini geçmiş uğruna doldurur; metin geri yükleme için yeterli |
| Geçmiş seyreltme | En fazla 10 dk'da bir, doküman başına 20 kayıt | Anlık görüntü dakikada birkaç kez üretilir; hepsini kaydetmek zaman çizelgesini okunamaz yapar |
| Geri yükleme | Geri sarma değil, **yeni düzenleme** olarak uygulama | CRDT'de tek taraflı geri sarma, o an bağlı olanların durumuyla çelişir |
| Aynalama hatası | Yutulur ve log'lanır, anlık görüntü geri alınmaz | Docs yazımı türev çıktıdır; doğruluk kaynağı CRDT'dir. Hata yüzünden anlık görüntüyü geri almak dokümanı kaydedilmemiş bırakırdı |

### Faz 2.5 — Derleme borcu: kilit dosyası ve Monaco (1 gün) ✅ *tamamlandı*

Plana sonradan eklendi. Faz 1'in ilk Jenkins koşusu `npm ci` aşamasında düştü;
kök sebebi araştırırken R5'in sanılandan büyük olduğu ortaya çıktı.

- [x] `package-lock.json` yeniden üretildi — Faz 1'de eklenen 6 paket kilitte yoktu,
      `npm ci` kilitten birebir kurduğu için CI ERESOLVE ile düşüyordu. *(0.5 g)*
- [x] TipTap ortak düzenleme eklentileri **sabit** `3.23.4`'e çekildi; TipTap akran
      sürümlerini birebir istiyor (`peer @tiptap/pm@"3.23.4"`), aralık vermek npm'e
      olmayan bir esneklik vaat ediyordu. *(0.5 g)*
- [x] `y-prosemirror` kaldırıldı, yerine `@tiptap/y-tiptap` — `extension-collaboration`
      v3'ün gerçek akran bağımlılığı bu; y-prosemirror kodda hiç import edilmiyordu.
- [x] **R5 kapatıldı:** `monaco-editor` npm bağımlılığı + `src/monaco/setup.js`
      (worker'lar + `loader.config({ monaco })`), CDN bırakıldı.

**Faz 2.5'te verilen ek kararlar:**

| Konu | Karar | Gerekçe |
|---|---|---|
| Monaco kaynağı | CDN değil **npm paketi** | `y-monaco` zaten `editor.api.js`'i statik import ediyor; CDN kalsaydı iki ayrı Monaco örneği (0.52.2 / 0.53.0) yan yana çalışır, y-monaco'nun `Range`/`Selection` nesneleri yabancı bir editöre giderdi |
| Dil worker'ları | Beşi de kayıtlı (json/css/html/ts/editor) | Vite her birini ayrı varlık olarak üretiyor, ancak o dilde model açılınca iniyor — ilk yükleme maliyeti yok, CDN'deki davranış korunuyor |
| `monaco-editor` sürümü | `^` yok, sabit `0.53.0` | `esm/vs/...` altındaki worker giriş noktaları paketin iç yapısı; ara sürümde yer değiştirirse derleme sessizce bozulur |
| Monaco'nun ana pakete girmemesi | Tembel rota ile sınırlandı | Tek tüketicisi `MonacoEditor.vue` → `CollabDocument` rotası; rota `import()` ile yüklendiği için Monaco ayrı bir async parçaya düşüyor, Docs/Dashboard bedelini ödemiyor |

### Faz 3 — Hesap tablosu (18 gün) ✅ *tamamlandı*

- [x] Univer OSS entegrasyonu, Vue sarmalayıcı, **kesin sürüm sabitleme** (0.25.1). *(2 g)*
- [x] `SHEET` Yjs şeması (§5) + `UniverYjsBridge.js` iki yönlü köprü.
      **Planın en riskli tek işi.** *(6 g)*
- [x] Formül: yerel yeniden hesaplama, bağımlılık grafiği, döngü tespiti (K5) —
      Univer'in OSS formül motoru sağlıyor; bize düşen CRDT'ye **formül metnini**
      yazmak, sonucu değil. *(2 g)*
- [x] Hücre düzeyi imleç/seçim paylaşımı (awareness). *(1.5 g)*
- [x] Sayfa (worksheet) ekle/sil/yeniden adlandır + sıralama. *(1 g)*
- [x] Apache POI **SAX** ile `.xlsx` / `.csv` içe aktarma + uyum raporu + kuyruk. *(2.5 g)*
- [x] **SXSSF** ile `.xlsx` / `.csv` dışa aktarma. *(1.5 g)*
- [x] `PlanFeature.COLLAB_SHEET` + hücre kotaları (§10). *(0.5 g)*
- [x] Mobilde salt-okunur düşürme (R8). *(1 g)*

**Faz 3'te verilen ek kararlar:**

| Konu | Karar | Gerekçe |
|---|---|---|
| CRDT'de ne durur | Univer mutation akışı değil, **kendi hücre modelimiz** | Ham mutation kaydetmek doğruluk kaynağını Univer'in iç sözleşmesine bağlar; bir sürüm yükseltmesi tüm geçmişi okunamaz yapardı (R1) |
| Hücre gösterimi | İç içe `Y.Map` değil **düz nesne** | §5'in taslağı alan bazlı birleşme kazandırırdı ama 200 bin hücre 200 bin CRDT tipi demek — tarayıcı belleği dayanmaz. Karşılığı hücre düzeyinde "son yazan kazanır"; hesap tablosunda beklenen davranış zaten bu |
| Kaplar | `meta` altında iç içe değil, **üst düzey** `Y` tipleri | İç içe tip oluşturmak CRDT'ye yazmaktır. Dokümanı yalnızca açan istemci bile boş kapları yazar, `last_seq` artar ve sunucudaki tohumlama kilidi (`last_seq = 0`) bir daha açılmaz — içe aktarılan tablo hiç yüklenmezdi |
| Değişiklik yolu | Hücreler **mutation**, sayfa yapısı **Facade** | Hücre yazımı saniyede onlarca kez olur, mutation en ucuz yol. Sayfa ekleme nadirdir ve Univer'in iç anlık görüntü biçimini elle üretmek yerine belgelenmiş `create()` kullanılır |
| Uzak değişiklik uygulama | `syncExecuteCommand` + `onlyLocal`, komut değil **mutation** | Komut çalıştırmak geri alma yığınına girerdi: Ctrl+Z, başkasının yazdığı hücreyi silerdi |
| Stiller | Havuz değil **satır içi** | Havuz, iki tarafta da eşitlenmesi gereken ikinci bir durum demekti. Sunucu sözleşmesindeki `s` anahtarına çevirme yalnızca anlık görüntü üretilirken yapılıyor |
| Univer kurulumu | `@univerjs/presets` yerine `preset-sheets-core` + 30 satırlık kendi bootstrap'ımız | Meta paket 22 preset'i bağımlılık olarak çeker; dar Jenkins ajanına (D3) hepsini kurmanın karşılığı yok. Kullanılan her şey belgelenmiş genel API |
| Hücre kotası | Canlı düzenlemede değil, **içe/dışa aktarmada** | Sunucunun her anlık görüntüde 500 bin hücrelik JSON'u ayrıştırıp sayması K2'ye aykırı. Bağlayıcı denetim, hücrelerin zaten sunucuda sayıldığı tek an olan Excel G/Ç'de |
| İçe aktarma tohumlaması | Yeni bir yol değil, **Faz 2'nin `claimSeed` kilidi** | Sunucuda Yjs yok (K2); POI'nin ürettiği model `snapshot_text`'te bekler, ilk açan istemci yazar. İki tohumlama mekanizması yerine tek mekanizma |
| Uzak imleç | `IMarkSelectionService`, DI'dan, `try/catch` ile | Univer'in iç API'si. Bir yükseltmede kaybolursa uzak imleçler kaybolur ama **düzenleme çalışmaya devam eder** — kozmetik özellik, kritik yola bağlanmamalı |
| Excel stil kapsamı | Yalnızca **sayı biçimi** | Her stil varyantını almak hücre sayısı kadar stil üretebilir; kullanıcı neyin alınmadığını uyum raporunda görüyor |

### Faz 4 — Makro motoru (14 gün) ✅ *tamamlandı*

- [x] `CollabMacro`, `CollabMacroRun` entity + servis + REST. *(1.5 g)*
- [x] `macroWorker.js` sandbox: worker kurulumu, global temizliği, zaman aşımı. *(2 g)*
- [x] Doküman API yüzeyi (§9.1) — oku-anlık-görüntü / yaz-toplu modeli (K8). *(3 g)*
- [x] `ScrumTools.tasks` / `.sprints` / `.docs` köprüleri (TQL motoru üzerinden). *(2 g)*
- [x] Onay akışı + rıza diyaloğu + statik API taraması (9.2). *(2 g)*
- [x] `MacroEditor.vue` — Monaco + `d.ts` tamamlama + hata paneli. *(1.5 g)*
- [x] `MacroPanel.vue` + çalıştırma günlüğü + `ActivityService` kaydı. *(1 g)*
- [x] `PlanFeature.COLLAB_MACRO` + kaynak limitleri. *(0.5 g)*

**Faz 4'te verilen ek kararlar:**

| Konu | Karar | Gerekçe |
|---|---|---|
| Veri API'leri | §9.1'in taslağı senkron gösteriyordu; **`Promise` döndürüyorlar** | K8 eşzamanlı RPC'yi zaten reddediyor — o yol `SharedArrayBuffer` + COOP/COEP demek, yani tüm sayfayı izole moda sokmak. `await` yazmak ucuz bir bedel |
| `ScrumTools.tasks/sprints/docs` | Makroya özel sunucu ucu **açılmadı**; ana iş parçacığı mevcut REST uçlarını **kullanıcının kendi oturumuyla** çağırıyor | "Makro çalıştıranın yetkisiyle koşar" (§9.2) böylece kuralla değil **yapıyla** garanti oluyor. İkinci bir veri yolu, ikinci bir yetki kontrolü ve zamanla ayrışacak iki doğruluk kaynağı demekti |
| Statik API taraması | Güvenlik sınırı değil, **rıza metninin kaynağı** | Regex taraması `ScrumTools['ta'+'sks']` yazımını kaçırır. Kaçırması yetki kazandırmıyor çünkü asıl sınır yukarıdaki satırda; taramanın işi kullanıcıya dürüst bir özet göstermek |
| Onay | Kaynağa değil kaynağın **SHA-256'sına** verilir | Aksi hâlde "onaylat, sonra değiştir", incelenmemiş kodu onaylı göstermenin en kolay yolu olurdu |
| Yazarın istisnası | Onaysız makroyu **yalnızca yazarı** çalıştırabilir; otomatik tetikleyicide bu istisna **yok** | `ON_OPEN`, dokümanı açan herkesin yetkisiyle sessizce koşar — orada "yazar zaten biliyor" argümanı geçersiz |
| Zaman aşımı | Sayaç worker'da değil **host'ta**, `terminate()` ile | Sonsuz döngüye girmiş bir worker kendi sayacını çalıştıramaz |
| Hata sonrası işlemler | Uygulanmaz, **tümü atılır** | Yarım çalışmış makronun dokümanı tutarsız bırakması, hiç çalışmamasından kötüdür |
| Makro yazımının origin'i | `LOCAL_ORIGIN` değil `'macro'` | Kendi Yjs gözlemcilerimiz de tetiklensin: aksi hâlde veri CRDT'ye girer, ağdaki herkes görür, ama makroyu çalıştıranın ekranı güncellenmezdi |
| Aktivite akışı | Yalnızca **yazan** ve başarılı çalıştırmalar düşer | Salt okuyan bir makronun her koşusunu akışa koymak, gerçek değişiklikleri görünmez yapardı |
| `ScrumTools.http` | Sunucu vekili; allowlist **boş = kapalı** | Yanlışlıkla açık kalmış bir vekil, sunucuyu isteğe bağlı bir istek üretecine çevirir. Yönlendirme takip edilmiyor — allowlist yalnızca ilk adresi doğrular, 302 zinciri o doğrulamayı anlamsız kılardı |
| Kaydedici (§9.3) | Faz 4'te **yapılmadı**, Faz 5'e alındı ve tahmini 0.5 g → 2 g'ye çıktı | Faz 4 iş listesinde yer almıyordu. Ayrıca "köprü zaten komut akışını dinliyor, ince bir katman" varsayımı yanlış çıktı: köprü bilerek **mutation** dinliyor, kaydedicinin ihtiyacı olan **command** seviyesi ise ayrı bir filtre, ayrı bir eşleme tablosu ve geri-alma davranışı demek (§9.3) |

### Faz 5 — Genişletme (7 gün)

- [ ] Docs içine canlı gömme (Y3): TipTap özel düğüm + salt-okunur render +
      DOMPurify izni. *(2 g)*
- [ ] Sunucu tarafı makro: GraalJS + bekçi iş parçacığı + **tek iş parçacıklı kuyruk**
      + 10 sn zaman aşımı (K8 D3 notu). *(2 g)*
- [ ] **Makro kaydedici (§9.3)** — kayıt deposu: `CommandType.COMMAND` filtresi,
      `applyingRemote` / `'macro'` origin dışlaması, geri-alma ile son adımı silme. *(0.5 g)*
- [ ] **Makro kaydedici** — komut → API eşleme tablosu ve betik üretimi;
      eşlemesi olmayan komut için `// kaydedilemedi:` yorumu. *(1 g)*
- [ ] **Makro kaydedici** — kayıt göstergesi, durdurunca düşen işlem raporu,
      üretilen kaynağın `MacroEditor`'a devri. *(0.5 g)*
- [ ] Webhook tetikleyicisi + imzalı uç nokta. *(0.5 g)*
- [ ] Collab kaynak izleme ekranı (§12). *(0.5 g)*

---

## 15. Kabul Kriterleri

**Faz 1**
- İki tarayıcı aynı kod dokümanında yazarken her iki taraf 300 ms içinde diğerinin
  değişikliğini ve imlecini görür.
- Bir istemci ağı kesip 2 dk çevrimdışı düzenler, geri bağlanınca **hiçbir karakter
  kaybolmadan** birleşir.
- Backend yeniden başlatılır; istemciler 10 sn içinde kendiliğinden yeniden bağlanır
  ve **hiçbir değişiklik kaybolmaz** (K9 / R7).
- Yetkisi olmayan kullanıcının `/ws/collab` bağlantısı `4403` ile reddedilir.
- 20 eşzamanlı düzenleyicide backend RAM artışı 100 MB'ın altında kalır (§12).

**Faz 2**
- Docs sayfası ortak düzenlendikten sonra `DocPageVersion` listesinde yeni sürüm
  görünür ve geri yükleme çalışır.
- Aynı sayfa iki kez "Ortak Düzenle" ile açıldığında içerik **ikilenmez** (R2).
- Yazma yetkisi olmayan kullanıcı salt-okunur katılır; gönderdiği güncelleme sunucuda
  reddedilir.

**Faz 3**
- 50 000 hücrelik tabloda kaydırma 60 fps altına düşmez.
- Aynı satırda iki kullanıcı farklı hücrelere yazınca ikisi de korunur.
- `.xlsx` içe → düzenle → dışa aktar turunda değer, formül ve sayı biçimi korunur.
- 200 000 hücrelik dışa aktarma sırasında backend heap kullanımı 512 MB'ı aşmaz (R6).

**Faz 4**
- Sonsuz döngü içeren makro 5 sn'de sonlandırılır, sekme donmaz, `TIMEOUT` kaydı düşer.
- Onaysız `ON_OPEN` makrosu **çalışmaz**.
- Makro kaynağı değiştirilince onay düşer ve tekrar onay istenir.

**Faz 5**
- Kayıt açıkken yapılan "değer gir → kalın yap → satır ekle" dizisi, çalıştırıldığında
  aynı sonucu veren okunabilir bir betik üretir.
- Kayıt sırasında **başka bir kullanıcının** aynı tabloya yazdığı hücreler üretilen
  betikte **yer almaz**.
- Eşlemesi olmayan bir işlem (birleştirme) yapıldığında betikte `// kaydedilemedi:`
  yorumu görünür ve kayıt bitiminde düşen işlem sayısı bildirilir.
- Kaydedilen makro onaysız oluşturulur; başka bir kullanıcı çalıştırmayı deneyince
  reddedilir.

---

## 16. Özet Karar Tablosu

| Konu | Karar | Gerekçe |
|---|---|---|
| Eski modül | Komple silinir, göç yok (D1) | Temiz model; `tag` çakışması sorunu da gider |
| Senkron algoritması | CRDT / Yjs | Sunucuda dönüşüm mantığı gerekmez; yük istemcide |
| Sunucu rolü | Röle + kalıcılaştırıcı, Y.Doc tutmaz | Dar sunucuda RAM'i bağlantı sayısına bağlar |
| Taşıma | Ayrı ham WS `/ws/collab` (K3-B) | %33 base64 vergisi ve CPU'su kalıcı olurdu |
| Metin editörü | TipTap (mevcut) + y-prosemirror | Docs ile aynı içerik biçimi (HTML) |
| Kod editörü | Monaco (mevcut) + y-monaco | Sıfır yeni bağımlılık |
| Tablo motoru | Univer OSS + kendi senkronumuz (D2) | Pro lisansı alınmıyor; ızgara yine de bedava |
| Formül sonucu | CRDT'de saklanmaz, türetilir | Ağ ve disk yazma patlamasını önler |
| Makro dili | JavaScript (Apps Script modeli) | VBA yorumlayıcısı ayrı bir ürün olurdu |
| Makro kaydedici | Komut seviyesini dinler, köprünün dinlediği mutation'ı değil (§9.3) | Mutation dönüşüm sonrası ve parçalıdır; ondan üretilen betik okunamaz. Oysa kaydedicinin bütün değeri **düzenlenebilir** çıktı vermesi |
| Excel G/Ç | Sunucuda Apache POI, SXSSF + SAX | Univer Pro kısıtını aşar, bellek piki kontrollü |
| Ölçek | Tek örnek, Redis yok (D3) | Sunucu kaldırmaz; append log restart'ı güvenli kılar |
| Kapsam | Proje birincil, takım etiket (D4) | Docs ile aynı hiza, tek yetki ağacı |
| Docs ilişkisi | Aynalama + gömme, izin Docs'tan | Çift yetki kaynağı olmaz |
