<template>
  <div class="flex flex-col items-center gap-2 w-16 sm:w-20">
    <!-- Kart — açılışta 3D olarak döner -->
    <div class="seat-card" :class="{ 'is-revealed': revealed }">
      <div class="seat-card__inner" :style="{ transitionDelay: `${index * 90}ms` }">
        <!-- Arka yüz: oy verildi mi bilgisini taşır, değeri saklar -->
        <div class="seat-card__face seat-card__face--back" :class="hasVoted ? 'is-ready' : 'is-waiting'">
          <span v-if="hasVoted" class="text-white/90 text-lg">✓</span>
          <span v-else class="seat-card__dots">
            <i></i><i></i><i></i>
          </span>
        </div>

        <!-- Ön yüz: gerçek oy -->
        <div class="seat-card__face seat-card__face--front" :class="frontClass">
          <span v-if="hasVoted" class="font-black text-xl sm:text-2xl leading-none">{{ vote }}</span>
          <span v-else class="text-gray-300 font-black text-xl leading-none">—</span>
        </div>
      </div>

      <!-- Uç değer rozetleri — tartışmayı başlatacak kişileri işaretler -->
      <span
        v-if="revealed && extreme"
        class="absolute -top-2 -right-1.5 px-1.5 py-0.5 rounded-full text-[9px] font-bold shadow-sm whitespace-nowrap"
        :class="extreme === 'low' ? 'bg-sky-100 text-sky-700 ring-1 ring-sky-200' : 'bg-rose-100 text-rose-700 ring-1 ring-rose-200'"
      >
        {{ extreme === 'low' ? 'EN AZ' : 'EN ÇOK' }}
      </span>
    </div>

    <!-- Oyuncu -->
    <div class="flex flex-col items-center gap-1 min-w-0 w-full">
      <div
        class="w-7 h-7 rounded-full flex items-center justify-center text-[10px] font-bold text-white shadow-sm ring-2 ring-white"
        :class="avatarColor"
        :title="name"
      >
        {{ initials }}
      </div>
      <span
        class="text-[11px] font-semibold text-center truncate w-full"
        :class="isSelf ? 'text-emerald-700' : 'text-gray-600'"
        :title="name"
      >
        {{ isSelf ? 'Sen' : shortName }}
      </span>
    </div>
  </div>
</template>

<script>
// Ada göre sabit bir avatar rengi — her turda aynı kişi aynı renkte kalır
const AVATAR_COLORS = [
  'bg-emerald-500', 'bg-sky-500', 'bg-violet-500', 'bg-amber-500',
  'bg-rose-500', 'bg-teal-500', 'bg-indigo-500', 'bg-fuchsia-500'
]

export default {
  name: 'PokerSeat',
  props: {
    name: { type: String, default: '' },
    email: { type: String, default: '' },
    vote: { type: String, default: '-' },
    revealed: { type: Boolean, default: false },
    isSelf: { type: Boolean, default: false },
    // Kartların sırayla dönmesi için gecikme çarpanı
    index: { type: Number, default: 0 },
    // 'low' | 'high' | null — turun uç değerleri
    extreme: { type: String, default: null }
  },
  computed: {
    hasVoted() {
      return !!this.vote && this.vote !== '-'
    },
    shortName() {
      return (this.name || this.email || '?').split(' ')[0]
    },
    initials() {
      const source = (this.name || this.email || '?').trim()
      const parts = source.split(/[\s.@_-]+/).filter(Boolean)
      if (parts.length === 0) return '?'
      if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
      return (parts[0][0] + parts[1][0]).toUpperCase()
    },
    avatarColor() {
      const key = this.email || this.name || ''
      let hash = 0
      for (let i = 0; i < key.length; i++) hash = (hash * 31 + key.charCodeAt(i)) >>> 0
      return AVATAR_COLORS[hash % AVATAR_COLORS.length]
    },
    frontClass() {
      if (!this.hasVoted) return 'bg-white border-dashed border-gray-200 text-gray-300'
      if (this.vote === '?') return 'bg-amber-50 border-amber-300 text-amber-600'
      return 'bg-white border-emerald-300 text-emerald-700'
    }
  }
}
</script>

<style scoped>
.seat-card {
  position: relative;
  width: 100%;
  height: 4.5rem;
  perspective: 900px;
}

@media (min-width: 640px) {
  .seat-card {
    height: 5.5rem;
  }
}

.seat-card__inner {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  transition: transform 0.65s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.seat-card.is-revealed .seat-card__inner {
  transform: rotateY(180deg);
}

.seat-card__face {
  position: absolute;
  inset: 0;
  border-radius: 0.65rem;
  border-width: 2px;
  border-style: solid;
  display: flex;
  align-items: center;
  justify-content: center;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  box-shadow: 0 4px 10px -4px rgb(0 0 0 / 0.25);
}

.seat-card__face--front {
  transform: rotateY(180deg);
}

/* Kart sırtı — çapraz desenli klasik poker sırtı */
.seat-card__face--back {
  border-color: rgb(255 255 255 / 0.25);
  background-color: #065f46;
  background-image: repeating-linear-gradient(
    45deg,
    rgb(255 255 255 / 0.08) 0px,
    rgb(255 255 255 / 0.08) 3px,
    transparent 3px,
    transparent 8px
  );
}

/* Oy verildi — sırt canlanır */
.seat-card__face--back.is-ready {
  border-color: #6ee7b7;
  background-color: #047857;
  box-shadow: 0 0 0 1px rgb(110 231 183 / 0.4), 0 6px 16px -6px rgb(16 185 129 / 0.8);
}

/* Oy bekleniyor — soluk ve nabız gibi */
.seat-card__face--back.is-waiting {
  opacity: 0.5;
}

.seat-card__dots {
  display: flex;
  gap: 3px;
}

.seat-card__dots i {
  width: 4px;
  height: 4px;
  border-radius: 9999px;
  background-color: rgb(255 255 255 / 0.75);
  animation: seat-dot 1.2s infinite ease-in-out;
}

.seat-card__dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.seat-card__dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes seat-dot {
  0%, 60%, 100% { opacity: 0.25; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-3px); }
}

@media (prefers-reduced-motion: reduce) {
  .seat-card__inner {
    transition: none;
  }
  .seat-card__dots i {
    animation: none;
  }
}
</style>
