<template>
  <div class="flex h-screen bg-white overflow-hidden">
    <!-- Mobil: sayfa ağacı arka plan karartması -->
    <div v-if="showPageTree" @click="showPageTree = false"
         class="fixed inset-0 bg-slate-900/40 backdrop-blur-[2px] z-30 lg:hidden"></div>

    <!-- Sol Panel: Sayfa Ağacı -->
    <aside :class="[
             'w-72 max-w-[85vw] bg-slate-50 border-r border-slate-200 flex flex-col',
             'fixed inset-y-0 left-0 z-40 transform transition-transform duration-200 lg:static lg:z-auto lg:translate-x-0',
             showPageTree ? 'translate-x-0' : '-translate-x-full'
           ]">
      <div class="px-3 pt-3 pb-2">
        <button @click="goBack"
                class="flex items-center gap-1.5 text-xs font-medium text-slate-500 hover:text-indigo-600 px-2 py-1.5 rounded-lg hover:bg-slate-200/60 transition">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18"/>
          </svg>
          Tüm Space'ler
        </button>
        <div class="mt-2 flex items-center gap-2.5 px-2">
          <div :class="['w-9 h-9 rounded-xl bg-gradient-to-br text-white flex items-center justify-center font-bold shadow-sm shrink-0', spaceGradientClass]">
            {{ space?.name?.charAt(0).toUpperCase() }}
          </div>
          <div class="min-w-0 flex-1">
            <h2 class="font-semibold text-slate-800 truncate text-sm leading-tight">{{ space?.name }}</h2>
            <p class="text-[11px] text-slate-400">Doküman alanı</p>
          </div>
          <button @click="createNewPage()"
                  class="w-7 h-7 shrink-0 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white flex items-center justify-center shadow-sm shadow-indigo-200 transition"
                  title="Yeni sayfa">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- Sayfa arama -->
      <div class="px-3 pb-2 pt-1">
        <div class="relative">
          <svg class="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"/>
          </svg>
          <input v-model="pageSearch" type="text" placeholder="Sayfalarda ara..."
                 class="w-full bg-white border border-slate-200 rounded-lg pl-8 pr-2.5 py-1.5 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition placeholder:text-slate-400"/>
        </div>
      </div>

      <div class="px-4 pb-1 pt-2 text-[11px] font-semibold uppercase tracking-wider text-slate-400">Sayfalar</div>

      <!-- Page Tree -->
      <div class="flex-1 overflow-y-auto px-2 pb-3">
        <PageTree
            :pages="filteredTree"
            :selectedPageId="selectedPageId"
            :forceExpand="!!pageSearch.trim()"
            @select="selectPage"
            @add-child="(p) => createNewPage(p.id)"
        />
        <div v-if="pageSearch.trim() && filteredTree.length === 0 && pageTree.length > 0" class="px-3 py-6 text-center">
          <p class="text-sm text-slate-400">Eşleşen sayfa bulunamadı</p>
        </div>
      </div>
    </aside>

    <!-- Sağ Panel: İçerik -->
    <main class="flex-1 flex flex-col overflow-hidden min-w-0">
      <!-- Üst Bar -->
      <header v-if="currentPage" class="bg-white border-b border-slate-200 px-3 sm:px-6 py-2.5 flex flex-wrap items-center justify-between gap-y-2">
        <div class="flex items-center gap-3 min-w-0 flex-1 mr-4">
          <!-- Mobil: sayfa ağacını aç -->
          <button @click="showPageTree = true"
                  class="lg:hidden shrink-0 p-1.5 rounded-lg text-slate-500 hover:text-indigo-600 hover:bg-slate-100 transition"
                  title="Sayfalar">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h16"/>
            </svg>
          </button>
          <div class="min-w-0">
            <div class="hidden sm:flex items-center gap-1.5 text-[11px] text-slate-400">
              <button @click="goBack" class="hover:text-indigo-600 transition">Spaces</button>
              <svg class="w-2.5 h-2.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5"/>
              </svg>
              <span class="truncate max-w-[180px]">{{ space?.name }}</span>
            </div>
            <div class="flex items-center gap-2 min-w-0">
              <template v-if="renamingTitle">
                <input ref="titleInput" v-model="titleDraft" type="text"
                       class="text-lg font-semibold text-slate-800 border-b-2 border-indigo-400 outline-none bg-indigo-50/50 rounded-t px-2 py-0.5 flex-1 min-w-0"
                       @keyup.enter="saveTitle"
                       @keyup.esc="cancelRename"
                       @blur="saveTitle"/>
              </template>
              <template v-else>
                <h1 @click="startRename"
                    class="text-lg font-semibold text-slate-800 truncate cursor-text hover:bg-slate-100 rounded-lg px-2 py-0.5 -mx-2 transition group flex items-center gap-2"
                    title="Yeniden adlandırmak için tıklayın">
                  {{ currentPage.title }}
                  <svg class="w-3.5 h-3.5 shrink-0 text-slate-300 group-hover:text-slate-400 transition" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round"
                          d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931z"/>
                  </svg>
                </h1>
              </template>
              <span class="text-[11px] font-medium bg-slate-100 text-slate-500 px-2 py-0.5 rounded-full shrink-0">v{{ currentPage.versionNumber }}</span>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-1">
          <button @click="showVersions = !showVersions"
                  :class="panelBtnClass(showVersions)"
                  title="Versiyon geçmişi">
            <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </button>
          <button @click="showAttachments = !showAttachments"
                  :class="panelBtnClass(showAttachments)"
                  title="Dosyalar">
            <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M18.375 12.739l-7.693 7.693a4.5 4.5 0 01-6.364-6.364l10.94-10.94A3 3 0 1119.5 7.372L8.552 18.32m.009-.01l-.01.01m5.699-9.941l-7.81 7.81a1.5 1.5 0 002.112 2.13"/>
            </svg>
          </button>
          <button @click="showPermissions = !showPermissions"
                  :class="panelBtnClass(showPermissions)"
                  title="Yetkiler">
            <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z"/>
            </svg>
          </button>
          <div class="w-px h-6 bg-slate-200 mx-1.5 hidden sm:block"></div>
          <button v-if="!editing" @click="startEditing"
                  class="inline-flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-500 active:bg-indigo-700 text-white px-3.5 py-1.5 rounded-xl text-sm font-medium shadow-sm shadow-indigo-200 transition">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931z"/>
            </svg>
            Düzenle
          </button>
          <template v-else>
            <button @click="savePage"
                    class="inline-flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-500 text-white px-3.5 py-1.5 rounded-xl text-sm font-medium shadow-sm shadow-emerald-200 transition">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"/>
              </svg>
              Kaydet
            </button>
            <button @click="cancelEditing"
                    class="text-slate-500 hover:text-slate-700 px-3 py-1.5 rounded-xl hover:bg-slate-100 text-sm font-medium transition">
              İptal
            </button>
          </template>
        </div>
      </header>

      <!-- İçerik Alanı -->
      <div v-if="currentPage" class="flex-1 overflow-y-auto bg-white">
        <div v-if="editing" class="h-full">
          <TiptapEditor
              v-model="editContent"
              :projectId="projectId"
              :spaceId="spaceId"
              :pageId="currentPage.id"
              class="h-full"
          />
        </div>
        <article v-else class="prose prose-indigo max-w-3xl mx-auto px-4 sm:px-8 py-8 sm:py-10 overflow-x-auto" v-html="renderedContent"></article>
      </div>

      <!-- Boş durum -->
      <div v-else class="flex-1 flex items-center justify-center bg-slate-50/50">
        <div class="text-center px-4">
          <div class="w-16 h-16 mx-auto rounded-2xl bg-indigo-50 text-indigo-400 flex items-center justify-center mb-4">
            <svg class="w-8 h-8" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"/>
            </svg>
          </div>
          <h3 class="font-semibold text-slate-700">Bir sayfa seçin</h3>
          <p class="text-slate-500 text-sm mt-1">Soldaki listeden bir sayfa açın veya yeni bir sayfa oluşturun</p>
          <div class="mt-5 flex items-center justify-center gap-2">
            <button @click="createNewPage()"
                    class="inline-flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-xl text-sm font-medium shadow-sm shadow-indigo-200 transition">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
              </svg>
              Yeni Sayfa
            </button>
            <button @click="showPageTree = true"
                    class="lg:hidden text-slate-600 hover:text-slate-800 bg-white border border-slate-200 hover:bg-slate-50 px-4 py-2 rounded-xl text-sm font-medium transition">
              Sayfaları Göster
            </button>
          </div>
        </div>
      </div>
    </main>

    <!-- Sağ Yan Panel: Versiyonlar -->
    <aside v-if="showVersions" class="fixed inset-y-0 right-0 z-40 w-80 max-w-[85vw] shadow-xl lg:static lg:z-auto lg:shadow-none bg-white border-l border-slate-200 flex flex-col">
      <VersionHistory
          :projectId="projectId"
          :spaceId="spaceId"
          :pageId="currentPage?.id"
          @restore="onVersionRestored"
          @close="showVersions = false"
      />
    </aside>

    <!-- Sağ Yan Panel: Dosyalar -->
    <aside v-if="showAttachments" class="fixed inset-y-0 right-0 z-40 w-80 max-w-[85vw] shadow-xl lg:static lg:z-auto lg:shadow-none bg-white border-l border-slate-200 flex flex-col">
      <DocAttachments
          :projectId="projectId"
          :spaceId="spaceId"
          :pageId="currentPage?.id"
          @inserted="onAttachmentInserted"
          @close="showAttachments = false"
      />
    </aside>

    <!-- Permission Dialog -->
    <DocPermissionDialog
        v-if="showPermissions"
        :projectId="projectId"
        :spaceId="spaceId"
        :pageId="currentPage?.id"
        @close="showPermissions = false"
    />

    <!-- Yeni Sayfa Dialog -->
    <InputDialog
        v-if="showNewPageDialog"
        title="Yeni Sayfa"
        label="Sayfa başlığı"
        placeholder="Örn: Kurulum Rehberi"
        confirmText="Oluştur"
        @confirm="confirmCreatePage"
        @cancel="showNewPageDialog = false"
    />

    <!-- Kaydet: Değişiklik Özeti Dialog -->
    <InputDialog
        v-if="showSaveDialog"
        title="Sayfayı Kaydet"
        label="Değişiklik özeti"
        placeholder="Örn: Kurulum adımları güncellendi"
        hint="Bu özet versiyon geçmişinde görünür."
        confirmText="Kaydet"
        :optional="true"
        @confirm="confirmSavePage"
        @cancel="showSaveDialog = false"
    />
  </div>
