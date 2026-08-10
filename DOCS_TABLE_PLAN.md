# Docs Tablo Planı — "online Excel kadar güçlü"

*2026-08-10 · Hedef: Docs sayfalarındaki tablo deneyimini Word tablosu seviyesinden
Google Sheets seviyesine taşımak*

Bu belge önce **bugün ne var** sorusunu koddan doğrulayarak cevaplıyor, sonra
"Excel kadar güçlü"nün ne anlama geldiğini üç seviyeye ayırıyor ve fazlara
bölüyor. Diğer plan dosyaları gibi kararlar *gerekçeleriyle* yazıldı; altı ay
sonra değerli olan kısım orası.

> **Durum (2026-08-10):** Faz 1 ve Faz 2 **yazıldı ve derlendi**. Faz 0'ın
> derleme ayağı tamamlandı: frontend build geçiyor, backend 99 testle yeşil,
> tablo mantığı için 138 test yazıldı. Elle uçtan uca deneme (tarayıcıda gidiş-
> dönüş turu) **yapılmadı**. Faz 3–5 açık. Ayrıntı için §10.

---

## 1. Bugünkü durum (koddan doğrulandı)

Docs'ta bugün **iki ayrı tablo** var ve ikisi de eksik:

### 1.1 Basit tablo — TipTap

