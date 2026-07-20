<template>
  <div class="max-w-3xl mx-auto">
    <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
      <!-- Header -->
      <div class="bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 p-6 sm:p-8 text-center">
        <p class="text-5xl mb-2">🪢</p>
        <h1 class="text-2xl sm:text-3xl font-bold text-white mb-1">Adam Asmaca</h1>
        <p class="text-white/80 text-sm">Kelimeyi harf harf tahmin et, adamı asılmaktan kurtar!</p>
      </div>

      <div class="p-6 sm:p-8">
        <!-- Dil seçimi + istatistik + yeni kelime -->
        <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
          <div class="flex items-center gap-2 bg-gray-100 rounded-xl p-1">
            <button
                v-for="opt in languageOptions"
                :key="opt.value"
                @click="language = opt.value"
                :class="['px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                         language === opt.value ? 'bg-indigo-600 text-white shadow' : 'text-gray-600 hover:bg-gray-200']">
              {{ opt.flag }} {{ opt.label }}
            </button>
          </div>

          <div class="flex items-center gap-4 text-sm">
            <span class="text-green-600 font-semibold">✓ {{ wins }} kazandı</span>
            <span class="text-red-500 font-semibold">✗ {{ losses }} kaybetti</span>
            <button @click="newWord"
                    class="px-4 py-2 bg-indigo-50 text-indigo-700 rounded-lg hover:bg-indigo-100 transition-colors font-medium">
              🔄 Yeni Kelime
            </button>
          </div>
        </div>

        <div class="grid sm:grid-cols-[auto_1fr] gap-6 items-start">
          <!-- Adam çizimi -->
          <div class="flex flex-col items-center">
            <svg viewBox="0 0 200 220" class="w-44 h-48 text-gray-700">
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
            <p class="text-sm font-medium" :class="remainingGuesses <= 2 ? 'text-red-600' : 'text-gray-500'">
              Kalan hak: {{ remainingGuesses }} / {{ maxWrong }}
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

            <div class="flex flex-wrap justify-center gap-1.5">
              <button
                  v-for="letter in alphabet"
                  :key="letter"
                  :disabled="isGuessed(letter) || isGameOver"
                  @click="guessLetter(letter)"
                  :class="['w-9 h-9 sm:w-10 sm:h-10 rounded-lg text-sm font-semibold transition-colors border',
                           letterButtonClass(letter)]">
                {{ letter }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Kazandı / Kaybetti overlay -->
      <div v-if="isGameOver" class="border-t border-gray-200 bg-gray-50 p-6 sm:p-8 text-center">
        <p class="text-5xl mb-3">{{ isWinner ? '🎉' : '💀' }}</p>
        <h2 class="text-xl font-bold mb-1" :class="isWinner ? 'text-green-600' : 'text-red-600'">
          {{ isWinner ? 'Tebrikler, bildin!' : 'Kaybettin!' }}
        </h2>
        <p class="text-gray-600 mb-4">
          Kelime: <span class="font-bold text-gray-900">{{ revealedWord }}</span>
        </p>
        <button @click="newWord"
                class="px-6 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-medium">
          Yeni Kelime Oyna →
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { randomHangmanWord } from '../../data/hangmanWords.js'
import { getHangmanWords } from '../../api/HangmanApi.js'

const TR_ALPHABET = ['A', 'B', 'C', 'Ç', 'D', 'E', 'F', 'G', 'Ğ', 'H', 'I', 'İ', 'J', 'K', 'L', 'M',
  'N', 'O', 'Ö', 'P', 'R', 'S', 'Ş', 'T', 'U', 'Ü', 'V', 'Y', 'Z']
const EN_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

const MAX_WRONG = 6

export default {
  name: 'HangmanGame',
  data: () => ({
    language: 'tr',
    targetWord: '',
    guessedLetters: [],
    wrongCount: 0,
    wins: 0,
    losses: 0,
    maxWrong: MAX_WRONG,
    languageOptions: [
      { value: 'tr', label: 'Türkçe', flag: '🇹🇷' },
      { value: 'en', label: 'English', flag: '🇬🇧' }
    ],
    extraWords: []
  }),
  computed: {
    locale() {
      return this.language === 'tr' ? 'tr-TR' : 'en-US'
    },
    alphabet() {
      return this.language === 'tr' ? TR_ALPHABET : EN_ALPHABET
    },
    displayWord() {
      return this.targetWord
          .split('')
          .map(ch => this.isGuessed(ch) ? ch.toLocaleUpperCase(this.locale) : '_')
          .join(' ')
    },
    revealedWord() {
      return this.targetWord.toLocaleUpperCase(this.locale)
    },
    wrongLetters() {
      return this.guessedLetters
          .filter(l => !this.targetWord.includes(l))
          .map(l => l.toLocaleUpperCase(this.locale))
    },
    remainingGuesses() {
      return Math.max(0, this.maxWrong - this.wrongCount)
    },
    isWinner() {
      return this.targetWord.length > 0 &&
          this.targetWord.split('').every(ch => this.isGuessed(ch))
    },
    isLoser() {
      return this.wrongCount >= this.maxWrong
    },
    isGameOver() {
      return this.isWinner || this.isLoser
    }
  },
  watch: {
    async language() {
      await this.loadWords()
      this.newWord()
    },
    isGameOver(over) {
      if (!over) return
      if (this.isWinner) this.wins++
      else this.losses++
    }
  },
  methods: {
    isGuessed(letter) {
      const lower = letter.toLocaleLowerCase(this.locale)
      return this.guessedLetters.includes(lower)
    },
    letterButtonClass(letter) {
      if (!this.isGuessed(letter)) {
        return 'bg-white border-gray-300 text-gray-700 hover:bg-indigo-50 hover:border-indigo-300'
      }
      const lower = letter.toLocaleLowerCase(this.locale)
      return this.targetWord.includes(lower)
          ? 'bg-green-100 border-green-300 text-green-700'
          : 'bg-red-100 border-red-300 text-red-500'
    },
    guessLetter(letter) {
      if (this.isGameOver) return
      const lower = letter.toLocaleLowerCase(this.locale)
      if (this.guessedLetters.includes(lower)) return
      this.guessedLetters.push(lower)
      if (!this.targetWord.includes(lower)) {
        this.wrongCount++
      }
    },
    newWord() {
      this.targetWord = randomHangmanWord(this.language, this.targetWord, this.extraWords)
      this.guessedLetters = []
      this.wrongCount = 0
    },
    handleKeydown(e) {
      if (e.key.length !== 1) return
      const upper = e.key.toLocaleUpperCase(this.locale)
      if (this.alphabet.includes(upper)) {
        this.guessLetter(upper)
      }
    },

    async loadWords() {
      try {
        const words = await getHangmanWords(this.language)
        this.extraWords = words.map(w => w.word)
      } catch (e) {
        console.error('Kelime havuzu yüklenemedi:', e)
      }
    }
  },
  async mounted() {
    await this.loadWords()
    this.newWord()
    window.addEventListener('keydown', this.handleKeydown)
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.handleKeydown)
  }
}
</script>
