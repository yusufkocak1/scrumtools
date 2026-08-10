<template>
  <div class="max-w-4xl mx-auto">
    <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">

      <!-- Üst Şerit -->
      <div class="bg-gradient-to-r from-slate-800 to-slate-700 px-6 py-4 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <span class="px-2.5 py-1 bg-amber-400 text-slate-900 rounded-full text-xs font-bold">🎤 MODERATÖR</span>
          <span class="text-white font-medium">{{ session.templateTitle }}</span>
        </div>
        <span class="text-slate-300 text-sm">
          Soru {{ session.currentQuestionIndex + 1 }} / {{ session.totalQuestions }}
        </span>
      </div>

      <!-- Progress -->
      <div class="bg-gray-100 px-6 py-3 flex items-center gap-4">
        <div class="flex-1">
          <div class="w-full bg-gray-200 rounded-full h-2">
            <div class="bg-indigo-600 h-2 rounded-full transition-all duration-500"
                 :style="{ width: progressPercent + '%' }"></div>
          </div>
        </div>
        <span class="text-sm font-medium" :class="timeLeft <= 5 ? 'text-red-600' : 'text-gray-600'">
          ⏱ {{ timeLeft }}s
        </span>
        <span class="text-sm font-medium text-gray-600">
          ✍️ {{ answeredCount }}/{{ totalParticipants }} cevapladı
        </span>
      </div>

      <!-- Soru -->
      <div class="p-6" v-if="question">
        <h2 class="text-xl md:text-2xl font-bold text-gray-900 mb-4">{{ question.questionText }}</h2>

        <div v-if="question.imageUrl" class="mb-5 flex justify-center">
          <img :src="question.imageUrl" alt="Soru görseli"
               class="max-h-64 w-auto rounded-xl border border-gray-200 shadow-sm" />
        </div>

        <!-- Seçenekler: doğru cevap moderatöre baştan açık, canlı dağılım gösterilir -->
        <div class="space-y-2">
          <div v-for="(opt, idx) in question.options" :key="idx"
               class="relative overflow-hidden p-3 rounded-xl border-2"
               :class="idx === question.correctOptionIndex
                 ? 'border-green-500 bg-green-50'
                 : 'border-gray-200 bg-gray-50'">
            <!-- Dağılım çubuğu -->
            <div class="absolute inset-y-0 left-0 bg-indigo-100/60 transition-all duration-500"
                 :style="{ width: distributionPercent(idx) + '%' }"></div>
            <div class="relative flex items-center gap-3">
              <span class="w-8 h-8 rounded-lg flex items-center justify-center font-bold text-sm shrink-0"
                    :class="idx === question.correctOptionIndex
                      ? 'bg-green-500 text-white'
                      : 'bg-white text-gray-500 border border-gray-300'">
                {{ letters[idx] }}
              </span>
              <span class="flex-1 font-medium text-gray-800">{{ opt }}</span>
              <span v-if="idx === question.correctOptionIndex"
                    class="text-green-600 text-sm font-bold whitespace-nowrap">✓ Doğru cevap</span>
              <span class="text-sm text-gray-500 w-10 text-right">{{ optionCount(idx) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Katılımcı Durumu -->
      <div class="border-t border-gray-200 p-6">
        <h4 class="text-sm font-semibold text-gray-600 mb-3">👥 Katılımcılar</h4>

        <div v-if="answerStatuses.length === 0" class="text-sm text-gray-400">
          Katılımcı yok
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-2">
          <div v-for="p in answerStatuses" :key="p.email"
               class="flex items-center justify-between px-3 py-2 rounded-lg border"
               :class="p.answered ? 'bg-white border-gray-200' : 'bg-gray-50 border-dashed border-gray-300'">
            <div class="flex items-center gap-2 min-w-0">
              <span class="text-sm">{{ p.answered ? (p.correct ? '✅' : '❌') : '⏳' }}</span>
              <span class="text-sm font-medium text-gray-800 truncate">{{ p.displayName }}</span>
            </div>
            <div class="flex items-center gap-3 text-xs shrink-0">
              <span v-if="p.answered" class="text-gray-500">
                {{ letters[p.selectedOptionIndex] || '—' }} • {{ (p.answeredInMs / 1000).toFixed(1) }}s
              </span>
              <span v-else class="text-gray-400">bekleniyor</span>
              <span class="font-bold text-indigo-600 w-12 text-right">{{ p.totalScore }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Sıralama -->
      <div v-if="session.leaderboard && session.leaderboard.length > 0"
           class="border-t border-gray-200 px-6 py-4 bg-gray-50">
        <h4 class="text-sm font-semibold text-gray-600 mb-2">🏅 Sıralama</h4>
        <div class="flex flex-wrap gap-2">
          <span v-for="entry in session.leaderboard.slice(0, 5)" :key="entry.email"
                class="px-3 py-1.5 bg-white rounded-lg border border-gray-200 text-sm">
            {{ rankEmoji(entry.rank) }} {{ entry.displayName }}
            <span class="font-bold text-indigo-600 ml-1">{{ entry.totalScore }}</span>
          </span>
        </div>
      </div>

      <!-- Kontroller -->
      <div class="border-t border-gray-200 p-4 bg-white flex flex-wrap justify-center gap-3">
        <button v-if="!isRevealed"
                @click="$emit('show-result')"
                class="px-6 py-2.5 bg-amber-500 text-white rounded-xl hover:bg-amber-600 transition-colors font-medium">
          👁 Cevapları Göster
        </button>

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

        <!-- Erken bitirme — son soruda zaten "Bitir" var, orada gizlenir -->
        <button v-if="!isOnLastRevealedQuestion" @click="confirmFinish"
                class="px-4 py-2.5 bg-gray-100 text-gray-600 rounded-xl hover:bg-gray-200 transition-colors text-sm font-medium">
          Yarışmayı Sonlandır
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { getModeratorView } from '../../api/QuizApi.js'

/**
 * Moderatör paneli — oturumu başlatan kişi yarışmaz, sunar.
 *
 * Doğru cevap ve kimin ne işaretlediği ortak WebSocket topic'inde taşınamaz
 * (oyunculara da giderdi); bu yüzden panel kendi verisini host'a özel
 * /sessions/{id}/moderator ucundan çeker ve her state/cevap olayında yeniler.
 */
export default {
  name: 'QuizModeratorPanel',
  props: {
    session: Object,
    teamId: String,
    answeredInfo: Object
  },
  emits: ['next', 'show-result', 'finish'],
  data() {
    return {
      view: null,
      timeLeft: 0,
      timer: null,
      letters: ['A', 'B', 'C', 'D', 'E', 'F'],
    }
  },
  computed: {
    question() {
      return this.view?.currentQuestion
    },
    isRevealed() {
      return this.session?.resultsRevealed === true
    },
    answerStatuses() {
      return this.view?.answerStatuses || []
    },
    answeredCount() {
      return this.view?.answeredCount ?? 0
    },
    totalParticipants() {
      return this.view?.totalParticipants ?? (this.session?.participants?.length || 0)
    },
    progressPercent() {
      if (!this.session) return 0
      return ((this.session.currentQuestionIndex + 1) / this.session.totalQuestions) * 100
    },
    isOnLastRevealedQuestion() {
      return this.isRevealed && this.session.currentQuestionIndex >= this.session.totalQuestions - 1
    }
  },
  methods: {
    confirmFinish() {
      if (confirm('Yarışmayı şimdi sonlandırmak istediğinize emin misiniz? Kalan sorular sorulmayacak.')) {
        this.$emit('finish')
      }
    },

    async loadView() {
      try {
        this.view = await getModeratorView(this.teamId, this.session.id)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    optionCount(idx) {
      return this.view?.optionCounts?.[idx] ?? 0
    },

    /** Dağılım çubuğu — en çok işaretlenen seçenek %100 genişlikte. */
    distributionPercent(idx) {
      const counts = this.view?.optionCounts || []
      const max = Math.max(...counts, 0)
      if (!max) return 0
      return (this.optionCount(idx) / max) * 100
    },

    startTimer() {
      this.stopTimer()
      if (!this.question) return

      this.timeLeft = this.question.timeLimitSeconds
      this.timer = setInterval(() => {
        this.timeLeft--
        if (this.timeLeft <= 0) this.stopTimer()
      }, 1000)
    },

    stopTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },

    rankEmoji(rank) {
      if (rank === 1) return '🥇'
      if (rank === 2) return '🥈'
      if (rank === 3) return '🥉'
      return `#${rank}`
    }
  },

  watch: {
    'session.currentQuestionIndex': {
      async handler() {
        await this.loadView()
        this.startTimer()
      }
    },
    // Sonuç açıklandığında puanlar/sıralama değişir — paneli tazele.
    'session.resultsRevealed'() {
      this.loadView()
    },
    // Biri cevap verdiğinde canlı dağılımı güncelle.
    answeredInfo() {
      this.loadView()
    }
  },

  async mounted() {
    await this.loadView()
    this.startTimer()
  },

  beforeUnmount() {
    this.stopTimer()
  }
}
</script>
