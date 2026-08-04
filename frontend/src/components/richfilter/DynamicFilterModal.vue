<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="emit('close')">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-lg">
      <div class="px-6 py-4 border-b border-gray-100">
        <h2 class="text-base font-semibold text-gray-800">
          {{ element ? 'Dinamik filtreyi düzenle' : 'Dinamik filtre oluştur' }}
        </h2>
        <p class="text-xs text-gray-500 mt-1">
          Seçenekleri sen değil veri belirler: listede yalnız o an sonuçta bulunan
          değerler, sayılarıyla birlikte çıkar.
        </p>
      </div>

      <div class="px-6 py-5 space-y-4">
        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Alan</label>
          <select
            v-model="form.field"
            class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400"
            @change="onFieldChange"
          >
            <option value="" disabled>Alan seçin…</option>
            <option v-for="field in fields" :key="field.name" :value="field.name">
              {{ field.label }}
            </option>
          </select>
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Kontrolün adı</label>
          <input
            v-model="form.name"
            type="text"
            maxlength="120"
            placeholder="Atanan"
            class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400 focus:ring-2 focus:ring-purple-500/20"
          />
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-600 mb-1.5">Azami seçenek</label>
          <input
            v-model.number="form.maxOptions"
            type="number"
            min="5"
            max="100"
            class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:outline-none focus:border-purple-400"
          />
          <p class="mt-1 text-[11px] text-gray-400">
            En çok göreve sahip değerler önce gelir; kalanlar listede gösterilmez.
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
 * Dinamik filtre editörü.
 *
 * Alan listesi sorgu dilinin kendi kataloğundan gelir (`groupable` bayrağı);
 * ayrı bir "filtrelenebilir alanlar" listesi tutulmaz — iki liste zamanla ayrışırdı.
 */
import { ref, computed, onMounted } from 'vue'
import { getQueryFields } from '../../api/QueryApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  element: { type: Object, default: null },
  saving: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['close', 'save'])

const fields = ref([])

const form = ref({
  name: props.element?.name ?? '',
  field: props.element?.config?.field ?? '',
  maxOptions: props.element?.config?.maxOptions ?? 25,
})

const canSave = computed(() => form.value.name.trim().length > 0 && !!form.value.field)

/** Ad boşsa alanın etiketini önerir — çoğu durumda kullanıcı aynısını yazacaktı. */
function onFieldChange() {
  if (form.value.name.trim()) return
  const field = fields.value.find(f => f.name === form.value.field)
  if (field) form.value.name = field.label
}

function submit() {
  if (!canSave.value || props.saving) return
  emit('save', {
    kind: 'DYNAMIC_FILTER',
    name: form.value.name.trim(),
    config: {
      field: form.value.field,
      maxOptions: Math.min(Math.max(Number(form.value.maxOptions) || 25, 5), 100),
    },
  })
}

onMounted(async () => {
  try {
    const catalog = await getQueryFields(props.teamId)
    fields.value = (catalog.fields || []).filter(f => f.groupable)
  } catch (e) {
    console.error('Alan kataloğu alınamadı', e)
  }
})
</script>
