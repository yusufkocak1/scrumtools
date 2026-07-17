// Blog makalesi: Ürün Sahibi, Ürün İş Listesi'ni Nasıl Organize Etmeli?
export default {
    slug: 'urun-is-listesi-nasil-organize-edilmeli',
    title: 'Ürün Sahibi, Ürün İş Listesi\'ni (Product Backlog) Nasıl Organize Etmeli?',
    metaTitle: 'Product Backlog Nasıl Organize Edilir? Ürün Sahibi Rehberi | ScrumTools Blog',
    description: 'Ürün İş Listesi (Product Backlog) nasıl organize edilir? DEEP kriterleri, önceliklendirme teknikleri (MoSCoW, WSJF), refinement pratikleri ve yaygın hatalar bu rehberde.',
    category: 'Backlog Yönetimi',
    date: '2026-07-17',
    readingMinutes: 9,
    excerpt: 'Ürün İş Listesi, ürünün tek ve yaşayan yol haritasıdır; değeri ise nasıl organize edildiğine bağlıdır. Bu rehberde Ürün Sahipleri için DEEP kriterlerini, önceliklendirme tekniklerini ve sürdürülebilir bir refinement düzenini ele alıyoruz.',
    related: ['scrum-nedir', 'story-point-nedir', 'scrum-poker-nedir'],
    html: `
<p><strong>Ürün İş Listesi (Product Backlog)</strong>, ürünü iyileştirmek için gereken her şeyin — özellikler, düzeltmeler, teknik iyileştirmeler, araştırmalar — tek, sıralı ve yaşayan listesidir. Scrum Kılavuzu'na göre bu listenin içeriğinden, sıralamasından ve anlaşılırlığından sorumlu tek kişi <strong>Ürün Sahibi'dir (Product Owner)</strong>. Bu sorumluluk devredilebilir işler içerse de hesap verebilirlik devredilemez.</p>

<p>İyi organize edilmiş bir backlog, takımın pusulasıdır: Sprint planlamaları hızlanır, paydaş beklentileri yönetilebilir hâle gelir ve "sırada ne var?" sorusunun cevabı her an bellidir. Kötü organize edilmiş bir backlog ise tam tersine, yüzlerce bayat maddenin biriktiği, kimsenin güvenmediği bir depoya dönüşür. Aradaki farkı yaratan, aşağıdaki ilkelerdir.</p>

<h2>Sağlıklı Bir Backlog'un Ölçütü: DEEP</h2>

<p>Roman Pichler ve Mike Cohn'un önerdiği <strong>DEEP</strong> kısaltması, iyi bir backlog'un dört özelliğini tanımlar:</p>

<ul>
<li><strong>D — Detailed appropriately (Uygun ayrıntıda):</strong> Listenin üst sıralarındaki maddeler sprint'e girecek kadar ayrıntılı, alt sıralardakiler ise kasıtlı olarak kaba olmalıdır. Aylar sonra yapılacak bir işi bugün ince ayrıntısıyla yazmak, büyük olasılıkla çöpe gidecek analiz demektir.</li>
<li><strong>E — Estimated (Tahminlenmiş):</strong> Üst sıralardaki maddelerin büyüklüğü takımca tahminlenmiş olmalıdır; sıralama kararı ancak değer ile maliyet birlikte görülünce sağlıklı verilir. Tahminleme pratiği için <a href="/blog/story-point-nedir">story point rehberimize</a> bakabilirsiniz.</li>
<li><strong>E — Emergent (Evrilen):</strong> Backlog bir kez yazılıp dondurulan bir belge değildir; öğrenildikçe yeni maddeler eklenir, mevcutlar değişir veya silinir.</li>
<li><strong>P — Prioritized (Sıralanmış):</strong> Backlog bir küme değil, sıralı bir listedir. İki maddenin "eşit öncelikte" olması kabul edilmez; her maddenin listede tek ve net bir yeri vardır.</li>
</ul>

<h2>Sıralama Önceliklendirmeden Fazlasıdır</h2>

<p>Scrum Kılavuzu bilinçli olarak "öncelik" değil <strong>"sıra" (order)</strong> kelimesini kullanır. Çünkü bir maddenin listedeki yerini yalnızca iş değeri belirlemez; Ürün Sahibi sıralarken şu etkenleri birlikte tartar:</p>

<ul>
<li><strong>Değer:</strong> Maddenin kullanıcıya veya işletmeye katkısı,</li>
<li><strong>Maliyet:</strong> Tahmini efor — küçük ama değerli işler öne çekilebilir,</li>
<li><strong>Risk ve belirsizlik:</strong> Riskli varsayımları erken doğrulamak, geç sürprizlerden ucuzdur,</li>
<li><strong>Bağımlılıklar:</strong> Teknik veya işlevsel ön koşullar,</li>
<li><strong>Öğrenme fırsatı:</strong> Bir sonraki kararları besleyecek geri bildirim.</li>
</ul>

<h2>Yaygın Önceliklendirme Teknikleri</h2>

<ul>
<li><strong>MoSCoW:</strong> Maddeleri Must / Should / Could / Won't kategorilerine ayırır. Basit ve paydaşlarla konuşması kolaydır; ancak kategori içi sıralamayı çözmez, kaba bir ilk eleme aracı olarak görülmelidir.</li>
<li><strong>Değer/Efor matrisi:</strong> Maddeler iki eksende puanlanır; yüksek değer + düşük efor bölgesindekiler öne alınır. Hızlı kazanımları görünür kılar.</li>
<li><strong>WSJF (Weighted Shortest Job First):</strong> Gecikme maliyetinin iş süresine bölünmesiyle hesaplanır; özellikle birden çok paydaşın yarıştığı ortamlarda sıralamayı sezgiden çıkarıp tartışılabilir bir formüle bağlar.</li>
<li><strong>Kano modeli:</strong> Özellikleri temel beklenti, performans ve heyecan verici olarak sınıflar; "hangi özellik müşteriyi gerçekten memnun eder?" sorusuna odaklanır.</li>
</ul>

<p>Teknik seçiminden daha önemlisi tutarlılıktır: Hangi yöntemi seçerseniz seçin, sıralama gerekçeleriniz paydaşlara açıklanabilir olmalıdır.</p>

<h2>Yapısal Düzen: Epic, Hikâye ve Dikey Dilimleme</h2>

<p>Büyük backlog'lar yapı olmadan yönetilemez. Yaygın hiyerarşi; büyük iş alanlarını temsil eden <strong>epic</strong>'ler, bunların altında sprint'e sığacak boyuttaki <strong>kullanıcı hikâyeleri</strong> ve gerektiğinde hikâyelerin altındaki görevlerdir. Hikâyeleri bölerken altın kural <strong>dikey dilimleme</strong>dir: Her dilim, katmanları uçtan uca kesen, tek başına değer üreten bir parça olmalıdır ("önce veritabanı, sonra arayüz" gibi yatay dilimler sprint sonunda gösterilebilir değer üretmez.) İyi bir hikâyenin ölçütü olarak <strong>INVEST</strong> kriterleri kullanılabilir: bağımsız, müzakere edilebilir, değerli, tahminlenebilir, küçük ve test edilebilir.</p>

<h2>Refinement: Backlog'u Canlı Tutan Düzen</h2>

<p>Backlog organizasyonu tek seferlik bir temizlik değil, düzenli bir bakım işidir. <strong>Backlog refinement</strong> (düzenleme), gelecek sprint'lerin adaylarını netleştirmek, bölmek ve tahminlemek için takımla birlikte yapılır; yaygın pratik, sprint kapasitesinin yüzde onunu geçmeyecek şekilde haftalık bir oturumdur. Sağlıklı bir hedef, backlog'un tepesinde her an iki-üç sprint'lik "hazır" iş bulundurmaktır — buna <em>hazır tanımı (Definition of Ready)</em> eşlik edebilir: Bir madde; kabul kriterleri yazılmış, bağımlılıkları çözülmüş ve takımca tahminlenmişse sprint'e girmeye hazırdır.</p>

<p>Refinement aynı zamanda budama zamanıdır: Aylardır sıralamanın dibinde bekleyen ve öngörülebilir gelecekte yapılmayacak maddeleri silmekten çekinmeyin. İki yüz maddelik "her şeyi hatırlayan" bir liste değil, yüz maddelik güvenilir bir liste hedefleyin.</p>

<h2>Yaygın Hatalar</h2>

<ul>
<li><strong>Depo backlog:</strong> Her fikrin sonsuza dek saklandığı listeler güveni yitirir; kimse dibini okumaz, sıralama anlamını kaybeder.</li>
<li><strong>Paydaş baskısıyla sıralama:</strong> En yüksek sesle konuşan paydaşın işinin öne alınması, sıralamayı değer yerine politik güce bağlar. Şeffaf bir önceliklendirme tekniği, Ürün Sahibi'nin en güçlü savunmasıdır.</li>
<li><strong>Teknik borcu dışlamak:</strong> Yalnızca özellik maddelerinden oluşan backlog, teknik borcun görünmez birikmesine yol açar. Teknik iyileştirmeler de backlog maddesidir ve aynı listede sıralanır.</li>
<li><strong>Komiteyle sıralama:</strong> Herkesin görüşü alınır, ancak son sıralama kararı tek kişinindir. Kararın sahipsizleşmesi, backlog'un tutarlılığını bozar.</li>
<li><strong>Sprint içine sızan değişiklik:</strong> Backlog'un evrilmesi serbesttir; ancak devam eden sprint'in kapsamı korunur. Yeni gelen "acil" işin doğal adresi, bir sonraki sprint planlamasıdır.</li>
</ul>

<p>Bu düzeni kurarken kullanılan aracın sıralamayı zahmetsiz kılması önemlidir. <a href="/">ScrumTools</a>'un backlog ekranında maddeler sürükle-bırak ile sıralanır; epic ve etiket yapısı, özel alanlar ve filtrelerle büyük listeler yönetilebilir kalır, refinement oturumunda uzlaşılan tahminler doğrudan maddelere işlenir.</p>

<h2>Sonuç</h2>

<p>Ürün İş Listesi'ni organize etmek, Ürün Sahibi'nin en görünür ve en stratejik işidir: Sıralı tek bir liste tutmak, üst sıraları hazır ve tahminli bulundurmak, alt sıraları kasıtlı olarak kaba bırakmak ve listeyi düzenli refinement ile canlı tutmak. Bu disiplini kuran bir Ürün Sahibi, takımına yalnızca düzenli bir liste değil; her sprint planlamasında güvenle üzerine basılacak sağlam bir zemin sunar.</p>
`,
    faq: [
        {
            q: 'Product Backlog ile Sprint Backlog arasındaki fark nedir?',
            a: 'Product Backlog ürünün tamamı için sıralı ve yaşayan iş listesidir ve Ürün Sahibi\'nin sorumluluğundadır; Sprint Backlog ise tek bir sprint için seçilen maddeleri, sprint hedefini ve teslim planını içerir ve geliştiricilere aittir.'
        },
        {
            q: 'Backlog\'u kim sıralar, takım itiraz edebilir mi?',
            a: 'Sıralama kararı ve hesap verebilirlik Ürün Sahibi\'ne aittir. Takım ve paydaşlar görüş bildirir, teknik bağımlılıkları ve riskleri aktarır; ancak son karar tek elden verilir. Bu, tutarlılığın ve hızlı karar almanın ön koşuludur.'
        },
        {
            q: 'Backlog refinement resmî bir Scrum etkinliği midir?',
            a: 'Hayır; Scrum Kılavuzu refinement\'ı sürekli yapılan bir faaliyet olarak tanımlar, ayrı bir zorunlu toplantı olarak tanımlamaz. Buna rağmen çoğu takım, pratikte haftalık düzenli bir refinement oturumu planlamayı faydalı bulur.'
        },
        {
            q: 'Backlog ne kadar uzun olmalı?',
            a: 'Kesin bir sayı yoktur; ölçüt güvenilirliktir. Tepede iki-üç sprint\'lik hazır iş, altında kabalaşan makul bir kuyruk sağlıklıdır. Öngörülebilir gelecekte yapılmayacak maddeleri silmek, listeyi küçültmek değil güçlendirmektir.'
        },
        {
            q: 'Bug\'lar ve teknik borç backlog\'a girer mi?',
            a: 'Evet. Ürünü iyileştiren her iş — özellik, hata düzeltme, teknik iyileştirme, araştırma — tek bir Product Backlog\'ta toplanır ve birlikte sıralanır. Ayrı "gölge listeler" tutmak, önceliklendirmenin bütünlüğünü bozar.'
        }
    ]
}
