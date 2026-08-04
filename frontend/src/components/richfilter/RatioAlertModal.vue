<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="emit('close')">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-lg">
      <div class="px-6 py-4 border-b border-gray-100">
        <h2 class="text-base font-semibold text-gray-800">
          {{ element ? 'Uyarı kuralını düzenle' : 'Uyarı kuralı oluştur' }}
        </h2>
        <p class="text-xs text-gray-500 mt-1">
          Oran hedefin dışına çıktığında bildirim gelir. Panoya bakmayı gerektirmeyen
          tek özellik budur.
        </p>
      </div>

      <div class="px-6 py-5 space-y-4">
        <p v-if="!smartFilters.length" class="text-xs text-amber-600 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2">
          Uyarı kuralı akıllı filtreler üzerinden hesaplanır. Önce en az bir akıllı filtre tanımlayın.
        </p>

        <template v-else>
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Kuralın adı</label>
            <input
              v-model="form.name"
              type="text"
              maxlength="120"
              placeholder="Test yığılması"
              class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-500/20"
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Pay</label>
            <select
              v-model="form.numeratorId"
              class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400"
              @change="onNumeratorChange"
            >
              <option value="" disabled>Akıllı filtre seçin…</option>
              <option v-for="smart in smartFilters" :key="smart.id" :value="smart.id">{{ smart.name }}</option>
            </select>
          </div>

          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1.5">Payda</label>
            <select
              v-model="form.denominatorId"
              class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400"
            >
              <option value="">Tüm sonuçlar</option>
              <option
                v-for="smart in smartFilters"
                :key="smart.id"
                :value="smart.id"
                :disabled="smart.id === form.numeratorId"
              >{{ smart.name }}</option>
            </select>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1.5">Hedef (%)</label>
              <input
                v-model.number="form.targetPercent"
                type="number"
                min="0"
                max="100"
                class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1.5">Yön</label>
              <select v-model="form.direction" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400">
                <option value="higher_better">Yüksek olması iyi</option>
                <option value="lower_better">Düşük olması iyi</option>
              </select>
            </div>
          </div>

          <p class="text-[11px] text-gray-400 bg-gray-50 border border-gray-100 rounded-lg px-3 py-2">
            Kural her sabah 09:00'da değerlendirilir. Bildirim yalnız <strong>durum
            değiştiğinde</strong> gider: eşiğin dışına çıkınca bir kez, normale dönünce bir kez.
            Böylece aynı uyarı her gün tekrarlanıp gürültüye dönüşmez.
          </p>
        </template>

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
 * Oran uyarı kuralı editörü (Ö3).
 *
 * Panodaki oran göstergesiyle aynı hesap, farklı amaç: gösterge bakıldığında
 * bilgi verir, kural bakılmadığında haber verir. İkisi ayrı yerlerde durur —
 * gösterim panonun kararı (K21), uyarı ise kimse bakmasa da çalışması gerektiği
 * için tanımın parçası.
 */
import { ref, computed } from 'vue'

const props = defineProps({
  /** Bağlanabilecek akıllı filtreler — zengin filtrenin kendi öğeleri. */
  smartFilters: { type: Array, default: () => [] },
  element: { type: Object, default: null },
  saving: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['close', 'save'])

const form = ref({
  name: props.element?.name ?? '',
  numeratorId: props.element?.config?.numeratorId ?? (props.smartFilters[0]?.id ?? ''),
  denominatorId: props.element?.config?.denominatorId ?? '',
  targetPercent: Math.round((props.element?.config?.target ?? 0.8) * 100),
  direction: props.element?.config?.direction ?? 'higher_better',
})

const canSave = computed(() =>
  form.value.name.trim().length > 0 && !!form.value.numeratorId
)

/** Ad boşsa payın adını önerir. */
function onNumeratorChange() {
  if (form.value.name.trim()) return
  const smart = props.smartFilters.find(s => s.id === form.value.numeratorId)
  if (smart) form.value.name = `${smart.name} oranı`
}

function submit() {
  if (!canSave.value || props.saving) return

  const percent = Number(form.value.targetPercent)
  emit('save', {
    kind: 'RATIO',
    name: form.value.name.trim(),
    config: {
      numeratorId: form.value.numeratorId,
      denominatorId: form.value.denominatorId || null,
      target: Math.min(Math.max(Number.isFinite(percent) ? percent : 80, 0), 100) / 100,
      direction: form.value.direction,
    },
  })
}
</script>
