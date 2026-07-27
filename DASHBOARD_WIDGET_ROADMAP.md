# Dashboard Widget Yol Haritası

> Kayıtlı filtrelere (STQL) dayalı, kullanıcının kendi kurduğu dashboard widget'ları.
> Bu doküman **plan**dır — kod yazılmadı. Sorgu dili ve kayıtlı filtre altyapısı
> tamamlandı; bu iş onun üzerine binecek.
>
> Önkoşul dokümanı: [TASK_QUERY_LANGUAGE.md](TASK_QUERY_LANGUAGE.md)

---

## 1. Amaç

Bugün dashboard sabit widget tipleriyle sınırlı: `SUMMARY`, `BURNDOWN`, `VELOCITY`,
`WORKLOAD`, `OVERDUE`, `CREATED_VS_RESOLVED`. Bunlar hazır sorularla gelir —
"benim takımımın son 2 sprintte kalan kritik bug'ları" gibi bir soruyu soramaz.

Hedef: **kullanıcının kaydettiği bir filtreyi widget'a dönüştürebilmesi.** Filtre
zaten bir soru; widget onun görselleştirilmiş cevabı.

**Kapsam dışı (ilk sürümde):** widget'lar arası çapraz filtreleme (bir widget'a
tıklayınca diğerlerinin daralması), zamanlanmış e-posta raporu, dashboard paylaşımı.

---

## 2. Yaslandığımız Mevcut Altyapı

| Mevcut yapı | Widget işindeki rolü |
|---|---|
| `SavedFilter` entity + `visibility` (PRIVATE/TEAM/PROJECT) | Widget'ın veri kaynağı; paylaşım kuralı hazır geliyor |
| `TaskQueryService.count()` | Sayaç widget'ı için tek çağrı yeter |
| `TaskQueryService.search()` | Liste widget'ı için sayfalı sonuç |
| `UserDashboard.layout` (JSONB, vue-grid-layout uyumlu) | Yeni widget tipleri aynı listeye girer, şema değişmez |
| `DashboardController` `/api/dashboards` + `/widgets` upsert | Widget CRUD'u hazır |
| `components/dashboard/*Widget.vue` deseni | Yeni widget'lar aynı deseni izler |
| `StatCard.vue` | `FILTER_COUNT` widget'ının görsel temeli |
| `ddl-auto: update` | Yeni alan/entity için migration gerekmez |

**Kritik nokta:** widget kendi sorgu metnini **taşımaz**, `savedFilterId` referansı tutar.
Filtre güncellenince ona bağlı tüm widget'lar kendiliğinden güncellenir; sorgu metni
kopyalansaydı filtre ile widget'lar zamanla birbirinden ayrışırdı.

---

## 3. Yeni Widget Tipleri

### K1 — `FILTER_COUNT` (sayaç)
Tek sayı + etiket. "Bana atanmış açık işler: 12"

```json
{ "id":"w1", "type":"FILTER_COUNT", "teamId":"…", "savedFilterId":"…",
  "title":"Bana atanmış açık işler", "threshold": { "warn": 10, "danger": 20 },
  "x":0, "y":0, "w":3, "h":2 }
```

Eşik aşılınca kart rengi değişir — "bu sayı büyüdüyse bir sorun var" sinyali.
Gereken uç: `POST /filters/{id}/count`.

### K2 — `FILTER_LIST` (liste)
Filtrenin ilk N görevi, tıklanabilir. "Bu sprintte gözden geçirilmeyi bekleyenler"

```json
{ "type":"FILTER_LIST", "savedFilterId":"…", "limit":10,
  "columns":["key","summary","assignee","priority"] }
```

Mevcut `POST /filters/{id}/run` ucu yeterli — yeni uç gerekmiyor.

### K3 — `FILTER_GROUPED_CHART` (gruplama grafiği)
Filtre sonucunu bir alana göre gruplayıp pasta/çubuk grafik. "Açık bug'ların
önceliğe göre dağılımı"

```json
{ "type":"FILTER_GROUPED_CHART", "savedFilterId":"…",
  "groupBy":"priority", "chart":"donut" }
```

**Yeni uç gerekiyor** (aşağıda K4).

### K4 — Agregasyon ucu

```
POST /api/teams/{teamId}/filters/{filterId}/aggregate
Body: { groupBy: "priority", metric: "count" | "storyPoints", projectId? }
→ [{ "key": "Critical", "label": "Critical", "value": 8 }, …]
```

`groupBy` yalnız `TaskFieldRegistry`'deki gruplanabilir alanları kabul eder
(ENUM, USER, ENTITY_REF, COLLECTION). Uygulama, `QueryPredicateBuilder`'ın ürettiği
predicate'e Criteria `groupBy` + `count`/`sum` eklemekten ibaret — sorgu motoru
yeniden yazılmaz.

`metric: "storyPoints"` seçeneği "hangi ekip üyesinde kaç puanlık iş var" gibi
kapasite sorularını mümkün kılar.

