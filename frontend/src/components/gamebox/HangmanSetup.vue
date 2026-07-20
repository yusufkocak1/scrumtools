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

        <!-- Rastgele ayarları -->
        <div v-if="wordSource === 'RANDOM'" class="space-y-4">
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">
              Kaç kelime oynanacak?
              <span class="text-indigo-600 font-bold">{{ roundCount }}</span>
            </label>
            <input v-model.number="roundCount" type="range" min="1" max="20"
                   class="w-full accent-indigo-600" />
            <div class="flex justify-between text-xs text-gray-400 mt-1">
              <span>1</span><span>20</span>
            </div>
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
import { startHangmanSession } from '../../api/HangmanApi.js'

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
    moderatorPlays: true,
    customWordsRaw: '',
    submitting: false,
    languageOptions: [
      { value: 'tr', label: 'Türkçe', flag: '🇹🇷' },
      { value: 'en', label: 'English', flag: '🇬🇧' }
    ]
  }),
  computed: {
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
  methods: {
    async submit() {
      if (!this.canSubmit) return
      this.submitting = true
      try {
        const custom = this.wordSource === 'CUSTOM'
        const session = await startHangmanSession(this.teamId, {
          language: this.language,
          roundCount: custom ? null : this.roundCount,
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
