<template>
  <div class="relative">
    <button
      @click="isOpen = !isOpen"
      class="group flex items-center gap-2 px-3 py-2 rounded-xl border border-gray-200 bg-white hover:bg-indigo-50 hover:border-indigo-300 text-sm transition-all shadow-sm"
    >
      <div v-if="currentOrg?.logoUrl" class="w-6 h-6 rounded overflow-hidden flex-shrink-0">
        <img :src="currentOrg.logoUrl" :alt="currentOrg.name" class="w-full h-full object-cover" />
      </div>
      <div v-else class="w-6 h-6 rounded-md bg-indigo-600 flex items-center justify-center text-xs font-bold text-white flex-shrink-0">
        {{ currentOrg?.name?.charAt(0)?.toUpperCase() || '?' }}
      </div>
      <span class="max-w-[130px] truncate font-medium text-gray-700 group-hover:text-indigo-700">
        {{ currentOrg?.name || 'Organizasyon Seç' }}
      </span>
      <svg class="w-4 h-4 text-gray-400 group-hover:text-indigo-500 transition-transform flex-shrink-0" :class="{ 'rotate-180': isOpen }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <transition name="dropdown">
      <div
        v-if="isOpen"
        class="absolute top-full right-0 mt-1.5 w-64 bg-white rounded-xl shadow-lg border border-gray-200 z-50 overflow-hidden"
      >
        <div class="p-2">
          <p class="text-xs text-gray-400 px-2 py-1.5 uppercase tracking-wider font-semibold">Organizasyonlar</p>

          <div v-if="loading" class="py-6 text-center text-gray-400 text-sm">
            <div class="w-5 h-5 border-2 border-indigo-200 border-t-indigo-600 rounded-full animate-spin mx-auto mb-2"></div>
            Yükleniyor...
          </div>

          <button
            v-for="org in organizations"
            :key="org.id"
            @click="selectOrg(org)"
            class="w-full flex items-center gap-3 px-2 py-2.5 rounded-lg hover:bg-gray-50 transition-colors text-left"
            :class="currentOrg?.id === org.id ? 'bg-indigo-50' : ''"
          >
            <div class="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white text-sm font-bold flex-shrink-0">
              {{ org.name.charAt(0).toUpperCase() }}
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-gray-900 truncate">{{ org.name }}</p>
              <p class="text-xs text-gray-400">{{ org.memberCount }} üye</p>
            </div>
            <svg v-if="currentOrg?.id === org.id" class="w-4 h-4 text-indigo-600 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
            </svg>
          </button>

          <div v-if="!loading && organizations.length === 0" class="py-4 text-center text-gray-400 text-sm">
            Henüz organizasyon yok
          </div>

          <div class="border-t border-gray-100 mt-2 pt-2">
            <button
              @click="$emit('create-org'); isOpen = false"
              class="w-full flex items-center gap-2 px-2 py-2.5 rounded-lg text-indigo-600 hover:bg-indigo-50 text-sm transition-colors font-medium"
            >
              <div class="w-7 h-7 rounded-md bg-indigo-100 flex items-center justify-center">
                <svg class="w-3.5 h-3.5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
              </div>
              Yeni Organizasyon
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useOrganizationContext } from '../../composables/useOrganizationContext.js'

defineEmits(['create-org'])

// Aktif organizasyon paylaşılan context'ten okunur; buradaki seçim ayarlar
// ekranıyla ve başlıkla aynı state üzerinden yürür.
const {
  organizations,
  activeOrg: currentOrg,
  loading,
  loadOrganizations,
  selectOrg: setActiveOrg,
} = useOrganizationContext()

const isOpen = ref(false)

function selectOrg(org) {
  setActiveOrg(org)
  isOpen.value = false
}

function handleClickOutside(e) {
  if (!e.target.closest('.relative')) {
    isOpen.value = false
  }
}

onMounted(() => {
  loadOrganizations()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.15s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
