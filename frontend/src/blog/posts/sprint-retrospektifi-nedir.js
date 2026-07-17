// Blog makalesi: Sprint Retrospektifi Nedir?
export default {
    slug: 'sprint-retrospektifi-nedir',
    title: 'Sprint Retrospektifi Nedir? Etkili Bir Retro Nasıl Yapılır?',
    metaTitle: 'Sprint Retrospektifi Nedir? Etkili Retro Rehberi | ScrumTools Blog',
    description: 'Sprint retrospektifi nedir, neden yapılır? Start-Stop-Continue, Sailboat gibi retro formatları, adım adım uygulama ve yaygın hatalar bu rehberde.',
    category: 'Scrum Etkinlikleri',
    date: '2026-07-13',
    readingMinutes: 8,
    excerpt: 'Sprint retrospektifi, takımın her sprint sonunda çalışma biçimini gözden geçirip somut iyileştirme adımları belirlediği Scrum etkinliğidir. Doğru yürütüldüğünde takımın en değerli sürekli iyileştirme mekanizmasıdır.',
    related: ['scrum-nedir', 'scrum-poker-nedir', 'burndown-grafigi-nedir'],
    html: `
<p><strong>Sprint retrospektifi</strong>, Scrum takımının her sprintin sonunda bir araya gelerek "Nasıl çalıştık ve nasıl daha iyi çalışabiliriz?" sorusunu yanıtladığı resmî Scrum etkinliğidir. Sprint Review ürünü ele alırken, retrospektif <em>süreci ve takımın kendisini</em> ele alır: iletişim, iş birliği, araçlar, teknik pratikler ve Bitti Tanımı (Definition of Done) masaya yatırılır.</p>

<p>Scrum Kılavuzu'na (Scrum Guide, 2020) göre retrospektifin amacı, "kaliteyi ve etkinliği artırmanın yollarını planlamaktır". Bir aylık sprintler için en fazla üç saatlik bir zaman kutusu öngörülür; daha kısa sprintlerde bu süre orantılı olarak kısalır. İki haftalık sprint yürüten takımların çoğu 60-90 dakikalık oturumlarla etkili sonuç alır.</p>

<h2>Retrospektif Neden Bu Kadar Önemli?</h2>

<p>Retrospektif, Scrum'ın ampirik döngüsünün — şeffaflık, denetim, uyarlama — takımın çalışma biçimine uygulandığı yerdir. Sprint'ler ürünü yinelemeli olarak geliştirir; retrospektif ise <em>takımı</em> yinelemeli olarak geliştirir. Bu etkinlik atlandığında veya şekilsel bir törene dönüştüğünde, aynı sorunlar sprint'ten sprint'e taşınır ve takım "koşarken tekerlek değiştirme" fırsatını kaybeder.</p>

<p>Etkili bir retrospektifin ön koşulu <strong>psikolojik güvenliktir</strong>: Katılımcılar hata ve aksaklıkları, suçlanma korkusu olmadan dile getirebilmelidir. Bu nedenle birçok takım oturumu, Norm Kerth'in ünlü "Ana Direktifi"ni hatırlatarak açar: <em>"Herkes, o an bildikleriyle ve elindeki imkânlarla yapabileceğinin en iyisini yapmıştır."</em> Amaç kusur aramak değil, sistemi iyileştirmektir.</p>

<h2>Adım Adım Etkili Bir Retrospektif</h2>

<ol>
<li><strong>Sahneyi kurun (5 dk):</strong> Amacı hatırlatın, gündemi paylaşın ve herkesin söz alacağı kısa bir açılış turu yapın. Oturumun ilk dakikalarında konuşan katılımcıların oturumun geri kalanına katkı verme olasılığı belirgin biçimde artar.</li>
<li><strong>Veri toplayın (10-15 dk):</strong> Seçtiğiniz formatın sütunlarına göre herkes gözlemlerini yazar. Fikirlerin önce bireysel yazılıp sonra paylaşılması, sesli grubun baskın çıkmasını engeller.</li>
<li><strong>İçgörü üretin (15-20 dk):</strong> Benzer maddeleri gruplayın, oylamayla en önemli iki-üç konuyu seçin ve kök nedenlerini tartışın. Her maddeye eşit süre ayırmak yerine, en çok oy alan konulara odaklanın.</li>
<li><strong>Aksiyon belirleyin (10-15 dk):</strong> Seçilen her konu için sahibi ve takip edilebilir tanımı olan somut bir aksiyon yazın. "İletişimi iyileştirelim" bir aksiyon değildir; "API değişiklikleri, değişiklikten önce #backend kanalında duyurulacak" bir aksiyondur.</li>
<li><strong>Kapanış (5 dk):</strong> Aksiyonları özetleyin ve bir sonraki retrospektifin açılışında bu aksiyonların durumunu gözden geçirmeyi takvime bağlayın.</li>
</ol>

<h2>Yaygın Retro Formatları</h2>

<ul>
<li><strong>Start - Stop - Continue:</strong> "Neye başlamalıyız, neyi bırakmalıyız, neyi sürdürmeliyiz?" En yalın ve en yaygın format; yeni takımlar için iyi bir başlangıçtır.</li>
<li><strong>Mad - Sad - Glad:</strong> Duygular üzerinden ilerler; takım ikliminin ve motivasyon sorunlarının konuşulması gerektiğinde etkilidir.</li>
<li><strong>4L (Liked - Learned - Lacked - Longed For):</strong> Öğrenmeye vurgu yapar; yeni bir teknoloji veya sürecin denendiği sprint'lerden sonra tercih edilebilir.</li>
<li><strong>Sailboat (Yelkenli):</strong> Takımı hedefe taşıyan rüzgârlar, yavaşlatan çapalar ve ilerideki kayalıklar (riskler) metaforuyla çalışır; resmin bütününü görmek için uygundur.</li>
</ul>

<p>Formatı düzenli aralıklarla değiştirmek, oturumların ezbere dönmesini engeller ve farklı türde sorunların gün yüzüne çıkmasını sağlar.</p>

<h2>Yaygın Hatalar</h2>

<ul>
<li><strong>Aksiyonsuz retro:</strong> Konuşulan ama hiçbir karara bağlanmayan oturumlar, katılımcılarda "nasılsa bir şey değişmiyor" yorgunluğu yaratır. Az sayıda ama gerçekten takip edilen aksiyon, uzun listelerden daha değerlidir.</li>
<li><strong>Suçlu arama:</strong> Tartışma kişilere yöneldiğinde güven kaybolur ve bir sonraki retroda kimse gerçek sorunları yazmaz. Kolaylaştırıcı, tartışmayı kişilerden sürece çevirmelidir.</li>
<li><strong>Yönetici gölgesi:</strong> Takım dışından yöneticilerin oturuma katılması, açık konuşmayı zorlaştırabilir. Retrospektif Scrum takımına aittir.</li>
<li><strong>Hep aynı sesler:</strong> Yazılı ve gerekiyorsa anonim katkı toplamak, sessiz üyelerin gözlemlerini de sürece dahil eder.</li>
</ul>

<p>Uzaktan çalışan takımlarda bu pratiklerin çoğu çevrimiçi panolarla yürütülür. <a href="/">ScrumTools</a>'un retro panosu; sütun şablonları, madde başına yorum ve oylama, tartışma zamanlayıcısı ve karar (aksiyon) panosu gibi ihtiyaçları tek yerde topladığı için, yukarıdaki akışı dağıtık bir takımla da aynı disiplinle uygulamak mümkündür.</p>

<h2>Sonuç</h2>

<p>Sprint retrospektifi, takımın kendi çalışma biçimi üzerinde düzenli ve yapılandırılmış biçimde düşündüğü tek Scrum etkinliğidir. Psikolojik güvenliği koruyan, oylamayla odak belirleyen ve her oturumdan sahipli, ölçülebilir aksiyonlarla çıkan takımlar için retrospektif, sürekli iyileştirmenin en güçlü motorudur. Küçük ama istikrarlı iyileştirmelerin bileşik etkisi, birkaç ay içinde takımın hızına ve iş kalitesine somut olarak yansır.</p>
`,
    faq: [
        {
            q: 'Sprint retrospektifi ne kadar sürmeli?',
            a: 'Scrum Kılavuzu bir aylık sprint için en fazla üç saat öngörür; sprint kısaldıkça süre orantılı azalır. İki haftalık sprintlerde 60-90 dakika yaygın ve yeterli bir süredir.'
        },
        {
            q: 'Retrospektife kimler katılır?',
            a: 'Tüm Scrum takımı katılır: geliştiriciler, Scrum Master ve Ürün Sahibi. Takım dışından yöneticilerin katılımı, açık konuşmayı zorlaştırabileceği için önerilmez.'
        },
        {
            q: 'Retrospektif ile Sprint Review arasındaki fark nedir?',
            a: 'Sprint Review üründeki ilerlemeyi ve geri bildirimi ele alır; retrospektif ise takımın çalışma biçimini, süreçlerini ve iş birliğini inceler. Review "ne ürettik", retrospektif "nasıl çalıştık" sorusuna odaklanır.'
        },
        {
            q: 'Retro kararları nasıl takip edilmeli?',
            a: 'Her aksiyonun bir sahibi ve doğrulanabilir bir tanımı olmalı, bir sonraki retrospektifin açılışında önceki aksiyonların durumu gözden geçirilmelidir. Bazı takımlar en önemli aksiyonu sprint backlog\'una iş olarak ekler.'
        },
        {
            q: 'Takım retroya isteksizse ne yapılmalı?',
            a: 'İsteksizlik çoğu zaman sonuçsuz geçen oturumların birikiminden kaynaklanır. Formatı değiştirmek, oturumu kısaltmak ve özellikle önceki kararların gerçekten hayata geçtiğini görünür kılmak katılımı yeniden canlandırır.'
        }
    ]
}
