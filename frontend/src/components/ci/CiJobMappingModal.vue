<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-2xl p-6 w-full max-w-2xl shadow-2xl border border-gray-200 max-h-[90vh] overflow-y-auto">
      <div class="flex items-center gap-3 mb-5">
        <div class="w-10 h-10 bg-gray-900 rounded-xl flex items-center justify-center">
          <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
        </div>
        <h4 class="text-lg font-semibold text-gray-900">
          {{ isEdit ? 'Job Eşlemesini Düzenle' : 'Job Eşle' }}
        </h4>
      </div>

      <form @submit.prevent="save" class="space-y-4">
        <!-- Bağlantı + Job seçimi (yalnız yeni eklemede; kimlik alanları) -->
        <template v-if="!isEdit">
          <div>
            <label class="label">CI/CD Bağlantısı</label>
            <p v-if="connectionsError" class="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
              {{ connectionsError }}
            </p>
            <select v-else v-model="form.connectionId" @change="onConnectionChange" class="input-field">
              <option value="" disabled>Bağlantı seçin</option>
              <option v-for="c in connections" :key="c.id" :value="c.id">
                {{ c.name }} <template v-if="c.status !== 'ACTIVE'">({{ c.status }})</template>
              </option>
            </select>
          </div>

          <!-- Job arama -->
          <div v-if="form.connectionId">
            <label class="label">Job</label>
            <input
              v-model="jobSearch"
              @input="debouncedJobSearch"
              type="text"
              class="input-field"
              placeholder="job adı yazın (folder'lar dahil aranır)..."
            />
            <div v-if="jobsLoading" class="text-center py-3 text-xs text-gray-400">Aranıyor...</div>
            <div v-else-if="jobs.length" class="mt-2 max-h-48 overflow-y-auto border border-gray-200 rounded-lg divide-y divide-gray-100 bg-white">
              <button
                v-for="j in jobs"
                :key="j.fullName"
                type="button"
                @click="selectJob(j)"
                :disabled="j.mapped"
                :class="[
                  'w-full flex items-center gap-2 px-3 py-2 text-left transition-colors',
                  form.jobFullName === j.fullName ? 'bg-indigo-50' : 'hover:bg-gray-50',
                  j.mapped ? 'opacity-50 cursor-not-allowed' : ''
                ]"
              >
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-mono text-gray-800 truncate">{{ j.fullName }}</p>
                  <p v-if="j.displayName !== j.fullName" class="text-[11px] text-gray-400 truncate">{{ j.displayName }}</p>
                </div>
                <span v-if="j.mapped" class="text-[11px] text-gray-400 flex-shrink-0">eşlenmiş</span>
                <span v-else-if="!j.buildable" class="text-[11px] text-amber-500 flex-shrink-0">tetiklenemez</span>
                <svg v-else-if="form.jobFullName === j.fullName" class="w-4 h-4 text-indigo-600 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
              </button>
            </div>
            <p v-else-if="jobsSearched" class="text-xs text-gray-400 mt-2 text-center py-2">Sonuç bulunamadı.</p>
          </div>
        </template>

        <!-- Düzenlemede kimlik alanları salt-okunur -->
        <div v-else class="text-sm text-gray-600 bg-gray-50 border border-gray-200 rounded-lg px-3 py-2">
          <p class="font-mono text-gray-800">{{ mapping.jobFullName }}</p>
          <p class="text-xs text-gray-400 mt-0.5">{{ mapping.connectionName }} — bağlantı ve job değiştirilemez</p>
        </div>

        <!-- Görünen ad -->
        <div v-if="form.jobFullName || isEdit">
          <label class="label">Görünen Ad</label>
          <input v-model="form.displayName" type="text" class="input-field"
            placeholder="Örn: Test ortamına deploy" />
        </div>

        <!-- Tip + Ortam -->
        <div v-if="form.jobFullName || isEdit" class="grid grid-cols-2 gap-3">
          <div>
            <label class="label">Kullanım</label>
            <select v-model="form.jobType" class="input-field">
              <option value="TASK_DEPLOY">Task Deploy</option>
              <option value="RELEASE_PIPELINE">Release Pipeline</option>
            </select>
          </div>
          <div>
            <label class="label">Ortam</label>
            <select v-model="form.environment" class="input-field">
              <option value="TEST">Test</option>
              <option value="STAGING">Staging</option>
              <option value="PROD">Prod</option>
            </select>
          </div>
        </div>

        <!-- Parametre şablonu -->
        <div v-if="form.jobFullName || isEdit">
          <div class="flex items-center justify-between mb-1.5">
            <label class="label mb-0">Parametreler</label>
            <button v-if="!isEdit && form.jobFullName" type="button" @click="loadJobParams"
              :disabled="paramsLoading"
              class="text-xs text-indigo-600 hover:underline disabled:opacity-50">
              {{ paramsLoading ? 'Yükleniyor...' : "Job'ın parametrelerini getir" }}
            </button>
          </div>

          <div v-if="params.length === 0" class="text-xs text-gray-400 border border-dashed border-gray-200 rounded-lg px-3 py-3 text-center">
            Parametre yok. Job parametreliyse "Job'ın parametrelerini getir" ile listeleyin ya da elle ekleyin.
          </div>

          <div v-else class="space-y-2">
            <div v-for="(p, idx) in params" :key="idx" class="flex items-center gap-2">
              <input v-model="p.name" type="text" class="input-field flex-1 font-mono text-xs" placeholder="PARAM_ADI" />
              <span class="text-gray-300">=</span>
              <div class="flex-1 flex items-center gap-1">
                <input v-model="p.value" type="text" class="input-field font-mono text-xs" :placeholder="valuePlaceholder" />
                <select @change="insertVariable(p, $event)" class="input-field w-8 px-1 text-xs cursor-pointer" title="Değişken ekle">
                  <option value="">+</option>
                  <option v-for="v in availableVars" :key="v.key" :value="v.key">{{ v.label }}</option>
                </select>
              </div>
              <button type="button" @click="params.splice(idx, 1)" class="p-1.5 text-gray-400 hover:text-red-600 rounded">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>
            </div>
          </div>

          <button type="button" @click="params.push({ name: '', value: '' })"
            class="mt-2 text-xs text-indigo-600 hover:underline">+ Parametre ekle</button>

          <p class="text-[11px] text-gray-400 mt-2">
            Kullanılabilir değişkenler tetikleme anında çözülür:
            <span class="font-mono">{{ availableVarsHint }}</span>
          </p>
        </div>

        <!-- Otomatik geçiş (yalnız RELEASE_PIPELINE) -->
        <label v-if="form.jobType === 'RELEASE_PIPELINE' && (form.jobFullName || isEdit)"
          class="flex items-start gap-2 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2.5">
          <input v-model="form.autoTransitionOnSuccess" type="checkbox" class="rounded border-gray-300 mt-0.5" />
          <span class="text-xs text-gray-600">
            <span class="font-medium text-gray-800">Build başarılı olunca release'i otomatik RELEASED'a geçir.</span>
            Kapalıysa build sonucunu görüp geçişi elle yaparsınız (önerilen).
          </span>
        </label>

        <!-- Etkin/pasif (yalnız düzenlemede) -->
        <label v-if="isEdit" class="flex items-center gap-2">
          <input v-model="form.enabled" type="checkbox" class="rounded border-gray-300" />
          <span class="text-sm text-gray-700">Etkin (kapatılırsa task/release ekranlarında görünmez)</span>
        </label>

        <p v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
          {{ error }}
        </p>

        <div class="flex gap-2 justify-end pt-2">
          <button type="button" @click="$emit('close')" class="btn-secondary">İptal</button>
          <button type="submit" :disabled="saving || (!isEdit && !form.jobFullName)" class="btn-primary">
            {{ saving ? 'Kaydediliyor...' : (isEdit ? 'Kaydet' : 'Eşle') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  getConnections, getJobs, getJobParameters,
  createJobMapping, updateJobMapping,
} from '../../api/CiApi.js'

const props = defineProps({
  projectId: { type: String, required: true },
  organizationId: { type: String, required: true },
  mapping: { type: Object, default: null },
})
const emit = defineEmits(['close', 'saved'])

const isEdit = computed(() => !!props.mapping)

const ALL_VARS = [
  { key: 'branch', label: 'Branch', types: ['TASK_DEPLOY'] },
  { key: 'taskKey', label: 'Task anahtarı', types: ['TASK_DEPLOY'] },
  { key: 'taskTitle', label: 'Task başlığı', types: ['TASK_DEPLOY'] },
  { key: 'releaseName', label: 'Sürüm adı', types: ['RELEASE_PIPELINE'] },
  { key: 'projectKey', label: 'Proje anahtarı', types: ['TASK_DEPLOY', 'RELEASE_PIPELINE'] },
  { key: 'triggeredBy', label: 'Tetikleyen', types: ['TASK_DEPLOY', 'RELEASE_PIPELINE'] },
]

const form = ref({
  connectionId: props.mapping?.connectionId || '',
  jobFullName: props.mapping?.jobFullName || '',
  displayName: props.mapping?.displayName || '',
  jobType: props.mapping?.jobType || 'TASK_DEPLOY',
  environment: props.mapping?.environment || 'TEST',
  autoTransitionOnSuccess: props.mapping?.autoTransitionOnSuccess || false,
  enabled: props.mapping?.enabled ?? true,
})

const connections = ref([])
const connectionsError = ref('')
const jobSearch = ref('')
const jobs = ref([])
const jobsLoading = ref(false)
const jobsSearched = ref(false)
const params = ref(parseTemplate(props.mapping?.parameterTemplate))
const paramsLoading = ref(false)
const saving = ref(false)
const error = ref('')
let jobSearchTimer = null

const availableVars = computed(() => ALL_VARS.filter(v => v.types.includes(form.value.jobType)))
const availableVarsHint = computed(() => availableVars.value.map(v => `{{${v.key}}}`).join(' '))
const valuePlaceholder = 'değer veya {{değişken}}'

function parseTemplate(template) {
  if (!template) return []
  try {
    const obj = JSON.parse(template)
    return Object.entries(obj).map(([name, value]) => ({ name, value: String(value) }))
  } catch {
    return []
  }
}

async function loadConnections() {
  connectionsError.value = ''
  try {
    connections.value = await getConnections(props.organizationId, { _skipErrorToast: true })
    if (connections.value.length === 0) {
      connectionsError.value = 'Henüz CI/CD bağlantısı yok. Organizasyon ayarlarındaki Entegrasyonlar → CI/CD sekmesinden ekleyin.'
    } else if (connections.value.length === 1) {
      form.value.connectionId = connections.value[0].id
      searchJobs()
    }
  } catch (e) {
    connectionsError.value = e?.response?.status === 403
      ? 'Bağlantıları listelemek için organizasyon yöneticisi yetkisi gerekli.'
      : 'Bağlantılar yüklenemedi.'
  }
}

function onConnectionChange() {
  form.value.jobFullName = ''
  jobs.value = []
  jobsSearched.value = false
  searchJobs()
}

function debouncedJobSearch() {
  clearTimeout(jobSearchTimer)
  jobSearchTimer = setTimeout(searchJobs, 400)
}

async function searchJobs() {
  if (!form.value.connectionId) return
  jobsLoading.value = true
  try {
    jobs.value = await getJobs(props.organizationId, form.value.connectionId, jobSearch.value)
    jobsSearched.value = true
  } catch (e) {
    console.error('Job araması başarısız:', e)
    jobs.value = []
  } finally {
    jobsLoading.value = false
  }
}

function selectJob(job) {
  if (job.mapped) return
  form.value.jobFullName = job.fullName
  if (!form.value.displayName) {
    form.value.displayName = job.displayName || job.fullName
  }
}

async function loadJobParams() {
  if (!form.value.connectionId || !form.value.jobFullName) return
  paramsLoading.value = true
  try {
    const jobParams = await getJobParameters(props.organizationId, form.value.connectionId, form.value.jobFullName)
    // Mevcut satırlarla birleştir — zaten olan parametreleri koru
    const existing = new Set(params.value.map(p => p.name))
    for (const jp of jobParams) {
      if (!existing.has(jp.name)) {
        params.value.push({ name: jp.name, value: jp.defaultValue || '' })
      }
    }
    if (jobParams.length === 0) {
      error.value = ''
    }
  } catch (e) {
    console.error('Job parametreleri alınamadı:', e)
  } finally {
    paramsLoading.value = false
  }
}

function insertVariable(param, event) {
  const key = event.target.value
  event.target.value = ''
  if (!key) return
  param.value = (param.value || '') + `{{${key}}}`
}

function serializeTemplate() {
  const obj = {}
  for (const p of params.value) {
    const name = (p.name || '').trim()
    if (name) obj[name] = p.value ?? ''
  }
  return Object.keys(obj).length ? JSON.stringify(obj) : null
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    const payload = {
      connectionId: form.value.connectionId,
      jobFullName: form.value.jobFullName,
      displayName: form.value.displayName,
      jobType: form.value.jobType,
      environment: form.value.environment,
      parameterTemplate: serializeTemplate(),
      autoTransitionOnSuccess: form.value.jobType === 'RELEASE_PIPELINE' && form.value.autoTransitionOnSuccess,
      enabled: form.value.enabled,
    }
    const saved = isEdit.value
      ? await updateJobMapping(props.projectId, props.mapping.id, payload)
      : await createJobMapping(props.projectId, payload)
    emit('saved', saved)
    emit('close')
  } catch (e) {
    error.value = e?.response?.data?.error || 'Eşleme kaydedilemedi. Alanları kontrol edin.'
  } finally {
    saving.value = false
  }
}

if (!isEdit.value) loadConnections()
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors; }
.label { @apply block text-sm font-medium text-gray-700 mb-1.5; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium text-sm; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium text-sm; }
</style>
