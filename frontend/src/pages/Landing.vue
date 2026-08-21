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
          <a href="#highlights" class="hover:text-blue-600 transition-colors">Öne Çıkanlar</a>
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
            Board, doküman, ortak çalışma ve CI/CD — tek platformda
          </span>
          <h1 class="text-4xl lg:text-5xl font-bold leading-tight mb-6">
            Takımınızın tüm işi
            <span class="text-blue-600">tek platformda</span>
          </h1>
          <p class="text-lg text-gray-600 mb-8 leading-relaxed">
            Sprint planlamadan retrospektife, görev takibinden dokümantasyona; eş zamanlı
            ortak çalışma alanından Git ve Jenkins entegrasyonuna kadar — ScrumTools,
            çevik ekiplerin ihtiyaç duyduğu tüm araçları tek çatı altında toplar.
            Kurulum yok, karmaşa yok; kaydolun ve takımınızla çalışmaya başlayın.
          </p>
          <div class="flex flex-col sm:flex-row gap-3 mb-4">
            <button
                @click="gotoSignup"
                class="px-8 py-3.5 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-xl shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-200">
              Ücretsiz Başla
            </button>
            <a
                href="#features"
                class="px-8 py-3.5 bg-white hover:bg-gray-50 text-gray-700 font-medium rounded-xl border border-gray-300 shadow-sm transition-all duration-200 text-center">
              Özellikleri Gör
            </a>
          </div>
          <p class="text-sm text-gray-500">
            Kredi kartı gerekmez<template v-if="trialPlan">
              &middot; {{ trialPlan.trialDays }} gün ücretsiz
              {{ (trialPlan.name || trialPlan.code).toUpperCase() }} deneme</template>
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
              <!-- Eş zamanlı çalışan takım arkadaşları (presence) -->
              <span class="ml-auto flex items-center -space-x-1.5">
                <span class="w-5 h-5 rounded-full bg-purple-500 text-white text-[9px] font-bold flex items-center justify-center ring-2 ring-white">YK</span>
                <span class="w-5 h-5 rounded-full bg-emerald-500 text-white text-[9px] font-bold flex items-center justify-center ring-2 ring-white">AD</span>
                <span class="w-5 h-5 rounded-full bg-orange-500 text-white text-[9px] font-bold flex items-center justify-center ring-2 ring-white">MS</span>
              </span>
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
                  <!-- Task'a bağlı branch ve build durumu -->
                  <div class="mt-2 flex items-center gap-1">
                    <span class="inline-flex items-center px-1.5 py-0.5 rounded bg-gray-100 text-gray-600 text-[9px] font-medium">
                      feature/SCRM-42
                    </span>
                    <span class="inline-flex items-center px-1.5 py-0.5 rounded bg-green-100 text-green-700 text-[9px] font-semibold">
                      ✓ build
                    </span>
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
            <!-- STQL sorgu çubuğu -->
            <div class="mt-4 flex items-center gap-2 bg-gray-900 rounded-lg px-3 py-2">
              <span class="text-[10px] font-bold text-gray-500 tracking-wider">STQL</span>
              <code class="text-[11px] text-emerald-300 font-mono truncate">
                sprint = currentSprint() AND assignee = currentUser()
              </code>
            </div>
            <!-- Poker kartları şeridi -->
            <div class="mt-3 flex items-center gap-2">
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

    <!-- ─── Öne Çıkanlar ─────────────────────────────────────────────────── -->
    <section id="highlights" class="bg-gray-900 text-white">
      <div class="max-w-7xl mx-auto px-4 lg:px-6 py-16 lg:py-24">
        <div class="text-center max-w-2xl mx-auto mb-14">
          <h2 class="text-3xl lg:text-4xl font-bold mb-4">Sıradan bir board'dan çok daha fazlası</h2>
          <p class="text-gray-400 text-lg">
            Eş zamanlı ortak çalışma alanı, akıllı filtreler, Git ile CI/CD entegrasyonu ve
            güçlü bir sorgu dili — ScrumTools'u ayıran dört yetenek.
          </p>
        </div>

        <div class="grid md:grid-cols-2 gap-6">
          <div v-for="item in highlights" :key="item.title"
               class="bg-white/5 border border-white/10 rounded-2xl p-7 hover:bg-white/10 transition-colors duration-200">
            <div class="flex items-center gap-3 mb-3">
              <div :class="['w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0', item.bg]">
                <svg class="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 20 20" v-html="item.icon"></svg>
              </div>
              <h3 class="font-semibold text-xl">{{ item.title }}</h3>
            </div>
            <p class="text-sm text-gray-400 leading-relaxed mb-5">{{ item.description }}</p>
            <ul class="space-y-2" :class="item.code ? 'mb-4' : ''">
              <li v-for="point in item.points" :key="point" class="flex items-start gap-2.5 text-sm text-gray-300">
                <svg class="w-4 h-4 flex-shrink-0 mt-0.5 text-blue-400" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
                <span>{{ point }}</span>
              </li>
            </ul>
            <code v-if="item.code" class="block bg-black/40 rounded-lg px-3 py-2 text-[12px] text-emerald-300 font-mono overflow-x-auto whitespace-nowrap">
              {{ item.code }}
            </code>
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

      <!-- Kart açmaya değmeyen ama günlük kullanımda fark yaratan yetenekler -->
      <div class="mt-12">
        <p class="text-center text-sm font-semibold text-gray-500 mb-5">Ve dahası</p>
        <div class="flex flex-wrap justify-center gap-2.5">
          <span v-for="extra in extras" :key="extra"
                class="px-3.5 py-1.5 rounded-full bg-gray-100 text-gray-700 text-sm font-medium">
            {{ extra }}
          </span>
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
          <template v-if="trialPlan">
            Yeni organizasyonlar {{ trialPlan.trialDays }} gün boyunca
            {{ (trialPlan.name || trialPlan.code).toUpperCase() }} paketi ücretsiz dener.
          </template>
        </p>
      </div>

      <!-- Paketler API'den gelene kadar iskelet; yanlış fiyat göstermemek için
           kartlar yüklenmeden basılmaz -->
      <div v-if="plansLoading" class="grid sm:grid-cols-2 xl:grid-cols-4 gap-6 max-w-7xl mx-auto">
        <div v-for="n in 4" :key="n"
             class="rounded-2xl border border-gray-200 bg-white p-8 shadow-sm animate-pulse">
          <div class="h-5 bg-gray-200 rounded w-1/3 mb-4"></div>
          <div class="h-3 bg-gray-100 rounded w-4/5 mb-6"></div>
          <div class="h-8 bg-gray-200 rounded w-2/5 mb-8"></div>
          <div class="space-y-3 mb-8">
            <div v-for="i in 4" :key="i" class="h-3 bg-gray-100 rounded"></div>
          </div>
          <div class="h-11 bg-gray-200 rounded-xl"></div>
        </div>
      </div>

      <!-- Paketler alınamazsa uydurma kart basmak yerine dürüst bir not -->
      <div v-else-if="!planCards.length"
           class="max-w-2xl mx-auto text-center bg-gray-50 border border-gray-200 rounded-2xl p-8">
        <p class="text-gray-600 mb-6">
          Paket bilgileri şu anda yüklenemedi. Ücretsiz hesabınızı oluşturup organizasyon
          panelindeki <span class="font-medium">Paketler</span> sekmesinden güncel paketleri
          görebilirsiniz.
        </p>
        <button
            @click="gotoSignup"
            class="px-8 py-3 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-xl shadow-md hover:shadow-lg transition-all duration-200">
          Ücretsiz Başla
        </button>
      </div>

      <div v-else :class="planGridClass">
        <div v-for="plan in planCards" :key="plan.name"
             :class="['rounded-2xl border p-8 flex flex-col transition-all duration-200',
                      plan.highlight
                        ? 'border-blue-600 bg-blue-600 text-white shadow-xl xl:scale-105'
                        : 'border-gray-200 bg-white shadow-sm hover:shadow-lg']">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-xl font-bold uppercase">{{ plan.name }}</h3>
            <span v-if="plan.badge"
                  :class="['text-[11px] font-semibold px-2.5 py-1 rounded-full',
                           plan.highlight ? 'bg-white text-blue-700' : 'bg-blue-100 text-blue-700']">
              {{ plan.badge }}
            </span>
          </div>
          <p :class="['text-sm mb-5', plan.highlight ? 'text-blue-100' : 'text-gray-500']">{{ plan.tagline }}</p>

          <div v-if="plan.price" class="mb-6">
            <div class="flex items-baseline gap-1.5">
              <span class="text-3xl font-bold">{{ plan.price }}</span>
              <span :class="['text-sm', plan.highlight ? 'text-blue-100' : 'text-gray-500']">{{ plan.period }}</span>
            </div>
            <p v-if="plan.priceNote"
               :class="['text-xs mt-1', plan.highlight ? 'text-blue-100' : 'text-gray-500']">{{ plan.priceNote }}</p>
          </div>

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
        Paket ayrıntılarının tamamını ve ödeme seçeneklerini, giriş yaptıktan sonra
        organizasyon panelindeki <span class="font-medium">Paketler</span> sekmesinde
        görebilirsiniz.
      </p>
    </section>

    <!-- ─── Alt CTA ──────────────────────────────────────────────────────── -->
    <section class="bg-gradient-to-br from-blue-600 to-indigo-700">
      <div class="max-w-4xl mx-auto px-4 lg:px-6 py-16 text-center text-white">
        <h2 class="text-3xl lg:text-4xl font-bold mb-4">Takımınızı bugün ScrumTools'a taşıyın</h2>
        <p class="text-blue-100 text-lg mb-8">
          Ücretsiz hesabınızı oluşturun; ilk sprintinizi bugün planlayın,
          ilk dokümanınızı takımınızla birlikte yazın, ilk retronuzu bu hafta yapın.
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
          <a href="#highlights" class="hover:text-white transition-colors">Öne Çıkanlar</a>
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
import PublicApi from '../api/PublicApi.js'
import { FEATURE_ORDER, featureLabel } from '../utils/planFeatures.js'

