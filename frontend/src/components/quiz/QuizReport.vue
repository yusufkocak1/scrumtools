<template>
  <div>
    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
    </div>

    <!-- Rapor Detayı -->
    <div v-else-if="selectedReport" class="max-w-4xl mx-auto">
      <button @click="selectedReport = null" class="mb-4 text-indigo-600 hover:text-indigo-800 font-medium">
        ← Listeye Dön
      </button>

      <div class="bg-white rounded-xl shadow-md border border-gray-200 p-6">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h2 class="text-2xl font-bold text-gray-900">{{ selectedReport.session.templateTitle }}</h2>
            <p class="text-gray-500 text-sm mt-1">
              Host: {{ selectedReport.session.hostName }} •
              {{ formatDate(selectedReport.session.finishedAt) }}
            </p>
          </div>
          <span class="px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">Tamamlandı</span>
        </div>

        <!-- Sıralama -->
        <h3 class="text-lg font-semibold text-gray-800 mb-3">🏅 Sıralama</h3>
        <div class="space-y-2 mb-6">
          <div v-for="(p, idx) in selectedReport.participants" :key="p.email"
               class="flex items-center justify-between px-4 py-3 rounded-xl"
               :class="idx < 3 ? 'bg-yellow-50 border border-yellow-200' : 'bg-gray-50 border border-gray-200'">
            <div class="flex items-center gap-3">
              <span class="w-8 text-center font-bold text-gray-600">{{ idx + 1 }}</span>
              <span class="font-medium text-gray-800">{{ p.displayName }}</span>
            </div>
            <div class="flex items-center gap-4 text-sm">
              <span class="text-green-600">{{ p.correctCount }}/{{ p.answeredCount }} doğru</span>
              <span class="font-bold text-indigo-600">{{ p.totalScore }} puan</span>
            </div>
          </div>
        </div>

        <!-- Soru Detayları -->
        <h3 class="text-lg font-semibold text-gray-800 mb-3">📝 Soru Detayları</h3>
        <div class="space-y-4">
          <div v-for="q in selectedReport.questions" :key="q.id"
               class="p-4 bg-gray-50 rounded-xl border border-gray-200">
            <p class="font-medium text-gray-800 mb-2">{{ q.questionOrder + 1 }}. {{ q.questionText }}</p>
            <div class="grid grid-cols-2 gap-2 mb-3">
              <div v-for="(opt, oi) in q.options" :key="oi"
                   :class="[
                     'px-3 py-2 rounded-lg text-sm',
                     oi === q.correctOptionIndex ? 'bg-green-100 text-green-800 font-medium' : 'bg-white text-gray-600 border border-gray-200'
                   ]">
                {{ ['A','B','C','D','E','F'][oi] }}. {{ opt }}
                <span v-if="oi === q.correctOptionIndex" class="ml-1">✓</span>
              </div>
            </div>
            <p class="text-xs text-gray-400">⏱ {{ q.timeLimitSeconds }} saniye</p>

            <!-- Bu sorudaki cevaplar -->
            <div class="mt-3 space-y-1">
              <div v-for="a in getAnswersForQuestion(q.id)" :key="a.userEmail"
                   class="flex items-center justify-between text-sm px-3 py-1.5 rounded-lg"
                   :class="a.correct ? 'bg-green-50' : 'bg-red-50'">
                <span class="text-gray-700">{{ getParticipantName(a.userEmail) }}</span>
                <div class="flex items-center gap-3">
                  <span :class="a.correct ? 'text-green-600' : 'text-red-600'">
                    {{ a.correct ? '✓' : '✗' }} {{ ['A','B','C','D','E','F'][a.selectedOptionIndex] }}
                  </span>
                  <span class="text-gray-500">{{ (a.answeredInMs / 1000).toFixed(1) }}s</span>
                  <span class="font-medium text-indigo-600">+{{ a.score }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Geçmiş Listesi -->
    <div v-else>
      <div v-if="sessions.length === 0" class="text-center py-16 text-gray-400">
        <p class="text-4xl mb-2">📊</p>
        <p>Henüz tamamlanmış quiz yok</p>
      </div>

      <div v-else class="space-y-4">
        <div v-for="s in sessions" :key="s.id"
             @click="loadReport(s.id)"
             class="bg-white rounded-xl shadow-md border border-gray-200 p-5 hover:shadow-lg transition-shadow cursor-pointer">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ s.templateTitle }}</h3>
              <p class="text-sm text-gray-500 mt-1">
                Host: {{ s.hostName }} • {{ s.totalQuestions }} soru •
                {{ s.participants?.length || 0 }} katılımcı
              </p>
            </div>
            <div class="text-right">
              <p class="text-sm text-gray-400">{{ formatDate(s.finishedAt) }}</p>
              <p v-if="s.leaderboard && s.leaderboard[0]" class="text-sm text-yellow-600 font-medium mt-1">
                🏆 {{ s.leaderboard[0].displayName }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getSessionHistory, getSessionReport } from '../../api/QuizApi.js'
import { createToast } from 'mosha-vue-toastify'

export default {
  name: 'QuizReport',
  props: {
    teamId: String
  },
  data() {
    return {
      sessions: [],
      loading: true,
      selectedReport: null,
    }
  },
  methods: {
    async loadHistory() {
      this.loading = true
      try {
        this.sessions = await getSessionHistory(this.teamId)
      } catch (e) {
        createToast('Geçmiş yüklenemedi', { type: 'danger' })
      }
      this.loading = false
    },
    async loadReport(sessionId) {
      this.loading = true
      try {
        this.selectedReport = await getSessionReport(this.teamId, sessionId)
      } catch (e) {
        createToast('Rapor yüklenemedi', { type: 'danger' })
      }
      this.loading = false
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      return new Date(dateStr).toLocaleDateString('tr-TR', {
        year: 'numeric', month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit'
      })
    },
    getAnswersForQuestion(questionId) {
      if (!this.selectedReport?.answers) return []
      return this.selectedReport.answers.filter(a => a.questionId === questionId)
    },
    getParticipantName(email) {
      if (!this.selectedReport?.participants) return email
      const p = this.selectedReport.participants.find(p => p.email === email)
      return p?.displayName || email
    }
  },
  mounted() {
    this.loadHistory()
  }
}
</script>

