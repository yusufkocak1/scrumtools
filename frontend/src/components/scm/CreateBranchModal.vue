<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-2xl p-6 w-full max-w-lg shadow-2xl border border-gray-200">
      <div class="flex items-center gap-3 mb-5">
        <div class="w-10 h-10 bg-gradient-to-br from-orange-400 to-rose-500 rounded-xl flex items-center justify-center">
          <svg class="w-5 h-5 text-white" fill="currentColor" viewBox="0 0 16 16">
            <path d="M9.5 3.25a2.25 2.25 0 113 2.122V6A2.5 2.5 0 0110 8.5H6a1 1 0 00-1 1v1.128a2.251 2.251 0 11-1.5 0V5.372a2.25 2.25 0 111.5 0v1.836A2.492 2.492 0 016 7h4a1 1 0 001-1v-.628A2.25 2.25 0 019.5 3.25z"/>
          </svg>
        </div>
        <div>
          <h4 class="text-lg font-semibold text-gray-900">Branch Aç</h4>
          <p v-if="taskKey" class="text-xs text-gray-400 font-mono">{{ taskKey }}</p>
        </div>
      </div>

      <form @submit.prevent="save" class="space-y-4">
        <!-- Repo seçimi -->
        <div>
          <label class="label">Repo</label>
          <select v-model="form.repositoryId" required class="input-field">
            <option v-for="repo in repos" :key="repo.id" :value="repo.id">
              {{ repo.fullName }} ({{ repo.provider }})
            </option>
          </select>
        </div>

        <!-- Kaynak branch -->
        <div>
          <label class="label">Kaynak Branch</label>
          <input
            v-model="form.sourceRef"
            type="text"
            required
            class="input-field font-mono"
            list="source-branches"
            placeholder="main"
          />
          <datalist id="source-branches">
            <option v-for="b in remoteBranches" :key="b.name" :value="b.name" />
          </datalist>
        </div>

        <!-- Branch adı -->
        <div>
          <label class="label">Branch Adı</label>
          <input
            v-model="form.branchName"
            type="text"
            required
            class="input-field font-mono"
            placeholder="feature/SCRM-12-kisa-aciklama"
          />
          <p class="text-xs text-gray-400 mt-1">
            Adında {{ taskKey || 'task anahtarı' }} geçen branch'ler push'ta bu task'a otomatik bağlanır.
          </p>
        </div>

        <!-- Kişisel hesap bilgilendirmesi -->
        <p v-if="!hasUserAccount" class="text-xs text-amber-700 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2">
          Kişisel Git hesabınız bağlı değil — branch, organizasyon bağlantısının hesabıyla açılır.
          <router-link to="/profile" class="underline font-medium">Profilinizden bağlayın</router-link>
          ki branch sağlayıcıda sizin adınıza açılsın.
        </p>

        <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
          {{ error }}
        </p>

        <div class="flex gap-2 justify-end pt-2">
          <button type="button" @click="$emit('close')" class="btn-secondary">İptal</button>
          <button type="submit" :disabled="saving || !form.repositoryId" class="btn-primary">
            {{ saving ? 'Oluşturuluyor...' : 'Branch Oluştur' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { createTaskBranch, getRepoBranches } from '../../api/ScmApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  taskId: { type: String, required: true },
  projectId: { type: String, default: null },
  taskKey: { type: String, default: null },
  taskTitle: { type: String, default: '' },
  repos: { type: Array, required: true },
  hasUserAccount: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'created'])

const saving = ref(false)
const error = ref('')
const remoteBranches = ref([])

const form = ref({
  repositoryId: props.repos[0]?.id || null,
  sourceRef: props.repos[0]?.defaultBranch || 'main',
  branchName: suggestBranchName(props.taskKey, props.taskTitle),
})

const selectedRepo = computed(() =>
  props.repos.find(r => r.id === form.value.repositoryId))

watch(() => form.value.repositoryId, () => {
  form.value.sourceRef = selectedRepo.value?.defaultBranch || 'main'
  loadRemoteBranches()
})

onMounted(loadRemoteBranches)

/** Örn: feature/SCRM-12-login-sayfasi-hatasi (Türkçe karakterler sadeleştirilir). */
function suggestBranchName(taskKey, title) {
  const slug = (title || '')
    .replace(/[çÇ]/g, 'c').replace(/[ğĞ]/g, 'g').replace(/[ıİ]/g, 'i')
    .replace(/[öÖ]/g, 'o').replace(/[şŞ]/g, 's').replace(/[üÜ]/g, 'u')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 40)
    .replace(/-+$/g, '')
  const parts = ['feature', [taskKey, slug].filter(Boolean).join('-')]
  return taskKey || slug ? parts.join('/') : ''
}

async function loadRemoteBranches() {
  remoteBranches.value = []
  if (!props.projectId || !form.value.repositoryId) return
  try {
    remoteBranches.value = await getRepoBranches(props.projectId, form.value.repositoryId)
  } catch {
    // canlı liste çekilemezse datalist boş kalır; ad elle yazılabilir
  }
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    const branch = await createTaskBranch(props.teamId, props.taskId, {
      repositoryId: form.value.repositoryId,
      branchName: form.value.branchName,
      sourceRef: form.value.sourceRef,
    })
    emit('created', branch)
    emit('close')
  } catch (e) {
    error.value = e?.response?.data?.error || 'Branch oluşturulamadı.'
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
