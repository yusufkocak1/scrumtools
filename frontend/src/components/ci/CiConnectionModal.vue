<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-2xl p-6 w-full max-w-lg shadow-2xl border border-gray-200">
      <div class="flex items-center gap-3 mb-5">
        <div class="w-10 h-10 bg-gray-900 rounded-xl flex items-center justify-center">
          <span v-html="JENKINS_ICON" class="w-5 h-5 text-white"></span>
        </div>
        <h4 class="text-lg font-semibold text-gray-900">
          {{ isEdit ? 'Bağlantıyı Düzenle' : 'CI/CD Bağlantısı Ekle' }}
        </h4>
      </div>

      <form @submit.prevent="save" class="space-y-4">
        <!-- Sağlayıcı (şimdilik yalnız Jenkins) -->
        <div v-if="!isEdit" class="flex items-center gap-2.5 px-3 py-2.5 rounded-lg border border-indigo-500 bg-indigo-50 text-indigo-700 text-sm font-medium">
          <span v-html="JENKINS_ICON" class="w-5 h-5 flex-shrink-0"></span>
          <span>Jenkins</span>
          <span class="ml-auto text-[10px] uppercase tracking-wide text-indigo-400">CI sunucusu</span>
        </div>

        <!-- Ad -->
        <div>
          <label class="label">Bağlantı Adı</label>
          <input v-model="form.name" type="text" required class="input-field"
            placeholder='Örn: "Şirket Jenkins&#39;i"' />
        </div>

        <!-- Jenkins adresi (zorunlu) -->
        <div>
          <label class="label">Jenkins Adresi</label>
          <input v-model="form.baseUrl" type="url" required class="input-field font-mono"
            placeholder="https://jenkins.sirket.com" />
          <p class="text-xs text-gray-400 mt-1">Jenkins kök adresi — sonundaki / gerekmez.</p>
        </div>

        <!-- Kullanıcı adı -->
        <div>
          <label class="label">Kullanıcı Adı</label>
          <input v-model="form.username" type="text" required class="input-field"
            placeholder="jenkins-kullanıcı-adı" autocomplete="username" />
        </div>

        <!-- API Token -->
        <div>
          <label class="label">
            API Token
            <span v-if="isEdit" class="font-normal text-gray-400">— boş bırakılırsa değişmez ({{ connection.tokenHint }})</span>
          </label>
          <input v-model="form.token" type="password" :required="!isEdit" class="input-field font-mono"
            placeholder="11xxxxxxxxxxxxxxxxxxxxxxxxx" autocomplete="new-password" />
          <p class="text-xs text-gray-400 mt-1">
            Jenkins → kullanıcı → Configure → API Token → Add new Token
          </p>
        </div>

        <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
          {{ error }}
        </p>

        <div class="flex gap-2 justify-end pt-2">
          <button type="button" @click="$emit('close')" class="btn-secondary">İptal</button>
          <button type="submit" :disabled="saving" class="btn-primary">
            {{ saving ? 'Doğrulanıyor...' : (isEdit ? 'Kaydet' : 'Doğrula ve Ekle') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { createConnection, updateConnection } from '../../api/CiApi.js'

const props = defineProps({
  orgId: { type: String, required: true },
  // Düzenleme modunda mevcut bağlantı; null = yeni ekleme
  connection: { type: Object, default: null },
})
const emit = defineEmits(['close', 'saved'])

const JENKINS_ICON = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1.2 4.3c.5-.2 1.1.1 1.2.6.1.4-.1.8-.5 1-.3.5-.2 1.1.2 1.5.1-.5.6-.9 1.1-.8.5 0 .9.5.9 1 0 .3-.2.6-.4.8.4.3.6.8.5 1.3l-.1.3c.5.1.9.5.9 1.1 0 .4-.2.7-.5.9.2.2.3.5.3.8 0 .6-.5 1.1-1.1 1.1-.2 0-.4-.1-.6-.2-.2.3-.5.5-.9.5-.3 0-.6-.1-.8-.4-.2.2-.5.4-.8.4-.5 0-1-.4-1-1 0-.1 0-.2.1-.3-.6-.1-1-.6-1-1.2 0-.3.1-.6.4-.8-.4-.2-.7-.7-.6-1.2.1-.5.5-.8 1-.9-.2-.3-.3-.7-.1-1.1.2-.5.7-.8 1.2-.7-.1-.4 0-.9.4-1.2.1-.1.2-.1.3-.2z"/></svg>'

const isEdit = computed(() => !!props.connection)

const form = ref({
  name: props.connection?.name || '',
  baseUrl: props.connection?.baseUrl || '',
  username: props.connection?.username || '',
  token: '',
})
const saving = ref(false)
const error = ref('')

async function save() {
  saving.value = true
  error.value = ''
  try {
    const payload = {
      provider: 'JENKINS',
      name: form.value.name,
      baseUrl: form.value.baseUrl,
      username: form.value.username,
      token: form.value.token || null,
    }
    const saved = isEdit.value
      ? await updateConnection(props.orgId, props.connection.id, payload)
      : await createConnection(props.orgId, payload)
    emit('saved', saved)
    emit('close')
  } catch (e) {
    error.value = e?.response?.data?.error
      || 'Bağlantı kaydedilemedi. Adres, kullanıcı adı ve token\'ı kontrol edin.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors; }
.label { @apply block text-sm font-medium text-gray-700 mb-1.5; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium text-sm; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium text-sm; }
</style>
