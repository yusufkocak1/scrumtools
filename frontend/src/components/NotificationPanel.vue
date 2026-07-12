<template>
  <!-- Bildirim Paneli — Sağa açılan dropdown -->
  <div
    class="fixed left-3 right-3 top-16 sm:absolute sm:left-auto sm:right-0 sm:top-full sm:mt-2 sm:w-96 bg-white rounded-2xl shadow-2xl border border-gray-200 z-[9999] flex flex-col overflow-hidden"
    style="max-height: 520px;"
  >
    <!-- Başlık -->
    <div class="flex items-center justify-between px-5 py-3 border-b border-gray-100 bg-gray-50">
      <span class="text-sm font-semibold text-gray-900">Bildirimler</span>
      <div class="flex items-center gap-2">
        <button
          v-if="notifications.some(n => !n.isRead)"
          @click="$emit('mark-all-read')"
          class="text-xs text-blue-600 hover:text-blue-800 font-medium transition-colors"
        >
          Tümünü okundu işaretle
        </button>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1 rounded-lg hover:bg-gray-100 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- Liste -->
    <div class="overflow-y-auto flex-1">
      <!-- Yükleniyor -->
      <div v-if="loading && notifications.length === 0" class="flex items-center justify-center py-10">
        <svg class="animate-spin w-6 h-6 text-blue-500" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
        </svg>
      </div>

      <!-- Boş durum -->
      <div v-else-if="notifications.length === 0" class="flex flex-col items-center justify-center py-12 text-gray-400">
        <svg class="w-12 h-12 mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
        </svg>
        <p class="text-sm font-medium">Bildirim yok</p>
        <p class="text-xs mt-1">Yeni aktiviteler burada görünecek</p>
      </div>

      <!-- Bildirim listesi -->
      <ul v-else class="divide-y divide-gray-50">
        <li
          v-for="n in notifications"
          :key="n.id"
          @click="handleClick(n)"
          class="flex items-start gap-3 px-4 py-3 cursor-pointer transition-colors hover:bg-gray-50"
          :class="{ 'bg-blue-50/60': !n.isRead }"
        >
          <!-- İkon -->
          <div class="flex-shrink-0 w-9 h-9 rounded-full flex items-center justify-center mt-0.5"
               :class="iconBg(n.type)">
            <span class="text-base leading-none">{{ iconEmoji(n.type) }}</span>
          </div>

          <!-- İçerik -->
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-gray-900 leading-snug" :class="{ 'font-semibold': !n.isRead }">
              {{ n.title }}
            </p>
            <p class="text-xs text-gray-500 mt-0.5 line-clamp-2">{{ n.message }}</p>
            <p class="text-[10px] text-gray-400 mt-1">{{ timeAgo(n.createdAt) }}</p>
          </div>

          <!-- Okunmamış nokta -->
          <div v-if="!n.isRead" class="flex-shrink-0 w-2 h-2 bg-blue-500 rounded-full mt-2"></div>
        </li>
      </ul>

      <!-- Daha fazla yükle -->
      <div v-if="hasMore && notifications.length > 0" class="flex justify-center py-3">
        <button
          @click="$emit('load-more')"
          :disabled="loading"
          class="text-xs text-blue-600 hover:text-blue-800 font-medium disabled:opacity-50 transition-colors"
        >
          {{ loading ? 'Yükleniyor...' : 'Daha fazla göster' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotificationPanel',
  props: {
    notifications: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    hasMore: { type: Boolean, default: false }
  },
  emits: ['close', 'mark-read', 'mark-all-read', 'load-more'],
  methods: {
    handleClick(n) {
      if (!n.isRead) {
        this.$emit('mark-read', n.id)
      }
      // Göreve yönlendir
      if (n.entityType === 'task' && n.data?.taskId) {
        this.$router.push(`/task/${n.data.taskId}`)
        this.$emit('close')
      }
      // Destek talebine yönlendir — adminler yeni talep/kullanıcı yanıtı
      // bildirimlerini admin panelinde, kullanıcılar kendi talep detayında görür
      if (n.entityType === 'support_ticket' && n.data?.ticketId) {
        const isAdminNotification = n.type === 'SUPPORT_TICKET_CREATED' ||
            (n.type === 'SUPPORT_TICKET_REPLIED' && n.data?.isAdminReply === false)
        this.$router.push(isAdminNotification ? '/admin' : `/support/${n.data.ticketId}`)
        this.$emit('close')
      }
    },
    iconEmoji(type) {
      const map = {
        TASK_ASSIGNED: '👤',
        TASK_UNASSIGNED: '🚪',
        TASK_STATUS_CHANGED: '🔄',
        TASK_PRIORITY_CHANGED: '⚡',
        TASK_COMMENTED: '💬',
        TASK_DUE_SOON: '⏰',
        TASK_OVERDUE: '🔴',
        WATCHED_TASK_UPDATED: '👁️',
        INVITATION_RECEIVED: '✉️',
        INVITATION_ACCEPTED: '✅',
        SUPPORT_TICKET_CREATED: '🎫',
        SUPPORT_TICKET_REPLIED: '💬',
        SUPPORT_TICKET_STATUS_CHANGED: '🔄',
        MENTION: '@',
        SYSTEM: 'ℹ️'
      }
      return map[type] ?? '🔔'
    },
    iconBg(type) {
      if (['TASK_ASSIGNED', 'WATCHED_TASK_UPDATED'].includes(type)) return 'bg-blue-100'
      if (type === 'TASK_COMMENTED' || type === 'MENTION') return 'bg-purple-100'
      if (type === 'TASK_STATUS_CHANGED') return 'bg-green-100'
      if (['TASK_DUE_SOON', 'TASK_OVERDUE'].includes(type)) return 'bg-red-100'
      if (['INVITATION_RECEIVED', 'INVITATION_ACCEPTED'].includes(type)) return 'bg-indigo-100'
      return 'bg-gray-100'
    },
    timeAgo(dateStr) {
      if (!dateStr) return ''
      const diff = Date.now() - new Date(dateStr).getTime()
      const mins = Math.floor(diff / 60000)
      if (mins < 1) return 'Az önce'
      if (mins < 60) return `${mins} dakika önce`
      const hrs = Math.floor(mins / 60)
      if (hrs < 24) return `${hrs} saat önce`
      const days = Math.floor(hrs / 24)
      if (days < 7) return `${days} gün önce`
      return new Date(dateStr).toLocaleDateString('tr-TR')
    }
  }
}
</script>

