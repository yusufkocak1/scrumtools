// Blog makalesi: Analiz, Geliştirme ve Test Aynı Sprintte mi Olmalı?
export default {
    slug: 'analiz-gelistirme-test-ayni-sprintte-mi',
    title: 'Analiz, Geliştirme ve Test Aynı Sprintte mi Olmalı? Sprint İçinde Kim Ne Yapar?',
    metaTitle: 'Analiz, Geliştirme ve Test Aynı Sprintte mi? | ScrumTools Blog',
    description: 'Bir işin analizi, geliştirmesi ve testi aynı sprintte mi tamamlanmalı? Analist, developer ve tester aynı takımda nasıl çalışır, kim ne zaman ne yapar?',
    category: 'Takım Pratikleri',
    date: '2026-07-17',
    readingMinutes: 9,
    excerpt: 'Evet: Bir işin sprint içinde "bitti" sayılması için analizi netleşmiş, geliştirilmiş ve test edilmiş olması gerekir. Ama bu, sprint\'in içine küçük bir şelale kurmak demek değildir. Doğru model faz bazlı değil, iş bazlı akıştır.',
    related: ['buyuk-isler-nasil-parcalanir', 'sprint-retrospektifi-nedir', 'buyuyen-scrum-takimi-nasil-bolunur'],
    html: `
<p>Scrum'a geçen takımların en sık sorduğu sorulardan biri şudur: <em>"Bir işin analizi, geliştirmesi ve testi aynı sprintte mi olmalı? Öyleyse analiz yapılırken developer ve tester, geliştirme yapılırken tester ne yapacak?"</em> Kısa yanıt: <strong>Evet, bir işin sprint sonunda "bitti" sayılabilmesi için analizi netleşmiş, geliştirilmiş ve test edilmiş olması gerekir.</strong> Test edilmemiş iş bitmemiş iştir. Ancak bu "evet", sprint'in içine haftalık fazlardan oluşan küçük bir şelale kurmak anlamına gelmez — sorunun asıl cevabı, işlerin sprint içinde <em>nasıl aktığında</em> gizlidir.</p>

<h2>Mini Şelale Tuzağı</h2>

<p>Bu soruyu soran takımların çoğu, farkında olmadan şu modeli hayal eder: Sprint'in ilk günleri analiz, ortası geliştirme, son iki günü test. Bu düzenin sahada bir adı vardır: <strong>mini şelale</strong> (scrumfall). Sonuçları da tanıdıktır:</p>

<ul>
<li>Test hep sprint sonuna sıkışır; bug'lar son gün ortaya çıkar ve düzeltecek zaman kalmaz.</li>
<li>Tester sprint'in ilk yarısında "iş bekler", son iki gün gece mesaisi yapar.</li>
<li>İşler sprint sonunda "kodu bitti ama testi bitmedi" limbosunda kalır ve sonraki sprint'e taşar.</li>
<li>Sprint review'da gösterilen artış aslında doğrulanmamıştır.</li>
</ul>

<p>Bu, Scrum değil iki haftalık şelaledir. Sorun rollerin aynı sprintte olması değil, işlerin <em>faz faz</em> ilerletilmesidir.</p>

<h2>Doğru Model: Faz Bazlı Değil, İş Bazlı Akış</h2>

<p>Sağlıklı bir sprintte fazlar değil, <strong>işler</strong> akar. Sprint backlog'undaki her iş kendi küçük analiz-geliştirme-test döngüsünü tamamlar ve işler birbirini beklemeden, kısmen paralel ilerler: 1 numaralı iş test edilirken 2 numaralı iş geliştirmede, 3 numaralı işin detayları netleştirilmektedir. Sprint'in üçüncü gününden itibaren test edilecek bir şeyler her zaman vardır; test etkinliği sona yığılmaz, sprint'e yayılır.</p>

<p>Bunu mümkün kılan iki disiplin vardır: İşlerin küçük ve dikey dilimlenmiş olması (büyük işleri parçalama pratiği burada devreye girer) ve devam eden iş sayısının (WIP) sınırlanması. Takım aynı anda az sayıda işe birlikte yüklenir (swarming); "herkes kendi işini açar, sprint sonunda hepsi yüzde 90'da kalır" düzeninin tersine, işler tek tek gerçekten biter.</p>

<h2>Analizin İki Katmanı</h2>

<p>Sorudaki kritik incelik şudur: "Analiz" tek bir şey değildir ve iki katmana ayrılır:</p>

<ul>
<li><strong>Hazırlık analizi — sprint'ten önce:</strong> Bir işin kapsamının netleşmesi, kabul kriterlerinin yazılması, büyük belirsizliklerin giderilmesi ve parçalanması <strong>backlog refinement</strong>'ta, iş sprint'e girmeden önce yapılır. Birçok takım bunu "iş, sprint'e hazır mı?" sorusunu yanıtlayan gayriresmî bir Definition of Ready ile destekler. Derin analiz sprint içine bırakılırsa tahminler anlamını yitirir ve sprint, analiz beklemeleriyle tıkanır.</li>
<li><strong>Detay analizi — sprint içinde:</strong> Geliştirme sırasında ortaya çıkan soruların yanıtlanması, uç senaryoların netleştirilmesi, küçük kapsam kararları sprint içinde, iş üzerinde çalışılırken yapılır. Bu doğaldır ve engellenmemelidir; her detayı önden yazmaya çalışmak sizi belge odaklı bir ön-şelaleye götürür.</li>
</ul>

<h2>Sprint Boyunca Kim Ne Yapar?</h2>

<h3>Analiz yapılırken developer ve tester ne yapar?</h3>

<p>İlk düzeltme şudur: Analiz, analistin tek başına yaptığı bir faz değildir. Refinement'ta en iyi sonucu veren pratik <strong>Three Amigos</strong> (üç ahbap) yaklaşımıdır: İş; "ne istiyoruz" (analist/Ürün Sahibi), "nasıl yaparız" (developer) ve "nerede kırılır" (tester) perspektifleriyle <em>birlikte</em> incelenir. Tester'ın refinement'ta sorduğu "peki kullanıcı bunu boş bırakırsa?" sorusu, sprint sonunda bulunacak bir bug'ı daha yazılmadan önler. Kabul kriterleri bu üçlünün ortak ürünüdür; example mapping gibi teknikler bu konuşmayı yapılandırır.</p>

<p>Ayrıca unutmayın: Refinement geleceğin işlerine bakar ve sprint'in küçük bir yüzdesini alır. Developer ve tester bu sırada "analiz bitsin de başlayalım" diye beklemez; sprint'teki mevcut işlerin geliştirmesi ve testi zaten devam etmektedir.</p>

<h3>Geliştirme sürerken tester ne yapar?</h3>

<p>Modern testçi, sprint sonunda kapıda bekleyen bir denetçi değil, işin başından itibaren çalışan bir kalite ortağıdır (shift-left yaklaşımı). Geliştirme sürerken tester:</p>

<ul>
<li>Kabul kriterlerinden test senaryolarını ve test verilerini <em>kod tamamlanmadan önce</em> tasarlar — bu senaryolar developer'la paylaşıldığında çoğu bug hiç yazılmaz.</li>
<li>Test otomasyonunu geliştirmeyle eş zamanlı yazar; API seviyesinde testler çoğu zaman arayüz bitmeden koşmaya başlayabilir.</li>
<li>Biten dilimleri ve önceki işleri anında test eder; sprint'in akışında test edilecek bir şey neredeyse her zaman vardır.</li>
<li>Developer'la eşleşerek birim test kapsamını konuşur, keşif testi (exploratory testing) oturumları planlar, test ortamını ve regresyon setini hazırlar.</li>
</ul>

<h3>Test sürerken developer ne yapar?</h3>

<p>Testten dönen bug'lar sıradaki yeni işten önceliklidir: Aynı sprint'te açılan bug, bağlam hâlâ tazeyken dakikalar içinde çözülür; iki sprint sonra döndüğünde ise yeniden anlama maliyetiyle birlikte saatler alır. Bunun dışında developer code review yapar, test otomasyonuna katkı verir ve sıradaki işin geliştirmesine geçer. Yol gösterici ilke: <em>"Yeni işe başlamadan önce, bitmek üzere olan işin bitmesine yardım et"</em> (stop starting, start finishing).</p>

<h2>Analist, Developer ve Tester Aynı Takımda mı Olmalı?</h2>

<p><strong>Evet.</strong> Scrum'da "analist takımı", "geliştirme takımı", "test takımı" diye ayrı yapılar yoktur; Scrum Kılavuzu 2020'den bu yana takım içinde alt roller bile tanımlamaz — analiz, geliştirme ve test becerilerine sahip herkes tek bir "Developers" şemsiyesi altındadır. Takımın bütünü, bir işi baştan sona "bitti" durumuna getirebilecek çapraz fonksiyonellikte olmalıdır.</p>

<p>Bunun bilinen alternatifi — geliştirmenin N. sprintte, testin ayrı bir ekip tarafından N+1. sprintte yapıldığı <strong>kaydırılmış test</strong> (staggered testing) düzeni — iyi bilinen bir anti-pattern'dir: Geri bildirim bir sprint gecikir, bug'lar bağlam kaybolduktan sonra döner, hiçbir sprint gerçekten "bitmiş" artış üretmez ve iki ekip arasında sonu gelmez bir "bu bug mu, eksik analiz mi?" pazarlığı başlar. Kalite, sona eklenen bir faz değil; takımın tamamının, işin her aşamasındaki ortak sorumluluğudur (whole-team quality).</p>

<h2>Pratik İpuçları</h2>

<ul>
<li><strong>Bitti Tanımı'na testi yazın:</strong> "Kabul kriterleri doğrulandı, otomasyon eklendi, bug'lar kapatıldı" maddeleri DoD'de yer almalı; böylece "kodu bitti" cümlesi "bitti" anlamına gelmez.</li>
<li><strong>Panoyu akış olarak kurun:</strong> Sütunlar fazları değil işin akışını göstermeli ve devam eden iş sınırlı tutulmalıdır; "Test" sütununda biriken yığın, mini şelalenin ilk görsel kanıtıdır.</li>
<li><strong>Refinement'ı ihmal etmeyin:</strong> Sprint içi analiz sıkışıyorsa neden çoğu zaman sprint öncesi hazırlığın zayıflığıdır.</li>
<li><strong>Retrospektifte konuşun:</strong> "Test neden sona sıkıştı?" sorusu, retrospektifin en verimli konularından biridir; kök neden çoğunlukla işlerin büyüklüğünde veya WIP fazlalığında çıkar.</li>
</ul>

<p>Bu akışı görünür kılmak için ortak bir pano şarttır: <a href="/">ScrumTools</a>'un scrum board'unda işlerin analiz-geliştirme-test akışı tek ekranda izlenir; sprint sonuna doğru test sütununda oluşan yığılma daha retrospektifi beklemeden görünür hale gelir ve retro panosunda somut veriyle tartışılabilir.</p>

<h2>Sonuç</h2>

<p>Bir işin analizi, geliştirmesi ve testi aynı sprintte tamamlanır — çünkü ancak o zaman iş gerçekten "bitti"dir. Ama bu, sprint'i haftalık fazlara bölmek değil; küçük dilimlenmiş işleri, çapraz fonksiyonel tek bir takımın iş bazlı akışla tek tek bitirmesi demektir. Derin analiz refinement'la sprint'in önüne alınır; tester ilk günden kalite ortağı olarak çalışır; developer testten dönen bug'ı yeni işten önce çözer. Roller ayrı sprint'lere veya ayrı takımlara bölündüğünde kaybedilen şey hız değil yalnızca — "bitti" kelimesinin anlamıdır.</p>
`,
    faq: [
        {
            q: 'Bir işin analizi, geliştirmesi ve testi aynı sprintte mi olmalı?',
            a: 'Evet. Bir iş ancak analizi netleşmiş, geliştirilmiş ve test edilmiş haliyle Bitti Tanımı\'nı karşılar. Ancak derin hazırlık analizi, iş sprint\'e girmeden önce backlog refinement\'ta yapılır; sprint içinde yalnızca detay netleştirme kalır.'
        },
        {
            q: 'Tester sprint\'in ilk günlerinde ne yapar?',
            a: 'Kabul kriterlerinden test senaryolarını tasarlar, test verisi ve ortamını hazırlar, otomasyon iskeletini kurar, refinement\'a katılır ve önceki sprint\'ten gelen veya erken biten dilimleri test eder. İş bazlı akışta "test edilecek bir şey yok" dönemi çok kısadır.'
        },
        {
            q: 'Testi bir sonraki sprint\'te ayrı ekip yapsa olmaz mı?',
            a: 'Bu düzen (staggered testing) bilinen bir anti-pattern\'dir: Geri bildirim bir sprint gecikir, bug\'lar bağlam kaybolduktan sonra döner ve hiçbir sprint gerçekten bitmiş artış üretmez. Test, aynı takımın içinde ve aynı sprint\'te yapılmalıdır.'
        },
        {
            q: 'Analist Scrum takımında hangi role girer?',
            a: 'Scrum Kılavuzu (2020) takım içinde alt rol tanımlamaz; analiz becerisine sahip kişiler de "Developers" şemsiyesindedir. Analist pratikte refinement\'ı besler, kabul kriterlerinin yazımına öncülük eder ve sprint içinde detay sorularını yanıtlar; bazı organizasyonlarda Ürün Sahibi\'ne destek rolü üstlenir.'
        },
        {
            q: 'Definition of Ready zorunlu mu?',
            a: 'Resmî bir Scrum öğesi değildir ama pratikte faydalıdır: "Kabul kriterleri yazılmış, bağımlılıklar netleşmiş, takımca anlaşılmış" gibi hafif bir kontrol listesi, sprint içinde analiz tıkanmasını önler. Aşırı katı bir DoR ise sprint öncesine mini bir şelale kurar; dengeli tutulmalıdır.'
        },
        {
            q: 'Test otomasyonu olmadan bu akış mümkün mü?',
            a: 'Zorlaşır ama ilke aynı kalır: Test, faz olarak sona değil iş bazında sprint\'e yayılır. Manuel ağırlıklı takımlarda regresyon yükü her sprint büyüdüğü için, otomasyona yapılan yatırım bu akışın sürdürülebilirliğinin ön koşuludur.'
        }
    ]
}
