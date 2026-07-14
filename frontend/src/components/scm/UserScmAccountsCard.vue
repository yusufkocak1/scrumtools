<template>
  <div class="bg-white rounded-xl shadow border border-gray-200">
    <div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
      <div>
        <h3 class="font-semibold text-gray-900">Git Hesaplarım</h3>
        <p class="text-sm text-gray-500 mt-0.5">
          Kişisel erişim token'ınızı (PAT) bağlayın — task'tan açtığınız branch'ler sağlayıcıda sizin adınıza açılır
        </p>
      </div>
      <button v-if="!showForm" @click="openForm" class="btn-primary flex-shrink-0">Hesap Bağla</button>
    </div>

    <div class="p-6 space-y-4">
      <!-- Bağlı hesaplar -->
      <p v-if="!loading && accounts.length === 0 && !showForm" class="text-sm text-gray-400">
        Henüz bağlı Git hesabınız yok.
      </p>
      <ul v-if="accounts.length > 0" class="space-y-2">
        <li
          v-for="account in accounts"
          :key="account.id"
          class="flex items-center gap-3 px-3 py-2.5 rounded-lg border border-gray-100 bg-gray-50/50"
        >
          <span v-html="providerIcon(account.provider)" class="w-5 h-5 text-gray-700 flex-shrink-0"></span>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-gray-800 truncate">
              {{ account.username || account.provider }}
              <span class="text-gray-400 font-normal font-mono text-xs ml-1">{{ account.tokenHint }}</span>
            </p>
            <p class="text-xs text-gray-400 truncate">
              {{ account.provider }}<span v-if="account.baseUrl"> · {{ account.baseUrl }}</span>
            </p>
          </div>
          <span
            :class="[
              'text-[11px] px-2 py-0.5 rounded-full font-medium flex-shrink-0',
              account.status === 'ACTIVE' ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-600'
            ]"
          >{{ account.status === 'ACTIVE' ? 'Aktif' : 'Token Geçersiz' }}</span>
          <button
            @click="removeAccount(account)"
            class="text-gray-300 hover:text-red-500 transition-colors flex-shrink-0"
            title="Hesabı kaldır"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
            </svg>
          </button>
        </li>
      </ul>

      <!-- Bağlama formu -->
      <form v-if="showForm" @submit.prevent="save" class="space-y-4 border border-gray-100 rounded-xl p-4 bg-gray-50/50">
        <div>
          <label class="label">Sağlayıcı</label>
          <div class="grid grid-cols-2 gap-2">
            <button
              v-for="p in providers"
              :key="p.id"
              type="button"
              @click="form.provider = p.id"
              :class="[
                'flex items-center gap-2.5 px-3 py-2.5 rounded-lg border text-sm font-medium transition-all bg-white',
                form.provider === p.id
                  ? 'border-indigo-500 bg-indigo-50 text-indigo-700 ring-1 ring-indigo-500'
                  : 'border-gray-200 text-gray-700 hover:border-gray-300'
              ]"
            >
              <span v-html="p.icon" class="w-5 h-5 flex-shrink-0"></span>
              <span>{{ p.label }}</span>
            </button>
          </div>
        </div>

        <div>
          <label class="flex items-center gap-2 mb-1.5">
            <input v-model="selfHosted" type="checkbox" class="rounded border-gray-300" />
            <span class="text-sm font-medium text-gray-700">Self-hosted kurulum</span>
          </label>
          <input v-if="selfHosted" v-model="form.baseUrl" type="url" class="input-field font-mono"
            :placeholder="form.provider === 'GITLAB' ? 'https://gitlab.sirket.com' : 'https://github.sirket.com'" />
        </div>

        <div>
          <label class="label">Erişim Token'ı (PAT)</label>
          <input v-model="form.token" type="password" required class="input-field font-mono"
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

        <div class="flex gap-2 justify-end">
          <button type="button" @click="showForm = false" class="btn-secondary">İptal</button>
          <button type="submit" :disabled="saving" class="btn-primary">
            {{ saving ? 'Doğrulanıyor...' : 'Doğrula ve Bağla' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyScmAccounts, connectMyScmAccount, deleteMyScmAccount } from '../../api/ScmApi.js'

const GITHUB_ICON = '<svg viewBox="0 0 16 16" fill="currentColor"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0016 8c0-4.42-3.58-8-8-8z"/></svg>'
const GITLAB_ICON = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M23.6 9.6l-1.2-3.7L20 .9c-.1-.4-.7-.4-.8 0l-2.4 5H7.2L4.8.9c-.1-.4-.7-.4-.8 0L1.6 5.9.4 9.6c-.1.3 0 .7.3.9l11.3 8.2 11.3-8.2c.3-.2.4-.6.3-.9z"/></svg>'

const providers = [
  { id: 'GITHUB', label: 'GitHub', icon: GITHUB_ICON },
  { id: 'GITLAB', label: 'GitLab', icon: GITLAB_ICON },
]

const accounts = ref([])
const loading = ref(true)
const showForm = ref(false)
const saving = ref(false)
const error = ref('')
const selfHosted = ref(false)

const form = ref({ provider: 'GITHUB', baseUrl: '', token: '' })

onMounted(load)

async function load() {
  loading.value = true
  try {
    accounts.value = await getMyScmAccounts()
  } catch (e) {
    console.error('Git hesapları yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

function openForm() {
  form.value = { provider: 'GITHUB', baseUrl: '', token: '' }
  selfHosted.value = false
  error.value = ''
  showForm.value = true
}

function providerIcon(provider) {
  return providers.find(p => p.id === provider)?.icon || GITHUB_ICON
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    await connectMyScmAccount({
      provider: form.value.provider,
      baseUrl: selfHosted.value ? form.value.baseUrl : null,
      token: form.value.token,
    })
    showForm.value = false
    await load()
  } catch (e) {
    error.value = e?.response?.data?.error || 'Hesap bağlanamadı. Token\'ı kontrol edin.'
  } finally {
    saving.value = false
  }
}

async function removeAccount(account) {
  if (!confirm(`${account.provider} hesabınızın bağlantısı kaldırılsın mı?`)) return
  try {
    await deleteMyScmAccount(account.id)
    accounts.value = accounts.value.filter(a => a.id !== account.id)
  } catch (e) {
    console.error('Hesap silinemedi:', e)
  }
}
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors; }
.label { @apply block text-sm font-medium text-gray-700 mb-1.5; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium text-sm; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium text-sm; }
</style>
