// Blog makalesi: Müşteri Çevik Çalışmıyorsa — Kabul Testleri ve Release Yönetimi
export default {
    slug: 'cevik-olmayan-musteri-kabul-testi-release-yonetimi',
    title: 'Müşteri "Bitmiş İş" İstiyorsa: Çevik Olmayan Müşteriyle Kabul Testleri ve Release Yönetimi',
    metaTitle: 'Çevik Olmayan Müşteriyle Kabul Testi ve Release Yönetimi | ScrumTools Blog',
    description: 'Müşteri artımlı teslimat kabul etmiyor, sprint review istemiyor, "dört dörtlük bitmiş iş" bekliyorsa ne yapmalı? İçeride çevik kalıp dışarıda tek büyük teslimat yapmanın yolu: kabul kriterleri, UAT süreci, sürümleme, kabul tutanağı ve rollback planı bu rehberde.',
    category: 'Release Yönetimi',
    date: '2026-07-17',
    readingMinutes: 12,
    excerpt: 'Her müşteri iki haftada bir demo izlemek istemez; kiminin sözleşmesi, kültürü veya beklentisi "her şey bitince tek seferde teslim" der. Bu, çevikliği çöpe atmak zorunda olduğunuz anlamına gelmez. Bu yazıda geliştirme yöntemi ile teslimat modelini birbirinden ayırıyor; kabul kriterlerinden UAT sürecine, sürümlemeden kabul tutanağına, büyük teslimatı krizsiz yönetmenin pratiğini anlatıyoruz.',
    related: ['buyuk-isler-nasil-parcalanir', 'analiz-gelistirme-test-ayni-sprintte-mi', 'urun-is-listesi-nasil-organize-edilmeli'],
    html: `
<p>Çevik dönüşüm yazılarının pek değinmediği bir gerçek var: Müşterinizin çevik olmak gibi bir zorunluluğu yok. Kamu ihalesi, sabit kapsamlı sözleşme, kurumsal satın alma süreci ya da yalnızca alışkanlık — sebep ne olursa olsun bazı müşteriler iki haftada bir demo izlemek, "şimdilik bu kadarı çalışıyor" cümlesini duymak, yarım özellik görmek istemez. Beklentileri nettir: <em>"Sözleşmede yazan işi eksiksiz bitirin, test edin, bize dört dörtlük teslim edin."</em></p>

<p>Bu noktada çoğu takım iki hatadan birine düşer. Birincisi, müşteriye çeviklik vaazı vermek: Sprint review'a davet edilen ama gelmeyen müşteriye kırılıp süreci suçlamak. İkincisi, çevikliği tamamen bırakmak: "Madem müşteri şelale istiyor, biz de şelale çalışalım" deyip altı ay boyunca entegre edilmemiş kod biriktirmek ve tüm riski teslim gününe yığmak. İki yol da yanlıştır; çünkü ikisi de aynı yanlış varsayıma dayanır — <strong>geliştirme yöntemi ile teslimat modelinin aynı şey olduğu varsayımına.</strong></p>

<h2>Temel Ayrım: İçeride Çevik, Dışarıda Sözleşmeli</h2>

<p>Müşteri <em>teslimat modelini</em> seçer: Neyi, ne zaman, hangi bütünlükte alacağını. Ama <em>geliştirme yönteminizi</em> seçmez: İşi içeride nasıl parçaladığınız, hangi sıklıkla entegre ettiğiniz, kaliteyi nasıl güvence altına aldığınız sizin mutfağınızdır. Bu ayrımı yaptığınız anda tablo netleşir:</p>

<ul>
<li><strong>İçeride hiçbir şey değişmez:</strong> İşleri yine dikey dilimlere bölersiniz, yine sprint'lerle (veya akışla) çalışırsınız, her dilim yine Bitti Tanımı'ndan geçer, kod yine sürekli entegre edilir. Bunların hiçbiri müşteriye görünmez ve müşterinin onayına tabi değildir.</li>
<li><strong>Dışarıda teslimat büyük ve bütündür:</strong> Müşteri, sözleşmedeki kapsamın tamamını tek (veya birkaç) büyük sürümde, kabul testinden geçmiş halde alır.</li>
</ul>

<p>Buna kısaca <strong>"artımlı geliştir, blok halinde teslim et"</strong> diyebiliriz. Kritik araç, biriken işi teslim gününe kadar güvenle saklamaktır: Tamamlanan özellikler ana koda sürekli birleştirilir ama <strong>feature flag</strong> (özellik anahtarı) arkasında ya da henüz müşteriye açılmamış bir ortamda tutulur. Böylece "her şey son gece entegre olur" felaketi yaşanmaz; teslim edilecek sürüm, aylardır çalışan ve test edilen kodun kendisidir.</p>

<p>Bu modelin size kazandırdığı şeyi küçümsemeyin: Çevik takımların en büyük iki riski — entegrasyon riski ve kalite riski — içeride çözülmüş olur. Geriye kalan tek büyük risk, müşterinin teslim edilen işi <em>beklediği iş sanmamasıdır.</em> İşte kabul testleri ve release yönetimi tam olarak bu riski yönetir.</p>

<h2>Kabul Kriterlerini Kod Yazılmadan Önce Yazın</h2>

<p>Büyük teslimatlı projelerde kriz neredeyse hiçbir zaman teslim günü çıkmaz; teslimden <em>aylar önce</em>, kimsenin fark etmediği bir anda çıkar — müşterinin kafasındaki iş ile sizin anladığınız işin ayrıştığı anda. Teslim günü bu ayrışmanın yalnızca faturası kesilir. Panzehir, kabulün ölçütünü baştan yazılı hale getirmektir:</p>

<ol>
<li><strong>Her gereksinim için test edilebilir kabul kriterleri yazın.</strong> "Sistem hızlı olmalı" bir kriter değildir; "Ürün listesi 500 kayıtla 2 saniyenin altında açılır" bir kriterdir. Kriter, evet/hayır cevabı verilebilen bir cümledir.</li>
<li><strong>Kriterleri müşteriye onaylatın — geliştirme başlamadan.</strong> Bu onay, projenin en değerli imzasıdır: Teslim günü tartışması "beğenmedim" ekseninden "kriter karşılandı mı" eksenine taşınır. Sabit kapsamlı sözleşmelerde kabul kriterlerinin sözleşme eki olması en sağlıklısıdır.</li>
<li><strong>İzlenebilirlik kurun:</strong> Her sözleşme maddesi → bir veya birden çok iş kalemi → her iş kaleminin kabul kriterleri → her kriterin kabul testi senaryosu. Bu zincir kurulunca "şu gereksinim nerede?" sorusunun cevabı saniyeler sürer ve kapsam dışı kalan hiçbir madde teslim gününe sürpriz olarak kalmaz.</li>
<li><strong>Kabul senaryolarını önceden paylaşın.</strong> Müşterinin kabul testinde ne yapacağını teslim günü öğrenmesi sizin aleyhinizedir. Senaryoları haftalar önce gönderin: "Kabulde şu 74 senaryoyu birlikte koşacağız." Müşteri senaryolara itiraz edecekse bunu erken etsin.</li>
</ol>

<h2>İki Seviyeli Kalite: Bitti Tanımı ve Kabul Testi</h2>

<p>Sık karışan iki kavramı ayıralım. <strong>Bitti Tanımı (Definition of Done)</strong> sizin iç kalite kapınızdır: Kod gözden geçirildi, testler yazıldı, entegre edildi, dokümante edildi. Her iş kalemi, müşteri hiç görmese bile bu kapıdan geçer. <strong>Kabul testi (UAT)</strong> ise müşterinin kapısıdır: Teslim edilen bütünün, üzerinde anlaşılmış kriterleri karşıladığının müşteri gözüyle doğrulanmasıdır. İlki sürekli ve içeride, ikincisi teslimat öncesinde ve müşteriyle birlikte işler. Bitti Tanımı'nı gevşetip "nasılsa UAT'de çıkar" demek, kendi güvenlik ağınızı müşterinin önünde delik göstermektir; UAT'de <em>sizin</em> bulmanız gereken hatalar çıkıyorsa süreç geri tepiyor demektir.</p>

<p>İyi bir UAT süreci şu unsurlardan oluşur:</p>

<ul>
<li><strong>Ayrı ve donmuş bir ortam:</strong> UAT, geliştirme ortamında değil; üretime en yakın, sürümü sabitlenmiş bir ortamda yapılır. UAT sırasında bu ortama yeni özellik girmez — yalnızca UAT'de çıkan hataların düzeltmeleri, kontrollü şekilde alınır.</li>
<li><strong>Gerçekçi test verisi:</strong> Müşterinin kendi iş akışını yansıtan veri hazırlanır; "lorem ipsum" dolu ekranlarla kabul testi yapılmaz.</li>
<li><strong>Tanımlı süre ve katılımcılar:</strong> "Müşteri müsait olunca bakar" değil; başlangıcı, bitişi, kimin test edeceği ve kimin karar vereceği belli, takvimlenmiş bir dönem.</li>
<li><strong>Hata sınıflandırması — önceden anlaşılmış:</strong> Çıkan her bulgu aynı ağırlıkta değildir. Tipik sınıflama: <em>Engelleyici</em> (temel akış çalışmıyor — kabulü durdurur), <em>Majör</em> (önemli ama geçici çözümü var — düzeltilmesi kabul şartıdır, yeniden tam tur gerektirmez), <em>Minör</em> (kozmetik — kabulü engellemez, eksik listesine yazılır). Bu sınıflamayı UAT başlamadan müşteriyle anlaşmaya bağlamak, kabul gününde "bu buton iki piksel kaymış, kabul etmiyoruz" krizini baştan çözer.</li>
<li><strong>Çıkış kriteri ve kabul tutanağı:</strong> "Tüm engelleyici ve majör bulgular kapandı, minörler eksik listesinde" durumuna gelindiğinde kabul tutanağı imzalanır. Tutanak; test edilen sürüm numarasını, koşulan senaryoları, açık minör maddeleri ve varsa şartları içerir. Süreli kabul maddesi de sözleşmenizde olsun: Müşteri tanımlı süre içinde test etmez veya itiraz bildirmezse teslimat kabul edilmiş sayılır — aksi halde kabul süreci belirsizce sarkar.</li>
</ul>

<h2>Release Yönetimi: Büyük Teslimatı Sıkıcı Hale Getirmek</h2>

<p>İyi release yönetiminin hedefi heyecan değil, sıkıcılıktır: Teslim günü, daha önce defalarca prova edilmiş bir işlemin son tekrarı olmalıdır. Yapı taşları:</p>

<h3>Sürümleme ve içerik</h3>

<ul>
<li>Her teslimat adlandırılmış bir <strong>sürümdür</strong> (ör. 1.4.0 veya 2026.07). Hangi sürümde hangi iş kalemlerinin olduğu her an listelenebilmelidir — kabul tutanağı bir sürüm numarasına imza atar, "son hali"ne değil.</li>
<li><strong>Release notları</strong> her sürümle birlikte yazılır: kapsam, düzeltmeler, bilinen sınırlamalar, varsa geçiş adımları. Müşteriye "ne teslim ettiniz?" sorusunun cevabı bir belge olmalıdır, bir e-posta zinciri değil.</li>
</ul>

<h3>Ortam zinciri ve sürüm dondurma</h3>

<ul>
<li>Standart zincir: <strong>geliştirme → test → UAT → üretim</strong>. Kod bu zincirde yalnızca ileri akar; UAT'ye çıkan paket, test ortamında regresyondan geçmiş paketin aynısıdır.</li>
<li>UAT'ye çıkmadan önce bir <strong>sürüm adayı</strong> (release candidate) kesilir ve <strong>dondurulur</strong>: Bu noktadan sonra pakete yeni özellik girmez, yalnızca UAT bulgu düzeltmeleri girer ve her düzeltme yeni bir aday numarası alır (1.4.0-rc2). "Dün test ettiğiniz sürümde o hata yoktu, bu sabah bir şey değişti" cümlesi, güveni en hızlı eriten cümledir.</li>
<li>Sürüm dondurma ile teslim arasında takım boş durmaz: Bir sonraki kapsamın dilimleri ana hatta ilerlemeye devam eder; dondurulan yalnızca teslim edilecek pakettir.</li>
</ul>

<h3>Geri dönüş ve acil düzeltme planı</h3>

<ul>
<li><strong>Rollback planı teslimattan önce yazılır:</strong> Üretime alınan sürüm ciddi sorun çıkarırsa hangi adımlarla önceki sürüme dönülür? Veritabanı değişiklikleri geri alınabilir mi, alınamıyorsa ileri düzeltme (fix-forward) stratejisi nedir? Bu soruların cevabı kriz anında değil, sakin bir öğleden sonra verilmelidir.</li>
<li><strong>Hotfix süreci tanımlı olsun:</strong> Kabulden sonra üretimde kritik hata çıkarsa düzeltme, geliştirmenin güncel halinden değil, teslim edilen sürümün üzerinden yapılır ve yama sürümü olarak (1.4.1) aynı test zincirinden hızlandırılmış geçer. Böylece acil düzeltme, müşteriye yarım kalmış yeni özellikler sızdırmaz.</li>
</ul>

<h2>Ara Demolar: Teslimat Değil, Önizleme</h2>

<p>Müşteri artımlı <em>teslimat</em> istemiyor olabilir; bu, artımlı <em>görünürlük</em> de istemediği anlamına gelmez ve bu ikisini ayırmak sizin en güçlü kozunuzdur. Müşteriye şunu önerin: <em>"Teslimat tek seferde olacak; ama ayda bir, 45 dakikalık gayriresmî bir önizleme yapalım. Bu bir teslimat değil, kabul turu hiç değil — yalnızca yönü birlikte kontrol ediyoruz."</em></p>

<p>Bu çerçeveleme önemlidir: "Sprint review" deyince direnen müşteri, "ilerleme sunumu" deyince çoğu zaman memnuniyetle gelir — üstelik satın alma tarafındaki yöneticiler için bu, ilerlemeyi raporlama imkânıdır. Önizlemelerde toplanan geri bildirim ikiye ayrılır: Kabul kriterlerinin <em>yorumuna</em> dair düzeltmeler (ücretsiz düzeltilir, çünkü zaten kriterin doğru anlaşılması demektir) ve kapsam <em>değişiklikleri</em> (değişiklik talebi sürecine girer: etki analizi, süre/bütçe teklifi, yazılı onay). Bu ayrımı baştan netleştirmek, "demo gösterdiniz, o zaman şunu da ekleyin" sarmalını önler.</p>

<p>Ara önizlemeyi tamamen reddeden müşteri de olur. O durumda bile aynı provayı içeride yapın: Her ay, teslim edilecek paketi kendi ekibinizle müşteri gözüyle baştan sona gezin. Kabul gününde sürprizle karşılaşma olasılığınız, işi son güne kadar "bitmiş sayıp" hiç bütün halinde görmemiş bir takımınkinden kat kat düşük olur.</p>

<h2>Kabul Gününü Krizsiz Geçirmenin Kontrol Listesi</h2>

<ol>
<li>Kabul kriterleri yazılı ve müşteri onaylı mı? Sözleşmeye ek mi?</li>
<li>Her kriterin bir kabul senaryosu var mı ve senaryolar müşteriyle önceden paylaşıldı mı?</li>
<li>UAT ortamı üretim benzeri mi, sürüm dondurulmuş mu, test verisi gerçekçi mi?</li>
<li>Hata sınıflandırması (engelleyici/majör/minör) ve kabulü neyin durduracağı önceden anlaşıldı mı?</li>
<li>UAT'nin süresi, katılımcıları ve karar vericisi takvimde mi? Süreli kabul maddesi sözleşmede mi?</li>
<li>Sürüm numarası, içerik listesi ve release notları hazır mı?</li>
<li>Rollback planı ve hotfix süreci yazılı mı?</li>
<li>UAT öncesi tam regresyon içeride koşuldu mu — müşterinin bulacağı ilk hata sizin de ilk kez göreceğiniz bir hata olmayacak, değil mi?</li>
</ol>

<p>Bu listenin tamamı araç desteğiyle çok daha hafifler: <a href="/">ScrumTools</a>'ta iş kalemlerini kabul kriterleriyle tanımlayıp release'lere bağlayabilir, hangi sürümde hangi işlerin teslim edildiğini release yönetimi ekranından izleyebilir, UAT bulgularını sınıflandırılmış görevler olarak panoda yönetebilirsiniz — böylece kabul tutanağına giren sürüm içeriği, elle tutulan bir Excel değil, panodaki gerçeğin kendisi olur.</p>

<h2>Sonuç</h2>

<p>Müşterinin çevik olmaması, sizin çevikliğinizi bırakmanız için bir sebep değildir; yalnızca çevikliğin <em>görünür yüzünü</em> değiştirmeniz için bir sebeptir. İçeride küçük dilimler, sürekli entegrasyon ve sıkı bir Bitti Tanımı ile çalışmaya devam edin — bunlar sizin kalite sigortanızdır ve müşterinin iznine tabi değildir. Dışarıda ise müşterinin dilinden konuşun: Baştan onaylanmış kabul kriterleri, senaryosu önceden bilinen bir UAT, sınıflandırılmış bulgular, numaralı sürümler, imzalı tutanaklar. "Dört dörtlük bitmiş iş" isteyen müşteriyi mutlu eden şey şelale süreci değildir; sürprizsiz bir kabul günüdür. Ve sürprizsiz kabul günü, en iyi, içeride çevik çalışan takımların elinden çıkar.</p>
`,
    faq: [
        {
            q: 'Müşteri sprint review\'lara katılmak istemiyorsa ne yapmalıyım?',
            a: 'Zorlamayın; çerçeveyi değiştirin. "Sprint review" yerine ayda bir kısa bir "ilerleme önizlemesi" önerin ve bunun teslimat ya da kabul turu olmadığını netleştirin. Müşteri bunu da istemezse aynı provayı içeride yapın: Teslim edilecek bütünü düzenli aralıklarla kendi ekibinizle müşteri gözüyle test edin.'
        },
        {
            q: 'Kabul testi (UAT) ile Bitti Tanımı arasındaki fark nedir?',
            a: 'Bitti Tanımı takımın iç kalite kapısıdır; her iş kalemi için sürekli uygulanır (kod incelemesi, testler, entegrasyon). Kabul testi ise müşterinin, teslim edilen bütünün üzerinde anlaşılmış kriterleri karşıladığını doğruladığı resmi süreçtir. Bitti Tanımı UAT\'nin yerine geçmez; UAT\'yi sürprizsiz hale getirir.'
        },
        {
            q: 'Kabul testinde çıkan her hata teslimatı durdurur mu?',
            a: 'Durdurmamalıdır — bunun için hata sınıflandırmasını UAT başlamadan müşteriyle anlaşmaya bağlayın. Yaygın model: Engelleyici hatalar kabulü durdurur, majör hatalar düzeltilmesi şartıyla süreci durdurmaz, minör/kozmetik bulgular eksik listesine yazılır ve kabulü engellemez.'
        },
        {
            q: 'Sabit kapsamlı sözleşmede değişiklik talepleri nasıl yönetilir?',
            a: 'Yazılı bir değişiklik talebi (change request) süreciyle: Talep kayda geçer, etki analizi yapılır (süre, bütçe, diğer maddelere etkisi), teklif sunulur ve yazılı onayla kapsama girer. Kabul kriterinin yanlış anlaşılmasından doğan düzeltmeler ise değişiklik değildir; bunlar kriterin doğru karşılanması kapsamında yapılır. Bu ayrımı baştan netleştirmek iki tarafı da korur.'
        },
        {
            q: 'Müşteri şelale teslimat isterken takım içeride Scrum kullanabilir mi?',
            a: 'Evet — geliştirme yöntemi ile teslimat modeli ayrı şeylerdir. Takım içeride sprint\'lerle, dikey dilimlerle ve sürekli entegrasyonla çalışır; tamamlanan özellikler feature flag arkasında birikir ve müşteriye tek büyük sürüm halinde teslim edilir. Müşteri teslimatın şeklini belirler, mutfağın işleyişini değil.'
        },
        {
            q: 'Kabul tutanağında neler olmalı?',
            a: 'Test edilen sürüm numarası, kabul testinin yapıldığı tarih aralığı ve katılımcılar, koşulan senaryo listesi ve sonuçları, açık kalan minör maddeler (eksik listesi) ve varsa kabul şartları ile imzalar. Tutanak belirli bir sürüme bağlanmalıdır; "son teslim edilen hali" gibi belirsiz ifadeler sonradan tartışma yaratır.'
        }
    ]
}
