# STQL — ScrumTools Query Language

> Görevleri Jira JQL'e benzer bir sorgu diliyle aramak ve filtrelemek için tasarlanmış
> dil. Söz dizimi, Jira'dan gelen kullanıcının ezberini bozmamak için kasten JQL'e yakın
> tutulmuştur.

Arayüzde **Görevler → sorgu çubuğu → STQL sekmesi** altından kullanılır. "Basit" sekmesindeki
görsel filtre de aynı motoru kullanır; iki sekme arasında geçiş yapıldığında sorgu korunur.

---

## 1. Hızlı Örnekler

```sql
-- Başlıkta "ödeme" geçen görevler (eski filtrede yapılamıyordu)
summary ~ "ödeme"

-- Bana atanmış, bitmemiş işler
assignee = currentUser() AND status != Done

-- Bu hafta biten yüksek öncelikli işler
due <= endOfWeek() AND priority IN (Critical, High) ORDER BY due ASC

-- Aktif sprintte etiketsiz kalan işler
sprint = currentSprint() AND labels IS EMPTY

-- Açıklamasında "regresyon" geçen ya da kritik olan hatalar
type = bug AND (description ~ "regresyon" OR priority = Critical)

-- Son 7 günde açılan, hâlâ atanmamış işler
created >= -7d AND assignee IS EMPTY ORDER BY created DESC

-- Özel alan üzerinden sorgu
cf[fixVersion] = "2.4.0"
```

---

## 2. Söz Dizimi

```
sorgu      := koşullar? sıralama?
koşullar   := ifade (AND | OR ifade)*
ifade      := NOT ifade | "(" koşullar ")" | koşul
koşul      := alan operatör değer
sıralama   := ORDER BY alan (ASC|DESC)? ("," alan (ASC|DESC)?)*
```

- **Öncelik:** `NOT` > `AND` > `OR`. Yani `a AND b OR c` = `(a AND b) OR c`.
  Farklı gruplama için parantez kullanın.
- **Büyük/küçük harf:** anahtar kelimeler, alan adları ve değerler duyarsızdır.
  `STATUS = done` ile `status = Done` aynı sonucu verir.
- **Tırnak:** boşluk veya özel karakter içeren değerler tırnaklanmalıdır — `"In Progress"`.
  Tek kelimelik değerler tırnaksız yazılabilir — `Done`.

### Operatörler

| Operatör | Anlam | Örnek |
|---|---|---|
| `=` | eşittir | `status = Done` |
| `!=` | eşit değildir (boş olanlar da döner) | `status != Done` |
| `~` | içerir (büyük/küçük harf duyarsız) | `summary ~ "ödeme"` |
| `!~` | içermez | `summary !~ "test"` |
| `>` `>=` `<` `<=` | karşılaştırma (sayı ve tarih) | `storyPoints >= 5` |
| `IN` | listedekilerden biri | `status IN (Done, Cancelled)` |
| `NOT IN` | listede yok | `priority NOT IN (Low)` |
| `IS EMPTY` | boş / atanmamış | `assignee IS EMPTY` |
| `IS NOT EMPTY` | dolu | `due IS NOT EMPTY` |

`IS NULL` ve `IS NOT NULL`, `IS EMPTY` / `IS NOT EMPTY` ile eş anlamlıdır.

> **Not:** `!=` ve `NOT IN`, alanı boş olan görevleri de sonuca dâhil eder.
> "Durumu Done olmayan" sorgusundan durumu hiç atanmamış görevlerin düşmesi
> beklenmediği için bilinçli olarak böyle tasarlandı.

---

## 3. Alanlar

