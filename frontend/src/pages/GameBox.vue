<template>
  <div class="flex w-full min-h-screen pb-20 lg:pb-0">
    <SideBar :teamId="teamId" />
    <div class="flex-1 min-w-0 p-4 sm:p-6 bg-gray-50 overflow-auto">

      <!-- Oyun Merkezi (henüz oyun seçilmedi ve aktif takım oturumu yok) -->
      <div v-if="!currentGame && !activeSession && !hangmanSession">
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">🎮 GameBox</h1>
          <p class="text-gray-500 mt-1">Takımınla oyna, eğlen, öğren</p>
        </div>

        <div class="grid sm:grid-cols-2 gap-6 max-w-3xl">
          <div @click="currentGame = 'quiz'"
               class="bg-white rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 cursor-pointer transform hover:-translate-y-1 border border-gray-200 p-6">
            <div class="w-14 h-14 bg-yellow-100 rounded-xl flex items-center justify-center mb-4">
              <span class="text-3xl">🧠</span>
            </div>
            <h3 class="text-xl font-semibold text-gray-900 mb-2">Quiz</h3>
            <p class="text-gray-600 text-sm">Kahoot benzeri takım yarışması — kendi sorularını hazırla, takımınla yarış</p>
          </div>

          <div @click="currentGame = 'hangman'"
               class="bg-white rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 cursor-pointer transform hover:-translate-y-1 border border-gray-200 p-6">
            <div class="w-14 h-14 bg-indigo-100 rounded-xl flex items-center justify-center mb-4">
              <span class="text-3xl">🪢</span>
            </div>
            <h3 class="text-xl font-semibold text-gray-900 mb-2">Adam Asmaca</h3>
            <p class="text-gray-600 text-sm">Türkçe ve İngilizce kelime havuzuyla klasik adam asmaca oyunu</p>
          </div>
        </div>
      </div>

      <!-- Adam Asmaca -->
      <div v-else-if="currentGame === 'hangman'">
        <!-- Aktif takım oturumu: lobi / oyun / sonuç -->
        <template v-if="hangmanSession">
          <HangmanLobby
              v-if="hangmanSession.status === 'LOBBY'"
              :session="hangmanSession"
              :isHost="isHangmanHost"
              :teamId="teamId"
              @begin="handleHangmanBegin"
              @joined="refreshHangmanSession"
          />

          <HangmanPlay
              v-else-if="hangmanSession.status === 'IN_PROGRESS'"
              :session="hangmanSession"
              :isHost="isHangmanHost"
              :teamId="teamId"
              @updated="hangmanSession = $event"
              @skip="handleHangmanSkip"
              @finish="handleHangmanFinish"
          />

          <HangmanResult
              v-else-if="hangmanSession.status === 'FINISHED'"
              :session="hangmanSession"
              @back="closeHangmanSession"
          />
        </template>

        <!-- Mod seçimi -->
        <template v-else>
          <button
              @click="exitHangman"
              class="mb-4 px-4 py-2 bg-gray-100 text-gray-700 rounded-xl hover:bg-gray-200 transition-colors font-medium text-sm">
            ← {{ hangmanMode ? 'Adam Asmaca' : 'GameBox' }}
          </button>

          <div v-if="!hangmanMode" class="grid sm:grid-cols-2 gap-6 max-w-3xl">
            <div @click="hangmanMode = 'solo'"
                 class="bg-white rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 cursor-pointer transform hover:-translate-y-1 border border-gray-200 p-6">
              <div class="w-14 h-14 bg-gray-100 rounded-xl flex items-center justify-center mb-4">
                <span class="text-3xl">🙋</span>
              </div>
              <h3 class="text-xl font-semibold text-gray-900 mb-2">Tek Kişilik</h3>
              <p class="text-gray-600 text-sm">Kendi başına pratik yap, istediğin kadar kelime çöz</p>
            </div>

            <div @click="hangmanMode = 'team'"
                 class="bg-white rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 cursor-pointer transform hover:-translate-y-1 border border-gray-200 p-6">
              <div class="w-14 h-14 bg-indigo-100 rounded-xl flex items-center justify-center mb-4">
                <span class="text-3xl">👥</span>
              </div>
              <h3 class="text-xl font-semibold text-gray-900 mb-2">Takım Oyunu</h3>
              <p class="text-gray-600 text-sm">
                Oturum aç, takımın sırayla oynasın, puan sıralaması oluşsun
              </p>
            </div>
          </div>

          <HangmanGame v-else-if="hangmanMode === 'solo'" />

          <HangmanSetup
              v-else-if="hangmanMode === 'team'"
              :teamId="teamId"
              @created="hangmanSession = $event"
          />
        </template>
      </div>

      <!-- Quiz: Aktif Oturum Varsa -->
      <div v-else-if="activeSession">
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

      <!-- Quiz: Aktif Oturum Yoksa → Şablon Listesi -->
      <div v-else>
        <div class="flex items-center justify-between mb-6">
          <div>
            <button
                @click="currentGame = null"
                class="mb-2 px-3 py-1.5 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium text-xs">
              ← GameBox
            </button>
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
import HangmanGame from '../components/gamebox/HangmanGame.vue'
import HangmanSetup from '../components/gamebox/HangmanSetup.vue'
import HangmanLobby from '../components/gamebox/HangmanLobby.vue'
import HangmanPlay from '../components/gamebox/HangmanPlay.vue'
import HangmanResult from '../components/gamebox/HangmanResult.vue'
import {
  getTemplates, getTemplate, deleteTemplate,
  startSession, getActiveSession, nextQuestion,
  showQuestionResult, finishSession
} from '../api/QuizApi.js'
import {
  getActiveHangmanSession, getHangmanSession, beginHangmanGame,
  skipHangmanTurn, finishHangmanSession
} from '../api/HangmanApi.js'
import { connect, subscribe, unsubscribe } from '../api/websocket.js'
import { createToast } from 'mosha-vue-toastify'

