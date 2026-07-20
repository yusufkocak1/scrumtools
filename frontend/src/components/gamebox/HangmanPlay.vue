<template>
  <div class="max-w-6xl mx-auto">
    <!-- Tur bitti bildirimi -->
    <transition name="fade">
      <div v-if="roundBanner"
           :class="['mb-4 rounded-2xl p-4 flex items-center gap-3 border',
                    roundBanner.solved
                      ? 'bg-green-50 border-green-200'
                      : 'bg-red-50 border-red-200']">
        <span class="text-3xl">{{ roundBanner.solved ? '🎉' : '💀' }}</span>
        <div>
          <p :class="['font-bold', roundBanner.solved ? 'text-green-700' : 'text-red-700']">
            {{ roundBanner.solved ? `${roundBanner.solvedByName} bildi!` : 'Kimse bilemedi!' }}
          </p>
          <p class="text-sm text-gray-600">
            Kelime: <strong class="font-mono tracking-wider">{{ upper(roundBanner.revealedWord) }}</strong>
          </p>
        </div>
      </div>
    </transition>

    <div class="grid lg:grid-cols-[1fr_320px] gap-6 items-start">
      <!-- ─── Oyun alanı ─── -->
      <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
        <!-- Sıra göstergesi -->
        <div :class="['p-4 sm:p-5 text-center transition-colors',
                      isMyTurn
                        ? 'bg-gradient-to-r from-green-500 to-emerald-600'
                        : 'bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500']">
          <p v-if="isMyTurn" class="text-white font-bold text-lg">🎯 Sıra sende!</p>
          <p v-else-if="isSpectator" class="text-white font-medium">
            👁️ İzliyorsun — sıra <strong>{{ session.currentTurnName }}</strong>'de
          </p>
          <p v-else class="text-white font-medium">
            ⏳ Sıra <strong>{{ session.currentTurnName }}</strong>'de
          </p>
          <p class="text-white/80 text-xs mt-1">
            Kelime {{ session.currentRoundIndex + 1 }} / {{ session.totalRounds }}
          </p>
        </div>

        <div class="p-6 sm:p-8">
          <div class="grid sm:grid-cols-[auto_1fr] gap-6 items-start">
            <!-- Darağacı -->
            <div class="flex flex-col items-center">
              <svg viewBox="0 0 200 220" class="w-40 h-44 text-gray-700">
                <line x1="10" y1="210" x2="130" y2="210" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>
                <line x1="40" y1="210" x2="40" y2="20" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>
                <line x1="40" y1="20" x2="120" y2="20" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>
                <line x1="120" y1="20" x2="120" y2="45" stroke="currentColor" stroke-width="4" stroke-linecap="round"/>

                <circle v-if="wrongCount >= 1" cx="120" cy="60" r="15" stroke="#ef4444" stroke-width="4" fill="none"/>
                <line v-if="wrongCount >= 2" x1="120" y1="75" x2="120" y2="130" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="wrongCount >= 3" x1="120" y1="90" x2="100" y2="110" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="wrongCount >= 4" x1="120" y1="90" x2="140" y2="110" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="wrongCount >= 5" x1="120" y1="130" x2="100" y2="160" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
                <line v-if="wrongCount >= 6" x1="120" y1="130" x2="140" y2="160" stroke="#ef4444" stroke-width="4" stroke-linecap="round"/>
              </svg>
              <p class="text-sm font-medium" :class="remaining <= 2 ? 'text-red-600' : 'text-gray-500'">
                Kalan hak: {{ remaining }} / {{ maxWrong }}
              </p>
            </div>

            <!-- Kelime + klavye -->
            <div>
              <div class="text-center mb-6">
                <p class="text-3xl sm:text-4xl font-mono font-bold tracking-widest text-gray-900 break-all">
                  {{ displayWord }}
                </p>
                <p v-if="wrongLetters.length" class="mt-3 text-sm text-gray-500">
                  Yanlış harfler:
                  <span class="text-red-500 font-semibold">{{ wrongLetters.join(' ') }}</span>
                </p>
              </div>

              <div class="flex flex-wrap justify-center gap-1.5 mb-6">
                <button
                    v-for="letter in alphabet"
                    :key="letter"
                    :disabled="!canGuess || isGuessed(letter) || busy"
                    @click="submitLetter(letter)"
                    :class="['w-9 h-9 sm:w-10 sm:h-10 rounded-lg text-sm font-semibold transition-colors border',
                             letterClass(letter)]">
                  {{ letter }}
                </button>
              </div>

              <!-- Kelime tahmini -->
              <div class="border-t border-gray-200 pt-5">
                <p class="text-sm font-semibold text-gray-700 mb-2">
                  💡 Kelimeyi biliyor musun?
                </p>
                <p class="text-xs text-gray-500 mb-3">
                  Doğru bilirsen <strong class="text-indigo-600">+50</strong> puan.
                  Yanlış bilirsen adam asılmaz, sadece sıranı kaybedersin.
                </p>
                <div class="flex gap-2">
                  <input
                      v-model="wordGuess"
                      :disabled="!canGuess || busy"
                      @keyup.enter="submitWord"
                      type="text"
                      :placeholder="canGuess ? 'Tüm kelimeyi yaz...' : 'Sıranı bekle'"
                      class="flex-1 px-4 py-2.5 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none disabled:bg-gray-50 disabled:text-gray-400" />
                  <button
                      @click="submitWord"
                      :disabled="!canGuess || busy || !wordGuess.trim()"
                      class="px-5 py-2.5 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap">
                    Tahmin Et
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Moderatör kontrolleri -->
        <div v-if="isHost" class="border-t border-gray-200 bg-gray-50 px-6 py-4 flex flex-wrap gap-3 justify-between items-center">
          <p class="text-xs text-gray-500">Moderatör kontrolleri</p>
          <div class="flex gap-2">
            <button @click="$emit('skip')"
                    class="px-4 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors text-sm font-medium">
              ⏭️ Sırayı Devret
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
        <!-- Puan tablosu -->
        <div class="bg-white rounded-2xl shadow-lg border border-gray-200 overflow-hidden">
          <div class="px-5 py-3 border-b border-gray-200 bg-gray-50">
            <h3 class="font-semibold text-gray-800">🏆 Puan Sıralaması</h3>
          </div>
          <div class="divide-y divide-gray-100">
            <div v-for="entry in leaderboard" :key="entry.email"
                 :class="['flex items-center gap-3 px-5 py-3 transition-colors',
                          entry.email === session.currentTurnEmail ? 'bg-green-50' : '']">
              <span :class="['w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0',
                             rankClass(entry.rank)]">
                {{ entry.rank }}
              </span>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-gray-800 truncate">
                  {{ entry.displayName }}
                  <span v-if="entry.email === myEmail" class="text-xs text-indigo-600">(sen)</span>
                </p>
                <p class="text-xs text-gray-400">
                  {{ entry.wordsSolved }} kelime · {{ entry.correctLetterCount }}✓ {{ entry.wrongLetterCount }}✗
                </p>
              </div>
              <span class="font-bold text-indigo-600 shrink-0">{{ entry.totalScore }}</span>
            </div>
          </div>
        </div>

        <!-- Canlı akış -->
        <div class="bg-white rounded-2xl shadow-lg border border-gray-200 overflow-hidden">
          <div class="px-5 py-3 border-b border-gray-200 bg-gray-50">
            <h3 class="font-semibold text-gray-800">📜 Son Hamleler</h3>
          </div>
          <div v-if="!recentGuesses.length" class="px-5 py-6 text-center text-sm text-gray-400">
            Henüz hamle yok
          </div>
          <div v-else class="divide-y divide-gray-100 max-h-72 overflow-y-auto">
            <div v-for="(g, i) in recentGuesses" :key="i"
                 class="px-5 py-2.5 flex items-center gap-2 text-sm">
              <span class="text-gray-800 font-medium truncate flex-1">{{ g.displayName }}</span>
              <span :class="['px-2 py-0.5 rounded font-mono text-xs font-semibold',
                             g.correct ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600']">
                {{ upper(g.value) }}
              </span>
              <span :class="['text-xs font-bold w-10 text-right',
                             g.scoreDelta > 0 ? 'text-green-600' : g.scoreDelta < 0 ? 'text-red-500' : 'text-gray-400']">
                {{ g.scoreDelta > 0 ? '+' : '' }}{{ g.scoreDelta }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { guessHangmanLetter, guessHangmanWord } from '../../api/HangmanApi.js'

const TR_ALPHABET = ['A', 'B', 'C', 'Ç', 'D', 'E', 'F', 'G', 'Ğ', 'H', 'I', 'İ', 'J', 'K', 'L', 'M',
  'N', 'O', 'Ö', 'P', 'R', 'S', 'Ş', 'T', 'U', 'Ü', 'V', 'Y', 'Z']
const EN_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

export default {
  name: 'HangmanPlay',
  props: {
    session: Object,
    isHost: Boolean,
    teamId: String
  },
  emits: ['updated', 'skip', 'finish'],
  data: () => ({
    wordGuess: '',
    busy: false,
    roundBanner: null,
    bannerTimer: null
  }),
  computed: {
    round() {
      return this.session?.round || null
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
    isMyTurn() {
      return this.session?.currentTurnEmail === this.myEmail
    },
    isSpectator() {
      return (this.session?.participants || [])
          .some(p => p.email === this.myEmail && p.spectator)
    },
    canGuess() {
      return this.isMyTurn && !this.isSpectator
    },
    wrongCount() {
      return this.round?.wrongCount || 0
    },
    maxWrong() {
      return this.round?.maxWrong || 6
    },
    remaining() {
      return Math.max(0, this.maxWrong - this.wrongCount)
    },
    displayWord() {
      if (!this.round) return ''
      // maskedWord: bilinen harfler açık, bilinmeyenler '_'
      return this.round.maskedWord
          .split('')
          .map(ch => ch === '_' ? '_' : ch.toLocaleUpperCase(this.locale))
          .join(' ')
    },
    wrongLetters() {
      return (this.round?.wrongLetters || []).map(l => l.toLocaleUpperCase(this.locale))
    },
    guessedLetters() {
      return this.round?.guessedLetters || []
    },
    leaderboard() {
      return this.session?.leaderboard || []
    },
    recentGuesses() {
      return this.session?.recentGuesses || []
    }
  },
  watch: {
    // Tur bittiğinde kelimeyi kısa süre göster — sunucu hemen sonraki tura geçtiği için.
    'session.lastFinishedRound.id': {
      handler(id, oldId) {
        if (!id || id === oldId) return
        const r = this.session.lastFinishedRound
        this.roundBanner = {
          solved: r.status === 'SOLVED',
          solvedByName: r.solvedByName,
          revealedWord: r.revealedWord
        }
        clearTimeout(this.bannerTimer)
        this.bannerTimer = setTimeout(() => { this.roundBanner = null }, 6000)
      }
    },
    // Sıra bize geçtiğinde eski tahmin metnini temizle.
    'session.currentTurnEmail'() {
      this.wordGuess = ''
    }
  },
  methods: {
    upper(v) {
      return (v || '').toLocaleUpperCase(this.locale)
    },
    isGuessed(letter) {
      return this.guessedLetters.includes(letter.toLocaleLowerCase(this.locale))
    },
    letterClass(letter) {
      if (!this.isGuessed(letter)) {
        return this.canGuess
            ? 'bg-white border-gray-300 text-gray-700 hover:bg-indigo-50 hover:border-indigo-300'
            : 'bg-white border-gray-200 text-gray-300 cursor-not-allowed'
      }
      // round.wrongLetters sunucudan zaten küçük harf geliyor.
      const lower = letter.toLocaleLowerCase(this.locale)
      return (this.round?.wrongLetters || []).includes(lower)
          ? 'bg-red-100 border-red-300 text-red-500'
          : 'bg-green-100 border-green-300 text-green-700'
    },
    rankClass(rank) {
      if (rank === 1) return 'bg-yellow-100 text-yellow-700'
      if (rank === 2) return 'bg-gray-200 text-gray-700'
      if (rank === 3) return 'bg-orange-100 text-orange-700'
      return 'bg-gray-100 text-gray-500'
    },
    async submitLetter(letter) {
      if (!this.canGuess || this.busy) return
      this.busy = true
      try {
        const updated = await guessHangmanLetter(this.teamId, this.session.id,
            letter.toLocaleLowerCase(this.locale))
        this.$emit('updated', updated)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
      this.busy = false
    },
    async submitWord() {
      const guess = this.wordGuess.trim()
      if (!this.canGuess || this.busy || !guess) return
      this.busy = true
      try {
        const updated = await guessHangmanWord(this.teamId, this.session.id,
            guess.toLocaleLowerCase(this.locale))
        this.wordGuess = ''
        this.$emit('updated', updated)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
      this.busy = false
    },
    handleKeydown(e) {
      if (!this.canGuess || this.busy) return
      // Kelime tahmini alanına yazarken klavye kısayolu devreye girmesin.
      if (e.target && ['INPUT', 'TEXTAREA'].includes(e.target.tagName)) return
      if (e.key.length !== 1) return
      const upper = e.key.toLocaleUpperCase(this.locale)
      if (this.alphabet.includes(upper) && !this.isGuessed(upper)) {
        this.submitLetter(upper)
      }
    }
  },
  mounted() {
    window.addEventListener('keydown', this.handleKeydown)
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeydown)
    clearTimeout(this.bannerTimer)
  }
}
</script>

<style scoped>
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