| Alan | Diğer yazımları | Tip | Açıklama |
|---|---|---|---|
| `summary` | `title` | metin | Görev başlığı |
| `description` | `desc` | uzun metin | Yalnız `~`, `!~`, `IS EMPTY` |
| `key` | `customId`, `issuekey` | metin | PROJE-12 biçimindeki görev no |
| `status` | — | seçim | Durum |
| `priority` | — | seçim | Critical / High / Medium / Low |
| `type` | `issueType` | seçim | task / story / bug / epic |
| `resolution` | — | seçim | Çözüm |
| `environment` | — | metin | Ortam |
| `assignee` | — | kullanıcı | Atanan |
| `reporter` | — | kullanıcı | Açan |
| `developer` `analyst` `tester` | — | kullanıcı | Rol bazlı atamalar |
| `labels` | `label` | liste | Etiketler |
| `watcher` | `watchers` | liste | Takip edenler |
| `sprint` | `sprintId` | ilişki | Sprint adı veya id |
| `project` | `projectId` | ilişki | Proje anahtarı veya adı |
| `release` | `fixVersion` | ilişki | Sürüm adı veya id |
| `parent` | `parentTask` | ilişki | Üst görevin no'su |
| `storyPoints` | `points` | sayı | |
| `estimatedHours` | `estimate` | sayı | |
| `loggedHours` | `timeSpent` | sayı | |
| `due` | `dueDate` | tarih | Son tarih |
| `startDate` | — | tarih | |
| `created` | `createdAt` | zaman | |
| `updated` | `updatedAt` | zaman | |
| `resolved` | `resolvedAt` | zaman | |
| `cf[anahtar]` | — | özel alan | `customFields` içindeki dinamik alan |
| `smart["zengin filtre"]` | — | sınıflandırma | Zengin filtrenin akıllı filtre kategorisi (bkz. §3.1) |

İlişki alanlarında hem ad hem id yazılabilir: `sprint = "Sprint 12"` veya `sprint = <uuid>`.

### 3.1 Akıllı filtreler — `smart["…"]`

Bir zengin filtrede tanımlanan renkli kategoriler sorgu dilinden de kullanılabilir.
Böylece "Test aşamasındaki işler" tanımı tek yerde durur; görev listesi, board ve
grafikler aynı tanımı paylaşır.

```sql
smart["Durum Akışı"] = "Test"
smart["Durum Akışı"] IN ("Test", "UAT")
smart["Durum Akışı"] IS EMPTY          -- hiçbir kategoriye girmeyenler
smart["Durum Akışı"] != "Tamamlandı"   -- sınıflandırılmamışlar da döner
```

**Ad her zaman tırnaklanır.** Zengin filtre adları boşluk ve Türkçe karakter içerebilir;
tırnaksız yazım çözümlenemez. Sorgu editörünün otomatik tamamlaması bu biçimi kendisi yazar.

**"İlk eşleşen kazanır."** `smart[RF] = "Test"`, *Test kuralını sağlayan ve kendisinden
önce gelen hiçbir kuralı sağlamayan* görevler demektir. Kuralların sırası zengin filtre
editöründen sürüklenerek değiştirilir; grafiklerdeki dilimlerle bu sorgunun sonucu her
zaman birebir örtüşür.

**Sınırlar.** Bir akıllı filtrenin sorgusu başka bir `smart[…]` içerebilir; iç içe geçme
en fazla 3 seviyedir ve döngüler reddedilir. Çözümlenmiş sorgu 200 koşulu aşamaz.
Erişemediğiniz bir zengin filtrenin adı, olmayan bir ad gibi hata verir.

Alanların tam listesi çalışma zamanında `GET /api/teams/{teamId}/tasks/query/fields`
ucundan alınır; sorgu editöründeki otomatik tamamlama da bu kaynaktan beslenir.

---

## 4. Fonksiyonlar

| Fonksiyon | Döndürdüğü |
|---|---|
| `currentUser()` | Oturumdaki kullanıcının e-postası |
| `now()` | Şu an |
| `today()` / `startOfDay()` | Bugünün başlangıcı |
| `endOfDay()` | Bugünün sonu |
| `startOfWeek()` / `endOfWeek()` | Haftanın başı (Pazartesi) / sonu (Pazar) |
| `startOfMonth()` / `endOfMonth()` | Ayın başı / sonu |
| `currentSprint()` / `openSprints()` | Açık sprintler |
| `closedSprints()` | Tamamlanmış sprintler |

