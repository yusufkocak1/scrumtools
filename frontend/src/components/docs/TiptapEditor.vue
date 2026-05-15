<template>
  <div class="tiptap-editor h-full flex flex-col">
    <!-- Main Toolbar -->
    <div class="bg-white border-b px-4 py-2 flex flex-wrap items-center gap-1">
      <!-- Mode toggle -->
      <div v-if="!compact" class="flex border rounded-lg overflow-hidden mr-2">
        <button @click="switchMode('visual')"
                :class="['px-2.5 py-1 text-xs font-medium transition',
                  editorMode === 'visual' ? 'bg-indigo-500 text-white' : 'bg-gray-50 text-gray-600 hover:bg-gray-100']">
          ✏️ Görsel
        </button>
        <button @click="switchMode('html')"
                :class="['px-2.5 py-1 text-xs font-medium transition',
                  editorMode === 'html' ? 'bg-indigo-500 text-white' : 'bg-gray-50 text-gray-600 hover:bg-gray-100']">
          &lt;/&gt; HTML
        </button>
      </div>

      <template v-if="editorMode === 'visual'">
        <button @click="editor?.chain().focus().toggleBold().run()"
                :class="toolBtnClass(editor?.isActive('bold'))" title="Kalın">
          <strong>B</strong>
        </button>
        <button @click="editor?.chain().focus().toggleItalic().run()"
                :class="toolBtnClass(editor?.isActive('italic'))" title="İtalik">
          <em>I</em>
        </button>
        <button @click="editor?.chain().focus().toggleStrike().run()"
                :class="toolBtnClass(editor?.isActive('strike'))" title="Üstü çizili">
          <s>S</s>
        </button>
        <span class="w-px h-5 bg-gray-300 mx-1"></span>

        <button @click="editor?.chain().focus().toggleHeading({level: 1}).run()"
                :class="toolBtnClass(editor?.isActive('heading', {level: 1}))" title="Başlık 1">
          H1
        </button>
        <button @click="editor?.chain().focus().toggleHeading({level: 2}).run()"
                :class="toolBtnClass(editor?.isActive('heading', {level: 2}))" title="Başlık 2">
          H2
        </button>
        <button @click="editor?.chain().focus().toggleHeading({level: 3}).run()"
                :class="toolBtnClass(editor?.isActive('heading', {level: 3}))" title="Başlık 3">
          H3
        </button>
        <span class="w-px h-5 bg-gray-300 mx-1"></span>

        <button @click="editor?.chain().focus().toggleBulletList().run()"
                :class="toolBtnClass(editor?.isActive('bulletList'))" title="Madde listesi">
          • List
        </button>
        <button @click="editor?.chain().focus().toggleOrderedList().run()"
                :class="toolBtnClass(editor?.isActive('orderedList'))" title="Numaralı liste">
          1. List
        </button>
        <button @click="editor?.chain().focus().toggleBlockquote().run()"
                :class="toolBtnClass(editor?.isActive('blockquote'))" title="Alıntı">
          ❝
        </button>
        <span class="w-px h-5 bg-gray-300 mx-1"></span>

        <button @click="editor?.chain().focus().toggleCodeBlock().run()"
                :class="toolBtnClass(editor?.isActive('codeBlock'))" title="Kod bloğu">
          &lt;/&gt;
        </button>
        <button @click="editor?.chain().focus().toggleHighlight().run()"
                :class="toolBtnClass(editor?.isActive('highlight'))" title="Vurgula">
          🖍
        </button>
        <button @click="setLink" :class="toolBtnClass(editor?.isActive('link'))" title="Link">
          🔗
        </button>
        <span class="w-px h-5 bg-gray-300 mx-1"></span>

        <button @click="showImageModal = true" class="px-2 py-1 rounded text-sm hover:bg-gray-100" title="Resim ekle">
          🖼️
        </button>
        <button @click="insertTable" class="px-2 py-1 rounded text-sm hover:bg-gray-100" title="Tablo ekle">
          📊
        </button>
        <button @click="uploadFile" v-if="!compact" class="px-2 py-1 rounded text-sm hover:bg-gray-100" title="Dosya yükle">
          📎
        </button>
        <span v-if="!compact" class="w-px h-5 bg-gray-300 mx-1"></span>
        <button v-if="!compact" @click="showMarkdownImport = true" class="px-2 py-1 rounded text-sm hover:bg-gray-100"
                title="Markdown içe aktar">
          📥 MD
        </button>
      </template>
    </div>

    <!-- Table Toolbar (visible when cursor is inside a table) -->
    <div v-if="editorMode === 'visual' && editor?.isActive('table')"
         class="bg-gray-50 border-b px-4 py-1.5 flex flex-wrap items-center gap-1">
      <span class="text-xs text-gray-500 mr-2 font-medium">Tablo:</span>
      <button @click="editor?.chain().focus().addRowBefore().run()"
              class="table-tool-btn" title="Üste satır ekle">
        ↑ Satır
      </button>
      <button @click="editor?.chain().focus().addRowAfter().run()"
              class="table-tool-btn" title="Alta satır ekle">
        ↓ Satır
      </button>
      <button @click="editor?.chain().focus().addColumnBefore().run()"
              class="table-tool-btn" title="Sola sütun ekle">
        ← Sütun
      </button>
      <button @click="editor?.chain().focus().addColumnAfter().run()"
              class="table-tool-btn" title="Sağa sütun ekle">
        → Sütun
      </button>
      <span class="w-px h-4 bg-gray-300 mx-1"></span>
      <button @click="editor?.chain().focus().deleteRow().run()"
              class="table-tool-btn text-red-600 hover:!bg-red-50" title="Satır sil">
        ✕ Satır
      </button>
      <button @click="editor?.chain().focus().deleteColumn().run()"
              class="table-tool-btn text-red-600 hover:!bg-red-50" title="Sütun sil">
        ✕ Sütun
      </button>
      <span class="w-px h-4 bg-gray-300 mx-1"></span>
      <button @click="editor?.chain().focus().mergeCells().run()"
              class="table-tool-btn" title="Hücreleri birleştir">
        ⊞ Birleştir
      </button>
      <button @click="editor?.chain().focus().splitCell().run()"
              class="table-tool-btn" title="Hücreyi ayır">
        ⊟ Ayır
      </button>
      <button @click="editor?.chain().focus().toggleHeaderRow().run()"
              class="table-tool-btn" title="Başlık satırı">
        ▤ Başlık
      </button>
      <span class="w-px h-4 bg-gray-300 mx-1"></span>
      <button @click="editor?.chain().focus().deleteTable().run()"
              class="table-tool-btn text-red-600 hover:!bg-red-50" title="Tabloyu sil">
        🗑️ Tablo
      </button>
    </div>

    <!-- Visual Editor Content -->
    <div v-show="editorMode === 'visual'" class="flex-1 overflow-y-auto"
         @dragover.prevent
         @drop.prevent="onDrop">
      <editor-content :editor="editor" class="prose prose-indigo max-w-none p-6 min-h-full"/>
    </div>

    <!-- HTML Source Editor -->
    <div v-if="editorMode === 'html' && !compact" class="flex-1 overflow-hidden flex flex-col">
      <div class="px-4 py-1.5 bg-gray-50 border-b flex items-center gap-2">
        <span class="text-xs text-gray-500">HTML kaynak kodu düzenleyin. Görsel moda dönünce içerik güncellenecektir.</span>
      </div>
      <textarea
          v-model="sourceCode"
          class="flex-1 w-full p-4 font-mono text-sm bg-gray-900 text-green-400 resize-none outline-none"
          spellcheck="false"
          placeholder="HTML içeriğinizi buraya yazın veya yapıştırın..."
      ></textarea>
    </div>

    <!-- Hidden file inputs -->
    <input ref="fileInput" type="file" class="hidden" @change="onFileSelected"/>
    <input ref="imageFileInput" type="file" accept="image/*" class="hidden" @change="onImageFileSelected"/>

    <!-- Image Insert Modal -->
    <teleport to="body">
      <div v-if="showImageModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
           @click.self="showImageModal = false">
        <div class="bg-white rounded-xl shadow-2xl w-full max-w-md mx-4 overflow-hidden">
          <div class="px-6 py-4 border-b flex items-center justify-between">
            <h3 class="font-semibold text-gray-800 text-lg">🖼️ Resim Ekle</h3>
            <button @click="showImageModal = false" class="text-gray-400 hover:text-gray-600 text-xl">✕</button>
          </div>
          <div class="p-6 space-y-4">
            <!-- Tab seçimi -->
            <div class="flex border rounded-lg overflow-hidden">
              <button @click="imageTab = 'upload'"
                      :class="['flex-1 py-2 text-sm font-medium transition', imageTab === 'upload' ? 'bg-indigo-500 text-white' : 'bg-gray-50 text-gray-600 hover:bg-gray-100']">
                📁 Dosya Yükle
              </button>
              <button @click="imageTab = 'url'"
                      :class="['flex-1 py-2 text-sm font-medium transition', imageTab === 'url' ? 'bg-indigo-500 text-white' : 'bg-gray-50 text-gray-600 hover:bg-gray-100']">
                🔗 URL ile Ekle
              </button>
            </div>

            <!-- Upload Tab -->
            <div v-if="imageTab === 'upload'">
              <label class="block w-full border-2 border-dashed border-gray-300 rounded-lg p-6 text-center cursor-pointer hover:border-indigo-400 hover:bg-indigo-50 transition">
                <input type="file" accept="image/*" class="hidden" @change="onImageModalFileSelect"/>
                <div v-if="imageUploading" class="text-indigo-600">
                  <div class="animate-spin inline-block w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full mb-2"></div>
                  <p class="text-sm">Yükleniyor...</p>
                </div>
                <div v-else>
                  <div class="text-3xl mb-2">📷</div>
                  <p class="text-sm text-gray-500">Resim seçmek için tıklayın</p>
                  <p class="text-xs text-gray-400 mt-1">PNG, JPG, GIF, WebP - Maks. 10MB</p>
                </div>
              </label>
            </div>

            <!-- URL Tab -->
            <div v-if="imageTab === 'url'" class="space-y-3">
              <div>
                <label class="text-sm text-gray-600 block mb-1">Resim URL'si</label>
                <input v-model="imageUrl" type="url" placeholder="https://example.com/image.png"
                       class="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-300 focus:border-indigo-400 outline-none"
                       @keyup.enter="insertImageFromUrl"/>
              </div>
              <div v-if="imageUrl" class="border rounded-lg p-2 bg-gray-50">
                <img :src="imageUrl" alt="Önizleme" class="max-h-32 mx-auto rounded"
                     @error="imagePreviewError = true" @load="imagePreviewError = false"/>
                <p v-if="imagePreviewError" class="text-xs text-red-500 text-center mt-1">Resim yüklenemedi</p>
              </div>
              <button @click="insertImageFromUrl"
                      :disabled="!imageUrl || imagePreviewError"
                      class="w-full py-2 bg-indigo-500 text-white rounded-lg text-sm font-medium hover:bg-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed transition">
                Ekle
              </button>
            </div>
          </div>
        </div>
      </div>
    </teleport>

    <!-- Markdown Import Modal -->
    <teleport to="body">
      <div v-if="showMarkdownImport" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
           @click.self="showMarkdownImport = false">
        <div class="bg-white rounded-xl shadow-2xl w-full max-w-2xl mx-4 overflow-hidden flex flex-col"
             style="max-height: 80vh;">
          <div class="px-6 py-4 border-b flex items-center justify-between shrink-0">
            <h3 class="font-semibold text-gray-800 text-lg">📥 Markdown İçe Aktar</h3>
            <button @click="showMarkdownImport = false" class="text-gray-400 hover:text-gray-600 text-xl">✕</button>
          </div>
          <div class="p-6 flex flex-col gap-4 flex-1 min-h-0">
            <p class="text-sm text-gray-500 shrink-0">
              Markdown formatında metni aşağıya yapıştırın. İçerik otomatik olarak zengin formata dönüştürülecektir.
            </p>

            <!-- Import mode: replace or append -->
            <div class="flex gap-3 shrink-0">
              <label class="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                <input type="radio" v-model="mdImportMode" value="replace" class="text-indigo-500"/>
                Tüm içeriği değiştir
              </label>
              <label class="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                <input type="radio" v-model="mdImportMode" value="append" class="text-indigo-500"/>
                Cursor konumuna ekle
              </label>
            </div>

            <textarea
                v-model="mdImportContent"
                class="flex-1 w-full border rounded-lg p-3 font-mono text-sm resize-none outline-none focus:ring-2 focus:ring-indigo-300 focus:border-indigo-400"
                placeholder="# Başlık&#10;&#10;Paragraf metni burada...&#10;&#10;- Madde 1&#10;- Madde 2&#10;&#10;**Kalın** ve *italik* metin"
                spellcheck="false"
            ></textarea>

            <!-- Preview -->
            <div v-if="mdImportContent" class="shrink-0">
              <button @click="mdShowPreview = !mdShowPreview"
                      class="text-xs text-indigo-600 hover:text-indigo-800 mb-1">
                {{ mdShowPreview ? '▼ Önizlemeyi gizle' : '▶ Önizlemeyi göster' }}
              </button>
              <div v-if="mdShowPreview"
                   class="border rounded-lg p-3 bg-gray-50 prose prose-sm prose-indigo max-w-none max-h-40 overflow-y-auto"
                   v-html="mdPreviewHtml">
              </div>
            </div>

            <div class="flex gap-2 justify-end shrink-0">
              <button @click="showMarkdownImport = false"
                      class="px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg transition">
                İptal
              </button>
              <button @click="applyMarkdownImport"
                      :disabled="!mdImportContent.trim()"
                      class="px-4 py-2 bg-indigo-500 text-white text-sm font-medium rounded-lg hover:bg-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed transition">
                İçe Aktar
              </button>
            </div>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import {ref, onBeforeUnmount, watch, computed} from 'vue'
