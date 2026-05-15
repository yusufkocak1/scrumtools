<template>
  <div class="flex w-full min-h-screen">
    <SideBar :teamId="teamId" />
    <div class="flex-1 p-6 bg-gray-50 overflow-auto">

      <!-- Aktif Oturum Varsa -->
      <div v-if="activeSession">
        <!-- LOBBY -->
        <QuizLobby
            v-if="activeSession.status === 'LOBBY'"
            :session="activeSession"
            :isHost="isHost"
            :teamId="teamId"
            @start="handleNextQuestion"
            @joined="refreshSession"
        />

        <!-- IN_PROGRESS -->
        <QuizPlay
            v-else-if="activeSession.status === 'IN_PROGRESS'"
            :session="activeSession"
            :isHost="isHost"
            :teamId="teamId"
            :answeredInfo="answeredInfo"
            @answered="handleAnswered"
            @next="handleNextQuestion"
            @show-result="handleShowResult"
            @finish="handleFinish"
        />

        <!-- FINISHED -->
        <QuizLeaderboard
            v-else-if="activeSession.status === 'FINISHED'"
            :session="activeSession"
            :teamId="teamId"
            @back="closeSession"
        />
      </div>

      <!-- Aktif Oturum Yoksa → Şablon Listesi -->
      <div v-else>
        <div class="flex items-center justify-between mb-6">
          <div>
            <h1 class="text-3xl font-bold text-gray-900">Quiz</h1>
            <p class="text-gray-500 mt-1">Kahoot benzeri takım yarışması</p>
          </div>
          <div class="flex gap-3">
            <button
                v-if="!showHistory"
                @click="showHistory = true"
                class="px-4 py-2 bg-gray-100 text-gray-700 rounded-xl hover:bg-gray-200 transition-colors font-medium">
              📊 Geçmiş
            </button>
            <button
                v-if="showHistory"
                @click="showHistory = false"
                class="px-4 py-2 bg-gray-100 text-gray-700 rounded-xl hover:bg-gray-200 transition-colors font-medium">
              ← Şablonlar
            </button>
            <button
                v-if="!showHistory"
                @click="showCreateForm = true"
                class="px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-medium">
              + Yeni Şablon
            </button>
          </div>
        </div>

        <!-- Geçmiş Raporlar -->
        <QuizReport
            v-if="showHistory"
            :teamId="teamId"
        />

        <!-- Şablon Oluştur/Düzenle Formu -->
        <QuizTemplateForm
            v-else-if="showCreateForm || editingTemplate"
            :teamId="teamId"
            :template="editingTemplate"
            @saved="handleTemplateSaved"
            @cancel="closeForm"
        />

        <!-- Şablon Listesi -->
        <QuizTemplateList
            v-else
            :teamId="teamId"
            :templates="templates"
            :loading="loading"
            @edit="handleEditTemplate"
            @delete="handleDeleteTemplate"
            @start="handleStartSession"
            @refresh="loadTemplates"
        />
      </div>
    </div>
  </div>
</template>

<script>
import SideBar from '../components/SideBar.vue'
import QuizTemplateList from '../components/quiz/QuizTemplateList.vue'
import QuizTemplateForm from '../components/quiz/QuizTemplateForm.vue'
import QuizLobby from '../components/quiz/QuizLobby.vue'
import QuizPlay from '../components/quiz/QuizPlay.vue'
import QuizLeaderboard from '../components/quiz/QuizLeaderboard.vue'
import QuizReport from '../components/quiz/QuizReport.vue'
import {
  getTemplates, getTemplate, deleteTemplate,
  startSession, getActiveSession, nextQuestion,
  showQuestionResult, finishSession
} from '../api/QuizApi.js'
import { connect, subscribe, unsubscribe } from '../api/websocket.js'
import { createToast } from 'mosha-vue-toastify'

