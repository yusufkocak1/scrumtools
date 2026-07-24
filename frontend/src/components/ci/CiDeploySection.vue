<template>
  <!-- Projede enabled TASK_DEPLOY eşlemesi yoksa bölüm hiç görünmez -->
  <div
    v-if="visible"
    class="bg-white rounded-xl border border-gray-200/80 shadow-sm overflow-hidden"
  >
    <div class="h-1 w-full bg-gradient-to-r from-blue-400 to-indigo-500"></div>
    <div class="p-5">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-base font-semibold text-gray-800 flex items-center gap-2">
          <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01"/>
          </svg>
          Deploy
        </h3>
        <button
          @click="openModal"
          :disabled="!view.canDeploy"
          :title="deployDisabledReason"
          class="text-xs font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed rounded-lg px-2.5 py-1 transition-colors flex items-center gap-1"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
          </svg>
          Test ortamına deploy
        </button>
      </div>

      <CiBuildHistory :builds="view.builds" @updated="onBuildUpdated" />
    </div>

    <!-- Deploy modalı -->
    <div v-if="showModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 w-full max-w-lg shadow-2xl border border-gray-200">
        <h4 class="text-lg font-semibold text-gray-900 mb-4">Test ortamına deploy</h4>

        <div class="space-y-4">
          <!-- Job seçimi (birden çoksa) -->
          <div v-if="view.mappings.length > 1">
            <label class="label">Job</label>
            <select v-model="selectedMappingId" class="input-field">
              <option v-for="m in view.mappings" :key="m.id" :value="m.id">
                {{ m.displayName }} ({{ envLabel(m.environment) }})
              </option>
            </select>
          </div>
          <div v-else class="text-sm text-gray-600">
            <span class="font-medium">{{ selectedMapping?.displayName }}</span>
            <span class="text-gray-400"> · {{ envLabel(selectedMapping?.environment) }}</span>
          </div>

          <!-- Branch -->
          <div>
            <label class="label">Branch</label>
            <input v-model="branch" type="text" class="input-field font-mono" list="ci-branch-suggestions"
              placeholder="deploy edilecek branch (örn: main)" />
            <datalist id="ci-branch-suggestions">
              <option v-for="b in branchSuggestions" :key="b" :value="b" />
            </datalist>
          </div>

          <!-- Çözülmüş parametre önizlemesi -->
          <div v-if="paramPreview.length">
            <label class="label">Parametreler</label>
            <div class="bg-gray-50 border border-gray-200 rounded-lg p-3 space-y-1 max-h-40 overflow-y-auto">
              <div v-for="p in paramPreview" :key="p.name" class="flex items-center gap-2 text-xs font-mono">
                <span class="text-gray-500">{{ p.name }}</span>
                <span class="text-gray-300">=</span>
                <span class="text-gray-800 truncate">{{ p.value }}</span>
              </div>
            </div>
            <p v-if="hasServerVars" class="text-[11px] text-gray-400 mt-1">{{ serverVarsNote }}</p>
          </div>

          <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
            {{ error }}
          </p>

          <div class="flex gap-2 justify-end pt-1">
            <button type="button" @click="showModal = false" class="btn-secondary">İptal</button>
            <button type="button" @click="trigger" :disabled="triggering" class="btn-primary">
              {{ triggering ? 'Tetikleniyor...' : 'Tetikle' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import CiBuildHistory from './CiBuildHistory.vue'
import { getTaskDeployView, getTaskBuilds, deployTask } from '../../api/CiApi.js'
import { getTaskScm } from '../../api/ScmApi.js'

const props = defineProps({
  taskId: { type: String, required: true },
  teamId: { type: String, default: null },
  taskKey: { type: String, default: '' },
  taskTitle: { type: String, default: '' },
})

const view = ref({ featureEnabled: false, projectId: null, canDeploy: false, mappings: [], builds: [] })
const loaded = ref(false)

const showModal = ref(false)
const selectedMappingId = ref('')
const branch = ref('')
const branchSuggestions = ref([])
const triggering = ref(false)
const error = ref('')

let pollTimer = null

// Bölüm yalnız eşleme varken görünür (feature kapalı + downgrade'de eşleme kalmışsa da bilgi amaçlı görünür)
const visible = computed(() => loaded.value && view.value.mappings.length > 0)

const selectedMapping = computed(() =>
  view.value.mappings.find(m => m.id === selectedMappingId.value) || view.value.mappings[0])

const deployDisabledReason = computed(() => {
  if (view.value.canDeploy) return 'Test ortamına deploy tetikle'
  if (!view.value.featureEnabled) return 'Paketinizde CI/CD entegrasyonu kapalı.'
  return 'Deploy için uygun job eşlemesi yok.'
})

// Basit istemci-taraflı önizleme; sunucuda çözülen değişkenler olduğu gibi bırakılır
const paramPreview = computed(() => {
  const tpl = selectedMapping.value?.parameterTemplate
  if (!tpl) return []
  let obj
  try { obj = JSON.parse(tpl) } catch { return [] }
  return Object.entries(obj).map(([name, value]) => ({ name, value: substitute(String(value)) }))
})

const hasServerVars = computed(() =>
  paramPreview.value.some(p => /\{\{\s*(projectKey|triggeredBy)\s*}}/.test(p.value)))
const serverVarsNote = '{{projectKey}} ve {{triggeredBy}} gibi değişkenler tetikleme anında sunucuda çözülür.'

function substitute(value) {
  return value
    .replace(/\{\{\s*branch\s*}}/g, branch.value || '{{branch}}')
    .replace(/\{\{\s*taskKey\s*}}/g, props.taskKey || '{{taskKey}}')
    .replace(/\{\{\s*taskTitle\s*}}/g, props.taskTitle || '{{taskTitle}}')
}

function envLabel(env) {
  return { TEST: 'Test', STAGING: 'Staging', PROD: 'Prod' }[env] || env
}

async function load() {
  try {
    view.value = await getTaskDeployView(props.taskId)
  } catch (e) {
    console.error('Deploy bölümü yüklenemedi:', e)
  } finally {
    loaded.value = true
    syncPolling()
  }
}

async function openModal() {
  error.value = ''
  selectedMappingId.value = selectedMapping.value?.id || ''
  branch.value = ''
  showModal.value = true
  loadBranchSuggestions()
}

async function loadBranchSuggestions() {
  if (!props.teamId) return
  try {
    const scm = await getTaskScm(props.teamId, props.taskId)
    const names = new Set()
    ;(scm.branches || []).forEach(b => b.name && names.add(b.name))
    ;(scm.repos || []).forEach(r => r.defaultBranch && names.add(r.defaultBranch))
    branchSuggestions.value = [...names]
    if (!branch.value && scm.repos?.[0]?.defaultBranch) {
      branch.value = scm.repos[0].defaultBranch
    }
  } catch {
    // SCM verisi yoksa serbest metin yeterli
  }
}

async function trigger() {
  if (!selectedMappingId.value) { error.value = 'Job seçilmeli.'; return }
  triggering.value = true
  error.value = ''
  try {
    const build = await deployTask(props.taskId, {
      mappingId: selectedMappingId.value,
      branch: branch.value || null,
    })
    view.value.builds.unshift(build)
    showModal.value = false
    createToast('Deploy tetiklendi.', { type: 'success', position: 'top-center', timeout: 3000 })
    syncPolling()
  } catch (e) {
    error.value = e?.response?.data?.error || 'Deploy tetiklenemedi.'
  } finally {
    triggering.value = false
  }
}

function onBuildUpdated(updated) {
  const idx = view.value.builds.findIndex(b => b.id === updated.id)
  if (idx !== -1) view.value.builds[idx] = updated
  syncPolling()
}

// ─── Aktif build oldukça 10 sn'de bir tazele ─────────────────────────────────
const TERMINAL = ['SUCCESS', 'FAILURE', 'UNSTABLE', 'ABORTED', 'LOST']
function hasActiveBuild() {
  return view.value.builds.some(b => !TERMINAL.includes(b.status))
}

function syncPolling() {
  if (hasActiveBuild() && !pollTimer) {
    pollTimer = setInterval(pollBuilds, 10000)
  } else if (!hasActiveBuild() && pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function pollBuilds() {
  try {
    view.value.builds = await getTaskBuilds(props.taskId)
  } catch (e) {
    console.error('Build tarihçesi tazelenemedi:', e)
  } finally {
    syncPolling()
  }
}

watch(() => props.taskId, load)
onMounted(load)
onBeforeUnmount(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors; }
.label { @apply block text-sm font-medium text-gray-700 mb-1.5; }
.btn-primary { @apply px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors font-medium text-sm; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium text-sm; }
</style>
