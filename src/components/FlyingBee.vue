<template>
  <div
      class="bee-container"
      :style="containerStyle"
      @mouseenter="flee"
  >
    <div class="bee-inner" :class="{ 'bee-flying': flying, 'bee-landed': !flying }">
      <!-- Konuşma baloncuğu -->
      <transition name="bubble">
        <div v-if="showBubble" class="bee-bubble" :class="facing === 1 ? 'bubble-left' : 'bubble-right'">
          {{ bubbleText }}
          <span class="bubble-tail" :class="facing === 1 ? 'tail-left' : 'tail-right'"></span>
        </div>
      </transition>

      <!-- Arı -->
      <svg :style="{ transform: `scaleX(${facing})` }" class="bee-svg" viewBox="0 0 64 64" width="44" height="44">
        <!-- Antenler -->
        <path d="M22 18 Q18 10 13 8" fill="none" stroke="#3a2b1b" stroke-linecap="round" stroke-width="2"/>
        <path d="M26 16 Q26 8 22 4" fill="none" stroke="#3a2b1b" stroke-linecap="round" stroke-width="2"/>
        <circle cx="13" cy="8" fill="#3a2b1b" r="2.2"/>
        <circle cx="22" cy="4" fill="#3a2b1b" r="2.2"/>
        <!-- Arka kanat -->
        <ellipse class="wing wing-back" cx="38" cy="14" fill="#bfe3ff" opacity="0.85" rx="9" ry="13" stroke="#8fc7f2" stroke-width="1"/>
        <!-- İğne -->
        <path d="M56 34 L63 32 L56 28 Z" fill="#3a2b1b"/>
        <!-- Gövde -->
        <ellipse cx="38" cy="32" fill="#ffce31" rx="20" ry="14"/>
        <path d="M33 18.5 q3 13.5 0 27" fill="none" stroke="#3a2b1b" stroke-linecap="round" stroke-width="5"/>
        <path d="M43 19.5 q3 12.5 0 25" fill="none" stroke="#3a2b1b" stroke-linecap="round" stroke-width="5"/>
        <path d="M51 23 q2.5 9 0 18" fill="none" stroke="#3a2b1b" stroke-linecap="round" stroke-width="5"/>
        <!-- Kafa -->
        <circle cx="20" cy="30" fill="#3a2b1b" r="10"/>
        <circle cx="17" cy="27" fill="#fff" r="2.6"/>
        <circle cx="17.8" cy="27.8" fill="#000" r="1.3"/>
        <path d="M14 34 q3 2.5 6 0" fill="none" stroke="#fff" stroke-linecap="round" stroke-width="1.4"/>
        <!-- Ön kanat -->
        <ellipse class="wing wing-front" cx="32" cy="12" fill="#e3f3ff" opacity="0.95" rx="10" ry="14" stroke="#8fc7f2" stroke-width="1"/>
      </svg>
    </div>
  </div>
</template>

