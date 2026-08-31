// Blog makalesi: Daily Scrum Nedir, Nasıl Yapılır?
export default {
    slug: 'daily-scrum-nedir-nasil-yapilir',
    title: 'Daily Scrum Nedir, Nasıl Yapılır? Hesap Verme mi, Senkronizasyon mu?',
    metaTitle: 'Daily Scrum Nedir, Nasıl Yapılır? Amacı Nedir? | ScrumTools Blog',
    description: 'Daily scrum nedir, amacı nedir, nasıl yapılır? Hesap verme toplantısına dönüşen daily ile gerçek senkronizasyonun farkını doğru ve yanlış diyaloglarla anlatan rehber.',
    category: 'Scrum Etkinlikleri',
    date: '2026-08-31',
    readingMinutes: 14,
    excerpt: 'Daily Scrum ne bir hesap verme seansı ne de "bak, çalışıyorum" bildirimidir; takımın sprint hedefine birlikte bakıp günün planını uyarladığı 15 dakikalık senkronizasyon noktasıdır. Aynı toplantının yanlış ve doğru hâllerini gerçekçi diyaloglarla inceliyoruz.',
    related: ['scrum-nedir', 'sprint-retrospektifi-nedir', 'burndown-grafigi-nedir'],
    html: `
<p><strong>Daily Scrum</strong> (günlük Scrum), geliştiricilerin her iş günü aynı saatte ve aynı yerde yaptığı, en fazla 15 dakikalık Scrum etkinliğidir. Scrum Kılavuzu'nun (2020) tanımı nettir: Amaç, <em>sprint hedefine doğru ilerlemeyi denetlemek ve gerektiğinde Sprint Backlog'u uyarlamak</em>, yani önümüzdeki iş günü için işlenebilir bir plan üretmektir. Başka bir deyişle Daily Scrum bir durum raporu değil, her gün tekrarlanan mini bir <em>planlama</em> toplantısıdır.</p>

<p>Tanım bu kadar net olmasına rağmen Daily Scrum, sahada en sık yozlaşan Scrum etkinliğidir. Aynı 15 dakika bir takımda sorgu odasına, başka bir takımda içi boş bir yoklamaya, üçüncüsünde ise takımı gerçekten hizalayan güçlü bir senkronizasyon anına dönüşür. Üç hâli de — aynı takımın aynı gününün üç ayrı evreni olarak — diyaloglarla inceleyeceğiz; ama önce sorunun kendisini masaya koyalım.</p>

<h2>Aynı Toplantı, Üç Farklı Zihniyet</h2>

<p>"Daily Scrum aslında nedir?" sorusuna sahada üç farklı cevap verilir ve bir takımın daily'si, hangi cevaba inanıldığına göre şekillenir:</p>

<ul>
<li><strong>"Hesap verme seansıdır":</strong> Toplantının merkezinde bir otorite vardır — Scrum Master, takım lideri veya dinlemeye gelen yönetici. Herkes sırayla dünü <em>ona</em> anlatır; cümleler geçmişi savunmak üzerine kurulur. Bilgi yukarı akar, aşağıya güvensizlik iner.</li>
<li><strong>"Çalıştığını gösterme bildirimidir":</strong> Ortada otorite baskısı yoktur ama toplantı, arka arkaya okunan "dün X yaptım, bugün devam edeceğim, engel yok" monologlarından oluşur. Herkes konuşur, kimse dinlemez; amaç bilgiyi işlemek değil, yoklamaya imza atmaktır.</li>
<li><strong>"Senkronizasyon noktasıdır":</strong> Takım sprint hedefine birlikte bakar, panodaki gerçeği denetler ve günün planını buna göre değiştirir. Konuşma kişiler arasında akar ve toplantıdan bir durum listesi değil, <em>güncellenmiş bir plan</em> çıkar.</li>
</ul>

<p>Cevabı baştan verelim: Daily Scrum ne hesap verme seansıdır ne de çalışkanlık bildirimi; <strong>aynı hedefe bakan takımın günlük yeniden planlama toplantısıdır</strong>. İlk iki yorum, üçüncünün en yaygın iki yozlaşma biçimidir — ve sinsi tarafları şudur: İkisi de dışarıdan "düzenli işleyen bir daily" gibi görünür. Herkes katılır, herkes konuşur, toplantı zamanında biter. Farkı görmek için forma değil, iki soruya bakmak gerekir: <em>Konuşurken kime konuşuluyor?</em> ve <em>toplantıdan çıkan şey ne?</em></p>

<p>Şimdi aynı takımı — Selin (Scrum Master), geliştiriciler Mert, Deniz, Ece ve Burak, Ürün Sahibi Ozan — aynı sprint gününün üç farklı evreninde izleyelim. Sprint hedefi her üç evrende de aynı: <em>"Kurumsal müşteriler faturalarını kendi panellerinden indirebilsin."</em></p>

<h2>1. Sahne — Yanlış: Hesap Verme Seansı</h2>

<p>Saat 09:30. Takım toplantı odasında; birim yöneticisi Levent de "sadece dinlemek için" katılmış. Selin elinde bir listeyle sırayla söz veriyor.</p>

<div class="not-prose my-8 rounded-2xl border border-rose-200 overflow-hidden shadow-sm">
  <div class="px-5 py-3 bg-rose-50 border-b border-rose-200 text-sm font-semibold text-rose-700">✗ Yanlış örnek: sorguya dönüşen daily</div>
  <div class="px-5 py-4 space-y-2.5 text-sm leading-relaxed text-gray-800 bg-white">
    <p><strong class="text-gray-900">Selin:</strong> "Mert, seninle başlayalım. Dün ne yaptın?"</p>
    <p><strong class="text-gray-900">Mert</strong> <em class="text-gray-500">(Selin'e ve Levent'e dönerek)</em><strong class="text-gray-900">:</strong> "Dün… sabah ödeme servisindeki hatayla uğraştım, öğleden sonra da toplantılar vardı. Yani boş durmadım."</p>
    <p><strong class="text-gray-900">Selin:</strong> "Peki fatura listesi ekranı ne durumda? Dün de 'bugün biter' demiştin."</p>
    <p><strong class="text-gray-900">Mert:</strong> "Bitecekti ama test ortamı çöktü, benden kaynaklı değil yani. Bugün kesin biter."</p>
    <p><strong class="text-gray-900">Levent</strong> <em class="text-gray-500">(araya girerek)</em><strong class="text-gray-900">:</strong> "Test ortamı yine mi çöktü? Bu ay üçüncü oluyor. Kim ilgileniyor bununla?"</p>
    <p class="text-gray-500 italic">(Kısa bir sessizlik. Herkes önündeki ekrana bakıyor.)</p>
    <p><strong class="text-gray-900">Selin:</strong> "Ben bir bakarım Levent Bey. Deniz, sen dün ne yaptın?"</p>
    <p><strong class="text-gray-900">Deniz:</strong> "Rapor sayfası üzerinde çalıştım, bugün de devam edeceğim."</p>
    <p class="text-gray-500 italic">(Deniz aslında iki gündür aynı yerde takılı; ama yöneticinin önünde "takıldım" demenin bedelini hesaplayıp vazgeçiyor.)</p>
    <p><strong class="text-gray-900">Selin:</strong> "Süper. Ece?"</p>
    <p><strong class="text-gray-900">Ece:</strong> "Dün kod incelemeleri yaptım, bugün yeni bir işe başlayacağım."</p>
    <p class="text-gray-500 italic">(Hangi işe başlayacağını kendisi de bilmiyor; ama sıra savuşturuldu. Sıradakiler kendi cümlelerini içlerinden prova ettiği için Ece'yi kimse dinlemedi.)</p>
    <p><strong class="text-gray-900">Selin:</strong> "Tamamdır, herkes bir şeylerle ilgileniyor. Hadi işimize."</p>
  </div>
</div>

<h3>Burada ne ters gitti?</h3>

<ul>
<li><strong>Bütün cümleler Selin'e ve Levent'e söylendi.</strong> Takım üyeleri birbirine tek soru sormadı; iletişim tekerlek göbeği gibi merkeze aktı. Oysa Daily Scrum geliştiricilerin <em>birbiriyle</em> senkron olması içindir.</li>
<li><strong>Sorular geçmişe dönüktü ve sorgu formundaydı.</strong> "Dün ne yaptın?", "Dün de biter demiştin" gibi cümleler, cevap verenleri otomatik olarak savunmaya itti: "boş durmadım", "benden kaynaklı değil". Enerji planlamaya değil, mazeret üretmeye harcandı.</li>
<li><strong>Asıl önemli bilgi gizli kaldı.</strong> Deniz'in iki günlük tıkanıklığı — takımın o gün öğrenmesi gereken en kritik gerçek — dile gelmedi, çünkü ortamda "takıldım" demenin algılanan maliyeti yüksekti.</li>
<li><strong>Sprint hedefi bir kez bile anılmadı ve plan değişmedi.</strong> Toplantının tek çıktısı, Selin'in defterindeki durum notlarıydı.</li>
<li><strong>Uzun vadeli hasar daha büyük:</strong> Bu formatta yapılan daily, insanları bir sonraki güne "söyleyecek iyi bir şey" hazırlayarak gelmeye koşullar. Toplantı, işi ilerletme aracı olmaktan çıkıp iyi görünme sahnesine dönüşür.</li>
</ul>

<h2>2. Sahne — Yanlış: "Bak, Çalışıyorum" Bildirimi</h2>

<p>Bu evrende yönetici yok, Selin de baskıcı değil. Takım ayakta, herkes sırayla "durumunu" söylüyor. Toplantı 8 dakikada bitiyor ve takım kısalığıyla gurur duyuyor.</p>

<div class="not-prose my-8 rounded-2xl border border-rose-200 overflow-hidden shadow-sm">
  <div class="px-5 py-3 bg-rose-50 border-b border-rose-200 text-sm font-semibold text-rose-700">✗ Yanlış örnek: yoklamaya dönüşen daily</div>
  <div class="px-5 py-4 space-y-2.5 text-sm leading-relaxed text-gray-800 bg-white">
    <p><strong class="text-gray-900">Mert:</strong> "Dün fatura listesi ekranına baktım, bugün devam edeceğim. Engel yok."</p>
    <p><strong class="text-gray-900">Deniz:</strong> "Dün rapor sayfasıyla uğraştım, bugün de onunla devam. Engel yok."</p>
    <p><strong class="text-gray-900">Ece:</strong> "Dün kod incelemeleri yaptım, bugün servis tarafına bakacağım. Engel yok."</p>
    <p><strong class="text-gray-900">Burak</strong> <em class="text-gray-500">(uzaktan, kamerası kapalı)</em><strong class="text-gray-900">:</strong> "Dün fatura servisinde refactor'a başladım, bugün sürdüreceğim. Bende de engel yok."</p>
    <p><strong class="text-gray-900">Selin:</strong> "Süper, sekiz dakikada bittik. Herkese iyi çalışmalar!"</p>
  </div>
</div>

<p>Kağıt üstünde kusursuz bir daily: Herkes üç soruyu cevapladı, süre kısa tuttu, kimse kimseyi sorgulamadı. Ama aynı günün öğleden sonrasına gidelim: Ece'nin "bakacağım" dediği servis ucu, Burak'ın sabah refactor'da imzasını değiştirdiği servisin ta kendisi çıkıyor — iki geliştirici aynı dosyalarda çakışıyor ve yarım gün birleştirme (merge) çatışmasıyla eriyor. Deniz'in "devam edeceğim" dediği rapor sayfası ise aslında üç gündür "devam ediyor": Grafik kütüphanesi lisans hatası veriyor ve Deniz bunu engel saymıyor, çünkü nihayetinde <em>çalışıyor</em> — sadece ilerlemiyor.</p>

<h3>Burada ne ters gitti?</h3>

<ul>
<li><strong>Ritüel tamamlandı, amaç es geçildi.</strong> "X'e baktım, devam edeceğim" cümlesi bilgi değil, yoklama imzasıdır. Kimsenin planını değiştirmeyen cümleye daily'de yer yoktur.</li>
<li><strong>Kimse kimseyi dinlemedi — çünkü dinlemeyi gerektiren bir şey söylenmedi.</strong> Ece ile Burak'ın çakışması tam da daily'nin yakalamak için var olduğu türden bir sorundu; monolog dizisinin arasından sessizce geçip gitti.</li>
<li><strong>"Engel yok" varsayılan kapanış kalıbına dönüştü.</strong> Üç gündür ilerlemeyen bir iş engel sayılmıyorsa, kelimenin toplantıdaki anlamı kalmamış demektir. İyi bir daily'de soru "engelin var mı?" değil, "bu iş yarın bu saatte hangi durumda olacak?" sorusudur.</li>
<li><strong>Sprint hedefi yine anılmadı.</strong> Sprint'in bitmesine günler kalmışken kimse "hedefe yetişiyor muyuz?" diye sormadı; herkes kendi işinin durum cümlesini okudu.</li>
<li><strong>Kısa sürmesi sağlıklı olduğunu göstermez.</strong> Toplantı hızlıydı çünkü içi boştu. 8 dakikalık boş bir daily, 15 dakikalık dolu bir daily'den ucuz değil, pahalıdır — çünkü "senkronuz" yanılsaması yaratır.</li>
</ul>

<h2>3. Sahne — Doğru: Aynı Hedefe Bakan Takım</h2>

<p>Aynı takım, aynı gün. Bu kez ekranda sprint panosu açık ve konuşma kişilere göre değil, <strong>panodaki işlere göre</strong> sağdan sola — bitmeye en yakın işten geriye doğru — ilerliyor. Sprint hedefi panonun üstünde yazılı.</p>

<div class="not-prose my-8 rounded-2xl border border-emerald-200 overflow-hidden shadow-sm">
  <div class="px-5 py-3 bg-emerald-50 border-b border-emerald-200 text-sm font-semibold text-emerald-800">✓ Doğru örnek: hedefe bakan, planı güncelleyen daily</div>
  <div class="px-5 py-4 space-y-2.5 text-sm leading-relaxed text-gray-800 bg-white">
    <p><strong class="text-gray-900">Selin:</strong> "Sprint'in bitmesine dört iş günü var, hedef ortada: müşteriler faturalarını panelden indirebilecek. Panonun sağından başlayalım — 'Fatura PDF çıktısı' Test'te bekliyor, ne bekliyor?"</p>
    <p><strong class="text-gray-900">Ece:</strong> "Benim onayımı bekliyor, dün akşam yarım kaldı. Sabah ilk iş onu bitirip öğlene Bitti'ye çekerim."</p>
    <p><strong class="text-gray-900">Selin:</strong> "'Fatura listesi ekranı'?"</p>
    <p><strong class="text-gray-900">Mert</strong> <em class="text-gray-500">(takıma dönerek)</em><strong class="text-gray-900">:</strong> "Burada bir riskim var arkadaşlar: PDF servisinde boyut limitine takılıyorum, büyük faturalarda zaman aşımı alıyorum. Bugün tek başıma çözemeyebilirim ve bu iş hedefin tam ortasında."</p>
    <p><strong class="text-gray-900">Burak:</strong> "Dur, ben geçen sprint aynı limite çarpmıştım, çözüm yolunu biliyorum. Refactor'ı öğleden sonraya erteleyeyim, sabah birlikte bakalım mı?"</p>
    <p><strong class="text-gray-900">Mert:</strong> "Harika olur. O zaman öğlene kadar bu iş ikimizde."</p>
    <p><strong class="text-gray-900">Deniz:</strong> "Benden de açık bir durum: Rapor sayfasındaki grafik kütüphanesi lisans hatası veriyor, üç gündür buna takılıp kaldım ve açıkçası bu iş sprint hedefine hizmet etmiyor. Bunu bırakıp fatura tarafındaki test eksiklerini alsam daha mantıklı görünüyor — ne dersiniz?"</p>
    <p><strong class="text-gray-900">Selin:</strong> "Bence de öyle ama iş Sprint Backlog'unda; Ozan, iki dakikan var mı? Hedef için rapor mu, test eksikleri mi?"</p>
    <p><strong class="text-gray-900">Ozan (Ürün Sahibi):</strong> "Hedef fatura. Rapor sayfası bekleyebilir, backlog'a geri alın; lisans konusunu da ben satın almayla konuşurum."</p>
    <p><strong class="text-gray-900">Selin:</strong> "Toparlıyorum: Ece sabah PDF onayını bitiriyor, Mert'le Burak limit sorununa birlikte bakıyor, Deniz test eksiklerini alıyor, rapor sayfası backlog'a dönüyor, lisans takibi Ozan'da. On dört dakika — dağılabiliriz. Limitin teknik detayına girecekler kalsın, after-party yapalım."</p>
  </div>
</div>

<h3>Bu toplantıyı doğru yapan ne?</h3>

<ul>
<li><strong>Konuşma kişiler üzerinden değil, işler ve hedef üzerinden aktı.</strong> "Dün ne yaptın?" hiç sorulmadı; geçmiş, yalnızca bugünün planına gerektiği kadar girdi. Pano sağdan sola yüründüğü için bitmeye en yakın işler — hedefe en çok yaklaştıranlar — önceliği aldı.</li>
<li><strong>Takım üyeleri birbirine konuştu.</strong> Burak, Mert'in riskini duyduğu için kendi planını kendisi değiştirdi. Kimse ona "yardım et" talimatı vermedi — senkronizasyon tam olarak budur.</li>
<li><strong>Toplantının çıktısı bir durum listesi değil, değişmiş bir plandı:</strong> Bir eşleşme kuruldu, bir iş backlog'a iade edildi, iki takip konusu sahiplendi. Dünkü planla bugünkü plan aynı değilse daily işini yapmıştır.</li>
<li><strong>Engel, suçlanma riski olmadan dile geldi.</strong> Deniz üç günlük tıkanıklığı bu evrende söyleyebildi; çünkü soru "neden bitmedi?" değil, "hedefe ne engel oluyor?"du. Aynı bilgi, 1. sahnede korkudan, 2. sahnede ilgisizlikten kaybolmuştu.</li>
<li><strong>Derin tartışma 15 dakikayı işgal etmedi.</strong> Limit sorununun tekniği, yalnızca ilgililerin kaldığı "after-party"ye (16. dakikaya) taşındı.</li>
<li><strong>Ürün Sahibi sorgulamak için değil, takım bir karara ihtiyaç duyduğu için konuştu.</strong> Kapsam kararı geliştiricilerin inisiyatifiyle gündeme geldi, karar sahibine soruldu.</li>
</ul>

<h2>Daily Scrum Nasıl Yapılır: Pratik Kurallar</h2>

<ol>
<li><strong>Zamanı ve yeri sabitleyin.</strong> Her iş günü, aynı saat, aynı yer (veya aynı çağrı bağlantısı). Tekrarlanabilirlik, toplantıyı organize etme maliyetini sıfıra indirir.</li>
<li><strong>15 dakikayı aşmayın.</strong> Zaman kutusu takım büyüklüğünden bağımsızdır. Ayakta yapılması (stand-up) bu disiplini destekleyen bir gelenektir, kural değildir.</li>
<li><strong>Sprint hedefini görünür kılın ve toplantıyı hedefle açın.</strong> "Hedefe şu kadar gün var, neredeyiz?" cümlesi, bütün konuşmanın referans çerçevesini kurar.</li>
<li><strong>Panoyu yürüyün, kişileri değil.</strong> Sağdan sola — bitmeye en yakın işten en yenisine — ilerleyin. Kişi turu "sıra savma" davranışı üretir; iş turu ise konuşmayı doğal olarak hedefe bağlar.</li>
<li><strong>Konuşmayı 24 saat ileriye kurun.</strong> Dünün dökümü değil, bugünün planı. Geçmiş, yalnızca planı etkilediği kadar konuşulur.</li>
<li><strong>Çıkarken planı özetleyin.</strong> Kim neye, kiminle bakıyor; değişen ne? Bir cümlelik özet, herkesin aynı planla dağılmasını garanti eder. Pano da toplantı içinde güncellenmelidir — pano gerçeği yansıtmıyorsa bir sonraki daily kör başlar.</li>
<li><strong>Derin tartışmaları park edin.</strong> İki kişiyi ilgilendiren teknik detay, toplantıdan hemen sonra ilgililerin kaldığı "after-party"de konuşulur. Böylece 15 dakika herkesin ortak paydası olarak korunur.</li>
</ol>

<h2>Klasik Üç Soru Yasak mı?</h2>

<p>"Dün ne yaptım, bugün ne yapacağım, önümde engel var mı?" üçlüsü, Scrum Kılavuzu'nun 2013-2017 sürümlerinde örnek yapı olarak yer alıyordu; <strong>2020 sürümünde bilinçli olarak çıkarıldı</strong>. Sebep soruların yasaklanması değil, şablonun amacın önüne geçmesiydi: Üç soru, 2. sahnedeki gibi boşluk doldurmalı bir yoklama metnine dönüşmeye fazlasıyla elverişlidir.</p>

<p>Bugünkü kural özgürlükçüdür: Geliştiriciler, sprint hedefine ilerlemeye odaklandığı ve ertesi güne işlenebilir bir plan ürettiği sürece istedikleri yapıyı seçebilir. Üç soruyu sevenler için küçük ama etkili bir düzeltme, her soruyu hedefe bağlamaktır: <em>"Sprint hedefine yaklaşmak için dün ne öğrendik? Bugün ne yapacağız? Bizi hedeften ne uzaklaştırıyor?"</em></p>

<h2>Kim Katılır, Kim Konuşur?</h2>

<ul>
<li><strong>Geliştiriciler:</strong> Toplantının sahibi ve tek zorunlu katılımcısıdır. Daily, geliştiricilerin geliştiriciler için yaptığı bir etkinliktir.</li>
<li><strong>Scrum Master:</strong> Toplantının yapılmasını ve 15 dakikada kalmasını güvence altına alır; ama toplantıyı <em>yönetmek</em> zorunda değildir. Olgun bir takımda daily, Scrum Master izindeyken de aynı kalitede döner — dönmüyorsa toplantı takımın değil, Scrum Master'ın demektir.</li>
<li><strong>Ürün Sahibi:</strong> Katılması yararlıdır; 3. sahnedeki gibi hızlı kapsam ve öncelik kararları toplantıyı bekletmeden çözülür. Ancak rolü dinlemek ve soruları cevaplamaktır, sorgulamak değil.</li>
<li><strong>Yöneticiler ve paydaşlar:</strong> En fazla sessiz misafir olabilirler. Varlıkları söylenenleri değiştiriyorsa — 1. sahnedeki Deniz'i hatırlayın — verdikleri zarar, edindikleri bilgiden büyüktür. İlerlemeyi daily'den değil, panodan ve raporlardan izlemeleri daha sağlıklıdır.</li>
</ul>

<h2>Toplantınız Hangisi? Beş Soruluk Turnusol</h2>

<ul>
<li><strong>1. Konuşurken gözler kime dönüyor?</strong> Tek kişiye (Scrum Master'a, yöneticiye) konuşuluyorsa rapor; takıma konuşuluyorsa senkronizasyon.</li>
<li><strong>2. Cümleler nerede bitiyor?</strong> Ağırlık "dün"deyse durum toplantısı; "önümüzdeki 24 saat"teyse planlama toplantısı.</li>
<li><strong>3. Son iki haftada daily, planı en az bir kez değiştirdi mi?</strong> Eşleşme, iş takası, önceliğin öne çekilmesi… Hiçbiri olmadıysa toplantı bilgiyi işlemiyor, sadece topluyor demektir.</li>
<li><strong>4. Scrum Master yokken toplantı yapılıyor mu?</strong> Yapılmıyorsa daily takıma ait değil.</li>
<li><strong>5. "Engel yok" cümlesi kaç gündür üst üste söyleniyor?</strong> Üç gündür ilerlemeyen bir işin yanında "engel yok" duyuyorsanız, engel var — sadece söylenmiyor.</li>
</ul>

<h2>Sık Görülen Anti-Desenler</h2>

<ul>
<li><strong>Zombi daily:</strong> Ritüel eksiksiz, katılım tam, etki sıfır. Herkes sırasını bekler, sırasını savar, telefonuna döner. Çözüm formatı değiştirmektir: pano yürüyüşüne geçmek, açılışı hedefle yapmak, soruyu "yarın bu iş hangi durumda olacak?"a çevirmek.</li>
<li><strong>Sorgu daily'si:</strong> Toplantı bir otoriteye performans kanıtlama seansıdır. Belirtileri: savunma cümleleri, mazeret dili, gizlenen engeller. Çözümün ilk adımı çoğu zaman dinleyici koltuğundaki yöneticiyi toplantıdan çıkarmak, ikincisi soruları geçmişten geleceğe çevirmektir.</li>
<li><strong>Problem çözme daily'si:</strong> İki kişinin mimari tartışması 40 dakika boyunca sekiz kişiyi rehin alır. Tartışma değerlidir — yeri 16. dakikadır, ilgilileriyle.</li>
<li><strong>"Zaten araçta yazıyor" itirazı:</strong> Doğrudur, durum araçta yazar — ve tam da bu yüzden daily'de durum okunmaz. Daily'nin konusu araçta yazmayan şeydir: risk, çakışma, öğrenilen şey ve planın buna göre değişmesi. Araç toplantının yerine değil, hizmetine çalışır.</li>
<li><strong>Kayan saat:</strong> "Bugün 10:00, yarın 11:30 olur mu?" diye her gün yeniden kurulan daily, birkaç haftada erir. Saat sabitse tartışma da bitmiştir.</li>
<li><strong>Tamamen asenkron daily:</strong> Yazılı güncellemeler dağıtık takımlar için değerli bir tamamlayıcıdır; ama daily'nin tamamı yazıya dönerse elde kalan şey tam olarak 2. sahnedir — imzalanmış bir yoklama listesi. Yeniden planlama karşılıklı konuşma ister; en azından haftanın çoğu gününde sesli senkron korunmalıdır.</li>
</ul>

<p>Dağıtık takımlarda bütün bu pratiklerin ön koşulu, herkesin aynı panoya bakabilmesidir. <a href="/">ScrumTools</a>'un sprint panosu, "panoyu birlikte yürüme" akışını uzaktan çalışan takımlar için de mümkün kılar: İşler daily sırasında sürükle-bırak ile güncellenir, <a href="/blog/burndown-grafigi-nedir">burndown grafiği</a> pano durumlarından otomatik üretildiği için "hedefe yetişiyor muyuz?" sorusunun cevabı toplantının açılışında hazırdır.</p>

<h2>Sonuç</h2>

<p>Daily Scrum'ın amacı sorusuna dönersek: Ne hesap verme seansıdır ne "bak, çalışıyorum" bildirimi. <strong>Aynı hedefe bakan takımın, günün gerçeğine göre planını uyarladığı 15 dakikalık senkronizasyon noktasıdır.</strong> Turnusol basittir: Konuşma takımın içinde mi akıyor ve toplantıdan değişmiş bir plan çıkıyor mu?</p>

<p>Günde 15 dakika, beş kişilik bir takım için haftada altı saatten fazla eder — bu maliyeti ödediğiniz toplantının karşılığında durum listesi değil, hizalanmış bir takım almalısınız. Daily'niz 1. veya 2. sahneye benziyorsa çare toplantıyı iptal etmek değil, biçimini değiştirmektir; bunu konuşacağınız en doğru yer de bir sonraki <a href="/blog/sprint-retrospektifi-nedir">sprint retrospektifinizdir</a>.</p>
`,
    faq: [
        {
            q: 'Daily Scrum\'a kimler katılmalı?',
            a: 'Zorunlu katılımcılar geliştiricilerdir; toplantı onlarındır. Scrum Master ve Ürün Sahibi, sprint backlog\'undaki işlerde aktif çalışıyorsa geliştirici olarak katılır; aksi halde katılımları yararlı ama zorunlu değildir. Takım dışı yöneticiler en fazla sessiz misafir olmalıdır.'
        },
        {
            q: 'Daily Scrum neden en fazla 15 dakika?',
            a: 'Zaman kutusu, toplantıyı plana odaklanmaya zorlar: Durum anlatmaya değil, planı değiştiren bilgiye yer vardır. Derinleşmesi gereken konular, toplantıdan hemen sonra yalnızca ilgililerin kaldığı "after-party"de konuşulur. Süre takım büyüklüğünden bağımsız olarak sabittir.'
        },
        {
            q: '"Dün ne yaptım, bugün ne yapacağım, engel var mı?" soruları zorunlu mu?',
            a: 'Hayır. Bu üç soru Scrum Kılavuzu\'nun 2020 sürümünde kaldırıldı; geliştiriciler sprint hedefine ilerlemeye odaklandıkları ve ertesi gün için işlenebilir bir plan ürettikleri sürece istedikleri yapıyı seçebilir. Sorular kullanılacaksa hedefe bağlanmalıdır: "Sprint hedefine yaklaşmak için bugün ne yapacağız?"'
        },
        {
            q: 'Yöneticiler Daily Scrum\'a katılabilir mi?',
            a: 'Katılmaları önerilmez; katılacaklarsa sessiz gözlemci olmalıdırlar. Yönetici varlığı söylenenleri değiştiriyorsa — engeller gizleniyor, cümleler savunmaya dönüyorsa — toplantı hesap verme seansına yozlaşmış demektir. Yöneticiler ilerlemeyi pano ve raporlar üzerinden izlemelidir.'
        },
        {
            q: 'Her şey zaten araçta yazıyorsa daily gereksiz değil mi?',
            a: 'Araçta yazan şey durumdur; daily\'nin konusu ise araçta yazmayan şeylerdir: riskler, çakışmalar, öğrenilenler ve planın bunlara göre değişmesi. İyi bir daily durum okumaz, plan günceller. Toplantı yalnızca araçtaki bilgiyi sesli okumaktan ibaretse sorun daily kavramında değil, o toplantının biçimindedir.'
        },
        {
            q: 'Uzaktan çalışan takımlar daily\'yi nasıl yapmalı?',
            a: 'Sabit saatte, ekranda ortak sprint panosu açıkken görüntülü yapılması en sağlıklısıdır; pano toplantı sırasında güncellenir. Yazılı asenkron güncellemeler tamamlayıcı olarak değerlidir ancak daily tamamen yazıya dönerse yeniden planlama konuşması kaybolur ve etkinlik yoklamaya dönüşür.'
        }
    ]
}