### K5 — `FILTER_TREND` (zaman serisi) — *sonraki faz*
Filtrenin eşleşen kayıt sayısının zaman içindeki değişimi. Anlık sorgudan üretilemez;
periyodik anlık görüntü (snapshot) tablosu gerektirir. İlk sürümün dışında bırakıldı,
mevcut `CreatedVsResolvedWidget` bu ihtiyacın bir kısmını zaten karşılıyor.

---

## 4. Mimari Kararlar

### K6 — Widget yapılandırması JSONB'de kalır, yeni entity açılmaz
`UserDashboard.layout` zaten şemasız bir liste. Widget tipi başına entity açmak,
her yeni tip için migration demek olurdu. Doğrulama uygulama katmanında yapılır.

### K7 — Filtre silinince widget kırılmaz, "yetim" duruma düşer
`savedFilterId` artık çözülemiyorsa widget "Bu widget'ın filtresi silinmiş — yeni bir
filtre seçin" kartına döner. Alternatif (widget'ı sessizce silmek) kullanıcının
dashboard düzenini haber vermeden bozardı.

### K8 — Görünürlük filtreden miras alınır, widget ayrı yetki taşımaz
Widget yalnız kullanıcının erişebildiği bir filtreyi gösterebilir; erişim kontrolü
`SavedFilterService.findAccessible` üzerinden zaten yapılıyor. Widget seviyesinde
ikinci bir yetki mekanizması kurulmaz.

### K9 — Yenileme aralığı widget başına
`refreshInterval` (saniye, varsayılan 0 = elle). Sayaç widget'ları ucuz olduğu için
kısa aralık verilebilir; liste ve grafik widget'ları için varsayılan elle yenileme.

### K10 — Dashboard hâlâ kullanıcıya ait
`UserDashboard` kullanıcı başına tek satır. Takım dashboard'u (paylaşılan düzen)
ayrı bir iştir; bu fazın kapsamında değil ama `SavedFilter.visibility` deseni
oraya doğal olarak genişler.

---

## 5. Fazlandırma

| Faz | İçerik | Bağımlılık |
|---|---|---|
| **1** | `FILTER_COUNT` + `/count` ucu + widget ekleme akışında filtre seçici | — |
| **2** | `FILTER_LIST` (mevcut `/run` ucuyla) | Faz 1'in filtre seçicisi |
| **3** | `/aggregate` ucu + `FILTER_GROUPED_CHART` | Faz 1 |
| **4** | Eşik/renk kuralları, `refreshInterval`, yetim widget kartı | Faz 1–3 |
| **5** *(opsiyonel)* | `FILTER_TREND` + snapshot tablosu | Ayrı değerlendirme |

---

## 6. Dokunulacak Dosyalar

**Backend**
- `query/TaskQueryService.java` — `aggregate(teamId, projectId, parsedQuery, groupBy, metric)` ekle
- `query/TaskFieldRegistry.java` — alanlara `groupable` bilgisi (ya da tip üzerinden türet)
- `service/SavedFilterService.java` — `count()`, `aggregate()` sarmalayıcıları
- `controller/SavedFilterController.java` — `/count`, `/aggregate` uçları
- `service/DashboardService.java` — widget doğrulaması (bilinmeyen tip / eksik `savedFilterId`)

**Frontend**
- `api/DashboardApi.js` — yeni widget tipleri için çağrılar
- `api/SavedFilterApi.js` — `countSavedFilter`, `aggregateSavedFilter`
- `components/dashboard/FilterCountWidget.vue` (yeni, `StatCard.vue` üzerine)
- `components/dashboard/FilterListWidget.vue` (yeni)
- `components/dashboard/FilterChartWidget.vue` (yeni, Chart.js — mevcut widget'lardaki desen)
- `components/dashboard/WidgetPicker` — widget eklerken kayıtlı filtre seçimi
- `pages/Dashboard.vue` — yeni tiplerin bileşen eşlemesi

---

## 7. Açık Kararlar

Bu iş başlamadan netleşmesi gerekenler:

1. **Widget başına takım seçimi mi, dashboard genelinde mi?** Kayıtlı filtre zaten bir
   takıma bağlı; widget'ın ayrıca `teamId` taşıması gereksiz olabilir. Mevcut widget'lar
   `teamId` taşıyor — tutarlılık için korumak mı, filtreden türetmek mi?
2. **Aynı filtreye bağlı çok sayıda widget** aynı sorguyu tekrar tekrar çalıştırır.
   Kısa ömürlü (30–60 sn) sunucu tarafı önbellek gerekir mi, yoksa istemci tarafı
   yeterli mi?
3. **Paket bazlı sınır** (`EntitlementService` / `PlanFeature`): filtre tabanlı widget'lar
   FREE pakette de olsun mu, yoksa PRO'ya mı bağlansın? Kayıtlı filtre sayısı sınırı?
4. **Grafik renk paleti**: mevcut widget'lar kendi renklerini taşıyor; `priority`/`status`
   için ortak bir renk sözlüğü çıkarılmalı mı?
