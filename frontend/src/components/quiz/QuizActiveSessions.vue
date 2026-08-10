<template>
  <div v-if="sessions.length > 0" class="mb-8">
    <h2 class="text-lg font-semibold text-gray-800 mb-3 flex items-center gap-2">
      <span class="w-2 h-2 bg-red-500 rounded-full animate-pulse"></span>
      Aktif Yarışmalar ({{ sessions.length }})
    </h2>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div v-for="s in sessions" :key="s.id"
           class="bg-white rounded-xl shadow-md border-2 border-indigo-100 p-5">
        <div class="flex items-start justify-between mb-2">
          <h3 class="font-bold text-gray-900 truncate flex-1">{{ s.templateTitle }}</h3>
          <span class="ml-2 px-2 py-1 text-xs font-medium rounded-full whitespace-nowrap"
                :class="s.status === 'LOBBY'
                  ? 'bg-amber-100 text-amber-700'
                  : 'bg-green-100 text-green-700'">
            {{ s.status === 'LOBBY' ? '⏳ Lobide' : '▶ Devam ediyor' }}
          </span>
        </div>

        <p class="text-sm text-gray-500 mb-1">
          {{ s.moderatorMode ? '🎤 Moderatör' : 'Host' }}: {{ s.hostName }}
        </p>
        <p class="text-xs text-gray-400 mb-4">
          👥 {{ s.participants?.length || 0 }} katılımcı • 📝 {{ s.totalQuestions }} soru
          <template v-if="s.status === 'IN_PROGRESS'">
            • Soru {{ s.currentQuestionIndex + 1 }}/{{ s.totalQuestions }}
          </template>
        </p>

        <div class="flex gap-2">
          <button @click="$emit('open', s)"
                  class="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors text-sm font-medium">
            {{ openLabel(s) }}
          </button>
          <button v-if="isHost(s) && s.status === 'LOBBY'" @click="cancel(s)"
                  :disabled="cancellingId === s.id"
                  title="Lobiyi kapat"
                  class="px-3 py-2 bg-red-50 text-red-500 rounded-lg hover:bg-red-100 transition-colors text-sm disabled:opacity-50">
            ✕
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { cancelSession } from '../../api/QuizApi.js'
import { createToast } from 'mosha-vue-toastify'

/**
 * Takımda süren yarışmaların listesi.
 *
 * Aynı anda birden fazla oturum olabildiği için GameBox kullanıcıyı otomatik bir
 * oyuna sokmaz; buradan seçip girer.
 */
export default {
  name: 'QuizActiveSessions',
  props: {
    sessions: { type: Array, default: () => [] },
    teamId: String
  },
  emits: ['open', 'cancelled'],
  data() {
    return {
      cancellingId: null,
    }
  },
  methods: {
    currentEmail() {
      return localStorage.getItem('user') || ''
    },
    isHost(session) {
      return session.hostEmail === this.currentEmail()
    },
    hasJoined(session) {
      return (session.participants || []).some(p => p.email === this.currentEmail())
    },
    openLabel(session) {
      if (this.isHost(session)) return session.moderatorMode ? '🎤 Yönet' : '▶ Devam Et'
      if (this.hasJoined(session)) return '▶ Devam Et'
      return '🎯 Katıl'
    },
    async cancel(session) {
      if (!confirm('Lobiyi kapatmak istediğinize emin misiniz?')) return
      this.cancellingId = session.id
      try {
        await cancelSession(this.teamId, session.id)
        createToast('Lobi kapatıldı', { type: 'success' })
        this.$emit('cancelled', session.id)
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
      this.cancellingId = null
    }
  }
}
</script>
