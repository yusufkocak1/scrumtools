<template>
  <div class="h-full">
    <MonacoEditor
        :language="language"
        :theme="theme"
        :options="editorOptions"
        @editorReady="onEditorReady"/>
  </div>
</template>

<script setup>
import { onBeforeUnmount, computed, watch } from 'vue'
import { MonacoBinding } from 'y-monaco'
import MonacoEditor from './MonacoEditor.vue'

/**
 * Eş zamanlı kod editörü — Monaco + y-monaco (COLLAB_WORKSPACE_PLAN.md K1).
 *
 * `MonacoBinding` model ile `Y.Text`'i iki yönlü bağlar; uzaktan gelen imleçler
 * awareness üzerinden dekorasyon olarak çizilir. Bu yüzden bileşene `modelValue`
 * verilmiyor: içeriğin sahibi CRDT'dir, prop'tan yazmak binding ile yarışır ve
 * karakter kaybına yol açar.
 */
const props = defineProps({
  ydoc: { type: Object, required: true },
  awareness: { type: Object, required: true },
  language: { type: String, default: 'javascript' },
  theme: { type: String, default: 'vs-light' },
  readOnly: { type: Boolean, default: false }
})

const emit = defineEmits(['snapshotText'])

let binding = null
let editorInstance = null
let sharedText = null

const editorOptions = computed(() => ({
  readOnly: props.readOnly,
  // Salt-okunur modda imleç yine görünsün ki kullanıcı nereye baktığını bilsin
  domReadOnly: props.readOnly,
  automaticLayout: true
}))

function onEditorReady(editor) {
  editorInstance = editor
  const model = editor.getModel()
  if (!model) return

  // 'monaco' adlı paylaşımlı metin: Faz 3'te aynı Y.Doc içinde başka tipler de
  // yaşayacağı için alan adı sabitlenmiş durumda.
  sharedText = props.ydoc.getText('monaco')
  binding = new MonacoBinding(sharedText, model, new Set([editor]), props.awareness)

  // Anlık görüntü metnini CRDT'den al, editörden değil: yazar istemci arka
  // planda bir sekmedeyse Monaco'nun modeli tazelenmemiş olabilir.
  emit('snapshotText', () => sharedText.toString())
}

/**
 * İçeriği tümüyle değiştirir — geçmişten geri yükleme için (plan §6 /history).
 *
 * Tek transaction: iki ayrı işlem olarak yapılsaydı diğer istemciler bir an
 * için boş doküman görür ve o arada yazan biri silinmiş metnin üstüne yazardı.
 */
function replaceContent(text) {
  if (!sharedText) return
  props.ydoc.transact(() => {
    sharedText.delete(0, sharedText.length)
    sharedText.insert(0, text ?? '')
  })
}

defineExpose({ replaceContent })

watch(() => props.readOnly, (value) => {
  editorInstance?.updateOptions({ readOnly: value, domReadOnly: value })
})

onBeforeUnmount(() => {
  binding?.destroy()
  binding = null
})
</script>
