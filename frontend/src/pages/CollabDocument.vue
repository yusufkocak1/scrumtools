<template>
  <div class="h-[calc(100vh-4rem)] bg-slate-50">
    <div v-if="loading" class="h-full flex items-center justify-center text-slate-400">
      <div class="animate-spin w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full mr-3"></div>
      Doküman açılıyor…
    </div>

    <div v-else-if="loadError" class="h-full flex flex-col items-center justify-center text-center px-6">
      <h2 class="text-lg font-semibold text-slate-800 mb-1">Doküman açılamadı</h2>
      <p class="text-slate-500 text-sm mb-4">{{ loadError }}</p>
      <button @click="goBack" class="px-4 py-2 bg-indigo-600 text-white text-sm rounded-xl hover:bg-indigo-500 transition">
        Listeye dön
      </button>
    </div>

    <div v-else class="h-full flex">
      <CollabShell
          class="flex-1 min-w-0"
          :title="doc.title"
          :type="doc.type"
          :language="doc.language || 'javascript'"
          :status="status"
          :can-write="canWrite"
          :is-writer="isWriter"
          :pending-changes="pendingChanges"
          :last-saved-at="lastSavedAt"
          :participants="participants"
          :doc-page-link="docPageLink"
          :history-open="showHistory"
          @back="goBack"
          @rename="rename"
          @language="changeLanguage"
          @retry="reconnect"
          @publish="showPublish = true"
          @export="exportSheet"
          @toggle-history="showHistory = !showHistory">
        <CollabSheetEditor
            v-if="doc.type === 'SHEET'"
            ref="sheetEditor"
            :ydoc="ydoc"
            :awareness="awareness"
            :document-id="documentId"
            :read-only="!canWrite"
            @snapshotText="setSnapshotTextProvider"/>
        <CollabCodeEditor
            v-else-if="doc.type === 'CODE'"
            ref="codeEditor"
            :ydoc="ydoc"
            :awareness="awareness"
            :language="doc.language || 'javascript'"
            :read-only="!canWrite"
            @snapshotText="setSnapshotTextProvider"/>
        <CollabTextEditor
            v-else
            ref="textEditor"
            :ydoc="ydoc"
            :awareness="awareness"
            :read-only="!canWrite"
            @snapshotText="setSnapshotTextProvider"/>
      </CollabShell>

      <HistoryPanel
          v-if="showHistory"
          :project-id="projectId"
          :document-id="documentId"
          :can-write="canWrite"
          @close="showHistory = false"
          @restore="applyRestoredText"/>
    </div>

    <DocsLinkDialog
        v-if="showPublish"
        :project-id="projectId"
        :document-id="documentId"
        :default-title="doc.title"
        :flush-snapshot="requestSnapshot"
        @close="showPublish = false"
        @published="onPublished"/>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createToast } from 'mosha-vue-toastify'
import CollabApi from '../api/CollabApi.js'
import { useCollabDoc } from '../composables/useCollabDoc.js'
import CollabShell from '../components/collab/CollabShell.vue'
import CollabCodeEditor from '../components/collab/editors/CollabCodeEditor.vue'
import CollabTextEditor from '../components/collab/editors/CollabTextEditor.vue'
import CollabSheetEditor from '../components/collab/editors/CollabSheetEditor.vue'
import DocsLinkDialog from '../components/collab/DocsLinkDialog.vue'
import HistoryPanel from '../components/collab/HistoryPanel.vue'

/**
 * Tek doküman kabuğu: üstveriyi çeker, CRDT oturumunu kurar ve tipe göre
 * editörü yükler (COLLAB_WORKSPACE_PLAN.md §7).
 */
const props = defineProps({
  projectId: { type: String, required: true },
  documentId: { type: String, required: true }
})

const router = useRouter()
const loading = ref(true)
const loadError = ref('')
// `document` adı bilinçli olarak kullanılmıyor: script setup kapsamında global
// `document`'i gölgeler ve DOM'a dokunan her şey sessizce bozulur.
const doc = ref({ title: '', type: 'TEXT', language: null, docPageId: null })

const showPublish = ref(false)
const showHistory = ref(false)
const textEditor = ref(null)
const codeEditor = ref(null)
const sheetEditor = ref(null)
const seedAttempted = ref(false)
const exporting = ref(false)

// CRDT oturumu bileşenin ömrü boyunca yaşar; useCollabDoc kendi
// onBeforeUnmount'unda bağlantıyı kapatıp son anlık görüntüyü gönderir.
const {
  ydoc, awareness, status, canWrite, isWriter, participants,
  pendingChanges, lastSavedAt, setSnapshotTextProvider, requestSnapshot, reconnect
} = useCollabDoc(props.projectId, props.documentId)