</template>

<script setup>
import {ref, computed, onMounted, watch, nextTick} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {marked} from 'marked'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'
import DocApi from '../api/DocApi.js'
import PageTree from '../components/docs/PageTree.vue'
import TiptapEditor from '../components/docs/TiptapEditor.vue'
import VersionHistory from '../components/docs/VersionHistory.vue'
import DocAttachments from '../components/docs/DocAttachments.vue'
import DocPermissionDialog from '../components/docs/DocPermissionDialog.vue'
import InputDialog from '../components/common/InputDialog.vue'

const route = useRoute()
const router = useRouter()

const projectId = computed(() => route.params.projectId)
const spaceId = computed(() => route.params.spaceId)
const selectedPageId = computed(() => route.params.pageId || null)

const space = ref(null)
const pageTree = ref([])
const currentPage = ref(null)
const editing = ref(false)
const editContent = ref('')
const showVersions = ref(false)
const showAttachments = ref(false)
const showPermissions = ref(false)

// Yeni sayfa & kaydet dialog state
const showNewPageDialog = ref(false)
const showPageTree = ref(false)
const newPageParentId = ref(null)
const showSaveDialog = ref(false)

// Sayfa arama
const pageSearch = ref('')

// Başlık yeniden adlandırma state
const renamingTitle = ref(false)
const titleDraft = ref('')
const titleInput = ref(null)

