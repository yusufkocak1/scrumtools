<template>
  <div class="min-h-screen w-full bg-gradient-to-br from-gray-50 to-gray-100 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">Scrum Tools</h1>
        <p class="text-gray-600">Agile team management platform</p>
      </div>

      <div class="bg-white rounded-2xl shadow-xl border border-gray-200 p-8">
        <!-- Gönderildi -->
        <div v-if="sent" class="text-center py-6 space-y-4">
          <div class="w-12 h-12 mx-auto rounded-full bg-green-100 flex items-center justify-center">
            <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
            </svg>
          </div>
          <h2 class="text-lg font-semibold text-gray-900">E-postanızı kontrol edin</h2>
          <p class="text-sm text-gray-600">
            Bu e-posta adresi kayıtlıysa, şifre sıfırlama bağlantısı gönderildi.
            Bağlantı 1 saat geçerlidir.
          </p>
          <router-link to="/login" class="inline-block text-sm text-blue-600 hover:text-blue-700 font-medium">
            Giriş sayfasına dön
          </router-link>
        </div>

        <!-- Form -->
        <form v-else class="space-y-5" @submit.prevent="submit">
          <div class="text-center mb-2">
            <h2 class="text-lg font-semibold text-gray-900">Şifremi Unuttum</h2>
            <p class="text-sm text-gray-600 mt-1">
              Kayıtlı e-posta adresinizi girin, size şifre sıfırlama bağlantısı gönderelim.
            </p>
          </div>

          <div class="space-y-2">
            <label class="block text-sm font-medium text-gray-700">E-posta</label>
            <input
              v-model="email"
              type="email"
              required
              class="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="ornek@sirket.com">
          </div>

          <button
            type="submit"
            :disabled="submitting"
            class="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-4 rounded-xl transition-all duration-200 disabled:opacity-50">
            {{ submitting ? 'Gönderiliyor...' : 'Sıfırlama Bağlantısı Gönder' }}
          </button>

          <p class="text-center">
            <router-link to="/login" class="text-sm text-gray-600 hover:text-gray-900">
              Giriş sayfasına dön
            </router-link>
          </p>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { forgotPassword } from '../api/AuthApi.js'

const email = ref('')
const sent = ref(false)
const submitting = ref(false)

async function submit() {
  submitting.value = true
  try {
    await forgotPassword(email.value)
    sent.value = true
  } catch {
    // Hata toast'ı axios interceptor tarafından gösterilir
  } finally {
    submitting.value = false
  }
}
</script>
