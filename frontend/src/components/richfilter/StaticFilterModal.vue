<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="emit('close')">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col">
      <div class="px-6 py-4 border-b border-gray-100">
        <h2 class="text-base font-semibold text-gray-800">
          {{ element ? 'Sabit filtreyi düzenle' : 'Sabit filtre oluştur' }}
        </h2>
        <p class="text-xs text-gray-500 mt-1">
          Kontrol çubuğunda seçenekli bir düğme grubu olarak çıkar. Seçenekler yazar
          tarafından tanımlanır — "Bu hafta / Bu ay / Bu çeyrek" gibi.
        </p>
      </div>

      <div class="px-6 py-5 space-y-4 overflow-y-auto">
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Kontrolün adı</label>
          <input
            v-model="form.name"
            type="text"
            maxlength="120"
            placeholder="Zaman aralığı"
            class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-500/20"
          />
        </div>

        <div>
          <div class="flex items-center justify-between mb-1.5">
            <label class="block text-xs font-medium text-gray-600">Seçenekler</label>
            <button class="text-[11px] text-purple-600 hover:text-purple-700" @click="addOption">
              + Seçenek
            </button>
          </div>

          <div v-for="(option, index) in form.options" :key="option.id" class="mb-3 rounded-lg border border-gray-100 p-3">
            <div class="flex items-center gap-2 mb-2">
              <input
                v-model="option.label"
                type="text"
                placeholder="Bu hafta"
                class="flex-1 rounded-lg border border-gray-200 px-3 py-1.5 text-sm focus:outline-none focus:border-purple-400"
              />
              <button
                class="p-1.5 rounded text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                title="Seçeneği sil"
                @click="form.options.splice(index, 1)"
              >🗑</button>
            </div>
            <StqlInput
              v-model="option.query"
              :team-id="teamId"
              :project-id="projectId"
              placeholder="due <= endOfWeek()"
            />
          </div>

          <p v-if="!form.options.length" class="text-[11px] text-gray-400">
            En az bir seçenek ekleyin.
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
          {{ saving ? 'Kaydediliyor…' : 'Kaydet' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Sabit filtre editörü.
 *
 * Seçenekler ayrı bir tabloda değil, öğenin `config.options[]` dizisinde durur:
 * hepsi tek diyalogda birlikte düzenlenir ve dışarıdan yalnız `id` ile referans
 * alınırlar (bkz. RICH_FILTER_PLAN.md — §5).
 */
import { ref, computed } from 'vue'
import StqlInput from '../work/StqlInput.vue'

const props = defineProps({
  teamId: { type: String, required: true },
  projectId: { type: String, default: null },
  element: { type: Object, default: null },
  saving: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['close', 'save'])

/** Seçenek id'si kalıcıdır: seçim ve görünümler bu id'ye referans verir. */
const newOptionId = () => `o${Date.now()}${Math.floor(Math.random() * 1000)}`

const form = ref({
  name: props.element?.name ?? '',
  options: (props.element?.config?.options ?? []).map(o => ({ ...o })),
})

const canSave = computed(() =>
  form.value.name.trim().length > 0
  && form.value.options.length > 0
  && form.value.options.every(o => o.label?.trim() && o.query?.trim())
)

function addOption() {
  form.value.options.push({ id: newOptionId(), label: '', query: '' })
}

function submit() {
  if (!canSave.value || props.saving) return
  emit('save', {
    kind: 'STATIC_FILTER',
    name: form.value.name.trim(),
    config: {
      options: form.value.options.map(o => ({
        id: o.id,
        label: o.label.trim(),
        query: o.query.trim(),
      })),
    },
  })
}

if (!form.value.options.length) addOption()
</script>
