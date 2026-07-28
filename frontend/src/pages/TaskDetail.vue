<template>
  <div class="flex flex-col lg:flex-row min-h-screen w-full bg-gray-50">
    <div class="flex flex-col flex-1 w-full min-w-0">

      <!-- ── Başlık ── -->
      <header class="sticky top-0 z-20 bg-white/95 backdrop-blur-sm border-b border-gray-200">
        <div class="px-4 sm:px-6 py-3 flex items-center gap-3">
          <button
            class="flex-shrink-0 w-8 h-8 rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50 hover:text-gray-700 transition-colors flex items-center justify-center"
            title="Geri"
            @click="$router.go(-1)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
          </button>

          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 text-[11px] text-gray-400">
              <span>{{ issueTypeIcon }}</span>
              <span class="font-mono text-gray-500">{{ task?.customId || taskId }}</span>
              <span v-if="task?.status" class="status-pill" :class="statusPillClass">{{ task.status }}</span>
            </div>
            <h1 class="text-base sm:text-lg font-semibold text-gray-900 truncate leading-tight">
              {{ task?.title || 'Yükleniyor…' }}
            </h1>
          </div>

          <div class="flex items-center gap-1.5">
            <button
              class="head-btn head-btn--amber"
              :disabled="startingPoker || !task"
              title="Scrum Poker ile takımca puanla"
              @click="estimateWithPoker"
            >
              <span class="text-sm leading-none">🃏</span>
              <span class="hidden sm:inline">{{ startingPoker ? 'Başlatılıyor…' : 'Puanla' }}</span>
            </button>

            <button class="head-btn" :disabled="!task" @click="showEditForm = true">
              <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
              <span class="hidden sm:inline">Düzenle</span>
            </button>

            <!-- Sayfa eylemleri tek menüde toplanır; başlık satırı kalabalıklaşmasın -->
            <div class="relative" data-task-menu>
              <button class="head-btn head-btn--icon" title="Diğer işlemler" @click="menuOpen = !menuOpen">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <circle cx="10" cy="4" r="1.6"/><circle cx="10" cy="10" r="1.6"/><circle cx="10" cy="16" r="1.6"/>
                </svg>
              </button>

              <div v-if="menuOpen" class="menu">
                <button class="menu-item" @click="startLayoutEditing">
                  <span>🧩</span> Düzeni değiştir
                </button>
                <button class="menu-item" @click="collapseAll(true)">
                  <span>⊟</span> Tüm panelleri daralt
                </button>
                <button class="menu-item" @click="collapseAll(false)">
                  <span>⊞</span> Tüm panelleri genişlet
                </button>
                <div class="menu-sep"></div>
                <button class="menu-item menu-item--danger" @click="cancelTask">
                  <span>⊘</span> Görevi iptal et
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Düzen modu şeridi -->
        <div v-if="layoutEditing" class="px-4 sm:px-6 py-2 bg-blue-50 border-t border-blue-100 flex flex-wrap items-center gap-2">
          <span class="text-xs text-blue-800 flex-1 min-w-0">
            Panelleri sürükleyerek sıralayın veya oklarla kolon değiştirin. Düzen bu tarayıcıda saklanır.
          </span>
          <button class="text-xs font-semibold text-blue-700 hover:text-blue-900 px-2 py-1" @click="resetLayout">
            Varsayılana dön
          </button>
          <button class="text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-lg px-3 py-1.5" @click="toggleLayoutEditing">
            Bitti
          </button>
        </div>
      </header>

      <!-- ── Yükleniyor ── -->
      <div v-if="loading" class="task-grid px-4 sm:px-6 py-5">
        <div class="task-col">
          <div v-for="n in 3" :key="n" class="bg-white rounded-xl border border-gray-200 p-4 animate-pulse">
            <div class="h-4 bg-gray-100 rounded w-32 mb-3"></div>
            <div class="space-y-2">
              <div class="h-3 bg-gray-100 rounded w-full"></div>
              <div class="h-3 bg-gray-100 rounded w-4/5"></div>
            </div>
          </div>
        </div>
        <div class="task-col">
          <div class="bg-white rounded-xl border border-gray-200 p-4 animate-pulse">
            <div class="h-4 bg-gray-100 rounded w-24 mb-3"></div>
            <div class="space-y-2">
              <div class="h-7 bg-gray-100 rounded"></div>
              <div class="h-7 bg-gray-100 rounded"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Bulunamadı ── -->
      <div v-else-if="!task" class="flex items-center justify-center min-h-[400px] px-4">
        <div class="text-center max-w-sm">
          <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-2xl flex items-center justify-center">
            <svg class="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                    d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <h3 class="text-base font-semibold text-gray-900 mb-1">Görev bulunamadı</h3>
          <p class="text-sm text-gray-500 mb-5">Aradığınız görev mevcut değil veya silinmiş olabilir.</p>
          <button class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors" @click="$router.go(-1)">
            Geri dön
          </button>
        </div>
      </div>

      <!-- ── İçerik ── -->
      <div v-else class="task-grid px-4 sm:px-6 py-5">
        <div
          v-for="column in ['main', 'side']"
          :key="column"
          class="task-col"
          :class="{ 'task-col--editing': layoutEditing }"
          @dragenter.self="dragOverColumn(column)"
          @dragover.prevent
          @drop.prevent="endDrag"
        >
          <component
            v-for="key in columns[column]"
            :is="panelComponent(key)"
            :key="key"
            :task="task"
            :teamId="teamId"
            :subtasks="subtasks"
            :links="taskLinks"
            :releases="selectableReleases"
            :deployments="taskDeployments"
            @refresh="refreshTask"
            @refresh-links="refreshLinks"
            @open-task="openTask"
            @field="updateTaskField"
            @patch="applyPatch"
          />
        </div>
      </div>

      <AddTaskForm
        :is-open="showEditForm"
        :task="task"
        :teamId="teamId || ''"
        :projects="teamProjects"
        @close="showEditForm = false"
        @updateTask="applyPatch"
      />
    </div>
  </div>
