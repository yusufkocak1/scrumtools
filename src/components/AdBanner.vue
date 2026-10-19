<template>
  <div v-if="config.enabled !== false" class="w-full max-w-6xl mx-auto my-8">
    <div class="bg-gradient-to-r from-amber-50 to-yellow-50 border-2 border-amber-200 rounded-xl p-6 shadow-lg relative">
      <!-- Sponsored Badge (desktop only) -->
      <div
        class="absolute top-2 right-2 hidden md:block"
        @mouseenter="showSponsoredTooltip = true"
        @mouseleave="showSponsoredTooltip = false"
      >
        <span
          class="inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-bold tracking-wide uppercase bg-amber-600 text-white shadow cursor-help"
          aria-label="Sponsored Banner"
        >
                  <span class="rounded-full border px-1 mr-1">?</span> Sponsored
        </span>
        <div
          v-if="showSponsoredTooltip"
          class="absolute right-0 mt-2 w-56 bg-gray-900 text-white text-xs rounded-lg px-3 py-2 shadow-lg z-20"
        >
          {{ config.sponsoredTooltip }}
          <div class="absolute -top-1 right-4 w-2 h-2 bg-gray-900 rotate-45"></div>
        </div>
      </div>

      <!-- Reklam Başlığı -->
      <div class="text-center mb-6">
        <h2 class="text-3xl font-bold text-amber-800 mb-2">{{ config.title }}</h2>
        <p class="text-amber-700 text-lg">{{ config.subtitle }}</p>
      </div>

      <!-- Ürün Grid -->
      <div class="grid grid-cols-1 gap-6" :class="gridColsClass">
        <div
          v-for="(product, index) in config.products"
          :key="index"
          class="bg-white rounded-lg p-4 border border-amber-200 shadow-md relative"
        >
          <div class="text-center">
            <div class="text-4xl mb-3">{{ product.emoji || '🍯' }}</div>
            <h3 class="text-xl font-semibold text-gray-800 mb-2 flex items-center justify-center gap-2">
              {{ product.name }}
              <span
                v-if="product.tooltip"
                class="relative inline-block"
                @mouseenter="activeTooltip = index"
                @mouseleave="activeTooltip = null"
              >
                <svg class="w-4 h-4 text-gray-500 hover:text-gray-700 cursor-help" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd"></path>
                </svg>
                <!-- Tooltip -->
                <div
                  v-if="activeTooltip === index"
                  class="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-3 py-2 text-sm text-white bg-gray-800 rounded-lg shadow-lg whitespace-nowrap z-10"
                >
                  {{ product.tooltip }}
                  <div class="absolute top-full left-1/2 transform -translate-x-1/2 border-4 border-transparent border-t-gray-800"></div>
                </div>
              </span>
            </h3>
            <p class="text-gray-600 text-sm mb-3">{{ product.description }}</p>
            <div
              v-if="currentStock(product) <= 0"
              class="bg-red-100 text-red-800 px-3 py-1 rounded-full text-sm font-medium"
            >
              Stokta Yok
            </div>
            <template v-else>
              <div class="bg-green-100 text-green-800 px-3 py-1 rounded-full text-sm font-medium mb-2">
                {{ currentStock(product) }} {{ product.unit }} Kaldı
              </div>
              <div v-if="product.maxStock > 0" class="w-full bg-gray-200 rounded-full h-2">
                <div
                  class="bg-green-500 h-2 rounded-full transition-all duration-500"
                  :style="{ width: stockPercentage(product) + '%' }"
                ></div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- Alt Bilgi -->
      <div class="text-center mt-6 pt-4 border-t border-amber-200">
        <p v-if="config.phone" class="text-amber-700 text-sm">
          📞 İletişim: {{ config.phone }}
        </p>
        <p class="text-amber-600 text-xs mt-1">
          <template v-for="(note, i) in config.footerNotes" :key="i">
            {{ note }}<br v-if="i < config.footerNotes.length - 1">
          </template>
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import {DEFAULT_AD_CONFIG, listenAdConfig} from '../firebase/AdService.js'

