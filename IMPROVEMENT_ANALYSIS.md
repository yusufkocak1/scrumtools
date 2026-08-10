# ScrumTools — İyileştirme Analizi

*2026-08-07 · Faz 5 sonrası durum taraması*

Bu belge bir yol haritası değil, **mevcut durumun dürüst bir envanteri**. Her
madde ya kodda doğrulandı ya da doğrulanamadığı açıkça yazıldı. Sıralama
etkiye göre: en üsttekiler ürünü bugün riske atanlar, aşağıya doğru
"olsa iyi olur"a iniyor.

Ölçek referansı: 620 Java dosyası, 183 Vue bileşeni, 92 JS modülü.

---

## 1. 🔴 Yetkilendirme: kimlik var, kapsam yok

**En kritik bulgu ve tek başına diğerlerinin hepsinden önemli.**

`JwtAuthFilter` isteği *kimliklendiriyor* ama hiçbir yerde "bu kullanıcı bu
takıma ait mi" diye sorulmuyor. Somut olarak:

| Ölçüm | Değer |
|---|---|
| Kontrolör sayısı | 56 |
| `@PreAuthorize` içeren kontrolör | **6** |
| `teams/{teamId}` yolu olup yetki kontrolü olmayan kontrolör | **21** |

Doğrulanmış örnek — [TaskQueryService.java:61](backend/src/main/java/com/scrumtools/query/TaskQueryService.java#L61):

```java
teamRepository.findById(teamId)
        .orElseThrow(() -> new IllegalArgumentException("Takım bulunamadı: " + teamId));
```

Takımın **var olduğu** kontrol ediliyor, kullanıcının o takımda olduğu değil.
`SprintService.getSprintsByTeam` ise doğrudan repository'ye gidiyor, hiçbir
kontrol yok. Kontrolörlerde de `@PreAuthorize` yok, ortada bir aspect ya da
filtre yok (tarandı: yalnızca `JwtAuthFilter` var).

**Sonuç:** kimliği doğrulanmış herhangi bir kullanıcı, UUID'sini bildiği
*herhangi* bir takımın görevlerini, sprintlerini, retro panolarını ve board'unu
okuyabiliyor. Çok kiracılı (multi-tenant) bir SaaS'ta bu, paket/faturalama
sınırlarından daha ciddi bir sorun.

**İyi haber:** mekanizma zaten yazılmış.
[ProjectSecurityEvaluator](backend/src/main/java/com/scrumtools/security/ProjectSecurityEvaluator.java)
`hasPermission(authentication, projectId, permission)` sunuyor ve 6 kontrolörde
kullanılıyor. Eksik olan mekanizma değil, **uygulanması**.

**Önerilen yaklaşım — tek tek anotasyon değil, varsayılanı tersine çevirmek:**

1. Takım kapsamı için `TeamSecurityEvaluator` yaz (`isMember(auth, teamId)`),
   `ProjectSecurityEvaluator` deseninin birebir eşi.
2. `@PreAuthorize`'ı tek tek 21 kontrolöre eklemek yerine `/api/teams/{teamId}/**`
   için bir `HandlerInterceptor` kur: yol değişkeninden `teamId`'yi çıkar,
   üyeliği doğrula, değilse 403. **Yeni bir kontrolör eklendiğinde kendiliğinden
   korunur** — anotasyon unutulabilir, interceptor unutulamaz.
3. İstisna listesi (varsa) açıkça yazılsın; "bilinmeyen yol reddedilir" ilkesi
   `WebSocketSubscriptionAuthorizer`'da zaten uygulanmış, aynı ilke burada da
   geçerli olmalı.

> **Not:** Bu, Faz 4/5'in makro motorunu da doğrudan ilgilendiriyor.
> `ScrumTools.tasks.query({ teamId })` bu boşluğu bir döngüyle sömürülebilir
> hâle getiriyor. Makro tarafında ek bir kontrol koymak **yanlış çözüm** olur;
> doğru yer yukarıdaki katman.

---

## 2. 🔴 Hiçbir şey derlenmedi, hiçbir şey çalıştırılmadı

Faz 0–5 boyunca (≈64 adam-günlük iş) tek bir `mvn package` ya da `npm run build`
koşmadı — bu senin bilinçli tercihindi ve tercihe uydum, ama teslimatın durumu
bu: **6 fazın tamamı derlenmemiş kod.**

Bilinen somut riskler:

- **GraalJS sürümü** (`polyglot` + `js-community` 24.1.2) Java 25 ile hiç
  denenmedi. Faz 5'in en olası derleme/çalışma zamanı arızası bu.
- **Univer 0.25.1 mutation kimlikleri** (R1) — köprü sabitleri içe aktarıyor,
  ama paketin bu sabitleri gerçekten dışa aktardığı doğrulanmadı.
- **Makro kaydedicinin komut kimlikleri** dize; yanlışlarsa sessizce
  "kaydedilemedi"ye düşer, yani *fark edilmesi zor* şekilde yarım çalışır.
- **`COLLAB_WORKSPACE_PLAN.md` §15 kabul kriterlerinin hiçbiri** doğrulanmadı.
- `tools/collab-loadtest/` yazıldı, anlamlı bir koşusu yok.

**Öneri:** en azından bir kez `mvn -q package -DskipTests` + `npm run build`
koş. Bu, "build kontrolü yapma" tercihini bozmaz — o tercih *her değişiklikten
sonra* koşmamakla ilgiliydi; altı fazın sonunda bir kez bakmak farklı bir şey.

---

## 3. 🟠 Test: pratikte yok

10 test dosyası var ve hepsi **saf fonksiyonlarda** yoğunlaşmış:

```
query/   → QueryLexerTest, QueryParserTest, TaskFieldRegistryTest, LegacyFilterTranslatorTest
scm/     → ScmUrlValidatorTest, ScmWebhookSignatureTest, ScmTokenCryptoTest, ScmBranchNamesTest, ScmCommitLinkerTest
mail/    → PostForgeWebhookSignatureTest
```

Yazılanlar iyi seçilmiş (ayrıştırıcı, imza, kripto — hata yapılması kolay ve
sessiz olan yerler). Ama **tek bir servis, kontrolör ya da yetki testi yok** ve
frontend'de hiç test yok.

Yüz test yazmanın kimseye faydası olmaz. Değeri en yüksek üç tanesi:

1. **Yetki testleri** (§1 düzeltilirken): "başka takımın görevini isteyen 403
   alır". Bu testler olmadan §1'in düzeltmesi de zamanla aşınır.
2. **`CollabUpdateBatcher` eşzamanlılık testi**: `enqueue`/`flush` yarışı, sıra
   numarası ayırmanın tekilliği. Kod yorumları bu yarışın farkında —
   `synchronized` ve `compute` seçimleri bilinçli — ama doğrulanmamış.
3. **Makro sandbox kaçış testi**: `fetch`, `importScripts`, `constructor.constructor`
   üzerinden kaçış denemeleri. Güvenlik sınırının test edilmemesi, olmamasıyla
   aynı kapıya çıkar.

---

## 4. 🟠 Şema yönetimi: `ddl-auto: update` + 7 elle göç

`application.yml` prod'da `ddl-auto: update` kullanıyor ve Flyway/Liquibase
**yok** (doğrulandı). Bunun yerine 7 adet `ApplicationRunner` göç sınıfı var:

```
SchemaConstraintFixRunner, DashboardMigrationRunner, BillingDataMigrationRunner,
TaskProjectBackfillRunner, MediaUrlBackfillRunner, CollabCleanupRunner, CollabInstanceGuard
```

Bu desen bugüne kadar çalıştı, ama asıl bedeli **teşhiste** ödetiyor: liste ekranı
`lower(bytea) does not exist` ile patladığında ilk akla gelen açıklama
`collab_documents.snapshot_text`'in canlıda `bytea` olarak oluşmuş olmasıydı ve
`SchemaConstraintFixRunner`'a bir `ALTER` yazıldı. Gerçek sebep şema değil,
`search()` sorgusuna null geçilen `:query` parametresinin Hibernate'te tipsiz
kalıp `VARBINARY` bağlanmasıydı — yazılan göç üç gün boyunca hiçbir şey yapmadan
canlıda durdu. Şemanın gözden geçirilebilir bir dosyada olmaması, "acaba kolon mu
kaydı?" sorusunun cevaplanmasını da zorlaştırıyor.

`ddl-auto: update`'in yapısal sınırları:
- Kolon **tipini** değiştirmez.
- Kolon **silmez** (ölü `hangman_words.team_id` bu yüzden hâlâ orada).
- Enum `CHECK` kısıtlarını genişletmez (`SchemaConstraintFixRunner`'ın varlık
  sebebi tam olarak bu).
- Göçlerin **sırası ve bir kez çalıştığı garanti değil** — hepsi idempotent
  yazılmak zorunda, ki bu her yeni göçte tekrar tekrar dikkat gerektiriyor.

**Öneri:** Flyway ekle, `ddl-auto: validate`'e geç. Geçiş maliyeti tek seferlik:
mevcut şemadan bir `V1__baseline.sql` üret, 7 runner'ı `V2__…` serisine çevir.
Karşılığında şema değişikliği *gözden geçirilebilir bir dosya* olur ve canlı ile
entity arasındaki sürüklenme derleme zamanında yakalanır.

---

## 5. 🟠 Frontend paket ağırlığı

Faz 2.5'ten beri derlemeye giren ağır bağımlılıklar: **Monaco**, **Univer**
(+ React 18 + rxjs, Univer'in peer bağımlılıkları), **TipTap** eklenti ailesi,
**Chart.js**, **lowlight**.

Bugün alınmış doğru kararlar: Univer `CollabSheetEditor`'da tembel yükleniyor,
Y3'ün okuma modu Univer'siz çalışıyor (bu hafta bir import'un bunu sessizce
iptal ettiği yakalandı ve düzeltildi).

Yapılmamış olanlar:

- **Paket analizi hiç çalıştırılmadı.** `rollup-plugin-visualizer` ekleyip bir
  kez bakmak, buradaki tüm tahminleri gereksiz kılar.
- **React yalnızca Univer için var.** Univer'in UI'ı React tabanlı; Vue
  uygulamasında ikinci bir çatı taşımak kalıcı bir vergi. Kısa vadede kabul —
  ama `preset-sheets-core` yerine yalnızca ızgara çekirdeğini kullanmak mümkün
  mü, bir kez araştırılmalı.
- **Jenkins ajanı dar.** Derleme OOM olursa ilk düğme `frontend/Dockerfile`
  build aşamasında `NODE_OPTIONS=--max-old-space-size`.

---

## 6. 🟡 Tek örnek kısıtı (D3) ve faturası

`COLLAB_WORKSPACE_PLAN.md` D3/K9 gereği: Redis yok, tek backend örneği, yatay
ölçek yasak. Mimari buna göre kuruldu ve tutarlı. Ama faturası şu:

- **Deploy = tüm ortak düzenleme oturumlarının düşmesi.** Veri kaybı yok
  (append log'da), ama kullanıcı deneyimi olarak her deploy bir kesinti.
- **STOMP broker süreç içi** — bildirimler, retro, poker, quiz hepsi aynı
  örnekte.
- **Makro kuyruğu, Excel kuyruğu ve HTTP isteği aynı CPU'yu paylaşıyor.**

Bunlar bilinen ve kabul edilmiş takaslar; burada listelenme sebepleri "ne zaman
gözden geçirilmeli" sorusuna bir eşik koymak:

> Eşzamanlı ortak düzenleyici sayısı düzenli olarak 20'yi geçtiğinde ya da
> deploy sıklığı günde birkaç kez olduğunda D3 yeniden tartışılmalı.

Yeni eklenen **admin → Ortak Çalışma** ekranı (§12) tam da bu eşiği görmek için
var: açık bağlantı, güncelleme/sn, heap doluluk, en büyük dokümanlar.

---

## 7. 🟡 Ortak Çalışma modülünün bilinen boşlukları

Faz 0–5 tamamlandı ama şunlar açık:

| Konu | Durum |
|---|---|
| `Landing.vue` ortak çalışma kartı | **Yok** — Faz 0'da "Kod Paylaşımı" kartı silindi, yerine yenisi konmadı. Kodda yalnızca bir yorum satırı var |
| `ON_SCHEDULE` tetikleyicisi | Enum'da var, **hiçbir zamanlayıcı yok**. Kullanıcı cron yazabilir, hiçbir şey olmaz |
| Makro kaydedici komut eşlemesi | Univer'in gerçek komut kimliklerine karşı **doğrulanmadı**; yanlışsa sessizce "kaydedilemedi"ye düşer |
| Sunucu makrosunda doküman erişimi | Bilerek yok (K2). Kullanıcıya bunu *önceden* anlatan bir arayüz metni yok — hatayı ancak çalıştırınca görüyor |
| Excel içe aktarmada uyum raporu | Plan §10'da vaat edildi ("kullanıcı bir uyum raporu görür"), arayüzde karşılığı belirsiz |
| `collab_updates` sıkıştırma görevi | Eşikler tanımlı; gerçekten tetiklendiği **gözlenmedi** |

---

## 8. 🟡 Belge enflasyonu

Kök dizinde 8 markdown var: `COLLAB_WORKSPACE_PLAN`, `RICH_FILTER_PLAN`,
`JENKINS_INTEGRATION_PLAN`, `REFERENCE_ARCHITECTURE`, `DASHBOARD_WIDGET_ROADMAP`,
`TASK_QUERY_LANGUAGE`, `DEPLOYMENT`, `README` (+ bu dosya).

Bunların kalitesi yüksek — özellikle karar tabloları, çünkü *neden* sorusunu
cevaplıyorlar ve altı ay sonra asıl değerli olan o. Sorun içerik değil
**gezinme**: hangisi güncel, hangisi tamamlanmış, hangisi hâlâ yol haritası
belli değil.

**Öneri (ucuz):** `README.md`'ye bir "Belgeler" tablosu — her dosya için tek
satır: kapsam, durum (tamamlandı / devam / fikir), son güncelleme.

---

## 9. Öncelik özeti

| # | Konu | Etki | Maliyet | Öneri |
|---|---|---|---|---|
| 1 | Takım kapsamı yetkilendirmesi | **Çok yüksek** — kiracılar arası veri sızıntısı | ~2 g | Hemen |
| 2 | Bir kez derle ve çalıştır | Yüksek — 64 g'lik iş doğrulanmamış | ~0.5 g | Hemen |
| 3 | Yetki + sandbox testleri | Yüksek | ~2 g | §1 ile birlikte |
| 4 | Flyway'e geçiş | Orta-yüksek (birikiyor) | ~2 g | Yakında |
| 5 | Paket analizi + `NODE_OPTIONS` | Orta | ~0.5 g | Yakında |
| 6 | `ON_SCHEDULE` ya da enum'dan kaldır | Orta — vaat edilip yapılmamış özellik | ~1 g | Yakında |
| 7 | Landing kartı | Düşük-orta (satış yüzü) | ~0.5 g | Sırada |
| 8 | README belge tablosu | Düşük | ~0.2 g | Sırada |

---

## Kapanış notu

Bu kod tabanının en güçlü yanı, kararların **gerekçeleriyle** yazılmış olması —
plan dosyalarındaki karar tabloları ve kod içi yorumlar, "neden böyle" sorusunu
altı ay sonra da cevaplayacak durumda. Bu nadir ve korunmaya değer.

En zayıf yanı ise doğrulama: 620 Java dosyası, 10 test ve sıfır derleme koşusu.
Özenle tasarlanmış ama hiç sınanmamış bir sistem, sınanmış basit bir sistemden
daha kırılgandır. Yukarıdaki listede 2 ve 3 numaralı maddelerin 1'den hemen
sonra gelmesinin sebebi bu.
