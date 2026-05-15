<template>
  <div class="markdown-editor border border-gray-300 rounded-lg overflow-hidden focus-within:ring-2 focus-within:ring-blue-500 focus-within:border-blue-500">
    <!-- Toolbar -->
    <div class="flex items-center flex-wrap gap-0.5 px-2 py-1.5 bg-gray-50 border-b border-gray-200">
      <!-- Formatting buttons -->
      <button type="button" @click="wrapSelection('**', '**')" title="Bold (Ctrl+B)"
        class="toolbar-btn" >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="M6 4h8a4 4 0 014 4 4 4 0 01-4 4H6z"/><path d="M6 12h9a4 4 0 014 4 4 4 0 01-4 4H6z"/></svg>
      </button>
      <button type="button" @click="wrapSelection('*', '*')" title="Italic (Ctrl+I)"
        class="toolbar-btn">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="19" y1="4" x2="10" y2="4"/><line x1="14" y1="20" x2="5" y2="20"/><line x1="15" y1="4" x2="9" y2="20"/></svg>
      </button>
      <button type="button" @click="wrapSelection('~~', '~~')" title="Strikethrough"
        class="toolbar-btn">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M16 4H9a3 3 0 00-3 3v0a3 3 0 003 3h6a3 3 0 013 3v0a3 3 0 01-3 3H8"/><line x1="4" y1="12" x2="20" y2="12"/></svg>
      </button>

      <div class="w-px h-5 bg-gray-300 mx-1"></div>

      <button type="button" @click="insertPrefix('# ')" title="Heading 1"
        class="toolbar-btn text-xs font-bold">H1</button>
      <button type="button" @click="insertPrefix('## ')" title="Heading 2"
        class="toolbar-btn text-xs font-bold">H2</button>
      <button type="button" @click="insertPrefix('### ')" title="Heading 3"
        class="toolbar-btn text-xs font-bold">H3</button>

      <div class="w-px h-5 bg-gray-300 mx-1"></div>

      <button type="button" @click="insertPrefix('- ')" title="Unordered List"
        class="toolbar-btn">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><circle cx="4" cy="6" r="1" fill="currentColor"/><circle cx="4" cy="12" r="1" fill="currentColor"/><circle cx="4" cy="18" r="1" fill="currentColor"/></svg>
      </button>
      <button type="button" @click="insertPrefix('1. ')" title="Ordered List"
        class="toolbar-btn">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="10" y1="6" x2="21" y2="6"/><line x1="10" y1="12" x2="21" y2="12"/><line x1="10" y1="18" x2="21" y2="18"/><text x="3" y="8" font-size="8" fill="currentColor" stroke="none">1</text><text x="3" y="14" font-size="8" fill="currentColor" stroke="none">2</text><text x="3" y="20" font-size="8" fill="currentColor" stroke="none">3</text></svg>
      </button>
      <button type="button" @click="insertPrefix('- [ ] ')" title="Task List"
        class="toolbar-btn">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7" rx="1"/><line x1="13" y1="6" x2="21" y2="6"/><rect x="3" y="14" width="7" height="7" rx="1"/><line x1="13" y1="17" x2="21" y2="17"/></svg>
      </button>

      <div class="w-px h-5 bg-gray-300 mx-1"></div>

      <button type="button" @click="wrapSelection('`', '`')" title="Inline Code"
        class="toolbar-btn">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
      </button>
      <button type="button" @click="insertCodeBlock" title="Code Block"
        class="toolbar-btn text-xs font-mono">{}</button>
      <button type="button" @click="insertPrefix('> ')" title="Blockquote"
        class="toolbar-btn">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M6 17h3l2-4V7H5v6h3zm8 0h3l2-4V7h-6v6h3z"/></svg>
      </button>
      <button type="button" @click="insertLink" title="Link"
        class="toolbar-btn">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/></svg>
      </button>
      <button type="button" @click="insertTable" title="Table"
        class="toolbar-btn">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/></svg>
      </button>
      <button type="button" @click="insertHr" title="Horizontal Rule"
        class="toolbar-btn text-xs">―</button>

      <div class="w-px h-5 bg-gray-300 mx-1"></div>

      <!-- Image Upload -->
      <label title="Upload Image" class="toolbar-btn cursor-pointer relative">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
          <circle cx="8.5" cy="8.5" r="1.5"/>
          <polyline points="21 15 16 10 5 21"/>
        </svg>
        <input type="file" accept="image/*" class="hidden" @change="handleImageUpload" :disabled="uploadingImage" />
      </label>
      <span v-if="uploadingImage" class="text-xs text-blue-500 ml-1 animate-pulse">Uploading...</span>

      <!-- Spacer -->
      <div class="flex-1"></div>

      <!-- Tab Toggle: Write / Preview -->
      <div class="flex rounded-md overflow-hidden border border-gray-300 text-xs">
        <button type="button" @click="activeTab = 'write'"
          :class="activeTab === 'write' ? 'bg-white text-gray-900 shadow-sm' : 'bg-gray-100 text-gray-500 hover:text-gray-700'"
          class="px-2.5 py-1 font-medium transition-colors">Write</button>
        <button type="button" @click="activeTab = 'preview'"
          :class="activeTab === 'preview' ? 'bg-white text-gray-900 shadow-sm' : 'bg-gray-100 text-gray-500 hover:text-gray-700'"
          class="px-2.5 py-1 font-medium transition-colors">Preview</button>
      </div>
    </div>

    <!-- Write Tab -->
    <div v-show="activeTab === 'write'">
      <textarea
        ref="textarea"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        @keydown.ctrl.b.prevent="wrapSelection('**', '**')"
        @keydown.ctrl.i.prevent="wrapSelection('*', '*')"
        rows="10"
        class="w-full px-4 py-3 text-sm font-mono resize-y focus:outline-none border-0 bg-white"
        placeholder="Markdown ile açıklama yazın... (Görselleri toolbar'daki 🖼 butonuyla ya da yapıştırarak ekleyebilirsiniz)"
        @paste="handlePaste"
        @dragover.prevent
        @drop.prevent="handleDrop"
      ></textarea>
    </div>

    <!-- Preview Tab -->
    <div v-show="activeTab === 'preview'" class="min-h-[200px] px-4 py-3 bg-white">
      <MarkdownViewer v-if="modelValue" :content="modelValue" />
      <p v-else class="text-gray-400 italic text-sm">Henüz içerik yok…</p>
    </div>
  </div>