export default {
  name: 'AdBanner',
  data() {
    return {
      config: DEFAULT_AD_CONFIG,
      activeTooltip: null, // Ürün tooltip'i
      showSponsoredTooltip: false, // Sponsored tooltip
      now: Date.now(),
    }
  },
  computed: {
    gridColsClass() {
      const count = (this.config.products || []).length;
      return count >= 3 ? 'md:grid-cols-3' : count === 2 ? 'md:grid-cols-2' : '';
    },
    // Her yıl için dinamik tarih hesaplama
    startDate() {
      const now = new Date(this.now);
      const currentYear = now.getFullYear();
      const currentMonth = now.getMonth(); // 0-11 arası (0=Ocak, 8=Eylül)

      // Eğer Ocak ayındaysak, önceki yılın Eylül'ünden başlıyoruz
      if (currentMonth < 8) { // Eylül'den önce (Ocak-Ağustos)
        return new Date(currentYear - 1, 8, 1); // Önceki yılın Eylül'ü
      } else {
        return new Date(currentYear, 8, 1); // Bu yılın Eylül'ü
      }
    },
    endDate() {
      const now = new Date(this.now);
      const currentYear = now.getFullYear();
      const currentMonth = now.getMonth();

      // Eğer Ocak ayındaysak, bu yılın Ocak sonuna kadar
      if (currentMonth < 8) { // Eylül'den önce (Ocak-Ağustos)
        return new Date(currentYear, 1, 0); // Bu yılın Ocak sonu (1 = Şubat, 0 = önceki ayın son günü)
      } else {
        return new Date(currentYear + 1, 1, 0); // Sonraki yılın Ocak sonu
      }
    }
  },
  methods: {
    // autoDecrease açıksa stok tarihe göre azalır, kapalıysa Firebase'deki değer aynen gösterilir
    currentStock(product) {
      const stock = Number(product.stock) || 0;
      return product.autoDecrease ? this.calculateStock(stock) : stock;
    },
    stockPercentage(product) {
      const max = Number(product.maxStock) || 0;
      if (max <= 0) return 0;
      return Math.min(100, Math.max(0, (this.currentStock(product) / max) * 100));
    },
    calculateStock(initialStock) {
      const startTime = this.startDate.getTime();
      const endTime = this.endDate.getTime();
      const currentTime = this.now;

      // Eğer başlangıç tarihinden önceysek tam stok
      if (currentTime < startTime) {
        return initialStock;
      }

      // Eğer bitiş tarihinden sonraysa stok sıfır
      if (currentTime > endTime) {
        return 0;
      }

      // Toplam gün sayısı
      const totalDays = Math.ceil((endTime - startTime) / (1000 * 60 * 60 * 24));

      // Geçen gün sayısı
      const daysPassed = Math.ceil((currentTime - startTime) / (1000 * 60 * 60 * 24));

      // İlerleme yüzdesi (0-1 arası)
      const progress = daysPassed / totalDays;

      // Belirli tarihlerde istenen stok seviyelerine göre hesaplama
      let stockProgress;

      // Toplam süre yaklaşık 5 ay (Eylül-Ocak)
      // Eylül-Ekim sonu: ~2 ay (40% of time) -> %50 stok kalsın (%50 azalsın)
      // Aralık sonu: ~4 ay (80% of time) -> %10 stok kalsın (%90 azalsın)
      // Ocak sonu: 5 ay (100% of time) -> %0 stok kalsın (%100 azalsın)

      if (progress <= 0.4) {
        // Eylül-Ekim sonu: İlk %40 zamanda %50 azalış
        stockProgress = progress * 1.25; // 0.4 * 1.25 = 0.5 (%50 azalış)
      } else if (progress <= 0.8) {
        // Kasım-Aralık sonu: %40-80 zamanda %50'den %90'a azalış (%40 daha azalış)
        stockProgress = 0.5 + (progress - 0.4); // 0.5 + 0.4 = 0.9 (%90 azalış)
      } else {
        // Ocak: Son %20 zamanda %90'dan %100'e azalış (%10 daha azalış)
        stockProgress = 0.9 + (progress - 0.8) * 0.5; // 0.9 + 0.2 * 0.5 = 1.0 (%100 azalış)
      }

      // Kalan stok hesaplama - stockProgress maksimum 1 olabilir
      stockProgress = Math.min(1, stockProgress);
      return Math.max(0, Math.floor(initialStock * (1 - stockProgress)));
    }
  },
  mounted() {
    // Firebase'deki reklam ayarlarını gerçek zamanlı dinle
    this.unsubscribeAdConfig = listenAdConfig((config) => {
      this.config = config;
    });
    // Her 1 saatte bir stok güncellemesi için interval
    this.stockInterval = setInterval(() => {
      this.now = Date.now();
    }, 3600000); // 1 saat = 3600000 ms
  },
  beforeUnmount() {
    if (this.unsubscribeAdConfig) {
      this.unsubscribeAdConfig();
    }
    if (this.stockInterval) {
      clearInterval(this.stockInterval);
    }
  }
}
</script>

<style scoped>
/* Hover efektleri */
.bg-white:hover {
  transform: translateY(-2px);
  transition: transform 0.2s ease-in-out;
}

/* Animasyonlar */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: .8;
  }
}
</style>