export default {
  name: 'Landing',
  data: () => ({
    appVersion: __APP_VERSION__,
    // İkonlar: Heroicons (20x20, solid) path'leri — v-html ile basılır (statik içerik)

    // Tanıtımda ayrı bir bölümle öne çıkarılan dört amiral modül.
    // Kaynak planlar: COLLAB_WORKSPACE_PLAN.md, RICH_FILTER_PLAN.md,
    // JENKINS_INTEGRATION_PLAN.md, TASK_QUERY_LANGUAGE.md
    highlights: [
      {
        title: 'Ortak Çalışma Alanı',
        description: 'Metin, kod ve hesap tablosu dokümanlarını takımınızla aynı anda düzenleyin. ' +
            'Karakter düzeyinde birleşme, kimin nerede yazdığını gösteren canlı imleçler ve sürüm geçmişi.',
        bg: 'bg-violet-500',
        points: [
          'Zengin metin, Monaco kod editörü ve hesap tablosu — tek editör altyapısı',
          'Canlı imleç ve varlık göstergesi ile çakışmasız eş zamanlı düzenleme',
          'Formül motoru, Excel içe/dışa aktarma ve JavaScript makroları',
          'Docs ile çift yönlü entegrasyon: sayfaya canlı tablo gömme',
        ],
        icon: '<path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"/>',
      },
      {
        title: 'Zengin Filtreler & Etkileşimli Dashboard',
        description: 'Bir temel sorgunun üstüne adlandırılmış, renkli akıllı filtreler kurun; ' +
            'aynı filtreye bağlı tüm widget\'lar ortak bir seçim durumunu paylaşsın.',
        bg: 'bg-emerald-500',
        points: [
          'Dokuz widget tipi: sayaç, çoklu ölçü, liste, grafik, kuyruk, oran, zaman serisi, ısı haritası',
          'Bir grafik diliminde tıklayın; sayfadaki bütün widget\'lar o dilime daralsın',
          'Grafikten göreve drill-down, CSV/PNG dışa aktarma, oran eşiği uyarıları',
          'Takım altında çoklu pano ve sürükle-bırak yerleşim',
        ],
        icon: '<path fill-rule="evenodd" d="M3 3a1 1 0 011-1h12a1 1 0 011 1v3a1 1 0 01-.293.707L12 11.414V15a1 1 0 01-.293.707l-2 2A1 1 0 018 17v-5.586L3.293 6.707A1 1 0 013 6V3z" clip-rule="evenodd"/>',
      },
      {
        title: 'Git & CI/CD Entegrasyonu',
        description: 'Task ile kod arasındaki bağı kurun: görev detayından branch açın, pull request oluşturun, ' +
            'Jenkins pipeline\'ını tetikleyin ve build durumunu uygulama içinden izleyin.',
        bg: 'bg-sky-500',
        points: [
          'Organizasyon seviyesinde bağlantı, proje seviyesinde repo ve job eşlemesi',
          'Görev detayında geliştirme paneli: branch oluşturma, PR açma, commit takibi',
          'Tek tıkla test ortamına deploy ve sürüm yaşam döngüsüne bağlı release pipeline\'ı',
          'Task ve release üzerinde build tarihçesi, sonuç bildirimleri',
        ],
        icon: '<path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd"/>',
      },
      {
        title: 'STQL — Görev Sorgu Dili',
        description: 'JQL\'e yakın söz dizimiyle görevlerinizi tam istediğiniz gibi süzün. Görsel filtre ile ' +
            'sorgu çubuğu aynı motoru kullanır; sekmeler arasında geçerken sorgunuz korunur.',
        bg: 'bg-amber-500',
        points: [
          'Metin araması, tarih aralıkları, özel alan sorguları ve sıralama',
          'currentUser(), currentSprint(), endOfWeek() gibi hazır fonksiyonlar',
          'Sorgularınızı kaydedin, takımla paylaşın, dashboard widget\'larına bağlayın',
        ],
        code: 'type = bug AND priority IN (Critical, High) AND created >= -7d',
        icon: '<path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>',
      },
    ],

    features: [
      {
        title: 'İş Panosu',
        description: 'Sürükle-bırak Kanban ve Scrum panosu, swimlane\'ler, özel alanlar ve akıllı filtre renklendirmesi.',
        bg: 'bg-blue-100', color: 'text-blue-600',
        icon: '<path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zM5 11a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zM11 5a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zM11 13a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>'
      },
      {
        title: 'Sprint, Backlog & Sürümler',
        description: 'Backlog\'unuzu önceliklendirin, sprint\'lerinizi planlayıp kapatın, release\'lerinizi tek ekrandan yönetin.',
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
        description: 'Burndown, velocity ve iş yükü grafikleriyle sprint sağlığınızı takip edin; panolarınızı kendiniz kurgulayın.',
        bg: 'bg-emerald-100', color: 'text-emerald-600',
        icon: '<path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"/>'
      },
      {
        title: 'Zengin Filtreler',
        description: 'Akıllı filtrelerle görevleri renkli kategorilere ayırın; çapraz filtrelemeli, etkileşimli panolar kurun.',
        bg: 'bg-teal-100', color: 'text-teal-600',
        icon: '<path fill-rule="evenodd" d="M3 3a1 1 0 011-1h12a1 1 0 011 1v3a1 1 0 01-.293.707L12 11.414V15a1 1 0 01-.293.707l-2 2A1 1 0 018 17v-5.586L3.293 6.707A1 1 0 013 6V3z" clip-rule="evenodd"/>'
      },
      {
        title: 'STQL Sorgu Dili',
        description: 'JQL\'e yakın söz dizimiyle görevlerinizi sorgulayın, filtrelerinizi kaydedin ve takımınızla paylaşın.',
        bg: 'bg-amber-100', color: 'text-amber-600',
        icon: '<path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>'
      },
      {
        title: 'Dokümanlar',
        description: 'Alanlara ayrılmış doküman ağacı, zengin metin editörü, güçlü tablolar, sürüm geçmişi ve paylaşım.',
        bg: 'bg-orange-100', color: 'text-orange-600',
        icon: '<path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd"/>'
      },
      {
        title: 'Ortak Çalışma Alanı',
        description: 'Metin, kod ve hesap tablosu dokümanlarını takımınızla eş zamanlı düzenleyin, imleçleri canlı görün.',
        bg: 'bg-violet-100', color: 'text-violet-600',
        icon: '<path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"/>'
      },
      {
        title: 'Hesap Tablosu & Makrolar',
        description: 'Formül motoru, Excel içe/dışa aktarma ve JavaScript makrolarıyla tekrar eden işlerinizi otomatikleştirin.',
        bg: 'bg-lime-100', color: 'text-lime-600',
        icon: '<path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0 100-2H7zm6 7a1 1 0 011 1v3a1 1 0 11-2 0v-3a1 1 0 011-1zm-3 3a1 1 0 100 2h.01a1 1 0 100-2H10zm-4 1a1 1 0 011-1h.01a1 1 0 110 2H7a1 1 0 01-1-1zm1-4a1 1 0 100 2h.01a1 1 0 100-2H7zm2 1a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1zm4-4a1 1 0 100 2h.01a1 1 0 100-2H13z" clip-rule="evenodd"/>'
      },
      {
        title: 'Git Entegrasyonu',
        description: 'Görev detayından branch açın, pull request oluşturun; commit ve PR hareketlerini task üzerinde izleyin.',
        bg: 'bg-slate-100', color: 'text-slate-600',
        icon: '<path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd"/>'
      },
      {
        title: 'CI/CD Entegrasyonu',
        description: 'Jenkins pipeline\'larını uygulama içinden tetikleyin, build durumunu ve deploy tarihçesini takip edin.',
        bg: 'bg-sky-100', color: 'text-sky-600',
        icon: '<path fill-rule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clip-rule="evenodd"/>'
      },
    ],

    // Kart açmaya değmeyen ama günlük kullanımda fark yaratan yetenekler
    extras: [
      'GameBox: Quiz & Adam Asmaca',
      'Özelleştirilebilir iş akışı ve durumlar',
      'Özel roller ve izin matrisi',
      'Özel alanlar',
      'Alt görevler ve görev bağlantıları',
      'Dosya ekleri',
      'Yorumlar ve bahsetme',
      'Takipçiler ve bildirimler',
      'Aktivite geçmişi',
      'E-posta ile takım daveti',
      'Çoklu proje ve takım',
      'Mobil uyumlu, kurulabilir PWA',
      'Destek merkezi',
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
        description: 'Board\'unuzu açın, poker ile tahminleyin, dokümanlarınızı birlikte yazın, retro ile her sprint biraz daha gelişin.'
      },
    ],

    // Paketler API'den (/api/plans) gelir; fiyat ve limitler superadmin panelinden
    // değiştirilebildiği için tanıtım sayfasında sabit yazılmaz.
    rawPlans: [],
    plansLoading: true,

  }),
  computed: {
    /**
     * API'den gelen paketleri vitrin kartına çevirir.
     *
     * Kart sayısı sabit değildir; superadmin panelinden paket eklenip
     * kaldırılabildiği için yerleşim de kart sayısına göre belirlenir.
     */
    planCards() {
      const sorted = [...this.rawPlans].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))

      // Deneme süresi tanımlı ilk paket vitrinde öne çıkar; yoksa en üst paket.
      let heroIndex = sorted.findIndex(p => (p.trialDays ?? 0) > 0)
      if (heroIndex < 0) heroIndex = sorted.length - 1

      return sorted.map((plan, i) => {
        const prev = i > 0 ? sorted[i - 1] : null
        const name = plan.name || plan.code
        const monthly = Number(plan.monthlyPriceTry ?? 0)
        const yearly = Number(plan.yearlyPriceTry ?? 0)
        const trialDays = plan.trialDays ?? 0

        // Fiyatı sıfır olan paket, varsayılan paketse gerçekten ücretsizdir;
        // değilse (ör. kurumsal) fiyat pazarlığa bağlıdır.
        const isFree = monthly === 0 && plan.isDefault
        let price = this.formatPrice(monthly)
        let period = '/ay'
        let priceNote = ''
        if (isFree) {
          priceNote = 'Süresiz ücretsiz, kredi kartı gerekmez'
        } else if (monthly === 0) {
          price = 'Özel fiyat'
          period = ''
          priceNote = 'Ekibinize özel fiyatlandırma'
        } else if (yearly > 0) {
          priceNote = `Yıllık ödemede ${this.formatPrice(yearly)}`
          const freeMonths = Math.round((monthly * 12 - yearly) / monthly)
          if (freeMonths > 0) priceNote += ` (${freeMonths} ay hediye)`
        }

        let badge = null
        if (trialDays > 0) badge = `${trialDays} gün ücretsiz`
        else if (i === 1) badge = 'Popüler'

        let cta = `${name} ile Başla`
        if (isFree) cta = 'Ücretsiz Başla'
        else if (trialDays > 0) cta = 'Ücretsiz Dene'

        return {
          name,
          tagline: plan.description || '',
          badge,
          highlight: i === heroIndex,
          price,
          period,
          priceNote,
          cta,
          items: this.planItems(plan, prev),
        }
      })
    },

    /** Kart sayısı dörde çıkınca üçlü ızgara sıkışıyor; yerleşim ona göre seçilir. */
    planGridClass() {
      return this.planCards.length >= 4
          ? 'grid sm:grid-cols-2 xl:grid-cols-4 gap-6 max-w-7xl mx-auto'
          : 'grid md:grid-cols-3 gap-6 max-w-5xl mx-auto'
    },

    /** Hero ve paket başlığındaki deneme cümlesi de paketlerden türetilir. */
    trialPlan() {
      return this.rawPlans.find(p => (p.trialDays ?? 0) > 0) || null
    },
  },
  methods: {
    gotoLogin() {
      this.$router.push('/login')
    },
    gotoSignup() {
      this.$router.push({ path: '/login', query: { mode: 'signup' } })
    },

    formatPrice(value) {
      return '₺' + Number(value || 0).toLocaleString('tr-TR', { maximumFractionDigits: 0 })
    },

    /**
     * Kart maddeleri: önce limitler, sonra bir alt pakete göre *fark*.
     * Tam özellik listesi yazılsaydı üst paketlerde on dört maddelik bir liste
     * çıkardı; fark listesi hem kısa hem de yükseltme sebebini gösteriyor.
     */
    planItems(plan, prev) {
      const items = []

      if (plan.maxMembers == null && plan.maxProjects == null) {
        items.push('Sınırsız üye ve proje')
      } else {
        const members = plan.maxMembers == null ? 'Sınırsız üye' : `${plan.maxMembers} üye`
        const projects = plan.maxProjects == null ? 'sınırsız proje' : `${plan.maxProjects} proje`
        items.push(`${members}, ${projects}`)
      }

      if (prev) items.push(`${prev.name || prev.code} paketindeki her şey`)

      const own = plan.features || []
      const inherited = new Set(prev?.features || [])
      const added = own.filter(f => !inherited.has(f))

      // Bilinen sıraya göre diz; sözlükte olmayan yeni bir enum değeri de
      // sessizce düşmesin diye sona ham adıyla eklenir (featureLabel).
      const ordered = FEATURE_ORDER.filter(f => added.includes(f))
          .concat(added.filter(f => !FEATURE_ORDER.includes(f)))
      ordered.forEach(f => items.push(featureLabel(f)))

      return items
    },
  },
  async mounted() {
    try {
      const { data } = await PublicApi.getPlans()
      this.rawPlans = Array.isArray(data) ? data : []
    } catch (e) {
      // Sessiz geç: paket bölümü "yüklenemedi" durumuna düşer, sayfa çalışmaya devam eder.
      this.rawPlans = []
    } finally {
      this.plansLoading = false
    }
  },
  created() {
    setSeo({
      title: 'ScrumTools — Çevik Takımlar için Scrum Poker, Retro, Sprint ve Doküman Yönetimi',
      description: 'Sprint panosu, planlama pokeri, retro panoları, backlog, zengin filtreli dashboard, eş zamanlı ortak çalışma alanı ve Git/CI-CD entegrasyonu tek platformda. Çevik takımınız için kurulum gerektirmeyen Scrum araçları — ücretsiz başlayın.',
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