### Göreli tarihler

Tarih alanlarında `-7d` gibi göreli değerler kullanılabilir:

| Birim | Anlamı | Örnek |
|---|---|---|
| `d` | gün | `created >= -7d` (son 7 gün) |
| `w` | hafta | `due <= 2w` (önümüzdeki 2 hafta) |
| `M` | ay | `created >= -1M` |
| `y` | yıl | `created >= -1y` |
| `h` | saat | `updated >= -4h` |
| `m` | dakika | `updated >= -30m` |

Zaman damgalı alanlarda (`created`, `updated`, `resolved`) `=` operatörü **gün bazlı**
çalışır: `created = "2026-01-15"` o gün içindeki tüm kayıtları döndürür.

---

## 5. Sıralama

```sql
ORDER BY priority DESC, created ASC
```

Yön belirtilmezse `ASC` varsayılır. `priority` alanı alfabetik değil **mantıksal
ağırlığa** göre sıralanır (Critical → High → Medium → Low).

Görev listesinde bir sütun başlığına tıklamak da sorguya `ORDER BY` yazar; böylece
sıralama paylaşılan linkin parçası olur.

---

## 6. Sınırlar

Kötü niyetli veya kazara ağır sorgulara karşı:

| Sınır | Değer |
|---|---|
| Sorgu uzunluğu | 2000 karakter |
| Koşul sayısı | 50 |
| İç içe parantez derinliği | 10 |
| Sayfa boyutu | 200 kayıt |

Ayrıca **takım ve aktif proje kapsamı her zaman zorlanır** — sorgu ne yazarsa yazsın
kullanıcı başka takımın verisini göremez. Değerler JPA Criteria parametresi olarak
bağlandığı için SQL enjeksiyonu mümkün değildir.

---

## 7. Kayıtlı Filtreler

Bir sorgu kurulduktan sonra **Kayıtlı Filtreler → Bu sorguyu kaydet** ile saklanabilir.

| Görünürlük | Kimler görür |
|---|---|
| `PRIVATE` | Yalnız oluşturan |
| `TEAM` | Filtrenin takımının üyeleri |
| `PROJECT` | Filtrenin projesinde çalışan tüm takımlar |

Düzenleme ve silme her durumda yalnız sahibine açıktır: paylaşılan bir filtreyi
başkasının altından değiştirmek, o filtreye bağlı görünümleri sessizce bozardı.

Kayıtlı filtreler ileride dashboard widget'larının veri kaynağı olacak —
bkz. [DASHBOARD_WIDGET_ROADMAP.md](DASHBOARD_WIDGET_ROADMAP.md).

---

## 8. API

| Uç | Açıklama |
|---|---|
| `POST /api/teams/{teamId}/tasks/query` | Sorguyu çalıştırır → sayfalı sonuç |
| `POST /api/teams/{teamId}/tasks/query/validate` | Doğrular **ve sayar** → `{valid, count}` veya `{valid:false, error:{message, position, length}}` |
| `POST /api/teams/{teamId}/tasks/query/count` | Yalnız eşleşen kayıt sayısı (program içi kullanım) |
| `POST /api/teams/{teamId}/tasks/query/aggregate` | Sonucu bir alana göre gruplar → `[{key,label,value,color,filter}]` |
| `GET /api/teams/{teamId}/tasks/query/fields` | Alan + operatör + fonksiyon kataloğu |
| `GET /api/teams/{teamId}/tasks/query/suggest` | Bir alan için değer önerileri |
| `GET/POST/PUT/DELETE /api/teams/{teamId}/filters` | Kayıtlı filtre CRUD |
| `POST /api/teams/{teamId}/filters/{id}/run` | Kayıtlı filtreyi çalıştırır |

