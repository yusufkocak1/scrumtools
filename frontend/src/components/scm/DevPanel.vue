<template>
  <TaskPanel panel-key="development" title="Geliştirme" :available="visible">
    <template #icon>
      <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 16 16">
        <path d="M9.5 3.25a2.25 2.25 0 113 2.122V6A2.5 2.5 0 0110 8.5H6a1 1 0 00-1 1v1.128a2.251 2.251 0 11-1.5 0V5.372a2.25 2.25 0 111.5 0v1.836A2.492 2.492 0 016 7h4a1 1 0 001-1v-.628A2.25 2.25 0 019.5 3.25z"/>
      </svg>
    </template>

    <template #actions>
      <button v-if="canCreateBranch" type="button" class="panel-action" @click="showCreateBranch = true">
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        Branch
      </button>
    </template>

    <div v-if="loading" class="flex justify-center py-3">
      <svg class="animate-spin w-5 h-5 text-gray-300" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
      </svg>
    </div>

    <template v-else-if="data">
      <!-- Repo eşlenmemiş: yalnız yöneticiye yönlendirme -->
      <div v-if="data.repos.length === 0" class="text-sm text-gray-500">
        <p>Bu projeye henüz repo eşlenmemiş.</p>
        <router-link
          v-if="data.canManageRepos && data.projectId"
          :to="`/projects/${data.projectId}`"
          class="text-blue-600 hover:underline text-xs inline-block mt-1"
        >Proje sayfasından repo eşle →</router-link>
      </div>

      <template v-else>
        <!-- Branch'ler -->
        <section class="mb-4">
          <p class="section-label">Branch'ler ({{ data.branches.length }})</p>
          <p v-if="data.branches.length === 0" class="text-xs text-gray-400">
            Bağlı branch yok. Adında {{ taskKeyHint }} geçen branch'ler otomatik bağlanır.
          </p>
          <ul v-else class="space-y-1.5">
            <li v-for="branch in data.branches" :key="branch.id" class="flex items-center gap-1.5 min-w-0">
              <svg class="w-3.5 h-3.5 text-gray-300 flex-shrink-0" fill="currentColor" viewBox="0 0 16 16">
                <path d="M9.5 3.25a2.25 2.25 0 113 2.122V6A2.5 2.5 0 0110 8.5H6a1 1 0 00-1 1v1.128a2.251 2.251 0 11-1.5 0V5.372a2.25 2.25 0 111.5 0v1.836A2.492 2.492 0 016 7h4a1 1 0 001-1v-.628A2.25 2.25 0 019.5 3.25z"/>
              </svg>
              <a
                :href="branch.webUrl || '#'"
                target="_blank"
                rel="noopener"
                class="font-mono text-[11px] text-gray-600 hover:text-blue-600 truncate flex-1"
                :title="`${branch.name} · ${branch.repositoryName}`"
              >{{ branch.name }}</a>
              <span class="text-[10px] px-1.5 py-0.5 rounded-full flex-shrink-0" :class="branchStatusClass(branch.status)">
                {{ branchStatusLabel(branch.status) }}
              </span>
            </li>
          </ul>
        </section>

        <!-- Commit'ler -->
        <section>
          <p class="section-label">Commit'ler ({{ data.commits.length }})</p>
          <p v-if="data.commits.length === 0" class="text-xs text-gray-400">
            Henüz commit bağlanmamış. Commit mesajında {{ taskKeyHint }} geçirin.
          </p>
          <ul v-else class="space-y-2">
            <li v-for="commit in visibleCommits" :key="commit.id" class="min-w-0">
              <div class="flex items-baseline gap-1.5 min-w-0">
                <a
                  :href="commit.webUrl || '#'"
                  target="_blank"
                  rel="noopener"
                  class="font-mono text-[11px] text-blue-600 hover:underline flex-shrink-0"
                >{{ commit.sha?.substring(0, 7) }}</a>
                <span class="text-xs text-gray-700 truncate" :title="commit.shortMessage">{{ commit.shortMessage }}</span>
              </div>
              <p class="text-[10px] text-gray-400 truncate">
                {{ commit.authorName || commit.authorEmail }} · {{ formatDate(commit.authoredAt) }}
              </p>
            </li>
          </ul>
          <button
            v-if="data.commits.length > commitLimit"
            type="button"
            class="text-[11px] text-blue-600 hover:underline mt-2"
            @click="commitLimit = data.commits.length"
          >Tümünü göster ({{ data.commits.length }})</button>
        </section>
      </template>
    </template>

    <CreateBranchModal
      v-if="showCreateBranch && data"
      :teamId="teamId"
      :taskId="task.id"
      :projectId="data.projectId"
      :taskKey="task.customId"
      :taskTitle="task.title"
      :repos="data.repos"
      :hasUserAccount="data.hasUserAccount"
      @close="showCreateBranch = false"
      @created="onBranchCreated"
    />
  </TaskPanel>
</template>

<script>
/**
 * Geliştirme paneli — göreve bağlı branch ve commit'ler.
 *
 * Jira'daki gibi varsayılan olarak yan kolonda durur; bu yüzden içerik dar
 * kolonda okunacak şekilde sıkıştırılmıştır (uzun branch adları kırpılır,
 * commit listesi ilk beşle sınırlıdır).
 */
import TaskPanel from '../work/TaskPanel.vue'
import { getTaskScm } from '../../api/ScmApi.js'
import { formatRelativeTime } from '../../utils/taskFormat.js'
import { taskPanelProps, taskPanelEmits } from '../work/panels/panelProps.js'
import CreateBranchModal from './CreateBranchModal.vue'

export default {
  name: 'DevPanel',
  components: { TaskPanel, CreateBranchModal },
  props: taskPanelProps,
  emits: taskPanelEmits,
  data() {
    return {
      data: null,
      loading: true,
      commitLimit: 5,
      showCreateBranch: false,
    }
  },
  computed: {
    /**
     * Kart görünürlüğü (§13): takım projeye bağlı değilse veya feature kapalı
     * ve gösterilecek veri de yoksa panel tamamen gizlenir; repo eşlenmemişse
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
      return this.task?.customId || 'task anahtarını (örn. SCRM-12)'
    },
    canCreateBranch() {
      return !!this.data && this.data.featureEnabled && this.data.canCreateBranch
        && this.data.repos.length > 0
    },
  },
  watch: {
    'task.id'() { this.load() },
  },
  mounted() {
    this.load()
  },
  methods: {
    formatDate: formatRelativeTime,

    async load() {
      if (!this.teamId || !this.task?.id) return
      this.loading = true
      try {
        this.data = await getTaskScm(this.teamId, this.task.id)
      } catch (e) {
        console.error('Geliştirme paneli yüklenemedi:', e)
        this.data = null
      } finally {
        this.loading = false
      }
    },
    onBranchCreated() {
      this.showCreateBranch = false
      this.load()
    },
    branchStatusLabel(status) {
      return { ACTIVE: 'Aktif', MERGED: 'Merged', DELETED: 'Silindi' }[status] || status
    },
    branchStatusClass(status) {
      return {
        ACTIVE: 'bg-green-50 text-green-600',
        MERGED: 'bg-purple-50 text-purple-600',
      }[status] || 'bg-gray-100 text-gray-400 line-through'
    },
  },
}
</script>

<style scoped>
.section-label {
  @apply text-[10px] font-semibold text-gray-400 uppercase tracking-wide mb-1.5;
}
.panel-action {
  @apply inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold text-gray-500 bg-gray-100 hover:bg-gray-200 hover:text-gray-700 transition-colors;
}
</style>