import {useEditor, EditorContent} from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import {Table} from '@tiptap/extension-table'
import {TableRow} from '@tiptap/extension-table-row'
import {TableCell} from '@tiptap/extension-table-cell'
import {TableHeader} from '@tiptap/extension-table-header'
import Highlight from '@tiptap/extension-highlight'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import Placeholder from '@tiptap/extension-placeholder'
import {common, createLowlight} from 'lowlight'
import {marked} from 'marked'
import DOMPurify from 'dompurify'
import DocApi from '../../api/DocApi.js'

const lowlight = createLowlight(common)

const props = defineProps({
  modelValue: {type: String, default: ''},
  projectId: {type: String, default: null},
  spaceId: {type: String, default: null},
  pageId: {type: String, default: null},
  /** External upload handler: (file: File) => Promise<{ downloadUrl, fileName }> */
  uploadHandler: {type: Function, default: null},
  placeholder: {type: String, default: 'İçeriğinizi yazmaya başlayın...'},
  /** Compact mode – hides some toolbar buttons (markdown import, file upload, HTML mode) */
  compact: {type: Boolean, default: false}
})

const emit = defineEmits(['update:modelValue'])
const fileInput = ref(null)
const imageFileInput = ref(null)

// Editor mode: 'visual' | 'html'
const editorMode = ref('visual')
const sourceCode = ref('')

