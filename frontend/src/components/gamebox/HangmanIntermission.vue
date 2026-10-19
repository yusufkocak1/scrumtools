<template>
  <!-- ─── Tur arası: kelime ilanı ───
       Tur bittiğinde oyun burada durur — kelime büyük harflerle ilan edilir. Otomatik geçiş
       açıksa kısa bir geri sayımdan sonra, değilse moderatörün butonuyla sonraki kelimeye geçilir.
       Sunucu bu sürede tahmin kabul etmez. Sıralı, takım ve yarış modu ortak kullanır. -->
  <transition name="fade">
    <div v-if="info"
         class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/70 backdrop-blur-sm">
      <div class="w-full max-w-2xl bg-white rounded-3xl shadow-2xl overflow-hidden max-h-full overflow-y-auto">
        <div :class="['px-6 py-6 text-center',
                      info.solved
                        ? 'bg-gradient-to-r from-green-500 to-emerald-600'
                        : 'bg-gradient-to-r from-rose-500 to-red-600']">
          <p class="text-5xl mb-2">{{ info.solved ? (isRace ? '🏁' : '🎉') : '💀' }}</p>
          <p class="text-white text-xl sm:text-2xl font-bold">
            <template v-if="!info.solved">Kimse bilemedi!</template>
            <template v-else-if="isRace">{{ info.solvedByName }} ilk bitirdi!</template>
            <template v-else>{{ info.solvedByName }} bildi!</template>
          </p>
          <p class="text-white/80 text-xs mt-1">
            Kelime {{ info.roundOrder + 1 }} / {{ session.totalRounds }}
          </p>
        </div>

        <div class="p-6 sm:p-8 text-center">
          <p class="text-xs font-semibold uppercase tracking-widest text-gray-400 mb-3">Kelime</p>
          <div class="flex flex-wrap justify-center gap-1.5 sm:gap-2">
            <span v-for="(ch, i) in revealedLetters" :key="i"
                  class="w-10 h-12 sm:w-12 sm:h-14 flex items-center justify-center rounded-xl
                         bg-indigo-50 border-2 border-indigo-200 text-indigo-700
                         font-mono font-bold text-2xl sm:text-3xl">
              {{ ch }}
            </span>
          </div>
          <p v-if="categoryText" class="mt-4 text-sm text-gray-500">
            🏷️ {{ categoryText }}
          </p>
          <p v-if="!isRace && info.solved && info.score > 0" class="mt-3 text-sm text-gray-600">
            <strong class="text-green-600">+{{ info.score }}</strong> puan
          </p>

          <!-- Yarışta tek "bilen" yok: kim kaçıncı bitirdi, kim yetişemedi. -->
          <div v-if="isRace && raceFinishers.length" class="mt-5 max-w-sm mx-auto text-left space-y-1.5">
            <div v-for="r in raceFinishers" :key="r.email"
                 class="flex items-center gap-2 px-3 py-2 rounded-lg bg-gray-50 text-sm">
              <span class="w-6 text-center">{{ placeLabel(r) }}</span>
              <span class="flex-1 truncate text-gray-800">{{ r.displayName }}</span>
              <span v-if="r.status === 'SOLVED'" class="font-bold text-green-600">+{{ r.scoreEarned }}</span>
              <span v-else-if="r.status === 'FAILED'" class="text-xs text-red-500">asıldı</span>
              <span v-else class="text-xs text-gray-400">yetişemedi</span>
            </div>
          </div>
        </div>

        <div class="border-t border-gray-200 bg-gray-50 px-6 py-5 text-center">
          <!-- Otomatik geçiş geri sayımı — moderatör dalgınken herkes beklemesin. -->
          <div v-if="counting" class="mb-4">
            <p class="text-sm text-gray-600 mb-2">
              {{ isLastRound ? '🏁 Sonuçlar' : '⏭️ Sonraki kelime' }}
              <strong class="text-indigo-600">{{ secondsLeft }} sn</strong> sonra
            </p>
            <div class="h-1.5 max-w-xs mx-auto bg-gray-200 rounded-full overflow-hidden">
              <div class="h-full bg-indigo-500 transition-all duration-1000 ease-linear"
                   :style="{ width: progress + '%' }"></div>
            </div>
          </div>
          <button v-if="isHost" @click="$emit('next-round')"
                  class="w-full sm:w-auto px-8 py-3.5 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700
                         transition-colors font-semibold text-lg shadow-lg shadow-indigo-600/20">
            {{ isLastRound ? '🏁 Sonuçları Göster' : '⏭️ Sonraki Kelime' }}
          </button>
          <p v-else-if="!counting" class="text-sm text-gray-500 flex items-center justify-center gap-2">
            <span class="w-2 h-2 rounded-full bg-indigo-500 animate-pulse"></span>
            {{ isLastRound
                ? 'Moderatör sonuçları açıyor…'
                : 'Moderatörün sonraki kelimeye geçmesi bekleniyor…' }}
          </p>
          <button v-if="isHost && counting" @click="$emit('pause-intermission')"
                  class="block mx-auto mt-3 text-xs text-gray-500 hover:text-indigo-700 transition-colors">
            ⏸️ Geri sayımı durdur — biraz konuşalım
          </button>
          <button v-if="isHost && !isLastRound" @click="$emit('finish')"
                  class="mt-3 text-xs text-gray-400 hover:text-red-600 transition-colors">
            ⏹️ Oyunu burada bitir
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script>
import { hangmanCategoryLabel } from '../../data/hangmanWords.js'

