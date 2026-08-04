# Rich Filter — Zengin Filtre ve Etkileşimli Dashboard Planı

> Jira'daki **Rich Filters for Jira Dashboards** eklentisinin ScrumTools karşılığı.
>
> **Durum: Faz 0–5 yazıldı** (2026-08-04) — gruplama altyapısı, zengin filtre CRUD'u,
> akıllı filtre sınıflandırma motoru, STQL `smart[…]` alanı, yönetim ekranı, dashboard
> entegrasyonu (kontrolcü + sayaç + çoklu ölçü + liste + grafik + kuyruk + oran
> widget'ları, çapraz filtreleme, URL'ye yansıyan seçim), grafikten göreve drill-down,
> sabit/dinamik filtreler, görünümler ve kendiliğinden tazeleme çalışır durumda.
> Faz 6'dan itibaren (zaman serileri, ısı haritası, bildirimler) plan hâlâ plan — §11.
>
> Önkoşul dokümanlar:
> [TASK_QUERY_LANGUAGE.md](TASK_QUERY_LANGUAGE.md) · [DASHBOARD_WIDGET_ROADMAP.md](DASHBOARD_WIDGET_ROADMAP.md)

---

## 1. Amaç

Bugün dashboard sabit widget tipleriyle geliyor (`SUMMARY`, `BURNDOWN`, `VELOCITY`,
`WORKLOAD`, `OVERDUE`, `CREATED_VS_RESOLVED`). Her widget kendi sorusunu kendi sorar,
widget'lar birbirinden habersizdir ve kullanıcı hiçbirini daraltamaz.

Hedef üç katmanlı:

1. **Zengin filtre (rich filter)** — bir temel sorgu + onun üstüne kurulmuş adlandırılmış
   parçalar (akıllı filtreler, dinamik/sabit filtreler, görünümler, kuyruklar, oranlar).
2. **Etkileşimli dashboard** — aynı zengin filtreye bağlı tüm widget'lar ortak bir seçim
   durumunu paylaşır. Bir grafikte "Critical" dilimine tıklamak, sayfadaki bütün
   widget'ları o dilime daraltır.
3. **Sınıflandırma** — akıllı filtreler görevleri renkli kategorilere ayırır; grafiklerin
   grup ekseni, listelerin renk kodu ve kuyrukların satırları bu kategorilerden gelir.

Ekrandaki Jira örneğinde görülen `status` zengin filtresi tam olarak budur: yedi adet
adlandırılmış JQL parçası (On Hold, Analysis, Development, Test, UAT, Completed, Cancelled),
her biri bir renkle — ve bu yedi kategori dashboard'daki her grafiğin ortak dili.

---

## 2. Kavram Modeli

| Kavram | Jira karşılığı | Ne işe yarar |
|---|---|---|
| **Zengin filtre** | Rich filter | Temel sorgu + tüm alt öğelerin kabı. Widget'lar buna bağlanır. |
| **Akıllı filtre** | Smart filter | Adlandırılmış + renkli STQL parçası. Filtrelemez, **sınıflandırır**. |
| **Dinamik filtre** | Dynamic filter | Bir alandan (atanan, öncelik…) sonuç kümesine göre üretilen açılır liste. |
| **Sabit filtre** | Static filter | Yazarın elle tanımladığı seçenekli açılır liste ("Bu hafta / Bu ay / Bu çeyrek"). |
| **Görünüm** | View | Seçimlerin adlandırılmış hâli — tek tıkla o duruma dönmek. |
| **Kuyruk** | Queue | Akıllı filtrelerden oluşan, sayaçlı liste paneli (triage görünümü). |
| **Özel değer** | Custom value | Görev başına türetilmiş sayı (yaş, çevrim süresi…). Grafik metriği olur. |
| **Özel oran** | Custom ratio | İki ölçünün oranı — "% tamamlanan". Gösterge (gauge) widget'ı. |
| **Zaman serisi** | Time series | Bir akıllı filtrenin sayısının zaman içindeki seyri. |

### Temel sorgu (base)

Zengin filtrenin kökü ya **var olan bir kayıtlı filtre** (`savedFilterId`) ya da doğrudan
**STQL metni**dir. Kayıtlı filtre tercih edilir: filtre güncellenince zengin filtre de
güncellenir, iki yerde aynı sorgu durmaz (bkz. `DASHBOARD_WIDGET_ROADMAP.md` §2).

---

## 3. Sorgu Birleştirme Semantiği

Çalışma anındaki sorgu şudur:

```
temel  AND  (sabit seçimler)  AND  (dinamik seçimler)  AND  (akıllı seçimler)  AND  (metin arama)
```

- **Kontroller arası:** `AND` — farklı açılır listeler birbirini daraltır.
- **Kontrol içi çoklu seçim:** `OR` — aynı listeden iki değer seçmek genişletir.
- **Akıllı filtre seçimi:** `OR` — "Test veya UAT aşamasındakiler".
- **Kapsam (takım + proje):** her zaman `AND` — kullanıcı ne seçerse seçsin aşılamaz.

### K1 — Birleştirme metin düzeyinde değil, **AST düzeyinde** yapılır

Sorgu parçalarını string olarak birleştirmek (`"(" + a + ") AND (" + b + ")"`) üç şeyi
bozar: `ORDER BY` içeren parçalar sözdizimi hatası verir, operatör önceliği parantez
hatalarına açıktır, ve hata konumları (`position`) anlamsızlaşır.

Bunun yerine her parça `QueryParser.parse()` ile `ParsedQuery`ye çevrilir, `where`
düğümleri `QueryNode.And` / `QueryNode.Or` ile birleştirilir, `ORDER BY` tek bir
kaynaktan (görünüm > temel sorgu) alınır. Yeni sınıf:

```java
// query/QueryComposer.java
public static ParsedQuery compose(ParsedQuery base, List<List<ParsedQuery>> orGroups)
```

### K2 — İstemci hiçbir zaman ham sorgu göndermez

Widget ve dashboard istekleri **seçim nesnesi** taşır, STQL taşımaz:

```json
{
  "static":  { "<elementId>": "<optionId>" },
  "dynamic": { "<elementId>": ["ahmet@x.com", "mehmet@x.com"] },
  "smart":   ["<smartId>", "<smartId>"],
  "text":    "ödeme"
}
```

