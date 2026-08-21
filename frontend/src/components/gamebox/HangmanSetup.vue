<template>
  <div class="max-w-2xl mx-auto">
    <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
      <div class="bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 p-6 sm:p-8 text-center">
        <p class="text-5xl mb-2">🪢</p>
        <h1 class="text-2xl sm:text-3xl font-bold text-white mb-1">Takım Adam Asmaca</h1>
        <p class="text-white/80 text-sm">Oturumu sen açıyorsun — moderatör sen olacaksın</p>
      </div>

      <div class="p-6 sm:p-8 space-y-6">
        <!-- Dil -->
        <div>
          <label class="block text-sm font-semibold text-gray-700 mb-2">Dil</label>
          <div class="flex items-center gap-2 bg-gray-100 rounded-xl p-1 w-fit">
            <button
                v-for="opt in languageOptions"
                :key="opt.value"
                type="button"
                @click="language = opt.value"
                :class="['px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                         language === opt.value ? 'bg-indigo-600 text-white shadow' : 'text-gray-600 hover:bg-gray-200']">
              {{ opt.flag }} {{ opt.label }}
            </button>
          </div>
        </div>

        <!-- Kelime kaynağı -->
        <div>
          <label class="block text-sm font-semibold text-gray-700 mb-2">Kelimeler</label>
          <div class="grid sm:grid-cols-2 gap-3">
            <button
                type="button"
                @click="wordSource = 'RANDOM'"
                :class="['text-left p-4 rounded-xl border-2 transition-all',
                         wordSource === 'RANDOM'
                           ? 'border-indigo-500 bg-indigo-50'
                           : 'border-gray-200 hover:border-gray-300']">
              <p class="font-semibold text-gray-900 mb-1">🎲 Rastgele</p>
              <p class="text-xs text-gray-600">Kelimeler havuzdan rastgele seçilir. Sen de oynayabilirsin.</p>
            </button>

            <button
                type="button"
                @click="wordSource = 'CUSTOM'"
                :class="['text-left p-4 rounded-xl border-2 transition-all',
                         wordSource === 'CUSTOM'
                           ? 'border-indigo-500 bg-indigo-50'
                           : 'border-gray-200 hover:border-gray-300']">
              <p class="font-semibold text-gray-900 mb-1">✍️ Kelimeleri ben gireceğim</p>
              <p class="text-xs text-gray-600">Cevapları bileceğin için bu oyunda oynayamazsın.</p>
            </button>
          </div>
        </div>

        <!-- Sıra adaleti: oyuncu sayısı tur sayısını belirler -->
        <div class="bg-indigo-50/60 border border-indigo-100 rounded-xl p-4">
          <label class="block text-sm font-semibold text-gray-700 mb-2">Kaç kişi oynayacak?</label>
          <div class="flex flex-wrap items-center gap-3">
            <input v-model.number="expectedPlayers" type="number" min="1" max="200" placeholder="—"
                   class="w-24 px-3 py-2 border border-gray-300 rounded-lg text-sm text-center font-semibold text-gray-800 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none" />
            <p class="text-xs text-gray-500 flex-1 min-w-[180px]">
              <template v-if="teamMemberCount">
                Takımda {{ teamMemberCount }} kişi var — sayıyı buna göre doldurduk, değiştirebilirsin.
              </template>
              <template v-else>
                Lobiye kaç kişi katılmasını bekliyorsan onu yaz.
              </template>
            </p>
          </div>

          <p v-if="recommendedRounds" class="text-xs text-gray-600 mt-3 leading-relaxed">
            Sıra yalnızca <strong>yanlış harfte, yanlış kelime tahmininde ve tur sonunda</strong> devreder;
            doğru harfte oyuncuda kalır. {{ expectedPlayersSafe }} kişide herkesin sırasının en az iki kez
            gelmesi için <strong>≈{{ recommendedRounds }} kelime</strong> gerekiyor.
          </p>
          <p v-if="recommendationCapped" class="text-xs text-amber-600 mt-2">
            ⚠️ Öneri {{ recommendationCapReason }} yüzünden {{ recommendedRounds }}'e sınırlandı;
            bu kalabalıkta listenin sonundakilere yine de az sıra gelebilir.
          </p>
        </div>

        <!-- Rastgele ayarları -->
        <div v-if="wordSource === 'RANDOM'" class="space-y-4">
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">Kategori</label>
            <div class="flex flex-wrap items-center gap-2">
              <select
                  v-model="category"
                  class="flex-1 min-w-[220px] px-4 py-2.5 border border-gray-300 rounded-xl text-sm bg-white text-gray-800 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none">
                <option :value="null">🎲 Karışık (tüm kategoriler)</option>
                <option v-for="opt in categoryOptions" :key="opt.code" :value="opt.code">
                  {{ opt.emoji }} {{ opt.label }}{{ opt.wordCount ? ` (${opt.wordCount})` : '' }}
                </option>
              </select>
            </div>
            <p class="text-xs text-gray-500 mt-2">
              <template v-if="selectedCategory">
                {{ selectedCategory.emoji }} {{ selectedCategory.label }} kategorisinde
                <strong>{{ selectedCategory.wordCount }}</strong> kelime var.
              </template>
              <template v-else>
                Kelimeler tüm kategorilerden karışık gelir.
              </template>
            </p>
            <p v-if="tooFewWords" class="text-xs text-amber-600 mt-1">
              ⚠️ Bu kategoride {{ selectedCategory.wordCount }} kelime var; oyun bu sayıda turla oynanır.
            </p>
          </div>

          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Kaç kelime oynanacak?
              <span class="text-indigo-600 font-bold">{{ roundCount }}</span>
            </label>
            <input v-model.number="roundCount" type="range" min="1" max="20"
                   @input="roundCountTouched = true"
                   class="w-full accent-indigo-600" />
            <div class="flex justify-between text-xs text-gray-400 mt-1">
              <span>1</span><span>20</span>
            </div>
            <p v-if="roundsBelowRecommended"
               class="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 mt-2">
              ⚠️ {{ expectedPlayersSafe }} kişi için <strong>en az {{ recommendedRounds }}</strong> kelime öneriyoruz.
              {{ roundCount }} kelimede bazı oyuncuların sırası hiç gelmeyebilir.
              <button type="button" @click="applyRecommendedRounds"
                      class="underline font-semibold hover:text-amber-900">{{ recommendedRounds }}'e çıkar</button>
            </p>
            <p v-else-if="recommendedRounds" class="text-xs text-green-700 mt-2">
              ✓ {{ expectedPlayersSafe }} kişi için yeterli — herkese sıra gelir.
            </p>
          </div>

          <label class="flex items-center gap-3 cursor-pointer">
            <input v-model="moderatorPlays" type="checkbox" class="w-4 h-4 accent-indigo-600" />
            <span class="text-sm text-gray-700">Ben de oyuncu olarak sıraya gireyim</span>
          </label>
        </div>

        <!-- Özel kelimeler -->
        <div v-else>
          <label class="block text-sm font-semibold text-gray-700 mb-2">
            Kelimeler <span class="font-normal text-gray-500">(her satıra bir kelime)</span>
          </label>
          <textarea
              v-model="customWordsRaw"
              rows="6"
              :placeholder="wordPlaceholder"
              class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none font-mono text-sm"></textarea>
          <p class="text-xs text-gray-500 mt-2">
            Boşluksuz, 2-30 harf. {{ parsedWords.length }} kelime girildi.
          </p>
          <p v-if="customWordsBelowRecommended"
             class="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 mt-2">
            ⚠️ {{ expectedPlayersSafe }} kişi için <strong>en az {{ recommendedRounds }}</strong> kelime öneriyoruz;
            şu an {{ parsedWords.length }} kelime var. Daha azında bazı oyuncuların sırası hiç gelmeyebilir.
          </p>
          <!-- Kategori burada havuzu daraltmaz; oyun sırasında açabileceğin bir ipucudur. -->
          <div class="mt-4">
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Kategori ipucu <span class="font-normal text-gray-500">(opsiyonel)</span>
            </label>
            <select
                v-model="category"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-xl text-sm bg-white text-gray-800 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none">
              <option :value="null">İpucu yok</option>
              <option v-for="opt in categoryOptions" :key="opt.code" :value="opt.code">
                {{ opt.emoji }} {{ opt.label }}
              </option>
            </select>
            <p class="text-xs text-gray-500 mt-2">
              Kelimelerin ortak kategorisini seçersen oyun sırasında
              <strong>“Kategoriyi Göster”</strong> ile oyunculara ipucu verebilirsin.
              Sen açana kadar kimseye görünmez.
            </p>
          </div>

          <div class="mt-3 flex items-start gap-2 bg-amber-50 border border-amber-200 rounded-xl p-3">
            <span class="text-lg leading-none">⚠️</span>
            <p class="text-xs text-amber-800">
              Kelimeleri sen belirlediğin için bu oturumda <strong>izleyici/sunucu</strong> olacaksın;
              sıraya girmez ve puan sıralamasında yer almazsın.
            </p>
          </div>
        </div>

        <!-- Kurallar -->
        <div class="bg-gray-50 rounded-xl p-4 text-sm text-gray-600 space-y-1">
          <p class="font-semibold text-gray-800 mb-2">Nasıl oynanır?</p>
          <p>• Ortak kelimeyi herkes görür, sırası gelen oyuncu tahmin eder.</p>
          <p>• Doğru harf <strong class="text-green-600">+10</strong> — sıra sende kalır, seri yapabilirsin.</p>
          <p>• Yanlış harf <strong class="text-red-600">-5</strong> — adam asılır, sıra sonrakine geçer.</p>
          <p>• Kelimeyi bilen, <strong class="text-indigo-600">kalan tüm harfleri</strong> bilmiş sayılır
            ve hepsinin puanını alır.</p>
          <p>• Kelime tahmini yanlışsa adam <strong>asılmaz</strong>, sadece sıranı kaybedersin.</p>
          <p>• Moderatör oyun sırasında kelimenin <strong>kategorisini</strong> ipucu olarak açabilir.</p>
          <p class="pt-1 text-xs text-gray-500">
            Harfleri tek tek toplayıp en sonda tahmin etmek ekstra puan kazandırmaz —
            kelime her hâlükârda aynı toplamı eder.
          </p>
        </div>

        <button
            @click="submit"
            :disabled="!canSubmit || submitting"
            class="w-full px-6 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-bold shadow-lg disabled:opacity-50 disabled:cursor-not-allowed">
          {{ submitting ? 'Oluşturuluyor...' : '🎮 Oturumu Aç' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { startHangmanSession, getHangmanCategories } from '../../api/HangmanApi.js'
import { getTeamById } from '../../api/TeamApi.js'
import { hangmanCategoryOptions } from '../../data/hangmanWords.js'

/**
 * Bir turda sıra ortalama kaç kez devreder.
 *
 * Sıra sadece yanlış harfte, yanlış kelime tahmininde ve tur kapanışında devrediyor
 * (doğru harfte oyuncuda kalıyor). Tur en fazla MAX_WRONG=6 yanlışta bittiğine göre
 * üst sınır 7; ortalama tur ~3 yanlışla çözülüyor, +1 de tur kapanışı → 4.
 */
const TURNS_PER_ROUND = 4

/** Herkesin sırası oyun boyunca en az kaç kez gelsin. */
const TARGET_TURNS_PER_PLAYER = 2

/** Kaydırıcının tavanı — sunucu da bundan fazlasını çizmiyor. */
const MAX_ROUNDS = 20

/** Az kişilik oyunda öneri saçma derecede düşmesin. */
const MIN_ROUNDS = 3

export default {
  name: 'HangmanSetup',
  props: {
    teamId: String
  },
  emits: ['created'],
  data: () => ({
    language: 'tr',
    wordSource: 'RANDOM',
    roundCount: 5,
    /** Moderatör kaydırıcıya dokunduysa öneri artık otomatik uygulanmaz. */
    roundCountTouched: false,
    /** Beklenen oyuncu sayısı — takım mevcudundan doldurulur, elle değiştirilebilir. */
    expectedPlayers: null,
    teamMemberCount: 0,
    /** Rastgelede kelime havuzunu daraltır (null = karışık); özel kelimelerde ipucu kategorisidir. */
    category: null,
    /** Sunucudan gelen kelime sayıları: { [code]: wordCount } */
    categoryCounts: {},
    moderatorPlays: true,
    customWordsRaw: '',
    submitting: false,
    languageOptions: [
      { value: 'tr', label: 'Türkçe', flag: '🇹🇷' },
      { value: 'en', label: 'English', flag: '🇬🇧' }
    ]
  }),
  computed: {
    /** Kategori listesi yereldeki emoji/etiketlerle, sayılar sunucudan. */
    categoryOptions() {
      return hangmanCategoryOptions(this.language)
          .map(opt => ({ ...opt, wordCount: this.categoryCounts[opt.code] || 0 }))
    },
    selectedCategory() {
      return this.categoryOptions.find(o => o.code === this.category) || null
    },
    /** Geçerli bir tahmin yoksa 0 — öneri kutuları da gizlenir. */
    expectedPlayersSafe() {
      const n = this.expectedPlayers
      return Number.isFinite(n) && n > 0 ? Math.min(Math.floor(n), 200) : 0
    },
    /** Sınırlanmamış ham öneri: her oyuncuya iki sıra düşecek kadar tur. */
    rawRecommendedRounds() {
      if (!this.expectedPlayersSafe) return 0
      const needed = Math.ceil((this.expectedPlayersSafe * TARGET_TURNS_PER_PLAYER) / TURNS_PER_ROUND)
      return Math.max(MIN_ROUNDS, needed)
    },
    /** Öneriyi kısan tavan: kaydırıcı sınırı ya da rastgele modda kategori havuzu. */
    recommendationCap() {
      const pool = this.wordSource === 'RANDOM' ? (this.selectedCategory?.wordCount || 0) : 0
      return pool > 0 ? Math.min(MAX_ROUNDS, pool) : MAX_ROUNDS
    },
    recommendedRounds() {
      if (!this.rawRecommendedRounds) return 0
      return Math.min(this.rawRecommendedRounds, this.recommendationCap)
    },
    recommendationCapped() {
      return !!this.rawRecommendedRounds && this.rawRecommendedRounds > this.recommendationCap
    },
    recommendationCapReason() {
      const pool = this.selectedCategory?.wordCount || 0
      return this.wordSource === 'RANDOM' && pool > 0 && pool < MAX_ROUNDS
          ? 'kategoride yeterli kelime olmaması'
          : 'bir oyunda en fazla ' + MAX_ROUNDS + ' kelime oynanabilmesi'
    },
    roundsBelowRecommended() {
      return this.wordSource === 'RANDOM'
          && !!this.recommendedRounds
          && this.roundCount < this.recommendedRounds
    },
    customWordsBelowRecommended() {
      return this.wordSource === 'CUSTOM'
          && !!this.recommendedRounds
          && this.parsedWords.length > 0
          && this.parsedWords.length < this.recommendedRounds
    },
    tooFewWords() {
      return !!this.selectedCategory
          && this.selectedCategory.wordCount > 0
          && this.selectedCategory.wordCount < this.roundCount
    },
    wordPlaceholder() {
      return this.language === 'tr'
          ? 'bilgisayar\nkahve\nsprint'
          : 'computer\ncoffee\nsprint'
    },
    parsedWords() {
      return this.customWordsRaw
          .split(/[\n,;]+/)
          .map(w => w.trim())
          .filter(w => w.length > 0)
    },
    canSubmit() {
      return this.wordSource === 'RANDOM' || this.parsedWords.length > 0
    }
  },
  watch: {
    language: 'loadCategories',
    /** Moderatör kaydırıcıya dokunmadıysa öneriyi kendiliğinden uygula. */
    recommendedRounds(value) {
      if (value && !this.roundCountTouched) this.roundCount = value
    }
  },
  mounted() {
    this.loadCategories()
    this.loadTeamSize()
  },
  methods: {
    /** Beklenen oyuncu sayısına makul bir başlangıç: takım mevcudu. */
    async loadTeamSize() {
      try {
        const team = await getTeamById(this.teamId)
        this.teamMemberCount = team?.memberEmails?.length || 0
        if (this.expectedPlayers === null && this.teamMemberCount > 0) {
          this.expectedPlayers = this.teamMemberCount
        }
      } catch (e) {
        // Mevcut alınamazsa kutu boş kalır; moderatör sayıyı elle girer.
        this.teamMemberCount = 0
      }
    },

    applyRecommendedRounds() {
      this.roundCount = this.recommendedRounds
      this.roundCountTouched = true
    },

    async loadCategories() {
      try {
        const categories = await getHangmanCategories(this.language)
        this.categoryCounts = Object.fromEntries(categories.map(c => [c.code, c.wordCount]))
      } catch (e) {
        // Sayılar gösterilemese de kategori seçimi çalışmaya devam eder.
        this.categoryCounts = {}
      }
    },
    async submit() {
      if (!this.canSubmit) return
      this.submitting = true
      try {
        const custom = this.wordSource === 'CUSTOM'
        const session = await startHangmanSession(this.teamId, {
          language: this.language,
          roundCount: custom ? null : this.roundCount,
          // Rastgelede havuzu daraltır (null = karışık); özel kelimelerde ise sunucu bunu
          // oturuma değil turlara yazar — moderatörün oyun içinde açacağı ipucudur.
          category: this.category,
          customWords: custom ? this.parsedWords : [],
          // Kelimeleri moderatör girdiyse sunucu zaten oynamasına izin vermez.
          moderatorPlays: custom ? false : this.moderatorPlays
        })
        this.$emit('created', session)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
      this.submitting = false
    }
  }
}
</script>
