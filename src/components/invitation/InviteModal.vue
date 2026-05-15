<template>
  <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
    <div class="bg-white rounded-xl p-6 w-full max-w-md shadow-2xl">
      <h4 class="text-lg font-semibold mb-5 text-gray-900">Davet Gönder</h4>

      <form @submit.prevent="send" class="space-y-4">
        <div>
          <label class="label">E-posta Adresi</label>
          <input
            v-model="form.email"
            type="email"
            required
            class="input-field"
            placeholder="kullanici@example.com"
          />
        </div>

        <div>
          <label class="label">Davet Tipi</label>
          <select v-model="form.type" class="input-field">
            <option value="ORGANIZATION">Organizasyon</option>
            <option value="PROJECT">Proje</option>
            <option value="TEAM">Takım</option>
          </select>
        </div>

        <div>
          <label class="label">Hedef ID</label>
          <input
            v-model="form.targetId"
            type="text"
            required
            class="input-field"
            placeholder="UUID"
          />
        </div>

        <div>
          <label class="label">Rol (Opsiyonel)</label>
          <select v-model="form.roleId" class="input-field">
            <option value="">Rol Seçilmedi</option>
            <option v-for="role in roles" :key="role.id" :value="role.id">{{ role.name }}</option>
          </select>
        </div>

        <div v-if="sent" class="p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700">
          ✅ Davet başarıyla gönderildi!
        </div>

        <div v-if="error" class="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          ❌ {{ error }}
        </div>

        <div class="flex gap-2 justify-end">
          <button type="button" @click="$emit('close')" class="btn-secondary">Kapat</button>
          <button type="submit" :disabled="sending" class="btn-primary">
            {{ sending ? 'Gönderiliyor...' : 'Davet Et' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import InvitationApi from '../../api/InvitationApi.js'
import RoleApi from '../../api/RoleApi.js'

const props = defineProps({
  defaultType: { type: String, default: 'PROJECT' },
  defaultTargetId: { type: String, default: '' },
})

const emit = defineEmits(['close', 'sent'])

const sending = ref(false)
const sent = ref(false)
const error = ref('')
const roles = ref([])

const form = ref({
  email: '',
  type: props.defaultType,
  targetId: props.defaultTargetId,
  roleId: '',
})

async function send() {
  sending.value = true
  error.value = ''
  sent.value = false
  try {
    await InvitationApi.send({
      ...form.value,
      roleId: form.value.roleId || null,
    })
    sent.value = true
    emit('sent')
    setTimeout(() => {
      form.value.email = ''
      sent.value = false
    }, 2000)
  } catch (e) {
    error.value = e.response?.data?.message || 'Davet gönderilemedi.'
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  try {
    const res = await RoleApi.getDefaults()
    roles.value = res.data
  } catch (e) {
    console.error('Roller yüklenemedi:', e)
  }
})
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500; }
.label { @apply block text-sm font-medium text-gray-700 mb-1; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors; }
</style>

