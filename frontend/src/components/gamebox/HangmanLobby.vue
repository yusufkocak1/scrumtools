<template>
  <div class="max-w-2xl mx-auto">
    <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
      <div class="bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 p-8 text-center">
        <p class="text-5xl mb-2">🪢</p>
        <h1 class="text-2xl sm:text-3xl font-bold text-white mb-1">Adam Asmaca — Lobi</h1>
        <p class="text-white/80 text-sm">Moderatör: {{ session.hostName }}</p>
        <div class="mt-4 inline-flex items-center px-4 py-2 bg-white/20 rounded-full backdrop-blur-sm">
          <span class="text-white font-medium text-sm">Oyuncular bekleniyor</span>
        </div>
      </div>

      <div class="p-6">
        <!-- Oyun bilgisi -->
        <div class="grid grid-cols-3 gap-3 mb-6 text-center">
          <div class="bg-gray-50 rounded-xl p-3">
            <p class="text-xs text-gray-500 mb-1">Dil</p>
            <p class="font-semibold text-gray-900">{{ session.language === 'tr' ? '🇹🇷 Türkçe' : '🇬🇧 English' }}</p>
          </div>
          <div class="bg-gray-50 rounded-xl p-3">
            <p class="text-xs text-gray-500 mb-1">Kelime</p>
            <p class="font-semibold text-gray-900">{{ session.totalRounds }}</p>
          </div>
          <div class="bg-gray-50 rounded-xl p-3">
            <p class="text-xs text-gray-500 mb-1">Kaynak</p>
            <p class="font-semibold text-gray-900">
              {{ session.wordSource === 'CUSTOM' ? '✍️ Moderatör' : '🎲 Rastgele' }}
            </p>
          </div>
        </div>

        <div v-if="session.wordSource === 'CUSTOM'"
             class="flex items-start gap-2 bg-amber-50 border border-amber-200 rounded-xl p-3 mb-6">
          <span class="text-lg leading-none">⚠️</span>
          <p class="text-xs text-amber-800">
            Kelimeleri moderatör belirledi, bu yüzden <strong>{{ session.hostName }}</strong> bu oyunda
            izleyici — sıraya girmiyor ve puan almıyor.
          </p>
        </div>

        <!-- Oyuncular -->
        <h3 class="text-lg font-semibold text-gray-800 mb-4">
          Oyuncular ({{ players.length }})
        </h3>

        <div v-if="players.length === 0" class="text-center py-8 text-gray-400">
          <p class="text-4xl mb-2">👋</p>
          <p>Henüz kimse katılmadı</p>
        </div>

        <div v-else class="grid grid-cols-2 md:grid-cols-3 gap-3 mb-4">
          <div v-for="p in players" :key="p.email"
               class="flex items-center gap-3 px-4 py-3 bg-gradient-to-r from-indigo-50 to-purple-50 rounded-xl border border-indigo-100">
            <div class="w-10 h-10 bg-indigo-200 rounded-full flex items-center justify-center text-indigo-700 font-bold text-sm shrink-0">
              {{ initials(p.displayName) }}
            </div>
            <span class="text-sm font-medium text-gray-800 truncate">{{ p.displayName }}</span>
          </div>
        </div>

        <!-- İzleyiciler -->
        <div v-if="spectators.length" class="mb-6">
          <p class="text-xs font-semibold text-gray-500 uppercase mb-2">İzleyici</p>
          <div class="flex flex-wrap gap-2">
            <span v-for="s in spectators" :key="s.email"
                  class="px-3 py-1.5 bg-gray-100 text-gray-600 rounded-lg text-xs font-medium">
              👁️ {{ s.displayName }}
            </span>
          </div>
        </div>

        <!-- Aksiyonlar -->
        <div class="flex flex-wrap justify-center gap-3 pt-2">
          <button v-if="!hasJoined" @click="join"
                  class="px-8 py-3 bg-green-600 text-white rounded-xl hover:bg-green-700 transition-all font-bold shadow-lg hover:shadow-xl transform hover:-translate-y-0.5">
            🎯 Oyuna Katıl
          </button>
          <div v-else-if="isSpectator" class="px-6 py-3 bg-gray-100 text-gray-600 rounded-xl font-medium">
            👁️ İzleyicisin
          </div>
          <div v-else class="px-6 py-3 bg-green-100 text-green-700 rounded-xl font-medium">
            ✓ Katıldın
          </div>

          <button v-if="isHost" @click="$emit('begin')"
                  :disabled="players.length === 0"
                  class="px-8 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-all font-bold shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none">
            ▶ Oyunu Başlat
          </button>
        </div>

        <p v-if="isHost && players.length === 0" class="text-center text-xs text-gray-400 mt-3">
          Başlatmak için en az bir oyuncu katılmalı
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { joinHangmanSession } from '../../api/HangmanApi.js'
import { createToast } from 'mosha-vue-toastify'

export default {
  name: 'HangmanLobby',
  props: {
    session: Object,
    isHost: Boolean,
    teamId: String
  },
  emits: ['begin', 'joined'],
  computed: {
    participants() {
      return this.session?.participants || []
    },
    players() {
      return this.participants.filter(p => !p.spectator)
    },
    spectators() {
      return this.participants.filter(p => p.spectator)
    },
    myEmail() {
      return localStorage.getItem('user') || ''
    },
    hasJoined() {
      return this.participants.some(p => p.email === this.myEmail)
    },
    isSpectator() {
      return this.participants.some(p => p.email === this.myEmail && p.spectator)
    }
  },
  methods: {
    initials(name) {
      if (!name) return '?'
      const words = name.trim().split(' ').filter(w => w.length > 0)
      if (words.length >= 2) return (words[0][0] + words[words.length - 1][0]).toUpperCase()
      return words[0][0].toUpperCase()
    },
    async join() {
      try {
        await joinHangmanSession(this.teamId, this.session.id)
        createToast('Oyuna katıldın!', { type: 'success' })
        this.$emit('joined')
      } catch (e) {
        // Hata interceptor tarafından otomatik gösterilir
      }
    }
  }
}
</script>
