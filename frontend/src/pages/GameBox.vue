<template>
  <div class="flex w-full min-h-screen">
    <div class="flex-1 min-w-0 p-4 sm:p-6 bg-gray-50 overflow-auto">

      <!-- Oyun Merkezi — süren oyun olsa bile kullanıcı buradan seçer, otomatik girilmez -->
      <div v-if="!currentGame">
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">🎮 GameBox</h1>
          <p class="text-gray-500 mt-1">Takımınla oyna, eğlen, öğren</p>
        </div>

        <div class="grid sm:grid-cols-2 gap-6 max-w-3xl">
          <div @click="currentGame = 'quiz'"
               class="bg-white rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 cursor-pointer transform hover:-translate-y-1 border border-gray-200 p-6">
            <div class="flex items-start justify-between mb-4">
              <div class="w-14 h-14 bg-yellow-100 rounded-xl flex items-center justify-center">
                <span class="text-3xl">🧠</span>
              </div>
              <span v-if="activeSessions.length > 0"
                    class="px-2.5 py-1 bg-red-50 text-red-600 rounded-full text-xs font-medium flex items-center gap-1.5">
                <span class="w-1.5 h-1.5 bg-red-500 rounded-full animate-pulse"></span>
                {{ activeSessions.length }} aktif
              </span>
            </div>
            <h3 class="text-xl font-semibold text-gray-900 mb-2">Quiz</h3>
            <p class="text-gray-600 text-sm">Kahoot benzeri takım yarışması — kendi sorularını hazırla, takımınla yarış</p>
          </div>

          <div @click="currentGame = 'hangman'"
               class="bg-white rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 cursor-pointer transform hover:-translate-y-1 border border-gray-200 p-6">
            <div class="flex items-start justify-between mb-4">
              <div class="w-14 h-14 bg-indigo-100 rounded-xl flex items-center justify-center">
                <span class="text-3xl">🪢</span>
              </div>
              <span v-if="hasActiveHangman"
                    class="px-2.5 py-1 bg-red-50 text-red-600 rounded-full text-xs font-medium flex items-center gap-1.5">
                <span class="w-1.5 h-1.5 bg-red-500 rounded-full animate-pulse"></span>
                Devam eden oyun
              </span>
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
              @cancelled="hangmanSession = null"
          />

          <HangmanPlay
              v-else-if="hangmanSession.status === 'IN_PROGRESS'"
              :session="hangmanSession"
              :isHost="isHangmanHost"
              :teamId="teamId"
              @updated="hangmanSession = $event"
              @skip="handleHangmanSkip"
              @next-round="handleHangmanNextRound"
              @reveal-category="handleHangmanRevealCategory"
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

      <!-- Quiz: Girilen Oturum -->
      <div v-else-if="currentSession">
        <!-- LOBBY -->
        <QuizLobby
            v-if="currentSession.status === 'LOBBY'"
            :session="currentSession"
            :isHost="isHost"
            :teamId="teamId"
            @start="handleNextQuestion"
            @joined="refreshSession"
            @leave="leaveSession"
            @cancelled="leaveSession"
        />

        <!-- IN_PROGRESS — moderatör sunar, diğerleri oynar -->
        <QuizModeratorPanel
            v-else-if="currentSession.status === 'IN_PROGRESS' && isModerator"
            :session="currentSession"
            :teamId="teamId"
            :answeredInfo="answeredInfo"
            @next="handleNextQuestion"
            @show-result="handleShowResult"
            @finish="handleFinish"
        />

        <QuizPlay
            v-else-if="currentSession.status === 'IN_PROGRESS'"
            :session="currentSession"
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
            v-else-if="currentSession.status === 'FINISHED'"
            :session="currentSession"
            :teamId="teamId"
            @back="leaveSession"
        />
      </div>

      <!-- Quiz: Aktif yarışmalar + şablonlar -->
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

        <!-- Süren yarışmalar — kullanıcı buradan girer -->
        <QuizActiveSessions
            v-if="!showHistory && !showCreateForm && !editingTemplate"
            :sessions="activeSessions"
            :teamId="teamId"
            @open="openSession"
            @cancelled="handleSessionCancelled"
        />

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
import QuizTemplateList from '../components/quiz/QuizTemplateList.vue'
import QuizTemplateForm from '../components/quiz/QuizTemplateForm.vue'
import QuizLobby from '../components/quiz/QuizLobby.vue'
import QuizPlay from '../components/quiz/QuizPlay.vue'
import QuizModeratorPanel from '../components/quiz/QuizModeratorPanel.vue'
import QuizActiveSessions from '../components/quiz/QuizActiveSessions.vue'
import QuizLeaderboard from '../components/quiz/QuizLeaderboard.vue'
import QuizReport from '../components/quiz/QuizReport.vue'
import HangmanGame from '../components/gamebox/HangmanGame.vue'
import HangmanSetup from '../components/gamebox/HangmanSetup.vue'
import HangmanLobby from '../components/gamebox/HangmanLobby.vue'
import HangmanPlay from '../components/gamebox/HangmanPlay.vue'
import HangmanResult from '../components/gamebox/HangmanResult.vue'
import {
  getTemplates, getTemplate, deleteTemplate,
  startSession, getActiveSessions, getSession, nextQuestion,
  showQuestionResult, finishSession
} from '../api/QuizApi.js'
import {
  getActiveHangmanSession, getHangmanSession, beginHangmanGame,
  skipHangmanTurn, nextHangmanRound, revealHangmanCategory, finishHangmanSession
} from '../api/HangmanApi.js'
import { connect, subscribe, unsubscribe } from '../api/websocket.js'
import { createToast } from 'mosha-vue-toastify'
import { useTeamContext } from '../composables/useTeamContext.js'

