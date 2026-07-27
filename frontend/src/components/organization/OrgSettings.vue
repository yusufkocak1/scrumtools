<template>
  <div class="space-y-5">
    <div>
      <h2 class="text-lg font-semibold text-gray-900">Ayarlar</h2>
      <p class="text-sm text-gray-500 mt-0.5">Organizasyon profili ve görünen bilgileri</p>
    </div>

    <form @submit.prevent="save" class="space-y-5 max-w-xl">
      <!-- Logo -->
      <div class="flex items-center gap-4">
        <div class="w-16 h-16 rounded-xl bg-indigo-100 flex items-center justify-center text-2xl font-bold text-indigo-600 overflow-hidden flex-shrink-0">
          <img v-if="form.logoUrl" :src="form.logoUrl" alt="" class="w-full h-full object-cover" />
          <span v-else>{{ form.name?.charAt(0)?.toUpperCase() || '?' }}</span>
        </div>
        <div class="flex-1 min-w-0">
          <label class="label">Logo URL</label>
          <input
            v-model="form.logoUrl"
            type="url"
            placeholder="https://example.com/logo.png"
            class="input-field"
          />
        </div>
      </div>

      <div>
        <label class="label">Organizasyon Adı</label>
        <input v-model="form.name" type="text" required class="input-field" />
      </div>

      <div>
        <label class="label">Açıklama</label>
        <textarea v-model="form.description" rows="3" class="input-field"></textarea>
      </div>

      <div>
        <label class="label">Slug (URL)</label>
        <input :value="org?.slug" disabled class="input-field opacity-60 cursor-not-allowed font-mono" />
        <p class="text-xs text-gray-500 mt-1">Slug değiştirilemez.</p>
      </div>

      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

      <div class="flex justify-end pt-1">
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? 'Kaydediliyor...' : 'Kaydet' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
/**
 * Organizasyon profili. Paket/kullanım göstergeleri Abonelik bölümünde,
 * organizasyon geçişi ise sidebar'daki switcher'da — burada tekrarlanmaz.
 */
import { ref, watch } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import OrganizationApi from '../../api/OrganizationApi.js'

const props = defineProps({
  org: { type: Object, required: true }
})

const emit = defineEmits(['updated'])

const saving = ref(false)
const error = ref('')
const form = ref({ name: '', description: '', logoUrl: '' })

watch(() => props.org, (val) => {
  if (val) {
    form.value = {
      name: val.name || '',
      description: val.description || '',
      logoUrl: val.logoUrl || '',
    }
  }
}, { immediate: true })

async function save() {
  saving.value = true
  error.value = ''
  try {
    const res = await OrganizationApi.update(props.org.id, form.value)
    emit('updated', res.data)
    createToast('Organizasyon güncellendi.', { type: 'success', position: 'top-center' })
  } catch (e) {
    error.value = e?.response?.data?.message || 'Kaydedilemedi.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500; }
.label { @apply block text-sm font-medium text-gray-700 mb-1; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium; }
</style>
