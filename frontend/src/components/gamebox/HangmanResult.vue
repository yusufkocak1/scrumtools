<template>
  <div class="max-w-2xl mx-auto">
    <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
      <div class="bg-gradient-to-r from-yellow-400 via-orange-400 to-pink-500 p-8 text-center">
        <p class="text-6xl mb-2">🏆</p>
        <h1 class="text-2xl sm:text-3xl font-bold text-white mb-1">Oyun Bitti!</h1>
        <p v-if="winner" class="text-white/90">
          Kazanan: <strong>{{ winner.displayName }}</strong> — {{ winner.totalScore }} puan
        </p>
      </div>

      <div class="p-6">
        <!-- Podyum -->
        <div v-if="leaderboard.length" class="space-y-2 mb-6">
          <div v-for="entry in leaderboard" :key="entry.email"
               :class="['flex items-center gap-3 px-4 py-3 rounded-xl border',
                        entry.rank === 1
                          ? 'bg-yellow-50 border-yellow-200'
                          : 'bg-gray-50 border-gray-200']">
            <span :class="['w-9 h-9 rounded-full flex items-center justify-center font-bold shrink-0',
                           rankClass(entry.rank)]">
              {{ medal(entry.rank) }}
            </span>
            <div class="flex-1 min-w-0">
              <p class="font-semibold text-gray-800 truncate">
                {{ entry.displayName }}
                <span v-if="entry.email === myEmail" class="text-xs text-indigo-600">(sen)</span>
              </p>
              <p class="text-xs text-gray-500">
                {{ entry.wordsSolved }} kelime bildi ·
                {{ entry.correctLetterCount }} doğru harf ·
                {{ entry.wrongLetterCount }} yanlış harf
              </p>
            </div>
            <span class="text-xl font-bold text-indigo-600 shrink-0">{{ entry.totalScore }}</span>
          </div>
        </div>

        <div v-else class="text-center py-8 text-gray-400">
          <p class="text-4xl mb-2">🤷</p>
          <p>Bu oyunda puan alan olmadı</p>
        </div>

        <button @click="$emit('back')"
                class="w-full px-6 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-bold shadow-lg">
          ← GameBox'a Dön
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HangmanResult',
  props: {
    session: Object
  },
  emits: ['back'],
  computed: {
    leaderboard() {
      return this.session?.leaderboard || []
    },
    winner() {
      return this.leaderboard[0] || null
    },
    myEmail() {
      return localStorage.getItem('user') || ''
    }
  },
  methods: {
    medal(rank) {
      if (rank === 1) return '🥇'
      if (rank === 2) return '🥈'
      if (rank === 3) return '🥉'
      return rank
    },
    rankClass(rank) {
      if (rank === 1) return 'bg-yellow-100 text-yellow-700'
      if (rank === 2) return 'bg-gray-200 text-gray-700'
      if (rank === 3) return 'bg-orange-100 text-orange-700'
      return 'bg-gray-100 text-gray-500 text-sm'
    }
  }
}
</script>
