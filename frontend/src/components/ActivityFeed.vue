<template>
  <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
    <!-- Başlık -->
    <div class="flex items-center justify-between px-5 py-3 border-b border-gray-100 bg-gray-50">
      <div class="flex items-center gap-2">
        <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M13 10V3L4 14h7v7l9-11h-7z"/>
        </svg>
        <span class="text-sm font-semibold text-gray-900">Aktivite Akışı</span>
      </div>
      <button
        v-if="hasMore"
        @click="$emit('load-more')"
        :disabled="loading"
        class="text-xs text-blue-600 hover:text-blue-800 font-medium disabled:opacity-50 transition-colors"
      >
        {{ loading ? 'Yükleniyor...' : 'Daha fazla' }}
      </button>
    </div>

    <!-- Yükleniyor -->
    <div v-if="loading && events.length === 0" class="flex justify-center py-8">
      <svg class="animate-spin w-5 h-5 text-blue-500" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
      </svg>
    </div>

    <!-- Boş durum -->
    <div v-else-if="events.length === 0" class="flex flex-col items-center justify-center py-10 text-gray-400">
      <svg class="w-10 h-10 mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
      </svg>
      <p class="text-sm">Henüz aktivite yok</p>
    </div>

    <!-- Aktivite listesi -->
    <ul v-else class="divide-y divide-gray-50 max-h-[400px] overflow-y-auto">
      <li v-for="event in events" :key="event.id" class="flex items-start gap-3 px-4 py-3 hover:bg-gray-50 transition-colors">
        <!-- Aksiyon ikonu -->
        <div class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center mt-0.5"
             :class="actionBg(event.action)">
          <span class="text-sm leading-none">{{ actionEmoji(event.action) }}</span>
        </div>

        <!-- İçerik -->
        <div class="flex-1 min-w-0">
          <p class="text-sm text-gray-800 leading-snug">
            <span class="font-medium">{{ shortEmail(event.actorEmail) }}</span>
            {{ actionLabel(event.action) }}
            <span v-if="event.details?.customId" class="font-mono text-xs bg-gray-100 text-gray-700 px-1.5 py-0.5 rounded ml-1">
              {{ event.details.customId }}
            </span>
          </p>
          <p v-if="event.details?.title" class="text-xs text-gray-500 mt-0.5 truncate">{{ event.details.title }}</p>
          <p class="text-[10px] text-gray-400 mt-0.5">{{ timeAgo(event.createdAt) }}</p>
        </div>
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'ActivityFeed',
  props: {
    events: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    hasMore: { type: Boolean, default: false }
  },
  emits: ['load-more'],
  methods: {
    shortEmail(email) {
      if (!email) return '?'
      return email.split('@')[0]
    },
    actionLabel(action) {
      const labels = {
        TASK_CREATED: 'bir görev oluşturdu',
        TASK_UPDATED: 'görevi güncelledi',
        TASK_DELETED: 'görevi sildi',
        TASK_STATUS_CHANGED: 'görev durumunu değiştirdi',
        TASK_ASSIGNED: 'görevi atadı',
        TASK_COMMENTED: 'göreve yorum ekledi',
        TASK_ATTACHMENT_ADDED: 'dosya ekledi',
        SPRINT_CREATED: 'sprint oluşturdu',
        SPRINT_STARTED: 'sprint başlattı',
        SPRINT_COMPLETED: 'sprint tamamlandı',
        MEMBER_ADDED: 'takıma üye ekledi',
        MEMBER_REMOVED: 'takımdan üye çıkardı',
        MEMBER_ROLE_CHANGED: 'üye rolünü değiştirdi',
        BOARD_CREATED: 'board oluşturdu',
        BOARD_UPDATED: 'board güncelledi'
      }
      return labels[action] ?? action
    },
    actionEmoji(action) {
      const map = {
        TASK_CREATED: '✨',
        TASK_UPDATED: '✏️',
        TASK_DELETED: '🗑️',
        TASK_STATUS_CHANGED: '🔄',
        TASK_ASSIGNED: '👤',
        TASK_COMMENTED: '💬',
        TASK_ATTACHMENT_ADDED: '📎',
        SPRINT_CREATED: '🏃',
        SPRINT_STARTED: '▶️',
        SPRINT_COMPLETED: '🏁',
        MEMBER_ADDED: '➕',
        MEMBER_REMOVED: '➖',
        MEMBER_ROLE_CHANGED: '🎭',
        BOARD_CREATED: '📋',
        BOARD_UPDATED: '📝'
      }
      return map[action] ?? '⚡'
    },
    actionBg(action) {
      if (action?.startsWith('TASK_')) return 'bg-blue-100'
      if (action?.startsWith('SPRINT_')) return 'bg-green-100'
      if (action?.startsWith('MEMBER_')) return 'bg-purple-100'
      if (action?.startsWith('BOARD_')) return 'bg-orange-100'
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