</template>

<script>
/**
 * Görev detay sayfası.
 *
 * Sayfa artık sabit bir yerleşim değil, panel listesidir: her bölüm kendi
 * TaskPanel kabuğunu taşır ve `useTaskLayout` hangi panelin hangi kolonda,
 * hangi sırada durduğunu tutar. Kullanıcı panelleri sürükleyerek taşıyabildiği
 * için sayfa buradan yalnız veriyi ve ortak event'leri dağıtır.
 */
import { provide } from 'vue';
import { updateTask, searchByCustomId, getLinks, getSubtasks } from '../api/WorkApi.js';
import { setPokerTask } from '../api/ScrumPokerApi.js';
import { getTeamReleases, getTaskDeployments } from '../api/ReleaseApi.js';
import { getTeamById } from '../api/TeamApi.js';
import { useTaskLayout } from '../composables/useTaskLayout.js';

import AddTaskForm from '../components/work/AddTaskForm.vue';
import TaskDescriptionPanel from '../components/work/panels/TaskDescriptionPanel.vue';
import TaskRelationsPanel from '../components/work/panels/TaskRelationsPanel.vue';
import TaskActivityPanel from '../components/work/panels/TaskActivityPanel.vue';
import TaskDetailsPanel from '../components/work/panels/TaskDetailsPanel.vue';
import TaskDeploymentsPanel from '../components/work/panels/TaskDeploymentsPanel.vue';
import AttachmentList from '../components/work/AttachmentList.vue';
import WatcherList from '../components/work/WatcherList.vue';
import DevPanel from '../components/scm/DevPanel.vue';
import CiDeploySection from '../components/ci/CiDeploySection.vue';

/** Panel anahtarı → bileşen. Anahtarlar useTaskLayout'taki düzenle aynı olmalı. */
const PANEL_COMPONENTS = {
  description: TaskDescriptionPanel,
  attachments: AttachmentList,
  relations: TaskRelationsPanel,
  activity: TaskActivityPanel,
  details: TaskDetailsPanel,
  development: DevPanel,
  deploy: CiDeploySection,
  deployments: TaskDeploymentsPanel,
  watchers: WatcherList,
};

