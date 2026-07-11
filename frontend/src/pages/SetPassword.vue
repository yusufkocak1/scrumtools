<template>
  <div class="min-h-screen w-full bg-gradient-to-br from-gray-50 to-gray-100 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">Scrum Tools</h1>
        <p class="text-gray-600">Agile team management platform</p>
      </div>

      <div class="bg-white rounded-2xl shadow-xl border border-gray-200 p-8">
        <!-- Token doğrulanıyor -->
        <div v-if="loading" class="text-center py-8">
          <p class="text-sm text-gray-500">Bağlantı doğrulanıyor...</p>
        </div>

        <!-- Geçersiz / süresi dolmuş token -->
        <div v-else-if="!tokenInfo?.valid" class="text-center py-6 space-y-4">
          <div class="w-12 h-12 mx-auto rounded-full bg-red-100 flex items-center justify-center">
            <svg class="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </div>
          <h2 class="text-lg font-semibold text-gray-900">Bağlantı geçersiz</h2>
          <p class="text-sm text-gray-600">
            Bu bağlantı geçersiz veya süresi dolmuş. Yeni bir bağlantı talep edebilirsiniz.
          </p>
          <router-link
            to="/forgot-password"
            class="inline-block text-sm text-blue-600 hover:text-blue-700 font-medium">
            Yeni bağlantı talep et
          </router-link>
        </div>

        <!-- Şifre belirleme formu -->
        <form v-else class="space-y-5" @submit.prevent="submit">
          <div class="text-center mb-2">
            <h2 class="text-lg font-semibold text-gray-900">
              {{ tokenInfo.purpose === 'ACCOUNT_SETUP' ? 'Hesabınızı Etkinleştirin' : 'Yeni Şifre Belirleyin' }}
            </h2>
            <p class="text-sm text-gray-600 mt-1">
              Merhaba <strong>{{ tokenInfo.name }}</strong> ({{ tokenInfo.email }})
            </p>
          </div>

          <div class="space-y-2">
            <label class="block text-sm font-medium text-gray-700">Şifre</label>
            <input
              v-model="password"
              type="password"
              required
              minlength="6"
              class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="En az 6 karakter">
          </div>

          <div class="space-y-2">
            <label class="block text-sm font-medium text-gray-700">Şifre (tekrar)</label>
            <input
              v-model="passwordConfirm"
              type="password"
              required
              minlength="6"
              class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Şifrenizi tekrar girin">
            <p v-if="passwordConfirm && password !== passwordConfirm" class="text-xs text-red-600">
              Şifreler eşleşmiyor.
            </p>
          </div>

          <button
            type="submit"
            :disabled="submitting || password.length < 6 || password !== passwordConfirm"
            class="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-4 rounded-xl transition-all duration-200 disabled:opacity-50">
            {{ submitting ? 'Kaydediliyor...' : 'Şifremi Belirle ve Giriş Yap' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createToast } from 'mosha-vue-toastify'
import { validatePasswordToken, setPasswordWithToken } from '../api/AuthApi.js'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const tokenInfo = ref(null)
const password = ref('')
const passwordConfirm = ref('')

const token = route.query.token || ''

onMounted(async () => {
  try {
    tokenInfo.value = await validatePasswordToken(token)
  } catch {
    tokenInfo.value = { valid: false }
  } finally {
    loading.value = false
  }
})

async function submit() {
  if (password.value !== passwordConfirm.value) return
  submitting.value = true
  try {
    await setPasswordWithToken(token, password.value)
    createToast('Şifreniz belirlendi, hoş geldiniz!', { type: 'success', position: 'top-center' })
    router.push('/organizations')
  } catch {
    // Hata toast'ı axios interceptor tarafından gösterilir
  } finally {
    submitting.value = false
  }
}
</script>