</template>

<script>
import { uploadAttachment } from '../../api/WorkApi.js'
import MarkdownViewer from './MarkdownViewer.vue'

export default {
  name: 'MarkdownEditor',
  components: { MarkdownViewer },
  props: {
    modelValue: { type: String, default: '' },
    teamId: { type: String, default: '' },
    taskId: { type: String, default: '' }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      activeTab: 'write',
      uploadingImage: false
    }
  },
  methods: {
    /* ── Text manipulation helpers ────────────────────── */
    getTextarea() {
      return this.$refs.textarea
    },
    insertAtCursor(text) {
      const ta = this.getTextarea()
      if (!ta) return
      const start = ta.selectionStart
      const end = ta.selectionEnd
      const value = ta.value
      const newValue = value.substring(0, start) + text + value.substring(end)
      this.$emit('update:modelValue', newValue)
      this.$nextTick(() => {
        ta.focus()
        const pos = start + text.length
        ta.setSelectionRange(pos, pos)
      })
    },
    wrapSelection(before, after) {
      const ta = this.getTextarea()
      if (!ta) return
      const start = ta.selectionStart
      const end = ta.selectionEnd
      const value = ta.value
      const selected = value.substring(start, end) || 'text'
      const newValue = value.substring(0, start) + before + selected + after + value.substring(end)
      this.$emit('update:modelValue', newValue)
      this.$nextTick(() => {
        ta.focus()
        ta.setSelectionRange(start + before.length, start + before.length + selected.length)
      })
    },
    insertPrefix(prefix) {
      const ta = this.getTextarea()
      if (!ta) return
      const start = ta.selectionStart
      const value = ta.value
      // Find the beginning of the current line
      const lineStart = value.lastIndexOf('\n', start - 1) + 1
      const newValue = value.substring(0, lineStart) + prefix + value.substring(lineStart)
      this.$emit('update:modelValue', newValue)
      this.$nextTick(() => {
        ta.focus()
        ta.setSelectionRange(start + prefix.length, start + prefix.length)
      })
    },
    insertLink() {
      this.wrapSelection('[', '](url)')
    },
    insertCodeBlock() {
      this.insertAtCursor('\n```\ncode\n```\n')
    },
    insertTable() {
      this.insertAtCursor('\n| Header 1 | Header 2 | Header 3 |\n| --- | --- | --- |\n| Cell 1 | Cell 2 | Cell 3 |\n')
    },
    insertHr() {
      this.insertAtCursor('\n---\n')
    },

    /* ── Image upload ────────────────────────────────── */
    async uploadAndInsertImage(file) {
      if (!file || !file.type.startsWith('image/')) return
      if (!this.teamId || !this.taskId) {
        alert('Görsel yüklemek için task kayıtlı olmalıdır.')
        return
      }
      if (file.size > 20 * 1024 * 1024) {
        alert('Dosya boyutu 20MB\'ı aşıyor.')
        return
      }

      this.uploadingImage = true
      try {
        const result = await uploadAttachment(this.teamId, this.taskId, file)
        const imageUrl = result.downloadUrl || ''
        const mdImage = `![${file.name}](${imageUrl})\n`
        this.insertAtCursor(mdImage)
      } catch (e) {
        console.error('Image upload failed:', e)
        alert('Görsel yüklenemedi: ' + (e.response?.data?.message || e.message))
      } finally {
        this.uploadingImage = false
      }
    },
    handleImageUpload(event) {
      const file = event.target.files?.[0]
      if (file) this.uploadAndInsertImage(file)
      event.target.value = ''
    },
    handlePaste(event) {
      const items = event.clipboardData?.items
      if (!items) return
      for (const item of items) {
        if (item.type.startsWith('image/')) {
          event.preventDefault()
          const file = item.getAsFile()
          if (file) this.uploadAndInsertImage(file)
          return
        }
      }
    },
    handleDrop(event) {
      const files = event.dataTransfer?.files
      if (!files?.length) return
      for (const file of files) {
        if (file.type.startsWith('image/')) {
          this.uploadAndInsertImage(file)
        }
      }
    }
  }
}
</script>

<style scoped>
.toolbar-btn {
  @apply p-1.5 rounded text-gray-600 hover:bg-gray-200 hover:text-gray-900 transition-colors;
}
</style>

