<template>
  <TaskPanel panel-key="description" title="Açıklama" data-panel-root="description">
    <template #icon>
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7"/>
      </svg>
    </template>

    <template #actions>
      <span v-if="saving" class="flex items-center gap-1 text-[11px] text-blue-500">
        <svg class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
        </svg>
        Kaydediliyor
      </span>
      <span v-else-if="savedFlash" class="flex items-center gap-1 text-[11px] text-green-600">
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
        </svg>
        Kaydedildi
      </span>

      <template v-if="editing">
        <button type="button" class="btn-xs-primary" @click="save">Kaydet</button>
        <button type="button" class="btn-xs-ghost" @click="cancel">Vazgeç</button>
      </template>
      <button v-else type="button" class="btn-xs-ghost" @click="startEdit">Düzenle</button>
    </template>

    <div v-if="editing" class="rounded-lg border border-blue-200 overflow-hidden">
      <TiptapEditor v-model="draft" :uploadHandler="uploadHandler" placeholder="Açıklama yazın..." />
    </div>

    <div v-else @click="startEdit" class="cursor-text rounded-lg -mx-1 px-1 hover:bg-gray-50/70 transition-colors">
      <RichContentViewer v-if="task.description" :content="task.description" />
      <p v-else class="py-4 text-sm text-gray-400">
        Açıklama eklemek için tıklayın…
      </p>
    </div>
  </TaskPanel>
</template>

<script setup>
/**
 * Açıklama paneli — satır içi düzenleme.
 *
 * Kaydı panel kendisi yapar (kaydediliyor/kaydedildi geri bildirimi için isteğin
 * sonucunu beklemesi gerekir) ve sonucu `patch` ile sayfaya bildirir.
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'
import TaskPanel from '../TaskPanel.vue'
import TiptapEditor from '../../docs/TiptapEditor.vue'
import RichContentViewer from '../RichContentViewer.vue'
import { updateTask, uploadAttachment } from '../../../api/WorkApi.js'
import { taskPanelProps, taskPanelEmits } from './panelProps.js'

const props = defineProps(taskPanelProps)
const emit = defineEmits(taskPanelEmits)

const editing = ref(false)
const draft = ref('')
const saving = ref(false)
const savedFlash = ref(false)

onMounted(() => document.addEventListener('mousedown', onDocumentMouseDown))
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocumentMouseDown))

function startEdit() {
  if (editing.value) return
  editing.value = true
  draft.value = props.task.description || ''
}

function cancel() {
  editing.value = false
  draft.value = ''
}

async function save() {
  if (!editing.value) return
  const value = draft.value.trim()
  editing.value = false

  if (value === (props.task.description || '')) return
  if (!props.teamId || !props.task?.id) return

  saving.value = true
  try {
    await updateTask(props.teamId, props.task.id, {
      description: value,
      updatedAt: new Date().toISOString(),
    })
    emit('patch', { description: value })
    savedFlash.value = true
    setTimeout(() => { savedFlash.value = false }, 1500)
  } catch (e) {
    console.error('Açıklama kaydedilemedi:', e)
  } finally {
    saving.value = false
  }
}

/** Panelin dışına tıklamak düzenlemeyi kaydeder — başlıktaki butonlar panele dahildir. */
function onDocumentMouseDown(event) {
  if (!editing.value) return
  if (event.target.closest?.('[data-panel-root="description"]')) return
  // Editörün modalları (resim, link, markdown içe aktar) body'ye teleport edildiği için
  // DOM'da panelin dışında kalır; içlerine tıklamak düzenlemeyi kapatmamalı.
  if (event.target.closest?.('[data-editor-overlay]')) return
  save()
}

async function uploadHandler(file) {
  if (!props.teamId || !props.task?.id) return null
  const result = await uploadAttachment(props.teamId, props.task.id, file)
  return { downloadUrl: result.downloadUrl, fileName: result.fileName || file.name }
}
</script>

<style scoped>
.btn-xs-primary {
  @apply px-2.5 py-1 rounded-md text-[11px] font-semibold text-white bg-blue-600 hover:bg-blue-700 transition-colors;
}
.btn-xs-ghost {
  @apply px-2.5 py-1 rounded-md text-[11px] font-semibold text-gray-500 bg-gray-100 hover:bg-gray-200 hover:text-gray-700 transition-colors;
}
</style>
