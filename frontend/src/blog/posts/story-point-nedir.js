// Blog makalesi: Story Point Nedir?
export default {
    slug: 'story-point-nedir',
    title: 'Story Point Nedir? Göreli Efor Tahmini Nasıl Yapılır?',
    metaTitle: 'Story Point Nedir? Fibonacci ile Efor Tahmini | ScrumTools Blog',
    description: 'Story point nedir, neden saat yerine kullanılır? Fibonacci dizisi, referans hikâye, velocity kavramı ve story point tahmininde yaygın hatalar bu rehberde.',
    category: 'Tahminleme',
    date: '2026-06-29',
    readingMinutes: 7,
    excerpt: 'Story point, bir işin eforunu saat yerine göreli bir ölçekle ifade eden tahmin birimidir. Karmaşıklık, belirsizlik ve iş miktarını tek bir değerde birleştirir; doğru kullanıldığında planlamayı hem hızlandırır hem isabetli kılar.',
    related: ['scrum-poker-nedir', 'burndown-grafigi-nedir', 'urun-is-listesi-nasil-organize-edilmeli'],
    html: `
<p><strong>Story point</strong>, çevik takımların bir backlog maddesinin gerektirdiği toplam eforu ifade etmek için kullandığı göreli ölçü birimidir. Tek bir değerin içinde üç bileşeni birleştirir: işin <strong>miktarı</strong> (ne kadar iş var), <strong>karmaşıklığı</strong> (ne kadar zor) ve <strong>belirsizliği</strong> (ne kadar bilinmez var). Bu yönüyle story point, "bu iş kaç saat sürer?" sorusunun değil, "bu iş, bildiğimiz şu işe göre ne kadar büyük?" sorusunun cevabıdır.</p>

<h2>Neden Saat Değil de Story Point?</h2>

<p>Saat bazlı tahmin ilk bakışta daha somut görünse de pratikte iki yapısal sorunla karşılaşır:</p>

<ul>
<li><strong>Kişiye bağımlılık:</strong> Aynı iş, deneyimli bir geliştirici için dört saat, göreve yeni başlayan biri için iki gün sürebilir. "Bu iş kaç saat?" sorusunun tek bir doğru cevabı yoktur; "Bu iş, X işinin yaklaşık iki katı" yargısında ise takım çok daha kolay uzlaşır. Göreli büyüklük, kişiden bağımsızdır.</li>
<li><strong>Sahte hassasiyet:</strong> "6,5 saat" gibi bir tahmin, gerçekte var olmayan bir kesinlik izlenimi verir ve tahmin ile taahhüt arasındaki çizgiyi siler. Story point'in kasıtlı olarak kaba olan ölçeği, belirsizliği dürüstçe kabullenir.</li>
</ul>

<p>Ayrıca insanların göreli karşılaştırmada mutlak kestirimden daha başarılı olduğu, karar verme literatüründe iyi bilinen bir bulgudur: İki binadan hangisinin daha yüksek olduğunu kolayca söyleriz; ancak metre cinsinden yüksekliklerini çoğu zaman büyük hatayla tahmin ederiz.</p>

<h2>Fibonacci Ölçeği ve Referans Hikâye</h2>

<p>Story point tahminlerinde yaygın ölçek, Fibonacci dizisinden türetilmiştir: <strong>1, 2, 3, 5, 8, 13, 21...</strong> Değerler büyüdükçe aralıkların açılması bilinçlidir; iş büyüdükçe tahminin doğal hata payı da büyür ve 20 ile 21'i ayırt etmeye çalışmak anlamsızlaşır. 13 ve üzeri tahmin alan maddeler, çoğu takımda "sprint'e girmeden önce bölünmesi gereken iş" olarak kabul edilir.</p>

<p>Ölçeğin işlemesi için takımın ortak bir <strong>referans hikâyesi</strong> olmalıdır: Daha önce tamamlanmış, herkesin iyi bildiği bir iş seçilir ve ölçeğin bir noktasına sabitlenir (örneğin "şifre sıfırlama e-postası = 3 puan"). Sonraki tüm tahminler bu çapaya göre yapılır. Referans hikâye takıma özgüdür; bu nedenle <em>bir takımın 5 puanı ile başka bir takımın 5 puanı karşılaştırılamaz.</em></p>

<h2>Velocity: Story Point'in Tamamlayıcısı</h2>

<p><strong>Velocity (hız)</strong>, takımın bir sprintte tamamladığı toplam story point miktarıdır. Birkaç sprintlik veri biriktiğinde takımın ortalama hızı ortaya çıkar ve bu ortalama, gelecek planlamanın en güvenilir girdisi hâline gelir: 60 puanlık bir sürüm kapsamı, ortalama hızı sprint başına 20 puan olan bir takım için yaklaşık üç sprintlik iştir. Tahminler tek tek hatalı olabilir; ancak hatalar zamanla iki yönde de dengelendiği için ortalama hız şaşırtıcı derecede kararlı bir öngörü aracıdır.</p>

<h2>Story Point Tahmini Nasıl Yapılır?</h2>

<ol>
<li>Takım, referans hikâyeyi ve ölçeği netleştirir.</li>
<li>Ürün Sahibi, tahmin edilecek maddeyi kabul kriterleriyle sunar.</li>
<li>Takım üyeleri tahminlerini bağımsız olarak belirler — bu aşamada çoğu takım, çapa etkisini önlemek için <a href="/blog/scrum-poker-nedir">Scrum Poker</a> tekniğini kullanır.</li>
<li>Farklı tahminlerin gerekçeleri tartışılır; gözden kaçan riskler bu tartışmada ortaya çıkar.</li>
<li>Fikir birliğine ulaşılan değer maddeye işlenir; uzlaşılamıyorsa madde bölünür veya analiz için ayrılır.</li>
</ol>

<h2>Yaygın Hatalar</h2>

<ul>
<li><strong>Point-saat çevrim tablosu kurmak:</strong> "1 puan = 4 saat" gibi eşlemeler, story point'i dolaylı saat tahminine dönüştürür ve göreli tahminin tüm avantajlarını sıfırlar.</li>
<li><strong>Velocity'yi performans metriği yapmak:</strong> Hız hedef hâline geldiğinde takımlar tahminleri şişirir ve metrik anlamını yitirir. Velocity bir planlama girdisidir, karne notu değildir.</li>
<li><strong>Takımlar arası karşılaştırma:</strong> Her takımın ölçeği kendi referansına göredir; "A takımı 30 puan, B takımı 20 puan yapıyor" karşılaştırması metodolojik olarak geçersizdir.</li>
<li><strong>Bireysel tahminleri ortalamak:</strong> Uzlaşma yerine ortalama almak, tahmin farklılıklarının ardındaki asıl değerli bilgiyi — farklı risk algılarını — tartışılmadan gömmek anlamına gelir.</li>
</ul>

<p>Tahminlerin işe dönüşmesi için kaydedildikleri yerin, sprint planlamasının yapıldığı yerle aynı olması işleri kolaylaştırır. <a href="/">ScrumTools</a>'ta poker oturumunda uzlaşılan puanlar doğrudan ilgili işin üzerine işlenir; sprint kapandığında velocity ve burndown grafikleri bu verilerden otomatik olarak üretilir.</p>

<h2>Sonuç</h2>

<p>Story point, doğru anlaşıldığında yalnızca bir sayı değil, takımın işi ortak bir dille konuşmasını sağlayan bir sözleşmedir. Göreli kalmasına özen gösterir, referans hikâyenizi canlı tutar ve velocity'yi hedef değil girdi olarak kullanırsanız; tahminleriniz sprint'ten sprint'e daha isabetli, planlama toplantılarınız ise daha kısa ve verimli hâle gelecektir.</p>
`,
    faq: [
        {
            q: 'Story point ile saat arasında sabit bir çevrim var mıdır?',
            a: 'Hayır; olması da amaçlanmaz. Story point göreli bir ölçüdür ve "1 puan = X saat" gibi eşlemeler, yöntemi dolaylı saat tahminine dönüştürerek faydasını ortadan kaldırır.'
        },
        {
            q: 'Velocity düşükse takım kötü mü çalışıyor demektir?',
            a: 'Hayır. Velocity takıma özgü, göreli bir planlama girdisidir; ölçeğe ve referans hikâyeye bağlıdır. Takımlar arası veya hedefe dayalı karşılaştırma için kullanılması metriği bozar.'
        },
        {
            q: 'Bug ve teknik borç maddelerine story point verilmeli mi?',
            a: 'Takımlar arasında iki yaygın pratik vardır: kapasiteyi görünür kılmak için puanlamak veya yalnızca ürün işlerini puanlayıp bug\'lara sabit kapasite ayırmak. Önemli olan, seçilen yaklaşımın tutarlı uygulanmasıdır.'
        },
        {
            q: '13 puan ve üzeri bir tahmin ne anlama gelir?',
            a: 'Çoğu takımda 13 ve üzeri, işin tek sprintte güvenle tamamlanamayacak kadar büyük veya belirsiz olduğuna işarettir; madde daha küçük parçalara bölünmelidir.'
        },
        {
            q: 'Yeni kurulan bir takım ilk sprintini nasıl planlamalı?',
            a: 'Henüz velocity verisi olmadığı için ilk birkaç sprint tahminî kapasiteyle planlanır ve muhafazakâr davranılır. İki-üç sprint sonunda oluşan ortalama hız, sonraki planlamaların temelini oluşturur.'
        }
    ]
}
