<template>
  <div class="max-w-6xl mx-auto">
    <HangmanIntermission :session="session" :isHost="isHost"
                         @next-round="$emit('next-round')"
                         @pause-intermission="$emit('pause-intermission')"
                         @finish="$emit('finish')" />

    <div class="grid lg:grid-cols-[1fr_320px] gap-6 items-start">
      <!-- ─── Oyun alanı: kendi tahtam ─── -->
      <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
        <div :class="['p-4 sm:p-5 text-center transition-colors',
                      intermission
                        ? 'bg-gradient-to-r from-slate-600 to-slate-700'
                        : 'bg-gradient-to-r from-amber-500 via-orange-500 to-rose-500']">
          <p v-if="intermission" class="text-white font-bold text-lg">⏸️ Tur bitti</p>
          <p v-else-if="isSpectator" class="text-white font-medium">👁️ Yarışı izliyorsun</p>
          <p v-else-if="board.status === 'SOLVED'" class="text-white font-bold text-lg">
            ✅ Bitirdin — {{ board.finishOrder }}. sıradasın!
          </p>
          <p v-else-if="board.status === 'FAILED'" class="text-white font-bold text-lg">💀 Asıldın — diğerlerini izle</p>
          <p v-else class="text-white font-bold text-lg">🏁 Yarış! Herkes aynı kelimede — ilk bitiren kazanır</p>
          <p class="text-white/80 text-xs mt-1">
            Kelime {{ session.currentRoundIndex + 1 }} / {{ session.totalRounds }}
            · {{ finishedCount }} / {{ progress.length }} kişi bitirdi
          </p>

          <div v-if="!intermission" class="mt-3 max-w-sm mx-auto">
            <div class="flex items-center justify-between text-[11px] text-white/85 mb-1">
              <span class="font-semibold">⏱️ {{ secondsLeft }} sn</span>
              <span>Süre dolunca bitiremeyen puan alamaz</span>
            </div>
            <div class="h-1.5 bg-white/25 rounded-full overflow-hidden">
              <div :class="['h-full transition-all duration-1000 ease-linear',
                            secondsLeft <= 10 ? 'bg-red-300' : 'bg-white']"
                   :style="{ width: timeProgress + '%' }"></div>
            </div>
          </div>
          <p v-if="categoryText"
             class="mt-2 inline-flex items-center gap-1.5 px-3 py-1 bg-white/20 rounded-full backdrop-blur-sm text-white text-xs font-medium">
            🏷️ Kategori: {{ categoryText }}
          </p>
        </div>

        <div v-if="!isSpectator" class="p-6 sm:p-8">
          <div class="grid sm:grid-cols-[auto_1fr] gap-6 items-start">
            <!-- Darağacı — yalnızca benim yanlışlarım -->
            <div class="flex flex-col items-center">
              <svg viewBox="0 0 200 220" class="w-40 h-44 text-gray-700">
                <line x1="10" y1="210" x2="130" y2="210" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>
                <line x1="40" y1="210" x2="40" y2="20" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>
                <line x1="40" y1="20" x2="120" y2="20" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>
                <line x1="120" y1="20" x2="120" y2="45" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>

                <circle v-if="board.wrongCount >= 1" cx="120" cy="60" r="15" stroke="#ef4444" stroke-width="4" fill="none"/>
                <line v-if="board.wrongCount >= 2" x1="120" y1="75" x2="120" y2="130" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="board.wrongCount >= 3" x1="120" y1="90" x2="100" y2="110" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="board.wrongCount >= 4" x1="120" y1="90" x2="140" y2="110" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="board.wrongCount >= 5" x1="120" y1="130" x2="100" y2="160" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="board.wrongCount >= 6" x1="120" y1="130" x2="140" y2="160" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
              </svg>
              <p class="text-sm font-medium" :class="lives <= 2 ? 'text-red-600' : 'text-gray-500'">
                Kalan can: {{ lives }} / {{ board.maxWrong }}
              </p>
            </div>

            <div>
              <div class="text-center mb-6">
                <p class="text-3xl sm:text-4xl font-mono font-bold tracking-widest text-gray-900 break-all">
                  {{ displayWord }}
                </p>
                <p v-if="wrongLetters.length" class="mt-3 text-sm text-gray-500">
                  Yanlış harfler:
                  <span class="text-red-500 font-semibold">{{ wrongLetters.join(' ') }}</span>
                </p>
                <p v-if="board.status === 'SOLVED'" class="mt-3 text-sm text-green-600 font-semibold">
                  +{{ board.scoreEarned }} puan
                </p>
              </div>

              <div class="flex flex-wrap justify-center gap-1.5 mb-6">
                <button v-for="letter in alphabet" :key="letter"
                        :disabled="!canPlay || isGuessed(letter) || busy"
                        @click="submitLetter(letter)"
                        :class="['w-9 h-9 sm:w-10 sm:h-10 rounded-lg text-sm font-semibold transition-colors border',
                                 letterClass(letter)]">
                  {{ letter }}
                </button>
              </div>

              <div class="border-t border-gray-200 pt-5">
                <p class="text-sm font-semibold text-gray-700 mb-2">💡 Kelimeyi biliyor musun?</p>
                <p class="text-xs text-gray-500 mb-3">
                  Doğruysa hemen bitirirsin. Yanlış tahmin <strong class="text-red-600">bir can</strong> götürür.
                </p>
                <div class="flex gap-2">
                  <input v-model="wordGuess" :disabled="!canPlay || busy" @keyup.enter="submitWord"
                         type="text" placeholder="Tüm kelimeyi yaz..."
                         class="flex-1 px-4 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-orange-400 focus:border-orange-400 outline-none disabled:bg-gray-50 disabled:text-gray-400" />
                  <button @click="submitWord" :disabled="!canPlay || busy || !wordGuess.trim()"
                          class="px-5 py-2.5 bg-orange-500 text-white rounded-xl hover:bg-orange-600 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap">
                    Tahmin Et
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="p-8 text-center text-sm text-gray-500">
          Kelimeleri sen belirlediğin için bu yarışta oynamıyorsun — sağdan ilerlemeyi izleyebilirsin.
        </div>

        <!-- Moderatör kontrolleri -->
        <div v-if="isHost" class="border-t border-gray-200 bg-gray-50 px-6 py-4 flex flex-wrap gap-3 justify-between items-center">
          <p class="text-xs text-gray-500">Moderatör kontrolleri</p>
          <div class="flex gap-2">
            <button v-if="canRevealCategory && !intermission" @click="$emit('reveal-category')"
                    title="Kelimenin kategorisini tüm oyunculara gösterir — geri alınamaz"
                    class="px-4 py-2 bg-white border border-indigo-300 text-indigo-700 rounded-lg hover:bg-indigo-50 transition-colors text-sm font-medium">
              🏷️ Kategoriyi Göster
            </button>
            <button @click="$emit('finish')"
                    class="px-4 py-2 bg-red-50 border border-red-200 text-red-700 rounded-lg hover:bg-red-100 transition-colors text-sm font-medium">
              ⏹️ Oyunu Bitir
            </button>
          </div>
        </div>
      </div>

      <!-- ─── Yan panel ─── -->
      <div class="space-y-4">
        <HangmanReactions v-if="isParticipant" :session="session" :teamId="teamId" :reactions="reactions" />

        <!-- Canlı ilerleme — harfler değil, yalnızca kaç harf açıldığı -->
        <div class="bg-white rounded-2xl shadow-lg border border-gray-200 overflow-hidden">
          <div class="px-5 py-3 border-b border-gray-200 bg-gray-50">
            <h3 class="font-semibold text-gray-800">🏃 Yarış Durumu</h3>
          </div>
          <div class="divide-y divide-gray-100">
            <div v-for="p in progress" :key="p.email"
                 :class="['px-5 py-3', p.email === myEmail ? 'bg-yellow-50' : '']">
              <div class="flex items-center gap-2 text-sm">
                <span class="w-6 text-center shrink-0">{{ statusIcon(p) }}</span>
                <span class="flex-1 truncate text-gray-800">
                  {{ p.displayName }}
                  <span v-if="p.email === myEmail" class="text-xs text-indigo-600">(sen)</span>
                </span>
                <span v-if="p.status === 'SOLVED'" class="text-xs font-bold text-green-600">+{{ p.scoreEarned }}</span>
                <span v-else class="text-xs text-gray-400">{{ '❤️'.repeat(Math.max(0, p.maxWrong - p.wrongCount)) }}</span>
              </div>
              <div class="mt-1.5 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                <div :class="['h-full transition-all duration-500',
                              p.status === 'SOLVED' ? 'bg-green-500' : p.status === 'FAILED' ? 'bg-red-400' : 'bg-orange-400']"
                     :style="{ width: (p.wordLength ? (p.revealedCount / p.wordLength) * 100 : 0) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Puan tablosu -->
        <div class="bg-white rounded-2xl shadow-lg border border-gray-200 overflow-hidden">
          <div class="px-5 py-3 border-b border-gray-200 bg-gray-50">
            <h3 class="font-semibold text-gray-800">🏆 Puan Sıralaması</h3>
          </div>
          <div class="divide-y divide-gray-100">
            <div v-for="entry in leaderboard" :key="entry.email" class="flex items-center gap-3 px-5 py-3">
              <span class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 bg-gray-100 text-gray-600">
                {{ entry.rank }}
              </span>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-gray-800 truncate">
                  {{ entry.displayName }}
                  <span v-if="entry.email === myEmail" class="text-xs text-indigo-600">(sen)</span>
                </p>
                <p class="text-xs text-gray-400">{{ entry.wordsSolved }} kelime bitirdi</p>
              </div>
              <span class="font-bold text-indigo-600 shrink-0">{{ entry.totalScore }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getHangmanRaceBoard, raceHangmanLetter, raceHangmanWord } from '../../api/HangmanApi.js'