Sunucu bu id'leri kendi kayıtlarından çözer. Sonuç: paylaşılan bir dashboard linki başka
takımın verisine açılamaz, seçim durumu kısa ve URL'ye sığar, sorgu limitleri
(2000 karakter / 50 koşul) yazma anında zaten denetlenmiştir. `text` alanı tek istisnadır
ve parametre olarak bağlanmış tek bir `summary ~ ?` koşuluna çevrilir.

### K3 — Bileşik sorgu için ayrı limit

Yazılan her parça mevcut limitlere tabidir. Birleşmiş sorgu doğal olarak bunları aşar;
onun için ayrı bir tavan konur: **azami 200 düğüm**. Aşılırsa istek reddedilir — 40 akıllı
filtreli bir zengin filtrenin veritabanını kilitlemesi engellenmiş olur.

---

## 4. Akıllı Filtreler — Sınıflandırma Motoru

### K4 — Sıra önemlidir: **ilk eşleşen kazanır**

Ekrandaki örnekte "Completed" ile "Test" çakışabilir (bir görev iki JQL'i de sağlayabilir).
Jira'da sıralama kolu (drag handle) tam bu yüzden vardır.

- **Renklendirme ve etiketleme:** görev, sırada **ilk eşleştiği** akıllı filtrenin rengini alır.
  Deterministiktir, kullanıcı sırayı sürükleyerek kontrol eder.
- **Grafik gruplaması:** iki mod sunulur —
  - `exclusive` (**varsayılan**): ilk eşleşen kazanır, dilimlerin toplamı toplam görev
    sayısına eşittir, eşleşmeyenler `Sınıflandırılmamış` kovasına düşer.
  - `overlapping`: her akıllı filtre bağımsız sayılır, toplam aşılabilir.

  Varsayılanın `exclusive` olmasının sebebi: toplamı gerçek toplamdan büyük olan bir pasta
  grafik yanlış bilgi verir. Kullanıcı bilinçli olarak diğerine geçebilir.

### K5 — Sınıflandırma tek SQL sorgusuyla yapılır

N akıllı filtre için N ayrı `count` sorgusu atmak yerine Criteria `selectCase()` ile tek
bir ifade kurulur:

```java
CriteriaBuilder.Case<String> bucket = cb.selectCase();
for (RichFilterElement smart : smartsInOrder) {
    bucket = bucket.when(predicateOf(smart), smart.getId().toString());
}
Expression<String> expr = bucket.otherwise(UNCLASSIFIED);
// select expr, count(*) ... group by expr
```

`CASE WHEN` zaten "ilk eşleşen kazanır" semantiğine sahiptir — K4 kararı veritabanı
davranışıyla birebir örtüşür. Aynı ifade görev listesinde de `select`e eklenerek her
görevin etiketi ek sorgu olmadan döner. `overlapping` modunda N adet `count` sorgusuna
düşülür; bu modun pahalı olduğu arayüzde belirtilir.

### K6 — "Sınıflandırılmamış" bir hata değil, bir sinyaldir

Hiçbir akıllı filtreye uymayan görevler gri bir dilim olarak gösterilir ve tıklanabilir.
"14 görev hiçbir kategoriye girmiyor" bilgisi çoğu zaman ya eksik bir akıllı filtreyi ya
da veri kalitesi sorununu (durumu unutulmuş görevler) ortaya çıkarır. Jira bunu sessizce
"Other" diye gösterir; biz sayacı ve listesini öne çıkarıyoruz.

### K18 — Akıllı filtreler STQL'in birinci sınıf alanıdır *(Faz 1 — karar verildi)*

Başlangıçta Faz 7 önerisiydi (Ö1); Faz 1'e alındı. Sonuç: sınıflandırma zengin filtre
kabuğunun içinde hapis kalmaz — görev listesinde, board'da, kayıtlı filtrede ve ileride
bildirim kurallarında da kullanılabilir. Jira'da akıllı filtreler bu sınırı aşamaz.

```sql
smart["Sprint Sağlığı"] = "Test"
smart["Sprint Sağlığı"] IN ("Test", "UAT")
smart["Sprint Sağlığı"] IS EMPTY          -- sınıflandırılmamışlar
smart["Sprint Sağlığı"] != "Cancelled"    -- boşlar da döner (STQL != kuralı)
```

**Ad her zaman nitelenmiş yazılır.** "Zengin filtre editörünün içindeyken kısa yazım
(`smart = "Test"`) geçerli olsun" cazip görünüyor ama aynı metnin bağlama göre farklı
anlama gelmesi demek: kayıtlı filtreye kopyalanan sorgu sessizce başka bir şey filtreler.
Editörün otomatik tamamlaması nitelenmiş hâli kendisi yazar, kullanıcı zahmet görmez.

**Çözümleme.** `cf[…]` deseninin aynısı: `TaskFieldRegistry.resolve()` `smart[…]` ön ekini
tanıyıp `FieldType.SMART_FILTER` tanımı üretir. Zengin filtre adı → sıralı kural listesi
çevirisi `QueryContext`e eklenen bir köprüyle yapılır — mevcut `SprintResolver` ile birebir
aynı desen:

```java
@FunctionalInterface
public interface SmartFilterResolver {
    /** Erişilebilir zengin filtreyi adıyla çözer; sırayı korur. */
    List<SmartClause> clausesOf(UUID teamId, String richFilterName);
}
```

**Semantik, K4 ile birebir aynı olmak zorundadır.** `smart[RF] = "Test"`, "Test kuralını
sağlayan **ve kendisinden önce gelen hiçbir kuralı sağlamayan**" görevler demektir. Aksi
hâlde halka grafiğin "Test" dilimine tıklayıp açılan liste, dilimin sayısıyla uyuşmazdı —
`CASE WHEN` ile predicate iki ayrı doğruluk üretirdi. `IS EMPTY` hiçbir kurala uymayanları,
`IN` ise bunların `OR`'unu döndürür.

**Görünürlük.** Çözücü yalnız kullanıcının erişebildiği zengin filtreleri görür (K14).
Erişilemeyen veya var olmayan ad, bilinmeyen alan hatasıyla aynı biçimde — konumuyla
birlikte — bildirilir; "var ama senin değil" ile "yok" ayırt edilmez.

### K19 — Döngü koruması ve düğüm sayımı

Bir akıllı filtrenin sorgusu başka bir `smart[…]` içerebilir. Bu, hem sonsuz özyinelemeye
hem de birkaç satırla milyonlarca düğümlük bir predicate ağacına açık kapı bırakır.

- Çözümleme yolu `(richFilterId, elementId)` çiftlerinden bir küme taşır; tekrar eden çift
  görülürse döngü hatası verilir.
- Azami iç içe derinlik **3**.
- Genişletilen düğümler K3'teki 200 düğüm tavanına dâhildir. `smart[RF] = "Test"` ifadesi,
  kuralın sırasına bağlı olarak *i+1* alt koşula açılır; tavan bu genişleme sonrası sayılır.

---

## 5. Veri Modeli

### K7 — Bir ana entity + tek çocuk entity (kind ayrımlı), tip başına tablo yok

```java
@Entity @Table(name = "rich_filters")
class RichFilter {
    UUID id;
    Team team;                 // kapsam
    Project project;           // nullable — varsayılan proje kapsamı
    User owner;
    String name;
    String description;
    SavedFilter baseFilter;    // nullable
    String baseQuery;          // nullable — baseFilter yoksa doğrudan STQL
    FilterVisibility visibility;   // PRIVATE | TEAM | PROJECT (mevcut enum)
    List<RichFilterElement> elements;  // position'a göre sıralı
}

@Entity @Table(name = "rich_filter_elements")
class RichFilterElement {
    UUID id;
    RichFilter richFilter;
    RichFilterElementKind kind;   // yeni enum
    String name;
    String query;      // nullable — STQL (SMART_FILTER, STATIC_FILTER seçenekleri)
    String color;      // nullable — #RRGGBB
    Integer position;
    Map<String,Object> config;    // JSONB — kind'e özel alanlar
}
```

```java
enum RichFilterElementKind {
    SMART_FILTER, STATIC_FILTER, DYNAMIC_FILTER,
    VIEW, QUEUE, CUSTOM_VALUE, RATIO, TIME_SERIES
}
```

Gerekçe: sekiz kavramın ortak yanı çoktur (ad, sıra, renk, sorgu, sahiplik). Tip başına
entity açmak sekiz tablo, sekiz repository, sekiz controller demekti; tamamen JSONB'ye
gömmek ise sıralamayı, id ile referanslamayı ve "hangi zengin filtre bu alanı kullanıyor"
sorusunu imkânsız kılardı. Ortada durmak: **tipli ortak sütunlar + tipe özel `config`**.
`ddl-auto: update` altında yeni bir kind eklemek migration gerektirmez.

### `config` şemaları

| kind | config |
|---|---|
| `SMART_FILTER` | `{ "icon": "🔥" }` (sorgu ve renk sütunlarda) |
| `STATIC_FILTER` | `{ "options": [{ "id":"o1", "label":"Bu hafta", "query":"due <= endOfWeek()" }], "multi": false, "default": "o1" }` |
| `DYNAMIC_FILTER` | `{ "field": "assignee", "multi": true, "maxOptions": 25, "sort": "count", "showCounts": true }` |
| `VIEW` | `{ "selection": { … }, "default": true }` |
| `QUEUE` | `{ "smartFilterIds": [...], "columns": ["key","summary","assignee"], "limit": 10 }` |
| `CUSTOM_VALUE` | `{ "kind": "AGE" \| "CYCLE_TIME" \| "TIME_IN_STATUS" \| "FIELD", "field": "storyPoints" }` |
| `RATIO` | `{ "numerator": {"type":"smart","id":"…"}, "denominator": {"type":"all"}, "metric":"count", "target": 0.8, "direction":"higher_better" }` |
| `TIME_SERIES` | `{ "source":"smart", "smartFilterId":"…", "metric":"count", "interval":"day", "window":"90d" }` |

**Sabit filtre seçenekleri neden ayrı entity değil?** Tek diyalogda birlikte düzenlenirler,
sıraları dizi sırasıdır ve dışarıdan yalnız `optionId` ile referans alınırlar. Ayrı tablo
kazanç sağlamaz.

---

## 6. API Yüzeyi

Tanım uçları — `/api/teams/{teamId}/rich-filters`:

| Uç | Açıklama |
|---|---|
| `GET /` | Kullanıcının görebildiği zengin filtreler |
| `POST /` · `GET /{id}` · `PUT /{id}` · `DELETE /{id}` | CRUD |
| `POST /{id}/elements` · `PUT /{id}/elements/{eid}` · `DELETE /{id}/elements/{eid}` | Öğe CRUD |
| `PUT /{id}/elements/reorder` | `["eid1","eid2",…]` — sıra (ilk eşleşen kazanır) |
| `POST /{id}/duplicate` | Kopyala (ekrandaki "Copy rich filter") |

Çalıştırma uçları — hepsi gövdede aynı **seçim nesnesini** alır:

| Uç | Döner |
|---|---|
| `POST /{id}/search` | Sayfalı görev listesi + her görevin `smartTag`'i |
| `POST /{id}/count` | `{ count }` — sayaç widget'ları |
| `POST /{id}/aggregate` | `groupBy: <alan> \| "smart"`, ops. `splitBy` → `[{key,label,color,value}]` |
| `POST /{id}/options` | Tüm dinamik filtrelerin güncel seçenekleri + sayıları (tek gidiş-dönüş) |
| ~~`POST /{id}/ratio/{eid}`~~ | **yazılmadı** — `aggregate` yeterli, bkz. K21 |
| `POST /{id}/series/{eid}` | `[{ date, value }]` |
| `POST /{id}/resolve` | `{ stql, count }` — drill-down linki ve önizleme için |

### K8 — `resolve` ucu: grafikten göreve giden köprü

Bileşik sorguyu **STQL metnine geri yazan** bir `StqlRenderer` (AST → metin) eklenir.
Böylece herhangi bir grafik diliminden "Görevlerde aç" denince kullanıcı, mevcut
`/workList/:teamId?q=…` ekranında tam olarak o kümeyi görür — ayrı bir drill-down ekranı
yazmaya gerek kalmaz, var olan liste/board/sorgu altyapısı olduğu gibi kullanılır.
Bu, K1'de AST düzeyinde birleştirmeyi seçmenin doğrudan kazancıdır.

### K9 — Kısa ömürlü sunucu önbelleği

Bir dashboard'da 8 widget aynı zengin filtreye bağlıysa aynı `WHERE` 8 kez çalışır.
Anahtar `(richFilterId, selectionHash, userEmail, projectId, istekTipi)`, TTL **30 sn**.
`userEmail` anahtarda zorunludur: `currentUser()` fonksiyonu ve görünürlük kuralları
kullanıcıya göre farklı sonuç üretir; ortak anahtar veri sızdırırdı.

> **Uygulama notu (Faz 4).** Önbellek yazılmadı. 28 bin kayıtlık bir takımda sorgular
> milisaniye mertebesinde dönüyor; bu ölçekte önbelleğin tek getirisi bayatlık riski
> olurdu. Karar, ölçülen bir yavaşlık ortaya çıkınca yeniden değerlendirilecek.

### K21 — Oran ve çoklu ölçü ayrı uç istemez

Plan `POST /{id}/ratio/{eid}` öngörüyordu. Yazılmadı: pay ve payda **akıllı filtrelerden**
seçildiği sürece ikisi de `aggregate` yanıtındaki kovalardır — sınıflandırma zaten bütün
kovaları tek `CASE WHEN` sorgusunda üretiyor. Ayrı bir uç, aynı sayıyı ikinci bir kod
yolundan hesaplardı; iki yol zamanla ayrışır ve "gösterge %40 diyor, grafik dilimi başka
diyor" şikâyeti doğardı. Aynı gerekçe `RF_MULTI_STAT` için de geçerli: N ölçü, N sayaç
isteği değil tek gruplama isteğidir.

Bedeli: pay/payda yalnız akıllı filtre (ya da payda için "tüm sonuçlar") olabilir; serbest
STQL paylı oranlar — "story point toplamının saat toplamına oranı" gibi — bu sürümde yok.
Böyle bir ihtiyaç somutlaşırsa uç o zaman eklenir; şimdiden eklemek, kullanılmayan bir
API yüzeyini bakıma mahkûm etmek olurdu.

### K22 — Tazeleme sekme görünürlüğüne bağlı

`refreshInterval` widget yapılandırmasında taşınır (alt sınır 15 sn) ve
`composables/useAutoRefresh.js` tarafından işletilir. Sekme arka plandayken tur **atlanır**,
sekmeye dönüldüğünde bir kez tazelenir. Tarayıcı arka plan zamanlayıcılarını kısar ama
durdurmaz; bu koruma olmadan açık unutulmuş bir pano günlerce sunucuya istek atardı.
Kullanıcı baktığı anda veri yine günceldir — tazelik, isteğin kendisinde değil bakılan
anda olmasında.

---

## 7. Dashboard Entegrasyonu

### Yeni widget tipleri

| Tip | İçerik |
|---|---|
| `RF_CONTROLLER` ✅ | Filtre çubuğu: sabit/dinamik açılır listeler, akıllı filtre çipleri, görünüm seçici, arama |
| `RF_RESULTS` ✅ | Görev listesi — satırlar akıllı filtre rengiyle etiketli |
| `RF_STAT` ✅ | Tek sayı + eşik rengi (roadmap'teki `FILTER_COUNT` bunun içinde erir) |
| `RF_MULTI_STAT` ✅ | Tek kartta birden çok ölçü ("Açık 42 · Bu hafta biten 18 · Geciken 5") |
| `RF_CHART` ✅ | Pasta / halka / çubuk — `groupBy` alan veya `smart` (yığılmış çubuk Faz 7'ye kaldı) |
| `RF_QUEUE` ✅ | Kuyruk paneli — akıllı filtre başına sayaç, pay şeridi ve açılır görev listesi |
| `RF_RATIO` ✅ | Gösterge (gauge) + hedef çizgisi — pay/payda akıllı filtrelerden (K21) |
| `RF_HEATMAP` | İki boyutlu matris (`groupBy` × `splitBy`) — ör. atanan × öncelik |
| `RF_TIME_SERIES` | Zaman serisi çizgisi |

Widget kaydı yine `UserDashboard.layout` JSONB'sinde durur (roadmap K6 kararı korunur):

```json
{ "id":"w7", "type":"RF_CHART", "richFilterId":"…", "groupBy":"smart",
  "chart":"donut", "mode":"exclusive", "viewId":null, "refreshInterval":0,
  "x":0,"y":0,"w":4,"h":3 }
```

### K10 — Etkileşim durumu istemcide, kontrolcü widget'a bağlı değil

Jira, etkileşimli filtrelemenin çalışması için dashboard'a "Rich Filter Controller"
gadget'ının eklenmiş olmasını şart koşar; unutulursa widget'lar sessizce statik kalır.

Bizde seçim durumu `richFilterId` ile anahtarlanmış bir istemci store'unda
(`composables/useRichFilterContext.js`) tutulur. `RF_CONTROLLER` yalnızca bu store'un
**görünen yüzü**dür; olmasa da grafik dilimine tıklayarak çapraz filtreleme çalışır.
Kontrolcü widget'ı ekli olmadığında daraltmanın hangi widget'tan geldiği, bağlı
widget'ların başlığındaki çipte gösterilir ve tek tıkla temizlenir.

### K11 — Seçim URL'ye yansır

`/dashboard?rf=<id>&sel=<kısa kodlanmış seçim>` — bir daraltılmış dashboard olduğu gibi
paylaşılabilir. Bu, sorgu çubuğunun mevcut `?q=` davranışının doğal devamıdır
(bkz. `useTaskQuery.js`).

### K12 — Renkler tek sözlükten gelir

`DASHBOARD_WIDGET_ROADMAP.md` §7'deki "ortak renk paleti" açık sorusunun cevabı budur:

1. Akıllı filtre rengi (yazar seçmiş) — en öncelikli.
2. `WorkflowStatus.color` — durum bazlı gruplamada zaten veritabanında var.
3. Öncelik/tür için sabit sözlük (`utils/chartPalette.js`).
4. Kalanlar için deterministik palet (etiket adının hash'i) — aynı etiket her grafikte
   aynı renkte çıkar.

---

## 8. Zaman Serileri

### K13 — Hibrit: ileriye dönük snapshot + geriye dönük `task_history` yeniden kurgusu

Saf snapshot yaklaşımının bilinen sıkıntısı, özelliği açtıktan sonra grafiğin dolması
için haftalarca beklemektir. Elimizde `task_history` (alan, eskiDeğer, yeniDeğer,
değişimZamanı) var; bu, geçmişi **yeniden kurmayı** mümkün kılıyor.

- **İleriye dönük:** gecelik iş, her `TIME_SERIES` öğesi için o günkü değeri
  `rich_filter_series_points (element_id, bucket_date, value)` tablosuna yazar.
  Mevcut `@EnableScheduling` altyapısı kullanılır (`SubscriptionScheduler` deseni).
- **Geriye dönük (backfill):** akıllı filtrenin sorgusu **yalnız** `task_history`'de
  izlenen alanlara (`status`, `assignee`, `priority`, `resolution`) ve değişmeyen alanlara
  dayanıyorsa, bugünden geriye doğru değişiklikler ters uygulanarak son 180 günün
  değerleri hesaplanır ve aynı tabloya yazılır.
- Kurgulanamayan sorgular için grafik "geçmiş veri yok — bugünden itibaren birikiyor"
  uyarısıyla açılır. Sessizce eksik veri göstermek yerine sınır açıkça söylenir.

Bu ayrım küçük ama önemli: gerçek hayatta zaman serisi istenen akıllı filtrelerin büyük
çoğunluğu durum tabanlıdır (ekrandaki yedi kategorinin altısı gibi), yani ilk günden
dolu grafik verir.

---

## 9. Jira'nın Ötesinde — Öneriler

Aşağıdakiler Jira eklentisinde yok; ScrumTools'un kendi altyapısı sayesinde ucuza gelen
ve ürünü ayrıştıran fikirler.

### Ö1 — Akıllı filtreler STQL'in birinci sınıf alanı olsun → **kabul edildi, Faz 1**

Tasarımı §4'e taşındı: bkz. **K18** (söz dizimi, çözümleme, semantik, görünürlük) ve
**K19** (döngü koruması). Artık öneri değil, çekirdek kapsamın parçası.

### Ö2 — Board ve liste renklendirmesi aynı akıllı filtrelerden beslensin

Mevcut `BoardView` / `ListView` için "renklendirme kaynağı: akıllı filtre" seçeneği.
Dashboard'da tanımlanan kategoriler günlük çalışma ekranlarına taşınır; ek veri modeli
gerekmez, K5'teki `CASE` ifadesi aynen kullanılır.

### Ö3 — Oran widget'ında eşik → bildirim

`RATIO` öğesinin hedefi aşıldığında/altına düştüğünde mevcut `Notification` altyapısıyla
uyarı ("Test aşamasındaki iş oranı %40'ı geçti"). Dashboard'a bakmayı gerektirmeyen tek
özellik budur ve pasif raporu aktif sinyale çevirir.

### Ö4 — Zengin filtre = tam sayfa rapor görünümü

Widget'lara ek olarak `/rich-filters/:id/board` — kuyruklar solda, grafikler sağda, tek
ekran. Toplantıda ekrana yansıtılan "takım sağlık panosu". Dashboard'a widget dizmek
istemeyen kullanıcı için tek tıkla hazır düzen.

### Ö5 — Sınıflandırma denetimi

Zengin filtre editöründe "Denetle" düğmesi: hangi görevlerin hiçbir akıllı filtreye
uymadığını, hangi ikisinin çakıştığını ve sıralamanın hangi görevleri hangi kategoriye
düşürdüğünü gösterir. Yedi JQL parçasını doğru sıralamak gözle zor; bu ekran onu görünür
kılar (K4 kararının kullanıcı tarafındaki tamamlayıcısı).

### Ö6 — Görünüm (view) bazlı widget sabitleme

Bir widget belirli bir görünüme sabitlenebilsin (`viewId`), böylece aynı dashboard'da
"Bu sprint" ve "Bu çeyrek" panelleri yan yana durabilsin — kontrolcüdeki seçimden
etkilenmezler.

### Ö7 — Dışa aktarma

Her widget'ta "CSV indir" ve "PNG indir". Ucuz; rapor paylaşımında en sık gelen istek.

### Ö8 — Özel değerler için ifade motoru **yazmayalım**

Jira'nın custom values özelliği küçük bir ifade dili barındırır. Onun yerine v1'de
`TaskFieldRegistry`'ye türetilmiş dört alan eklenmesi öneriliyor: `age` (açık kalma
süresi), `cycleTime` (oluşturma→çözülme), `timeInStatus`, `dueIn`. Bunlar hem sorgulanabilir
hem grafik metriği olur; ihtiyaçların büyük kısmını karşılar ve bakım maliyeti bir ifade
motorunun onda biridir. Gerçek talep gelirse motor sonra eklenir.

---

## 10. Diğer Mimari Kararlar

### K14 — Görünürlük `SavedFilter` desenini aynen tekrarlar
`PRIVATE` / `TEAM` / `PROJECT`, düzenleme yalnız sahibine açık. Widget seviyesinde ikinci
bir yetki katmanı yok (roadmap K8 ile tutarlı). Zengin filtre paylaşımı, ona bağlı
widget'ları da otomatik olarak paylaşılabilir kılar.

### K15 — Kaynağı silinen widget yetim düşer, silinmez
Zengin filtre silinirse widget "Bu widget'ın zengin filtresi silinmiş" kartına döner
(roadmap K7). Akıllı filtre silinirse ona referans veren widget yapılandırmaları
(`smartFilterIds`, `RATIO` payları) silme anında temizlenir — sunucu tarafında, kayıt
sırasında.

### K16 — Paket sınırı: yeni `PlanFeature.RICH_FILTERS` *(karar verildi)*
FREE'de **salt görüntüleme** — başkasının paylaştığı zengin filtreye bağlı widget'lar,
grafikler ve `smart[…]` sorguları çalışır. **Oluşturma ve düzenleme PRO'dan itibaren.**
Sınır: PRO 10 zengin filtre / her birinde 25 öğe, MAX sınırsız.

Uygulama notu: yetki denetimi yalnız yazma uçlarında (`POST`/`PUT`/`DELETE`) ve editör
ekranının girişinde yapılır; çalıştırma uçları (`search`, `count`, `aggregate`, `options`,
`resolve`) paket denetimine tabi değildir. Salt görüntülemenin açık olması bilinçli:
paylaşılan bir dashboard'un takımın FREE üyelerinde bozuk görünmesi, ürünün yayılmasını
engeller. Aynı sebeple `smart[…]` alanı FREE'de de sorgulanabilir.

### K20 — v1'de rollup/materyalize sayaç yok *(ölçek kararı)*
Referans ölçek: on yıllık birikimle **~28.000 görev**. Bu hacimde `status`/`assignee`
indeksleri üzerinden çalışan `count` ve `GROUP BY` sorguları milisaniyeler mertebesinde;
K5'teki tek `CASE WHEN` taraması da filtrelenmiş küme üzerinde döner. Materyalize sayaç,
özet tablo veya arka plan hesabı **eklenmiyor** — erken optimizasyon, sonuçları bayatlatma
ve tutarsızlık riski getirirdi.

Yeniden değerlendirme eşiği: takım başına **250.000 görev** ya da `aggregate` uçlarında
p95 süresinin **1 sn**'yi aşması. O noktada ilk adım rollup tablosu değil, dinamik filtre
seçeneklerinin (`/options`) ayrı önbelleğe alınmasıdır — tecrübeye göre en pahalı uç odur.
Faz 6'daki snapshot tablosu bu kararın istisnası değildir: o, ölçek için değil, geçmişi
sorgulanamayan veriyi (zaman içindeki durum) saklamak için vardır.

### K17 — `DASHBOARD_WIDGET_ROADMAP.md` bu planla birleşir
Roadmap'teki `FILTER_COUNT` / `FILTER_LIST` / `FILTER_GROUPED_CHART` widget'ları ayrı bir
aile olarak yazılmamalı: **öğesiz bir zengin filtre zaten kayıtlı filtredir**. Roadmap'in
Faz 1–3'ü bu planın Faz 0–2'si içinde erir; `/aggregate` ucu ortak gövdedir. İki paralel
widget ailesi kullanıcıya "hangisini seçeyim" sorusunu sordurur, bize iki kod yolu bırakır.
Roadmap dokümanına bu yönde bir not düşülmeli.

---

## 11. Fazlandırma

| Faz | İçerik | Çıktı | Tahmin |
|---|---|---|---|
| **0** ✅ | `QueryComposer`, `StqlRenderer`, `TaskFieldRegistry`'de `groupable`, `/aggregate` ucu (alan bazlı) | Grafik altyapısı; roadmap Faz 1–3'ün gövdesi | 2–3 gün |
| **1** ✅ | `RichFilter` + `RichFilterElement` entity/CRUD, editör sayfası (Genel + **Akıllı filtreler**), sıralama, renk seçici, `CASE` sınıflandırma, **STQL `smart[…]` alanı** (K18–K19) + otomatik tamamlama | Ekran görüntüsündeki yönetim sayfası; sınıflandırma tüm ürüne açık | 4–5 gün |
| **2** ✅ | `useRichFilterContext`, `RF_CONTROLLER`, `RF_STAT`, `RF_RESULTS`, çapraz filtreleme, URL durumu | **İlk kullanılabilir sürüm** | 3–4 gün |
| **3** ✅ | `RF_CHART` (pasta/halka/çubuk, `groupBy: smart\|alan`), dilime tıkla → daralt, "Görevlerde aç" | Grafikli dashboard | 2–3 gün |
| **4** ✅ | Dinamik filtreler, sabit filtreler, görünümler (`/options` ucu) | Tam etkileşimli kontrol çubuğu | 3 gün |
| **5** ✅ | Kuyruklar, `RF_RATIO` (gauge), `RF_MULTI_STAT`, eşik renkleri, `refreshInterval`, ortak yapılandırma adımı (K21) | Jira paritesi | 3 gün |
| **6** | Zaman serileri: snapshot işi + `task_history` backfill + `RF_TIME_SERIES` | Tarihsel analiz | 4–5 gün |
| **7** | Ö2 (board renklendirme), Ö3 (oran → bildirim), Ö5 (denetim ekranı), `RF_HEATMAP`, dışa aktarma | Ayrıştırıcı özellikler | ayrı değerlendirme |

**Önerilen ilk teslim: Faz 0–3 (~12–15 gün).** Bu dört faz sonunda kullanıcı ekran
görüntüsündeki gibi renkli akıllı filtreler tanımlayıp bunlardan beslenen, birbirine bağlı
grafikli bir dashboard kurabilir; ayrıca `smart[…]` sayesinde aynı sınıflandırmayı görev
listesinde de sorgulayabilir. Faz 4–6 derinlik katar ama Faz 3 sonundaki ürün kendi başına
tamdır.

Faz 1 içindeki sıra önemli: **önce `CASE` sınıflandırma motoru, sonra `smart[…]` alanı.**
İkisi aynı predicate üreticisini paylaşır (K18); alan önce yazılırsa semantik ikinci kez
— ve büyük olasılıkla farklı — kurulur.

---

## 12. Dokunulacak Dosyalar

**Backend** — `com.scrumtools`

| Dosya | Durum |
|---|---|
| `query/QueryComposer.java` | yeni — AST birleştirme (K1) |
| `query/StqlRenderer.java` | yeni — AST → STQL metni (K8) |
| `query/TaskAggregationService.java` | yeni — `groupBy`/`splitBy`, `CASE` sınıflandırma (K5) |
| `query/TaskFieldRegistry.java` | `groupable` bilgisi; `smart[…]` çözümü (K18); Ö8 türetilmiş alanlar |
| `query/FieldType.java` | `SMART_FILTER` tipi |
| `query/QueryContext.java` | `SmartFilterResolver` köprüsü (`SprintResolver` deseni) |
| `query/QueryPredicateBuilder.java` | `Expression` üretimini dışa açan ek; `smart[…]` predicate'i + döngü/derinlik koruması (K19) |
| `query/QuerySuggestionService.java` | `richFilters` ve `smartFilters` öneri kaynakları — editör nitelenmiş adı kendisi yazsın |
| `entity/RichFilter.java`, `entity/RichFilterElement.java` | yeni |
| `entity/enums/RichFilterElementKind.java`, `PlanFeature.RICH_FILTERS` | yeni |
| `repository/RichFilter*Repository.java` | yeni |
| `service/RichFilterService.java` | ✅ yazıldı — CRUD + görünürlük (K14) |
| `service/RichFilterRuntimeService.java` | ✅ yazıldı — seçim çözme, search/count/aggregate/resolve (`/options` Faz 4) |
| `query/QueryFragments.java` | ✅ yazıldı — seçimlerden koşul düğümü üreten fabrika |
| `service/RichFilterSeriesService.java` + zamanlanmış iş | Faz 6 (K13) |
| `controller/RichFilterController.java` | yeni |
| `service/DashboardService.java` | yeni widget tiplerinin doğrulaması, yetim referans temizliği (K15) |

**Frontend** — `frontend/src`

| Dosya | Durum |
|---|---|
| `api/RichFilterApi.js` | ✅ yazıldı (tanım + çalıştırma uçları) |
| `composables/useRichFilterContext.js` | ✅ yazıldı — paylaşılan seçim durumu (K10, K11) |
| `components/dashboard/RfControllerWidget.vue`, `RfStatWidget.vue`, `RfResultsWidget.vue`, `RfChartWidget.vue`, `OrphanRichFilter.vue` | ✅ yazıldı |
| `components/dashboard/RfQueueWidget.vue`, `RfRatioWidget.vue`, `RfMultiStatWidget.vue` | ✅ yazıldı (Faz 5) — üçü de `aggregate` kovalarından beslenir (K21) |
| `components/dashboard/RfWidgetConfigModal.vue` | ✅ yazıldı — tip başına ortak yapılandırma adımı (§13/23) |
| `composables/useAutoRefresh.js` | ✅ yazıldı — `refreshInterval`, sekme görünürlüğüne duyarlı (K22) |
| `pages/RichFilters.vue`, `pages/RichFilterEditor.vue` | ✅ yazıldı — sol menülü yönetim ekranı |
| `components/richfilter/SmartFilterModal.vue` | ✅ yazıldı — sorgu editörü + renk paleti; liste ve sürükleme editör sayfasında |
| `components/richfilter/DynamicFilterEditor.vue`, `StaticFilterEditor.vue`, `ViewList.vue`, `QueueEditor.vue`, `RatioEditor.vue` | Faz 4–5 |
| `components/richfilter/RichFilterToolbar.vue` | yeni — `RF_CONTROLLER` gövdesi |
| `components/dashboard/RfStatWidget.vue`, `RfResultsWidget.vue`, `RfChartWidget.vue`, `RfQueueWidget.vue`, `RfRatioWidget.vue`, `RfTimeSeriesWidget.vue` | yeni |
| `utils/chartPalette.js` | ✅ yazıldı — ortak renk sözlüğü (K12) |
| `pages/Dashboard.vue` | ✅ widget eşlemesi, zengin filtre seçici, ortak yapılandırma adımı, seçim URL senkronu |
| `router.js` | `/rich-filters`, `/rich-filters/:id` |

---

## 13. Kararlar

Verilenler (2026-07-31):

| # | Karar | Nerede |
|---|---|---|
| 1 | **Kapsam takım + `PROJECT` görünürlüğü.** Organizasyon kapsamı v1'de yok; `SavedFilter` yetki kodu olduğu gibi kullanılır. | K14 |
| 2 | **FREE salt görüntüleme, oluşturma PRO'dan itibaren.** PRO 10 zengin filtre × 25 öğe, MAX sınırsız. | K16 |
| 3 | **STQL `smart[…]` alanı Faz 1'e çekildi.** Sınıflandırma en baştan tüm ürüne açık. | K18, K19 |
| 4 | **Rollup yok.** ~28.000 görev referans ölçeğinde canlı `CASE` sorgusu yeterli; eşik 250.000 görev / p95 1 sn. | K20 |

Uygulama sırasında verilen kararlar:

| # | Karar | Gerekçe |
|---|---|---|
| 5 | **Zengin filtre adı takım içinde benzersiz** (büyük/küçük harf duyarsız, DB kısıtı + servis denetimi). | `smart["ad"]` filtreyi adıyla çözüyor; iki aynı adlı kayıt sorguyu belirsiz kılardı. §13/2 kapandı. |
| 6 | **Akıllı filtre adları da zengin filtre içinde benzersiz.** | Aynı sebep: ad, STQL'de değer olarak yazılıyor. |
| 7 | **Paket sınırı sayısal değil, sabit uygulama tavanı** (takım başına 25 zengin filtre, filtre başına 50 öğe). | Plana göre PRO 10 / MAX sınırsız olmalıydı; bu, `Plan` entity'sine `maxRichFilters` sütunu + admin paneli plumbing'i demek. Faz 1'i şişirmemek için özellik kapısı (`PlanFeature.RICH_FILTERS`) yeterli sayıldı, sayısal limit faturalandırma işine bırakıldı. |
| 8 | **Akıllı filtre grafiklerinde sıfır sayılı kategoriler de dönüyor**, sıra kural sırasıdır (değere göre değil). | Kovalar veriden değil yazarın tanımından geliyor; kaybolan dilim, sayının sıfır olduğunu değil kategorinin silindiğini düşündürür. |
| 9 | **Seçimler `smart[…] IN (…)` koşuluna çevriliyor** — ayrı bir "seçim → predicate" yolu yok (`QueryFragments`). | Faz 1'deki sınıflandırma semantiği tek yerde kalıyor; widget'ın gösterdiği sayı ile drill-down listesi zorunlu olarak aynı kümeyi veriyor. |
| 10 | **Görev etiketleri (`smartTags`) ayrı bir sorguda hesaplanıyor**, liste sorgusuna eklenmiyor. | `TaskResponse` şeması bozulmuyor; etiketler yalnız görüntülenen sayfa için, birincil anahtar üzerinden tek sorguda çıkıyor. |
| 11 | **URL'ye tek zengin filtrenin seçimi yazılıyor** (`?rf=&smart=&rfq=`). | Bir panoda pratikte tek kontrolcü olur; hepsini kodlamak URL'yi okunmaz yapardı. Daraltılmış filtre paylaşılır, diğerleri varsayılan açılır. |
| 12 | **Kısa ömürlü sunucu önbelleği (K9) yazılmadı.** | ~28.000 görev ölçeğinde sayaç sorguları milisaniyelik; önbellek bayat veri riskini şimdiden getirirdi. K20'deki eşiklere gelindiğinde eklenecek. |
| 13 | **Dilime tıklamanın anlamı eksene bağlı**: sınıflandırma ekseninde çapraz filtreleme, alan ekseninde görev listesine geçiş. | Seçim nesnesi henüz alan bazlı daraltma taşımıyor (o, Faz 4'teki dinamik filtre). Tıklamayı alan ekseninde işlevsiz bırakmak yerine anlamlı bir çıkış verildi; Faz 4'te bu da daraltmaya döner. |
| 14 | **Kova drill-down'ında STQL metni istemcide birleştiriliyor** — `(<çözülmüş sorgu>) AND (<kova koşulu>)`. | K2'nin istisnası ve tek yeri: üretilen metin görev listesi linkidir, kullanıcının göreceği ve düzenleyebileceği bir sorgudur. Sunucuda yeniden çözümlenir, kapsam denetimi orada zorlanır; iki taraf da parantezlenir. |
| 17 | **Dinamik filtre seçenekleri kendi seçimi dışlanarak hesaplanıyor** (`resolveQuery(..., excludeElementId)`). | "Ahmet" seçiliyken listede yalnız Ahmet kalsaydı, kullanıcı seçimi temizlemeden ikinci kişiyi ekleyemezdi. Diğer kontroller yine uygulanır; seçenekler gerçekten var olan sonuçları gösterir. |
| 18 | **Sabit filtreler tek seçimli.** | "Bu hafta / Bu ay / Bu çeyrek" gibi kontrollerde birden çok seçenek aynı anda anlamlı değil; çoklu seçim gerekirse dinamik filtre zaten var. `config.multi` alanı kaldırıldı. |
| 19 | **Görünümler panodaki kontrolcüden kaydediliyor**, editörde yalnız silinip varsayılan seçiliyor. | Görünüm "şu anki seçim"in fotoğrafı; editörde canlı seçim yok. Varsayılan tektir — ikisi olsaydı panonun hangisiyle açılacağı sıraya kalırdı. Varsayılan yalnız kullanıcı hiçbir şey seçmemişken uygulanır, paylaşılan linkteki seçimi ezmez. |
| 20 | **URL seçimi tek parametreye taşındı**: `?rf=<id>&rfsel=<base64url JSON>`. | Faz 2'deki okunabilir `smart=`/`rfq=` biçimi, dinamik filtre değerleri (e-posta, etiket, sürüm adı) ayraçlarla çakıştığı için yetmiyordu. Kaçış kurallarıyla uğraşmak yerine tek ve kaçışsız bir kodlama seçildi. |
| 16 | **`PlanFeature.RICH_FILTERS` üç yere işlendi**: `PlanService.seedDefaultPlans` (yeni kurulum, PRO), `DataInitializer.grantPlanFeatures` (çalışan kurulumlara backfill, PRO+MAX) ve iki arayüz etiket sözlüğü (`PlanManager`, `BillingTab`). | Enum'a değer eklemek yetmiyor: seed metodu varlık kontrolüyle atladığı için mevcut kurulumların planları özelliği hiç görmüyor ve "paketinizde yok" hatası dönüyor. Arayüz listeleri de elle yazıldığı için özellik panelde görünmüyor — GIT/CI aynı sebeple bir süre atanamamış durumdaydı. |
| 15 | **Grafik renkleri istemcide sabit sözlükten**, sıraya göre değil anahtardan türetiliyor (`utils/chartPalette.js`). | Sıraya göre renk atansaydı bir hafta mavi olan etiket, sıralama değişince ertesi hafta yeşile döner ve panolar karşılaştırılamazdı. §7'deki açık soru (K12) böylece kapandı. |
| 21 | **`/ratio` ucu yazılmadı; oran ve çoklu ölçü `aggregate` kovalarından okunuyor** (K21). | Pay/payda akıllı filtre olduğu sürece ikisi de zaten tek `CASE WHEN` sorgusunun kovaları. Ayrı uç aynı sayıyı ikinci bir kod yolundan hesaplar, iki yol zamanla ayrışır ve "gösterge %40 diyor, dilim başka diyor" durumu doğardı. Bedeli: serbest STQL paylı oranlar bu sürümde yok. |
| 22 | **Kendiliğinden tazeleme sekme görünürlüğüne bağlı** (`useAutoRefresh`, alt sınır 15 sn) (K22). | Tarayıcı arka plan zamanlayıcılarını kısar ama durdurmaz; koruma olmadan açık unutulmuş bir pano günlerce istek atardı. Gizliyken tur atlanır, dönüşte bir kez tazelenir — kullanıcı baktığı anda veri gündemdir. |
| 23 | **Widget yapılandırması tek panelde toplandı** (`RfWidgetConfigModal.vue`); ekleme akışı her zaman üç adım: tip → zengin filtre → ayar. | Faz 4'e kadar yalnız grafiğin yapılandırma adımı vardı ve Dashboard şablonuna gömülüydü. Kuyruk/oran/çoklu ölçü ile birlikte tip başına adım yazmak beş dallı bir şablon demekti; ortak alanlar (başlık, tazeleme) bir kez, tipe özel alanlar tek dalda tanımlanıyor. |

Hâlâ açık:

1. **Takım dashboard'u:** zengin filtre paylaşılabiliyor ama `UserDashboard` hâlâ kullanıcı
   başına tek satır — paylaşılan bir zengin filtreyi herkes kendi dashboard'una elle
   dizmek zorunda. *Öneri:* ayrı iş olarak kalsın; bu plan onu engellemiyor, `visibility`
   deseni oraya doğal genişliyor. Faz 2 sonunda ihtiyacın gerçekten hissedilip
   hissedilmediğine bakılarak karar verilebilir.
2. **Zengin filtre adı benzersiz mi?** `smart["Sprint Sağlığı"]` sorgusu adla çözüldüğü için
   (K18) aynı takımda iki aynı adlı zengin filtre sorguyu belirsiz kılar. *Öneri:* takım
   içinde ad benzersizliği zorlansın — id ile yazım (`smart[<uuid>]`) kullanıcıya sorgu
   yazdırılabilir bir şey değil. Faz 1'de karara bağlanmalı.
