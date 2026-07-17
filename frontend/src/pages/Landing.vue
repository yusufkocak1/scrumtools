<template>
  <div class="min-h-screen bg-white text-gray-900">
    <!-- ─── Üst Menü ─────────────────────────────────────────────────────── -->
    <header class="sticky top-0 z-50 bg-white/90 backdrop-blur border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 lg:px-6 py-3 flex items-center justify-between">
        <a href="#top" class="flex items-center gap-2 lg:gap-3 text-lg lg:text-2xl font-bold text-gray-900">
          <div class="w-8 h-8 lg:w-10 lg:h-10 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 lg:w-6 lg:h-6 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
              <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 18V6h16v12H4z"/>
              <path d="M6 8h2v2H6zm0 3h2v2H6zm3-3h2v2H9zm0 3h2v2H9zm3-3h6v2h-6zm0 3h4v2h-4z"/>
              <circle cx="17" cy="14" r="1.5"/>
              <path d="M6 15l2-2 2 2"/>
            </svg>
          </div>
          <span>ScrumTools</span>
        </a>

        <nav class="hidden md:flex items-center gap-6 text-sm font-medium text-gray-600">
          <a href="#features" class="hover:text-blue-600 transition-colors">Özellikler</a>
          <a href="#how-it-works" class="hover:text-blue-600 transition-colors">Nasıl Çalışır?</a>
          <a href="#pricing" class="hover:text-blue-600 transition-colors">Paketler</a>
          <router-link to="/blog" class="hover:text-blue-600 transition-colors">Blog</router-link>
        </nav>

        <div class="flex items-center gap-2 lg:gap-3">
          <button
              @click="gotoLogin"
              class="px-3 lg:px-4 py-2 text-sm font-medium text-gray-700 hover:text-blue-600 rounded-xl hover:bg-gray-50 transition-all duration-200">
            Giriş Yap
          </button>
          <button
              @click="gotoSignup"
              class="px-3 lg:px-5 py-2 text-sm font-medium bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-md hover:shadow-lg transition-all duration-200">
            Ücretsiz Başla
          </button>
        </div>
      </div>
    </header>

    <!-- ─── Hero ─────────────────────────────────────────────────────────── -->
    <section id="top" class="bg-gradient-to-br from-gray-50 to-gray-100 border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 lg:px-6 py-16 lg:py-24 grid lg:grid-cols-2 gap-12 items-center">
        <div>
          <span class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-100 text-blue-700 text-xs font-semibold mb-6">
            <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10a1 1 0 011.12-.38z" clip-rule="evenodd"/>
            </svg>
            Çevik takımlar için hepsi bir arada platform
          </span>
          <h1 class="text-4xl lg:text-5xl font-bold leading-tight mb-6">
            Takımınızın tüm Scrum süreçleri
            <span class="text-blue-600">tek platformda</span>
          </h1>
          <p class="text-lg text-gray-600 mb-8 leading-relaxed">
            Sprint planlamadan retrospektife, görev takibinden dokümantasyona —
            ScrumTools, çevik ekiplerin ihtiyaç duyduğu tüm araçları tek çatı altında toplar.
            Kurulum yok, karmaşa yok; kaydolun ve takımınızla çalışmaya başlayın.
          </p>
          <div class="flex flex-col sm:flex-row gap-3 mb-4">
            <button
                @click="gotoSignup"
                class="px-8 py-3.5 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-xl shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-200">
              Ücretsiz Başla
            </button>
            <button
                @click="gotoLogin"
                class="px-8 py-3.5 bg-white hover:bg-gray-50 text-gray-700 font-medium rounded-xl border border-gray-300 shadow-sm transition-all duration-200">
              Giriş Yap
            </button>
          </div>
          <p class="text-sm text-gray-500">
            Kredi kartı gerekmez &middot; 14 gün ücretsiz MAX deneme
          </p>
        </div>

        <!-- Mini board illüstrasyonu (CSS ile, ekran görüntüsü gerektirmez) -->
        <div class="hidden lg:block">
          <div class="bg-white rounded-2xl shadow-xl border border-gray-200 p-5">
            <div class="flex items-center gap-1.5 mb-4">
              <span class="w-3 h-3 rounded-full bg-red-400"></span>
              <span class="w-3 h-3 rounded-full bg-yellow-400"></span>
              <span class="w-3 h-3 rounded-full bg-green-400"></span>
              <span class="ml-3 text-xs text-gray-400 font-medium">Sprint 12 &middot; Board</span>
            </div>
            <div class="grid grid-cols-3 gap-3">
              <div class="bg-gray-50 rounded-xl p-3">
                <p class="text-xs font-semibold text-gray-500 mb-2">Yapılacak</p>
                <div class="bg-white rounded-lg border border-gray-200 p-2.5 mb-2 shadow-sm">
                  <div class="h-2 bg-gray-200 rounded w-4/5 mb-2"></div>
                  <span class="inline-block px-1.5 py-0.5 rounded bg-blue-100 text-blue-700 text-[10px] font-semibold">5 SP</span>
                </div>
                <div class="bg-white rounded-lg border border-gray-200 p-2.5 shadow-sm">
                  <div class="h-2 bg-gray-200 rounded w-3/5 mb-2"></div>
                  <span class="inline-block px-1.5 py-0.5 rounded bg-purple-100 text-purple-700 text-[10px] font-semibold">3 SP</span>
                </div>
              </div>
              <div class="bg-gray-50 rounded-xl p-3">
                <p class="text-xs font-semibold text-gray-500 mb-2">Devam Ediyor</p>
                <div class="bg-white rounded-lg border-2 border-blue-200 p-2.5 shadow-sm">
                  <div class="h-2 bg-gray-200 rounded w-full mb-2"></div>
                  <div class="flex items-center justify-between">
                    <span class="inline-block px-1.5 py-0.5 rounded bg-orange-100 text-orange-700 text-[10px] font-semibold">8 SP</span>
                    <span class="w-5 h-5 rounded-full bg-purple-100 text-purple-600 text-[10px] font-bold flex items-center justify-center">YK</span>
                  </div>
                </div>
              </div>
              <div class="bg-gray-50 rounded-xl p-3">
                <p class="text-xs font-semibold text-gray-500 mb-2">Tamamlandı</p>
                <div class="bg-white rounded-lg border border-gray-200 p-2.5 mb-2 shadow-sm opacity-75">
                  <div class="h-2 bg-gray-200 rounded w-2/3 mb-2"></div>
                  <span class="inline-block px-1.5 py-0.5 rounded bg-green-100 text-green-700 text-[10px] font-semibold">✓ Bitti</span>
                </div>
                <div class="bg-white rounded-lg border border-gray-200 p-2.5 shadow-sm opacity-75">
                  <div class="h-2 bg-gray-200 rounded w-1/2 mb-2"></div>
                  <span class="inline-block px-1.5 py-0.5 rounded bg-green-100 text-green-700 text-[10px] font-semibold">✓ Bitti</span>
                </div>
              </div>
            </div>
            <!-- Poker kartları şeridi -->
            <div class="mt-4 flex items-center gap-2">
              <span class="text-xs text-gray-400 font-medium mr-1">Scrum Poker:</span>
              <span v-for="card in ['1','2','3','5','8']" :key="card"
                    :class="['w-8 h-11 rounded-lg border-2 flex items-center justify-center text-sm font-bold shadow-sm',
                             card === '5' ? 'bg-blue-600 border-blue-600 text-white -translate-y-1' : 'bg-white border-gray-200 text-gray-600']">
                {{ card }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ─── Özellikler ───────────────────────────────────────────────────── -->
    <section id="features" class="max-w-7xl mx-auto px-4 lg:px-6 py-16 lg:py-24">
      <div class="text-center max-w-2xl mx-auto mb-14">
        <h2 class="text-3xl lg:text-4xl font-bold mb-4">İhtiyacınız olan her araç, tek yerde</h2>
        <p class="text-gray-600 text-lg">
          Farklı araçlar arasında gidip gelmeyi bırakın. ScrumTools, çevik sürecinizin
          her adımını kapsayan modülleriyle takımınızı tek platformda buluşturur.
        </p>
      </div>

      <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div v-for="feature in features" :key="feature.title"
             class="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm hover:shadow-lg hover:-translate-y-1 transition-all duration-200">
          <div :class="['w-12 h-12 rounded-xl flex items-center justify-center mb-4', feature.bg]">
            <svg :class="['w-6 h-6', feature.color]" fill="currentColor" viewBox="0 0 20 20" v-html="feature.icon"></svg>
          </div>
          <h3 class="font-semibold text-lg mb-2">{{ feature.title }}</h3>
          <p class="text-sm text-gray-600 leading-relaxed">{{ feature.description }}</p>
        </div>
      </div>
    </section>

    <!-- ─── Nasıl Çalışır ────────────────────────────────────────────────── -->
    <section id="how-it-works" class="bg-gray-50 border-y border-gray-200">
      <div class="max-w-7xl mx-auto px-4 lg:px-6 py-16 lg:py-24">
        <div class="text-center max-w-2xl mx-auto mb-14">
          <h2 class="text-3xl lg:text-4xl font-bold mb-4">Dakikalar içinde başlayın</h2>
          <p class="text-gray-600 text-lg">Kurulum, sunucu ya da teknik bilgi gerekmez.</p>
        </div>

        <div class="grid md:grid-cols-3 gap-8">
          <div v-for="(step, i) in steps" :key="step.title" class="relative text-center">
            <div class="w-14 h-14 mx-auto bg-blue-600 text-white rounded-2xl flex items-center justify-center text-xl font-bold shadow-lg mb-5">
              {{ i + 1 }}
            </div>
            <h3 class="font-semibold text-lg mb-2">{{ step.title }}</h3>
            <p class="text-sm text-gray-600 leading-relaxed max-w-xs mx-auto">{{ step.description }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ─── Paketler ─────────────────────────────────────────────────────── -->
    <section id="pricing" class="max-w-7xl mx-auto px-4 lg:px-6 py-16 lg:py-24">
      <div class="text-center max-w-2xl mx-auto mb-14">
        <h2 class="text-3xl lg:text-4xl font-bold mb-4">Her takıma uygun bir paket</h2>
        <p class="text-gray-600 text-lg">
          Ücretsiz başlayın, takımınız büyüdükçe yükseltin.
          Yeni organizasyonlar 14 gün boyunca MAX paketi ücretsiz dener.
        </p>
      </div>

      <div class="grid md:grid-cols-3 gap-6 max-w-5xl mx-auto">
        <div v-for="plan in plans" :key="plan.name"
             :class="['rounded-2xl border p-8 flex flex-col transition-all duration-200',
                      plan.highlight
                        ? 'border-blue-600 bg-blue-600 text-white shadow-xl md:scale-105'
                        : 'border-gray-200 bg-white shadow-sm hover:shadow-lg']">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-xl font-bold">{{ plan.name }}</h3>
            <span v-if="plan.badge"
                  :class="['text-[11px] font-semibold px-2.5 py-1 rounded-full',
                           plan.highlight ? 'bg-white text-blue-700' : 'bg-blue-100 text-blue-700']">
              {{ plan.badge }}
            </span>
          </div>
          <p :class="['text-sm mb-6', plan.highlight ? 'text-blue-100' : 'text-gray-500']">{{ plan.tagline }}</p>
          <ul class="space-y-3 mb-8 flex-1">
            <li v-for="item in plan.items" :key="item" class="flex items-start gap-2.5 text-sm">
              <svg :class="['w-5 h-5 flex-shrink-0 mt-0.5', plan.highlight ? 'text-blue-200' : 'text-green-500']"
                   fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
              </svg>
              <span :class="plan.highlight ? 'text-blue-50' : 'text-gray-700'">{{ item }}</span>
            </li>
          </ul>
          <button
              @click="gotoSignup"
              :class="['w-full py-3 rounded-xl font-medium transition-all duration-200',
                       plan.highlight
                         ? 'bg-white text-blue-700 hover:bg-blue-50 shadow-md'
                         : 'bg-blue-600 text-white hover:bg-blue-700 shadow-md hover:shadow-lg']">
            {{ plan.cta }}
          </button>
        </div>
      </div>

      <p class="text-center text-sm text-gray-500 mt-8">
        Güncel fiyatlar ve limit detayları için giriş yaptıktan sonra organizasyon panelindeki
        <span class="font-medium">Paketler</span> sekmesine göz atabilirsiniz.
      </p>
    </section>

    <!-- ─── Alt CTA ──────────────────────────────────────────────────────── -->
    <section class="bg-gradient-to-br from-blue-600 to-indigo-700">
      <div class="max-w-4xl mx-auto px-4 lg:px-6 py-16 text-center text-white">
        <h2 class="text-3xl lg:text-4xl font-bold mb-4">Takımınızı bugün ScrumTools'a taşıyın</h2>
        <p class="text-blue-100 text-lg mb-8">
          Ücretsiz hesabınızı oluşturun; ilk sprintinizi bugün planlayın,
          ilk retronuzu bu hafta yapın.
        </p>
        <button
            @click="gotoSignup"
            class="px-10 py-4 bg-white text-blue-700 font-semibold rounded-xl shadow-lg hover:shadow-xl hover:bg-blue-50 transform hover:scale-105 transition-all duration-200">
          Ücretsiz Başla
        </button>
      </div>
    </section>

    <!-- ─── Footer ───────────────────────────────────────────────────────── -->
    <footer class="bg-gray-900 text-gray-400">
      <div class="max-w-7xl mx-auto px-4 lg:px-6 py-10 flex flex-col md:flex-row items-center justify-between gap-4">
        <div class="flex items-center gap-2 text-white font-bold text-lg">
          <div class="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-white" fill="currentColor" viewBox="0 0 24 24">
              <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 18V6h16v12H4z"/>
              <path d="M6 8h2v2H6zm0 3h2v2H6zm3-3h2v2H9zm0 3h2v2H9zm3-3h6v2h-6zm0 3h4v2h-4z"/>
            </svg>
          </div>
          ScrumTools
        </div>
        <div class="flex items-center gap-6 text-sm">
          <a href="#features" class="hover:text-white transition-colors">Özellikler</a>
          <a href="#pricing" class="hover:text-white transition-colors">Paketler</a>
          <router-link to="/blog" class="hover:text-white transition-colors">Blog</router-link>
          <button @click="gotoLogin" class="hover:text-white transition-colors">Giriş Yap</button>
        </div>
        <p class="text-xs">
          &copy; {{ new Date().getFullYear() }} ScrumTools &middot; v{{ appVersion }}
        </p>
      </div>
    </footer>
  </div>
