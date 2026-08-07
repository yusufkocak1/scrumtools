<template>
  <aside class="w-80 shrink-0 border-l border-slate-200 bg-white flex flex-col h-full">
    <div class="px-4 py-3 border-b border-slate-100 flex items-center gap-2">
      <h3 class="font-semibold text-slate-800 text-sm">Makrolar</h3>
      <button @click="openEditor(null)"
              class="ml-auto text-xs px-2 py-1 rounded-lg bg-indigo-600 text-white hover:bg-indigo-500 transition">
        Yeni
      </button>
      <button @click="$emit('close')"
              class="p-1 text-slate-400 hover:text-slate-700 transition" title="Kapat">✕</button>
    </div>

    <div class="flex-1 overflow-y-auto">
      <div v-if="loading" class="p-4 text-sm text-slate-400">Yükleniyor…</div>

      <p v-else-if="macros.length === 0" class="p-4 text-sm text-slate-500">
        Henüz makro yok. Makrolar JavaScript'tir — VBA değil (plan K7).
      </p>

      <article v-for="macro in macros" :key="macro.id"
               class="px-4 py-3 border-b border-slate-100">
        <div class="flex items-start gap-2">
          <div class="min-w-0 flex-1">
            <h4 class="text-sm font-medium text-slate-800 truncate">{{ macro.name }}</h4>
            <p v-if="macro.description" class="text-xs text-slate-500 truncate">{{ macro.description }}</p>
          </div>
          <span v-if="!macro.approved"
                class="shrink-0 px-1.5 py-0.5 rounded bg-amber-50 text-amber-700 text-[10px] font-medium"
                title="Onaysız — yalnızca yazarı çalıştırabilir">
            onaysız
          </span>
        </div>

        <div class="flex flex-wrap items-center gap-1.5 mt-2">
          <button @click="requestRun(macro)" :disabled="running"
                  class="px-2 py-1 text-xs rounded-lg bg-slate-800 text-white hover:bg-slate-700 disabled:opacity-50 transition">
            Çalıştır
          </button>
          <button @click="openEditor(macro)"
                  class="px-2 py-1 text-xs rounded-lg text-slate-600 hover:bg-slate-100 transition">
            Düzenle
          </button>
          <button v-if="!macro.approved" @click="approve(macro)"
                  class="px-2 py-1 text-xs rounded-lg text-emerald-700 hover:bg-emerald-50 transition">
            Onayla
          </button>
          <button v-else @click="revoke(macro)"
                  class="px-2 py-1 text-xs rounded-lg text-slate-500 hover:bg-slate-100 transition">
            Onayı kaldır
          </button>
          <button @click="toggleRuns(macro)"
                  class="ml-auto px-2 py-1 text-xs rounded-lg text-slate-500 hover:bg-slate-100 transition">
            Günlük
          </button>
        </div>

        <ul v-if="openRunsFor === macro.id" class="mt-2 space-y-1">
          <li v-if="runs.length === 0" class="text-[11px] text-slate-400">Henüz çalıştırılmadı.</li>
          <li v-for="run in runs" :key="run.id"
              class="text-[11px] flex items-center gap-2 text-slate-500">
            <span :class="STATUS_CLASS[run.status] || 'text-slate-400'">{{ STATUS_LABEL[run.status] }}</span>
            <span class="truncate">{{ run.triggeredByName || '—' }}</span>
            <span class="ml-auto shrink-0">{{ run.durationMs != null ? run.durationMs + ' ms' : '' }}</span>
          </li>
        </ul>
      </article>
    </div>

    <!-- Rıza diyaloğu (plan §9.2): ilk çalıştırmada makronun neye dokunacağı
         açıkça yazılır. Onay tarayıcıda saklanır ve kaynak değişirse düşer. -->
    <div v-if="consentFor"
         class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm px-4"
         @click.self="consentFor = null">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-100">
          <h3 class="font-semibold text-slate-800">“{{ consentFor.name }}” çalıştırılsın mı?</h3>
        </div>
        <div class="p-6 space-y-3">
          <p class="text-sm text-slate-600">
            Bu makro <strong>sizin yetkinizle</strong> çalışacak. Kaynağında bulunanlar:
          </p>
          <ul class="space-y-1.5">
            <li v-for="scope in consentFor.apiScopes" :key="scope"
                class="text-sm text-slate-700 flex items-start gap-2">
              <span class="text-indigo-500 mt-0.5">•</span>
              <span>{{ SCOPE_LABELS[scope] || scope }}</span>
            </li>
            <li v-if="!consentFor.apiScopes?.length" class="text-sm text-slate-500">
              Taramada bilinen bir API kullanımı bulunamadı.
            </li>
          </ul>
          <p class="text-xs text-slate-400">
            Liste statik taramayla çıkarılır ve eksik olabilir; makro yine de sizin
            yapabileceğinizden fazlasını yapamaz.
          </p>
        </div>
        <div class="px-6 py-4 border-t border-slate-100 flex gap-2">
          <button @click="consentFor = null"
                  class="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-xl transition">
            Vazgeç
          </button>
          <button @click="confirmConsent"
                  class="ml-auto px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-xl hover:bg-indigo-500 transition">
            Çalıştır
          </button>
        </div>
      </div>
    </div>

    <MacroEditor v-if="editorOpen"
                 :project-id="projectId"
                 :document-id="documentId"
                 :macro="editing"
                 @close="editorOpen = false"
                 @saved="onSaved"/>
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import CollabMacroApi from '../../../api/CollabMacroApi.js'
import MacroEditor from './MacroEditor.vue'
import { SCOPE_LABELS } from '../../../collab/macro/macroTypeDefs.js'

