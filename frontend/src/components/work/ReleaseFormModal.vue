<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="$emit('close')">
    <div class="bg-white rounded-xl shadow-2xl w-full max-w-md mx-4 p-6">
      <h2 class="text-lg font-semibold text-gray-900 mb-4">
        {{ release ? 'Sürümü Düzenle' : 'Yeni Sürüm Oluştur' }}
      </h2>

      <div class="space-y-3 mb-5">
        <!-- Ad -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Sürüm Adı *</label>
          <input
            v-model="form.name"
            placeholder="ör: v2.4.0"
            class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
          />
        </div>

        <!-- Açıklama -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Açıklama</label>
          <textarea
            v-model="form.description"
            rows="3"
            placeholder="Sürüm kapsamı, notlar..."
            class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
          ></textarea>
        </div>

        <!-- Release Manager -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Release Manager</label>
          <AutoCompleteInput
            v-model="form.releaseManagerEmail"
            :options="teamMembers"
            placeholder="Yönetici ara ve seç..."
            :displayField="'displayName'"
            :valueField="'email'"
          />
          <p class="text-xs text-gray-400 mt-1">Boş bırakılırsa siz atanırsınız. Durum geçişlerini sadece yönetici ve org admin yapabilir.</p>
        </div>

        <!-- Tarihler -->
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Paket Kapanış Tarihi</label>
            <input
              v-model="form.freezeDate"
              type="date"
              class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Planlanan Yayın Tarihi</label>
            <input
              v-model="form.plannedReleaseDate"
              type="date"
              class="w-full text-sm rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>
        </div>
      </div>

      <div class="flex justify-end gap-3">
        <button @click="$emit('close')" class="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">
          İptal
        </button>
        <button
          @click="submit"
          :disabled="!form.name.trim() || submitting"
          class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          {{ release ? 'Kaydet' : 'Oluştur' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AutoCompleteInput from './AutoCompleteInput.vue'
import { createRelease, updateRelease } from '../../api/ReleaseApi.js'

const props = defineProps({
  projectId: { type: String, required: true },
  /** Düzenleme modunda mevcut release; null ise oluşturma modu */
  release: { type: Object, default: null },
  /** [{ email, displayName, ... }] — manager seçimi için */
  teamMembers: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'saved'])

const form = ref({
  name: props.release?.name || '',
  description: props.release?.description || '',
  releaseManagerEmail: props.release?.releaseManagerEmail || '',
  freezeDate: props.release?.freezeDate || '',
  plannedReleaseDate: props.release?.plannedReleaseDate || ''
})
const submitting = ref(false)

async function submit() {
  if (!form.value.name.trim() || submitting.value) return
  submitting.value = true
  try {
    const payload = { ...form.value, name: form.value.name.trim() }
    const saved = props.release
      ? await updateRelease(props.projectId, props.release.id, payload)
      : await createRelease(props.projectId, payload)
    emit('saved', saved)
    emit('close')
  } catch (e) {
    console.error('Sürüm kaydedilirken hata:', e)
  } finally {
    submitting.value = false
  }
}
</script>
