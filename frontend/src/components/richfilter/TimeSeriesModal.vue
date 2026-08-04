<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="emit('close')">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-lg">
      <div class="px-6 py-4 border-b border-gray-100">
        <h2 class="text-base font-semibold text-gray-800">
          {{ element ? 'Zaman serisini düzenle' : 'Zaman serisi oluştur' }}
        </h2>
        <p class="text-xs text-gray-500 mt-1">
          Bir akıllı filtrenin sayısı her gece ölçülüp saklanır; grafik bu ölçümlerin
          seyrini gösterir.
        </p>
      </div>

      <div class="px-6 py-5 space-y-4">
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Kaynak</label>
          <select
            v-model="form.smartFilterId"
            class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400"
            @change="onSourceChange"
          >
            <option value="">Tüm sonuçlar (temel sorgu)</option>
            <option v-for="smart in smartFilters" :key="smart.id" :value="smart.id">
              {{ smart.name }}
            </option>
          </select>
          <p class="mt-1 text-[11px] text-gray-400">
            Akıllı filtre seçilirse seri, o kategoriye düşen görevleri sayar — grafikteki
            dilimle aynı kural: ilk eşleşen kazanır.
          </p>
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Serinin adı</label>
          <input
            v-model="form.name"
            type="text"
            maxlength="120"
            placeholder="Testte bekleyen"
            class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-500/20"
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Renk</label>
          <div class="flex flex-wrap gap-1.5">
            <button
              v-for="color in PALETTE"
              :key="color"
              class="w-7 h-7 rounded-lg border-2 transition-transform"
              :class="form.color === color ? 'border-gray-800 scale-110' : 'border-transparent'"
              :style="{ backgroundColor: color }"
              @click="form.color = color"
            ></button>
            <button
              class="px-2 h-7 rounded-lg border border-dashed border-gray-300 text-[11px] text-gray-500 hover:border-gray-400"
              @click="form.color = ''"
            >
              Kaynağın rengi
            </button>
          </div>
        </div>

        <p class="text-[11px] text-gray-400 bg-gray-50 border border-gray-100 rounded-lg px-3 py-2">
          Kaydettikten sonra <strong>Geçmişi kur</strong> ile son 180 gün görev tarihçesinden
          yeniden kurgulanabilir. Sorgu tarihçede izlenmeyen bir alana dayanıyorsa kurgu
          yapılmaz; seri o zaman bugünden itibaren birikir.
        </p>

        <p v-if="error" class="text-xs text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
          {{ error }}
        </p>
      </div>

      <div class="px-6 py-4 border-t border-gray-100 flex items-center justify-end gap-2">
        <button class="text-sm text-gray-500 hover:text-gray-700 px-3 py-1.5" @click="emit('close')">İptal</button>
        <button
          class="text-sm bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white px-4 py-1.5 rounded-lg transition-colors"
          :disabled="!canSave || saving"
          @click="submit"
        >
          {{ saving ? 'Kaydediliyor…' : 'Kaydet' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Zaman serisi editörü.
 *
 * Seri kendi sorgusunu taşımaz; var olan bir akıllı filtreye bağlanır. Böylece
 * grafikteki dilim, kuyruktaki satır ve serideki çizgi aynı kuraldan beslenir —
 * seriye ayrı bir sorgu yazdırmak, zamanla ikisinin ayrışmasına açık kapı bırakırdı.
 */
import { ref, computed } from 'vue'
import { CHART_PALETTE } from '../../utils/chartPalette.js'

const props = defineProps({
  /** Bağlanabilecek akıllı filtreler — zengin filtrenin kendi öğeleri. */
  smartFilters: { type: Array, default: () => [] },
  element: { type: Object, default: null },
  saving: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['close', 'save'])

const PALETTE = CHART_PALETTE

const form = ref({
  name: props.element?.name ?? '',
  smartFilterId: props.element?.config?.smartFilterId ?? '',
  color: props.element?.color ?? '',
})

const canSave = computed(() => form.value.name.trim().length > 0)

/** Ad boşsa kaynağın adını önerir — çoğu durumda kullanıcı aynısını yazacaktı. */
function onSourceChange() {
  if (form.value.name.trim()) return
  const smart = props.smartFilters.find(s => s.id === form.value.smartFilterId)
  form.value.name = smart ? smart.name : 'Toplam'
}

function submit() {
  if (!canSave.value || props.saving) return
  emit('save', {
    kind: 'TIME_SERIES',
    name: form.value.name.trim(),
    color: form.value.color || null,
    config: {
      smartFilterId: form.value.smartFilterId || null,
      metric: 'count',
      interval: 'day',
    },
  })
}
</script>
