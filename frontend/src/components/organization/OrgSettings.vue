<template>
  <div class="bg-white rounded-xl shadow p-6 space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold text-gray-900">Organizasyon Ayarları</h2>
      <span
        class="text-xs px-2 py-1 rounded-full"
        :class="planBadgeClass"
      >{{ org?.plan }}</span>
    </div>

    <form @submit.prevent="save" class="space-y-4">
      <!-- Logo -->
      <div class="flex items-center gap-4">
        <div class="w-16 h-16 rounded-xl bg-indigo-100 flex items-center justify-center text-2xl font-bold text-indigo-600">
          {{ form.name?.charAt(0)?.toUpperCase() || '?' }}
        </div>
        <div>
          <p class="text-sm font-medium text-gray-700">Logo URL</p>
          <input
            v-model="form.logoUrl"
            type="url"
            placeholder="https://example.com/logo.png"
            class="mt-1 input-field"
          />
        </div>
      </div>

      <!-- Ad -->
      <div>
        <label class="label">Organizasyon Adı</label>
        <input v-model="form.name" type="text" required class="input-field" />
      </div>

      <!-- Açıklama -->
      <div>
        <label class="label">Açıklama</label>
        <textarea v-model="form.description" rows="3" class="input-field"></textarea>
      </div>

      <!-- Slug (read-only) -->
      <div>
        <label class="label">Slug (URL)</label>
        <input :value="org?.slug" disabled class="input-field opacity-60 cursor-not-allowed" />
        <p class="text-xs text-gray-500 mt-1">Slug değiştirilemez.</p>
      </div>

      <div class="flex justify-end">
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? 'Kaydediliyor...' : 'Kaydet' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import OrganizationApi from '../../api/OrganizationApi.js'

const props = defineProps({
  org: { type: Object, required: true }
})

const emit = defineEmits(['updated'])

const saving = ref(false)
const form = ref({
  name: '',
  slug: '',
  description: '',
  logoUrl: '',
})

watch(() => props.org, (val) => {
  if (val) {
    form.value = {
      name: val.name || '',
      slug: val.slug || '',
      description: val.description || '',
      logoUrl: val.logoUrl || '',
    }
  }
}, { immediate: true })

const planBadgeClass = computed(() => {
  const plan = props.org?.plan
  if (plan === 'ENTERPRISE') return 'bg-purple-100 text-purple-700'
  if (plan === 'PRO') return 'bg-blue-100 text-blue-700'
  return 'bg-gray-100 text-gray-600'
})

async function save() {
  saving.value = true
  try {
    const res = await OrganizationApi.update(props.org.id, form.value)
    emit('updated', res.data)
  } catch (e) {
    console.error('Kayıt hatası:', e)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.input-field {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500;
}
.label {
  @apply block text-sm font-medium text-gray-700 mb-1;
}
.btn-primary {
  @apply px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors;
}
</style>

