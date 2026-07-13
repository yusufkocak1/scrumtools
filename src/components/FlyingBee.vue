<template>
  <div
      v-show="!banished"
      class="bee-container"
      :style="containerStyle"
      @mouseenter="flee"
  >
    <div class="bee-inner"
         :class="{ 'bee-flying': flying, 'bee-landed': !flying && !spraying, 'bee-panic': spraying && !flying }">
      <!-- Duman bulutu -->
      <div v-if="spraying && !flying" class="smoke-cloud">
        <span v-for="n in 10" :key="n" class="smoke-puff"></span>
      </div>

      <!-- Konuşma baloncuğu -->
      <transition name="bubble">
        <div v-if="showBubble" class="bee-bubble" :class="bubbleClasses">
          {{ bubbleText }}
          <span class="bubble-tail"></span>
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
        <!-- Güneş gözlüğü (Pala modu) -->
        <g v-if="wearingSunglasses" class="bee-shades">
          <path d="M22.5 25.5 L29 23" fill="none" stroke="#101010" stroke-linecap="round" stroke-width="2"/>
          <ellipse cx="16" cy="27.5" fill="#101010" rx="6.4" ry="5.2" stroke="#000" stroke-width="1"/>
          <path d="M12 25.5 q2.5 -1.2 5 0" fill="none" opacity="0.7" stroke="#6b6b6b" stroke-linecap="round" stroke-width="1.2"/>
        </g>
        <!-- Ön kanat -->
        <ellipse class="wing wing-front" cx="32" cy="12" fill="#e3f3ff" opacity="0.95" rx="10" ry="14" stroke="#8fc7f2" stroke-width="1"/>
      </svg>
    </div>
  </div>

  <!-- Duman sıkma butonu (mobilde ve arı yokken gizli) -->
  <button
      v-show="!banished"
      class="spray-btn"
      :class="{ 'spray-active': spraying }"
      title="Arıya duman sık!"
      type="button"
      @click="spray"
  >
    <svg class="spray-svg" viewBox="0 0 64 64" width="36" height="36">
      <!-- Baca (huni) -->
      <path d="M23 18 L11 4 L31 13 Z" fill="#78909c"/>
      <!-- Kapak -->
      <ellipse cx="27" cy="18" fill="#90a4ae" rx="11" ry="4"/>
      <!-- Gövde (teneke) -->
      <rect fill="#b0bec5" height="34" rx="4" width="22" x="16" y="18"/>
      <rect fill="#90a4ae" height="3" width="22" x="16" y="25"/>
      <rect fill="#90a4ae" height="3" width="22" x="16" y="44"/>
      <!-- Körük (deri) -->
      <path d="M38 22 L50 25 L50 49 L38 52 Z" fill="#a1887f"/>
      <line stroke="#8d6e63" stroke-width="1.5" x1="38" x2="50" y1="28" y2="30"/>
      <line stroke="#8d6e63" stroke-width="1.5" x1="38" x2="50" y1="34" y2="36"/>
      <line stroke="#8d6e63" stroke-width="1.5" x1="38" x2="50" y1="40" y2="41"/>
      <line stroke="#8d6e63" stroke-width="1.5" x1="38" x2="50" y1="46" y2="46"/>
      <!-- Körük tahtası -->
      <rect fill="#6d4c41" height="30" rx="2" width="7" x="50" y="22"/>
    </svg>
    <!-- Butondan çıkan duman -->
    <span v-if="spraying" class="btn-puffs">
      <span v-for="n in 7" :key="n" class="btn-puff"></span>
    </span>
  </button>
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
        "Yeni ScrumTools'a geçmemiz lazım, Google amca sıkıntı çıkarıyor!"
      ]
    },
    // Baloncuğun ekranda kalma süresi (ms)
    bubbleDuration: { type: Number, default: 5000 },
    // Kaçarken sataşmak için kullanıcı adı
    userName: { type: String, default: "" },
    // Duman sıkılınca arının kaybolma süresi (ms)
    banishDuration: { type: Number, default: 5 * 60 * 1000 }
  },
  data: () => ({
    x: -60,
    y: 120,
    facing: 1,
    flying: false,
    flightDuration: 2,
    showBubble: false,
    bubbleText: "",
    wearingSunglasses: false,
    spraying: false,
    banished: false,
    timers: []
  }),
  computed: {
    containerStyle() {
      return {
        transform: `translate(${this.x}px, ${this.y}px)`,
        transition: `transform ${this.flightDuration}s cubic-bezier(0.45, 0.05, 0.35, 1)`
      };
    },
    bubbleClasses() {
      // Baloncuğu ekran içinde kalacak şekilde konumlandır:
      // sağ kenara yakınsa sola, üste (navbar'a) yakınsa arının altına aç
      const side = this.x > window.innerWidth - 240 ? 'bubble-right' : 'bubble-left';
      const vertical = this.y < 170 ? 'bubble-below' : 'bubble-above';
      return [side, vertical];
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
      this.wearingSunglasses = false;

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
        // Slogan string ya da { text, sunglasses } objesi olabilir
        const pick = this.slogans[Math.floor(Math.random() * this.slogans.length)];
        this.bubbleText = typeof pick === "string" ? pick : pick.text;
        this.wearingSunglasses = typeof pick === "object" && !!pick.sunglasses;
        this.showBubble = true;
      }
      this.later(() => {
        this.showBubble = false;
        // Mesaj bitince gözlüğü çıkar
        this.wearingSunglasses = false;
        // Bir süre dinlenip başka bir yere uç
        this.later(() => this.flyTo(this.randomPoint()), 30000 + Math.random() * 15000);
      }, this.bubbleDuration);
    },
    spray() {
      if (this.spraying || this.banished) return;
      this.clearTimers();
      this.showBubble = false;
      this.spraying = true;

      // Dumanı görünce söylensin
      this.later(() => {
        this.bubbleText = "Kim sigara içti burada kardeşim?! Sağlığa zararlı demiyor muyuz? 😤";
        this.showBubble = true;
      }, 500);

      // Söylene söylene ekran dışına kaç
      this.later(() => {
        this.flightDuration = 1.6;
        this.facing = this.x < window.innerWidth / 2 ? 1 : -1;
        this.x = this.facing === 1 ? window.innerWidth + 120 : -140;
        this.y = Math.max(this.y - 150, 40);
        this.flying = true;

        this.later(() => {
          this.spraying = false;
          this.flying = false;
          this.showBubble = false;
          this.banished = true;
          // Refresh olsa bile süre boyunca kayıp kalsın
          localStorage.setItem("beeBanishedUntil", String(Date.now() + this.banishDuration));
          this.later(() => this.respawn(), this.banishDuration);
        }, 1700);
      }, 2200);
    },
    respawn() {
      localStorage.removeItem("beeBanishedUntil");
      // Ekran dışına ışınlan, sonra süzülerek geri gel
      this.flightDuration = 0;
      this.x = -60;
      this.y = 100 + Math.random() * Math.max(window.innerHeight - 300, 100);
      this.facing = 1;
      this.flying = false;
      this.banished = false;
      this.later(() => this.flyTo(this.randomPoint()), 150);
    },
    flee() {
      if (this.spraying || this.banished) return;
      // Mouse üzerine gelince mevcut konumdan uzak bir noktaya kaç
      let target = this.randomPoint();
      for (let i = 0; i < 5; i++) {
        if (Math.hypot(target.x - this.x, target.y - this.y) > 250) break;
        target = this.randomPoint();
      }
      this.flyTo(target);

      // Kaçarken sataş (sadece ilk isim)
      const firstName = this.userName.trim().split(/\s+/)[0];
      this.bubbleText = firstName
          ? `${firstName} beni yakalayamaz! 😜`
          : "Beni yakalayamazsın! 😜";
      this.showBubble = true;
      this.later(() => {
        this.showBubble = false;
      }, Math.min(this.flightDuration * 1000 - 300, 2500));
    }
  },
  mounted() {
    // Duman yemişse süresi dolana kadar gelmesin (refresh'e dayanıklı)
    const banishedUntil = Number(localStorage.getItem("beeBanishedUntil")) || 0;
    const remaining = banishedUntil - Date.now();
    if (remaining > 0) {
      this.banished = true;
      this.later(() => this.respawn(), remaining);
      return;
    }
    localStorage.removeItem("beeBanishedUntil");

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

/* Güneş gözlüğü yukarıdan süzülerek takılır */
.bee-shades {
  animation: shades-drop 0.35s ease-out;
  transform-origin: 16px 27px;
}

@keyframes shades-drop {
  from { opacity: 0; transform: translateY(-8px) scale(1.2); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* Konuşma baloncuğu */
.bee-bubble {
  position: absolute;
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

.bubble-above { bottom: 48px; }
.bubble-below { top: 44px; }
.bubble-left { left: 26px; }
.bubble-right { right: 26px; }

.bubble-tail {
  position: absolute;
  width: 12px;
  height: 12px;
  background: #fff;
  transform: rotate(45deg);
}

.bubble-above .bubble-tail {
  bottom: -8px;
  border-right: 2px solid #ffce31;
  border-bottom: 2px solid #ffce31;
}

.bubble-below .bubble-tail {
  top: -8px;
  border-left: 2px solid #ffce31;
  border-top: 2px solid #ffce31;
}

.bubble-left .bubble-tail { left: 12px; }
.bubble-right .bubble-tail { right: 12px; }

.bubble-enter-active,
.bubble-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.bubble-enter-from,
.bubble-leave-to {
  opacity: 0;
  transform: translateY(6px) scale(0.8);
}

/* Duman yiyince panikleyip öksürme */
.bee-panic {
  animation: bee-panic 0.14s linear infinite;
}

@keyframes bee-panic {
  0%, 100% { transform: translate(-3px, 1px) rotate(-7deg); }
  50% { transform: translate(3px, -2px) rotate(7deg); }
}

/* Arının üstündeki duman bulutu */
.smoke-cloud {
  position: absolute;
  inset: -12px;
  pointer-events: none;
}

.smoke-puff {
  position: absolute;
  left: 18px;
  top: 18px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(140, 140, 145, 0.95), rgba(200, 200, 205, 0.4));
  opacity: 0;
  animation: smoke-rise 1.4s ease-out infinite;
}

.smoke-puff:nth-child(1) { --dx: -30px; --dy: -24px; animation-delay: 0s; }
.smoke-puff:nth-child(2) { --dx: 26px; --dy: -32px; animation-delay: 0.15s; }
.smoke-puff:nth-child(3) { --dx: -10px; --dy: -40px; animation-delay: 0.3s; }
.smoke-puff:nth-child(4) { --dx: 34px; --dy: -12px; animation-delay: 0.45s; }
.smoke-puff:nth-child(5) { --dx: -34px; --dy: 6px; animation-delay: 0.6s; }
.smoke-puff:nth-child(6) { --dx: 8px; --dy: -50px; animation-delay: 0.75s; }
.smoke-puff:nth-child(7) { --dx: -22px; --dy: -44px; animation-delay: 0.9s; }
.smoke-puff:nth-child(8) { --dx: 30px; --dy: -40px; animation-delay: 1.05s; }
.smoke-puff:nth-child(9) { --dx: 0px; --dy: -28px; animation-delay: 1.2s; }
.smoke-puff:nth-child(10) { --dx: -14px; --dy: -14px; animation-delay: 1.35s; }

@keyframes smoke-rise {
  0% { opacity: 0; transform: translate(0, 0) scale(0.4); }
  20% { opacity: 0.9; }
  100% { opacity: 0; transform: translate(var(--dx, 0), var(--dy, -30px)) scale(2.8); }
}

/* Duman sıkma butonu */
.spray-btn {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 9991;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid #e5e7eb;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.spray-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.25);
}

.spray-btn:active {
  transform: scale(0.95);
}

/* Körük pompalama: büyüyüp küçülerek basıyor */
.spray-active .spray-svg {
  animation: smoker-pump 0.45s ease-in-out 5;
  transform-origin: center;
}

@keyframes smoker-pump {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.45) rotate(-8deg); }
}

/* Butonun püskürttüğü duman */
.btn-puffs {
  position: absolute;
  top: 4px;
  left: 10px;
  pointer-events: none;
}

.btn-puff {
  position: absolute;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(140, 140, 145, 0.9);
  opacity: 0;
  animation: btn-smoke 1s ease-out infinite;
}

.btn-puff:nth-child(2) { animation-delay: 0.15s; --bx: -46px; --by: -30px; }
.btn-puff:nth-child(3) { animation-delay: 0.3s; --bx: -30px; --by: -50px; }
.btn-puff:nth-child(4) { animation-delay: 0.45s; --bx: -52px; --by: -44px; }
.btn-puff:nth-child(5) { animation-delay: 0.6s; --bx: -24px; --by: -38px; }
.btn-puff:nth-child(6) { animation-delay: 0.75s; --bx: -44px; --by: -56px; }
.btn-puff:nth-child(7) { animation-delay: 0.9s; --bx: -36px; --by: -26px; }

@keyframes btn-smoke {
  0% { opacity: 0.95; transform: translate(0, 0) scale(0.5); }
  100% { opacity: 0; transform: translate(var(--bx, -38px), var(--by, -42px)) scale(2.4); }
}

/* Mobilde butonu gizle */
@media (max-width: 767px) {
  .spray-btn {
    display: none;
  }
}
</style>
