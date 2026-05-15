<template>
  <div class="max-w-2xl mx-auto">
    <div class="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
      <!-- Header -->
      <div class="bg-gradient-to-r from-yellow-400 via-orange-500 to-red-500 p-8 text-center">
        <p class="text-6xl mb-4">🏆</p>
        <h1 class="text-3xl font-bold text-white mb-2">Yarışma Tamamlandı!</h1>
        <p class="text-white/80">{{ session.templateTitle }}</p>
      </div>

      <!-- Podyum -->
      <div class="p-6">
        <div v-if="leaderboard.length > 0" class="flex justify-center items-end gap-4 mb-8 pt-4">
          <!-- 2. sıra -->
          <div v-if="leaderboard[1]" class="text-center">
            <div class="w-16 h-16 mx-auto bg-gray-200 rounded-full flex items-center justify-center text-gray-700 font-bold text-lg mb-2">
              {{ getInitials(leaderboard[1].displayName) }}
            </div>
            <p class="text-sm font-medium text-gray-800">{{ leaderboard[1].displayName }}</p>
            <div class="mt-2 w-20 mx-auto bg-gray-300 rounded-t-lg flex items-center justify-center" style="height: 80px">
              <div class="text-center">
                <p class="text-2xl">🥈</p>
                <p class="text-sm font-bold text-gray-700">{{ leaderboard[1].totalScore }}</p>
              </div>
            </div>
          </div>

          <!-- 1. sıra -->
          <div v-if="leaderboard[0]" class="text-center">
            <div class="w-20 h-20 mx-auto bg-yellow-200 rounded-full flex items-center justify-center text-yellow-700 font-bold text-xl mb-2 ring-4 ring-yellow-400">
              {{ getInitials(leaderboard[0].displayName) }}
            </div>
            <p class="text-base font-bold text-gray-900">{{ leaderboard[0].displayName }}</p>
            <div class="mt-2 w-24 mx-auto bg-yellow-400 rounded-t-lg flex items-center justify-center" style="height: 120px">
              <div class="text-center">
                <p class="text-3xl">🥇</p>
                <p class="text-lg font-bold text-yellow-800">{{ leaderboard[0].totalScore }}</p>
              </div>
            </div>
          </div>

          <!-- 3. sıra -->
          <div v-if="leaderboard[2]" class="text-center">
            <div class="w-14 h-14 mx-auto bg-orange-200 rounded-full flex items-center justify-center text-orange-700 font-bold text-base mb-2">
              {{ getInitials(leaderboard[2].displayName) }}
            </div>
            <p class="text-sm font-medium text-gray-800">{{ leaderboard[2].displayName }}</p>
            <div class="mt-2 w-18 mx-auto bg-orange-300 rounded-t-lg flex items-center justify-center" style="height: 60px">
              <div class="text-center">
                <p class="text-xl">🥉</p>
                <p class="text-sm font-bold text-orange-700">{{ leaderboard[2].totalScore }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Tam Liste -->
        <div class="space-y-2">
          <div v-for="entry in leaderboard" :key="entry.email"
               class="flex items-center justify-between px-5 py-3 rounded-xl transition-colors"
               :class="entry.rank <= 3 ? 'bg-yellow-50 border border-yellow-200' : 'bg-gray-50 border border-gray-200'">
            <div class="flex items-center gap-4">
              <span class="w-8 text-center font-bold" :class="entry.rank <= 3 ? 'text-yellow-600' : 'text-gray-500'">
                {{ entry.rank }}
              </span>
              <span class="font-medium text-gray-800">{{ entry.displayName }}</span>
            </div>
            <div class="flex items-center gap-4 text-sm">
              <span class="text-green-600 font-medium">{{ entry.correctCount }}/{{ entry.answeredCount }} doğru</span>
              <span class="font-bold text-indigo-600 text-lg">{{ entry.totalScore }}</span>
            </div>
          </div>
        </div>

        <!-- Geri Dön -->
        <div class="mt-8 text-center">
          <button @click="$emit('back')"
                  class="px-8 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors font-medium">
            ← Ana Sayfaya Dön
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'QuizLeaderboard',
  props: {
    session: Object,
    teamId: String
  },
  emits: ['back'],
  computed: {
    leaderboard() {
      return this.session?.leaderboard || []
    }
  },
  methods: {
    getInitials(name) {
      if (!name) return '?'
      const words = name.trim().split(' ').filter(w => w.length > 0)
      if (words.length >= 2) return (words[0][0] + words[words.length - 1][0]).toUpperCase()
      return words[0][0].toUpperCase()
    }
  }
}
</script>