Eski `POST /api/teams/{teamId}/tasks/filter` ucu geriye dönük uyum için durmaya devam
ediyor; sunucuda o da aynı motora çevriliyor (`LegacyFilterTranslator`).

### Editörün istek davranışı

Sorgu editörü **yazarken sorgu çalıştırmaz**. Kullanıcı 700 ms duraklayınca tek bir
`validate` isteği atar; bu istek hem sözdizimini denetler hem eşleşen kayıt sayısını
döndürür. Sorgu ancak Enter'a basıldığında veya "Çalıştır" ile fiilen çalışır.

Sorgu uçlarında global hata bildirimi (toast) kapalıdır: yazım hâlindeki bir sorgunun
geçersiz olması beklenen bir durumdur, hata editörün içinde konumuyla gösterilir.
Yazım anındaki uyarı amber, çalıştırma hatası kırmızıdır.

Değer önerileri alan başına önbelleğe alınır; sunucunun 50'lik öneri sınırına
ulaşılmadıysa ön ek süzmesi yerelde yapılır ve her tuşta istek atılmaz.

---

## 9. Kod Haritası

**Backend** — `backend/src/main/java/com/scrumtools/query/`

| Sınıf | Sorumluluk |
|---|---|
| `QueryLexer` / `QueryToken` | Metin → token; her token konumunu taşır |
| `QueryParser` / `QueryNode` / `ParsedQuery` | Token → soyut sözdizim ağacı |
| `TaskFieldRegistry` / `FieldDescriptor` / `FieldType` | Alan kataloğu — **tek doğruluk kaynağı** |
| `QueryFunctions` | `currentUser()`, tarih fonksiyonları, göreli tarihler |
| `QueryPredicateBuilder` | AST → JPA Criteria `Predicate`; akıllı filtre sınıflandırması (`CASE WHEN`) |
| `QueryComposer` | Birden çok sorgu parçasını ağaç düzeyinde birleştirir |
| `StqlRenderer` | AST → STQL metni (grafikten görev listesine geçiş) |
| `SmartFilterCatalog` | `smart["…"]` → zengin filtrenin sıralı kuralları |
| `TaskQueryService` | Çözümleme + çalıştırma + sayfalama |
| `TaskAggregationService` | Gruplama/sayma/toplama + iki eksenli matris — grafik ve ısı haritası widget'larının kaynağı |
| `SeriesReplay` | Geçmişi `task_history`'den kurgulayan **dar** değerlendirici — yalnız zaman serisi backfill'i için, kapsamı bilinçli olarak küçük (bkz. RICH_FILTER_PLAN.md — K13) |
| `QuerySuggestionService` | Otomatik tamamlama kaynakları |
| `LegacyFilterTranslator` | Eski `filters[]` formatı ↔ STQL |

**Frontend** — `frontend/src/`

| Dosya | Sorumluluk |
|---|---|
| `api/QueryApi.js` | Sorgu uçları |
| `api/SavedFilterApi.js` | Kayıtlı filtre uçları |
| `composables/useTaskQuery.js` | Sorgu state'i, görsel ↔ STQL senkronu, URL (`?q=`) |
| `utils/stql.js` | İstemci tarafı çeviri ve `ORDER BY` yardımcıları |
| `components/work/QueryBar.vue` | "Basit" / "STQL" sekmeli kabuk |
| `components/work/StqlInput.vue` | Otomatik tamamlamalı sorgu editörü |
| `components/work/SavedFilterMenu.vue` | Kayıtlı filtre menüsü |

### Yeni alan sorgulanabilir yapmak

Tek değişiklik `TaskFieldRegistry.FIELDS` listesine bir satır eklemektir. Arayüzün alan
listesi ve otomatik tamamlaması katalogdan beslendiği için ayrıca güncelleme gerekmez.

```java
FieldDescriptor.of("severity", "Önem", FieldType.ENUM, "severity").withSuggest("severities")
```
