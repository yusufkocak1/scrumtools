<template>
  <div v-if="visible" class="fixed inset-0 z-[9999] flex items-center justify-center bg-black/50 p-4">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 text-center space-y-4">
      <div class="w-14 h-14 mx-auto rounded-full bg-indigo-100 flex items-center justify-center">
        <svg class="w-7 h-7 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
        </svg>
      </div>
      <h3 class="text-lg font-semibold text-gray-900">Paket Limitine Ulaştınız</h3>
      <p class="text-sm text-gray-600">{{ message }}</p>
      <div class="flex gap-2 justify-center pt-2">
        <button
          @click="close"
          class="px-4 py-2 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
        >Kapat</button>
        <button
          @click="goToBilling"
          class="px-4 py-2 text-sm bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >Paketleri İncele</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

/**
 * Global upgrade modalı.
 * axios interceptor'ı 402 + PLAN_LIMIT yanıtlarında 'scrumtools:plan-limit'
 * window event'i fırlatır; bu bileşen App.vue içinde bir kez mount edilir ve dinler.
 */
const router = useRouter()
const visible = ref(false)
const message = ref('')

function onPlanLimit(e) {
  message.value = e.detail?.message ||
    'Bu işlem mevcut paketinizin limitlerini aşıyor. Devam etmek için paketinizi yükseltin.'
  visible.value = true
}

function close() {
  visible.value = false
}

function goToBilling() {
  visible.value = false
  router.push('/organizations')
  // OrganizationDashboard Abonelik sekmesine geçmesi için sinyal
  window.dispatchEvent(new CustomEvent('scrumtools:open-billing-tab'))
}

onMounted(() => window.addEventListener('scrumtools:plan-limit', onPlanLimit))
onBeforeUnmount(() => window.removeEventListener('scrumtools:plan-limit', onPlanLimit))
</script>
