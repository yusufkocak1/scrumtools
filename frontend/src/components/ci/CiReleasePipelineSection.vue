<template>
  <!-- Projede enabled RELEASE_PIPELINE eşlemesi yoksa bölüm hiç görünmez -->
  <div v-if="visible" class="mt-5">
    <div class="flex items-center justify-between mb-2">
      <h4 class="text-[11px] font-semibold text-gray-400 uppercase tracking-wider flex items-center gap-1.5">
        <svg class="w-3.5 h-3.5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/>
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        Release Pipeline
      </h4>
      <button
        @click="openConfirm"
        :disabled="!view.canRun"
        :title="view.blockedReason || 'Pipeline çalıştır'"
        class="text-xs font-medium text-white bg-purple-600 hover:bg-purple-700 disabled:opacity-40 disabled:cursor-not-allowed rounded-lg px-2.5 py-1 transition-colors flex items-center gap-1"
      >
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/>
        </svg>
        Pipeline Çalıştır
      </button>
    </div>

    <p v-if="!view.canRun && view.blockedReason" class="text-[11px] text-gray-400 mb-2">
      {{ view.blockedReason }}
    </p>

    <CiBuildHistory :builds="view.builds" @updated="onBuildUpdated" />

    <!-- Onay modalı -->
    <div v-if="showConfirm" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 w-full max-w-lg shadow-2xl border border-gray-200">
        <h4 class="text-lg font-semibold text-gray-900 mb-4">Pipeline çalıştır</h4>

        <div class="space-y-4">
          <!-- Job seçimi -->
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

          <!-- Otomatik geçiş uyarısı -->
          <div v-if="selectedMapping?.autoTransitionOnSuccess"
            class="flex items-start gap-2 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2.5">
            <svg class="w-4 h-4 text-amber-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
            </svg>
            <span class="text-xs text-gray-600">
              Bu job <span class="font-medium text-gray-800">başarılı olursa release otomatik RELEASED'a geçecek</span>.
            </span>
          </div>

          <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
            {{ error }}
          </p>

          <div class="flex gap-2 justify-end pt-1">
            <button type="button" @click="showConfirm = false" class="btn-secondary">İptal</button>
            <button type="button" @click="run" :disabled="running" class="btn-primary">
              {{ running ? 'Çalıştırılıyor...' : 'Çalıştır' }}
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
import { getReleasePipelineView, getReleaseBuilds, runReleasePipeline } from '../../api/CiApi.js'

const props = defineProps({
  releaseId: { type: String, required: true },
  releaseName: { type: String, default: '' },
})
const emit = defineEmits(['released'])

const view = ref({ featureEnabled: false, releaseStatus: null, canRun: false, blockedReason: null, mappings: [], builds: [] })
const loaded = ref(false)

const showConfirm = ref(false)
const selectedMappingId = ref('')
const running = ref(false)
const error = ref('')

let pollTimer = null

const visible = computed(() => loaded.value && view.value.mappings.length > 0)

const selectedMapping = computed(() =>
  view.value.mappings.find(m => m.id === selectedMappingId.value) || view.value.mappings[0])

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
  return value.replace(/\{\{\s*releaseName\s*}}/g, props.releaseName || view.value.releaseName || '{{releaseName}}')
}

function envLabel(env) {
  return { TEST: 'Test', STAGING: 'Staging', PROD: 'Prod' }[env] || env
}

async function load() {
  try {
    view.value = await getReleasePipelineView(props.releaseId)
  } catch (e) {
    console.error('Pipeline bölümü yüklenemedi:', e)
  } finally {
    loaded.value = true
    syncPolling()
  }
}

function openConfirm() {
  error.value = ''
  selectedMappingId.value = selectedMapping.value?.id || ''
  showConfirm.value = true
}

async function run() {
  if (!selectedMappingId.value) { error.value = 'Job seçilmeli.'; return }
  running.value = true
  error.value = ''
  try {
    const build = await runReleasePipeline(props.releaseId, { mappingId: selectedMappingId.value })
    view.value.builds.unshift(build)
    showConfirm.value = false
    createToast('Pipeline tetiklendi.', { type: 'success', position: 'top-center', timeout: 3000 })
    syncPolling()
  } catch (e) {
    error.value = e?.response?.data?.error || 'Pipeline tetiklenemedi.'
  } finally {
    running.value = false
  }
}

function onBuildUpdated(updated) {
  const idx = view.value.builds.findIndex(b => b.id === updated.id)
  const prevStatus = idx !== -1 ? view.value.builds[idx].status : null
  if (idx !== -1) view.value.builds[idx] = updated
  // Otomatik geçiş sonucu release RELEASED olmuş olabilir → üst bileşen tazelesin
  if (prevStatus !== 'SUCCESS' && updated.status === 'SUCCESS' && selectedMapping.value?.autoTransitionOnSuccess) {
    emit('released')
  }
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
    view.value.builds = await getReleaseBuilds(props.releaseId)
  } catch (e) {
    console.error('Build tarihçesi tazelenemedi:', e)
  } finally {
    syncPolling()
  }
}

watch(() => props.releaseId, load)
onMounted(load)
onBeforeUnmount(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors; }
.label { @apply block text-sm font-medium text-gray-700 mb-1.5; }
.btn-primary { @apply px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50 transition-colors font-medium text-sm; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium text-sm; }
</style>
