<template>
  <aside class="w-80 shrink-0 border-l border-slate-200 bg-white flex flex-col">
    <div class="px-4 py-3 border-b border-slate-100 flex items-center justify-between">
      <h3 class="font-semibold text-slate-800 text-sm">Geçmiş</h3>
      <button @click="$emit('close')"
              class="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <p class="px-4 py-2.5 text-[11px] leading-relaxed text-slate-500 bg-slate-50 border-b border-slate-100">
      Duraklar kaydetme sırasında oluşur. Geri yükleme dokümanı geri sarmaz —
      seçtiğiniz metni <strong>yeni bir düzenleme</strong> olarak uygular, böylece
      o an bağlı olan herkes aynı şeyi görür.
    </p>

    <div v-if="loading" class="p-4 text-sm text-slate-400">Yükleniyor…</div>

    <div v-else-if="entries.length === 0" class="p-4 text-sm text-slate-400">
      Henüz kayıtlı bir durak yok.
    </div>

    <ul v-else class="flex-1 overflow-y-auto divide-y divide-slate-100">
      <li v-for="entry in entries" :key="entry.id" class="px-4 py-3 hover:bg-slate-50 transition">
        <div class="flex items-baseline justify-between gap-2">
          <span class="text-sm font-medium text-slate-700">{{ formatDate(entry.createdAt) }}</span>
          <span class="text-[11px] text-slate-400">{{ formatSize(entry.length) }}</span>
        </div>
        <div class="text-[11px] text-slate-500 mt-0.5">
          {{ entry.createdByName || 'Bilinmiyor' }}
          <template v-if="entry.participantCount > 1">
            · {{ entry.participantCount }} katılımcı
          </template>
        </div>
        <button v-if="canWrite" @click="restore(entry)" :disabled="restoringId === entry.id"
                class="mt-1.5 text-[11px] font-medium text-indigo-600 hover:text-indigo-800 disabled:opacity-50 transition">
          {{ restoringId === entry.id ? 'Geri yükleniyor…' : 'Bu hâle geri dön' }}
        </button>
      </li>
    </ul>
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import CollabApi from '../../api/CollabApi.js'

/**
 * Anlık görüntü zaman çizelgesi (COLLAB_WORKSPACE_PLAN.md §6 — /history).
 *
 * Duraklar sunucuda seyreltilerek saklanır (varsayılan: en fazla 10 dakikada bir,
 * doküman başına 20 kayıt) — her anlık görüntüyü kaydetmek listeyi okunamaz,
 * dar sunucunun diskini de dolu yapardı.
 */
const props = defineProps({
  projectId: { type: String, required: true },
  documentId: { type: String, required: true },
  canWrite: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'restore'])

const entries = ref([])
const loading = ref(true)
const restoringId = ref(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const { data } = await CollabApi.getHistory(props.projectId, props.documentId)
    entries.value = data || []
  } catch {
    entries.value = []
  } finally {
    loading.value = false
  }
}

async function restore(entry) {
  restoringId.value = entry.id
  try {
    const { data } = await CollabApi.getSnapshotText(props.projectId, props.documentId, entry.id)
    // Uygulamayı editör yapar: metni CRDT'ye yazmak için şemayı bilmek gerekir.
    emit('restore', data.snapshotText)
    createToast('Seçilen hâl yeni bir düzenleme olarak uygulandı', {
      type: 'success', position: 'bottom-right', timeout: 3000
    })
  } catch {
    // interceptor
  } finally {
    restoringId.value = null
  }
}

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleString('tr-TR', {
    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
  })
}

/** Bayt değil karakter: kullanıcı için anlamlı olan içeriğin uzunluğu. */
function formatSize(length) {
  if (!length) return 'boş'
  return length < 1000 ? `${length} karakter` : `${(length / 1000).toFixed(1)}b karakter`
}

defineExpose({ reload: load })
</script>