// Image modal state
const showImageModal = ref(false)
const imageTab = ref('upload')
const imageUrl = ref('')
const imagePreviewError = ref(false)
const imageUploading = ref(false)

// Markdown import modal state
const showMarkdownImport = ref(false)
const mdImportContent = ref('')
const mdImportMode = ref('replace')
const mdShowPreview = ref(false)

const mdPreviewHtml = computed(() => {
  if (!mdImportContent.value) return ''
  try {
    const html = marked(mdImportContent.value)
    return DOMPurify.sanitize(html)
  } catch {
    return '<p class="text-red-500">Önizleme oluşturulamadı</p>'
  }
})

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({codeBlock: false}),
    Image.configure({
      inline: false,
      allowBase64: true,
      HTMLAttributes: {
        class: 'editor-image',
      },
    }),
    Link.configure({openOnClick: false}),
    Table.configure({resizable: true}),
    TableRow,
    TableCell,
    TableHeader,
    Highlight,
    CodeBlockLowlight.configure({lowlight}),
    Placeholder.configure({placeholder: props.placeholder})
  ],
  onUpdate: ({editor}) => {
    emit('update:modelValue', editor.getHTML())
  },
  editorProps: {
    handlePaste(view, event) {
      const items = event.clipboardData?.items
      if (!items) return false

      for (const item of items) {
        if (item.type.startsWith('image/')) {
          event.preventDefault()
          const file = item.getAsFile()
          if (file) handleImageUpload(file)
          return true
        }
      }
      return false
    },
    handleDrop(view, event) {
      const files = event.dataTransfer?.files
      if (!files || files.length === 0) return false

      const imageFiles = Array.from(files).filter(f => f.type.startsWith('image/'))
      if (imageFiles.length === 0) return false

      event.preventDefault()
      imageFiles.forEach(file => handleImageUpload(file))
      return true
    }
  }
})

