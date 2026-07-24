# Jenkins (CI/CD) Entegrasyon Planı

> Work modülü içinden task'ların test ortamına deploy edilmesi ve release süreçlerinin
> Jenkins pipeline'ları üzerinden yürütülmesi. Kurgu, mevcut Git/SCM entegrasyonunun
> (org seviyesi bağlantı → proje seviyesi eşleme → task/release üzerinde aksiyon)
> deseninin birebir devamıdır.

---

## 1. Amaç ve Kapsam

**Hedefler:**
- Task detayından tek tıkla "test ortamına deploy" — task'a bağlı branch parametresiyle Jenkins job'ı tetikleme.
- Release ekranından release pipeline'ı çalıştırma — sürüm yaşam döngüsüne (APPROVED → RELEASED) bağlı, yetki kontrollü.
- Build durumunun (kuyrukta / çalışıyor / başarılı / başarısız) uygulama içinden izlenmesi; task ve release üzerinde build tarihçesi.

**Kapsam dışı (ilk sürümde):**
- Jenkins job/pipeline tanımlarını uygulama içinden oluşturmak/düzenlemek (job'lar Jenkins tarafında hazır kabul edilir).
- Console log streaming (sadece Jenkins'e derin link verilir).
- Jenkins dışı CI sağlayıcıları (GitLab CI, GitHub Actions) — veri modeli buna hazır tasarlanır ama implementasyon yapılmaz.

---

## 2. Yaslandığımız Mevcut Altyapı

| Mevcut yapı | Jenkins entegrasyonunda rolü |
|---|---|
| `ScmConnection` (org seviyesi, AES-GCM şifreli token, `ScmTokenCrypto`) | `CiConnection` aynı desenle kurulur, aynı crypto sınıfı yeniden kullanılır |
| `ScmRepository` (proje ↔ repo eşlemesi) | `CiJobMapping` (proje ↔ Jenkins job eşlemesi) aynı desen |
| `ScmBranch` / `ScmTaskDevService` (task dev paneli) | Deploy tetiklerken branch seçimi buradan beslenir |
| `Release` + `ReleaseStatus` yaşam döngüsü | Release pipeline tetikleme kapıları (APPROVED'da açılır) |
| `EntitlementService.assertFeature` + `PlanFeature` | `CI_CD_INTEGRATION` feature'ı ile paket bazlı gating |
| `NotificationService` | Build sonucu bildirimleri |
| ddl-auto: update | Yeni entity'ler için ayrıca migration gerekmez |

---

## 3. Mimari Kararlar (İstişare Sonuçları)

### K1 — Jenkins'e özel değil, CI sağlayıcı soyutlaması
`ScmProvider` deseninde `CiProvider` enum'u (`JENKINS` ile başlar). Entity/servis adları `Ci*` ön ekiyle
generic tutulur; Jenkins'e özgü mantık `JenkinsClient` içinde izole edilir. İleride GitLab CI /
GitHub Actions eklemek sadece yeni bir `CiClient` implementasyonu demek olur.

### K2 — Build durumu takibi: önce poller, webhook sonra
SaaS modelinde müşterilerin kendi Jenkins'lerine plugin kurmasını şart koşmak sürtünme yaratır.
Bu yüzden:
- **Faz 1:** Aktif build'ler (QUEUED/RUNNING) için zamanlanmış poller (`@Scheduled`, 15–30 sn aralık,
  yalnız açık build'i olan bağlantılar sorgulanır).
- **Sonraki faz (opsiyonel):** `/api/ci/webhook/{connectionId}` ucu — Jenkinsfile'ın `post` bloğundan
  basit bir `curl` ile bildirim (plugin gerektirmez, `webhookSecret` imzalı). Webhook gelirse poller o
  build'i atlar; webhook hiç kurulmasa da sistem çalışır.

### K3 — Job eşleme proje seviyesinde ve tipli
Bir projeye birden çok job eşlenebilir; her eşlemenin **tipi** (`TASK_DEPLOY` / `RELEASE_PIPELINE`)
ve **ortamı** (`TEST` / `STAGING` / `PROD`) vardır. Böylece task panelinde sadece TASK_DEPLOY
job'ları, release ekranında sadece RELEASE_PIPELINE job'ları listelenir. Aynı projede birden fazla
test ortamı job'ı olabilir (ör. "test-1'e deploy", "test-2'ye deploy").

### K4 — Parametre şablonu
Her eşlemede JSON parametre şablonu tutulur; değerler tetikleme anında bağlamdan çözülür:

```json
{ "BRANCH": "{{branch}}", "TASK_KEY": "{{taskKey}}", "VERSION": "{{releaseName}}", "ENV": "test" }
```

Desteklenen değişkenler: `{{branch}}`, `{{taskKey}}`, `{{taskTitle}}`, `{{releaseName}}`,
`{{projectKey}}`, `{{triggeredBy}}`. Sabit değerler olduğu gibi geçer. Böylece müşterinin mevcut
job parametre adlarına uyum sağlanır, job tarafında değişiklik istenmez.

### K5 — Release pipeline'ı statü kapılı
- RELEASE_PIPELINE tetikleme yalnız `APPROVED` statüsündeki release'lerde ve yalnız
  release manager / org admin tarafından yapılabilir (mevcut release yetki modeliyle aynı).
- Eşlemede `autoTransitionOnSuccess` bayrağı: açıksa build SUCCESS olduğunda release otomatik
  `RELEASED`'a geçer (mevcut geçiş servisi çağrılır, `ReleaseDeployment` snapshot'ları yazılır);
  kapalıysa kullanıcı build sonucunu görüp geçişi elle yapar. **Varsayılan: kapalı** (temkinli).

### K6 — Yetki modeli
| Aksiyon | Yetki |
|---|---|
| CiConnection CRUD + test | ORG_OWNER / ORG_ADMIN |
| CiJobMapping CRUD | ORG_OWNER / ORG_ADMIN (SCM repo eşlemesiyle aynı) |
| TASK_DEPLOY tetikleme | Proje üyesi (task'ı görebilen herkes) |
| RELEASE_PIPELINE tetikleme | Release manager veya org admin |
| Build tarihçesi görüntüleme | Proje üyesi |

### K7 — Plan gating
`PlanFeature.CI_CD_INTEGRATION` eklenir; PRO/MAX paketlerde açık, FREE'de kapalı
(GIT_INTEGRATION ile aynı muamele). Admin panelindeki plan feature yönetimine otomatik düşer.

---

## 4. Veri Modeli

### 4.1 Yeni enum'lar (`entity/enums`)

```
CiProvider        → JENKINS
CiConnectionStatus→ ACTIVE, INVALID, DISABLED          (ScmConnectionStatus ile aynı yapı)
CiJobType         → TASK_DEPLOY, RELEASE_PIPELINE
CiEnvironment     → TEST, STAGING, PROD
CiBuildStatus     → QUEUED, RUNNING, SUCCESS, FAILURE, UNSTABLE, ABORTED, LOST
CiBuildContext    → TASK, RELEASE, MANUAL
```

`LOST`: kuyruğa girdi ama makul sürede build numarası alınamadı / poller artık bulamıyor
(temizlik job'ı QUEUED'da 30 dk'yı aşanları LOST'a çeker).

### 4.2 `CiConnection` — org seviyesi Jenkins bağlantısı

| Alan | Açıklama |
|---|---|
| id (UUID), organization FK | ScmConnection ile aynı |
| provider (CiProvider) | JENKINS |
| name | "Şirket Jenkins'i" |
| baseUrl | `https://jenkins.firma.com` (zorunlu — Jenkins'te cloud varsayılanı yok) |
| username | Jenkins kullanıcı adı (API token'la birlikte Basic auth) |
| encryptedToken | Jenkins API token — AES-GCM, `ScmTokenCrypto` ile |
| tokenHint | ****abcd |
| webhookSecret | İleride webhook imzası için şimdiden üretilir |
| crumbEnabled (boolean) | CSRF crumb gereksinimi; test sırasında otomatik tespit edilip yazılır |
| status, createdBy, lastValidatedAt, createdAt, updatedAt | ScmConnection ile aynı |

### 4.3 `CiJobMapping` — proje ↔ Jenkins job eşlemesi

| Alan | Açıklama |
|---|---|
| id, connection FK, project FK | |
| jobFullName | Folder dahil tam yol: `scrumtools/deploy-test` |
| displayName | UI'da görünen ad: "Test ortamına deploy" |
| jobType (CiJobType) | TASK_DEPLOY / RELEASE_PIPELINE |
| environment (CiEnvironment) | TEST / STAGING / PROD |
| parameterTemplate (TEXT, JSON) | K4'teki şablon |
| autoTransitionOnSuccess (boolean) | Yalnız RELEASE_PIPELINE için anlamlı, varsayılan false |
| enabled (boolean) | Eşlemeyi silmeden devre dışı bırakma |
| unique (connection_id, project_id, jobFullName) | Aynı job aynı projeye iki kez eşlenemez |

### 4.4 `CiBuild` — tetiklenen her build'in kaydı

| Alan | Açıklama |
|---|---|
| id, jobMapping FK | |
| contextType (CiBuildContext) | TASK / RELEASE / MANUAL |
| taskId (UUID, düz kolon, nullable) | ReleaseDeployment'taki gibi FK değil — task silinse de tarihçe kalır |
| taskCustomId, releaseId, releaseName | Denormalize snapshot alanları |
| queueItemUrl | `buildWithParameters` yanıtındaki Location — build no çözümlenene kadar |
| buildNumber (nullable) | Kuyruktan çözülünce dolar |
| buildUrl | Jenkins'e derin link |
| status (CiBuildStatus) | Poller/webhook günceller |
| parametersJson | Gönderilen çözümlenmiş parametreler (denetim izi) |
| branch | Deploy edilen branch (varsa) |
| triggeredBy (email), triggeredAt, startedAt, finishedAt, durationMs | |

İndeksler: `(job_mapping_id)`, `(task_id)`, `(release_id)`, `(status)` — poller `status IN (QUEUED, RUNNING)` tarar.

---

## 5. Backend Kurgusu

### 5.1 Paket yapısı — `com.scrumtools.service.ci`

```
service/ci/
  CiConnectionService.java      // CRUD + test + yetki (ScmConnectionService deseni)
  CiJobMappingService.java      // eşleme CRUD + job keşfi
  CiBuildService.java           // tetikleme, parametre çözümleme, tarihçe sorguları
  CiBuildPoller.java            // @Scheduled durum senkronizasyonu
  CiParameterResolver.java      // {{...}} şablon değişkenlerini bağlamdan çözer
  client/
    CiClient.java               // arayüz (ScmClient deseni)
    CiClientFactory.java
    JenkinsClient.java          // REST implementasyonu
    CiJobInfo.java, CiBuildInfo.java, CiQueueItem.java, CiApiException.java
```

### 5.2 `JenkinsClient` — Jenkins REST API detayları

- **Auth:** Basic auth (`username:apiToken`). API token kullanıldığında crumb çoğu kurulumda
  gerekmez; yine de `crumbEnabled` ise `GET /crumbIssuer/api/json` ile crumb alınıp header eklenir.
- **Bağlantı testi:** `GET {base}/api/json?tree=nodeName` → 200 = OK. Yanıt versiyon header'ı
  (`X-Jenkins`) `lastValidatedAt` ile birlikte saklanır.
- **Job keşfi:** `GET {base}/api/json?tree=jobs[name,fullName,url,jobs[...]]` — folder'lar
  recursive açılır (eşleme ekranındaki arama/dropdown için). Derinlik 3 ile sınırlanır.
- **Job parametreleri:** `GET {base}/job/{path}/api/json?tree=property[parameterDefinitions[name,type,defaultParameterValue[value]]]`
  — eşleme ekranında şablonu otomatik doldurmak için.
- **Tetikleme:** `POST {base}/job/{path}/buildWithParameters` (parametresiz job'da `/build`).
  Yanıtın `Location` header'ı queue item URL'idir → `CiBuild.queueItemUrl`.
- **Kuyruk çözümü:** `GET {queueItemUrl}api/json` → `executable.number` + `executable.url`
  dolunca `buildNumber`/`buildUrl` yazılır, status RUNNING olur.
- **Build durumu:** `GET {buildUrl}api/json?tree=building,result,timestamp,duration` →
  `building=false` olunca `result` (SUCCESS/FAILURE/UNSTABLE/ABORTED) map edilir.

### 5.3 Poller stratejisi (`CiBuildPoller`)

- `@Scheduled(fixedDelay = 20_000)`; yalnız `status IN (QUEUED, RUNNING)` build'ler,
  bağlantı bazında gruplanarak sorgulanır (bağlantı başına tek client örneği).
- QUEUED → kuyruk çözümü; RUNNING → build durumu. Terminal duruma geçince:
  - `finishedAt`, `durationMs` yazılır,
  - task/release bağlamına `NotificationService` ile bildirim gönderilir,
  - RELEASE_PIPELINE + SUCCESS + `autoTransitionOnSuccess` ise release `RELEASED`'a geçirilir.
- 30 dk'dan eski QUEUED kayıtlar `LOST`'a çekilir; ardışık bağlantı hatalarında (5xx/timeout ×5)
  bağlantı `INVALID` işaretlenip org adminlerine bildirim düşülür, poller o bağlantıyı atlar.

### 5.4 REST API uçları

```
# Bağlantı yönetimi (ORG_OWNER/ADMIN) — ScmConnectionController deseni
GET/POST            /api/organizations/{orgId}/ci/connections
PUT/DELETE          /api/organizations/{orgId}/ci/connections/{connId}
POST                /api/organizations/{orgId}/ci/connections/{connId}/test
GET                 /api/organizations/{orgId}/ci/connections/{connId}/jobs?search=   // keşif
GET                 /api/organizations/{orgId}/ci/connections/{connId}/jobs/params?jobFullName=

# Job eşleme (ORG_OWNER/ADMIN)
GET/POST            /api/projects/{projectId}/ci/job-mappings
PUT/DELETE          /api/projects/{projectId}/ci/job-mappings/{mappingId}

# Tetikleme + tarihçe (proje üyesi / release yetkisi)
POST                /api/tasks/{taskId}/ci/deploy        { mappingId, branch, overrides? }
GET                 /api/tasks/{taskId}/ci/builds
POST                /api/releases/{releaseId}/ci/run     { mappingId, overrides? }
GET                 /api/releases/{releaseId}/ci/builds
GET                 /api/projects/{projectId}/ci/builds?status=&page=   // proje geneli tarihçe
POST                /api/ci/builds/{buildId}/refresh     // manuel durum yenileme
```

DTO'lar `dto/` altına: `CiConnectionRequest/Response`, `CiConnectionTestResponse`,
`CiJobMappingRequest/Response`, `CiJobInfoResponse`, `CiTriggerRequest`, `CiBuildResponse`.
Token hiçbir yanıtta yer almaz (tokenHint gösterilir) — SCM ile aynı kural.

### 5.5 Güvenlik ve gating

- Tüm servis metodlarında org üyeliği/rol kontrolü (mevcut SCM servislerindeki desenle).
- `EntitlementService.assertFeature(orgId, PlanFeature.CI_CD_INTEGRATION)` — bağlantı
  oluşturma ve build tetikleme uçlarında.
- `baseUrl` için `ScmUrlValidator` yeniden kullanılır (SSRF koruması: iç ağ IP engeli vb.).
- Rate limit: aynı task/release için dakikada en çok 2 tetikleme (çift tık koruması).

---

## 6. Frontend Kurgusu

### 6.1 Yeni API modülü — `frontend/src/api/CiApi.js`
Bölüm 5.4'teki uçların birebir karşılığı (`ScmApi.js` deseni).

### 6.2 Org ayarları — CI/CD bağlantı yönetimi (`components/ci/`)
SCM bağlantı ekranının (components/scm) kopyası uyarlanır:
- `CiConnectionList.vue` / `CiConnectionFormModal.vue` — baseUrl, username, API token,
  "Bağlantıyı test et" butonu (versiyon + crumb tespiti sonucu gösterilir).
- Organizasyon ayarlarında SCM sekmesinin yanına "CI/CD" sekmesi.

### 6.3 Proje ayarları — job eşleme
- `CiJobMappingList.vue` / `CiJobMappingFormModal.vue`:
  - Bağlantı seç → job dropdown'u (keşif ucu, arama destekli, folder'lı gösterim),
  - Tip (TASK_DEPLOY / RELEASE_PIPELINE) + ortam seçimi,
  - Parametre şablonu editörü: job'ın gerçek parametreleri otomatik listelenir, her birine
    sabit değer ya da `{{değişken}}` atanır (değişkenler dropdown'dan seçilebilir),
  - RELEASE_PIPELINE için `autoTransitionOnSuccess` anahtarı.

### 6.4 Task detayı — dev panelinde "Deploy" (`TaskDetail.vue` + `components/work/`)
- Mevcut geliştirme paneline `CiDeploySection.vue`:
  - Projede enabled TASK_DEPLOY eşlemesi yoksa bölüm hiç görünmez.
  - "Test ortamına deploy" butonu → modal: job (birden çoksa), branch (task'a bağlı
    `ScmBranch`'ler + repo default branch; serbest metin de girilebilir), çözülmüş
    parametrelerin önizlemesi → Tetikle.
  - Altında build tarihçesi: durum rozeti (renk + spinner RUNNING'de), build no,
    branch, tetikleyen, süre, Jenkins linki. QUEUED/RUNNING satırları 10 sn'de bir
    tazelenir (mevcut polling composable deseni).

### 6.5 Release ekranı — pipeline çalıştırma (`ReleasesView.vue` / `ReleaseFormModal.vue` çevresi)
- Release detayına `CiReleasePipelineSection.vue`:
  - Buton yalnız `APPROVED` statüsünde ve yetkili kullanıcıda aktif; diğer statülerde
    neden pasif olduğu tooltip'te açıklanır ("Pipeline çalıştırmak için release APPROVED olmalı").
  - Tetikleme öncesi onay modalı: çözülmüş parametreler + hedef job + (varsa)
    "başarıda otomatik RELEASED" uyarısı.
  - Build tarihçesi task panelindekiyle aynı bileşen (`CiBuildHistory.vue` ortak).
- Task kartlarında build rozeti (son build durumu) — **ileri faz**, Faz 6'ya bırakıldı.

---

## 7. Adım Adım Yapılacaklar

### Faz 0 — Veri modeli ve iskelet (backend)
- [x] Enum'ları ekle: `CiProvider`, `CiConnectionStatus`, `CiJobType`, `CiEnvironment`, `CiBuildStatus`, `CiBuildContext`
- [x] `PlanFeature`'a `CI_CD_INTEGRATION` ekle; PRO/MAX planlarına tanımla (admin panelden veya seed)
      → `PlanService.seedDefaultPlans` (PRO) + `DataInitializer.backfillScmGrants` (mevcut kurulumlar)
- [x] Entity'leri yaz: `CiConnection`, `CiJobMapping`, `CiBuild` (bölüm 4'teki alanlar + indeksler)
- [x] Repository arayüzleri: `CiConnectionRepository`, `CiJobMappingRepository`, `CiBuildRepository`
      (poller için `findActiveWithConnection`, task/release/proje tarihçe sorguları)

### Faz 1 — Jenkins istemcisi ve bağlantı yönetimi (backend)
- [x] `CiClient` arayüzü + `CiClientFactory` + `JenkinsClient` (bölüm 5.2: auth, crumb, test, keşif)
- [x] `CiConnectionService`: CRUD, token şifreleme (`ScmTokenCrypto`), test (crumb otomatik tespiti dahil), yetki kontrolleri, `ScmUrlValidator` ile baseUrl doğrulama
- [x] `CiConnectionController` + DTO'lar (bağlantı CRUD + test + job keşfi + job parametre uçları)
- [x] Entitlement kontrolünü bağlantı oluşturmaya ekle

> **Uygulama notu (K1'e ek karar):** Jenkins yanıtlarında dönen mutlak URL'ler kendi
> "Jenkins URL" ayarına göre üretilir; iç ağdaki bir host olabilir. Bu adreslere doğrudan
> istek atmak `ScmUrlValidator`'ın SSRF korumasını devre dışı bırakırdı. Bu yüzden
> `JenkinsClient` dönen her URL'in yalnız yol kısmını alıp doğrulanmış `baseUrl` üzerine
> yeniden çapalar (`reanchor`). Queue/build adresleri DB'ye de çapalanmış hâliyle yazılır.

### Faz 2 — Job eşleme (backend)
- [x] `CiJobMappingService`: CRUD, unique kontrol, parametre şablonu JSON validasyonu
      → yetki PROJECT_MANAGE_SETTINGS (SCM repo eşlemesiyle birebir aynı; K6'daki "org admin"
        ifadesi bu izinle karşılanır). jobFullName + bağlantı kimlik alanı sayılıp update'te değiştirilemez.
- [x] `CiJobMappingController` + DTO'lar
- [x] `CiParameterResolver`: `{{branch}}`, `{{taskKey}}`, `{{taskTitle}}`, `{{releaseName}}`, `{{projectKey}}`, `{{triggeredBy}}` çözümlemesi + bilinmeyen değişkende anlamlı hata
      → ayrıca `validateTemplate` (eşleme kaydında yapısal + bilinmeyen değişken kontrolü) ve
        "bilinen ama bu bağlamda boş değişken" (ör. task deploy'da `{{releaseName}}`) için ayrı hata.

### Faz 3 — Build tetikleme ve durum takibi (backend)
- [x] `CiBuildService.triggerForTask`: eşleme doğrulama, yetki, entitlement, rate limit, parametre çözümleme, `buildWithParameters`, `CiBuild` kaydı (QUEUED)
- [x] `CiBuildService.triggerForRelease`: + statü kapısı (APPROVED) ve release manager/admin kontrolü
- [x] `CiBuildPoller`: kuyruk çözümü → RUNNING → terminal durum; LOST temizliği; ardışık hata durumunda bağlantıyı INVALID işaretleme (bölüm 5.3)
      → durum makinesi `CiBuildSyncService`'te izole; poller yalnız bağlantı bazında gruplayıp orkestre eder,
        bağlantı sağlığı `CiConnectionHealthService`'te (eşik 5, başarıda otomatik ACTIVE'e dönüş).
- [x] Terminal durumda bildirim: tetikleyen kullanıcıya + (release'de) release manager'a `NotificationService` ile
      → yeni `NotificationType.CI_BUILD_SUCCEEDED/FAILED` (SchemaConstraintFixRunner check kısıtını zaten düşürüyor).
- [x] `autoTransitionOnSuccess` akışı: SUCCESS'te mevcut release geçiş servisiyle `RELEASED`'a geçir
      → **transaction ayrımı:** sync terminal durumu kendi tx'inde yazar, geçiş primitiflerle AYRI çağrılır
        (ReleaseService.updateStatus kendi tx'i); geçiş hatası build'in SUCCESS'ini geri almaz.
- [x] Tetikleme/tarihçe/refresh uçları: task, release, proje bazlı (bölüm 5.4)
      → ek olarak panel başına tek çağrılık `GET /api/tasks/{id}/ci` ve `GET /api/releases/{id}/ci`
        (featureEnabled + eşlemeler + tarihçe + canRun/blockedReason), polling için ayrı `/builds` ucu korundu.

### Faz 4 — Frontend: bağlantı ve eşleme yönetimi
- [x] `CiApi.js` (bağlantı + eşleme + task/release/proje build uçları; Faz 5 için de hazır)
- [x] `components/ci/CiConnectionsTab.vue` + `CiConnectionModal.vue`; org ayarları Entegrasyonlar sekmesine
      **alt-sekme** olarak eklendi (Git/SCM | CI/CD) — yeni üst sekme açmak yerine mevcut sekme bölündü.
- [x] `CiJobMappingPanel.vue` + `CiJobMappingModal.vue` (job keşif dropdown'u, `getJobParameters` ile
      şablon ön doldurma, değişken ekleyici, RELEASE_PIPELINE'da autoTransition); proje sayfasına SCM panelinin altına.
      → düzenlemede bağlantı+jobFullName salt-okunur (backend kimlik alanı kararıyla tutarlı).
- [x] Plan gating UI: `CiConnectionsTab` feature kapalıysa upgrade yönlendirmesi gösterir (GIT_INTEGRATION deseni);
      job paneli SCM repo paneliyle aynı şekilde boş-durum + bağlantıya yönlendirme kullanır.

### Faz 5 — Frontend: work modülü akışları
- [x] `CiBuildHistory.vue` (ortak tarihçe bileşeni, durum rozeti + RUNNING spinner, manuel yenileme, Jenkins linki)
      → polling bileşenin kendisinde değil, host bölümlerde (task/release) 10 sn'de bir `/builds` ile;
        aktif build kalmayınca timer durur. Manuel "yenile" butonu `refreshBuild` ile satır bazında.
- [x] `CiDeploySection.vue` → TaskDetail dev paneline: deploy modalı (job + branch datalist + parametre önizleme) + tarihçe
      → branch önerileri `getTaskScm`'den (task branch'leri + repo default), istemci-taraflı parametre önizlemesi
        (branch/taskKey/taskTitle çözülür; projectKey/triggeredBy sunucuda çözülür notuyla).
- [x] `CiReleasePipelineSection.vue` → Release detayına (ReleasesView genişletilen kart): statü kapılı buton
      (blockedReason tooltip), onay modalı (parametre önizleme + autoTransition uyarısı), tarihçe; oto-geçişte üst listeyi tazeler.
- [x] Task history / activity feed'e "deploy tetiklendi" kaydı → backend `auditService.recordChange(task,"ciDeploy",...)`
      (Faz 3) + `TaskHistory.vue`'ye `ciDeploy`🚀 ve `branch`🌿 etiket/ikonları.

### Faz 6 — Cila ve ileri özellikler (opsiyonel)
- [ ] Webhook ucu `/api/ci/webhook/{connectionId}` (secret imzalı) + Jenkinsfile `post` bloğu için kopyalanabilir `curl` örneği UI'da
- [ ] Task kartlarında son build durum rozeti (Board/List görünümleri)
- [ ] Proje dashboard'una "son build'ler" widget'ı
- [ ] Dokümantasyon: müşteri için "Jenkins bağlama rehberi" (Docs modülünde veya landing docs)

### Doğrulama (her fazın sonunda)
- [ ] Gerçek bir Jenkins'e karşı uçtan uca senaryo: bağlantı testi → job keşfi → eşleme → task deploy → durum SUCCESS/FAILURE akışı → release pipeline → autoTransition
- [ ] Yetki matrisi testi (K6): üye/admin/release manager kombinasyonları
- [ ] FREE plan org'unda uçların 402/403 döndüğünün kontrolü

---

## 8. Açık Sorular / İleride Karar Verilecekler

1. **Ortam kavramı genişletilsin mi?** Şimdilik enum (`TEST/STAGING/PROD`); müşteriler serbest
   ortam tanımı isterse ayrı `CiEnvironment` entity'sine geçilir.
2. **Task statüsüyle otomasyon:** "Task 'Test'e taşınınca otomatik deploy" kural motoru —
   bilinçli olarak kapsam dışı; talep gelirse Faz 6 sonrası.
3. **Poller ölçeği:** Org sayısı büyüyünce 20 sn'lik tek poller yerine bağlantı başına
   jitter'lı dağıtım gerekebilir; şimdilik yeterli.
4. **Jenkins folder yetkileri:** Keşif, token sahibinin görebildiği job'larla sınırlı —
   müşteriye dokümantasyonda belirtilecek.
