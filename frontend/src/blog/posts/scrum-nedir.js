// Blog makalesi: Scrum Nedir?
export default {
    slug: 'scrum-nedir',
    title: 'Scrum Nedir? Rolleri, Etkinlikleri ve Eserleriyle Kapsamlı Rehber',
    metaTitle: 'Scrum Nedir? Roller, Etkinlikler ve Temel Kavramlar | ScrumTools Blog',
    description: 'Scrum nedir, nasıl çalışır? Scrum rolleri (Product Owner, Scrum Master, Developers), sprint etkinlikleri ve eserleri; yeni başlayanlar için kapsamlı rehber.',
    category: 'Temel Kavramlar',
    date: '2026-06-22',
    readingMinutes: 9,
    excerpt: 'Scrum, karmaşık ürünler geliştiren takımların işlerini kısa döngülerle planlayıp düzenli olarak denetlemesine dayanan, dünyanın en yaygın çevik çerçevesidir. Bu rehber; rolleri, etkinlikleri ve eserleriyle Scrum\'ın bütününü aktarır.',
    related: ['scrum-poker-nedir', 'sprint-retrospektifi-nedir', 'kanban-ile-scrum-farklari', 'urun-is-listesi-nasil-organize-edilmeli'],
    html: `
<p><strong>Scrum</strong>, karmaşık problemler üzerinde çalışan takımların, işlerini <em>sprint</em> adı verilen kısa ve sabit uzunluktaki döngüler hâlinde planlamasına, her döngünün sonunda kullanılabilir bir ürün parçası ortaya koymasına ve hem ürünü hem çalışma biçimini düzenli olarak gözden geçirmesine dayanan bir çerçevedir (framework). 1990'ların başında Ken Schwaber ve Jeff Sutherland tarafından geliştirilmiş, ilk kez 1995'te resmî olarak sunulmuştur. Çerçevenin güncel ve tek resmî tanımı, ücretsiz yayımlanan <em>Scrum Guide</em> belgesidir (son sürüm: Kasım 2020).</p>

<p>Scrum bir metodoloji değil, bilinçli olarak eksik bırakılmış bir çerçevedir: Ne kod inceleme süreci ne dallanma stratejisi ne de toplantı tutanağı şablonu tanımlar. Az sayıda rol, etkinlik ve eser (artifact) belirler; geri kalanını takımın bağlamına göre doldurmasını bekler.</p>

<h2>Scrum'ın Temeli: Ampirizm</h2>

<p>Scrum, ampirik süreç kontrolüne dayanır ve üç sütun üzerinde yükselir:</p>

<ul>
<li><strong>Şeffaflık:</strong> İş ve ilerleme, kararları etkileyen herkes tarafından görülebilir olmalıdır.</li>
<li><strong>Denetim (Inspection):</strong> Ürün ve süreç, sapmaları erken yakalamak için düzenli aralıklarla gözden geçirilir.</li>
<li><strong>Uyarlama (Adaptation):</strong> Denetimde saptanan sapmalara göre plan veya çalışma biçimi gecikmeden düzeltilir.</li>
</ul>

<p>Bu döngüyü mümkün kılan beş Scrum değeri vardır: <strong>bağlılık, odak, açıklık, saygı ve cesaret</strong>. Kurallara şeklen uyulan ama bu değerlerin yaşatılmadığı ortamlarda Scrum, içi boş bir tören dizisine dönüşür.</p>

<h2>Scrum Rolleri</h2>

<ul>
<li><strong>Ürün Sahibi (Product Owner):</strong> Üründen elde edilen değeri en üst düzeye çıkarmaktan sorumludur. Ürün hedefini belirler, Product Backlog'u yönetir ve önceliklendirir. Takım başına tek bir Ürün Sahibi bulunur; bu, kararların tek elden ve hızlı verilmesini sağlar.</li>
<li><strong>Scrum Master:</strong> Scrum'ın anlaşılmasından ve etkin uygulanmasından sorumludur. Takımın önündeki engelleri kaldırır, etkinliklerin amacına uygun yürümesini kolaylaştırır ve organizasyona çevik çalışma konusunda koçluk yapar. Scrum Master bir proje yöneticisi değil, hizmetkâr liderdir.</li>
<li><strong>Geliştiriciler (Developers):</strong> Her sprintte kullanılabilir bir ürün parçası (Increment) üretmekle yükümlü olan uzmanlardır. Yazılımcı, testçi, tasarımcı gibi ayrımlar Scrum'ın gözünde yoktur; takım, işi uçtan uca teslim edebilecek tüm yetkinlikleri içermelidir. Önerilen takım büyüklüğü, roller dahil 10 kişiyi aşmaz.</li>
</ul>

<h2>Scrum Etkinlikleri</h2>

<ul>
<li><strong>Sprint:</strong> Diğer tüm etkinlikleri kapsayan, bir ay veya daha kısa sabit süreli döngüdür. Bir sprint biter bitmez yenisi başlar.</li>
<li><strong>Sprint Planlama:</strong> Sprintin açılışında yapılır; "Bu sprint neden değerli?", "Ne yapılabilir?" ve "Nasıl yapılacak?" soruları yanıtlanır. Çıktısı, sprint hedefi ve Sprint Backlog'dur. Efor tahmini için birçok takım <a href="/blog/scrum-poker-nedir">Scrum Poker</a> tekniğinden yararlanır.</li>
<li><strong>Daily Scrum:</strong> Geliştiricilerin her gün aynı saatte yaptığı 15 dakikalık planlama toplantısıdır. Amaç durum raporu vermek değil, sprint hedefine göre günün planını gözden geçirmek ve engelleri görünür kılmaktır.</li>
<li><strong>Sprint Review:</strong> Sprintin sonunda takım, ortaya çıkan ürün parçasını paydaşlara sunar ve geri bildirim toplar. Review bir demo töreninden ibaret değildir; ürünün gidişatına dair kararların şekillendiği bir çalışma oturumudur.</li>
<li><strong>Sprint Retrospektifi:</strong> Takımın çalışma biçimini gözden geçirip iyileştirme adımları belirlediği kapanış etkinliğidir. Ayrıntılı bir rehber için <a href="/blog/sprint-retrospektifi-nedir">sprint retrospektifi yazımıza</a> göz atabilirsiniz.</li>
</ul>

<h2>Scrum Eserleri (Artifacts)</h2>

<ul>
<li><strong>Product Backlog:</strong> Ürün için gereken her şeyin sıralı, yaşayan listesidir. Taahhüdü <em>Ürün Hedefi</em>'dir.</li>
<li><strong>Sprint Backlog:</strong> Sprint hedefi, seçilen backlog maddeleri ve teslim planından oluşur. Taahhüdü <em>Sprint Hedefi</em>'dir.</li>
<li><strong>Increment:</strong> Sprint içinde üretilen, Bitti Tanımı'nı (Definition of Done) karşılayan kullanılabilir ürün parçasıdır. Taahhüdü <em>Bitti Tanımı</em>'dır.</li>
</ul>

<h2>Scrum Ne Değildir?</h2>

<ul>
<li><strong>Hız garantisi değildir:</strong> Scrum işleri kendiliğinden hızlandırmaz; sorunları erken görünür kılar. Hız, görünür kılınan sorunların çözülmesiyle gelir.</li>
<li><strong>Yalnızca yazılım için değildir:</strong> Çerçeve; donanım, pazarlama, eğitim gibi karmaşık iş alanlarında da uygulanmaktadır.</li>
<li><strong>Toplantı dizisi değildir:</strong> Etkinlikler amaca hizmet ettiği ölçüde değerlidir; törenleşen ama denetim ve uyarlama üretmeyen toplantılar Scrum'ın kendisi değil, karikatürüdür.</li>
</ul>

<p>Scrum'ı uygulamaya başlarken en sık karşılaşılan pratik ihtiyaç, backlog'un, sprint panosunun ve etkinlik çıktılarının herkes için şeffaf olduğu ortak bir çalışma alanıdır. <a href="/">ScrumTools</a>; backlog yönetimi, sprint panosu, planlama pokeri ve retro panolarını tek platformda sunarak bu şeffaflığı ilk günden kurmayı kolaylaştırır — dileyen takımlar ücretsiz paketle başlayıp süreçlerini adım adım taşıyabilir.</p>

<h2>Sonuç</h2>

<p>Scrum'ın gücü karmaşıklığında değil, yalınlığındadır: kısa döngüler, net sorumluluklar, düzenli denetim ve uyarlama. Çerçeveyi bir kurallar listesi olarak değil, ampirik düşünme biçimi olarak benimseyen takımlar; belirsizliği yönetilebilir parçalara böler, hatalarını erken yakalar ve her sprintte hem ürünlerini hem kendilerini geliştirir. Başlamak için mükemmel bir süreç beklemek gerekmez — Scrum, tam da mükemmel olmayan süreçleri adım adım iyileştirmek için tasarlanmıştır.</p>
`,
    faq: [
        {
            q: 'Scrum ile Agile (Çeviklik) aynı şey midir?',
            a: 'Hayır. Agile, 2001 tarihli Çevik Manifesto\'da tanımlanan değer ve ilkeler bütünüdür; Scrum ise bu ilkeleri hayata geçirmenin en yaygın çerçevelerinden biridir. Kanban ve Extreme Programming (XP) gibi başka çevik yaklaşımlar da vardır.'
        },
        {
            q: 'Bir sprint ne kadar sürmelidir?',
            a: 'Scrum Kılavuzu bir ay veya daha kısa olmasını şart koşar; sektörde en yaygın tercih iki haftadır. Kısa sprintler daha sık geri bildirim sağlar, ancak etkinliklerin göreli maliyetini artırır; takım kendi bağlamına göre sabit bir süre seçmelidir.'
        },
        {
            q: 'Scrum Master ile proje yöneticisi arasındaki fark nedir?',
            a: 'Proje yöneticisi kapsam, takvim ve kaynakları yönetir; Scrum Master ise süreci ve takımın etkinliğini gözetir, engelleri kaldırır ve organizasyona koçluk yapar. Scrum Master, takıma iş atamaz ve ilerlemeyi kişiler üzerinden takip etmez.'
        },
        {
            q: 'Scrum küçük takımlar için de uygun mudur?',
            a: 'Evet; Scrum zaten küçük, çok yetkinlikli takımlar için tasarlanmıştır. Önerilen büyüklük 10 kişiyi aşmaz. Daha büyük ürünlerde birden çok Scrum takımı, Nexus veya LeSS gibi ölçekleme yaklaşımlarıyla koordine edilir.'
        },
        {
            q: 'Scrum\'a geçiş ne kadar sürer?',
            a: 'Etkinlikleri ve rolleri kurmak birkaç sprint alır; ancak değerlerin içselleşmesi ve ampirik düşünmenin yerleşmesi aylar sürebilir. İlk sprintlerde aksaklık normaldir — retrospektifler tam da bu aksaklıkları ele almak için vardır.'
        }
    ]
}