// ─── Mode Switching ───────────────────────────────────────────────────────────

function switchMode(mode) {
  if (mode === editorMode.value) return

  if (mode === 'html') {
    // Visual → HTML: get current HTML from editor
    sourceCode.value = formatHtml(editor.value?.getHTML() || '')
    editorMode.value = 'html'
  } else if (mode === 'visual') {
    // HTML → Visual: apply source code back to editor
    if (editor.value) {
      editor.value.commands.setContent(sourceCode.value, false)
      emit('update:modelValue', editor.value.getHTML())
    }
    editorMode.value = 'visual'
  }
}

function formatHtml(html) {
  // Basit bir HTML formatlama - okunabilirlik için
  if (!html) return ''
  return html
      .replace(/></g, '>\n<')
      .replace(/\n{3,}/g, '\n\n')
}

// ─── Markdown Import ──────────────────────────────────────────────────────────

function applyMarkdownImport() {
  if (!mdImportContent.value.trim()) return

  try {
    const html = marked(mdImportContent.value)
    const sanitized = DOMPurify.sanitize(html, {
      ADD_TAGS: ['img', 'table', 'thead', 'tbody', 'tr', 'th', 'td'],
      ADD_ATTR: ['src', 'alt', 'href', 'target', 'colspan', 'rowspan']
    })

    if (mdImportMode.value === 'replace') {
      editor.value?.commands.setContent(sanitized, false)
    } else {
      editor.value?.chain().focus().insertContent(sanitized).run()
    }
    emit('update:modelValue', editor.value?.getHTML() || '')
  } catch (e) {
    console.error('Markdown dönüştürme hatası:', e)
  }

  showMarkdownImport.value = false
  mdImportContent.value = ''
  mdShowPreview.value = false
}

