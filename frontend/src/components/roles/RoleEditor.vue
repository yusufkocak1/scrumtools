<template>
  <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
    <div class="bg-white dark:bg-gray-800 rounded-xl p-6 w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
      <h4 class="text-lg font-semibold mb-5 text-gray-900 dark:text-white">
        {{ role ? 'Rol Düzenle' : 'Yeni Rol Oluştur' }}
      </h4>

      <form @submit.prevent="save" class="space-y-4">
        <div>
          <label class="label">Rol Adı</label>
          <input v-model="form.name" type="text" required class="input-field" />
        </div>

        <div>
          <label class="label">Açıklama</label>
          <textarea v-model="form.description" rows="2" class="input-field"></textarea>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="label">Kapsam</label>
            <select v-model="form.scope" class="input-field">
              <option value="PROJECT">Proje</option>
              <option value="ORGANIZATION">Organizasyon</option>
              <option value="TEAM">Takım</option>
            </select>
          </div>
          <div>
            <label class="label">Renk</label>
            <input v-model="form.color" type="color" class="h-10 w-full rounded border border-gray-300 dark:border-gray-600 cursor-pointer" />
          </div>
        </div>

        <!-- İzinler -->
        <div>
          <label class="label">İzinler</label>
          <div class="border border-gray-200 dark:border-gray-600 rounded-lg p-3 max-h-60 overflow-y-auto space-y-1">
            <div
              v-for="(perms, group) in groupedPermissions"
              :key="group"
              class="mb-2"
            >
              <p class="text-xs font-semibold text-gray-500 uppercase mb-1">{{ group }}</p>
              <div class="grid grid-cols-2 gap-1">
                <label
                  v-for="perm in perms"
                  :key="perm"
                  class="flex items-center gap-2 cursor-pointer text-xs"
                >
                  <input
                    type="checkbox"
                    :value="perm"
                    v-model="form.permissions"
                    class="rounded text-indigo-600"
                  />
                  <span class="text-gray-700 dark:text-gray-300">{{ formatPermission(perm) }}</span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <div class="flex gap-2 justify-end pt-2">
          <button type="button" @click="$emit('cancel')" class="btn-secondary">İptal</button>
          <button type="submit" :disabled="saving" class="btn-primary">
            {{ saving ? 'Kaydediliyor...' : 'Kaydet' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import RoleApi from '../../api/RoleApi.js'

const props = defineProps({
  role: { type: Object, default: null }
})

const emit = defineEmits(['saved', 'cancel'])

const saving = ref(false)
const allPermissions = ref([])

const form = ref({
  name: '',
  description: '',
  scope: 'PROJECT',
  color: '#6B7280',
  permissions: [],
})

watch(() => props.role, (val) => {
  if (val) {
    form.value = {
      name: val.name || '',
      description: val.description || '',
      scope: val.scope || 'PROJECT',
      color: val.color || '#6B7280',
      permissions: [...(val.permissions || [])],
    }
  }
}, { immediate: true })

const groupedPermissions = computed(() => {
  const groups = {}
  for (const perm of allPermissions.value) {
    const group = perm.split('_')[0]
    if (!groups[group]) groups[group] = []
    groups[group].push(perm)
  }
  return groups
})

function formatPermission(perm) {
  return perm.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())
}

async function save() {
  saving.value = true
  try {
    if (props.role) {
      await RoleApi.update(props.role.id, form.value)
    } else {
      await RoleApi.create(form.value)
    }
    emit('saved')
  } catch (e) {
    console.error('Kayıt hatası:', e)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const res = await RoleApi.getPermissions()
    allPermissions.value = res.data
  } catch (e) {
    console.error('İzinler yüklenemedi:', e)
  }
})
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500; }
.label { @apply block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 transition-colors; }
</style>

