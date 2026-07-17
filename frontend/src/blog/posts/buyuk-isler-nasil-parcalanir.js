// Blog makalesi: Büyük İşler Nasıl Parçalanır?
export default {
    slug: 'buyuk-isler-nasil-parcalanir',
    title: 'Büyük İşleri Nasıl Parçalamalıyım? Sprint\'e Sığmayan İşler İçin Parçalama Rehberi',
    metaTitle: 'Büyük İşler Nasıl Parçalanır? Story Splitting | ScrumTools Blog',
    description: 'Bir sprint\'e sığmayan büyük işler nasıl parçalanır? Dikey dilimleme, INVEST, SPIDR teknikleri ve üç sprint\'lik gerçek bir özelliğin adım adım dilimlenmesi bu rehberde.',
    category: 'Backlog Yönetimi',
    date: '2026-07-17',
    readingMinutes: 11,
    excerpt: 'Bir sprint\'e sığmayan iş sprint\'e alınmaz; sprint\'e sığacak, tek başına değer üreten dilimi alınır. Bu yazıda dikey dilimleme ilkesini ve parçalama tekniklerini anlatıyor, ardından hepsini üç sprint\'lik gerçek bir özellik — retro panosu — üzerinde adım adım uyguluyoruz.',
    related: ['urun-is-listesi-nasil-organize-edilmeli', 'story-point-nedir', 'analiz-gelistirme-test-ayni-sprintte-mi'],
    html: `
<p>Her takımın refinement oturumlarında yaşadığı bir an vardır: Masaya gelen iş kalemi tartışıldıkça büyür ve birileri kaçınılmaz cümleyi kurar — <em>"Bu iş bir sprint'e sığmaz."</em> Bu noktada iki yol vardır: İşi olduğu gibi sprint'e alıp "elimizden geleni yaparız" demek ya da işi <strong>parçalamak</strong>. İlk yol, sprint sonlarında "yüzde 80 bitti" cümleleriyle, taşan işlerle ve güvenilirliğini yitiren sprint hedefleriyle sonuçlanır. İkinci yol ise öğrenilebilir bir zanaattır ve bu yazının konusudur.</p>

<p>Kural basittir: <strong>Bir sprint'te bitmeyecek iş, sprint'e o haliyle alınmaz.</strong> Scrum'da sprint'e giren her Product Backlog kalemi, o sprint içinde Bitti Tanımı'na (Definition of Done) ulaşabilecek boyutta olmalıdır. Büyük işler backlog'un derinliklerinde büyük durabilir; ancak sprint'e yaklaştıkça refinement sürecinde küçültülmeleri gerekir.</p>

<h2>Neden Küçük Parçalar?</h2>

<ul>
<li><strong>Erken geri bildirim:</strong> İki haftada bir çalışan küçük bir dilim görmek, altı hafta sonra büyük bir sürprizle karşılaşmaktan her zaman iyidir. Yanlış yöne gidiliyorsa bunu ilk dilimde öğrenirsiniz.</li>
<li><strong>Daha isabetli tahmin:</strong> Tahmin hatası, iş büyüdükçe orantısız artar. Üç günlük bir işin tahmini yüzde 30 şaşabilir; üç haftalık bir işinki yüzde 300 şaşabilir.</li>
<li><strong>Akış ve moral:</strong> Küçük işler panoda akar; takım her hafta bir şeyleri gerçekten "bitirir". Sprint sonunda üç işin tamamının bitmesi, altı işin yarısının bitmesinden hem ölçüm hem motivasyon açısından değerlidir.</li>
<li><strong>Risk yönetimi:</strong> Küçük dilimler halinde entegre olan iş, dev bir birleştirmenin (big bang merge) entegrasyon riskini taşımaz.</li>
</ul>

<h2>Altın Kural: Dikey Dilimleme</h2>

<p>Parçalamanın en önemli ilkesi, işi <strong>dikey dilimlemektir</strong> (vertical slicing). Pasta metaforuyla düşünün: Pastayı katlarına ayırmazsınız (önce tüm pandispanya, sonra tüm krema), dikine keser ve her dilimde tüm katlardan bir parça bulursunuz. Yazılımda da her dilim; arayüzden, servis katmanından ve veriden ince bir kesit içermeli ve <em>tek başına çalışan, gösterilebilir bir değer</em> üretmelidir.</p>

<p>Bunun tersi olan yatay dilimleme — "önce veritabanı tablolarını yapalım, sonra API'yi, en son ekranları" — parçalama değil, faz planlamasıdır: Hiçbir parça tek başına test edilemez, gösterilemez ve değer üretmez. İyi bir dilimin sağlaması için <strong>INVEST</strong> kriterleri kullanılır: <em>Independent</em> (bağımsız), <em>Negotiable</em> (müzakere edilebilir), <em>Valuable</em> (değerli), <em>Estimable</em> (tahmin edilebilir), <em>Small</em> (küçük), <em>Testable</em> (test edilebilir).</p>

<h2>Parçalama Teknikleri</h2>

<p>Mike Cohn'un <strong>SPIDR</strong> (Spike, Path, Interface, Data, Rules) yaklaşımı, en işlevsel teknikleri beş başlıkta toplar; aşağıdaki liste bunları ve sahada sık kullanılan birkaç tekniği içerir:</p>

<ul>
<li><strong>İş akışı adımlarına göre (Path):</strong> "Sipariş verme" işini akışın adımlarına bölün: sepete ekleme, adres seçimi, ödeme, sipariş onayı. İlk dilim olarak uçtan uca çalışan en yalın akışı (walking skeleton) hedefleyin; zenginleştirmeyi sonraki dilimlere bırakın.</li>
<li><strong>İş kurallarına göre (Rules):</strong> "Ödeme al" işini kural varyasyonlarına bölün: önce tek çekim kredi kartı, sonra taksit seçenekleri, sonra kupon ve indirim kuralları.</li>
<li><strong>Mutlu yol önce:</strong> İlk dilimde her şeyin yolunda gittiği senaryoyu bitirin; hata durumları, sınır kontrolleri ve istisnalar sonraki dilimlere.</li>
<li><strong>Veri çeşitlerine göre (Data):</strong> Önce tek dosya formatı, tek dil, tek para birimi; diğer varyasyonlar ayrı dilimler.</li>
<li><strong>Arayüz çeşitlerine göre (Interface):</strong> Önce tek platform (ör. web) veya yalın bir arayüz; mobil uyumluluk ve arayüz cilası ayrı dilimler.</li>
<li><strong>CRUD ayrımı:</strong> "Kullanıcı yönetimi" gibi bir iş; oluşturma/listeleme ile güncelleme/silme olarak bölünebilir — çoğu zaman ilk ikisi değerin büyük kısmını taşır.</li>
<li><strong>Kabul kriterlerine göre:</strong> Bir işin beş kabul kriteri varsa, her biri potansiyel bir dilimdir. Kriterleri gruplayarak iki-üç anlamlı parça çıkarmak çoğu zaman mümkündür.</li>
<li><strong>Spike ayırın:</strong> İş, büyük bir bilinmezlik içeriyorsa önce zaman kutulu bir araştırma işi (spike) tanımlayın. Spike'ın çıktısı kod değil <em>öğrenmedir</em>: teknik yaklaşım kararı ve artık tahmin edilebilir hale gelmiş dilimler.</li>
</ul>

<h2>Gerçek Bir Örnek: Retro Panosu Özelliğini Üç Sprint'te Dilimlemek</h2>

<p>Teknikler soyut kalmasın diye, bu blogun üzerinde yaşadığı üründen gerçek boyutlu bir işi ele alalım: <strong>ScrumTools'a retro panosu eklemek</strong>. İşin backlog'a ilk düşen hali tek cümledir: <em>"Takımlar sprint sonunda çevrimiçi retrospektif yapabilsin: pano açılsın, katılımcılar madde yazsın, maddeler oylanıp tartışılsın, alınan kararlar kayda geçsin."</em> Planning poker'da takımın bu kaleme verebildiği en dürüst tahmin "büyük — en az üç sprint"tir. Yani karşımızda bir epic var.</p>

<h3>Önce yanlış yolu görelim</h3>

<p>İlk refleks çoğu zaman işi katmanlara bölmektir:</p>

<ul>
<li><strong>Sprint 1:</strong> Veritabanı şeması ve tüm backend API'leri</li>
<li><strong>Sprint 2:</strong> Tüm arayüz ekranları</li>
<li><strong>Sprint 3:</strong> Entegrasyon, test ve hata düzeltme</li>
</ul>

<p>Bu plan kâğıt üstünde derli topludur ama iki sprint boyunca gösterilecek, denenecek, geri bildirim alınacak hiçbir şey üretmez. Tüm entegrasyon riski üçüncü sprint'e yığılır; API'lerin ekran ihtiyaçlarıyla örtüşmediği de tam orada, en geç anda ortaya çıkar. Üç "parçanın" hiçbiri tek başına iş görmez — bu dilimleme değil, faz planlamasıdır.</p>

<h3>Şimdi dikey dilimleyelim</h3>

<p>Refinement'ta aynı iş, yukarıdaki teknikler uygulanarak kullanıcının gözünden dilimlenir:</p>

<ol>
<li><strong>Yürüyen iskelet</strong> <em>(iş akışı adımları + mutlu yol)</em>: Kolaylaştırıcı sabit Start-Stop-Continue şablonuyla bir pano açar ve bağlantısını paylaşır; katılımcılar madde ekler, herkes maddeleri görür. Gerçek zamanlılık yok — liste sayfa yenilenince güncellenir. Kulağa mütevazı geliyor, ama uçtan uca çalışır: Bu dilim bittiğinde bir takım gerçek retrosunu bu panoda yapabilir.</li>
<li><strong>Gerçek zamanlı güncelleme:</strong> Eklenen maddeler herkesin ekranında anında belirir. En riskli teknik varsayım (anlık iletişim altyapısı) böylece ikinci dilimde, henüz üzerine çok şey inşa edilmemişken test edilir.</li>
<li><strong>Oylama</strong> <em>(iş kuralı)</em>: Madde başına oy verme ve kişi başına oy limiti; en çok oy alan maddeler üste sıralanır.</li>
<li><strong>Benzer maddeleri gruplama</strong> <em>(iş akışı adımı)</em>: Aynı konuya işaret eden maddeler sürükle-bırak ile birleştirilir.</li>
<li><strong>Tartışma zamanlayıcısı:</strong> Madde başına zaman kutusu.</li>
<li><strong>Karar panosu</strong> <em>(iş akışının son adımı)</em>: Tartışılan maddeden sahibi atanmış bir aksiyon oluşturma.</li>
<li><strong>Sütun şablonları</strong> <em>(veri çeşitleri)</em>: Mad-Sad-Glad, 4L, Sailboat gibi ek formatlar.</li>
<li><strong>Anonim mod</strong> <em>(kural varyasyonu)</em>: Madde yazarının gizlenebilmesi.</li>
<li><strong>Aksiyon takibi:</strong> Önceki retronun aksiyonlarının yeni retronun açılışında gösterilmesi.</li>
<li><strong>Dışa aktarım:</strong> Oturum özetinin PDF/Markdown olarak indirilmesi.</li>
</ol>

<p>Tek kalem halindeyken tahmin bile edilemeyen epic, her biri kabaca 2-8 puan bandında, bağımsız test ve demo edilebilir on dilime dönüştü. Sprint planı da kendiliğinden şekillenir:</p>

<ul>
<li><strong>Sprint 1 — hedef: "İlk gerçek retro yapılabilsin":</strong> Dilim 1 ve 2. Özellik bayrağı arkasında yalnızca pilot bir takıma açılır; sprint review'da slayt değil, panonun üzerinde yapılan gerçek bir mini retro gösterilir.</li>
<li><strong>Sprint 2 — hedef: "Oturum disiplini kurulsun":</strong> Dilim 3, 4 ve 5. Oylamayla odak, zamanlayıcıyla tempo gelir; pilot takım sayısı artırılır.</li>
<li><strong>Sprint 3 — hedef: "Sürekli iyileştirme döngüsü kapansın":</strong> Dilim 6 ve sonrası — ama hangileri olacağı artık pilot takımların geri bildirimine bağlıdır.</li>
</ul>

<p>Bu "artık geri bildirime bağlı" kısmı, dikey dilimlemenin asıl ödülüdür. Diyelim ki pilot takımlar ilk iki sprintin sonunda en çok şunu söyledi: "Yönetici odadayken kimse dürüst madde yazmıyor." Bu durumda anonim mod (dilim 8), sütun şablonlarının (dilim 7) önüne geçer; dışa aktarım (dilim 10) ise belki hiç yapılmaz — belki de hiç gerekmiyordur. Yatay planlanmış versiyonda bu esneklik yoktur: Üçüncü sprintin sonuna kadar ortada kullanılabilir hiçbir şey olmadığı için, ne öğrenme ne rota değişikliği mümkündür. Dikey dilimlenmiş epic ise her sprint sonunda hem çalışan bir artış hem de bir sonraki kararı besleyen gerçek veri üretir.</p>

<h2>Bir Sprint'te Bitmeyecek İş Sprint'e Nasıl Alınır?</h2>

<p>Sorunun dürüst yanıtı: <strong>Alınmaz — sprint'e sığacak dilimi alınır.</strong> Pratikte bu şöyle işler:</p>

<ol>
<li><strong>Büyük iş, backlog'da epic olarak kalır.</strong> Epic'i silmek veya zorla küçültmek gerekmez; o bir şemsiyedir ve dilimleri sprint'lere girer.</li>
<li><strong>En öğretici dilimle başlayın:</strong> İlk sprint'e, en riskli varsayımı test eden veya mimari omurgayı uçtan uca kuran dilimi alın. En kolay dilimle başlamak cazip ama yanıltıcıdır; asıl riski sona saklar.</li>
<li><strong>Sürekli entegre edin:</strong> Dilimler tamamlandıkça ana koda birleşmelidir. Özellik bütünüyle hazır olana dek kullanıcıya açılmayacaksa, <strong>feature flag</strong> (özellik anahtarı) arkasında kapalı tutulur; aylarca yaşayan özellik dalları (long-lived branch) entegrasyon riskini biriktirir.</li>
<li><strong>Taşmayı normalleştirmeyin:</strong> Sprint sonunda bitmeyen iş "otomatik olarak" sonraki sprint'e akmaz; backlog'a döner ve diğer işlerle birlikte yeniden önceliklendirilir. Çoğu zaman yine ilk sıradan devam eder, ancak bu bilinçli bir karar olmalıdır.</li>
<li><strong>Kısmi bitmişlik saymayın:</strong> "Yüzde 80 bitti" diye bir durum yoktur; iş ya Bitti Tanımı'nı karşılar ya karşılamaz. Bitmeyen işten sprint sonunda kısmi puan almak, hem velocity'yi hem şeffaflığı bozar.</li>
</ol>

<h2>Yaygın Hatalar</h2>

<ul>
<li><strong>Katmana göre bölmek:</strong> "Backend story'si" ve "frontend story'si" birer dilim değil, birbirini bekleyen iki yarım iştir. İkisi de tek başına değer üretmez.</li>
<li><strong>Faza göre bölmek:</strong> "Analiz işi", "geliştirme işi", "test işi" olarak parçalamak, sprint'in içine küçük bir şelale kurar. Analiz, geliştirme ve test bir dilimin <em>içindeki</em> etkinliklerdir, ayrı backlog kalemleri değil.</li>
<li><strong>Aşırı parçalamak:</strong> Her dilim hâlâ anlamlı bir değer üretmelidir. İşi yarım günlük kırıntılara bölmek, değer üretmeyen bir yönetim yükü ve anlamsız bir "iş bitirme" istatistiği yaratır.</li>
<li><strong>Parçalamayı sprint planlamaya bırakmak:</strong> Planlama toplantısında apar topar yapılan bölme, düşünülmemiş dilimler üretir. Parçalama, refinement'ın işidir; sprint planlamaya işler hazır gelmelidir.</li>
<li><strong>Bağımlılık zinciri kurmak:</strong> Dilimler mümkün olduğunca bağımsız sıralanmalıdır. Her dilimi bir öncekine kilitleyen parçalama, tek gecikmeyle tüm planı deviren bir domino dizisi yaratır.</li>
</ul>

<p>Parçalama sinyalini çoğu zaman tahmin oturumları verir: <a href="/">ScrumTools</a>'un planning poker'ında bir iş 13 ve üzeri puan alıyorsa ya da oylar sürekli geniş bir aralığa dağılıyorsa, takım fiilen "bu işi yeterince anlamıyoruz veya çok büyük" demektedir — bu, işi tahmin etmeye çalışmayı bırakıp parçalamaya başlama işaretidir.</p>

<h2>Sonuç</h2>

<p>Büyük işleri parçalamak, Scrum'ın "her sprint çalışan bir artış üret" sözünü tutmasını sağlayan temel beceridir. İlke nettir: Her parça dikey bir dilim olmalı, tek başına değer üretmeli ve bir sprint'e rahatça sığmalıdır. Sprint'e sığmayan iş epic olarak backlog'da bekler; sprint'lere onun en riskli, en öğretici dilimleri girer. Bunu refinement disipliniyle düzenli yapan takımlarda "bu iş sığmaz" cümlesi bir kriz değil, on dakikalık bir parçalama egzersizinin başlangıcıdır.</p>
`,
    faq: [
        {
            q: 'Bir iş en fazla ne kadar büyük olmalı?',
            a: 'Yaygın pratik, bir işin sprint kapasitesinin küçük bir bölümünü (kabaca yüzde 10-20) geçmemesi ve birkaç günde bitecek boyutta olmasıdır. Story point kullanan takımlarda 13 ve üzeri tahminler çoğunlukla "parçala" sinyali sayılır.'
        },
        {
            q: 'Epic nedir, sprint\'e alınır mı?',
            a: 'Epic, tek sprint\'e sığmayacak kadar büyük, şemsiye niteliğinde bir backlog kalemidir. Kendisi sprint\'e alınmaz; refinement\'ta dikey dilimlere bölünür ve sprint\'lere bu dilimler girer.'
        },
        {
            q: 'Sprint\'te bitmeyen iş sonraki sprint\'e otomatik geçer mi?',
            a: 'Hayır. Bitmeyen iş backlog\'a döner ve yeniden önceliklendirilir. Çoğu zaman yine üst sıradan devam eder ama bu bilinçli bir karardır. İş için kısmi puan da sayılmaz; velocity yalnızca Bitti Tanımı\'nı karşılayan işlerden hesaplanır.'
        },
        {
            q: 'Teknik işler ve refactoring nasıl parçalanır?',
            a: 'Mümkünse teknik iyileştirmeyi, o bölgeye dokunan değer dilimlerinin içine yerleştirin ("dokunduğun yeri temiz bırak"). Bağımsız yürümesi gereken büyük teknik işler ise sonuç odaklı tanımlanmış, sprint\'e sığan parçalara bölünür; büyük belirsizlik varsa önce zaman kutulu bir spike yapılır.'
        },
        {
            q: 'Feature flag olmadan çok sprint\'lik bir özellik yönetilebilir mi?',
            a: 'Mümkün ama risklidir: Alternatif olan uzun ömürlü özellik dalları, entegrasyon sorunlarını biriktirir. Dilimleri ana koda sürekli birleştirip özelliği kullanıcıya bir anahtarla kapalı tutmak, hem entegrasyonu hem yayın kontrolünü basitleştirir.'
        },
        {
            q: 'Parçalama kimin sorumluluğunda?',
            a: 'Tüm Scrum takımının. Ürün Sahibi değer ve öncelik perspektifini, geliştiriciler teknik bölünebilirlik perspektifini getirir. Parçalama tipik olarak refinement oturumlarında birlikte yapılır; tek kişinin masa başında yaptığı bölmeler çoğu zaman ya yatay ya da tahmin edilemez çıkar.'
        }
    ]
}