export default {
  name: 'Quiz',
  components: {
    SideBar,
    QuizTemplateList,
    QuizTemplateForm,
    QuizLobby,
    QuizPlay,
    QuizLeaderboard,
    QuizReport,
  },
  props: {
    teamId: String
  },
  data: () => ({
    templates: [],
    loading: true,
    showCreateForm: false,
    editingTemplate: null,
    activeSession: null,
    showHistory: false,
    answeredInfo: null,
  }),
  computed: {
    isHost() {
      if (!this.activeSession) return false
      const email = localStorage.getItem('user') || ''
      return this.activeSession.hostEmail === email
    }
  },
  methods: {
    async loadTemplates() {
      this.loading = true
      try {
        this.templates = await getTemplates(this.teamId)
      } catch (e) {
        console.error('Şablonlar yüklenemedi:', e)
      }
      this.loading = false
    },

    async checkActiveSession() {
      try {
        const session = await getActiveSession(this.teamId)
        if (session && session.id) {
          this.activeSession = session
        }
      } catch {
        // aktif oturum yok
      }
    },

    handleTemplateSaved() {
      this.showCreateForm = false
      this.editingTemplate = null
      this.loadTemplates()
    },

    closeForm() {
      this.showCreateForm = false
      this.editingTemplate = null
    },

    async handleEditTemplate(templateId) {
      try {
        this.editingTemplate = await getTemplate(this.teamId, templateId)
        this.showCreateForm = false
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    async handleDeleteTemplate(templateId) {
      try {
        await deleteTemplate(this.teamId, templateId)
        createToast('Şablon silindi', { type: 'success' })
        this.loadTemplates()
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    async handleStartSession(templateId) {
      try {
        this.activeSession = await startSession(this.teamId, templateId)
        createToast('Quiz lobby oluşturuldu!', { type: 'success' })
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    async handleNextQuestion() {
      try {
        this.activeSession = await nextQuestion(this.teamId, this.activeSession.id)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    async handleShowResult() {
      try {
        const result = await showQuestionResult(this.teamId, this.activeSession.id)
        console.log('[Quiz] showQuestionResult response:', JSON.stringify(result, null, 2))
        console.log('[Quiz] resultsRevealed:', result.resultsRevealed, 'correctOptionIndex:', result.correctOptionIndex)
        this.activeSession = result
      } catch (e) {
        console.error('[Quiz] showQuestionResult error:', e)
      }
    },

    async handleFinish() {
      try {
        this.activeSession = await finishSession(this.teamId, this.activeSession.id)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    handleAnswered(info) {
      // QuizPlay'den gelen cevap bilgisi
    },

    async refreshSession() {
      if (this.activeSession) {
        try {
          const { getSession } = await import('../api/QuizApi.js')
          this.activeSession = await getSession(this.teamId, this.activeSession.id)
        } catch (e) {
          console.error('Session yenilenemedi:', e)
        }
      }
    },

    closeSession() {
      this.activeSession = null
      this.loadTemplates()
    },

    setupWebSocket() {
      connect(() => {
        subscribe(`/topic/quiz/${this.teamId}/state`, (data) => {
          console.log('[Quiz WS] state received:', 'resultsRevealed:', data.resultsRevealed, 'correctOptionIndex:', data.correctOptionIndex, 'status:', data.status)
          // WebSocket mesajında resultsRevealed undefined gelirse mevcut değeri koru
          if (this.activeSession && data.resultsRevealed === undefined && this.activeSession.resultsRevealed === true
              && data.currentQuestionIndex === this.activeSession.currentQuestionIndex) {
            data.resultsRevealed = this.activeSession.resultsRevealed
            data.correctOptionIndex = this.activeSession.correctOptionIndex
          }
          this.activeSession = data
        })
        subscribe(`/topic/quiz/${this.teamId}/answered`, (data) => {
          this.answeredInfo = data
        })
      })
    },

    cleanupWebSocket() {
      unsubscribe(`/topic/quiz/${this.teamId}/state`)
      unsubscribe(`/topic/quiz/${this.teamId}/answered`)
    }
  },

  mounted() {
    this.loadTemplates()
    this.checkActiveSession()
    this.setupWebSocket()
  },

  beforeUnmount() {
    this.cleanupWebSocket()
  }
}
</script>

