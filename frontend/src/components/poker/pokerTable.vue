<template>
  <div class="w-full bg-white border border-gray-200 rounded-2xl shadow-sm relative overflow-hidden">
    <!-- Uzlaşma konfetisi — yalnızca tam uzlaşmada patlar -->
    <div v-if="celebrating" class="pointer-events-none absolute inset-0 z-20 overflow-hidden">
      <span v-for="piece in confetti" :key="piece.id" class="confetti" :style="piece.style"></span>
    </div>

    <div class="relative z-10 p-4 sm:p-6">
      <!-- Üst sıra oyuncular -->
      <div v-if="topSeats.length" class="flex flex-wrap justify-center gap-3 sm:gap-4 mb-5">
        <PokerSeat
          v-for="(seat, i) in topSeats"
          :key="seat.email"
          :name="seat.displayName"
          :email="seat.email"
          :vote="seat.vote"
          :revealed="isVotesVisible"
          :is-self="seat.email === currentUserEmail"
          :extreme="seat.extreme"
          :index="i"
        />
      </div>

      <!-- Masa yüzeyi: turun tek kontrol merkezi -->
      <div class="poker-felt rounded-[2rem] px-4 py-6 sm:px-8 sm:py-7 text-center shadow-inner">
        <p class="text-sm sm:text-base font-medium text-emerald-50/90 mb-4 min-h-[1.5rem]">
          {{ statusMessage }}
        </p>

        <!-- Oy toplama ilerlemesi — kartlar kapalıyken -->
        <div v-if="!isVotesVisible" class="max-w-xs mx-auto mb-5">
          <div class="h-2 rounded-full bg-black/25 overflow-hidden">
            <div
              class="h-full rounded-full bg-gradient-to-r from-emerald-300 to-emerald-400 transition-all duration-500 ease-out"
              :style="{ width: progressPercent + '%' }"
            ></div>
          </div>
          <p class="text-[11px] text-emerald-100/70 mt-1.5 font-medium tracking-wide">
            {{ votedCount }} / {{ seats.length }} oy
          </p>
        </div>

        <!-- TEK birincil aksiyon: aç / yeni tur -->
        <button
          type="button"
          @click="newRound"
          :disabled="primaryDisabled"
          :title="primaryDisabled ? 'Önce en az bir kişi oy vermeli' : null"
          class="inline-flex items-center gap-2.5 rounded-full py-3 px-7 sm:px-9 font-semibold text-sm shadow-lg transition-all duration-300 select-none disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100"
          :class="[
            isVotesVisible
              ? 'bg-white text-emerald-800 hover:bg-emerald-50 shadow-black/20'
              : 'bg-gradient-to-r from-amber-400 to-amber-500 text-amber-950 hover:from-amber-300 hover:to-amber-400 shadow-amber-900/30',
            !primaryDisabled ? 'hover:scale-105 active:scale-95' : '',
            everyoneReady && !isVotesVisible ? 'ready-pulse' : ''
          ]"
        >
          <svg v-if="!isVotesVisible" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
          <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          {{ isVotesVisible ? 'Yeni Tur' : 'Oyları Aç' }}
        </button>

        <!-- Tur sonucu — yalnızca kartlar açıkken -->
        <div v-if="isVotesVisible" class="mt-6">
          <div class="flex flex-wrap justify-center gap-2.5 sm:gap-3">
            <div class="stat-chip">
              <span class="stat-chip__label">Ortalama</span>
              <span class="stat-chip__value">{{ average !== null ? average.toFixed(1) : '—' }}</span>
            </div>
            <div class="stat-chip">
              <span class="stat-chip__label">Aralık</span>
              <span class="stat-chip__value">{{ range }}</span>
            </div>
            <div class="stat-chip">
              <span class="stat-chip__label">Uzlaşma</span>
              <span class="stat-chip__value">{{ agreementPercent !== null ? agreementPercent + '%' : '—' }}</span>
            </div>
          </div>

          <!-- Oy dağılımı — hangi değerde kaç kişi toplandı -->
          <div v-if="distribution.length" class="flex flex-wrap justify-center gap-2 mt-4">
            <span
              v-for="bucket in distribution"
              :key="bucket.value"
              class="inline-flex items-center gap-1.5 rounded-full bg-black/20 px-3 py-1 text-[11px] font-semibold text-emerald-50 ring-1 ring-white/10"
            >
              <span class="font-black text-sm">{{ bucket.value }}</span>
              <span class="text-emerald-100/70">×{{ bucket.count }}</span>
            </span>
          </div>
        </div>
      </div>

      <!-- Alt sıra oyuncular -->
      <div v-if="bottomSeats.length" class="flex flex-wrap justify-center gap-3 sm:gap-4 mt-5">
        <PokerSeat
          v-for="(seat, i) in bottomSeats"
          :key="seat.email"
          :name="seat.displayName"
          :email="seat.email"
          :vote="seat.vote"
          :revealed="isVotesVisible"
          :is-self="seat.email === currentUserEmail"
          :extreme="seat.extreme"
          :index="topSeats.length + i"
        />
      </div>

      <!-- Kimse yoksa -->
      <p v-if="!seats.length" class="text-center text-sm text-gray-400 mt-5">
        Masada henüz kimse yok — takım arkadaşlarını davet et.
      </p>
    </div>
  </div>
