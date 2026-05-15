<template>
  <div class="max-w-2xl mx-auto">
    <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
      <!-- Header -->
      <div class="bg-gradient-to-r from-indigo-600 to-purple-600 p-8 text-center">
        <h1 class="text-3xl font-bold text-white mb-2">🏆 {{ session.templateTitle }}</h1>
        <p class="text-indigo-200">Host: {{ session.hostName }}</p>
        <div class="mt-4 inline-flex items-center px-4 py-2 bg-white/20 rounded-full backdrop-blur-sm">
          <span class="text-white font-medium">Lobby — Katılımcılar bekleniyor</span>
        </div>
      </div>

      <!-- Katılımcılar -->
      <div class="p-6">
        <h3 class="text-lg font-semibold text-gray-800 mb-4">
          Katılımcılar ({{ participants.length }})
        </h3>

        <div v-if="participants.length === 0" class="text-center py-8 text-gray-400">
          <p class="text-4xl mb-2">👋</p>
          <p>Henüz kimse katılmadı</p>
        </div>

        <div v-else class="grid grid-cols-2 md:grid-cols-3 gap-3 mb-6">
          <div v-for="p in participants" :key="p.email"
               class="flex items-center gap-3 px-4 py-3 bg-gradient-to-r from-indigo-50 to-purple-50 rounded-xl border border-indigo-100">
            <div class="w-10 h-10 bg-indigo-200 rounded-full flex items-center justify-center text-indigo-700 font-bold text-sm">
              {{ getInitials(p.displayName) }}
            </div>
            <span class="text-sm font-medium text-gray-800 truncate">{{ p.displayName }}</span>
          </div>
        </div>

        <!-- Bilgi -->
        <div class="bg-indigo-50 rounded-xl p-4 mb-6 text-center">
          <p class="text-indigo-700 text-sm">
            📝 {{ session.totalQuestions }} soru
          </p>
        </div>

        <!-- Katıl / Başlat Butonları -->
        <div class="flex justify-center gap-4">
          <button v-if="!hasJoined" @click="join"
                  class="px-8 py-3 bg-green-600 text-white rounded-xl hover:bg-green-700 transition-all font-bold text-lg shadow-lg hover:shadow-xl transform hover:-translate-y-0.5">
            🎯 Katıl
          </button>
          <div v-else class="px-6 py-3 bg-green-100 text-green-700 rounded-xl font-medium">
            ✓ Katıldınız
          </div>

          <button v-if="isHost" @click="$emit('start')"
                  :disabled="participants.length === 0"
                  class="px-8 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-all font-bold text-lg shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed">
            ▶ Başlat
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { joinSession } from '../../api/QuizApi.js'
import { createToast } from 'mosha-vue-toastify'

export default {
  name: 'QuizLobby',
  props: {
    session: Object,
    isHost: Boolean,
    teamId: String
  },
  emits: ['start', 'joined'],
  computed: {
    participants() {
      return this.session?.participants || []
    },
    hasJoined() {
      const email = localStorage.getItem('user') || ''
      return this.participants.some(p => p.email === email)
    }
  },
  methods: {
    getInitials(name) {
      if (!name) return '?'
      const words = name.trim().split(' ').filter(w => w.length > 0)
      if (words.length >= 2) return (words[0][0] + words[words.length - 1][0]).toUpperCase()
      return words[0][0].toUpperCase()
    },
    async join() {
      try {
        await joinSession(this.teamId, this.session.id)
        createToast('Yarışmaya katıldınız!', { type: 'success' })
        this.$emit('joined')
      } catch (e) {
        createToast('Katılım başarısız', { type: 'danger' })
      }
    }
  }
}
</script>