// İsimden deterministik gradyan seçimi (Docs.vue ile aynı palet)
const avatarGradients = [
  'from-indigo-500 to-violet-600',
  'from-sky-500 to-blue-600',
  'from-emerald-500 to-teal-600',
  'from-amber-500 to-orange-600',
  'from-rose-500 to-pink-600',
  'from-fuchsia-500 to-purple-600'
]

const spaceGradientClass = computed(() => {
  let h = 0
  for (const ch of (space.value?.name || '')) h = (h + ch.charCodeAt(0)) % 997
  return avatarGradients[h % avatarGradients.length]
})

// Arama filtresi: başlığı eşleşen ya da eşleşen alt sayfası olan düğümler kalır
const filteredTree = computed(() => {
  const q = pageSearch.value.trim().toLowerCase()
  if (!q) return pageTree.value
  const filter = (nodes) => nodes
      .map(n => {
        const children = filter(n.children || [])
        if (n.title?.toLowerCase().includes(q) || children.length) {
          return {...n, children}
        }
        return null
      })
      .filter(Boolean)
  return filter(pageTree.value)
})

function panelBtnClass(active) {
  return [
    'p-2 rounded-lg transition',
    active ? 'bg-indigo-50 text-indigo-600' : 'text-slate-400 hover:text-slate-600 hover:bg-slate-100'
  ]
}

