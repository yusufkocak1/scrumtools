<template>
  <button
    type="button"
    class="poker-card"
    :class="cardClass"
    :disabled="disabled"
    :aria-pressed="isSelected"
    :aria-label="`${number} tahmin kartı`"
    @click="select"
  >
    <!-- Köşe rakamları — gerçek oyun kartı hissi -->
    <span class="poker-card__corner poker-card__corner--tl">{{ number }}</span>
    <span class="poker-card__value">{{ number }}</span>
    <span class="poker-card__corner poker-card__corner--br">{{ number }}</span>

    <!-- Seçim rozeti -->
    <span
      v-if="isSelected"
      class="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-white text-emerald-600 shadow-lg flex items-center justify-center ring-2 ring-emerald-500"
    >
      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
      </svg>
    </span>
  </button>
</template>

<script>
export default {
  name: "pokerCard",
  props: {
    number: String,
    selectedCardNumber: String,
    // true: seçilebilir kart geçici olarak kapalı (oylar açıkken)
    disabled: { type: Boolean, default: false }
  },
  emits: ['selectPokerCard'],
  computed: {
    isSelected() {
      return this.selectedCardNumber === this.number
    },
    cardClass() {
      if (this.disabled) {
        return this.isSelected
          ? 'poker-card--selected poker-card--frozen'
          : 'poker-card--idle poker-card--frozen'
      }
      return this.isSelected ? 'poker-card--selected' : 'poker-card--idle'
    }
  },
  methods: {
    select() {
      if (!this.disabled) {
        this.$emit('selectPokerCard', this.number)
      }
    }
  }
}
</script>

<style scoped>
.poker-card {
  position: relative;
  width: 4rem;
  height: 5.75rem;
  border-radius: 0.75rem;
  border-width: 2px;
  border-style: solid;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  user-select: none;
  transition: transform 0.25s cubic-bezier(0.2, 0.8, 0.2, 1), box-shadow 0.25s ease,
    background-color 0.25s ease, border-color 0.25s ease, opacity 0.25s ease;
}

@media (min-width: 640px) {
  .poker-card {
    width: 4.75rem;
    height: 6.75rem;
  }
}

.poker-card__value {
  font-size: 1.5rem;
  line-height: 1;
  letter-spacing: -0.02em;
}

@media (min-width: 640px) {
  .poker-card__value {
    font-size: 1.875rem;
  }
}

.poker-card__corner {
  position: absolute;
  font-size: 0.625rem;
  line-height: 1;
  opacity: 0.65;
}

.poker-card__corner--tl {
  top: 0.4rem;
  left: 0.45rem;
}

.poker-card__corner--br {
  bottom: 0.4rem;
  right: 0.45rem;
  transform: rotate(180deg);
}

/* Seçilmemiş kart — beyaz yüz, yeşil mürekkep */
.poker-card--idle {
  background-color: #ffffff;
  border-color: #e5e7eb;
  color: #047857;
  box-shadow: 0 1px 3px rgb(0 0 0 / 0.1);
}

.poker-card--idle:hover:not(:disabled) {
  transform: translateY(-0.6rem) rotate(-2deg);
  border-color: #6ee7b7;
  box-shadow: 0 18px 30px -12px rgb(5 150 105 / 0.45);
}

/* Seçili kart — masaya sürülmüş gibi yukarı kalkar */
.poker-card--selected {
  background-image: linear-gradient(135deg, #10b981, #047857);
  border-color: #34d399;
  color: #ffffff;
  transform: translateY(-0.9rem);
  box-shadow: 0 22px 34px -14px rgb(5 150 105 / 0.7);
}

.poker-card--selected:hover:not(:disabled) {
  transform: translateY(-1.1rem) rotate(1deg);
}

.poker-card:active:not(:disabled) {
  transform: translateY(-0.3rem) scale(0.97);
}

/* Oylar açıkken deste dondurulur */
.poker-card--frozen {
  cursor: not-allowed;
}

.poker-card--frozen.poker-card--idle {
  opacity: 0.35;
  filter: grayscale(1);
}

.poker-card--frozen.poker-card--selected {
  opacity: 0.85;
  transform: translateY(-0.5rem);
}

@media (prefers-reduced-motion: reduce) {
  .poker-card,
  .poker-card:hover:not(:disabled) {
    transition: none;
    transform: none;
  }
}
</style>
