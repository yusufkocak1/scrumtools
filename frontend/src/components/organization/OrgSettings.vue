<template>
  <div class="space-y-5">
    <div>
      <h2 class="text-lg font-semibold text-gray-900">Ayarlar</h2>
      <p class="text-sm text-gray-500 mt-0.5">Organizasyon profili ve görünen bilgileri</p>
    </div>

    <form @submit.prevent="save" class="space-y-5 max-w-xl">
      <!-- Logo — dosya seçilir seçilmez yüklenir, Kaydet'i beklemez -->
      <div>
        <label class="label">Logo</label>
        <div class="flex items-center gap-4">
          <button
            type="button"
            @click="pickFile"
            @dragover.prevent
            @drop.prevent="onDrop"
            :disabled="logoBusy"
            class="w-16 h-16 rounded-xl bg-indigo-100 flex items-center justify-center text-2xl font-bold text-indigo-600 overflow-hidden flex-shrink-0 border-2 border-dashed border-indigo-200 hover:border-indigo-400 transition-colors disabled:opacity-60"
            :title="logoUrl ? 'Logoyu değiştir' : 'Logo yükle'"
          >
            <div v-if="logoBusy" class="w-5 h-5 border-2 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div>
            <img v-else-if="logoUrl" :src="logoUrl" alt="" class="w-full h-full object-cover" />
            <span v-else>{{ form.name?.charAt(0)?.toUpperCase() || '?' }}</span>
          </button>

          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <button type="button" @click="pickFile" :disabled="logoBusy" class="btn-secondary">
                {{ logoUrl ? 'Değiştir' : 'Dosya Seç' }}
              </button>
              <button
                v-if="logoUrl"
                type="button"
                @click="removeLogo"
                :disabled="logoBusy"
                class="btn-danger-ghost"
              >
                Kaldır
              </button>
            </div>
            <p class="text-xs text-gray-500 mt-1.5">PNG, JPEG, WEBP veya GIF · en fazla 2MB</p>
          </div>
        </div>

        <input
          ref="fileInput"
          type="file"
          :accept="ACCEPTED_TYPES.join(',')"
          class="hidden"
          @change="onFileChange"
        />
      </div>

      <div>
        <label class="label">Organizasyon Adı</label>
        <input v-model="form.name" type="text" required class="input-field" />
      </div>

      <div>
        <label class="label">Açıklama</label>
        <textarea v-model="form.description" rows="3" class="input-field"></textarea>
      </div>

      <div>
        <label class="label">Slug (URL)</label>
        <input :value="org?.slug" disabled class="input-field opacity-60 cursor-not-allowed font-mono" />
        <p class="text-xs text-gray-500 mt-1">Slug değiştirilemez.</p>
      </div>

      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

      <div class="flex justify-end pt-1">
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? 'Kaydediliyor...' : 'Kaydet' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
/**
 * Organizasyon profili. Paket/kullanım göstergeleri Abonelik bölümünde,
 * organizasyon geçişi ise sidebar'daki switcher'da — burada tekrarlanmaz.
 */
import { computed, ref, watch } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import OrganizationApi from '../../api/OrganizationApi.js'

const props = defineProps({
  org: { type: Object, required: true }
})

const emit = defineEmits(['updated'])

// Sunucu tarafıyla aynı liste: SVG kabul edilmez (aynı origin'den servis edildiği
// için script içeren SVG oturum verisine erişebilirdi).
const ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']
const MAX_LOGO_SIZE = 2 * 1024 * 1024

const saving = ref(false)
const error = ref('')
const form = ref({ name: '', description: '' })

const fileInput = ref(null)
const logoBusy = ref(false)

/** Logo yüklemeyle anında güncellenir; form alanlarının kaydedilmesini beklemez. */
const logoUrl = computed(() => props.org?.logoUrl || '')

// Yalnızca organizasyon değiştiğinde formu sıfırla — logo yüklendiğinde gelen
// org güncellemesi kullanıcının henüz kaydetmediği ad/açıklamasını silmemeli.
watch(() => props.org?.id, () => {
  form.value = {
    name: props.org?.name || '',
    description: props.org?.description || '',
  }
}, { immediate: true })

function pickFile() {
  fileInput.value?.click()
}

function onDrop(event) {
  const file = event.dataTransfer?.files?.[0]
  if (file) uploadLogo(file)
}

function onFileChange(event) {
  const file = event.target.files?.[0]
  // Aynı dosya tekrar seçilebilsin diye input sıfırlanır.
  event.target.value = ''
  if (file) uploadLogo(file)
}

async function uploadLogo(file) {
  if (!ACCEPTED_TYPES.includes(file.type)) {
    createToast('Logo yalnızca PNG, JPEG, WEBP veya GIF olabilir.', { type: 'danger', position: 'top-center' })
    return
  }
  if (file.size > MAX_LOGO_SIZE) {
    createToast('Logo boyutu 2MB\'ı aşamaz.', { type: 'danger', position: 'top-center' })
    return
  }

  logoBusy.value = true
  try {
    const res = await OrganizationApi.uploadLogo(props.org.id, file)
    emit('updated', res.data)
    createToast('Logo güncellendi.', { type: 'success', position: 'top-center' })
  } catch {
    // Hata mesajı axios interceptor'ı tarafından gösterilir.
  } finally {
    logoBusy.value = false
  }
}

async function removeLogo() {
  logoBusy.value = true
  try {
    const res = await OrganizationApi.deleteLogo(props.org.id)
    emit('updated', res.data)
    createToast('Logo kaldırıldı.', { type: 'success', position: 'top-center' })
  } catch {
    // Hata mesajı axios interceptor'ı tarafından gösterilir.
  } finally {
    logoBusy.value = false
  }
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    // Slug sunucuda değiştirilmez ama istek doğrulaması zorunlu kılar.
    const res = await OrganizationApi.update(props.org.id, { ...form.value, slug: props.org.slug })
    emit('updated', res.data)
    createToast('Organizasyon güncellendi.', { type: 'success', position: 'top-center' })
  } catch (e) {
    error.value = e?.response?.data?.message || 'Kaydedilemedi.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500; }
.label { @apply block text-sm font-medium text-gray-700 mb-1; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium; }
.btn-secondary { @apply px-3 py-1.5 border border-gray-300 bg-white text-gray-700 text-sm rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors font-medium; }
.btn-danger-ghost { @apply px-3 py-1.5 text-red-600 text-sm rounded-lg hover:bg-red-50 disabled:opacity-50 transition-colors font-medium; }
</style>