export default {
  name: 'GameBox',
  components: {
    QuizTemplateList,
    QuizTemplateForm,
    QuizLobby,
    QuizPlay,
    QuizModeratorPanel,
    QuizActiveSessions,
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
  setup(props) {
    // Paylaşılan linkteki takım merkezi context'e adopte edilir — sonraki
    // gezinmeler (Board, Retro...) aynı takımda devam eder.
    useTeamContext().adoptTeam(props.teamId)
  },
  data: () => ({
    currentGame: null, // null | 'quiz' | 'hangman'
    templates: [],
    loading: true,
    showCreateForm: false,
    editingTemplate: null,
    activeSessions: [],   // takımda süren tüm yarışmalar (liste ekranı)
    currentSession: null, // kullanıcının içine girdiği oturum
    showHistory: false,
    answeredInfo: null,
    // Adam Asmaca
    hangmanMode: null,    // null | 'solo' | 'team'
    hangmanSession: null,
  }),
  computed: {
    isHost() {
      if (!this.currentSession) return false
      const email = localStorage.getItem('user') || ''
      return this.currentSession.hostEmail === email
    },
    /** Moderatör modunda oturumu başlatan kişi yarışmaz — oyun ekranı yerine panel görür. */
    isModerator() {
      return this.isHost && this.currentSession?.moderatorMode === true
    },
    isHangmanHost() {
      if (!this.hangmanSession) return false
      const email = localStorage.getItem('user') || ''
      return this.hangmanSession.hostEmail === email
    },
    hasActiveHangman() {
      return !!this.hangmanSession && this.hangmanSession.status !== 'FINISHED'
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

    /**
     * Süren yarışmaları yükler — kullanıcıyı otomatik oyuna sokmaz.
     * Aynı takımda paralel oturumlar olabildiği için hangisine girileceği
     * kullanıcının seçimi.
     */
    async loadActiveSessions() {
      try {
        this.activeSessions = await getActiveSessions(this.teamId)
      } catch {
        this.activeSessions = []
      }
    },

    /** Listeden bir oturuma girer — güncel durumu sunucudan alarak. */
    async openSession(session) {
      this.answeredInfo = null
      this.currentSession = session
      this.currentGame = 'quiz'
      try {
        this.currentSession = await getSession(this.teamId, session.id)
      } catch (e) {
        // Liste verisiyle devam edilir
      }
    },

    /** Oturumdan çıkar (oyun bitmez) — liste ekranına döner. */
    leaveSession() {
      this.currentSession = null
      this.answeredInfo = null
      this.loadActiveSessions()
      this.loadTemplates()
    },

    handleSessionCancelled(sessionId) {
      this.activeSessions = this.activeSessions.filter(s => s.id !== sessionId)
      if (this.currentSession?.id === sessionId) {
        this.currentSession = null
      }
    },

    /** WS'ten gelen durumu aktif liste ile eşitler. */
    mergeActiveSession(session) {
      const isActive = session.status === 'LOBBY' || session.status === 'IN_PROGRESS'
      const rest = this.activeSessions.filter(s => s.id !== session.id)
      this.activeSessions = isActive ? [session, ...rest] : rest
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

    async handleStartSession({ templateId, moderatorMode }) {
      try {
        this.answeredInfo = null
        this.currentSession = await startSession(this.teamId, templateId, moderatorMode)
        this.mergeActiveSession(this.currentSession)
        createToast(moderatorMode
            ? 'Quiz lobby oluşturuldu — moderatörsünüz'
            : 'Quiz lobby oluşturuldu!', { type: 'success' })
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    async handleNextQuestion() {
      try {
        this.answeredInfo = null
        this.currentSession = await nextQuestion(this.teamId, this.currentSession.id)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    async handleShowResult() {
      try {
        this.currentSession = await showQuestionResult(this.teamId, this.currentSession.id)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    async handleFinish() {
      try {
        this.currentSession = await finishSession(this.teamId, this.currentSession.id)
        this.mergeActiveSession(this.currentSession)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    handleAnswered(info) {
      // QuizPlay'den gelen cevap bilgisi
    },

    async refreshSession() {
      if (!this.currentSession) return
      try {
        this.currentSession = await getSession(this.teamId, this.currentSession.id)
      } catch (e) {
        console.error('Session yenilenemedi:', e)
      }
    },

    // ─── Adam Asmaca ────────────────────────────────────────────────────────

    /**
     * Süren oturumu yükler ama ekranı değiştirmez — kullanıcı GameBox'tan
     * kartın üzerindeki "devam eden oyun" rozetini görüp kendisi girer.
     */
    async checkActiveHangmanSession() {
      try {
        const session = await getActiveHangmanSession(this.teamId)
        if (session && session.id) {
          this.hangmanSession = session
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

    /** Tur arası ekranından sonraki kelimeye geçer; kelime kalmadıysa oyun biter. */
    async handleHangmanNextRound() {
      try {
        this.hangmanSession = await nextHangmanRound(this.teamId, this.hangmanSession.id)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },

    async handleHangmanRevealCategory() {
      try {
        this.hangmanSession = await revealHangmanCategory(this.teamId, this.hangmanSession.id)
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
          // Lobi kapatıldıysa ekranı boşalt — CANCELLED hiçbir ekranın karşılığı değil,
          // state'te bırakılırsa kullanıcı boş bir sayfada kilitli kalır. Kapatan
          // moderatöre kendi ekranı zaten bilgi verdi, ona ikinci toast gösterilmez.
          if (data.status === 'CANCELLED') {
            if (this.currentGame === 'hangman' && this.hangmanSession
                && data.hostEmail !== (localStorage.getItem('user') || '')) {
              createToast('Moderatör lobiyi kapattı', { type: 'info' })
            }
            this.hangmanSession = null
            return
          }
          // Oturumu kapatmış bir kullanıcıyı bitmiş oyuna geri sürükleme.
          if (!this.hangmanSession && data.status === 'FINISHED') return
          // Ekran değiştirilmez: kullanıcı oyuna GameBox'tan kendisi girer.
          this.hangmanSession = data
        })
        subscribe(`/topic/quiz/${this.teamId}/state`, (data) => {
          // Aynı takımda paralel yarışmalar olabilir: liste her zaman güncellenir,
          // ekran ise yalnızca içinde bulunduğum oturumu takip eder.
          this.mergeActiveSession(data)
          if (this.currentSession?.id !== data.id) return

          // Lobi kapatıldıysa ekranı boşalt. Kapatan kişiye kendi ekranı zaten
          // bilgi verdi — ikinci bir toast göstermeyelim.
          if (data.status === 'CANCELLED') {
            if (data.hostEmail !== (localStorage.getItem('user') || '')) {
              createToast('Moderatör lobiyi kapattı', { type: 'info' })
            }
            this.leaveSession()
            return
          }

          // Soru değiştiyse önceki sorunun cevap sayacı taşınmasın.
          if (data.currentQuestionIndex !== this.currentSession.currentQuestionIndex) {
            this.answeredInfo = null
          }

          // WebSocket mesajında resultsRevealed undefined gelirse mevcut değeri koru
          if (data.resultsRevealed === undefined && this.currentSession.resultsRevealed === true
              && data.currentQuestionIndex === this.currentSession.currentQuestionIndex) {
            data.resultsRevealed = this.currentSession.resultsRevealed
            data.correctOptionIndex = this.currentSession.correctOptionIndex
          }
          this.currentSession = data
        })
        subscribe(`/topic/quiz/${this.teamId}/answered`, (data) => {
          // Başka bir oturumun cevap sayacı bu ekrana yazılmasın.
          if (data.sessionId && data.sessionId !== this.currentSession?.id) return
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
    this.loadActiveSessions()
    this.checkActiveHangmanSession()
    this.setupWebSocket()
  },

  beforeUnmount() {
    this.cleanupWebSocket()
  }
}
</script>