</template>

<script>
import { setSeo, resetSeo, SITE_URL, SITE_NAME } from '../utils/seo.js'

export default {
  name: 'Landing',
  data: () => ({
    appVersion: __APP_VERSION__,
    // İkonlar: Heroicons (20x20, solid) path'leri — v-html ile basılır (statik içerik)
    features: [
      {
        title: 'Kanban Board',
        description: 'Sürükle-bırak board, swimlane\'ler, özel alanlar ve güçlü filtrelerle görevlerinizi görselleştirin.',
        bg: 'bg-blue-100', color: 'text-blue-600',
        icon: '<path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zM5 11a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zM11 5a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zM11 13a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>'
      },
      {
        title: 'Sprint & Backlog',
        description: 'Backlog\'unuzu önceliklendirin, sprint\'lerinizi planlayın, release\'lerinizi tek ekrandan yönetin.',
        bg: 'bg-indigo-100', color: 'text-indigo-600',
        icon: '<path fill-rule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm0 6a1 1 0 011-1h8a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1v-2zm0 6a1 1 0 011-1h4a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1v-2z" clip-rule="evenodd"/>'
      },
      {
        title: 'Scrum Poker',
        description: 'Gerçek zamanlı planlama pokeri ile tüm takım eforu birlikte tahminler, tartışır ve uzlaşır.',
        bg: 'bg-purple-100', color: 'text-purple-600',
        icon: '<path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm2 10a2 2 0 104 0 2 2 0 00-4 0zm2-7a1 1 0 011 1v1a1 1 0 11-2 0V6a1 1 0 011-1z" clip-rule="evenodd"/>'
      },
      {
        title: 'Retro Panoları',
        description: 'Tartışma zamanlayıcısı, oylama ve karar panosuyla verimli, aksiyon odaklı retrospektifler yapın.',
        bg: 'bg-rose-100', color: 'text-rose-600',
        icon: '<path fill-rule="evenodd" d="M18 13V5a2 2 0 00-2-2H4a2 2 0 00-2 2v8a2 2 0 002 2h3l3 3 3-3h3a2 2 0 002-2zM5 7a1 1 0 011-1h8a1 1 0 110 2H6a1 1 0 01-1-1zm1 3a1 1 0 100 2h3a1 1 0 100-2H6z" clip-rule="evenodd"/>'
      },
      {
        title: 'Dashboard & Raporlar',
        description: 'Burndown, velocity ve iş yükü grafikleriyle sprint sağlığınızı anlık takip edin.',
        bg: 'bg-emerald-100', color: 'text-emerald-600',
        icon: '<path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"/>'
      },
      {
        title: 'Dokümanlar',
        description: 'Zengin metin editörlü doküman alanlarıyla takım bilgi birikiminizi projenizin yanında tutun.',
        bg: 'bg-amber-100', color: 'text-amber-600',
        icon: '<path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd"/>'
      },
      {
        title: 'Takım Quizleri',
        description: 'Eğlenceli bilgi yarışmalarıyla takım etkinliklerinize renk katın, skor tablosunda yarışın.',
        bg: 'bg-teal-100', color: 'text-teal-600',
        icon: '<path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zM8.94 6.94a.75.75 0 11-1.061-1.061 3 3 0 112.871 5.026v.345a.75.75 0 01-1.5 0v-.5c0-.72.57-1.172 1.081-1.287A1.5 1.5 0 108.94 6.94zM10 15a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd"/>'
      },
      {
        title: 'Kod Paylaşımı',
        description: 'Monaco editör tabanlı gerçek zamanlı kod paylaşımıyla pair programming ve teknik tartışmalar.',
        bg: 'bg-cyan-100', color: 'text-cyan-600',
        icon: '<path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd"/>'
      },
    ],
    steps: [
      {
        title: 'Hesabınızı oluşturun',
        description: 'E-posta adresinizle saniyeler içinde ücretsiz kaydolun. Kredi kartı bilgisi istemiyoruz.'
      },
      {
        title: 'Organizasyonunuzu kurun',
        description: 'Organizasyonunuzu, projelerinizi ve takımlarınızı oluşturun; ekip arkadaşlarınızı davet edin.'
      },
      {
        title: 'Sprintinize başlayın',
        description: 'Board\'unuzu açın, poker ile tahminleyin, retro ile her sprint biraz daha gelişin.'
      },
    ],
    plans: [
      {
        name: 'FREE',
        tagline: 'Scrum\'a yeni başlayan küçük takımlar için.',
        badge: null,
        highlight: false,
        cta: 'Ücretsiz Başla',
        items: [
          'Temel modüller: board, poker, retro',
          'Küçük takımlar için yeterli limitler',
          'Süresiz ücretsiz',
        ],
      },
      {
        name: 'PRO',
        tagline: 'Büyüyen takımlar ve birden çok proje için.',
        badge: 'Popüler',
        highlight: false,
        cta: 'PRO ile Başla',
        items: [
          'Daha yüksek üye ve proje limitleri',
          'Gelişmiş raporlama ekranları',
          'Aylık veya yıllık ödeme seçeneği',
        ],
      },
      {
        name: 'MAX',
        tagline: 'Tüm modüller, en yüksek limitler.',
        badge: '14 gün ücretsiz',
        highlight: true,
        cta: 'MAX\'ı Ücretsiz Dene',
        items: [
          'Tüm modüller ve en yüksek limitler',
          'Yeni organizasyonlara 14 gün ücretsiz deneme',
          'Öncelikli destek',
        ],
      },
    ],
  }),
  methods: {
    gotoLogin() {
      this.$router.push('/login')
    },
    gotoSignup() {
      this.$router.push({ path: '/login', query: { mode: 'signup' } })
    },
  },
  created() {
    setSeo({
      title: 'ScrumTools — Çevik Takımlar için Scrum Poker, Retro ve Sprint Yönetimi',
      description: 'Sprint panosu, planlama pokeri, retro panoları, backlog ve raporlar tek platformda. Çevik takımınız için kurulum gerektirmeyen Scrum araçları — ücretsiz başlayın.',
      path: '/',
      jsonLd: [{
        '@context': 'https://schema.org',
        '@type': 'SoftwareApplication',
        name: SITE_NAME,
        url: SITE_URL,
        applicationCategory: 'BusinessApplication',
        operatingSystem: 'Web',
        inLanguage: 'tr',
        offers: { '@type': 'Offer', price: '0', priceCurrency: 'TRY', description: 'Ücretsiz başlangıç paketi' }
      }]
    })
  },
  unmounted() {
    resetSeo()
  },
}
</script>
