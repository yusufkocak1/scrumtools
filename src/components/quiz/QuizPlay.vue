<template>
  <div class="max-w-3xl mx-auto">
    <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">

      <!-- Progress Bar -->
      <div class="bg-gray-100 px-6 py-3 flex items-center justify-between">
        <span class="text-sm font-medium text-gray-600">
          Soru {{ session.currentQuestionIndex + 1 }} / {{ session.totalQuestions }}
        </span>
        <div class="flex-1 mx-4">
          <div class="w-full bg-gray-200 rounded-full h-2">
            <div class="bg-indigo-600 h-2 rounded-full transition-all duration-500"
                 :style="{ width: progressPercent + '%' }"></div>
          </div>
        </div>
        <span v-if="answeredInfo" class="text-xs text-gray-500">
          {{ answeredInfo.answeredCount }}/{{ answeredInfo.totalParticipants }} cevapladı
        </span>
      </div>

      <!-- Geri Sayım -->
      <div class="relative">
        <div class="absolute top-4 right-4 z-10">
          <div class="relative w-16 h-16">
            <svg class="w-16 h-16 transform -rotate-90" viewBox="0 0 64 64">
              <circle cx="32" cy="32" r="28" fill="none" stroke="#E5E7EB" stroke-width="4" />
              <circle cx="32" cy="32" r="28" fill="none"
                      :stroke="timerColor" stroke-width="4"
                      stroke-linecap="round"
                      :stroke-dasharray="circumference"
                      :stroke-dashoffset="timerOffset"
                      class="transition-all duration-1000 ease-linear" />
            </svg>
            <div class="absolute inset-0 flex items-center justify-center">
              <span class="text-lg font-bold" :class="timeLeft <= 5 ? 'text-red-600' : 'text-gray-800'">
                {{ timeLeft }}
              </span>
            </div>
          </div>
        </div>

        <!-- Soru -->
        <div class="p-8 pt-6" v-if="currentQuestion">
          <h2 class="text-xl md:text-2xl font-bold text-gray-900 mb-8 pr-20">
            {{ currentQuestion.questionText }}
          </h2>

          <!-- Seçenekler -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <button v-for="(opt, idx) in currentQuestion.options" :key="idx"
                    @click="selectAnswer(idx)"
                    :disabled="hasAnswered || timeLeft <= 0"
                    :class="[
                      'relative p-5 rounded-xl border-2 text-left transition-all duration-300 transform',
                      optionClass(idx)
                    ]">
              <div class="flex items-center gap-3">
                <span :class="[
                  'w-10 h-10 rounded-lg flex items-center justify-center font-bold text-lg',
                  optionBadgeClass(idx)
                ]">
                  {{ ['A','B','C','D','E','F'][idx] }}
                </span>
                <span class="text-base font-medium">{{ opt }}</span>
              </div>
              <!-- Doğru/Yanlış ikonu — sadece sonuç açıklandığında -->
              <div v-if="isRevealed && idx === revealedCorrectIndex"
                   class="absolute top-2 right-2 w-8 h-8 bg-green-500 rounded-full flex items-center justify-center">
                <span class="text-white text-lg">✓</span>
              </div>
              <div v-if="isRevealed && idx === selectedIndex && idx !== revealedCorrectIndex"
                   class="absolute top-2 right-2 w-8 h-8 bg-red-500 rounded-full flex items-center justify-center">
                <span class="text-white text-lg">✗</span>
              </div>
            </button>
          </div>

          <!-- Cevap Kaydedildi (sonuç henüz açıklanmadı) -->
          <div v-if="hasAnswered && !isRevealed" class="mt-6 text-center">
            <div class="inline-flex items-center gap-2 px-6 py-3 bg-indigo-100 text-indigo-700 rounded-xl">
              <svg class="w-5 h-5 animate-pulse" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path>
              </svg>
              <span class="font-bold">Cevabınız kaydedildi — Sonuç bekleniyor...</span>
            </div>
          </div>

          <!-- Sonuç açıklandı — doğru/yanlış göster -->
          <div v-if="isRevealed && hasAnswered" class="mt-6 text-center">
            <div v-if="selectedIndex === revealedCorrectIndex"
                 class="inline-flex items-center gap-2 px-6 py-3 bg-green-100 text-green-700 rounded-xl">
              <span class="text-2xl">🎉</span>
              <span class="font-bold">Doğru!</span>
            </div>
            <div v-else
                 class="inline-flex items-center gap-2 px-6 py-3 bg-red-100 text-red-700 rounded-xl">
              <span class="text-2xl">😞</span>
              <span class="font-bold">Yanlış! Doğru cevap: {{ ['A','B','C','D','E','F'][revealedCorrectIndex] }}</span>
            </div>
          </div>

          <!-- Süre Doldu — cevap verilmedi, sonuç henüz açıklanmadı -->
          <div v-if="timeLeft <= 0 && !hasAnswered && !isRevealed" class="mt-6 text-center">
            <div class="inline-flex items-center gap-2 px-6 py-3 bg-yellow-100 text-yellow-700 rounded-xl">
              <span class="text-2xl">⏰</span>
              <span class="font-bold">Süre doldu!</span>
            </div>
          </div>

          <!-- Süre Doldu + Sonuç açıklandı — cevap verilmemişti -->
          <div v-if="isRevealed && !hasAnswered" class="mt-6 text-center">
            <div class="inline-flex items-center gap-2 px-6 py-3 bg-yellow-100 text-yellow-700 rounded-xl">
              <span class="text-2xl">⏰</span>
              <span class="font-bold">Cevap vermediniz. Doğru cevap: {{ ['A','B','C','D','E','F'][revealedCorrectIndex] }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Leaderboard Özet — sadece sonuç açıklandığında göster -->
      <div v-if="isRevealed && session.leaderboard && session.leaderboard.length > 0" class="border-t border-gray-200 p-6">
        <h4 class="text-sm font-semibold text-gray-600 mb-3">🏅 Sıralama</h4>
        <div class="space-y-2">
          <div v-for="entry in session.leaderboard.slice(0, 5)" :key="entry.email"
               class="flex items-center justify-between px-4 py-2 rounded-lg"
               :class="entry.rank <= 3 ? 'bg-yellow-50' : 'bg-gray-50'">
            <div class="flex items-center gap-3">
              <span class="text-lg">{{ rankEmoji(entry.rank) }}</span>
              <span class="font-medium text-gray-800">{{ entry.displayName }}</span>
            </div>
            <span class="font-bold text-indigo-600">{{ entry.totalScore }}</span>
          </div>
        </div>
      </div>

      <!-- Host Kontrolleri -->
      <div v-if="isHost" class="border-t border-gray-200 p-4 bg-gray-50 flex justify-center gap-4">
        <!-- Cevapları Göster butonu — henüz açıklanmadıysa -->
        <button v-if="!isRevealed"
                @click="$emit('show-result')"
                class="px-6 py-2.5 bg-amber-500 text-white rounded-xl hover:bg-amber-600 transition-colors font-medium">
          👁 Cevapları Göster
        </button>

        <!-- Sonraki Soru / Bitir — sonuç açıklandıktan sonra -->
        <template v-if="isRevealed">
          <button v-if="session.currentQuestionIndex < session.totalQuestions - 1"
                  @click="$emit('next')"
                  class="px-6 py-2.5 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-medium">
            Sonraki Soru ▶
          </button>
          <button v-else
                  @click="$emit('finish')"
                  class="px-6 py-2.5 bg-green-600 text-white rounded-xl hover:bg-green-700 transition-colors font-medium">
            🏁 Bitir
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { submitAnswer } from '../../api/QuizApi.js'


export default {
  name: 'QuizPlay',
  props: {
    session: Object,
    isHost: Boolean,
    teamId: String,
    answeredInfo: Object
  },
  emits: ['answered', 'next', 'show-result', 'finish'],
  data() {
    return {
      timeLeft: 0,
      timer: null,
      hasAnswered: false,
      selectedIndex: -1,
      startTime: 0,
    }
  },
  computed: {
    currentQuestion() {
      return this.session?.currentQuestion
    },
    isRevealed() {
      // Jackson boolean serialization: record'larda "resultsRevealed" veya "isResultsRevealed" olabilir
      return this.session?.resultsRevealed === true
    },
    revealedCorrectIndex() {
      return this.session?.correctOptionIndex ?? -1
    },
    circumference() {
      return 2 * Math.PI * 28
    },
    timerOffset() {
      if (!this.currentQuestion) return 0
      const total = this.currentQuestion.timeLimitSeconds
      const ratio = this.timeLeft / total
      return this.circumference * (1 - ratio)
    },
    timerColor() {
      if (this.timeLeft <= 5) return '#EF4444'
      if (this.timeLeft <= 10) return '#F59E0B'
      return '#6366F1'
    },
    progressPercent() {
      if (!this.session) return 0
      return ((this.session.currentQuestionIndex + 1) / this.session.totalQuestions) * 100
    }
  },
  methods: {
    startTimer() {
      this.stopTimer()
      if (!this.currentQuestion) return

      this.timeLeft = this.currentQuestion.timeLimitSeconds
      this.hasAnswered = false
      this.selectedIndex = -1
      this.startTime = Date.now()

      this.timer = setInterval(() => {
        this.timeLeft--
        if (this.timeLeft <= 0) {
          this.stopTimer()
        }
      }, 1000)
    },

    stopTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },

    async selectAnswer(index) {
      if (this.hasAnswered || this.timeLeft <= 0) return

      this.selectedIndex = index
      this.hasAnswered = true
      this.stopTimer()

      const answeredInMs = Date.now() - this.startTime

      try {
        await submitAnswer(this.teamId, this.session.id, {
          questionId: this.currentQuestion.id,
          selectedOptionIndex: index,
          answeredInMs
        })
        this.$emit('answered')
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    optionClass(idx) {
      if (this.isRevealed) {
        if (idx === this.revealedCorrectIndex) {
          return 'border-green-500 bg-green-50 scale-[1.02]'
        }
        if (idx === this.selectedIndex && idx !== this.revealedCorrectIndex) {
          return 'border-red-500 bg-red-50'
        }
        return 'border-gray-200 opacity-50 cursor-not-allowed'
      }

      if (this.hasAnswered) {
        if (idx === this.selectedIndex) {
          return 'border-indigo-500 bg-indigo-50'
        }
        return 'border-gray-200 opacity-60 cursor-not-allowed'
      }

      if (this.timeLeft <= 0) {
        return 'border-gray-200 opacity-60 cursor-not-allowed'
      }

      return 'border-gray-200 hover:border-indigo-400 hover:bg-indigo-50 hover:-translate-y-1 cursor-pointer'
    },

    optionBadgeClass(idx) {
      const colors = [
        'bg-red-100 text-red-700',
        'bg-blue-100 text-blue-700',
        'bg-yellow-100 text-yellow-700',
        'bg-green-100 text-green-700',
        'bg-purple-100 text-purple-700',
        'bg-pink-100 text-pink-700',
      ]
      return colors[idx % colors.length]
    },

    rankEmoji(rank) {
      if (rank === 1) return '🥇'
      if (rank === 2) return '🥈'
      if (rank === 3) return '🥉'
      return `#${rank}`
    }
  },

  watch: {
    'session.currentQuestionIndex'() {
      this.startTimer()
    }
  },

  mounted() {
    if (this.currentQuestion) {
      this.startTimer()
    }
  },

  beforeUnmount() {
    this.stopTimer()
  }
}
</script>

