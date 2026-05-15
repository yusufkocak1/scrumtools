<template>
  <!-- Bildirim Çanı Butonu -->
  <div class="relative" ref="bellRef">
    <button
      @click="togglePanel"
      class="relative flex items-center justify-center w-10 h-10 rounded-xl transition-all duration-200 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-300"
      :class="{ 'bg-blue-50': panelOpen }"
      title="Bildirimler"
      type="button"
    >
      <!-- Zil İkonu -->
      <svg class="w-6 h-6 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
      </svg>

      <!-- Okunmamış badge -->
      <span
        v-if="unreadCount > 0"
        class="absolute -top-1 -right-1 min-w-[18px] h-[18px] bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center px-1 leading-none"
      >
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </button>

    <!-- Panel -->
    <NotificationPanel
      v-if="panelOpen"
      :notifications="notifications"
      :loading="loading"
      :hasMore="hasMore"
      @close="panelOpen = false"
      @mark-read="markRead"
      @mark-all-read="markAllRead"
      @load-more="loadNotifications(false)"
    />
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { useNotifications } from '../composables/useNotifications.js'
import NotificationPanel from './NotificationPanel.vue'

export default {
  name: 'NotificationBell',
  components: { NotificationPanel },
  setup() {
    const panelOpen = ref(false)
    const bellRef = ref(null)

    const {
      notifications,
      unreadCount,
      loading,
      hasMore,
      loadNotifications,
      markRead,
      markAllRead
    } = useNotifications()

    function togglePanel() {
      panelOpen.value = !panelOpen.value
      if (panelOpen.value && notifications.value.length === 0) {
        loadNotifications(true)
      }
    }

    // Dışarı tıklayınca kapat
    function onClickOutside(e) {
      if (bellRef.value && !bellRef.value.contains(e.target)) {
        panelOpen.value = false
      }
    }

    onMounted(() => document.addEventListener('mousedown', onClickOutside))
    onUnmounted(() => document.removeEventListener('mousedown', onClickOutside))

    return {
      bellRef, panelOpen, notifications, unreadCount, loading, hasMore,
      togglePanel, loadNotifications, markRead, markAllRead
    }
  }
}
</script>