// İçerik render - TipTap HTML çıktısı ürettiği için sadece sanitize ediyoruz
// Eski markdown içerikler için de marked ile fallback yapılır
const renderedContent = computed(() => {
  if (!currentPage.value?.content) return ''
  const content = currentPage.value.content
  // TipTap HTML ile başlıyorsa doğrudan sanitize et
  if (content.trimStart().startsWith('<')) {
    return DOMPurify.sanitize(content, {
      ADD_TAGS: ['img', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'colgroup', 'col'],
      ADD_ATTR: ['src', 'alt', 'href', 'target', 'colspan', 'rowspan', 'colwidth', 'style']
    })
  }
  // Eski markdown içerikler için fallback
  const html = marked(content)
  return DOMPurify.sanitize(html, {
    ADD_TAGS: ['img', 'table', 'thead', 'tbody', 'tr', 'th', 'td'],
    ADD_ATTR: ['src', 'alt', 'href', 'target', 'colspan', 'rowspan']
  })
})

onMounted(async () => {
  await loadSpace()
  await loadPageTree()
  if (selectedPageId.value) {
    await loadPage(selectedPageId.value)
  }
})

watch(() => route.params.pageId, async (newId) => {
  if (newId) {
    await loadPage(newId)
  } else {
    currentPage.value = null
  }
})

async function loadSpace() {
  try {
    const res = await DocApi.getSpace(projectId.value, spaceId.value)
    space.value = res.data
  } catch (e) {
    console.error(e)
  }
}

async function loadPageTree() {
  try {
    const res = await DocApi.getPageTree(projectId.value, spaceId.value)
    pageTree.value = res.data
  } catch (e) {
    console.error(e)
  }
}

async function loadPage(pageId) {
  try {
    const res = await DocApi.getPage(projectId.value, spaceId.value, pageId)
    currentPage.value = res.data
    editing.value = false
  } catch (e) {
    console.error(e)
  }
}

function selectPage(page) {
  showPageTree.value = false
  router.push({
    name: 'DocPage',
    params: {projectId: projectId.value, spaceId: spaceId.value, pageId: page.id}
  })
}

function createNewPage(parentPageId = null) {
  newPageParentId.value = parentPageId
  showNewPageDialog.value = true
}

async function confirmCreatePage(title) {
  showNewPageDialog.value = false
  try {
    const res = await DocApi.createPage(projectId.value, spaceId.value, {
      title,
      content: '',
      parentPageId: newPageParentId.value
    })
    await loadPageTree()
    selectPage(res.data)
  } catch (e) {
    console.error(e)
  }
}

// ─── Başlık yeniden adlandırma ───────────────────────────────────────────────

function startRename() {
  titleDraft.value = currentPage.value.title
  renamingTitle.value = true
  nextTick(() => titleInput.value?.focus())
}

function cancelRename() {
  renamingTitle.value = false
}

async function saveTitle() {
  if (!renamingTitle.value) return
  renamingTitle.value = false
  const newTitle = titleDraft.value.trim()
  if (!newTitle || newTitle === currentPage.value.title) return
  try {
    const res = await DocApi.updatePage(projectId.value, spaceId.value, currentPage.value.id, {
      title: newTitle,
      content: currentPage.value.content,
      changeSummary: 'Sayfa yeniden adlandırıldı'
    })
    currentPage.value = res.data
    await loadPageTree()
  } catch (e) {
    console.error(e)
  }
}

function startEditing() {
  editContent.value = currentPage.value.content || ''
  editing.value = true
}

function savePage() {
  showSaveDialog.value = true
}

async function confirmSavePage(summary) {
  showSaveDialog.value = false
  try {
    const res = await DocApi.updatePage(projectId.value, spaceId.value, currentPage.value.id, {
      title: currentPage.value.title,
      content: editContent.value,
      changeSummary: summary || null
    })
    currentPage.value = res.data
    editing.value = false
    await loadPageTree()
  } catch (e) {
    console.error(e)
  }
}

function cancelEditing() {
  editing.value = false
}

async function onVersionRestored() {
  await loadPage(currentPage.value.id)
  await loadPageTree()
}

function onAttachmentInserted(url) {
  // Attachment'ı editöre resim olarak ekle (TipTap HTML editörü)
  if (editing.value) {
    editContent.value += `<img src="${url}" alt="attachment" />`
  }
}

function goBack() {
  router.push({name: 'Docs', params: {projectId: projectId.value}})
}
</script>

<style>
/* Görüntüleme modunda tablo ve resim stilleri */
.prose table {
  border-collapse: collapse;
  width: 100%;
}

.prose th,
.prose td {
  border: 1px solid #e2e8f0;
  padding: 0.5rem 0.75rem;
}

.prose th {
  background-color: #f8fafc;
  font-weight: 600;
}

.prose img {
  max-width: 100%;
  height: auto;
  border-radius: 0.5rem;
}
</style>
