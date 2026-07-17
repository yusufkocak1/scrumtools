// Blog makalesi: Kanban ile Scrum Arasındaki Farklar
export default {
    slug: 'kanban-ile-scrum-farklari',
    title: 'Kanban ile Scrum Arasındaki Farklar: Hangisi, Ne Zaman?',
    metaTitle: 'Kanban vs Scrum: Farklar ve Seçim Rehberi | ScrumTools Blog',
    description: 'Kanban ile Scrum arasındaki farklar nelerdir? İterasyon, roller, metrikler ve değişim politikası açısından karşılaştırma; hangi takıma hangisi uygun?',
    category: 'Temel Kavramlar',
    date: '2026-07-02',
    readingMinutes: 8,
    excerpt: 'Kanban ve Scrum, çevik dünyanın en yaygın iki yaklaşımıdır; ancak farklı problemlere farklı cevaplar verirler. Bu yazıda iki yaklaşımı yapısal olarak karşılaştırıyor ve seçim için pratik ölçütler sunuyoruz.',
    related: ['scrum-nedir', 'burndown-grafigi-nedir', 'sprint-retrospektifi-nedir'],
    html: `
<p>Çevik çalışmaya geçen takımların en sık sorduğu sorulardan biri şudur: <strong>"Scrum mı uygulamalıyız, Kanban mı?"</strong> Soru meşrudur; çünkü iki yaklaşım da çevik değerleri paylaşır, ancak işleyişleri ve güçlü oldukları bağlamlar belirgin biçimde farklıdır. Doğru seçim, takımın iş akışının doğasına bağlıdır.</p>

<h2>İki Yaklaşımın Özü</h2>

<p><strong>Scrum</strong>, işi sabit uzunluktaki döngülere (sprint) bölen, her döngüde net bir hedefe odaklanan ve döngü sonlarında hem ürünü hem süreci gözden geçiren yinelemeli bir çerçevedir. Ayrıntılı bir tanım için <a href="/blog/scrum-nedir">Scrum nedir?</a> rehberimize bakabilirsiniz.</p>

<p><strong>Kanban</strong> ise kökeni Toyota Üretim Sistemi'ne dayanan bir akış yönetimi yöntemidir. İşi döngülere bölmez; mevcut iş akışını görselleştirir, aynı anda yürüyen iş sayısını (<em>WIP — Work in Progress</em>) sınırlar ve işlerin sistemden geçiş süresini sürekli olarak iyileştirmeyi hedefler. Kanban'ın mottosu "başlamayı bırak, bitirmeye başla" olarak özetlenebilir.</p>

<h2>Yapısal Karşılaştırma</h2>

<table>
<thead>
<tr><th>Boyut</th><th>Scrum</th><th>Kanban</th></tr>
</thead>
<tbody>
<tr><td><strong>Ritim</strong></td><td>Sabit uzunlukta sprint'ler (genellikle 2 hafta)</td><td>Sürekli akış; döngü yoktur</td></tr>
<tr><td><strong>Roller</strong></td><td>Ürün Sahibi, Scrum Master, Geliştiriciler</td><td>Zorunlu rol tanımlamaz; mevcut roller korunur</td></tr>
<tr><td><strong>Değişim politikası</strong></td><td>Sprint kapsamı sprint boyunca korunur; değişiklik sonraki sprint'e girer</td><td>Kapasite elverdiği anda yeni iş alınabilir</td></tr>
<tr><td><strong>Temel kısıt</strong></td><td>Zaman kutusu (sprint süresi)</td><td>WIP limiti (aynı anda yürüyen iş sayısı)</td></tr>
<tr><td><strong>Temel metrikler</strong></td><td>Velocity, sprint burndown</td><td>Çevrim süresi (cycle time), teslim süresi (lead time), akış (throughput)</td></tr>
<tr><td><strong>Teslimat</strong></td><td>Sprint sonunda kullanılabilir Increment</td><td>Her iş bittiğinde tekil teslim</td></tr>
<tr><td><strong>Tahminleme</strong></td><td>Story point ile sprint planlama</td><td>Zorunlu değil; akış verileriyle olasılıksal öngörü yaygın</td></tr>
</tbody>
</table>

<h2>Hangi Takıma Hangisi Uygun?</h2>

<p><strong>Scrum'ın güçlü olduğu bağlamlar:</strong></p>
<ul>
<li>Belirli bir ürün hedefine doğru planlı ilerleyen <strong>ürün geliştirme</strong> takımları,</li>
<li>Paydaşlara düzenli aralıklarla toplu ilerleme göstermenin önemli olduğu ortamlar,</li>
<li>Odak korumakta zorlanan, kesintiye sık uğrayan ve sprint kapsamının sağladığı korumaya ihtiyaç duyan takımlar,</li>
<li>Çevik pratiklere yeni geçen ve net bir yapıya ihtiyaç duyan ekipler.</li>
</ul>

<p><strong>Kanban'ın güçlü olduğu bağlamlar:</strong></p>
<ul>
<li>İşlerin öngörülemeyen zamanlarda geldiği <strong>destek, bakım ve operasyon</strong> ekipleri,</li>
<li>İş kalemlerinin boyutça birbirine yakın ve akışın sürekliliğinin esas olduğu süreçler,</li>
<li>Sprint taahhüdünün yapay kaldığı, önceliklerin gün içinde değişebildiği ortamlar,</li>
<li>Mevcut sürecini kökten değiştirmeden evrimsel olarak iyileştirmek isteyen organizasyonlar.</li>
</ul>

<h2>İkisi Bir Arada: Scrumban</h2>

<p>Seçim her zaman ikili olmak zorunda değildir. <strong>Scrumban</strong>, Scrum'ın etkinlik ritmini (planlama, retro) koruyup Kanban'ın WIP limitlerini ve akış metriklerini süreçlere ekleyen melez bir yaklaşımdır. Pratikte birçok olgun takım, saf bir çerçeve yerine kendi bağlamına uyarlanmış böyle bir karışımı uygular: Örneğin iki haftalık retrospektif ritmini korurken, sprint taahhüdü yerine WIP limitli sürekli akışla çalışmak meşru ve yaygın bir tercihtir.</p>

<p>Önemli olan araçların ve törenlerin kendisi değil, ampirik döngünün korunmasıdır: İş görünür mü, akış ölçülüyor mu, süreç düzenli olarak gözden geçirilip uyarlanıyor mu?</p>

<p>Bu esnekliği çalışma ortamınızın da desteklemesi gerekir. <a href="/">ScrumTools</a>'taki panolar hem sprint'li hem sprint'siz akışlarla kullanılabilir; sütunlarınızı sürece göre düzenleyebilir, swimlane ve filtrelerle akışı görselleştirebilir, dilerseniz aynı organizasyon içinde bir takımı Scrum, diğerini Kanban düzeninde çalıştırabilirsiniz.</p>

<h2>Sonuç</h2>

<p>Scrum ve Kanban rakip değil, farklı problemlere odaklanmış iki komşu yaklaşımdır: Scrum belirsiz ürün geliştirmede ritim ve odak sağlar; Kanban değişken talep altında akışı ve öngörülebilirliği iyileştirir. Sağlıklı bir seçim için önce iş akışınızın doğasına bakın: İşleriniz planlanabilir döngülere oturuyorsa Scrum'la, kesintili ve sürekli bir talep akışıyla çalışıyorsanız Kanban'la başlayın — ve hangisini seçerseniz seçin, süreci düzenli gözden geçirip kendi bağlamınıza uyarlamaktan vazgeçmeyin.</p>
`,
    faq: [
        {
            q: 'Kanban çevik (agile) bir yöntem midir?',
            a: 'Evet. Kanban, Çevik Manifesto\'dan önce üretim dünyasında doğmuş olsa da; işi görünür kılma, akışı sınırlama ve sürekli iyileştirme ilkeleriyle çevik değerlerle tam uyumludur ve çevik yöntemler ailesinin parçası kabul edilir.'
        },
        {
            q: 'Kanban\'da sprint ve tahminleme var mıdır?',
            a: 'Zorunlu değildir. Kanban sürekli akışla çalışır; öngörü ihtiyacı, story point yerine genellikle çevrim süresi ve akış verilerine dayalı olasılıksal analizlerle karşılanır.'
        },
        {
            q: 'WIP limiti nedir ve neden önemlidir?',
            a: 'WIP (Work in Progress) limiti, aynı anda yürütülebilecek iş sayısına konan üst sınırdır. Bağlam değiştirme maliyetini azaltır, darboğazları görünür kılar ve işlerin başlamasını değil bitmesini teşvik eder.'
        },
        {
            q: 'Scrum\'dan Kanban\'a (veya tersine) geçilebilir mi?',
            a: 'Evet; iki yönde de geçiş yaygındır. Kanban evrimsel bir yöntem olduğu için mevcut Scrum süreci üzerine WIP limitleri ekleyerek başlamak, yumuşak bir geçiş yolu sunar. Önemli olan geçişin bir retrospektif kararıyla, bilinçli yapılmasıdır.'
        },
        {
            q: 'Scrumban nedir?',
            a: 'Scrum\'ın etkinlik ritmini (planlama, retrospektif) Kanban\'ın WIP limitleri ve akış metrikleriyle birleştiren melez yaklaşımdır. Sprint taahhüdü yapay kalan ama düzenli gözden geçirme ritmini korumak isteyen takımlarda yaygındır.'
        }
    ]
}
