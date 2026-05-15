<template>
  <div class="bg-white dark:bg-gray-800 rounded-xl shadow p-6 space-y-5">
    <h2 class="text-lg font-semibold text-gray-900 dark:text-white">Proje Ayarları</h2>

    <form @submit.prevent="save" class="space-y-4">
      <div class="grid grid-cols-2 gap-4">
        <div class="col-span-2">
          <label class="label">Proje Adı</label>
          <input v-model="form.name" type="text" required class="input-field" />
        </div>

        <div>
          <label class="label">Proje Anahtarı</label>
          <input :value="project?.key" disabled class="input-field opacity-60 cursor-not-allowed font-mono" />
          <p class="text-xs text-gray-500 mt-1">Anahtar değiştirilemez.</p>
        </div>

        <div>
          <label class="label">Proje Tipi</label>
          <select v-model="form.projectType" class="input-field">
            <option value="SCRUM">Scrum</option>
            <option value="KANBAN">Kanban</option>
            <option value="BUG_TRACKING">Bug Tracking</option>
            <option value="CUSTOM">Özel</option>
          </select>
        </div>

        <div class="col-span-2">
          <label class="label">Açıklama</label>
          <textarea v-model="form.description" rows="3" class="input-field"></textarea>
        </div>

        <div>
          <label class="label">Renk</label>
          <div class="flex items-center gap-2">
            <input v-model="form.color" type="color" class="h-10 w-16 rounded cursor-pointer border border-gray-300 dark:border-gray-600" />
            <span class="text-sm text-gray-500">{{ form.color }}</span>
          </div>
        </div>

        <div>
          <label class="label">İkon URL</label>
          <input v-model="form.iconUrl" type="url" class="input-field" placeholder="https://..." />
        </div>
      </div>

      <div class="flex justify-end gap-2">
        <button type="button" @click="$emit('cancel')" class="btn-secondary">İptal</button>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? 'Kaydediliyor...' : 'Kaydet' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import ProjectApi from '../../api/ProjectApi.js'

const props = defineProps({
  project: { type: Object, required: true }
})

const emit = defineEmits(['updated', 'cancel'])
const saving = ref(false)

const form = ref({
  name: '',
  description: '',
  projectType: 'SCRUM',
  color: '#3B82F6',
  iconUrl: '',
})

watch(() => props.project, (val) => {
  if (val) {
    form.value = {
      name: val.name || '',
      description: val.description || '',
      projectType: val.projectType || 'SCRUM',
      color: val.color || '#3B82F6',
      iconUrl: val.iconUrl || '',
      key: val.key || '',
    }
  }
}, { immediate: true })

async function save() {
  saving.value = true
  try {
    const res = await ProjectApi.update(props.project.id, form.value)
    emit('updated', res.data)
  } catch (e) {
    console.error('Kayıt hatası:', e)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500; }
.label { @apply block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors; }
</style>

