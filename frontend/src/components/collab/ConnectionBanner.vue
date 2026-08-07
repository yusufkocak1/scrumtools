<template>
  <div v-if="message"
       :class="['px-4 py-2 text-sm flex items-center gap-2 border-b', tone.wrapper]">
    <span v-if="status === 'connecting' || status === 'offline'"
          :class="['w-3 h-3 rounded-full border-2 border-t-transparent animate-spin', tone.spinner]"></span>
    <svg v-else class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round"
            d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"/>
    </svg>
    <span>{{ message }}</span>
    <button v-if="status === 'offline'" @click="$emit('retry')"
            class="ml-auto text-xs font-medium underline underline-offset-2 hover:no-underline">
      Şimdi dene
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

/**
 * Bağlantı durumu şeridi.
 *
 * Kopukken kullanıcının yazmaya devam edebildiğini söylemek önemli: CRDT
 * sayesinde çevrimdışı düzenleme kaybolmuyor, geri bağlanınca birleşiyor
 * (plan K1/R7). Bunu söylemezsek kullanıcı yazmayı bırakır.
 */
const props = defineProps({
  status: { type: String, default: 'connecting' }
})
defineEmits(['retry'])

const message = computed(() => ({
  connecting: 'Bağlanılıyor…',
  offline: 'Bağlantı koptu — yazmaya devam edebilirsiniz, bağlantı gelince değişiklikleriniz birleştirilecek.',
  forbidden: 'Bu dokümana erişim yetkiniz yok.',
  synced: ''
}[props.status] || ''))

const tone = computed(() => {
  if (props.status === 'forbidden') {
    return { wrapper: 'bg-rose-50 border-rose-200 text-rose-700', spinner: 'border-rose-500' }
  }
  if (props.status === 'offline') {
    return { wrapper: 'bg-amber-50 border-amber-200 text-amber-800', spinner: 'border-amber-500' }
  }
  return { wrapper: 'bg-slate-50 border-slate-200 text-slate-600', spinner: 'border-slate-400' }
})
</script>
