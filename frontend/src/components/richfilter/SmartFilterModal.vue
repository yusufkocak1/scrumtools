<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="emit('close')">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-2xl">
      <div class="px-6 py-4 border-b border-gray-100">
        <h2 class="text-base font-semibold text-gray-800">
          {{ isEdit ? 'Akıllı filtreyi düzenle' : 'Akıllı filtre oluştur' }}
        </h2>
        <p class="text-xs text-gray-500 mt-1">
          Sorguya uyan görevler bu ad ve renkle etiketlenir. Grafiklerin grup ekseni bu kategorilerdir.
        </p>
      </div>

      <div class="px-6 py-5 space-y-4">
        <!-- Ad -->
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Ad</label>
          <input
            ref="nameInput"
            v-model="form.name"
            type="text"
            maxlength="120"
            placeholder="Test"
            class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-500/20"
            @keydown.enter.prevent="submit"
          />
          <p class="mt-1 text-[11px] text-gray-400">
            Sorgularda bu adla kullanılır: <code class="font-mono">{{ smartFieldPreview }} = "{{ form.name || 'Ad' }}"</code>
          </p>
        </div>

        <!-- Renk -->
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Renk</label>
          <div class="flex items-center gap-2 flex-wrap">
            <button
              v-for="color in PALETTE"
              :key="color"
              type="button"
              class="w-7 h-7 rounded-md border-2 transition-transform hover:scale-110"
              :class="form.color?.toLowerCase() === color.toLowerCase() ? 'border-gray-800' : 'border-transparent'"
              :style="{ backgroundColor: color }"
              :title="color"
              @click="form.color = color"
            ></button>
            <label class="ml-1 flex items-center gap-1.5 text-[11px] text-gray-500 cursor-pointer">
              <input type="color" v-model="form.color" class="w-7 h-7 rounded cursor-pointer border border-gray-200" />
              Özel
            </label>
          </div>
        </div>

        <!-- Sorgu -->
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Sorgu (STQL)</label>
          <StqlInput
            v-model="form.query"
            :team-id="teamId"
            :project-id="projectId"
            placeholder='status in ("Ready to Test", Test) AND tester is not EMPTY'
            @validated="onValidated"
          />
          <p class="mt-1.5 text-[11px] text-gray-400">
            Bu sorgu temel sorgunun üstüne uygulanır — burada yalnız kategoriyi ayıran koşulu yazın.
          </p>
        </div>

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
          {{ saving ? 'Kaydediliyor…' : (isEdit ? 'Kaydet' : 'Oluştur') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Akıllı filtre oluşturma/düzenleme diyaloğu.
 *
 * Sorgu editörü görev listesindekiyle aynı bileşendir: yazarken doğrulama ve
 * "N sonuç" sayacı buradan da çalışır, kullanıcı kuralı kaydetmeden önce kaç
 * göreve dokunduğunu görür.
 */
import { ref, computed, onMounted, nextTick } from 'vue'
import StqlInput from '../work/StqlInput.vue'
import { smartField } from '../../api/RichFilterApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  projectId: { type: String, default: null },
  richFilterName: { type: String, default: '' },
  /** Düzenlenen öğe; null ise yeni kayıt. */
  element: { type: Object, default: null },
  saving: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['close', 'save'])

/** RichFilterService.DEFAULT_COLORS ile aynı palet — sunucu da renk atarken bunu kullanır. */
const PALETTE = ['#6366F1', '#0EA5E9', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#64748B']

const isEdit = computed(() => !!props.element)
const nameInput = ref(null)

const form = ref({
  name: props.element?.name ?? '',
  color: props.element?.color ?? PALETTE[0],
  query: props.element?.query ?? '',
})

/**
 * Sorgu geçerli mi. Başlangıçta iyimser: doğrulama 700 ms'lik duraklamadan sonra
 * geldiği için, yapıştırıp hemen kaydetmek isteyen kullanıcıyı bekletmemek gerekir.
 * Gerçekten bozuk bir sorgu sunucuda da reddedilir ve hata burada gösterilir.
 */
const queryValid = ref(true)

const smartFieldPreview = computed(() => smartField(props.richFilterName || 'zengin filtre'))

const canSave = computed(() =>
  form.value.name.trim().length > 0 && form.value.query.trim().length > 0 && queryValid.value
)

function onValidated(result) {
  queryValid.value = !!result?.valid
}

function submit() {
  if (!canSave.value || props.saving) return
  emit('save', {
    kind: 'SMART_FILTER',
    name: form.value.name.trim(),
    color: form.value.color,
    query: form.value.query.trim(),
  })
}

onMounted(() => nextTick(() => nameInput.value?.focus()))
</script>