</template>

<script>
import PokerSeat from "./PokerSeat.vue";

export default {
  name: "pokerTable",
  components: { PokerSeat },
  props: {
    votes: { type: Array, default: () => [] },
    members: { type: Object, default: () => ({}) },
    isVotesVisible: Boolean,
    currentUserEmail: { type: String, default: '' }
  },
  emits: ['newRound'],
  data() {
    return {
      celebrating: false,
      confetti: [],
      celebrationTimer: null
    }
  },
  computed: {
    // Oy listesi WS ile sürekli yenileniyor; koltuklar zıplamasın diye sabit sıralama
    seats() {
      const numeric = this.numericVotes
      const min = numeric.length ? Math.min(...numeric) : null
      const max = numeric.length ? Math.max(...numeric) : null
      const hasSpread = min !== null && min !== max

      return [...this.votes]
        .map(vote => {
          const value = vote.vote
          const asNumber = this.toNumber(value)
          let extreme = null
          if (hasSpread && asNumber !== null) {
            if (asNumber === min) extreme = 'low'
            else if (asNumber === max) extreme = 'high'
          }
          return {
            email: vote.email,
            // displayName oy kaydıyla birlikte geliyor; takımdan çıkmış üyede de çalışsın
            displayName: vote.displayName || this.members?.[vote.email]?.displayName || vote.email,
            vote: value,
            extreme
          }
        })
        .sort((a, b) => a.displayName.localeCompare(b.displayName, 'tr'))
    },
    topSeats() {
      return this.seats.slice(0, Math.ceil(this.seats.length / 2))
    },
    bottomSeats() {
      return this.seats.slice(Math.ceil(this.seats.length / 2))
    },
    numericVotes() {
      return this.votes
        .map(v => this.toNumber(v.vote))
        .filter(n => n !== null)
    },
    votedCount() {
      return this.votes.filter(v => v.vote && v.vote !== '-').length
    },
    everyoneReady() {
      return this.seats.length > 0 && this.votedCount === this.seats.length
    },
    progressPercent() {
      if (!this.seats.length) return 0
      return Math.round((this.votedCount / this.seats.length) * 100)
    },
    average() {
      if (!this.numericVotes.length) return null
      return this.numericVotes.reduce((acc, cur) => acc + cur, 0) / this.numericVotes.length
    },
    range() {
      if (!this.numericVotes.length) return '—'
      const min = Math.min(...this.numericVotes)
      const max = Math.max(...this.numericVotes)
      return min === max ? `${min}` : `${min} – ${max}`
    },
    // Sayısal oylar içinde en çok tekrar eden değerin oranı
    agreementPercent() {
      if (!this.numericVotes.length) return null
      const counts = new Map()
      this.numericVotes.forEach(n => counts.set(n, (counts.get(n) || 0) + 1))
      const top = Math.max(...counts.values())
      return Math.round((top / this.numericVotes.length) * 100)
    },
    // Açılan tüm oylar (? dahil) değere göre gruplanır, çoktan aza sıralı
    distribution() {
      const counts = new Map()
      this.votes
        .filter(v => v.vote && v.vote !== '-')
        .forEach(v => counts.set(v.vote, (counts.get(v.vote) || 0) + 1))

      return [...counts.entries()]
        .map(([value, count]) => ({ value, count }))
        .sort((a, b) => {
          if (b.count !== a.count) return b.count - a.count
          const an = this.toNumber(a.value)
          const bn = this.toNumber(b.value)
          if (an === null) return 1
          if (bn === null) return -1
          return an - bn
        })
    },
    // Herkes aynı sayıyı verdi — kutlama koşulu
    isConsensus() {
      return this.numericVotes.length > 1 &&
        this.numericVotes.length === this.votedCount &&
        new Set(this.numericVotes).size === 1
    },
    primaryDisabled() {
      return !this.isVotesVisible && this.votedCount === 0
    },
    statusMessage() {
      if (this.isVotesVisible) {
        if (this.isConsensus) return `Tam uzlaşma! Herkes ${this.numericVotes[0]} dedi 🎉`
        if (this.votedCount === 0) return 'Kimse oy vermemiş — yeni tur başlatın.'
        return 'Kartlar açıldı. Şimdi sıra tartışmada.'
      }
      if (!this.seats.length) return 'Masa kuruldu, oyuncular bekleniyor…'
      if (this.votedCount === 0) return 'Deste dağıtıldı. İlk kartı kim sürecek?'
      if (this.everyoneReady) return 'Herkes kartını verdi — açılış zamanı!'
      const remaining = this.seats.length - this.votedCount
      return `${remaining} kişi hâlâ düşünüyor…`
    }
  },
  watch: {
    isConsensus: {
      handler(consensus) {
        if (consensus && this.isVotesVisible) this.celebrate()
      }
    },
    isVotesVisible(visible) {
      if (visible && this.isConsensus) this.celebrate()
      if (!visible) this.stopCelebration()
    }
  },
  beforeUnmount() {
    if (this.celebrationTimer) clearTimeout(this.celebrationTimer)
  },
  methods: {
    toNumber(value) {
      if (value === null || value === undefined) return null
      const trimmed = String(value).trim()
      if (trimmed === '' || trimmed === '-' || trimmed === '?') return null
      const parsed = parseFloat(trimmed)
      return isNaN(parsed) ? null : parsed
    },
    newRound() {
      this.$emit('newRound')
    },
    celebrate() {
      if (this.celebrating) return
      const colors = ['#34d399', '#fbbf24', '#60a5fa', '#f472b6', '#a78bfa', '#ffffff']
      this.confetti = Array.from({ length: 44 }, (_, id) => ({
        id,
        style: {
          left: `${Math.random() * 100}%`,
          backgroundColor: colors[id % colors.length],
          animationDelay: `${Math.random() * 0.5}s`,
          animationDuration: `${1.8 + Math.random() * 1.2}s`,
          transform: `rotate(${Math.random() * 360}deg)`
        }
      }))
      this.celebrating = true

      if (this.celebrationTimer) clearTimeout(this.celebrationTimer)
      this.celebrationTimer = setTimeout(() => this.stopCelebration(), 3200)
    },
    stopCelebration() {
      if (this.celebrationTimer) {
        clearTimeout(this.celebrationTimer)
        this.celebrationTimer = null
      }
      this.celebrating = false
      this.confetti = []
    }
  }
}
</script>