<script>
export default {
  name: "FlyingBee",
  props: {
    slogans: {
      type: Array,
      default: () => [
        "Şşşt, bal ye! 🍯",
        "Vız vız... çalışmaya devam!",
        "Sprint bitmeden bal bitmez!",
        "Mola vermeyi unutma! ☕",
        "Bugün harika görünüyorsun!"
      ]
    },
    // Baloncuğun ekranda kalma süresi (ms)
    bubbleDuration: { type: Number, default: 3500 }
  },
  data: () => ({
    x: -60,
    y: 120,
    facing: 1,
    flying: false,
    flightDuration: 2,
    showBubble: false,
    bubbleText: "",
    timers: []
  }),
  computed: {
    containerStyle() {
      return {
        transform: `translate(${this.x}px, ${this.y}px)`,
        transition: `transform ${this.flightDuration}s cubic-bezier(0.45, 0.05, 0.35, 1)`
      };
    }
  },
  methods: {
    later(fn, ms) {
      this.timers.push(setTimeout(fn, ms));
    },
    clearTimers() {
      this.timers.forEach(clearTimeout);
      this.timers = [];
    },
    randomPoint() {
      const margin = 30;
      const topOffset = 90; // navbar'ın altında kalsın
      const maxX = window.innerWidth - 80 - margin;
      const maxY = window.innerHeight - 80 - margin;
      return {
        x: margin + Math.random() * Math.max(maxX - margin, 1),
        y: topOffset + Math.random() * Math.max(maxY - topOffset, 1)
      };
    },
    flyTo(target) {
      this.clearTimers();
      this.showBubble = false;

      const dist = Math.hypot(target.x - this.x, target.y - this.y);
      this.flightDuration = Math.min(Math.max(dist / 350, 1.2), 4);
      this.facing = target.x >= this.x ? 1 : -1;
      this.flying = true;
      this.x = target.x;
      this.y = target.y;

      this.later(() => this.land(), this.flightDuration * 1000);
    },
    land() {
      this.flying = false;
      this.later(() => this.speak(), 700);
    },
    speak() {
      if (this.slogans.length > 0) {
        this.bubbleText = this.slogans[Math.floor(Math.random() * this.slogans.length)];
        this.showBubble = true;
      }
      this.later(() => {
        this.showBubble = false;
        // Bir süre dinlenip başka bir yere uç
        this.later(() => this.flyTo(this.randomPoint()), 2000 + Math.random() * 5000);
      }, this.bubbleDuration);
    },
    flee() {
      // Mouse üzerine gelince mevcut konumdan uzak bir noktaya kaç
      let target = this.randomPoint();
      for (let i = 0; i < 5; i++) {
        if (Math.hypot(target.x - this.x, target.y - this.y) > 250) break;
        target = this.randomPoint();
      }
      this.flyTo(target);
    }
  },
  mounted() {
    // Ekran dışından süzülerek gelsin
    this.later(() => this.flyTo(this.randomPoint()), 1500);
  },
  beforeUnmount() {
    this.clearTimers();
  }
};
</script>

<style scoped>
.bee-container {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 9990;
  cursor: default;
  will-change: transform;
}

.bee-inner {
  position: relative;
}

/* Uçarken hafif salınım */
.bee-flying {
  animation: bee-hover 0.5s ease-in-out infinite alternate;
}

/* Konmuşken yavaş nefes alma hareketi */
.bee-landed {
  animation: bee-idle 2.4s ease-in-out infinite;
}

@keyframes bee-hover {
  from { transform: translateY(-4px) rotate(-3deg); }
  to { transform: translateY(4px) rotate(3deg); }
}

@keyframes bee-idle {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(1px) scale(1.03, 0.97); }
}

.bee-svg {
  display: block;
  filter: drop-shadow(0 3px 3px rgba(0, 0, 0, 0.25));
}

/* Kanat çırpma */
.wing {
  transform-origin: 34px 26px;
  animation: wing-flap 0.12s linear infinite alternate;
}

.wing-back {
  animation-delay: 0.06s;
}

.bee-landed .wing {
  animation-duration: 0.6s;
}

@keyframes wing-flap {
  from { transform: rotate(-14deg); }
  to { transform: rotate(18deg); }
}

/* Konuşma baloncuğu */
.bee-bubble {
  position: absolute;
  bottom: 48px;
  max-width: 190px;
  width: max-content;
  padding: 8px 12px;
  background: #fff;
  color: #3a2b1b;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.3;
  border: 2px solid #ffce31;
  border-radius: 14px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
  pointer-events: none;
}

.bubble-left { left: 26px; }
.bubble-right { right: 26px; }

.bubble-tail {
  position: absolute;
  bottom: -8px;
  width: 12px;
  height: 12px;
  background: #fff;
  border-right: 2px solid #ffce31;
  border-bottom: 2px solid #ffce31;
  transform: rotate(45deg);
}

.tail-left { left: 12px; }
.tail-right { right: 12px; }

.bubble-enter-active,
.bubble-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.bubble-enter-from,
.bubble-leave-to {
  opacity: 0;
  transform: translateY(6px) scale(0.8);
}
</style>
