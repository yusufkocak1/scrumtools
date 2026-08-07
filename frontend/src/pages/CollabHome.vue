<template>
  <div class="min-h-[calc(100vh-4rem)] bg-slate-50">
    <div class="max-w-6xl mx-auto px-4 py-8">
      <div class="flex items-start justify-between gap-4 mb-6">
        <div>
          <h1 class="text-2xl font-bold text-slate-900">Ortak Çalışma Alanı</h1>
          <p class="text-slate-500 text-sm mt-1">
            Metin, kod ve hesap tablolarını ekibinizle aynı anda düzenleyin — değişiklikler anında birleşir.
          </p>
        </div>
        <div class="shrink-0 flex items-center gap-2">
          <button @click="fileInput?.click()" :disabled="importing"
                  class="px-4 py-2 bg-white border border-slate-200 text-slate-700 text-sm font-medium rounded-xl hover:border-indigo-300 disabled:opacity-50 transition">
            {{ importing ? 'Yükleniyor…' : 'Excel Yükle' }}
          </button>
          <input ref="fileInput" type="file" class="hidden"
                 accept=".xlsx,.xlsm,.csv" @change="importSheet"/>
          <button @click="showCreate = true"
                  class="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-xl hover:bg-indigo-500 shadow-sm shadow-indigo-200 transition">
            Yeni Doküman
          </button>
        </div>
      </div>

      <!-- Filtreler -->
      <div class="flex flex-wrap items-center gap-2 mb-5">
        <div class="flex bg-white border border-slate-200 rounded-xl p-0.5">
          <button v-for="option in TYPE_FILTERS" :key="option.value"
                  @click="setType(option.value)"
                  :class="['px-3 py-1.5 text-xs font-medium rounded-lg transition',
                           typeFilter === option.value ? 'bg-indigo-600 text-white' : 'text-slate-500 hover:text-slate-700']">
            {{ option.label }}
          </button>
        </div>
        <input v-model="search" @input="debouncedLoad" type="search"
               placeholder="Başlık veya içerikte ara…"
               class="flex-1 min-w-[12rem] max-w-sm border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100 transition"/>
      </div>

      <div v-if="loading" class="py-16 text-center text-slate-400">Yükleniyor…</div>

      <div v-else-if="documents.length === 0"
           class="bg-white border border-dashed border-slate-200 rounded-2xl py-16 text-center">
        <h3 class="text-slate-800 font-semibold mb-1">Henüz doküman yok</h3>
        <p class="text-slate-500 text-sm mb-5">
          İlk ortak çalışma dokümanınızı oluşturun; ekip arkadaşlarınız aynı anda yazabilir.
        </p>
        <button @click="showCreate = true"
                class="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-xl hover:bg-indigo-500 transition">
          Yeni Doküman
        </button>
      </div>

      <div v-else class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <article v-for="item in documents" :key="item.id"
                 @click="open(item)"
                 class="group bg-white border border-slate-200 rounded-2xl p-4 cursor-pointer hover:border-indigo-300 hover:shadow-md transition">
          <div class="flex items-center gap-2 mb-2">
            <span :class="['px-2 py-0.5 rounded-md text-[11px] font-semibold', TYPE_BADGES[item.type].class]">
              {{ TYPE_BADGES[item.type].label }}
            </span>
            <span v-if="item.language" class="text-[11px] text-slate-400">{{ item.language }}</span>
            <span v-if="item.teamName"
                  class="ml-auto text-[11px] text-slate-400 truncate max-w-[8rem]">{{ item.teamName }}</span>
          </div>

          <h3 class="font-semibold text-slate-800 truncate group-hover:text-indigo-600 transition">
            {{ item.title }}
          </h3>
          <p class="text-xs text-slate-500 mt-1 line-clamp-2 min-h-[2rem]">
            {{ item.preview || 'Boş doküman' }}
          </p>

          <div class="flex items-center justify-between mt-3 pt-3 border-t border-slate-100 text-[11px] text-slate-400">
            <span class="truncate">{{ item.updatedByName || '—' }}</span>
            <span>{{ formatDate(item.updatedAt) }}</span>
          </div>
        </article>
      </div>
    </div>

    <!-- Yeni doküman modalı -->
    <teleport to="body">
      <div v-if="showCreate"
           class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm px-4"
           @click.self="showCreate = false">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
          <div class="px-6 py-4 border-b border-slate-100">
            <h3 class="font-semibold text-slate-800">Yeni Doküman</h3>
          </div>
          <div class="p-6 space-y-4">
            <div>
              <label class="text-sm text-slate-600 block mb-1.5">Tip</label>
              <div class="grid grid-cols-3 gap-2">
                <button v-for="option in CREATE_TYPES" :key="option.value"
                        @click="form.type = option.value"
                        :class="['border rounded-xl px-3 py-2.5 text-left transition',
                                 form.type === option.value
                                   ? 'border-indigo-400 bg-indigo-50/60 ring-2 ring-indigo-100'
                                   : 'border-slate-200 hover:border-slate-300']">
                  <div class="text-sm font-medium text-slate-800">{{ option.label }}</div>
                  <div class="text-[11px] text-slate-500 mt-0.5">{{ option.hint }}</div>
                </button>
              </div>
            </div>

            <div>
              <label class="text-sm text-slate-600 block mb-1">Başlık</label>
              <input v-model="form.title" @keyup.enter="create" maxlength="500"
                     placeholder="Örn. Sprint 12 teknik notları"
                     class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100 transition"/>
            </div>

            <div v-if="form.type === 'CODE'">
              <label class="text-sm text-slate-600 block mb-1">Dil</label>
              <select v-model="form.language"
                      class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-indigo-300 transition">
                <option v-for="lang in LANGUAGES" :key="lang" :value="lang">{{ lang }}</option>
              </select>
            </div>

            <div class="flex justify-end gap-2 pt-1">
              <button @click="showCreate = false"
                      class="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-xl transition">
                İptal
              </button>
              <button @click="create" :disabled="!form.title.trim() || creating"
                      class="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-xl hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition">
                {{ creating ? 'Oluşturuluyor…' : 'Oluştur' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { createToast } from 'mosha-vue-toastify'
import CollabApi from '../api/CollabApi.js'

/**
 * Doküman listesi/galerisi — silinen CodeShare.vue'nun yerini alır.
 *
 * Eski modelde erişim bir `tag` yazarak oluyordu ve iki kişinin aynı etiketi
 * farklı amaçla kullanması sessiz veri kaybına yol açıyordu. Artık dokümanlar
 * UUID ile adreslenir, erişim bu liste ve arama üzerinden olur (plan D1/§5).
 */
const props = defineProps({
  projectId: { type: String, required: true }
})

const router = useRouter()

const TYPE_BADGES = {
  TEXT: { label: 'METİN', class: 'bg-indigo-50 text-indigo-600' },
  CODE: { label: 'KOD', class: 'bg-slate-800 text-white' },
  SHEET: { label: 'TABLO', class: 'bg-emerald-50 text-emerald-600' }
}
const TYPE_FILTERS = [
  { value: '', label: 'Tümü' },
  { value: 'TEXT', label: 'Metin' },
  { value: 'CODE', label: 'Kod' },
  { value: 'SHEET', label: 'Tablo' }
]
const CREATE_TYPES = [
  { value: 'TEXT', label: 'Metin', hint: 'Zengin metin, tablo, başlık' },
  { value: 'CODE', label: 'Kod', hint: 'Monaco, sözdizimi renklendirme' },
  // SHEET ayrı bir paket özelliği (COLLAB_SHEET); FREE pakette backend reddeder
  // ve kullanıcı yükseltme akışına düşer.
  { value: 'SHEET', label: 'Tablo', hint: 'Izgara, formül, Excel aktarımı' }
]
const LANGUAGES = [
  'javascript', 'typescript', 'java', 'python', 'csharp', 'go', 'rust', 'kotlin',
  'php', 'ruby', 'sql', 'html', 'css', 'json', 'yaml', 'xml', 'markdown', 'shell', 'plaintext'
]

const documents = ref([])
const loading = ref(true)
const typeFilter = ref('')
const search = ref('')
const showCreate = ref(false)
const creating = ref(false)
const importing = ref(false)
const fileInput = ref(null)
const form = reactive({ type: 'TEXT', title: '', language: 'javascript' })

let searchTimer = null

onMounted(() => {
  // Navbar'ın "Ortak Çalışma" girişi proje seçimi taşımıyor; Docs'taki desenin
  // aynısıyla son kullanılan proje hatırlanıyor.
  localStorage.setItem('collab_last_project_id', props.projectId)
  load()
})
onBeforeUnmount(() => clearTimeout(searchTimer))

async function load() {
  loading.value = true
  try {
    const { data } = await CollabApi.listDocuments(props.projectId, {
      type: typeFilter.value,
      query: search.value
    })
    documents.value = data
  } catch {
    documents.value = []
  } finally {
    loading.value = false
  }
}

function setType(value) {
  typeFilter.value = value
  load()
}

/** Her tuşta istek atmamak için; arama backend'de LIKE taraması yapıyor. */
function debouncedLoad() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(load, 300)
}

function open(item) {
  router.push(`/projects/${props.projectId}/collab/${item.id}`)
}

async function create() {
  const title = form.title.trim()
  if (!title || creating.value) return
  creating.value = true
  try {
    const { data } = await CollabApi.createDocument(props.projectId, {
      type: form.type,
      title,
      language: form.type === 'CODE' ? form.language : null
    })
    showCreate.value = false
    form.title = ''
    router.push(`/projects/${props.projectId}/collab/${data.id}`)
  } catch {
    // Paket limiti (402) axios interceptor'ında upgrade akışını tetikliyor
  } finally {
    creating.value = false
  }
}

/**
 * Excel/CSV yükleyip yeni bir tablo dokümanı açar (plan §10).
 *
 * Uyum raporu bir toast olarak gösteriliyor: aktarılmayan özellikler (grafik,
 * pivot, VBA) sessizce düşerse kullanıcı kaybı ancak dosyayı Excel'de tekrar
 * açtığında fark eder.
 */
async function importSheet(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || importing.value) return

  importing.value = true
  try {
    const { data } = await CollabApi.importSheet(props.projectId, file)
    if (data.warnings?.length) {
      createToast(data.warnings.join(' '), {
        type: 'warning', position: 'bottom-right', timeout: 8000
      })
    }
    router.push(`/projects/${props.projectId}/collab/${data.document.id}`)
  } catch {
    // 402 (paket limiti) ve 400 (biçim) mesajlarını interceptor gösteriyor
  } finally {
    importing.value = false
  }
}

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleDateString('tr-TR', {
    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
  })
}
</script>
