<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-2xl p-6 w-full max-w-lg shadow-2xl border border-gray-200">
      <div class="flex items-center gap-3 mb-5">
        <div class="w-10 h-10 bg-gradient-to-br from-indigo-400 to-violet-500 rounded-xl flex items-center justify-center">
          <svg class="w-5 h-5 text-white" fill="currentColor" viewBox="0 0 16 16">
            <path d="M7.177 3.073L9.573.677A.25.25 0 0110 .854v4.792a.25.25 0 01-.427.177L7.177 3.427a.25.25 0 010-.354zM3.75 2.5a.75.75 0 100 1.5.75.75 0 000-1.5zm-2.25.75a2.25 2.25 0 113 2.122v5.256a2.251 2.251 0 11-1.5 0V5.372A2.25 2.25 0 011.5 3.25zM11 2.5h-1V4h1a1 1 0 011 1v5.628a2.251 2.251 0 101.5 0V5A2.5 2.5 0 0011 2.5zm1 10.25a.75.75 0 111.5 0 .75.75 0 01-1.5 0zM3.75 12a.75.75 0 100 1.5.75.75 0 000-1.5z"/>
          </svg>
        </div>
        <div>
          <h4 class="text-lg font-semibold text-gray-900">{{ providerLabel }} Aç</h4>
          <p v-if="taskKey" class="text-xs text-gray-400 font-mono">{{ taskKey }}</p>
        </div>
      </div>

      <form @submit.prevent="save" class="space-y-4">
        <!-- Kaynak branch -->
        <div>
          <label class="label">Kaynak Branch</label>
          <select v-model="form.branchId" required class="input-field font-mono">
            <option v-for="b in selectableBranches" :key="b.id" :value="b.id">
              {{ b.name }} — {{ b.repositoryName }}
            </option>
          </select>
          <p v-if="openPrForBranch" class="text-xs text-amber-600 mt-1">
            Bu branch için zaten açık bir {{ providerLabel }} var:
            <a :href="openPrForBranch.webUrl || '#'" target="_blank" rel="noopener" class="underline">
              #{{ openPrForBranch.externalId }}
            </a>
          </p>
        </div>

        <!-- Hedef branch -->
        <div>
          <label class="label">Hedef Branch</label>
          <select
            v-if="targetOptions.length"
            v-model="form.targetBranch"
            required
            class="input-field font-mono"
          >
            <option v-for="name in targetOptions" :key="name" :value="name">{{ name }}</option>
          </select>
          <input
            v-else
            v-model="form.targetBranch"
            type="text"
            required
            class="input-field font-mono"
            placeholder="main"
          />
        </div>

        <!-- Başlık -->
        <div>
          <label class="label">Başlık</label>
          <input v-model="form.title" type="text" required maxlength="500" class="input-field" />
        </div>

        <!-- Açıklama -->
        <div>
          <label class="label">Açıklama</label>
          <textarea
            v-model="form.description"
            rows="3"
            class="input-field resize-none"
            placeholder="Boş bırakılırsa göreve link eklenir."
          ></textarea>
        </div>

        <label class="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
          <input v-model="form.draft" type="checkbox" class="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" />
          Taslak olarak aç
        </label>

        <!-- Kişisel hesap bilgilendirmesi -->
        <p v-if="!hasUserAccount" class="text-xs text-amber-700 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2">
          Kişisel Git hesabınız bağlı değil — {{ providerLabel }} organizasyon bağlantısının hesabıyla açılır.
          <router-link to="/profile" class="underline font-medium">Profilinizden bağlayın</router-link>
          ki sağlayıcıda sizin adınıza görünsün.
        </p>

        <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
          {{ error }}
        </p>

        <div class="flex gap-2 justify-end pt-2">
          <button type="button" @click="$emit('close')" class="btn-secondary">İptal</button>
          <button type="submit" :disabled="saving || !form.branchId || !form.targetBranch" class="btn-primary">
            {{ saving ? 'Açılıyor...' : providerLabel + ' Aç' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
/**
 * Göreve bağlı bir branch'ten pull request açma modalı.
 * Silinmiş branch'ler seçilemez; hedef branch listesi sağlayıcıdan canlı çekilir,
 * çekilemezse elle yazılabilir (branch açma modalıyla aynı davranış).
 */
import { ref, computed, watch, onMounted } from 'vue'
import { createTaskPullRequest, getRepoBranches } from '../../api/ScmApi.js'

const props = defineProps({
  teamId: { type: String, required: true },
  taskId: { type: String, required: true },
  projectId: { type: String, default: null },
  taskKey: { type: String, default: null },
  taskTitle: { type: String, default: '' },
  branches: { type: Array, required: true },
  pullRequests: { type: Array, default: () => [] },
  repos: { type: Array, default: () => [] },
  preselectedBranchId: { type: String, default: null },
  hasUserAccount: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'created'])

const saving = ref(false)
const error = ref('')
const remoteBranches = ref([])

const selectableBranches = computed(() =>
  props.branches.filter(b => b.status !== 'DELETED'))

const initialBranch = computed(() =>
  selectableBranches.value.find(b => b.id === props.preselectedBranchId)
    || selectableBranches.value[0]
    || null)

const form = ref({
  branchId: initialBranch.value?.id || null,
  targetBranch: initialBranch.value?.repositoryDefaultBranch || 'main',
  title: [props.taskKey, props.taskTitle].filter(Boolean).join(' '),
  description: '',
  draft: false,
})

const selectedBranch = computed(() =>
  selectableBranches.value.find(b => b.id === form.value.branchId))

/** GitLab'da kavram "merge request" — etiketler seçili repoya göre değişir. */
const providerLabel = computed(() => {
  const repo = props.repos.find(r => r.id === selectedBranch.value?.repositoryId)
  return repo?.provider === 'GITLAB' ? 'Merge Request' : 'Pull Request'
})

const openPrForBranch = computed(() =>
  props.pullRequests.find(pr => pr.branchId === form.value.branchId && pr.state === 'OPEN'))

/** Kaynak branch'in kendisi hedef olamaz. */
const targetOptions = computed(() =>
  remoteBranches.value.map(b => b.name).filter(name => name !== selectedBranch.value?.name))

watch(() => form.value.branchId, () => {
  form.value.targetBranch = selectedBranch.value?.repositoryDefaultBranch || 'main'
  loadRemoteBranches()
})

onMounted(loadRemoteBranches)

async function loadRemoteBranches() {
  remoteBranches.value = []
  const repositoryId = selectedBranch.value?.repositoryId
  if (!props.projectId || !repositoryId) return
  try {
    remoteBranches.value = await getRepoBranches(props.projectId, repositoryId)
    if (targetOptions.value.length && !targetOptions.value.includes(form.value.targetBranch)) {
      const fallback = selectedBranch.value?.repositoryDefaultBranch
      form.value.targetBranch = targetOptions.value.includes(fallback)
        ? fallback
        : targetOptions.value[0]
    }
  } catch {
    // canlı liste çekilemezse select yerine input gösterilir; ad elle yazılabilir
  }
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    const pr = await createTaskPullRequest(props.teamId, props.taskId, {
      branchId: form.value.branchId,
      targetBranch: form.value.targetBranch,
      title: form.value.title,
      description: form.value.description,
      draft: form.value.draft,
    })
    emit('created', pr)
    emit('close')
  } catch (e) {
    error.value = e?.response?.data?.error || `${providerLabel.value} açılamadı.`
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