watch(() => props.modelValue, (val) => {
  if (editor.value && editor.value.getHTML() !== val) {
    editor.value.commands.setContent(val, false)
  }
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})

function toolBtnClass(isActive) {
  return [
    'px-2 py-1 rounded text-sm transition',
    isActive ? 'bg-indigo-100 text-indigo-700' : 'hover:bg-gray-100 text-gray-700'
  ]
}

function setLink() {
  const url = prompt('URL girin:')
  if (url) {
    editor.value.chain().focus().setLink({href: url}).run()
  }
}

// ─── Image Functions ──────────────────────────────────────────────────────────

async function handleImageUpload(file) {
  if (!file) return

  // Try external upload handler first (for task context etc.)
  if (props.uploadHandler) {
    try {
      const result = await props.uploadHandler(file)
      if (result?.downloadUrl) {
        editor.value?.chain().focus().setImage({src: result.downloadUrl, alt: result.fileName || file.name}).run()
        return
      }
    } catch (e) {
      console.error('External image upload failed:', e)
    }
    // Fallback to base64
    const reader = new FileReader()
    reader.onload = (ev) => {
      editor.value?.chain().focus().setImage({src: ev.target.result, alt: file.name}).run()
    }
    reader.readAsDataURL(file)
    return
  }

  // Doc context upload (original behavior)
  if (!props.pageId) {
    const reader = new FileReader()
    reader.onload = (e) => {
      editor.value?.chain().focus().setImage({src: e.target.result, alt: file.name}).run()
    }
    reader.readAsDataURL(file)
    return
  }

  try {
    const res = await DocApi.uploadAttachment(props.projectId, props.spaceId, props.pageId, file)
    const attachment = res.data
    editor.value?.chain().focus().setImage({src: attachment.downloadUrl, alt: attachment.fileName}).run()
  } catch (e) {
    console.error('Resim yüklenemedi:', e)
    const reader = new FileReader()
    reader.onload = (ev) => {
      editor.value?.chain().focus().setImage({src: ev.target.result, alt: file.name}).run()
    }
    reader.readAsDataURL(file)
  }
}

function insertImageFromUrl() {
  if (imageUrl.value && !imagePreviewError.value) {
    editor.value?.chain().focus().setImage({src: imageUrl.value}).run()
    showImageModal.value = false
    imageUrl.value = ''
  }
}

async function onImageModalFileSelect(event) {
  const file = event.target.files[0]
  if (!file) return

  imageUploading.value = true
  await handleImageUpload(file)
  imageUploading.value = false
  showImageModal.value = false
  event.target.value = ''
}

