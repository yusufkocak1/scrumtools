<template>
  <div class="flex flex-row w-screen bg-gray-50 min-h-screen">
    <SideBar :team-id="teamId" class="hidden lg:flex" />
    <div class="flex-1 p-6">
      <div class="mx-auto">
        <!-- Header -->
        <div class="mb-8">
          <h1 class="text-4xl font-bold text-gray-900 mb-4">Code Sharing</h1>
          <p class="text-xl text-gray-600">Share code snippets with your teammates and edit collaboratively.</p>
        </div>

        <!-- Controls -->
        <div class="bg-white rounded-xl shadow-lg border border-gray-200 p-8 mb-8">
          <div class="flex flex-col sm:flex-row gap-4">
            <div class="flex-1">
              <label for="tag-input" class="block text-sm font-medium text-gray-900 mb-2">Tag</label>
              <div class="relative">
                <input id="tag-input" v-model="tag" type="text"
                       class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 bg-white text-gray-900 placeholder-gray-500 transition-all duration-200"
                       placeholder="Enter a tag for the code snippet..." @keyup.enter="handleGetCode" />
                <div v-if="isLoading" class="absolute right-3 top-1/2 -translate-y-1/2">
                  <div class="animate-spin rounded-full h-5 w-5 border-b-2 border-orange-500"></div>
                </div>
              </div>
            </div>
            <div class="flex gap-3 sm:items-end">
              <button @click="handleGetCode" :disabled="!tag.trim() || isLoading"
                      class="px-6 py-3 bg-orange-600 hover:bg-orange-700 disabled:bg-gray-400 text-white font-medium rounded-lg transition-all duration-200 focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 disabled:cursor-not-allowed flex items-center gap-2 transform hover:scale-105 active:scale-95">
                <svg v-if="!isLoading" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
                <span>{{ isLoading ? 'Loading...' : 'Fetch' }}</span>
              </button>
              <button @click="handleSaveCode" :disabled="!tag.trim() || !data.trim() || isSaving"
                      class="px-6 py-3 bg-orange-500 hover:bg-orange-600 disabled:bg-gray-400 text-white font-medium rounded-lg transition-all duration-200 focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 disabled:cursor-not-allowed flex items-center gap-2 transform hover:scale-105 active:scale-95">
                <svg v-if="!isSaving" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.367 2.684 3 3 0 00-5.367-2.684z" />
                </svg>
                <span>{{ isSaving ? 'Saving...' : 'Share' }}</span>
              </button>
            </div>
          </div>
          <!-- Recent Tags -->
          <div v-if="recentTags.length > 0" class="mt-6 pt-6 border-t border-gray-200">
            <p class="text-sm font-medium text-gray-900 mb-3">Recently used tags:</p>
            <div class="flex flex-wrap gap-2">
              <button v-for="recentTag in recentTags" :key="recentTag" @click="selectRecentTag(recentTag)"
                      class="px-4 py-2 text-sm bg-orange-50 text-orange-700 border border-orange-200 rounded-full hover:bg-orange-100 transition-all duration-200 transform hover:scale-105">
                {{ recentTag }}
              </button>
            </div>
          </div>
        </div>

        <!-- Editor -->
        <div class="bg-white rounded-xl shadow-lg border border-gray-200 overflow-hidden">
          <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 bg-gray-50">
            <div class="flex items-center gap-3">
              <div class="flex gap-1">
                <div class="w-3 h-3 bg-red-500 rounded-full"></div>
                <div class="w-3 h-3 bg-yellow-500 rounded-full"></div>
                <div class="w-3 h-3 bg-green-500 rounded-full"></div>
              </div>
              <span class="text-sm font-medium text-gray-900">{{ tag || 'Untitled' }}</span>
            </div>
            <div class="flex items-center gap-2">
              <span v-if="lastSaved" class="text-xs text-gray-500">Last saved: {{ formatTime(lastSaved) }}</span>
              <select v-model="selectedLanguage" @change="onLanguageChange"
                      class="px-2 py-1 text-xs border border-gray-300 rounded-md bg-white text-gray-700 focus:ring-2 focus:ring-orange-500 focus:border-orange-500">
                <option v-for="lang in languageOptions" :key="lang.value" :value="lang.value">{{ lang.label }}</option>
              </select>
              <!-- Theme Selector -->
              <select v-model="selectedTheme" @change="onThemeChange"
                      class="px-2 py-1 text-xs border border-gray-300 rounded-md bg-white text-gray-700 focus:ring-2 focus:ring-orange-500 focus:border-orange-500">
                <option v-for="opt in themeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
              <button @click="copyToClipboard" class="p-2 text-gray-500 hover:text-orange-600 rounded-lg hover:bg-orange-50 transition-all duration-200 transform hover:scale-110" title="Copy to clipboard">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                </svg>
              </button>
            </div>
          </div>
          <div class="h-screen">
            <div v-if="isChangingLanguage" class="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center z-10">
              <div class="flex items-center gap-2 text-orange-600">
                <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-orange-500"></div>
                <span class="text-sm">Changing language...</span>
              </div>
            </div>
            <MonacoEditor
              v-model="data"
              :language="language"
              :theme="selectedTheme"
              class="h-screen"
              @editorReady="onEditorReady"
            />
          </div>
        </div>

        <!-- Status -->
        <div v-if="statusMessage" class="mt-6">
          <div :class="[
              'p-4 rounded-xl flex items-center gap-3 border',
              statusMessage.type === 'success' ? 'bg-green-50 border-green-200 text-green-800' :
              statusMessage.type === 'error' ? 'bg-red-50 border-red-200 text-red-800' :
              'bg-orange-50 border-orange-200 text-orange-800'
            ]">
            <svg v-if="statusMessage.type === 'success'" class="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" /></svg>
            <svg v-else-if="statusMessage.type === 'error'" class="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" /></svg>
            <svg v-else class="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" /></svg>
            <span class="font-medium">{{ statusMessage.text }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import SideBar from '../components/SideBar.vue'
import { defineAsyncComponent } from 'vue'
const MonacoEditor = defineAsyncComponent({
  loader: () => import('../components/share/MonacoEditor.vue'),
  delay: 50,
  timeout: 20000,
  onError (err, retry, fail, attempts) { if (attempts <= 2) retry(); else fail() }
})
import { getCodeShare, saveCodeShare } from '../firebase/codeShareService.js'

export default {
  name: 'CodeShare',
  components: { SideBar, MonacoEditor },
  props: { teamId: { type: String, required: true } },
  data () {
    return {
      data: '',
      tag: '',
      isLoading: false,
      isSaving: false,
      lastSaved: null,
      recentTags: [],
      statusMessage: null,
      editorInstance: null,
      currentRequestId: 0,
      selectedLanguage: localStorage.getItem('codeShareLang') || 'javascript',
      language: localStorage.getItem('codeShareLang') || 'javascript',
      languageOptions: [
        { value: 'javascript', label: 'JavaScript' },
        { value: 'typescript', label: 'TypeScript' },
        { value: 'json', label: 'JSON' },
        { value: 'html', label: 'HTML' },
        { value: 'css', label: 'CSS' },
        { value: 'xml', label: 'XML' },
        { value: 'python', label: 'Python' },
        { value: 'java', label: 'Java' },
        { value: 'cpp', label: 'C++' },
        { value: 'sql', label: 'SQL' },
        { value: 'php', label: 'PHP' },
        { value: 'shell', label: 'Shell' }
      ],
      // Tema seçimleri
      selectedTheme: localStorage.getItem('codeShareTheme') || 'vs-light',
      themeOptions: [
        { value: 'vs-light', label: 'Light' },
        { value: 'vs-dark', label: 'Dark' },
        { value: 'hc-black', label: 'High Contrast' }
      ],
      isEditorLoading: true,
      isChangingLanguage: false
    }
  },
  mounted () {
    this.loadRecentTags()
    const savedTag = localStorage.getItem('tag')
    if (savedTag) { this.tag = savedTag; this.getCode() }
  },
  methods: {
    loadRecentTags () {
      const stored = localStorage.getItem('recentCodeTags')
      if (stored) this.recentTags = JSON.parse(stored).slice(0, 5)
    },
    addToRecentTags (val) {
      if (!val.trim()) return
      const arr = [...this.recentTags]
      const i = arr.indexOf(val)
      if (i > -1) arr.splice(i, 1)
      arr.unshift(val)
      this.recentTags = arr.slice(0, 5)
      localStorage.setItem('recentCodeTags', JSON.stringify(this.recentTags))
    },
    showStatusMessage (text, type = 'info', duration = 3000) {
      this.statusMessage = { text, type }
      setTimeout(() => { this.statusMessage = null }, duration)
    },
    async saveCode () {
      if (!this.tag.trim() || !this.data.trim()) return
      try {
        this.isSaving = true
        await saveCodeShare(this.teamId, this.data, this.tag)
        this.lastSaved = new Date()
        this.addToRecentTags(this.tag)
        this.showStatusMessage('Code saved successfully!', 'success')
      } catch (e) {
        console.error(e)
        this.showStatusMessage('Error while saving!', 'error')
      } finally { this.isSaving = false }
    },
    async getCode () {
      if (!this.tag.trim()) return
      const requestId = ++this.currentRequestId
      this.isLoading = true
      try {
        await getCodeShare(this.teamId, this.tag, (res) => {
          if (requestId !== this.currentRequestId) return
            if (res) {
            localStorage.setItem('tag', this.tag)
            this.data = res.data || ''
            this.addToRecentTags(this.tag)
            this.showStatusMessage('Code loaded successfully!', 'success')
          } else {
            this.data = ''
            this.showStatusMessage('No code found for this tag.', 'info')
          }
        })
      } catch (e) {
        if (requestId === this.currentRequestId) {
          console.error(e)
          this.data = ''
          this.showStatusMessage('Error while loading code!', 'error')
        }
      } finally {
        if (requestId === this.currentRequestId) {
          this.isLoading = false
        }
      }
    },
    onLanguageChange () {
      localStorage.setItem('codeShareLang', this.selectedLanguage)
      let tempData = this.data
      this.data = null
      this.language = this.selectedLanguage
      this.data = tempData
    },
    onThemeChange () {
      localStorage.setItem('codeShareTheme', this.selectedTheme)
      // Monaco bileşeni prop değişimini otomatik işler, ekstra işlem gerekmez
    },
    onLanguageChanging (isChanging) {
      this.isChangingLanguage = isChanging
    },
    selectRecentTag (val) {
      this.tag = val
      this.getCode()
    },
    async copyToClipboard () {
      try {
        await navigator.clipboard.writeText(this.data || '')
        this.showStatusMessage('Code copied to clipboard!', 'success')
      } catch (e) {
        this.showStatusMessage('Copy failed.', 'error')
      }
    },
    formatTime (d) {
      if (!d) return ''
      try {
        const date = (d instanceof Date) ? d : new Date(d)
        return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
      } catch { return '' }
    },
    onEditorReady (i) { this.editorInstance = i; this.isEditorLoading = false },
    handleSaveCode () { this.saveCode() },
    handleGetCode () { this.getCode() }
  }
}
</script>

<style scoped>
:deep(.code-editor) { @apply rounded-lg overflow-hidden; }
:deep(.code-editor::-webkit-scrollbar) { width:8px; height:8px; }
:deep(.code-editor::-webkit-scrollbar-track) { @apply bg-gray-100 dark:bg-gray-800; }
:deep(.code-editor::-webkit-scrollbar-thumb) { @apply bg-gray-300 dark:bg-gray-600 rounded; }
:deep(.code-editor::-webkit-scrollbar-thumb:hover) { @apply bg-gray-400 dark:bg-gray-500; }
</style>