export default {
  name: 'GameBox',
  components: {
    SideBar,
    QuizTemplateList,
    QuizTemplateForm,
    QuizLobby,
    QuizPlay,
    QuizLeaderboard,
    QuizReport,
    HangmanGame,
    HangmanSetup,
    HangmanLobby,
    HangmanPlay,
    HangmanResult,
  },
  props: {
    teamId: String
  },
  data: () => ({
    currentGame: null, // null | 'quiz' | 'hangman'
    templates: [],
    loading: true,
    showCreateForm: false,
    editingTemplate: null,
    activeSession: null,
    showHistory: false,
    answeredInfo: null,
    // Adam Asmaca
    hangmanMode: null,    // null | 'solo' | 'team'
    hangmanSession: null,
  }),
  computed: {
    isHost() {
      if (!this.activeSession) return false
      const email = localStorage.getItem('user') || ''
      return this.activeSession.hostEmail === email
    },
    isHangmanHost() {
      if (!this.hangmanSession) return false
      const email = localStorage.getItem('user') || ''
      return this.hangmanSession.hostEmail === email
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

    // ─── Adam Asmaca ────────────────────────────────────────────────────────

    async checkActiveHangmanSession() {
      try {
        const session = await getActiveHangmanSession(this.teamId)
        if (session && session.id) {
          // Takımda oturum varsa oyuncular doğrudan görsün.
          this.hangmanSession = session
          this.currentGame = 'hangman'
        }
      } catch {
        // aktif oturum yok
      }
    },

    async refreshHangmanSession() {
      if (!this.hangmanSession) return
      try {
        this.hangmanSession = await getHangmanSession(this.teamId, this.hangmanSession.id)
      } catch (e) {
        console.error('Adam asmaca oturumu yenilenemedi:', e)
      }
    },

    async handleHangmanBegin() {
      try {
        this.hangmanSession = await beginHangmanGame(this.teamId, this.hangmanSession.id)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    async handleHangmanSkip() {
      try {
        this.hangmanSession = await skipHangmanTurn(this.teamId, this.hangmanSession.id)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    async handleHangmanFinish() {
      try {
        this.hangmanSession = await finishHangmanSession(this.teamId, this.hangmanSession.id)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    closeHangmanSession() {
      this.hangmanSession = null
      this.hangmanMode = null
      this.currentGame = null
    },

    /** Adam asmaca içinde bir geri adım: mod seçiliyse moda, değilse GameBox'a. */
    exitHangman() {
      if (this.hangmanMode) {
        this.hangmanMode = null
      } else {
        this.currentGame = null
      }
    },

    setupWebSocket() {
      connect(() => {
        subscribe(`/topic/hangman/${this.teamId}/state`, (data) => {
          // Oturumu kapatmış bir kullanıcıyı bitmiş oyuna geri sürükleme.
          if (!this.hangmanSession && data.status === 'FINISHED') return
          this.hangmanSession = data
          this.currentGame = 'hangman'
        })
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
      unsubscribe(`/topic/hangman/${this.teamId}/state`)
    }
  },

  mounted() {
    this.loadTemplates()
    this.checkActiveSession()
    this.checkActiveHangmanSession()
    this.setupWebSocket()
  },

  beforeUnmount() {
    this.cleanupWebSocket()
  }
}
</script>

