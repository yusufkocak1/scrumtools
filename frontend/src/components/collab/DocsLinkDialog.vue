<template>
  <teleport to="body">
    <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm px-4"
         @click.self="$emit('close')">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
          <h3 class="font-semibold text-slate-800">Docs'a Kaydet</h3>
          <button @click="$emit('close')"
                  class="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="p-6 space-y-4">
          <p class="text-xs text-slate-500 leading-relaxed">
            Doküman yeni bir Docs sayfası olarak kaydedilir ve sayfayla
            <strong>aynalanır</strong>: bundan sonra buradaki her kayıt sayfayı da
            günceller ve sürüm geçmişine düşer.
          </p>

          <div v-if="loading" class="py-6 text-center text-sm text-slate-400">Alanlar yükleniyor…</div>

          <template v-else-if="spaces.length === 0">
            <div class="rounded-xl bg-amber-50 border border-amber-200 px-3 py-2.5 text-sm text-amber-800">
              Bu projede yazabileceğiniz bir Docs alanı yok. Önce Docs'ta bir alan oluşturun.
            </div>
          </template>

          <template v-else>
            <div>
              <label class="text-sm text-slate-600 block mb-1">Alan (Space)</label>
              <select v-model="form.spaceId" @change="loadPages"
                      class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100 transition">
                <option v-for="space in spaces" :key="space.id" :value="space.id">{{ space.name }}</option>
              </select>
            </div>

            <div>
              <label class="text-sm text-slate-600 block mb-1">Üst sayfa</label>
              <select v-model="form.parentPageId"
                      class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100 transition">
                <option :value="null">— Alanın kökü —</option>
                <option v-for="page in flatPages" :key="page.id" :value="page.id">
                  {{ '— '.repeat(page.depth) }}{{ page.title }}
                </option>
              </select>
            </div>

            <div>
              <label class="text-sm text-slate-600 block mb-1">Sayfa başlığı</label>
              <input v-model="form.title" maxlength="500" @keyup.enter="submit"
                     class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100 transition"/>
            </div>
          </template>

          <div class="flex justify-end gap-2 pt-1">
            <button @click="$emit('close')"
                    class="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-xl transition">
              İptal
            </button>
            <button @click="submit" :disabled="!canSubmit"
                    class="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-xl hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition">
              {{ saving ? 'Kaydediliyor…' : 'Kaydet' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import DocApi from '../../api/DocApi.js'
import CollabApi from '../../api/CollabApi.js'

/**
 * "Docs'a Kaydet" akışı (COLLAB_WORKSPACE_PLAN.md Y2).
 *
 * Kaydetmeden önce anlık görüntü zorlanır: sayfa içeriği sunucudaki
 * `snapshot_text`'ten üretiliyor, yani son saniyenin yazdıkları henüz oraya
 * yazılmamış olabilir (plan K6).
 */
const props = defineProps({
  projectId: { type: String, required: true },
  documentId: { type: String, required: true },
  defaultTitle: { type: String, default: '' },
  /** Anlık görüntüyü zorlayan fonksiyon — useCollabDoc.requestSnapshot */
  flushSnapshot: { type: Function, default: null }
})

const emit = defineEmits(['close', 'published'])

const loading = ref(true)
const saving = ref(false)
const spaces = ref([])
const flatPages = ref([])
const form = reactive({ spaceId: null, parentPageId: null, title: props.defaultTitle })

const canSubmit = computed(() =>
    !saving.value && !loading.value && !!form.spaceId && !!form.title.trim()
)

onMounted(async () => {
  try {
    const { data } = await DocApi.getSpaces(props.projectId)
    spaces.value = data || []
    if (spaces.value.length > 0) {
      form.spaceId = spaces.value[0].id
      await loadPages()
    }
  } catch {
    spaces.value = []
  } finally {
    loading.value = false
  }
})

async function loadPages() {
  form.parentPageId = null
  if (!form.spaceId) { flatPages.value = []; return }
  try {
    const { data } = await DocApi.getPageTree(props.projectId, form.spaceId)
    flatPages.value = flatten(data || [], 0)
  } catch {
    flatPages.value = []
  }
}

/** Ağacı seçim kutusuna sığacak düz listeye indirger; derinlik girinti olur. */
function flatten(nodes, depth) {
  return nodes.flatMap(node => [
    { id: node.id, title: node.title, depth },
    ...flatten(node.children || [], depth + 1)
  ])
}

async function submit() {
  if (!canSubmit.value) return
  saving.value = true
  try {
    // Önce son hâli sunucuya yazdır; aksi hâlde yeni sayfa eksik doğar.
    if (props.flushSnapshot) await props.flushSnapshot()

    const { data } = await CollabApi.publishToDocs(props.projectId, props.documentId, {
      spaceId: form.spaceId,
      parentPageId: form.parentPageId,
      title: form.title.trim()
    })
    createToast('Docs sayfası oluşturuldu ve aynalama başladı', {
      type: 'success', position: 'bottom-right', timeout: 3000
    })
    emit('published', data)
    emit('close')
  } catch {
    // axios interceptor hata mesajını gösteriyor
  } finally {
    saving.value = false
  }
}
</script>