import { hangmanCategoryLabel } from '../../data/hangmanWords.js'
import HangmanIntermission from './HangmanIntermission.vue'
import HangmanReactions from './HangmanReactions.vue'

const TR_ALPHABET = ['A', 'B', 'C', 'Ç', 'D', 'E', 'F', 'G', 'Ğ', 'H', 'I', 'İ', 'J', 'K', 'L', 'M',
  'N', 'O', 'Ö', 'P', 'R', 'S', 'Ş', 'T', 'U', 'Ü', 'V', 'Y', 'Z']
const EN_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

/** Tahta henüz yüklenmediyse (ya da hiç tahmin yoksa) gösterilecek boş hâl. */
const emptyBoard = (length = 0) => ({
  roundId: null,
  maskedWord: '_'.repeat(length),
  wordLength: length,
  guessedLetters: [],
  wrongLetters: [],
  wrongCount: 0,
  maxWrong: 6,
  status: 'ACTIVE',
  finishOrder: null,
  scoreEarned: 0
})

/**
 * Yarış modu oyun ekranı — herkes aynı kelimeyi kendi tahtasında aynı anda çözer.
 *
 * Tahtanın harfleri yalnızca bize döner (race/board ve tahmin yanıtları); WS ile gelen
 * oturum durumunda diğerlerinin sadece ilerlemesi (raceProgress) vardır.
 */
