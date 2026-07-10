<template>
  <div class="flex h-screen bg-gray-50">
    <!-- Mobil: sayfa ağacı arka plan karartması -->
    <div v-if="showPageTree" @click="showPageTree = false"
         class="fixed inset-0 bg-black/40 z-30 lg:hidden"></div>

    <!-- Sol Panel: Sayfa Ağacı -->
    <aside :class="[
             'w-72 max-w-[85vw] bg-white border-r flex flex-col',
             'fixed inset-y-0 left-0 z-40 transform transition-transform duration-200 lg:static lg:z-auto lg:translate-x-0',
             showPageTree ? 'translate-x-0' : '-translate-x-full'
           ]">
      <div class="p-4 border-b">
        <button @click="goBack" class="text-sm text-gray-500 hover:text-indigo-600 flex items-center gap-1 mb-2">
          ← Spaces
        </button>
        <div class="flex items-center justify-between">
          <h2 class="font-bold text-gray-800 truncate">{{ space?.name }}</h2>
          <button @click="createNewPage()"
                  class="bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg px-2.5 py-1 text-xs font-medium transition"
                  title="Yeni Sayfa">
            ＋
          </button>
        </div>
      </div>

      <!-- Page Tree -->
      <div class="flex-1 overflow-y-auto p-3">
        <PageTree
            :pages="pageTree"
            :selectedPageId="selectedPageId"
            @select="selectPage"
        />
      </div>
    </aside>

    <!-- Sağ Panel: İçerik -->
    <main class="flex-1 flex flex-col overflow-hidden">
      <!-- Üst Bar -->
      <header v-if="currentPage" class="bg-white border-b px-3 sm:px-6 py-3 flex flex-wrap items-center justify-between gap-y-2">
        <div class="flex items-center gap-3 min-w-0 flex-1 mr-4">
          <!-- Mobil: sayfa ağacını aç -->
          <button @click="showPageTree = true"
                  class="lg:hidden shrink-0 p-1.5 rounded-lg text-gray-500 hover:text-indigo-600 hover:bg-gray-100 transition"
                  title="Sayfalar">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
            </svg>
          </button>
          <template v-if="renamingTitle">
            <input ref="titleInput" v-model="titleDraft" type="text"
                   class="text-lg font-semibold text-gray-800 border-b-2 border-indigo-400 outline-none bg-indigo-50/50 rounded-t px-2 py-0.5 flex-1 min-w-0"
                   @keyup.enter="saveTitle"
                   @keyup.esc="cancelRename"
                   @blur="saveTitle"/>
          </template>
          <template v-else>
            <h1 @click="startRename"
                class="text-lg font-semibold text-gray-800 truncate cursor-text hover:bg-gray-100 rounded px-2 py-0.5 -mx-2 transition group flex items-center gap-2"
                title="Yeniden adlandırmak için tıklayın">
              {{ currentPage.title }}
              <span class="text-gray-300 group-hover:text-gray-400 text-sm">✏️</span>
            </h1>
          </template>
          <span class="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded shrink-0">v{{ currentPage.versionNumber }}</span>
        </div>
        <div class="flex items-center gap-2">
          <button @click="showVersions = !showVersions"
                  class="text-sm text-gray-500 hover:text-indigo-600 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition">
            🕒 Geçmiş
          </button>
          <button @click="showAttachments = !showAttachments"
                  class="text-sm text-gray-500 hover:text-indigo-600 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition">
            📎 Dosyalar
          </button>
          <button @click="showPermissions = !showPermissions"
                  class="text-sm text-gray-500 hover:text-indigo-600 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition">
            🔒 Yetkiler
          </button>
          <button v-if="!editing" @click="startEditing"
                  class="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-1.5 rounded-lg text-sm transition">
            ✏️ Düzenle
          </button>
          <template v-else>
            <button @click="savePage"
                    class="bg-green-600 hover:bg-green-700 text-white px-4 py-1.5 rounded-lg text-sm transition">
              💾 Kaydet
            </button>
            <button @click="cancelEditing"
                    class="text-gray-500 hover:text-gray-700 px-3 py-1.5 rounded-lg hover:bg-gray-100 text-sm transition">
              İptal
            </button>
          </template>
        </div>
      </header>

      <!-- İçerik Alanı -->
      <div v-if="currentPage" class="flex-1 overflow-y-auto">
        <div v-if="editing" class="h-full">
          <TiptapEditor
              v-model="editContent"
              :projectId="projectId"
              :spaceId="spaceId"
              :pageId="currentPage.id"
              class="h-full"
          />
        </div>
        <div v-else class="prose prose-indigo max-w-none p-4 sm:p-8 overflow-x-auto" v-html="renderedContent"></div>
      </div>

      <!-- Boş durum -->
      <div v-else class="flex-1 flex items-center justify-center">
        <div class="text-center px-4">
          <div class="text-5xl mb-3">📄</div>
          <p class="text-gray-500">Bir sayfa seçin veya yeni bir sayfa oluşturun</p>
          <button @click="showPageTree = true"
                  class="lg:hidden mt-4 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg text-sm transition">
            Sayfaları Göster
          </button>
        </div>
      </div>
    </main>

    <!-- Sağ Yan Panel: Versiyonlar -->
    <aside v-if="showVersions" class="fixed inset-y-0 right-0 z-40 w-80 max-w-[85vw] shadow-xl lg:static lg:z-auto lg:shadow-none bg-white border-l flex flex-col">
      <VersionHistory
          :projectId="projectId"
          :spaceId="spaceId"
          :pageId="currentPage?.id"
          @restore="onVersionRestored"
          @close="showVersions = false"
      />
    </aside>

    <!-- Sağ Yan Panel: Dosyalar -->
    <aside v-if="showAttachments" class="fixed inset-y-0 right-0 z-40 w-80 max-w-[85vw] shadow-xl lg:static lg:z-auto lg:shadow-none bg-white border-l flex flex-col">
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
        title="📄 Yeni Sayfa"
        label="Sayfa başlığı"
        placeholder="Örn: Kurulum Rehberi"
        confirmText="Oluştur"
        @confirm="confirmCreatePage"
        @cancel="showNewPageDialog = false"
    />

    <!-- Kaydet: Değişiklik Özeti Dialog -->
    <InputDialog
        v-if="showSaveDialog"
        title="💾 Sayfayı Kaydet"
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

// Başlık yeniden adlandırma state
const renamingTitle = ref(false)
const titleDraft = ref('')
const titleInput = ref(null)

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
  border: 1px solid #d1d5db;
  padding: 0.5rem 0.75rem;
}

.prose th {
  background-color: #f3f4f6;
  font-weight: 600;
}

.prose img {
  max-width: 100%;
  height: auto;
  border-radius: 0.5rem;
}
</style>