/**
 * Makro paneli: liste, çalıştırma, onay ve günlük (plan Faz 4).
 *
 * Çalıştırma izni burada değil sunucuda verilir — panel yalnızca `run()`
 * çağırır ve 403 alırsa mesajı gösterir. İzin kararını arayüzde tekrarlamak,
 * iki kuralın zamanla ayrışması demekti.
 */
const props = defineProps({
  projectId: { type: String, required: true },
  documentId: { type: String, required: true },
  running: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'run'])

const STATUS_LABEL = {
  RUNNING: 'çalışıyor', SUCCESS: 'başarılı', FAILED: 'hata',
  TIMEOUT: 'zaman aşımı', DENIED: 'reddedildi'
}
const STATUS_CLASS = {
  SUCCESS: 'text-emerald-600', FAILED: 'text-rose-600',
  TIMEOUT: 'text-amber-600', DENIED: 'text-slate-500'
}

const macros = ref([])
const loading = ref(true)
const editorOpen = ref(false)
const editing = ref(null)
const openRunsFor = ref(null)
const runs = ref([])
const consentFor = ref(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const { data } = await CollabMacroApi.list(props.projectId, props.documentId)
    macros.value = data
  } catch {
    macros.value = []
  } finally {
    loading.value = false
  }
}

function openEditor(macro) {
  editing.value = macro
  editorOpen.value = true
}

function onSaved() {
  editorOpen.value = false
  load()
}

/** Rıza, makro + kaynak özeti başına bir kez sorulur; kaynak değişirse yeniden. */
function consentKey(macro) {
  return `collab_macro_consent_${macro.id}_${macro.updatedAt}`
}

function requestRun(macro) {
  if (localStorage.getItem(consentKey(macro))) {
    emit('run', macro)
    return
  }
  consentFor.value = macro
}

function confirmConsent() {
  const macro = consentFor.value
  consentFor.value = null
  localStorage.setItem(consentKey(macro), '1')
  emit('run', macro)
}

async function approve(macro) {
  try {
    await CollabMacroApi.approve(props.projectId, macro.id)
    createToast('Makro onaylandı', { type: 'success', position: 'bottom-right', timeout: 2500 })
    load()
  } catch { /* interceptor */ }
}

async function revoke(macro) {
  try {
    await CollabMacroApi.revoke(props.projectId, macro.id)
    load()
  } catch { /* interceptor */ }
}

async function toggleRuns(macro) {
  if (openRunsFor.value === macro.id) {
    openRunsFor.value = null
    return
  }
  openRunsFor.value = macro.id
  try {
    const { data } = await CollabMacroApi.runs(props.projectId, macro.id)
    runs.value = data
  } catch {
    runs.value = []
  }
}

defineExpose({ reload: load })
</script>
