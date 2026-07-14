<template>
  <!-- Gösterilecek hiçbir şey yoksa kart tamamen gizlenir -->
  <div
    v-if="visible"
    class="bg-white rounded-xl border border-gray-200/80 shadow-sm overflow-hidden transition-shadow duration-200 hover:shadow-md"
  >
    <div class="h-1 w-full bg-gradient-to-r from-orange-400 to-rose-500"></div>
    <div class="p-5">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-base font-semibold text-gray-800 flex items-center gap-2">
          <svg class="w-5 h-5 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z"/>
          </svg>
          Geliştirme
        </h3>
      </div>

      <div v-if="loading" class="flex justify-center py-4">
        <svg class="animate-spin w-5 h-5 text-orange-500" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
        </svg>
      </div>

      <template v-else-if="data">
        <!-- Repo eşlenmemiş: yöneticiye yönlendirme -->
        <div v-if="data.repos.length === 0" class="text-sm text-gray-500 py-2">
          <p>Bu projeye henüz repo eşlenmemiş.</p>
          <router-link
            v-if="data.canManageRepos && data.projectId"
            :to="`/projects/${data.projectId}`"
            class="text-indigo-600 hover:underline text-sm inline-flex items-center gap-1 mt-1"
          >
            Proje sayfasından repo eşle →
          </router-link>
        </div>

        <template v-else>
          <!-- Branch'ler -->
          <div class="mb-4">
            <p class="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
              Branch'ler ({{ data.branches.length }})
            </p>
            <p v-if="data.branches.length === 0" class="text-sm text-gray-400">
              Bu task'a bağlı branch yok.
              <span class="text-gray-300">Adında {{ taskKeyHint }} geçen branch'ler otomatik bağlanır.</span>
            </p>
            <ul v-else class="space-y-1.5">
              <li
                v-for="branch in data.branches"
                :key="branch.id"
                class="flex items-center gap-2 text-sm"
              >
                <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="currentColor" viewBox="0 0 16 16">
                  <path d="M9.5 3.25a2.25 2.25 0 113 2.122V6A2.5 2.5 0 0110 8.5H6a1 1 0 00-1 1v1.128a2.251 2.251 0 11-1.5 0V5.372a2.25 2.25 0 111.5 0v1.836A2.492 2.492 0 016 7h4a1 1 0 001-1v-.628A2.25 2.25 0 019.5 3.25z"/>
                </svg>
                <a
                  :href="branch.webUrl || '#'"
                  target="_blank"
                  rel="noopener"
                  class="font-mono text-xs text-gray-700 hover:text-indigo-600 truncate"
                  :title="branch.name"
                >{{ branch.name }}</a>
                <span class="text-[11px] text-gray-400 flex-shrink-0">{{ branch.repositoryName }}</span>
                <span
                  :class="[
                    'text-[11px] px-1.5 py-0.5 rounded-full flex-shrink-0',
                    branch.status === 'ACTIVE' ? 'bg-green-50 text-green-600'
                      : branch.status === 'MERGED' ? 'bg-purple-50 text-purple-600'
                      : 'bg-gray-100 text-gray-400 line-through'
                  ]"
                >{{ branchStatusLabel(branch.status) }}</span>
              </li>
            </ul>
          </div>

          <!-- Commit'ler -->
          <div>
            <p class="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
              Commit'ler ({{ data.commits.length }})
            </p>
            <p v-if="data.commits.length === 0" class="text-sm text-gray-400">
              Henüz commit bağlanmamış.
              <span class="text-gray-300">Commit mesajında {{ taskKeyHint }} geçirin.</span>
            </p>
            <ul v-else class="space-y-2">
              <li
                v-for="commit in visibleCommits"
                :key="commit.id"
                class="flex gap-3 text-sm"
              >
                <a
                  :href="commit.webUrl || '#'"
                  target="_blank"
                  rel="noopener"
                  class="font-mono text-xs text-indigo-600 hover:underline flex-shrink-0 mt-0.5"
                >{{ commit.sha?.substring(0, 7) }}</a>
                <div class="flex-1 min-w-0">
                  <p class="text-gray-700 truncate" :title="commit.shortMessage">{{ commit.shortMessage }}</p>
                  <p class="text-xs text-gray-400 mt-0.5">
                    {{ commit.authorName || commit.authorEmail }}
                    · {{ formatDate(commit.authoredAt) }}
                    · {{ commit.repositoryName }}<span v-if="commit.branchHint"> ({{ commit.branchHint }})</span>
                  </p>
                </div>
              </li>
            </ul>
            <button
              v-if="data.commits.length > commitLimit"
              @click="commitLimit = data.commits.length"
              class="text-xs text-indigo-600 hover:underline mt-2"
            >
              Tümünü göster ({{ data.commits.length }})
            </button>
          </div>
        </template>
      </template>
    </div>
  </div>
</template>

<script>
import { getTaskScm } from '../../api/ScmApi.js'

export default {
  name: 'DevPanel',
  props: {
    teamId: { type: String, required: true },
    taskId: { type: String, required: true },
    // Boş durum ipuçlarında gösterilecek task anahtarı (örn. SCRM-12)
    taskKey: { type: String, default: null },
  },
  data() {
    return {
      data: null,
      loading: true,
      commitLimit: 5,
    }
  },
  computed: {
    /**
     * Kart görünürlüğü (§13): takım projeye bağlı değilse veya feature kapalı
     * ve gösterilecek veri de yoksa kart tamamen gizlenir; repo eşlenmemişse
     * sadece yöneticiye eşleme yönlendirmesi gösterilir.
     */
    visible() {
      if (this.loading) return true
      if (!this.data || !this.data.projectLinked) return false
      const hasData = this.data.branches.length > 0 || this.data.commits.length > 0
      if (!this.data.featureEnabled && !hasData) return false
      if (this.data.repos.length === 0 && !this.data.canManageRepos) return false
      return true
    },
    visibleCommits() {
      return (this.data?.commits || []).slice(0, this.commitLimit)
    },
    taskKeyHint() {
      return this.taskKey || 'task anahtarını (örn. SCRM-12)'
    },
  },
  watch: {
    taskId() { this.load() },
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        this.data = await getTaskScm(this.teamId, this.taskId)
      } catch (e) {
        console.error('Geliştirme paneli yüklenemedi:', e)
        this.data = null
      } finally {
        this.loading = false
      }
    },
    branchStatusLabel(status) {
      return { ACTIVE: 'Aktif', MERGED: 'Merged', DELETED: 'Silindi' }[status] || status
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      const diffMs = Date.now() - date.getTime()
      const diffMin = Math.floor(diffMs / 60000)
      if (diffMin < 1) return 'az önce'
      if (diffMin < 60) return `${diffMin} dk önce`
      const diffHour = Math.floor(diffMin / 60)
      if (diffHour < 24) return `${diffHour} saat önce`
      const diffDay = Math.floor(diffHour / 24)
      if (diffDay < 7) return `${diffDay} gün önce`
      return date.toLocaleDateString('tr-TR')
    },
  },
}
</script>
