// Blog makalesi: Scrum Poker Nedir?
// İçerik SEO amaçlı statik HTML olarak tutulur; BlogPost.vue prose sınıflarıyla basar.
export default {
    slug: 'scrum-poker-nedir',
    title: 'Scrum Poker Nedir? Planning Poker ile Efor Tahmini Rehberi',
    metaTitle: 'Scrum Poker (Planning Poker) Nedir? Nasıl Oynanır? | ScrumTools Blog',
    description: 'Scrum Poker (Planning Poker) nedir, nasıl oynanır? Fibonacci kartları, tahmin süreci ve etkili efor tahmini için pratik öneriler bu rehberde.',
    category: 'Tahminleme',
    date: '2026-07-06',
    readingMinutes: 7,
    excerpt: 'Scrum Poker, yazılım takımlarının iş kalemlerinin eforunu birlikte ve tarafsız biçimde tahmin etmesini sağlayan, oyunlaştırılmış bir tahminleme tekniğidir. Bu rehberde tekniğin dayandığı ilkeleri ve adım adım nasıl uygulandığını bulacaksınız.',
    related: ['story-point-nedir', 'scrum-nedir', 'sprint-retrospektifi-nedir'],
    html: `
<p><strong>Scrum Poker</strong> — yaygın diğer adıyla <strong>Planning Poker</strong> — çevik yazılım takımlarının, bir iş kaleminin (kullanıcı hikâyesinin) gerektirdiği eforu birlikte tahmin etmek için kullandığı, fikir birliğine dayalı bir tahminleme tekniğidir. Teknik ilk kez 2002 yılında James Grenning tarafından tanımlanmış, Mike Cohn'un <em>Agile Estimating and Planning</em> kitabıyla geniş kitlelere ulaşmıştır.</p>

<p>Yöntemin özü basittir: Takımın her üyesi, ele alınan iş için tahminini <em>diğerlerinin tahminini görmeden</em> bir kartla belirtir; kartlar aynı anda açılır ve farklılıklar tartışılarak ortak bir tahmine ulaşılır. Bu basit kurgu, grup tahminlemesinde sıkça görülen iki temel sorunu ortadan kaldırır.</p>

<h2>Scrum Poker Neden İşe Yarar?</h2>

<p>Serbest tartışmayla yapılan tahminlerde iki bilişsel tuzak öne çıkar:</p>

<ul>
<li><strong>Çapa etkisi (anchoring):</strong> İlk söylenen rakam, sonraki tüm tahminleri kendine çeker. Kıdemli bir geliştirici "Bu iş en fazla iki gün sürer" dediğinde, farklı düşünenler bile bu rakamın yakınında kalma eğilimi gösterir.</li>
<li><strong>Otoriteye uyum:</strong> Deneyimi veya unvanı yüksek kişilerin görüşü, sesli dile getirilmeyen çekinceleri bastırır; riskler masaya gelmeden karar alınır.</li>
</ul>

<p>Scrum Poker'de herkes tahminini eş zamanlı ve bağımsız olarak açıkladığı için bu etkiler büyük ölçüde devre dışı kalır. Teknik, uzman görüşlerinin bağımsız toplanıp yinelemeli olarak uzlaştırılmasına dayanan <strong>Delphi yöntemi</strong>nin hafifletilmiş bir uyarlamasıdır. Ayrıca tahmin farklılıkları üzerine yapılan kısa tartışmalar, işin gözden kaçan risklerini ve varsayımlarını erken aşamada görünür kılar; çoğu takım için bu tartışmalar, tahminin kendisinden bile değerlidir.</p>

<h2>Kartlar ve Fibonacci Dizisi</h2>

<p>Klasik bir Scrum Poker destesi genellikle şu değerlerden oluşur: <strong>0, 1, 2, 3, 5, 8, 13, 21, 40, 100</strong>. Değerlerin Fibonacci dizisini (veya benzerini) izlemesi bilinçli bir tercihtir: İş büyüdükçe tahminin belirsizliği de büyür. 3 ile 5 arasındaki farkı sezgisel olarak ayırt edebilirsiniz; ancak 20 ile 21 arasındaki farkı kimse güvenilir biçimde savunamaz. Aralıkların büyümesi, sahte bir hassasiyet yanılgısını engeller.</p>

<p>Destede ayrıca özel kartlar bulunur:</p>

<ul>
<li><strong>? (soru işareti):</strong> "Bu işi tahmin edebilecek kadar bilgim yok." Ek analiz veya açıklama ihtiyacını gösterir.</li>
<li><strong>∞ (sonsuz):</strong> "Bu iş tek kalemde tahmin edilemeyecek kadar büyük." İşin bölünmesi gerektiğine işaret eder.</li>
<li><strong>☕ (kahve):</strong> "Mola verelim." Uzayan oturumlarda takımın dinlenme talebini nazikçe iletir.</li>
</ul>

<h2>Scrum Poker Adım Adım Nasıl Oynanır?</h2>

<ol>
<li><strong>İşin sunumu:</strong> Ürün Sahibi (Product Owner), ele alınacak kullanıcı hikâyesini kabul kriterleriyle birlikte kısaca anlatır.</li>
<li><strong>Soru-cevap:</strong> Takım, kapsamı netleştirmek için sorularını sorar. Bu aşama zaman kutusuna bağlanmalıdır; amaç tasarım toplantısı yapmak değil, tahmin için yeterli netliğe ulaşmaktır.</li>
<li><strong>Gizli tahmin:</strong> Her üye, tahminini temsil eden kartı kapalı olarak seçer.</li>
<li><strong>Kartların açılması:</strong> Tüm kartlar aynı anda açılır.</li>
<li><strong>Tartışma:</strong> En düşük ve en yüksek tahmini verenler gerekçelerini açıklar. Çoğu zaman fark, tarafların işi farklı varsayımlarla değerlendirmesinden kaynaklanır.</li>
<li><strong>Yeni tur:</strong> Tartışmanın ardından oylama tekrarlanır. Genellikle iki-üç turda fikir birliğine ulaşılır; ulaşılamıyorsa iş bölünür veya analiz maddesi olarak ayrılır.</li>
</ol>

<h2>Etkili Tahminleme İçin Öneriler</h2>

<ul>
<li><strong>Referans hikâye belirleyin:</strong> Takımın daha önce tamamladığı, herkesin bildiği bir işi (örneğin "profil fotoğrafı yükleme = 3 puan") çapa olarak kullanın. Göreli tahmin, mutlak tahminden her zaman daha tutarlıdır.</li>
<li><strong>Tahmini taahhütle karıştırmayın:</strong> Story point bir efor ölçüsüdür, teslim sözü değildir. Tahminlerin performans değerlendirmesinde kullanılması, takımı savunmacı ve şişirilmiş tahminlere iter.</li>
<li><strong>Tartışmayı zaman kutusuna bağlayın:</strong> Bir iş için ayrılan tartışma süresi dolduğunda karar verin veya işi ayırın; poker oturumunun mimari tartışmaya dönüşmesine izin vermeyin.</li>
<li><strong>Tüm geliştirme ekibini dahil edin:</strong> Testçi, tasarımcı ve geliştirici aynı işe farklı riskler görür; tahminin gücü bu çeşitlilikten gelir.</li>
</ul>

<p>Aynı odada çalışan takımlar fiziksel kartlarla ilerleyebilir; uzaktan veya hibrit çalışan takımlar için ise gerçek zamanlı çevrimiçi araçlar pratik bir çözümdür. Örneğin <a href="/">ScrumTools</a>'un Scrum Poker modülü, kartların eş zamanlı açılması, oy dağılımının anlık görülmesi ve sonuçların ilgili işe kaydedilmesi gibi ihtiyaçları tek ekranda karşılar.</p>

<h2>Sonuç</h2>

<p>Scrum Poker, doğru uygulandığında yalnızca bir tahmin aracı değil, takımın işi ortak biçimde anlamasını sağlayan bir hizalanma pratiğidir. Bağımsız tahmin ilkesine sadık kalır, farklılıkları tartışma fırsatı olarak görür ve tahminleri taahhüde dönüştürme baskısından uzak durursanız, sprint planlamalarınızın hem hızı hem isabeti gözle görülür biçimde artacaktır.</p>
`,
    faq: [
        {
            q: 'Scrum Poker hangi toplantıda oynanır?',
            a: 'Genellikle sprint planlama toplantısında veya backlog düzenleme (refinement) oturumlarında oynanır. Birçok takım, planlamayı hızlandırmak için tahminlemeyi refinement aşamasında tamamlamayı tercih eder.'
        },
        {
            q: 'Story point yerine saat ile tahmin yapılabilir mi?',
            a: 'Teknik saat ile de uygulanabilir; ancak göreli birimler (story point) kişiler arası hız farkından etkilenmediği ve sahte hassasiyet yaratmadığı için çevik takımlarda tercih edilir.'
        },
        {
            q: 'Kartlardaki ? ve ∞ ne anlama gelir?',
            a: '? kartı "tahmin için yeterli bilgim yok", ∞ kartı ise "bu iş tek kalemde tahmin edilemeyecek kadar büyük, bölünmeli" anlamına gelir.'
        },
        {
            q: 'Tahminlerde fikir birliği sağlanamazsa ne yapılmalı?',
            a: 'İki-üç turdan sonra uzlaşma yoksa iş genellikle ya daha küçük parçalara bölünür ya da belirsizliği gidermek için bir araştırma (spike) maddesi oluşturulur. Sonsuz tur dönmek yerine belirsizliğin kaynağını ele almak daha sağlıklıdır.'
        },
        {
            q: 'Scrum Poker kaç kişiyle oynanır?',
            a: 'İşi fiilen yapacak tüm geliştirme ekibinin katılması beklenir; ideal aralık 3-9 kişidir. Ürün Sahibi soruları yanıtlamak için katılır ancak oy kullanmaz.'
        }
    ]
}