function onImageFileSelected(event) {
  const file = event.target.files[0]
  if (!file) return
  handleImageUpload(file)
  event.target.value = ''
}

// ─── Table Functions ──────────────────────────────────────────────────────────

function insertTable() {
  editor.value.chain().focus().insertTable({rows: 3, cols: 3, withHeaderRow: true}).run()
}

// ─── File Upload ──────────────────────────────────────────────────────────────

function uploadFile() {
  fileInput.value?.click()
}

async function onFileSelected(event) {
  const file = event.target.files[0]
  if (!file) return

  // Try external upload handler first
  if (props.uploadHandler) {
    try {
      const result = await props.uploadHandler(file)
      if (result?.downloadUrl) {
        if (file.type?.startsWith('image/')) {
          editor.value.chain().focus().setImage({src: result.downloadUrl, alt: result.fileName || file.name}).run()
        } else {
          editor.value.chain().focus().insertContent(
              `<a href="${result.downloadUrl}" target="_blank">${result.fileName || file.name}</a>`
          ).run()
        }
      }
    } catch (e) {
      console.error('File upload failed:', e)
    }
    event.target.value = ''
    return
  }

  // Doc context upload (original behavior)
  if (!props.pageId) { event.target.value = ''; return }

  try {
    const res = await DocApi.uploadAttachment(props.projectId, props.spaceId, props.pageId, file)
    const attachment = res.data

    if (attachment.mimeType?.startsWith('image/')) {
      editor.value.chain().focus().setImage({src: attachment.downloadUrl, alt: attachment.fileName}).run()
    } else {
      editor.value.chain().focus().insertContent(
          `<a href="${attachment.downloadUrl}" target="_blank">${attachment.fileName}</a>`
      ).run()
    }
  } catch (e) {
    console.error('Dosya yüklenemedi:', e)
  }

  event.target.value = ''
}

// ─── Drag & Drop ──────────────────────────────────────────────────────────────

function onDrop(event) {
  // Handled by editor's handleDrop, this is fallback
}
</script>

<style>
.tiptap-editor .ProseMirror {
  outline: none;
  min-height: 100%;
}

.tiptap-editor .ProseMirror p.is-editor-empty:first-child::before {
  color: #adb5bd;
  content: attr(data-placeholder);
  float: left;
  height: 0;
  pointer-events: none;
}

/* ─── Table Styles ───────────────────────────────────────────────────────── */

.tiptap-editor .ProseMirror table {
  border-collapse: collapse;
  margin: 1rem 0;
  width: 100%;
  table-layout: fixed;
  overflow: hidden;
}

.tiptap-editor .ProseMirror th,
.tiptap-editor .ProseMirror td {
  border: 2px solid #d1d5db;
  padding: 0.5rem 0.75rem;
  min-width: 80px;
  position: relative;
  vertical-align: top;
}

.tiptap-editor .ProseMirror th {
  background-color: #f3f4f6;
  font-weight: 600;
}

.tiptap-editor .ProseMirror td > *,
.tiptap-editor .ProseMirror th > * {
  margin: 0;
}

/* Selected cells */
.tiptap-editor .ProseMirror .selectedCell::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(99, 102, 241, 0.1);
  pointer-events: none;
  z-index: 2;
}

/* Column resize handle */
.tiptap-editor .ProseMirror .column-resize-handle {
  position: absolute;
  right: -2px;
  top: 0;
  bottom: -2px;
  width: 4px;
  background-color: #6366f1;
  cursor: col-resize;
  z-index: 10;
}

.tiptap-editor .ProseMirror.resize-cursor {
  cursor: col-resize;
}

/* ─── Image Styles ───────────────────────────────────────────────────────── */

.tiptap-editor .ProseMirror img.editor-image {
  max-width: 100%;
  height: auto;
  border-radius: 0.5rem;
  margin: 1rem 0;
  cursor: default;
  transition: box-shadow 0.2s;
}

.tiptap-editor .ProseMirror img.editor-image.ProseMirror-selectednode {
  outline: 3px solid #6366f1;
  outline-offset: 2px;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.2);
}

/* ─── Table Toolbar Button ───────────────────────────────────────────────── */

.table-tool-btn {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
  color: #374151;
  transition: all 0.15s;
  white-space: nowrap;
}

.table-tool-btn:hover {
  background-color: #e5e7eb;
}
</style>

