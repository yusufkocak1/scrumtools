<template>
  <VueMonacoEditor 
    v-model:value="innerValue"
    :language="language"
    :theme="theme"
    :options="mergedOptions"
    @mount="handleMount"
  />
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { loader, VueMonacoEditor } from '@guolao/vue-monaco-editor'
import customLang from '../../..//custom-lang-monarch.js'

// VS CDN yapılandırması (bileşen içinde lokal kurulum)
loader.config({
  paths: {
    vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.52.2/min/vs'
  }
})

const props = defineProps({
  modelValue: { type: String, default: '' },
  language: { type: String, default: 'javascript' },
  theme: { type: String, default: 'vs-light' },
  options: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'editorReady'])

const innerValue = ref(props.modelValue)
let editorInstance = null
let monacoInstance = null
let customLangRegistered = false

const DEFAULT_OPTIONS = {
  automaticLayout: true,
  fontSize: 14,
  formatOnPaste: true,
  formatOnType: true,
  minimap: { enabled: true },
  scrollBeyondLastLine: false,
  smoothScrolling: true,
  wordWrap: 'on'
}
const mergedOptions = computed(() => ({ ...DEFAULT_OPTIONS, ...props.options }))

watch(() => props.modelValue, v => { if (v !== innerValue.value) innerValue.value = v })
watch(innerValue, v => emit('update:modelValue', v))

watch(() => props.language, lang => {
  if (editorInstance && monacoInstance) {
    const model = editorInstance.getModel()
    if (model) monacoInstance.editor.setModelLanguage(model, lang)
  }
})

function registerCustomLanguage(monaco) {
  if (customLangRegistered) return
  try {
    const id = 'customLang'
    monaco.languages.register({ id })
    monaco.languages.setMonarchTokensProvider(id, customLang)
    customLangRegistered = true
  } catch (e) {
    // Sessizce yut (isteğe bağlı log)
    // console.warn('Custom language register failed', e)
  }
}

function handleMount(editor, monaco) {
  editorInstance = editor
  monacoInstance = monaco
  registerCustomLanguage(monaco)
  emit('editorReady', editor)
}
</script>

<style scoped>
</style>