<style scoped>
/* Masa çuhası — kartların üstünde durduğu koyu yeşil yüzey */
.poker-felt {
  background-image:
    radial-gradient(ellipse at 50% 0%, rgb(16 185 129 / 0.35), transparent 65%),
    linear-gradient(160deg, #065f46, #064e3b 55%, #022c22);
  box-shadow: inset 0 2px 24px rgb(0 0 0 / 0.35), inset 0 0 0 1px rgb(255 255 255 / 0.08);
}

.stat-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.1rem;
  min-width: 5.5rem;
  padding: 0.5rem 0.9rem;
  border-radius: 0.9rem;
  background-color: rgb(0 0 0 / 0.22);
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 0.1);
}

.stat-chip__label {
  font-size: 0.625rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgb(209 250 229 / 0.65);
  font-weight: 600;
}

.stat-chip__value {
  font-size: 1.125rem;
  font-weight: 900;
  color: #ffffff;
  line-height: 1.2;
}

/* Herkes oy verdiğinde "aç" butonu dikkat çeker */
.ready-pulse {
  animation: ready-pulse 1.8s ease-in-out infinite;
}

@keyframes ready-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgb(251 191 36 / 0.55); }
  50% { box-shadow: 0 0 0 12px rgb(251 191 36 / 0); }
}

.confetti {
  position: absolute;
  top: -12px;
  width: 8px;
  height: 14px;
  border-radius: 2px;
  opacity: 0;
  animation-name: confetti-fall;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
}

@keyframes confetti-fall {
  0% { opacity: 1; transform: translateY(0) rotate(0deg); }
  85% { opacity: 1; }
  100% { opacity: 0; transform: translateY(640px) rotate(540deg); }
}

@media (prefers-reduced-motion: reduce) {
  .ready-pulse {
    animation: none;
  }
  .confetti {
    display: none;
  }
}
</style>
