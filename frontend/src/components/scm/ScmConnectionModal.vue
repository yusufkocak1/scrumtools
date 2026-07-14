<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-2xl p-6 w-full max-w-lg shadow-2xl border border-gray-200">
      <div class="flex items-center gap-3 mb-5">
        <div class="w-10 h-10 bg-gray-900 rounded-xl flex items-center justify-center">
          <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/>
          </svg>
        </div>
        <h4 class="text-lg font-semibold text-gray-900">
          {{ isEdit ? 'Bağlantıyı Düzenle' : 'SCM Bağlantısı Ekle' }}
        </h4>
      </div>

      <form @submit.prevent="save" class="space-y-4">
        <!-- Sağlayıcı seçimi -->
        <div v-if="!isEdit">
          <label class="label">Sağlayıcı</label>
          <div class="grid grid-cols-2 gap-2">
            <button
              v-for="p in providers"
              :key="p.id"
              type="button"
              @click="p.enabled && (form.provider = p.id)"
              :disabled="!p.enabled"
              :class="[
                'flex items-center gap-2.5 px-3 py-2.5 rounded-lg border text-sm font-medium transition-all',
                form.provider === p.id
                  ? 'border-indigo-500 bg-indigo-50 text-indigo-700 ring-1 ring-indigo-500'
                  : p.enabled
                    ? 'border-gray-200 text-gray-700 hover:border-gray-300 hover:bg-gray-50'
                    : 'border-gray-100 text-gray-300 cursor-not-allowed'
              ]"
            >
              <span v-html="p.icon" class="w-5 h-5 flex-shrink-0"></span>
              <span>{{ p.label }}</span>
              <span v-if="!p.enabled" class="ml-auto text-[10px] uppercase tracking-wide text-gray-400">yakında</span>
            </button>
          </div>
        </div>
        <div v-else class="flex items-center gap-2 text-sm text-gray-600">
          <span v-html="providerIcon" class="w-5 h-5"></span>
          <span class="font-medium">{{ connection.provider }}</span>
          <span v-if="connection.baseUrl" class="text-gray-400 font-mono text-xs">{{ connection.baseUrl }}</span>
        </div>

        <!-- Ad -->
        <div>
          <label class="label">Bağlantı Adı</label>
          <input v-model="form.name" type="text" required class="input-field"
            placeholder='Örn: "Şirket GitHub Hesabı"' />
        </div>

        <!-- Self-hosted URL -->
        <div v-if="!isEdit">
          <label class="flex items-center gap-2 mb-1.5">
            <input v-model="selfHosted" type="checkbox" class="rounded border-gray-300" />
            <span class="text-sm font-medium text-gray-700">Self-hosted kurulum</span>
          </label>
          <input v-if="selfHosted" v-model="form.baseUrl" type="url" class="input-field font-mono"
            :placeholder="form.provider === 'GITLAB' ? 'https://gitlab.sirket.com' : 'https://github.sirket.com'" />
        </div>

        <!-- Token -->
        <div>
          <label class="label">
            Erişim Token'ı (PAT)
            <span v-if="isEdit" class="font-normal text-gray-400">— boş bırakılırsa değişmez ({{ connection.tokenHint }})</span>
          </label>
          <input v-model="form.token" type="password" :required="!isEdit" class="input-field font-mono"
            :placeholder="form.provider === 'GITLAB' ? 'glpat-...' : 'ghp_... veya github_pat_...'"
            autocomplete="new-password" />
          <p class="text-xs text-gray-400 mt-1">
            {{ form.provider === 'GITLAB'
              ? 'GitLab → Settings → Access Tokens (scope: api)'
              : 'GitHub → Settings → Developer settings → Personal access tokens (scope: repo)' }}
          </p>
        </div>

        <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
          {{ error }}
        </p>

        <div class="flex gap-2 justify-end pt-2">
          <button type="button" @click="$emit('close')" class="btn-secondary">İptal</button>
          <button type="submit" :disabled="saving || (!isEdit && !form.provider)" class="btn-primary">
            {{ saving ? 'Doğrulanıyor...' : (isEdit ? 'Kaydet' : 'Doğrula ve Ekle') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { createConnection, updateConnection } from '../../api/ScmApi.js'

const props = defineProps({
  orgId: { type: String, required: true },
  // Düzenleme modunda mevcut bağlantı; null = yeni ekleme
  connection: { type: Object, default: null },
})
const emit = defineEmits(['close', 'saved'])

const GITHUB_ICON = '<svg viewBox="0 0 16 16" fill="currentColor"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0016 8c0-4.42-3.58-8-8-8z"/></svg>'
const GITLAB_ICON = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M23.6 9.6l-1.2-3.7L20 .9c-.1-.4-.7-.4-.8 0l-2.4 5H7.2L4.8.9c-.1-.4-.7-.4-.8 0L1.6 5.9.4 9.6c-.1.3 0 .7.3.9l11.3 8.2 11.3-8.2c.3-.2.4-.6.3-.9z"/></svg>'
const BITBUCKET_ICON = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M.8 1.4c-.5 0-.8.4-.8.8l3.3 19.5c.1.5.5.8 1 .8h15.6c.4 0 .7-.3.8-.7L24 2.2c.1-.5-.3-.8-.8-.8H.8zm13.9 13.8H9.4L8 8h7.9l-1.2 7.2z"/></svg>'
const GITEA_ICON = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm4.7 14.9c-.4.9-1.4 1.4-2.4 1.1l-4.9-1.6c-1.3-.4-2-1.8-1.6-3.1l1.6-4.9c.3-1 1.3-1.6 2.4-1.4l4.9 1.1c1.3.3 2.2 1.6 1.9 2.9l-1.9 5.9z"/></svg>'

const providers = [
  { id: 'GITHUB', label: 'GitHub', icon: GITHUB_ICON, enabled: true },
  { id: 'GITLAB', label: 'GitLab', icon: GITLAB_ICON, enabled: true },
  { id: 'BITBUCKET', label: 'Bitbucket', icon: BITBUCKET_ICON, enabled: false },
  { id: 'GITEA', label: 'Gitea', icon: GITEA_ICON, enabled: false },
]

const isEdit = computed(() => !!props.connection)
const providerIcon = computed(() =>
  providers.find(p => p.id === props.connection?.provider)?.icon || GITHUB_ICON)

const form = ref({
  provider: props.connection?.provider || 'GITHUB',
  name: props.connection?.name || '',
  baseUrl: props.connection?.baseUrl || '',
  token: '',
  username: props.connection?.username || '',
})
const selfHosted = ref(!!props.connection?.baseUrl)
const saving = ref(false)
const error = ref('')

async function save() {
  saving.value = true
  error.value = ''
  try {
    const payload = {
      provider: form.value.provider,
      name: form.value.name,
      baseUrl: selfHosted.value ? form.value.baseUrl : null,
      token: form.value.token || null,
      username: form.value.username || null,
    }
    const saved = isEdit.value
      ? await updateConnection(props.orgId, props.connection.id, payload)
      : await createConnection(props.orgId, payload)
    emit('saved', saved)
    emit('close')
  } catch (e) {
    // axios interceptor toast gösterir; modal içinde de bırakıyoruz
    error.value = e?.response?.data?.error || 'Bağlantı kaydedilemedi. Token ve adresi kontrol edin.'
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
