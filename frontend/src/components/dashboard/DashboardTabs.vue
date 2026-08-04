<template>
  <div class="flex items-center gap-1 overflow-x-auto">
    <div
      v-for="dashboard in dashboards"
      :key="dashboard.id"
      class="relative shrink-0"
    >
      <button
        class="group flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm transition-colors border"
        :class="dashboard.id === activeId
          ? 'border-purple-300 bg-purple-50 text-purple-800'
          : 'border-transparent text-gray-600 hover:bg-gray-100'"
        @click="$emit('select', dashboard.id)"
      >
        <!-- Paylaşım durumu: kilitli (özel) / takım -->
        <svg v-if="dashboard.visibility === 'TEAM'" class="w-3.5 h-3.5 text-gray-400 shrink-0"
             fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
        </svg>
        <svg v-else class="w-3.5 h-3.5 text-gray-300 shrink-0"
             fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
        </svg>

        <span class="max-w-[12rem] truncate">{{ dashboard.name }}</span>

        <!-- Başkasının panosunda sahibini yaz: aynı adda iki pano olabilir -->
        <span v-if="!dashboard.owned" class="text-[10px] text-gray-400 shrink-0">
          {{ dashboard.ownerName }}
        </span>
      </button>

      <!-- Aktif sekmenin işlemleri -->
      <button
        v-if="dashboard.id === activeId"
        class="absolute -right-1 top-1/2 -translate-y-1/2 p-1 rounded text-gray-400 hover:text-gray-700"
        title="Pano işlemleri"
        @click.stop="toggleMenu(dashboard.id)"
      >
        <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
          <path d="M6 10a2 2 0 11-4 0 2 2 0 014 0zM12 10a2 2 0 11-4 0 2 2 0 014 0zM18 10a2 2 0 11-4 0 2 2 0 014 0z"/>
        </svg>
      </button>

      <!-- İşlem menüsü -->
      <div
        v-if="openMenuId === dashboard.id"
        class="absolute right-0 top-full mt-1 z-30 w-48 rounded-xl border border-gray-200 bg-white shadow-lg py-1"
      >
        <button
          v-if="dashboard.canEdit"
          class="w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
          @click="pick('rename', dashboard)"
        >
          Yeniden adlandır
        </button>
        <button
          class="w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
          @click="pick('duplicate', dashboard)"
        >
          Kopyala
        </button>
        <button
          v-if="dashboard.canEdit"
          class="w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
          @click="pick('toggle-visibility', dashboard)"
        >
          {{ dashboard.visibility === 'TEAM' ? 'Paylaşımı kaldır' : 'Takımla paylaş' }}
        </button>
        <button
          v-if="dashboard.canEdit"
          class="w-full text-left px-3 py-2 text-sm text-red-600 hover:bg-red-50"
          @click="pick('delete', dashboard)"
        >
          Sil
        </button>
        <p v-if="!dashboard.canEdit" class="px-3 py-2 text-[11px] text-gray-400 leading-snug">
          Bu pano {{ dashboard.ownerName }} tarafından paylaşıldı; değiştirmek için kopyalayın.
        </p>
      </div>
    </div>

    <!-- Yeni pano -->
    <button
      class="shrink-0 flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-sm text-gray-500 hover:text-purple-700 hover:bg-purple-50 transition-colors"
      title="Yeni pano"
      @click="$emit('create')"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
      </svg>
      Yeni pano
    </button>
  </div>
</template>

<script setup>
/**
 * Pano sekmeleri.
 *
 * İşlem menüsü yalnız AKTİF sekmede açılır: her sekmede üç nokta göstermek,
 * beş panosu olan kullanıcıda şeridi okunmaz hâle getiriyordu. Menüdeki
 * seçenekler sunucunun döndüğü `canEdit` bayrağına göre süzülür — rol yorumu
 * arayüzde tekrar edilmez.
 */
import { ref, onMounted, onUnmounted } from 'vue'

defineProps({
  dashboards: { type: Array, default: () => [] },
  activeId: { type: String, default: '' },
})

const emit = defineEmits(['select', 'create', 'rename', 'duplicate', 'delete', 'toggle-visibility'])

const openMenuId = ref(null)

function toggleMenu(id) {
  openMenuId.value = openMenuId.value === id ? null : id
}

function pick(action, dashboard) {
  openMenuId.value = null
  emit(action, dashboard)
}

/** Dışarı tıklamada menü kapanır — sayfanın geri kalanı tıklanabilir kalsın. */
function onDocumentClick() {
  openMenuId.value = null
}

onMounted(() => document.addEventListener('click', onDocumentClick))
onUnmounted(() => document.removeEventListener('click', onDocumentClick))
</script>
