// Blog makalesi: Büyüyen Scrum Takımı Nasıl Bölünür?
export default {
    slug: 'buyuyen-scrum-takimi-nasil-bolunur',
    title: 'Scrum Takımım Çok Büyüdü: Takımı Nasıl Bölmeliyim, Nelere Dikkat Etmeliyim?',
    metaTitle: 'Büyüyen Scrum Takımı Nasıl Bölünür? | ScrumTools Blog',
    description: 'Scrum takımınız 10 kişiyi aştıysa bölme zamanı gelmiş olabilir. Takım nasıl bölünür, özellik takımı mı bileşen takımı mı, tek backlog mu? Rehber ve yaygın hatalar.',
    category: 'Ölçeklendirme',
    date: '2026-07-17',
    readingMinutes: 9,
    excerpt: 'Büyüyen bir Scrum takımında iletişim maliyeti üye sayısından çok daha hızlı artar. Takımı doğru sınırlardan bölmek, tek backlog disiplinini korumak ve takımlar arası koordinasyonu baştan kurgulamak, ölçeklenmenin üç kritik ayağıdır.',
    related: ['scrum-nedir', 'sprint-retrospektifi-nedir', 'urun-is-listesi-nasil-organize-edilmeli'],
    html: `
<p>Ürün büyür, işe alımlar hızlanır ve bir gün daily scrum'ınızın 25 dakika sürdüğünü, yarısında ekranın karşısında sessizce bekleyen sekiz kişi olduğunu fark edersiniz. <strong>Scrum Kılavuzu (2020), Scrum takımının 10 veya daha az kişiden oluşmasını önerir</strong> ve bunun bir nedeni vardır: Küçük takımlar daha iyi iletişim kurar ve daha üretkendir. Takım bu eşiği belirgin biçimde aştığında soru artık "bölmeli miyiz?" değil, "<em>nasıl</em> bölmeliyiz?" sorusudur.</p>

<p>İletişim maliyeti üye sayısıyla doğrusal değil, kombinasyonel büyür: n kişilik bir takımda n(n-1)/2 ikili iletişim kanalı vardır. 6 kişilik bir takımda 15 kanal varken, 12 kişide bu sayı 66'ya çıkar. Takım büyüdükçe herkesin her şeyden haberdar olması imkânsızlaşır, kararlar yavaşlar ve etkinlikler kalabalık toplantılara dönüşür.</p>

<h2>Takımın Çok Büyüdüğünü Gösteren İşaretler</h2>

<ul>
<li><strong>Daily scrum 15 dakikaya sığmıyor</strong> ve konuşulanların çoğu, dinleyenlerin yarısını ilgilendirmiyor.</li>
<li><strong>Doğal alt gruplar oluşmuş:</strong> "Ödeme tarafındakiler", "mobilciler" gibi gayriresmî kümeler zaten ayrı çalışıyor, sprint planlamada birbirlerinin işlerine katkı vermiyor.</li>
<li><strong>Etkinliklerde katılım düşüyor:</strong> Planlama ve retrospektifte hep aynı üç-dört kişi konuşuyor, geri kalanlar izliyor.</li>
<li><strong>Sprint backlog'u tek bir hedef etrafında toplanamıyor;</strong> sprint, birbirinden bağımsız işlerin torbasına dönüşmüş durumda.</li>
<li><strong>Ürün Sahibi yetişemiyor:</strong> Tek kişi bu genişlikteki backlog'u rafine etmeye ve tüm sorulara yanıt vermeye yetmiyor.</li>
</ul>

<p>Bu işaretlerin birkaçı bir aradaysa, takımı büyütmeye devam etmek yerine bölmeyi planlamanın zamanı gelmiştir. Ancak bölme işlemi bir yeniden örgütlenmedir; aceleye getirilirse iletişim sorununu çözmek yerine yerine bağımlılık sorunu koyarsınız.</p>

<h2>Bölmeden Önce: Sınırı Doğru Yerden Çizin</h2>

<p>En kritik karar, takımları <em>neye göre</em> böleceğinizdir. İki temel yaklaşım vardır:</p>

<ul>
<li><strong>Bileşen takımları (component teams):</strong> Mimari katmanlara göre bölünür — backend takımı, frontend takımı, mobil takımı. İlk bakışta doğal görünür; ancak müşteriye giden hemen her özellik birden fazla takıma dokunacağı için takımlar arası bağımlılık, bekleme ve el değiştirme (hand-off) maliyeti hızla büyür.</li>
<li><strong>Özellik takımları (feature / stream-aligned teams):</strong> Değer akışına göre bölünür — "ödeme deneyimi", "raporlama", "onboarding" gibi. Her takım, kendi alanındaki bir işi baştan sona (tasarım, backend, frontend, test) teslim edebilecek kadar çapraz fonksiyoneldir.</li>
</ul>

<p>Modern ölçeklendirme yaklaşımlarının (LeSS, Nexus, Team Topologies) ortak önerisi mümkün olduğunca <strong>özellik takımlarına</strong> bölünmektir: Bağımsız değer üretebilen takımlar, koordinasyon ihtiyacını en aza indirir. Burada <strong>Conway Yasası</strong>'nı da hatırlamakta yarar var: Sistemlerin mimarisi, onları üreten organizasyonun iletişim yapısını yansıtma eğilimindedir. Takım sınırlarınızı nereden çizerseniz, yazılımınızın modül sınırları da zamanla oraya kayar — bu yüzden takım sınırları, ürününüzde <em>olmasını istediğiniz</em> mimari sınırlarla örtüşmelidir.</p>

<h2>Adım Adım Bölünme</h2>

<ol>
<li><strong>Değer akışlarını haritalayın:</strong> Ürünün müşteriye değer ürettiği ana alanları listeleyin. Takım sayısını buradan türetin; her takım 4-9 kişi aralığında kalmalıdır.</li>
<li><strong>Takım kompozisyonunu kurgulayın:</strong> Her takım kendi alanındaki bir işi dışarıya bağımlı olmadan "bitti" durumuna getirebilmelidir. Kritik uzmanlıkların (ör. tek DevOps uzmanı) tek takımda kilitlenmemesine dikkat edin.</li>
<li><strong>Takımı sürece dahil edin:</strong> Bölünme kararı yukarıdan tebliğ edilmek yerine takımla birlikte şekillendirilirse sahiplenme çok daha yüksek olur. Bazı organizasyonlar sınırları yönetimin belirlediği, kişilerin ise takımını kendisinin seçtiği "self-selection" çalıştayları düzenler ve iyi sonuç alır.</li>
<li><strong>Backlog ve rol yapısına karar verin:</strong> Ürün tekse backlog da tek kalmalıdır (aşağıda ayrıntılı ele alıyoruz). Her yeni takımın Scrum Master ihtiyacını ve Ürün Sahibi'nin nasıl destekleneceğini netleştirin.</li>
<li><strong>Sprint sınırında geçiş yapın:</strong> Bölünmeyi sprint ortasında değil, bir sprintin bitip yenisinin başladığı doğal sınırda gerçekleştirin. İlk sprintleri bilinçli olarak küçük hedeflerle planlayın.</li>
<li><strong>Birkaç sprint sonra gözden geçirin:</strong> İlk bölünme nadiren mükemmeldir. İki-üç sprint sonra takımlar arası bağımlılıkları ölçün ve gerekiyorsa sınırları ayarlayın.</li>
</ol>

<h2>Nelere Dikkat Etmelisiniz?</h2>

<h3>Tek ürün, tek backlog</h3>

<p>Hem LeSS hem Nexus'un temel ilkesi aynıdır: <strong>Bir ürünün tek bir Ürün Backlog'u ve tek bir Ürün Sahibi olur.</strong> Takım başına ayrı backlog'lar açmak, kısa sürede öncelik çatışmalarına ve "bizim backlog / sizin backlog" bölgeciliğine yol açar. Takımlar sprint planlamada işlerini bu ortak backlog'dan çeker; Ürün Sahibi yetişemiyorsa alan bazında destek verecek kişilerle güçlendirilir, ancak önceliklendirmenin tek sahibi olmaya devam eder.</p>

<h3>Ortak Bitti Tanımı</h3>

<p>Aynı ürün üzerinde çalışan takımların ortak bir <strong>Definition of Done</strong> üzerinde anlaşması gerekir; aksi halde bir takımın "bitti"si diğerinin teknik borcu olur. Takımlar bu ortak tanımın üzerine kendi ek standartlarını koyabilir ama altına inemez.</p>

<h3>Takımlar arası koordinasyon</h3>

<p>Bölünmeyle birlikte "takım içi iletişim" sorununun yerini "takımlar arası koordinasyon" ihtiyacı alır. Bunu baştan tasarlayın:</p>

<ul>
<li><strong>Scrum of Scrums:</strong> Her takımdan bir temsilcinin katıldığı, bağımlılık ve engellerin konuşulduğu kısa ve düzenli senkronizasyon toplantısı.</li>
<li><strong>Ortak refinement:</strong> Büyük işlerin hangi takıma gideceğinin ve takımlar arası kesişimlerin konuşulduğu ortak backlog rafine oturumları.</li>
<li><strong>Pratik toplulukları (communities of practice):</strong> Takımlar özellik odaklı bölündüğünde, aynı uzmanlığa sahip kişilerin (frontend, test, mimari) düzenli buluştuğu gönüllü topluluklar, teknik tutarlılığı ve bilgi paylaşımını korur.</li>
</ul>

<h3>Velocity'yi sıfırdan kurun, kıyaslamayın</h3>

<p>Bölünen takımların story point ölçekleri artık ortak değildir; her takım kendi referanslarını ve kendi velocity'sini zamanla oluşturur. Eski büyük takımın velocity'sini ikiye bölerek yeni takımlara hedef vermek de, takımların velocity'lerini birbiriyle kıyaslamak da yanıltıcıdır ve puan enflasyonundan başka bir sonuç üretmez.</p>

<h3>Yeni takımlar, yeni takım dinamikleri</h3>

<p>Bölünme, Tuckman'ın deyimiyle takımları yeniden "forming" evresine döndürür: Yerleşik güven ve çalışma alışkanlıkları kısmen sıfırlanır. İlk sprintlerde hız düşüşü normaldir; bunu bir başarısızlık değil, yatırım maliyeti olarak planlayın. Her yeni takımın kendi çalışma anlaşmasını (working agreement) kurması ve ilk retrospektiflerini aksatmadan yapması bu evreyi kısaltır.</p>

<h2>Yaygın Hatalar</h2>

<ul>
<li><strong>Katmana göre bölmek:</strong> Backend/frontend ayrımı, her özelliği iki takımın ortak projesine çevirir; bekleme süreleri ve entegrasyon sorunları katlanır.</li>
<li><strong>Kıdeme göre bölmek:</strong> "Güçlü takım - yeni takım" ayrımı bilgi tekelini kalıcılaştırır ve yeni takımı baştan başarısızlığa mahkûm eder. Deneyimi takımlar arasında dengeleyin.</li>
<li><strong>Backlog'u da bölmek:</strong> Tek ürünü fiilen iki ayrı ürünmüş gibi yönetmek, ürün bütünlüğünü ve önceliklendirmeyi bozar.</li>
<li><strong>Koordinasyonu şansa bırakmak:</strong> "Zaten aynı ofisteler, konuşurlar" varsayımı birkaç sprint içinde entegrasyon sürprizleriyle sonuçlanır.</li>
<li><strong>Bölünmeyi geciktirmek:</strong> 14-15 kişilik bir "takım", fiilen koordine olamayan bir kalabalıktır. Sorun kendini bölünmüş gayriresmî kümeler olarak zaten göstermeye başlamıştır; resmî yapı bunu ne kadar geç tanırsa geçiş o kadar sancılı olur.</li>
</ul>

<p>Dağıtık ve çok takımlı düzende ortak araçların önemi artar. <a href="/">ScrumTools</a>'ta her takım kendi planning poker oturumunu, retro panosunu ve scrum board'unu ayrı yürütürken, yapı tüm takımlar için ortak kaldığından takımlar arası tutarlılık da korunur — yeni bölünmüş takımların her birinin kendi ritüellerini hızla kurması için pratik bir başlangıçtır.</p>

<h2>Sonuç</h2>

<p>Scrum takımını bölmek bir başarısızlık değil, büyümenin doğal ve sağlıklı bir sonucudur. İyi bir bölünmenin üç ayağı vardır: sınırları mimari katmanlara göre değil <strong>değer akışına göre</strong> çizmek, <strong>tek backlog ve ortak Bitti Tanımı</strong> disiplinini korumak ve <strong>takımlar arası koordinasyonu baştan tasarlamak</strong>. Bunlara takımın karara katılımı ve ilk sprintlerdeki doğal yavaşlamaya tahammül de eklendiğinde, birkaç sprint içinde iki (veya daha fazla) odaklı ve hızlı takımla yola devam edersiniz — kalabalık tek bir takımın hiçbir zaman ulaşamayacağı bir çeviklikle.</p>
`,
    faq: [
        {
            q: 'Scrum takımı en fazla kaç kişi olmalı?',
            a: 'Scrum Kılavuzu (2020) tüm Scrum takımı için 10 veya daha az kişiyi önerir. Pratikte 5-9 geliştirici aralığı yaygındır; takım bu sınırı kalıcı olarak aştığında bölünme planlanmalıdır.'
        },
        {
            q: 'Takım bölününce her takıma ayrı Ürün Sahibi mi atanmalı?',
            a: 'Ürün tekse hayır. LeSS ve Nexus gibi ölçeklendirme çerçeveleri tek ürün için tek Ürün Sahibi ve tek Ürün Backlog\'u önerir. Ürün Sahibi alan bazında destekçilerle güçlendirilebilir, ancak önceliklendirmenin tek sahibi kalır.'
        },
        {
            q: 'Özellik takımı mı, bileşen takımı mı tercih edilmeli?',
            a: 'Mümkün olduğunca özellik (feature) takımı tercih edilmelidir: Bir işi baştan sona teslim edebilen çapraz fonksiyonel takımlar, bileşen takımlarının yarattığı takımlar arası bağımlılık ve bekleme maliyetini büyük ölçüde ortadan kaldırır.'
        },
        {
            q: 'Bölünmüş takımlar arasındaki koordinasyon nasıl sağlanır?',
            a: 'En yaygın mekanizmalar Scrum of Scrums senkronizasyon toplantıları, ortak backlog refinement oturumları ve uzmanlık bazlı pratik topluluklarıdır. İkiden fazla takım için Nexus veya LeSS gibi hafif ölçeklendirme çerçeveleri değerlendirilebilir.'
        },
        {
            q: 'Bölünmeden sonra takımların velocity\'si düşerse endişelenmeli miyim?',
            a: 'Hayır, ilk birkaç sprintte düşüş beklenen bir durumdur: Takımlar yeni dinamiklerini ve yeni tahmin referanslarını kuruyordur. Eski büyük takımın velocity\'siyle veya takımların birbiriyle kıyaslanması yanıltıcıdır; her takımın kendi trendine bakın.'
        },
        {
            q: 'Bölünme için en doğru zaman nedir?',
            a: 'Sprint sınırıdır: Bir sprint kapanıp yenisi başlarken geçiş yapmak, yarım kalan işlerin ve sahipliklerin havada kalmasını önler. Bölünme öncesinde son bir ortak retrospektifle geçişin nasıl yapılacağını takımla birlikte planlamak iyi bir pratiktir.'
        }
    ]
}
