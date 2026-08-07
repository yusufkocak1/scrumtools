<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm px-4"
       @click.self="$emit('close')">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden">
      <div class="px-6 py-4 border-b border-slate-100 flex items-center gap-3">
        <h3 class="font-semibold text-slate-800">{{ macro?.id ? 'Makroyu Düzenle' : 'Yeni Makro' }}</h3>
        <span v-if="macro?.id && !macro.approved"
              class="px-2 py-0.5 rounded-md bg-amber-50 text-amber-700 text-[11px] font-medium">
          onaysız
        </span>
        <button @click="$emit('close')" class="ml-auto text-slate-400 hover:text-slate-700 transition">✕</button>
      </div>

      <div class="p-6 space-y-4 overflow-y-auto">
        <div class="grid sm:grid-cols-2 gap-3">
          <div>
            <label class="text-sm text-slate-600 block mb-1.5">Ad</label>
            <input v-model="form.name" maxlength="200"
                   class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-indigo-300 focus:ring-2 focus:ring-indigo-100 transition"/>
          </div>
          <div>
            <label class="text-sm text-slate-600 block mb-1.5">Tetikleyici</label>
            <select v-model="form.triggerType"
                    class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-indigo-300 transition">
              <option value="MANUAL">Manuel</option>
              <option value="ON_OPEN">Doküman açıldığında</option>
              <option value="ON_EDIT">Doküman değiştiğinde</option>
            </select>
          </div>
        </div>

        <div>
          <label class="text-sm text-slate-600 block mb-1.5">Açıklama</label>
          <input v-model="form.description" maxlength="1000"
                 placeholder="Bu makro ne yapıyor?"
                 class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-indigo-300 transition"/>
        </div>

        <p v-if="form.triggerType !== 'MANUAL'"
           class="text-xs text-amber-700 bg-amber-50 border border-amber-100 rounded-xl px-3 py-2">
          Otomatik tetikleyiciler yalnızca <strong>onaylı</strong> makrolarda çalışır — dokümanı açan
          herkesin yetkisiyle sessizce koştukları için onay zorunlu.
        </p>

        <div>
          <label class="text-sm text-slate-600 block mb-1.5">Kaynak</label>
          <div class="h-80 border border-slate-200 rounded-xl overflow-hidden">
            <MonacoEditor
                v-model="form.source"
                language="javascript"
                :options="{ minimap: { enabled: false }, fontSize: 13 }"
                @editorReady="onEditorReady"/>
          </div>
        </div>

        <p v-if="macro?.id && sourceChanged"
           class="text-xs text-amber-700">
          Kaynağı değiştirdiğiniz için mevcut onay düşecek ve makro yeniden onaylanana kadar
          yalnızca sizin tarafınızdan çalıştırılabilecek.
        </p>
      </div>

      <div class="px-6 py-4 border-t border-slate-100 flex items-center gap-2">
        <button @click="$emit('close')"
                class="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-xl transition">
          Vazgeç
        </button>
        <button @click="save" :disabled="!canSave || saving"
                class="ml-auto px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-xl hover:bg-indigo-500 disabled:opacity-50 transition">
          {{ saving ? 'Kaydediliyor…' : 'Kaydet' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import MonacoEditor from '../editors/MonacoEditor.vue'
import monaco from '../../../monaco/setup.js'
import { MACRO_TYPE_DEFS } from '../../../collab/macro/macroTypeDefs.js'

/**
 * Makro düzenleyici — Monaco + `ScrumTools` tip tanımları (plan Faz 4).
 *
 * Tip tanımları editöre "ekstra kütüphane" olarak veriliyor; bu, hem tamamlama
 * hem de yazım hatalarının anında görünmesi demek. Betik yine de derlenmiyor —
 * çalıştırma sırasında sandbox'ta değerlendirilecek düz JavaScript.
 */
const props = defineProps({
  projectId: { type: String, required: true },
  documentId: { type: String, default: null },
  macro: { type: Object, default: null }
})

const emit = defineEmits(['close', 'saved'])

const saving = ref(false)
const form = reactive({
  name: props.macro?.name || '',
  description: props.macro?.description || '',
  source: props.macro?.source || DEFAULT_SOURCE(),
  triggerType: props.macro?.triggerType || 'MANUAL'
})

const canSave = computed(() => form.name.trim().length > 0 && form.source.trim().length > 0)
const sourceChanged = computed(() => props.macro?.source && props.macro.source !== form.source)

function DEFAULT_SOURCE() {
  return `// ScrumTools makrosu — JavaScript (VBA değil)\n`
      + `const doc = ScrumTools.getActiveDocument()\n`
      + `const sheet = doc.getActiveSheet()\n\n`
      + `// sheet.getRange('A1:B3').setValues([[1, 2], [3, 4], [5, 6]])\n`
      + `// const tasks = await ScrumTools.tasks.query('status != Done')\n`
      + `\nScrumTools.ui.toast('Makro çalıştı')\n`
}

let typesRegistered = false

function onEditorReady() {
  // Tip tanımı yalnızca bir kez eklenir: her editör açılışında eklemek aynı
  // sanal dosyayı çakıştırır ve tamamlama sessizce bozulur.
  if (typesRegistered) return
  try {
    monaco.languages.typescript.javascriptDefaults.addExtraLib(
      MACRO_TYPE_DEFS, 'ts:scrumtools-macro.d.ts')
    typesRegistered = true
  } catch {
    // Tamamlama olmadan da editör kullanılabilir kalır.
  }
}

async function save() {
  if (!canSave.value || saving.value) return
  saving.value = true
  try {
    const CollabMacroApi = (await import('../../../api/CollabMacroApi.js')).default
    const payload = {
      name: form.name.trim(),
      description: form.description,
      source: form.source,
      documentId: props.documentId,
      triggerType: form.triggerType,
      enabled: true
    }
    const { data } = props.macro?.id
      ? await CollabMacroApi.update(props.projectId, props.macro.id, payload)
      : await CollabMacroApi.create(props.projectId, payload)
    emit('saved', data)
  } catch {
    // 402/403 mesajlarını axios interceptor gösteriyor
  } finally {
    saving.value = false
  }
}
</script>