export default {
  name: 'TaskDetailPage',
  components: { AddTaskForm },
  props: {
    taskId: String,
  },
  setup() {
    const taskLayout = useTaskLayout();
    // Paneller kabuklarını (TaskPanel) buradan besler: katlama, sürükleme, taşıma
    provide('taskLayout', taskLayout);
    return {
      columns: taskLayout.layout,
      layoutEditing: taskLayout.editing,
      toggleLayoutEditing: taskLayout.toggleEditing,
      resetLayout: taskLayout.resetLayout,
      setAllCollapsed: taskLayout.setAllCollapsed,
      dragOverColumn: taskLayout.dragOverColumn,
      endDrag: taskLayout.endDrag,
    };
  },
  data() {
    return {
      task: null,
      teamId: null,
      loading: true,
      showEditForm: false,
      menuOpen: false,
      taskLinks: [],
      subtasks: [],
      releases: [],
      teamProjects: [],
      taskDeployments: [],
      startingPoker: false,
    };
  },
  computed: {
    issueTypeIcon() {
      const icons = { bug: '🐛', story: '📖', epic: '⚡', task: '✅', subtask: '📌' };
      return icons[(this.task?.issueType || 'task').toLowerCase()] || '✅';
    },
    statusPillClass() {
      const status = (this.task?.status || '').toLowerCase();
      if (status === 'done') return 'bg-green-100 text-green-700';
      if (status === 'in progress') return 'bg-blue-100 text-blue-700';
      if (status === 'cancelled') return 'bg-red-100 text-red-700';
      return 'bg-gray-100 text-gray-600';
    },
    selectableReleases() {
      // Yayınlanmış/iptal sürümler yeni seçim olarak sunulmaz; mevcut bağ görünür kalır
      return this.releases.filter(r =>
        (r.status !== 'RELEASED' && r.status !== 'CANCELLED') || r.id === this.task?.releaseId
      );
    },
  },
  watch: {
    // RouterView :key'siz olduğundan alt görev/ilişki tıklamasıyla TaskDetail→TaskDetail
    // geçişinde bileşen remount olmaz — route param değişince task'ı yeniden yükle
    taskId() {
      this.loadTask();
    },
  },
  async mounted() {
    await this.loadTask();
    document.addEventListener('mousedown', this.handleMenuClickOutside);
  },
  beforeUnmount() {
    document.removeEventListener('mousedown', this.handleMenuClickOutside);
  },
  methods: {
    panelComponent(key) {
      return PANEL_COMPONENTS[key];
    },

    async loadTask() {
      try {
        this.loading = true;
        this.taskLinks = [];
        this.subtasks = [];
        const result = await searchByCustomId(this.taskId);
        if (result) {
          this.task = result;
          this.teamId = result.teamId;
          await Promise.all([
            this.refreshLinks(), this.refreshSubtasks(), this.loadReleases(),
            this.loadDeployments(), this.loadTeamProjects(),
          ]);
        } else {
          this.task = null;
        }
      } catch (error) {
        console.error('Görev yüklenemedi:', error);
        this.task = null;
      } finally {
        this.loading = false;
      }
    },
    async refreshTask() {
      if (!this.teamId || !this.task?.id) return;
      try {
        const result = await searchByCustomId(this.taskId);
        if (result) this.task = result;
        await this.refreshSubtasks();
      } catch (e) {
        console.error(e);
      }
    },
    async refreshLinks() {
      if (!this.teamId || !this.task?.id) return;
      try {
        this.taskLinks = await getLinks(this.teamId, this.task.id);
      } catch (e) {
        this.taskLinks = [];
      }
    },
    async refreshSubtasks() {
      if (!this.teamId || !this.task?.id) return;
      try {
        this.subtasks = await getSubtasks(this.teamId, this.task.id);
      } catch (e) {
        this.subtasks = [];
      }
    },
    async loadReleases() {
      if (!this.teamId) return;
      try {
        // Sürümler görevin projesine ait — takım birden fazla projede çalışabilir.
        this.releases = await getTeamReleases(this.teamId, this.task?.projectId || null);
      } catch (e) {
        this.releases = [];
      }
    },
    /** Takımın projeleri — düzenleme formunda görevi başka projeye taşıyabilmek için. */
    async loadTeamProjects() {
      if (!this.teamId) return;
      try {
        const team = await getTeamById(this.teamId);
        this.teamProjects = team?.projects || [];
      } catch (e) {
        this.teamProjects = [];
      }
    },
    async loadDeployments() {
      if (!this.teamId || !this.task?.id) return;
      try {
        this.taskDeployments = await getTaskDeployments(this.teamId, this.task.id);
      } catch (e) {
        this.taskDeployments = [];
      }
    },

    /** Panel zaten kaydettiği bir değişikliği bildirdi — yerel görevi güncelle. */
    applyPatch(patch) {
      this.task = { ...this.task, ...patch };
    },

    /**
     * Tek alanlık güncelleme. İstek başarısız olursa (ör. paket kapandığı için
     * sürüm değişimi reddedilir) alan eski değerine döner.
     */
    async updateTaskField(field, value) {
      if (!this.task || !this.teamId || !this.task.id) return;
      const previous = this.task[field];
      // Boş sürüm seçimi sunucuya '' gider, yerelde null tutulur
      const localValue = field === 'releaseId' ? (value || null) : value;
      this.task = { ...this.task, [field]: localValue };
      try {
        const updatedAt = new Date().toISOString();
        await updateTask(this.teamId, this.task.id, { [field]: value, updatedAt });
        this.task = { ...this.task, updatedAt };
      } catch (e) {
        console.error('Alan güncellenemedi:', e);
        this.task = { ...this.task, [field]: previous };
      }
    },

    openTask(customId) {
      if (!customId) return;
      this.$router.push({ name: 'TaskDetail', params: { taskId: customId } });
    },

    // Görevi takımın poker oturumuna bağlar ve ScrumPoker sayfasına geçer.
    // Puan uygulandığında ScrumPoker sayfası bu göreve geri yönlendirir.
    async estimateWithPoker() {
      if (!this.teamId || !this.task?.id) return;
      this.startingPoker = true;
      try {
        await setPokerTask(this.teamId, this.task.id);
        this.$router.push({ name: 'ScrumPoker', params: { teamId: this.teamId } });
      } catch (e) {
        console.error('Poker oturumu başlatılamadı:', e);
        alert('Scrum Poker oturumu başlatılamadı. Lütfen tekrar deneyin.');
      } finally {
        this.startingPoker = false;
      }
    },

    cancelTask() {
      this.menuOpen = false;
      if (!confirm('Görev iptal edilecek. Emin misiniz?')) return;
      this.updateTaskField('status', 'Cancelled');
    },

    startLayoutEditing() {
      this.menuOpen = false;
      if (!this.layoutEditing) this.toggleLayoutEditing();
    },
    collapseAll(value) {
      this.menuOpen = false;
      this.setAllCollapsed(value);
    },
    handleMenuClickOutside(event) {
      if (this.menuOpen && !event.target.closest?.('[data-task-menu]')) this.menuOpen = false;
    },
  },
};
</script>

