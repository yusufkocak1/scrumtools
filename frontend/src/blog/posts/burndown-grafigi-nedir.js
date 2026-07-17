// Blog makalesi: Burndown Grafiği Nedir?
export default {
    slug: 'burndown-grafigi-nedir',
    title: 'Burndown Grafiği Nedir? Sprint İlerlemesi Nasıl Okunur?',
    metaTitle: 'Burndown Grafiği Nedir? Nasıl Okunur? | ScrumTools Blog',
    description: 'Burndown grafiği nedir, nasıl okunur? İdeal çizgi, tipik grafik desenleri, burnup farkı ve sprint takibinde doğru kullanım bu rehberde.',
    category: 'Metrikler',
    date: '2026-07-09',
    readingMinutes: 6,
    excerpt: 'Burndown grafiği, sprintte kalan işin zamana karşı azalışını gösteren en yaygın çevik takip aracıdır. Doğru okunduğunda sorunları günler öncesinden haber verir; yanlış kullanıldığında ise takım üzerinde baskı aracına dönüşür.',
    related: ['story-point-nedir', 'scrum-nedir', 'kanban-ile-scrum-farklari'],
    html: `
<p><strong>Burndown grafiği</strong>, bir sprint (veya sürüm) boyunca <em>kalan iş miktarının</em> zamana karşı çizildiği basit ama etkili bir izleme aracıdır. Yatay eksen sprint günlerini, dikey eksen kalan işi — genellikle story point veya görev sayısını — gösterir. Sprint sağlıklı ilerliyorsa çizgi, sol üstten sağ alta doğru düzenli biçimde "yanarak" iner; adını da buradan alır.</p>

<h2>Grafiğin Bileşenleri</h2>

<ul>
<li><strong>İdeal çizgi:</strong> Sprint başındaki toplam işin, son güne sıfır kalacak şekilde eşit hızla azaldığını varsayan düz referans doğrusudur. Bir hedef değil, kıyas aracıdır.</li>
<li><strong>Gerçekleşen çizgi:</strong> Her gün sonunda kalan işin gerçek değeridir. İşler tamamlandıkça düşer; sprint'e iş eklendiğinde yükselebilir.</li>
<li><strong>Kalan iş tanımı:</strong> Grafiğin anlamlı olması için "kalan iş"in nasıl sayıldığı net olmalıdır: yalnızca Bitti Tanımı'nı karşılayan işler düşülür; "yüzde 80'i bitti" gibi kısmi ilerlemeler sayılmaz.</li>
</ul>

<h2>Tipik Desenler ve Anlamları</h2>

<p>Burndown'ın asıl değeri, gün sonu rakamından çok çizginin <em>biçiminde</em> gizlidir. Sık görülen desenler şunlardır:</p>

<ul>
<li><strong>İdealin üzerinde seyreden çizgi:</strong> Takım planın gerisindedir. Erken fark edildiğinde kapsam daraltma veya engel kaldırma için zaman vardır; grafiğin işlevi tam da bu erken uyarıdır.</li>
<li><strong>Son günlerde dik düşüş (uçurum deseni):</strong> İşler sprint boyunca "devam ediyor" durumunda bekleyip son günlerde topluca kapanıyordur. Genellikle işlerin çok büyük parçalandığına veya Bitti Tanımı'nın sprint sonuna yığıldığına işaret eder.</li>
<li><strong>Platoya dönüşen çizgi:</strong> Günlerce değişmeyen kalan iş, görünmez bir engelin — bekleyen bir bağımlılık, tıkanan bir onay süreci — işaretidir ve Daily Scrum'da ele alınmalıdır.</li>
<li><strong>Yükselen çizgi:</strong> Sprint'e sonradan iş eklenmiştir (kapsam artışı). Ara sıra ve bilinçli olması normaldir; kronikleşmesi, sprint planlamasının veya kapsam disiplininin gözden geçirilmesi gerektiğini gösterir.</li>
</ul>

<h2>Burndown ile Burnup Farkı</h2>

<p><strong>Burnup grafiği</strong>, kalan işi değil <em>tamamlanan işi</em> ve toplam kapsamı iki ayrı çizgiyle gösterir. Aradaki fark kritik bir görünürlük sağlar: Burndown'da kapsam artışı ile yavaş ilerleme aynı görüntüyü (inmeyen çizgi) üretir ve birbirinden ayırt edilemez; burnup'ta ise kapsam çizgisinin yükseldiği açıkça görülür. Kapsamı sık değişen işlerde burnup daha dürüst bir resim sunar; sprint içi takipte ise yalınlığı nedeniyle burndown yaygındır.</p>

<h2>Doğru Kullanım İçin İlkeler</h2>

<ul>
<li><strong>Grafik takımındır:</strong> Burndown, takımın kendi ilerlemesini denetlemesi için bir araçtır; yönetimin bireysel performans izleme aracı değildir. Baskı aracına dönüştüğünde takımlar rakamları "iyi görünecek" biçimde şekillendirir ve grafik bilgi değerini yitirir.</li>
<li><strong>Günlük güncellenmelidir:</strong> Güncel olmayan burndown, yanlış güven verir. Veri girişi ne kadar zahmetsizse grafik o kadar güvenilirdir; ideali, pano üzerindeki iş durumu değiştikçe grafiğin kendiliğinden güncellenmesidir.</li>
<li><strong>Deseni konuşun, rakamı değil:</strong> Daily Scrum'da "neredeyiz" sorusundan daha değerlisi "çizgi neden bu şekli aldı" sorusudur. Plato ve uçurum desenleri, retrospektif için somut birer gündem maddesidir.</li>
</ul>

<p>Bu ilkelerin en zahmetli kısmı olan veri güncelliği, panoyla entegre araçlarda kendiliğinden çözülür. Örneğin <a href="/">ScrumTools</a>'ta burndown ve velocity grafikleri, sprint panosundaki iş durumlarından otomatik üretilir; takım ayrıca bir grafik beslemek zorunda kalmaz ve dashboard her an sprintin gerçek resmini gösterir.</p>

<h2>Sonuç</h2>

<p>Burndown grafiği, karmaşık bir sprint gerçekliğini tek bakışta okunabilir bir çizgiye indirger; gücü de sınırı da bu yalınlıktadır. Kalan işi dürüstçe sayan, grafiği günlük güncel tutan ve desenleri suçlama için değil öğrenme için okuyan takımlar için burndown, sprint sorunlarını günler öncesinden haber veren güvenilir bir erken uyarı sistemidir.</p>
`,
    faq: [
        {
            q: 'Burndown grafiği hangi birimle çizilmelidir?',
            a: 'En yaygın birimler story point ve görev sayısıdır; bazı takımlar kalan saat kullanır. Birimin kendisinden önemlisi tutarlılıktır: Sprint boyunca aynı birim kullanılmalı ve yalnızca Bitti Tanımı\'nı karşılayan işler düşülmelidir.'
        },
        {
            q: 'Burndown çizgisi hiç inmiyorsa ne anlama gelir?',
            a: 'Genellikle iki nedeni vardır: İşler görünmez bir engele takılmıştır (bağımlılık, onay süreci) veya sprint\'e sürekli yeni iş ekleniyordur. İlk durum Daily Scrum\'da, ikincisi retrospektifte ele alınmalıdır.'
        },
        {
            q: 'Burndown ile burnup grafiklerinden hangisi tercih edilmeli?',
            a: 'Sprint içi takip için yalınlığı nedeniyle burndown yaygındır. Kapsamı sık değişen sürüm veya proje takibinde ise kapsam değişimini ayrı çizgiyle gösteren burnup daha doğru bir resim sunar.'
        },
        {
            q: 'İdeal çizgiden sapma her zaman sorun mudur?',
            a: 'Hayır. İdeal çizgi bir kıyas aracıdır, taahhüt değildir; küçük sapmalar doğaldır. Önemli olan sapmanın sistematik hâle gelmesi veya çizginin plato ve uçurum gibi sorun desenleri çizmesidir.'
        },
        {
            q: 'Sprint burndown ile release burndown arasındaki fark nedir?',
            a: 'Sprint burndown tek sprintin günlük ilerlemesini, release burndown ise bir sürüm kapsamının sprint\'ler boyunca azalışını izler. İlki günlük denetim, ikincisi sürüm öngörüsü için kullanılır.'
        }
    ]
}