/** Sunucudaki HangmanSessionService.INTERMISSION_SECONDS — yalnızca ilerleme çubuğu için. */
const INTERMISSION_SECONDS = 8

export default {
  name: 'HangmanIntermission',
  props: {
    session: Object,
    isHost: Boolean
  },
  emits: ['next-round', 'pause-intermission', 'finish'],
  data: () => ({
    /** Ara ekranının kendiliğinden kapanmasına kalan saniye; null = geri sayım yok. */
    secondsLeft: null,
    tickTimer: null
  }),
  computed: {
    isRace() {
      return this.session?.mode === 'RACE'
    },
    /** Biten turun bilgileri; tur arasında değilsek null (ekran gizli). */
    info() {
      if (!this.session?.awaitingNextRound) return null
      const r = this.session.lastFinishedRound
      if (!r) return null
      return {
        solved: r.status === 'SOLVED',
        solvedByName: r.solvedByName,
        solvedByEmail: r.solvedByEmail,
        revealedWord: r.revealedWord,
        roundOrder: r.roundOrder,
        categoryLabel: r.categoryLabel,
        category: r.category,
        score: this.solvedScore(r)
      }
    },
    locale() {
      return this.session?.language === 'tr' ? 'tr-TR' : 'en-US'
    },
    revealedLetters() {
      return (this.info?.revealedWord || '').toLocaleUpperCase(this.locale).split('')
    },
    categoryText() {
      const r = this.info
      if (!r?.category && !r?.categoryLabel) return null
      return hangmanCategoryLabel(r.category, this.session?.language) || r.categoryLabel
    },
    /** Son kelimeden sonra sıradaki adım sonuç ekranıdır. */
    isLastRound() {
      return (this.session?.currentRoundIndex ?? 0) + 1 >= (this.session?.totalRounds ?? 0)
    },
    counting() {
      return !!this.info && this.secondsLeft !== null
    },
    progress() {
      return Math.max(0, Math.min(100, ((this.secondsLeft ?? 0) / INTERMISSION_SECONDS) * 100))
    },
    raceFinishers() {
      return this.session?.raceProgress || []
    }
  },
  watch: {
    // Ebeveyn her WS mesajında yeni bir oturum nesnesi verir; sunucunun kalan süresi esastır.
    session: {
      handler() {
        this.secondsLeft = this.session?.intermissionSecondsLeft ?? null
      },
      immediate: true
    }
  },
  methods: {
    /**
     * Turu bitiren tahminin kazandırdığı puan — canlı akıştaki kaydından okunur.
     * Akış son birkaç hamleyle sınırlı olduğu için bulunamazsa 0 döner.
     */
    solvedScore(round) {
      if (round?.status !== 'SOLVED') return 0
      const winning = (this.session?.recentGuesses || [])
          .find(g => g.correct && g.userEmail === round.solvedByEmail)
      return winning?.scoreDelta || 0
    },
    placeLabel(r) {
      if (r.finishOrder === 1) return '🥇'
      if (r.finishOrder === 2) return '🥈'
      if (r.finishOrder === 3) return '🥉'
      if (r.finishOrder) return r.finishOrder
      return '—'
    },
    tick() {
      // Sıfırda durur; geçişi sunucu yapar ve yeni durum WS ile gelir.
      if (this.secondsLeft > 0) this.secondsLeft--
    }
  },
  mounted() {
    this.tickTimer = setInterval(this.tick, 1000)
  },
  beforeUnmount() {
    clearInterval(this.tickTimer)
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