<style scoped>
/* Ana kolon okuma/yazma akışı, yan kolon üstveri — yan kolon sabit genişlikte
   kalır ki Geliştirme/Deploy panelleri her ekranda aynı yerde dursun */
.task-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 1rem;
}
@media (min-width: 1024px) {
  .task-grid {
    grid-template-columns: minmax(0, 1fr) 340px;
    gap: 1.25rem;
  }
}
@media (min-width: 1280px) {
  .task-grid {
    grid-template-columns: minmax(0, 1fr) 380px;
  }
}

.task-col {
  @apply flex flex-col gap-4 min-w-0;
}
/* Boşalan kolon da bırakma hedefi kalmalı — yoksa taşınan panel geri dönemez */
.task-col--editing {
  @apply rounded-xl outline-dashed outline-1 outline-offset-4 outline-blue-200 min-h-[96px];
}

.status-pill {
  @apply inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase tracking-wide;
}

.head-btn {
  @apply inline-flex items-center gap-1.5 px-2.5 sm:px-3 py-1.5 rounded-lg border border-gray-200 bg-white text-xs font-semibold text-gray-700 hover:bg-gray-50 hover:border-gray-300 transition-colors disabled:opacity-50 disabled:cursor-not-allowed;
}
.head-btn--amber {
  @apply border-amber-200 text-amber-700 bg-amber-50 hover:bg-amber-100 hover:border-amber-300;
}
.head-btn--icon {
  @apply px-2 text-gray-500;
}

.menu {
  @apply absolute right-0 top-full mt-1.5 w-56 bg-white border border-gray-200 rounded-xl shadow-lg py-1 z-30;
}
.menu-item {
  @apply w-full flex items-center gap-2.5 px-3 py-2 text-left text-xs font-medium text-gray-700 hover:bg-gray-50 transition-colors;
}
.menu-item--danger {
  @apply text-red-600 hover:bg-red-50;
}
.menu-sep {
  @apply my-1 border-t border-gray-100;
}
</style>