const docPageLink = computed(() => {
  if (!doc.value.docPageId || !doc.value.docSpaceId) return ''
  return `/projects/${props.projectId}/docs/${doc.value.docSpaceId}/pages/${doc.value.docPageId}`
})

onMounted(async () => {
  try {
    const { data } = await CollabApi.getDocument(props.projectId, props.documentId)
    doc.value = data
  } catch (e) {
    loadError.value = e?.response?.data?.error || 'Doküman bulunamadı ya da erişim yetkiniz yok.'
  } finally {
    loading.value = false
  }
})

/**
 * Tohumlama (plan Y1 adım 3 / R2 — Faz 3'te içe aktarma da aynı yolu kullanır).
 *
 * Bağlantı kurulduktan **sonra** denenir: içerik Y.Doc'a yazılıyor ve oradan
 * CRDT güncellemesi olarak yayılıyor; bağlantı yokken yazılsaydı paket kimseye
 * gitmez, üstelik sunucudaki "tohumlandı" işareti konmuş olurdu.
 *
 * Docs'a bağlı dokümanlarda kaynak sayfa HTML'i, içe aktarılmış tablolarda
 * POI'nin ürettiği JSON'dur; hangisi olduğuna sunucu karar verir.
 */
watch([status, () => doc.value.type, () => doc.value.docPageId, canWrite],
    async ([currentStatus, type, docPageId, writable]) => {
      if (seedAttempted.value) return
      if (currentStatus !== 'synced' || !writable) return
      // Tohumlanacak bir şeyi olabilecek tek iki durum; diğerlerinde sunucuya
      // her açılışta boşuna istek atmıyoruz.
      if (!docPageId && type !== 'SHEET') return
      seedAttempted.value = true
      try {
        const { data } = await CollabApi.claimSeed(props.projectId, props.documentId)
        // granted=false: başka bir sekme aktarımı üstlendi — dokunmuyoruz.
        if (data?.granted && data.content) {
          if (type === 'SHEET') sheetEditor.value?.seedContent(data.content)
          else textEditor.value?.seedContent(data.content)
        }
      } catch {
        // Tohumlama başarısız olsa da doküman boş olarak kullanılabilir kalır.
        seedAttempted.value = false
      }
    })

/**
 * Excel/CSV indirme (plan §10).
 *
 * Önce anlık görüntü zorlanıyor: hesaplanmış formül değerleri yalnızca
 * istemcide var (K5), sunucu son anlık görüntüde ne varsa onu yazar — bu adım
 * atlanırsa son dakikanın düzenlemeleri dosyada görünmez.
 */
async function exportSheet(format) {
  if (exporting.value) return
  exporting.value = true
  try {
    await requestSnapshot()
    const { data } = await CollabApi.exportSheet(props.projectId, props.documentId, format)
    const url = URL.createObjectURL(data)
    const link = document.createElement('a')
    link.href = url
    link.download = `${doc.value.title || 'tablo'}.${format}`
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    createToast('Dosya indirilemedi', { type: 'danger', position: 'bottom-right' })
  } finally {
    exporting.value = false
  }
}

function goBack() {
  router.push(`/projects/${props.projectId}/collab`)
}

async function rename(title) {
  const next = (title || '').trim()
  if (!next || next === doc.value.title) return
  try {
    const { data } = await CollabApi.patchDocument(props.projectId, props.documentId, { title: next })
    doc.value = data
  } catch { /* axios interceptor toast gösteriyor */ }
}

async function changeLanguage(language) {
  try {
    const { data } = await CollabApi.patchDocument(props.projectId, props.documentId, { language })
    doc.value = data
    createToast(`Dil ${language} olarak ayarlandı`, { type: 'info', position: 'bottom-right', timeout: 2000 })
  } catch { /* interceptor */ }
}

function onPublished(updated) {
  doc.value = updated
  // Yeni sayfa dokümanın kendi içeriğinden doğdu; tohumlama denenmemeli.
  seedAttempted.value = true
}

/** Geçmişten geri yükleme: eski metin yeni bir düzenleme olarak uygulanır. */
function applyRestoredText(text) {
  if (!canWrite.value || !text) return
  if (doc.value.type === 'SHEET') {
    sheetEditor.value?.seedContent(text)
  } else if (doc.value.type === 'CODE') {
    codeEditor.value?.replaceContent(text)
  } else {
    textEditor.value?.seedContent(text)
  }
}
</script>