export default {
  name: 'HangmanRacePlay',
  components: { HangmanIntermission, HangmanReactions },
  props: {
    session: Object,
    isHost: Boolean,
    teamId: String,
    reactions: { type: Array, default: () => [] }
  },
  emits: ['finish', 'reveal-category', 'next-round', 'pause-intermission'],
  data: () => ({
    board: emptyBoard(),
    wordGuess: '',
    busy: false,
    secondsLeft: 0,
    tickTimer: null
  }),
  computed: {
    round() {
      return this.session?.round || null
    },
    intermission() {
      return !!this.session?.awaitingNextRound && !!this.session?.lastFinishedRound
    },
    locale() {
      return this.session?.language === 'tr' ? 'tr-TR' : 'en-US'
    },
    alphabet() {
      return this.session?.language === 'tr' ? TR_ALPHABET : EN_ALPHABET
    },
    myEmail() {
      return localStorage.getItem('user') || ''
    },
    isParticipant() {
      return (this.session?.participants || []).some(p => p.email === this.myEmail)
    },
    isSpectator() {
      return (this.session?.participants || []).some(p => p.email === this.myEmail && p.spectator)
    },
    canPlay() {
      return this.isParticipant && !this.isSpectator && !this.intermission
          && this.board.status === 'ACTIVE' && this.secondsLeft > 0
    },
    progress() {
      return this.session?.raceProgress || []
    },
    finishedCount() {
      return this.progress.filter(p => p.status !== 'ACTIVE').length
    },
    leaderboard() {
      return this.session?.leaderboard || []
    },
    timeProgress() {
      const total = this.session?.turnSeconds || 90
      return Math.max(0, Math.min(100, (this.secondsLeft / total) * 100))
    },
    lives() {
      return Math.max(0, this.board.maxWrong - this.board.wrongCount)
    },
    displayWord() {
      return (this.board.maskedWord || '')
          .split('')
          .map(ch => ch === '_' ? '_' : ch.toLocaleUpperCase(this.locale))
          .join(' ')
    },
    wrongLetters() {
      return (this.board.wrongLetters || []).map(l => l.toLocaleUpperCase(this.locale))
    },
    categoryText() {
      if (!this.round?.categoryRevealed) return null
      return hangmanCategoryLabel(this.round.category, this.session?.language) || this.round.categoryLabel
    },
    canRevealCategory() {
      return !!this.round?.categoryAvailable && !this.round?.categoryRevealed
    },
    /** Tur değişince tahtayı yeniden çek. */
    roundKey() {
      return this.round?.id || null
    }
  },
  watch: {
    session: {
      handler() {
        this.secondsLeft = this.session?.turnSecondsLeft ?? 0
      },
      immediate: true
    },
    roundKey: {
      handler(id, old) {
        if (id && id !== old) {
          this.board = emptyBoard(this.round?.wordLength || 0)
          this.wordGuess = ''
          this.loadBoard()
        }
      },
      immediate: true
    }
  },
  methods: {
    async loadBoard() {
      if (!this.isParticipant || this.isSpectator) return
      try {
        this.board = await getHangmanRaceBoard(this.teamId, this.session.id)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    },
    isGuessed(letter) {
      return (this.board.guessedLetters || []).includes(letter.toLocaleLowerCase(this.locale))
    },
    letterClass(letter) {
      if (!this.isGuessed(letter)) {
        return this.canPlay
            ? 'bg-white border-gray-300 text-gray-700 hover:bg-orange-50 hover:border-orange-300'
            : 'bg-white border-gray-200 text-gray-300 cursor-not-allowed'
      }
      return (this.board.wrongLetters || []).includes(letter.toLocaleLowerCase(this.locale))
          ? 'bg-red-100 border-red-300 text-red-500'
          : 'bg-green-100 border-green-300 text-green-700'
    },
    statusIcon(p) {
      if (p.status === 'SOLVED') {
        return p.finishOrder === 1 ? '🥇' : p.finishOrder === 2 ? '🥈' : p.finishOrder === 3 ? '🥉' : '✅'
      }
      if (p.status === 'FAILED') return '💀'
      return '✏️'
    },
    async submitLetter(letter) {
      if (!this.canPlay || this.busy) return
      this.busy = true
      try {
        this.board = await raceHangmanLetter(this.teamId, this.session.id, letter.toLocaleLowerCase(this.locale))
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
      this.busy = false
    },
    async submitWord() {
      const guess = this.wordGuess.trim()
      if (!this.canPlay || this.busy || !guess) return
      this.busy = true
      try {
        this.board = await raceHangmanWord(this.teamId, this.session.id, guess.toLocaleLowerCase(this.locale))
        this.wordGuess = ''
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
      this.busy = false
    },
    tick() {
      if (this.secondsLeft > 0) this.secondsLeft--
    },
    handleKeydown(e) {
      if (!this.canPlay || this.busy) return
      if (e.target && ['INPUT', 'TEXTAREA'].includes(e.target.tagName)) return
      if (e.key.length !== 1) return
      const upper = e.key.toLocaleUpperCase(this.locale)
      if (this.alphabet.includes(upper) && !this.isGuessed(upper)) this.submitLetter(upper)
    }
  },
  mounted() {
    window.addEventListener('keydown', this.handleKeydown)
    this.tickTimer = setInterval(this.tick, 1000)
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeydown)
    clearInterval(this.tickTimer)
  }
}
</script>