[TiptapEditor.vue:524](frontend/src/components/docs/TiptapEditor.vue#L524):

```js
Table.configure({resizable: true}), TableRow, TableCell, TableHeader
```

Araç çubuğunda olanlar ([TiptapEditor.vue:126-173](frontend/src/components/docs/TiptapEditor.vue#L126-L173)):
satır/sütun ekle-sil, birleştir/ayır, başlık satırı, tabloyu sil. **Hepsi bu.**

Olmayanlar: hizalama, hücre rengi, sütun tipi, sayı biçimi, sıralama, filtre,
toplam satırı, yapışkan başlık, satır/sütun sürükleme, CSV/Excel içe-dışa
aktarma, Excel'den TSV yapıştırma, formül.

CSS de tabloya çalışıyor ([TiptapEditor.vue:821](frontend/src/components/docs/TiptapEditor.vue#L821)):

```css
table-layout: fixed;  width: 100%;  overflow: hidden;
```

`table-layout: fixed` + `width: 100%`, kullanıcının sürükleyerek ayarladığı
`colwidth` değerleriyle çelişiyor; 12 sütunlu bir tablo okunamayacak kadar
sıkışıyor ve `overflow: hidden` yüzünden kaydırılamıyor da.

### 1.2 Hesap tablosu — Univer gömme (Y3)

`COLLAB_WORKSPACE_PLAN.md` Y3 ile gelen gerçek hesap tablosu **zaten yazılmış**:

| Parça | Dosya | Durum |
|---|---|---|
| Univer ↔ Yjs köprüsü | [UniverYjsBridge.js](frontend/src/collab/UniverYjsBridge.js) (940 satır) | Hücre değeri, formül metni, stil, satır yüksekliği, sütun genişliği, birleşme, çok sayfa |
| Gömme düğümü | [collabEmbedExtension.js](frontend/src/components/docs/collabEmbedExtension.js) | HTML'de yalnızca kimlik tutar |
| Gömme görünümü | [CollabEmbed.vue](frontend/src/components/collab/CollabEmbed.vue) | Okuma modu = ham önizleme, canlı mod = düğme arkasında |
| Excel içe/dışa aktarma | `service/collab/sheet/CollabSheetImportService`, `…ExportService` (POI) | Sunucuda hazır |
| Makro motoru | `service/collab/macro/*` + [macroWorker.js](frontend/src/collab/macro/macroWorker.js) | Faz 5'te tamamlandı |

Yani **Excel gücü zaten kod tabanında var** — sadece Docs tarafında ulaşılabilir
değil. Bugünkü sürtünme noktaları:

1. **Docs'tan tablo yaratılamıyor.** [`openEmbedPicker`](frontend/src/components/docs/TiptapEditor.vue#L461)
   yalnızca *var olan* SHEET dokümanlarını listeliyor. Kullanıcı önce Ortak
   Çalışma alanına gidip tablo oluşturmak, sonra Docs'a dönüp gömmek zorunda.
   Bu sürtünme tek başına özelliğin kullanılmamasına yetiyor.
2. **Okuma modu render'ı çok kaba.** [CollabEmbed.vue:94-132](frontend/src/components/collab/CollabEmbed.vue#L94-L132)
   yalnızca hücre *değerlerini* basıyor: stil yok, birleşme yok, sütun genişliği
   yok, sayı biçimi yok, 50×20 sınırı var. Canlı düzenlemede özenle biçimlendirilen
   tablo, okuyan kişiye biçimsiz bir ızgara olarak görünüyor.
3. **Sahiplik yok.** Gömülü tablo bağımsız bir `CollabDocument`; sayfa silinince
   öksüz kalıyor, izinleri ayrı yürüyor.

### 1.3 Doğrulanmamış olan

`frontend/node_modules` içinde **`@univerjs` yok** — `package.json`'da sabitlenmiş
ama hiç kurulmamış. Bu, `IMPROVEMENT_ANALYSIS.md` §2'nin ("hiçbir şey derlenmedi")
bu plana yansıması: aşağıdaki Faz 3'ün tamamı, hiç çalıştırılmamış bir kodun
üzerine inşa ediliyor. **Faz 0 bu yüzden var ve atlanamaz.**

---

## 2. "Excel kadar güçlü" ne demek — üç seviye

Tek bir tablo bileşeniyle hem akış metnindeki 4 satırlık tabloyu hem 10 bin
satırlık finansal modeli karşılamak mümkün değil. Neyin nerede olduğunu net
ayırmak, hem kullanıcı hem kod için daha iyi:

| Seviye | Ne | Nerede saklanır | Tipik kullanım |
|---|---|---|---|
| **S1 — Basit tablo** | Akış metni içinde tablo, biçimlendirme | Sayfa HTML'i | Karşılaştırma tablosu, karar tablosu (bu belgedekiler gibi) |
| **S2 — Veri tablosu** | S1 + sütun tipi, sayı biçimi, sıralama, filtre, toplam satırı | Sayfa HTML'i (`data-*` öznitelikleri) | Sprint metrikleri, maliyet listesi, envanter — 5–500 satır |
| **S3 — Hesap tablosu** | Univer: formüller, çok sayfa, koşullu biçim, makro, xlsx | `CollabDocument` (CRDT) | Gerçek hesaplama, xlsx alışverişi, 500+ satır |

**Kullanıcının "online Excel" dediği şeyin çoğu aslında S2.** Sıralanabilen,
sayıları sağa yaslı ve binlik ayraçlı gösteren, altında toplamı olan, hücresini
renklendirebildiği bir tablo. Formül yazacağı durum azınlıkta — ve o azınlık için
S3 zaten var. Planın ağırlığı bu yüzden S2'de.

---

## 3. Kararlar

| # | Karar | Seçim | Gerekçe |
|---|---|---|---|
| **T1** | Tek "her şeyi yapan" tablo mu, iki blok tipi mi? | **İki blok + tek tıkla dönüştürme** | ProseMirror belge ağacında formül motoru ve 10 bin satır sanallaştırması gerçekçi değil. Tersi de doğru: her tabloya Univer koymak, üç satırlık tablo için ~3 MB paket + bir WS oturumu demek |
| **T2** | S2'ye formül motoru konsun mu? | **Hayır — yalnızca sütun toplayıcıları** (SUM/AVG/COUNT/MIN/MAX) | HyperFormula GPLv3/ticari çift lisanslı; kapalı kaynak SaaS'ta GPL bulaşıcı. MIT alternatifi (`formulajs` + A1 ayrıştırıcı) ise "hücreler arası bağımlılık grafiği + döngü tespiti + yeniden hesaplama sırası" demek, yani ayrı bir ürün. O ürün zaten Univer olarak duruyor |
| **T3** | S2 verisi nerede durur? | **Sayfa HTML'inde**, hücre/sütun öznitelikleri olarak | Sürüm geçmişi (`DocPageVersion`), izinler, arama, dışa aktarma bedavaya gelir. Ayrı tabloya taşımak ikinci bir doğruluk kaynağı yaratırdı |
| **T4** | Sıralama ve filtre kalıcı mı? | **Sıralama kalıcı** (belge sırasını değiştirir), **filtre geçici** (yalnız görüntü) | Kalıcı filtre, okuyan kullanıcıya eksik veriyi *tam* diye gösterirdi — bir tabloda en tehlikeli hata türü bu |
| **T5** | Gömülü tablo okuma modunda canlı mı? | **Hayır** — ama render stil/birleşme/genişlik/sayı biçimini uygular | D3 (tek örnek) bağlantı bütçesi korunur. Bugünkü kayıp *canlılık* değil *görsellik*; düzeltilmesi gereken o |
| **T6** | Docs'tan hesap tablosu yaratılabilsin mi? | **Evet** — araç çubuğundan, sayfaya sahiplenilmiş olarak | Bugünkü "önce Ortak Çalışma'ya git" akışı özelliği pratikte ölü bırakıyor |
| **T7** | Gömülü tablonun izni | **Sayfanın iznini devralır** | `COLLAB_WORKSPACE_PLAN.md` §6'nın "çift yetki kaynağı olmasın" ilkesi. Bugün sayfayı okuyabilen kişi tabloyu göremeyebiliyor ve sebebini anlamıyor |
| **T8** | Yeni öznitelikler | **Dört sanitize noktasında birden** beyaz listeye eklenir | §6'ya bakın — atlanırsa veri *sessizce* kaybolur |
| **T9** | S2 için dış bağımlılık | **Yok** — TipTap eklentisi olarak kendi kodumuz | `IMPROVEMENT_ANALYSIS.md` §5 paket ağırlığı zaten kırmızı; S2'nin tamamı birkaç yüz satır |

---

## 4. Fazlar

### Faz 0 — Doğrulama *(0.5 gün, engelleyici)*

Aşağıdaki her şey mevcut kodun çalıştığı varsayımına yaslanıyor; varsayım
bugün **doğrulanmamış** (§1.3).

- [ ] `npm install` + `npm run build` — Univer/React gerçekten kuruluyor mu, paket boyutu ne
- [ ] `rollup-plugin-visualizer` ile bir kez bakılsın (`IMPROVEMENT_ANALYSIS` §5)
- [ ] Bir Docs sayfasına elle gömme yapılıp canlı mod açılsın — köprü çalışıyor mu
- [ ] `mvn -q package -DskipTests`

**Çıktı:** Faz 3'ün gerçekçi olup olmadığına dair net cevap. Univer patlarsa plan
S1+S2'ye daralır ve bu belge güncellenir.

---

### Faz 1 — Tablo ergonomisi *(S1, ~2 gün)*

Bugünkü tablonun "kullanılabilir" olmadığı yerler. Bunlar özellik değil, **hata
düzeltmesi gibi** ele alınmalı.

| İş | Dosya | Not |
|---|---|---|
| Yatay kaydırma sarmalayıcı | `TiptapEditor.vue` CSS + `DocRenderedContent.vue` | `table-layout: auto` + `min-width` + `overflow-x: auto`. `overflow: hidden` kaldırılır |
| Yapışkan başlık satırı | CSS | `position: sticky; top: 0` — uzun tabloda başlık kayıp gitmez |
| İlk sütunu dondur (opsiyonel) | Tablo öznitelikleri | Geniş tablolarda satırın kim olduğu kaybolmasın |
| Hücre bağlam araç çubuğu | Yeni `TableBubbleMenu.vue` | Bugünkü sabit çubuk, imleç tabloya girdiğinde sayfayı zıplatıyor. `BubbleMenu` seçime yapışır |
| Sağ tık menüsü | Aynı bileşen | Excel/Sheets refleksi: satır ekle/sil sağ tıkta aranıyor |
| Hizalama (sol/orta/sağ) | `TableCell`/`TableHeader` extend → `align` attr | |
| Hücre/satır arka plan rengi | `backgroundColor` attr + 8 renklik palet | Karar tabloları, durum sütunları için |
| Satır/sütun sürükle-taşı | Tutamaç + `moveRow`/`moveColumn` komutu | prosemirror-tables'ta hazır komut yok; hücre taşıma ile yazılır |
| `Enter` → yeni satır (son hücrede) | Klavye eşlemesi | `Tab` zaten var (TipTap varsayılanı) |
| Excel/Sheets'ten TSV yapıştırma | `handlePaste` genişletmesi | HTML tablo yapıştırma ProseMirror'da çalışıyor; **düz metin TSV çalışmıyor**. Ayrıca tabloya yapıştırınca ızgara genişlemeli |
| Tablo başlığı (caption) | Yeni node | Uzun sayfada tabloya atıf yapılabilsin |
| Mobil | CSS + kompakt çubuk | Bugün tablo mobil düzeni kırıyor |

**Bonus:** `TiptapEditor.vue` görev açıklaması ve yorumlarda da kullanılıyor
([TaskDescriptionPanel.vue:32](frontend/src/components/work/panels/TaskDescriptionPanel.vue#L32),
[TaskActivityPanel.vue:49](frontend/src/components/work/panels/TaskActivityPanel.vue#L49),
[AddTaskForm.vue:92](frontend/src/components/work/AddTaskForm.vue#L92)) — bu faz
oraya da düşer. Karşılığında `RichContentViewer.vue` beyaz listesi de
güncellenmek zorunda (§6).

---

### Faz 2 — Veri tablosu *(S2, ~3 gün — planın ağırlık merkezi)*

Yeni bir TipTap eklentisi değil, mevcut tablo düğümlerinin **öznitelik
zenginleştirmesi** + bir hesaplama katmanı.

#### 2.1 Sütun tipleri

`TableHeader` üzerinde `data-col-type` ve `data-col-format`:

| Tip | Davranış |
|---|---|
| `text` | Varsayılan |
| `number` | Sağa yaslı, binlik ayraç, ondalık basamak ayarı |
| `currency` | `number` + para birimi simgesi (proje ayarından varsayılan) |
| `percent` | `number` + `%`, 0–1 mi 0–100 mü açıkça seçilir |
| `date` | Yerelleştirilmiş gösterim, ISO saklama |
| `checkbox` | Onay kutusu; toplayıcıda "kaç tanesi işaretli" |
| `select` | Etiket rozetleri, renk eşlemesi |

Değer **ham** saklanır (`data-v`), gösterim türetilir. Ters yapılsaydı sıralama
ve toplama metin üzerinden çalışırdı — `1.234,50` dizisi `999`dan küçük çıkardı.

#### 2.2 Sıralama

- Başlığa tıkla → artan/azalan/sırasız üçlü döngü.
- **Belge sırasını değiştirir** (T4), yani kaydedilir ve okuyan da aynı sırayı görür.
- Birleştirilmiş hücre içeren tabloda **kapalı** (R3).

#### 2.3 Filtre / arama

- Başlık menüsünden değer seçimi + tablo üstü serbest arama kutusu.
- Yalnız görüntü: satır `hidden` olur, belgeden silinmez.
- Filtre etkinken tablo üstünde kalıcı bir uyarı şeridi: *"3 satır gizli"* —
  aksi hâlde ekran görüntüsü alan kişi eksik veriyi tam sanır.

#### 2.4 Toplam satırı

Sütun altında açılır seçim: `Yok / Toplam / Ortalama / Adet / En düşük / En yüksek / Boş olmayan`.
Sonuç `data-agg` ile saklanır ama **her yüklemede yeniden hesaplanır** — kayıtlı
değere güvenilmez, çünkü sayfa başka bir istemcide düzenlenmiş olabilir.

#### 2.5 Koşullu renk (küçük kapsam)

Yalnızca üç kural: `> eşik`, `< eşik`, `boş`. Daha fazlası S3'ün işi.

#### 2.6 İçe / dışa aktarma

- **CSV/XLSX → tablo**: dosya seç, önizle, ekle. Sunucudaki
  `CollabSheetImportService` (POI) yeniden kullanılır — sadece hücre matrisi
  döndüren bir uç nokta gerekir.
- **Tablo → CSV/XLSX**: `CollabSheetExportService` aynı şekilde.
- Sütun tipleri içe aktarırken **tahmin edilir**, kullanıcı düzeltebilir.

#### 2.7 Boyut sınırı

2000 hücreyi geçen tabloda editörde uyarı: *"Bu tablo büyüdü — hesap tablosuna
dönüştürmek daha iyi çalışır."* Gerekçe R4'te.

---

### Faz 3 — Hesap tablosunu Docs'a getir *(S3, ~3 gün)*

Yeni motor yazılmıyor; var olan Univer gömmesinin **erişilebilir ve düzgün
görünür** hâle getirilmesi.

| İş | Not |
|---|---|
| Araç çubuğunda "Hesap tablosu ekle" | Sunucuda `CollabDocument(type=SHEET)` yaratır, sayfaya sahiplendirir, düğümü ekler, canlı modu açar. Bugünkü "önce Collab'a git" akışı kalır ama artık zorunlu değil |
| Sahiplik ve yaşam döngüsü | `CollabDocument`'a `owner_page_id`. Sayfa silinince tablo arşivlenir; Collab listesinde "Docs sayfasına ait" rozetiyle görünür |
| İzin devralma (T7) | Sahipli tabloya erişim `DocPermission`'dan türetilir. `CollabDocumentAccessResolver` genişletilir |
| **Okuma modu render'ı** | En görünür kazanç. `snapshot_text` zaten `styles` havuzu + `merges` + `rows`/`cols` taşıyor ([UniverYjsBridge.js:600-643](frontend/src/collab/UniverYjsBridge.js#L600-L643)) — render bunları uygulasın. 50×20 sınırı kalksın, yerine "ilk 200 satır + devamı için Aç" |
| Yükseklik ve genişletme | Sürükleyerek yükseklik, tam ekran düğmesi, "sayfa genişliğini aş" seçeneği |
| Sürüm panelinde bağlantı | Sayfa sürüm geçmişi tablonun içeriğini kapsamıyor; `VersionHistory.vue`'da bunu söyleyen bir satır + tablonun kendi geçmişine bağlantı |
| Boş durum metni | "Bu projede gömülebilecek tablo yok" mesajı artık "Yeni oluştur" düğmesiyle birlikte |

---

### Faz 4 — Dönüştürme *(~1.5 gün)*

Üç seviye arasındaki geçiş tek tıkla olmazsa, kullanıcı en baştan doğru seviyeyi
seçmek zorunda kalır — ki seçemez, çünkü tablonun nereye büyüyeceğini bilmez.

- **S1/S2 → S3:** Tablodan hücre matrisi üret → yeni `CollabDocument` → köprünün
  `seedFromModel`'i ile tohumla → düğümü gömme ile değiştir. Sütun tipleri sayı
  biçimine çevrilir.
- **S3 → S2 ("dondur"):** Anlık görüntüden statik tablo üret. Y2'de
  `SHEET → HTML tablo` düzleştirmesi zaten var; oradaki kod yeniden kullanılır.
  Formüller **değerlerine** dönüşür ve kullanıcı bu konuda açıkça uyarılır.
- **Yazdırma/PDF:** Sayfa çıktısında gömülü tablo, önizleme render'ıyla basılır
  (bugün boş çıkıyor).

---

### Faz 5 — Cila *(~1 gün)*

Klavye kısayolları listesi, `th` için `scope` (erişilebilirlik), ekran okuyucu
etiketleri, boş durumlar, yardım baloncukları, tema tutarlılığı.

---

## 5. Efor özeti

| Faz | Kapsam | Süre | Durum |
|---|---|---|---|
| 0 | Doğrulama | 0.5 g | **Açık — engelleyici** |
| 1 | Tablo ergonomisi (S1) | 2 g | ✅ yazıldı |
| 2 | Veri tablosu (S2) | 3 g | ✅ yazıldı (XLSX ve koşullu renk hariç) |
| 3 | Hesap tablosu erişimi (S3) | 3 g | Açık |
| 4 | Dönüştürme | 1.5 g | Açık |
| 5 | Cila | 1 g | Açık |
| | **Toplam** | **~11 gün** | |

Faz 1+2 tek başına teslim edilebilir ve kullanıcının hissettiği farkın büyük
kısmını verir. Faz 3 var olan ama ulaşılamayan bir özelliği açar.

---

## 6. Sessiz veri kaybı tuzağı — dört sanitize noktası

Bu planın en kolay atlanacak ve en pahalıya patlayacak kısmı. Tabloya eklenen
**her yeni öznitelik** dört yerde birden beyaz listeye girmeli; biri unutulursa
öznitelik *kaydedilir ama gösterilmez* ve hata "bazen çalışmıyor" diye gelir:

| # | Yer | Bugünkü hâli |
|---|---|---|
| 1 | [DocPage.vue](frontend/src/pages/DocPage.vue) — `renderedContent` | `colspan, rowspan, colwidth, style` + `data-collab-embed` ailesi |
| 2 | [RichContentViewer.vue:48](frontend/src/components/work/RichContentViewer.vue#L48) | Yalnız `colspan, rowspan` — **`colwidth` bile yok**; görevlerde tablo genişlikleri zaten kayboluyor |
| 3 | [CollabHtmlSanitizer.java](backend/src/main/java/com/scrumtools/service/collab/CollabHtmlSanitizer.java) | `td`/`th` için `colspan, rowspan, colwidth` |
| 4 | `CollabTextEditor.vue` düğüm tanımları | Ortak düzenlemeden geçen sayfa, tanınmayan düğüm/özniteliği düşürür |

**Öneri:** öznitelik listesi tek bir modülde (`docs/tableSchema.js`) tanımlansın,
üç istemci noktası oradan okusun; Java tarafı için aynı liste bir yorumla
eşlenip test edilsin. Faz 1'in ilk işi bu olmalı.

Kabul testi: *tablo oluştur → biçimlendir → kaydet → sayfayı yenile → Ortak
Düzenle ile aç → Docs'a geri kaydet → hiçbir biçim kaybolmadı.*

---

## 7. Riskler

| # | Risk | Etki | Önlem |
|---|---|---|---|
| **R1** | Univer hiç kurulmadı/çalıştırılmadı (§1.3) | Faz 3+4 temelsiz | Faz 0 engelleyici. Patlarsa plan S1+S2'ye daralır — S2 hiçbir dış bağımlılığa yaslanmıyor, bu bilinçli (T9) |
| **R2** | Sanitize beyaz listesi | Sessiz biçim kaybı | §6, tek kaynak + gidiş-dönüş testi |
| **R3** | Sıralama + birleştirilmiş hücre | Bozuk tablo | Birleşme varsa sıralama kapalı ve sebebi araç ipucunda yazılı |
| **R4** | HTML şişmesi | 5000 hücrelik tablo `doc_pages.content` TEXT'ini şişirir ve **her kayıtta `DocPageVersion`'a kopyalanır** | 2000 hücre uyarısı (2.7) + S3'e yönlendirme |
| **R5** | Paket ağırlığı | `IMPROVEMENT_ANALYSIS` §5 zaten kırmızı | S2 dış bağımlılıksız; S3 tembel yüklemede kalır |
| **R6** | D3 tek örnek, WS bütçesi | Çok canlı gömme = çok oturum | T5 korunur: okuma modu asla canlı değil |
| **R7** | Geriye dönük uyum | Eski sayfalar bozulur | Tüm yeni öznitelikler opsiyonel; özniteliksiz tablo bugünküyle birebir aynı davranır |
| **R8** | Mobil | Izgara + dokunmatik zor | S1/S2 mobilde kaydırmalı ve düzenlenebilir; S3 mobilde zaten salt okunur ([CollabSheetEditor.vue:45](frontend/src/components/collab/editors/CollabSheetEditor.vue#L45)) |

---

## 8. Kabul kriterleri

**S1**
- 15 sütunlu tablo sayfayı bozmadan yatay kaydırılıyor, başlık satırı yapışıyor.
- Excel'den kopyalanan bir aralık, düz metin olarak yapıştırıldığında tabloya dönüşüyor.
- Hücre rengi ve hizalama, kaydet-yenile turundan sonra duruyor (§6 testi).
- Satır sürükleyerek taşınabiliyor; `Tab` son hücrede yeni satır açıyor.

**S2**
- `number` sütunu sağa yaslı, binlik ayraçlı; sıralama sayısal olarak doğru.
- Toplam satırı sayfa yenilendiğinde yeniden hesaplanıyor ve doğru.
- Filtre etkinken "N satır gizli" şeridi görünüyor; kaydedilen belgede satır kaybı yok.
- 200 satırlık bir CSV içe aktarılıp XLSX olarak geri alınabiliyor.

**S3**
- Docs araç çubuğundan yaratılan tablo, Collab alanına hiç uğramadan sayfada düzenlenebiliyor.
- Okuma modunda gömülü tablo, canlı moddaki **biçimiyle** görünüyor (renk, birleşme, sütun genişliği, sayı biçimi).
- Sayfayı okuyabilen herkes gömülü tabloyu da okuyabiliyor (T7).
- Sayfa silinince tablo öksüz kalmıyor.

**Dönüştürme**
- S2 tablosu hesap tablosuna çevrildiğinde tüm değerler ve sütun biçimleri taşınıyor.
- Hesap tablosu dondurulduğunda formüller değerlerine dönüyor ve kullanıcı önceden uyarılıyor.

---

## 9. Açık sorular

1. **S2 mi S3 mü önce?** Bu plan S2'yi önceliklendiriyor (Faz 1→2→3). Alternatif:
   Faz 3'ü öne almak — daha az yeni kod, ama Univer riski (R1) baştan üstlenilir.
2. **`select` sütun tipi** proje etiketleriyle mi entegre olsun, serbest mi?
   Entegre olursa görev alanlarıyla ilişki kurulabilir, ama Docs'u iş akışına bağlar.
3. **Paket ayrımı:** S2 tüm paketlerde mi, yoksa `PRO`+ mı? Yeni bir `PlanFeature`
   gerekirse kontrol listesi hatırlatması: enum + seed + `DataInitializer` backfill
   + iki arayüz etiket sözlüğü.
4. **Belge sayısı:** bu, kök dizindeki 10. markdown. `IMPROVEMENT_ANALYSIS` §8'in
   önerdiği README belge tablosu bu planla birlikte eklenmeli.

---

## 10. Teslim durumu — Faz 1 & 2

*2026-08-10 · yazıldı, **derlenmedi ve çalıştırılmadı** (Faz 0 hâlâ açık)*

### Yeni dosyalar

| Dosya | Sorumluluk |
|---|---|
| [tableSchema.js](frontend/src/components/docs/table/tableSchema.js) | Sütun tipleri, sayı/tarih ayrıştırma ve biçimlendirme, toplayıcılar, **sanitize izin listesinin tek kaynağı** |
| [tableUtils.js](frontend/src/components/docs/table/tableUtils.js) | ProseMirror konum yardımcıları: tablo/hücre bulma, satır rolleri, birleşme tespiti |
| [tableExtensions.js](frontend/src/components/docs/table/tableExtensions.js) | Genişletilmiş TipTap düğümleri + 11 komut + toplam hesaplayıcı, normalize edici ve filtre eklentileri |
| [tableClipboard.js](frontend/src/components/docs/table/tableClipboard.js) | TSV/CSV ayrıştırma (tırnak farkındalıklı), HTML tablo → ızgara, CSV üretme |
| [tableView.js](frontend/src/components/docs/table/tableView.js) | Okuma görünümünde kaydırma sarmalayıcısı |
| [TableContextToolbar.vue](frontend/src/components/docs/table/TableContextToolbar.vue) | Seçime yapışan yüzen araç çubuğu |
| [doc-table.css](frontend/src/assets/doc-table.css) | Dört görünümün ortak tablo stili (`.doc-content` kapsamı) |

### Yapılanlar

**S1 — ergonomi**
- [x] Yatay kaydırma sarmalayıcısı; `overflow: hidden` kaldırıldı
- [x] Uzun tabloda (20+ satır) kendi dikey kaydırması + yapışkan başlık
- [x] İlk sütunu dondurma, çizgili ve sık düzen bayrakları
- [x] Yüzen bağlam araç çubuğu (sabit şerit kaldırıldı — sayfa artık zıplamıyor)
- [x] Hizalama ve 7 renklik hücre arka planı
- [x] Satır/sütun taşıma komutları
- [x] Son hücrede `Enter` → yeni satır
- [x] Excel/Sheets'ten **TSV yapıştırma** ve yapıştırırken **ızgarayı büyütme**
- [x] Dört görünümde tek tip tablo stili

**S2 — veri tablosu**
- [x] Yedi sütun tipi: metin, sayı, para, yüzde, tarih, onay kutusu, etiket
- [x] Sütun biçimi: ondalık basamak, para simgesi, binlik gruplama
- [x] Hücreden çıkışta otomatik normalize (`1234.5` → `1.234,50 ₺`)
- [x] Kalıcı sıralama (boşlar her zaman sonda, birleşmiş tabloda kapalı)
- [x] Toplam satırı: toplam / ortalama / en düşük / en yüksek / adet / dolu hücre —
      belgeye yazılır, **her değişiklikte yeniden hesaplanır**
- [x] Görüntü filtresi + "N satır gizli" uyarısı
- [x] CSV içe/dışa aktarma (BOM'lu UTF-8 — Excel Türkçe karakterleri bozmasın)

**§6 — dört sanitize noktası**
- [x] `DocPage.vue`, `RichContentViewer.vue`, `CollabTextEditor.vue`,
      `CollabHtmlSanitizer.java` — hepsi tek listeye bağlandı
- [x] Fazladan iki nokta bulundu ve düzeltildi: `VersionHistory.vue` (sürüm
      önizlemesi tüm `data-*`'ları süzüyordu) ve `TiptapEditor` markdown içe
      aktarma önizlemesi
- [x] `RichContentViewer` bugüne kadar `colwidth`'i bile geçirmiyordu — yani
      görevlerdeki tabloların sütun genişlikleri zaten kayboluyordu

### Bilinçli olarak yapılmayanlar

| Konu | Neden |
|---|---|
| Tablo başlığı (caption) | TipTap'te düğüm tanımı gerekiyor; CSS hazır, düğüm yok. Markdown'dan gelen `<caption>` zaten doğru görünüyor |
| Sürükle-bırak satır/sütun tutamağı | Komutlar ve araç çubuğu düğmeleri var; tutamak ayrı bir etkileşim katmanı |
| Koşullu renk (2.5) | Kapsamın en az kullanılan parçası; sütun tipi + hücre rengi çoğu ihtiyacı karşılıyor |
| XLSX içe/dışa aktarma | CSV yolu çalışıyor; XLSX için sunucudaki POI servislerine bir uç nokta gerekiyor (Faz 3 ile birlikte) |
| Okuma modunda filtre | Filtre editörde yaşıyor. Okuma modunda çalışması için sayfaya JS eklemek gerekir; sıralama zaten kalıcı olduğu için acil değil |

### Doğrulama sonuçları

| Kontrol | Sonuç |
|---|---|
| `npm run build` | ✅ 4705 modül, ~60 sn. **Projenin ilk başarılı frontend derlemesi** — `IMPROVEMENT_ANALYSIS` §2'nin kapanışı. `@univerjs` de sorunsuz çözüldü (yalnızca `node_modules` bayattı, `package-lock` doğruymuş) |
| Backend derleme + test | ✅ 99 test, 0 hata |
| Saf fonksiyon testleri | ✅ 78 — sayı/tarih ayrıştırma, biçimlendirme, toplayıcılar, CSV/TSV |
| Komut testleri | ✅ 60 — sıralama, toplam hesaplayıcı, normalize, yapıştırma, taşıma, filtre |
| `CollabHtmlSanitizerTest` | ✅ 4 — **repoya eklendi**, §6'yı kalıcı olarak koruyor |

**Ortam notu:** `JAVA_HOME` JDK 17'yi gösteriyor, proje Java 25 hedefliyor —
maven `release version 25 not supported` ile düşüyor. Derlemek için
`JAVA_HOME=C:\Program Files\Java\jdk-25.0.3` gerekiyor. Kod sorunu değil ama
Jenkins ajanında da aynı tuzağın olup olmadığına bakılmalı.

### Derleme sırasında bulunan iki gerçek hata

**1. Sütun genişlikleri okuma modunda hiç çalışmıyormuş.** TipTap genişliği
hücrenin `colwidth` özniteliğinde saklıyor ve `getHTML()` çıktısına `<colgroup>`
koymuyor — `<colgroup>`'u yalnızca editördeki NodeView çiziyor. `colwidth` ise
standart bir HTML özniteliği değil, tarayıcı onu yok sayıyor. Yani sanitize
listelerinde `colwidth` bunca zamandır dururken bile kullanıcının sürükleyerek
ayarladığı genişlikler okuma modunda uygulanmıyordu. `tableView.js` artık
`colwidth`'ten DOM'da `<colgroup>` üretiyor.

**2. Tablo düzeyi öznitelikler editörde DOM'a yazılmıyordu.** prosemirror-tables
`<table>` elemanını NodeView içinde elle üretiyor. Düzeltilmeseydi "Çizgili" ve
"İlk sütunu dondur" düğmeleri editörde hiçbir şey yapmıyor, ama sayfa kaydedilip
okumaya geçilince ayar uygulanmış görünüyor olacaktı. NodeView sarmalandı
(`view.dom.querySelector('table')`) — sürüme bağımlı tek nokta burası.

### Bir kapsam kararı: `style` özniteliği

Sanitize listesi ortaklaştırılırken `style` görev açıklaması ve yorumlarına da
açılıyordu. Yorum yazabilen herkesin `position: fixed` ile arayüzün üzerine
bindirme yapabilmesi demek olurdu bu. Ayrıştırıldı: `DOC_CONTENT_SANITIZE_CONFIG`
(Docs — HTML kaynak modu olduğu için `style` korunuyor, mevcut sayfalar
gerilemiyor) ve `RICH_CONTENT_SANITIZE_CONFIG` (görevler — `style` yok).

### Hâlâ elle denenmesi gerekenler

1. Gidiş-dönüş turu: tablo oluştur → biçimlendir → kaydet → yenile →
   "Ortak Düzenle" → Docs'a geri kaydet → hiçbir biçim kaybolmadı.
2. Yüzen araç çubuğunun konumlanması (kaydırma, pencere boyutu, mobil).
3. NodeView sarmalaması gerçek Univer/TipTap sürümüyle (yukarıdaki 2. hata).
4. Excel'den gerçek bir aralık kopyalayıp yapıştırma.
