<template>
  <div class="flex flex-col lg:flex-row w-full min-h-screen bg-gray-50">
    <SideBar :team-id="teamId" class="hidden lg:flex flex-shrink-0" />

    <div class="flex-1 flex flex-col min-w-0">
      <!-- Header -->
      <div class="bg-white border-b border-gray-200 px-4 sm:px-6 py-4 flex-shrink-0">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div class="flex flex-col sm:flex-row sm:items-center gap-4">
            <div class="flex items-center space-x-3">
              <button
                @click="$router.go(-1)"
                class="inline-flex items-center px-3 py-2 border border-gray-300 rounded-md shadow-sm text-xs sm:text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
              >
                <svg class="w-4 h-4 mr-1 sm:mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
                </svg>
                <span class="hidden xs:inline">Back to Backlog</span>
                <span class="xs:hidden">Back</span>
              </button>
              <div>
                <h1 class="text-xl sm:text-2xl font-semibold text-gray-900 leading-tight">{{ sprint.name || 'Sprint Board' }}</h1>
                <div v-if="sprint.name" class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs sm:text-sm text-gray-500 mt-1">
                  <span
                    class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] sm:text-xs font-medium"
                    :class="{
                      'bg-green-100 text-green-800': sprint.status === 'open',
                      'bg-blue-100 text-blue-800': sprint.status === 'backlog',
                      'bg-gray-100 text-gray-800': sprint.status === 'done'
                    }"
                  >
                    {{ getSprintStatusText(sprint.status) }}
                  </span>
                  <span v-if="sprint.startDate" class="text-[10px] sm:text-xs">{{ formatDate(sprint.startDate) }} - {{ formatDate(sprint.endDate) }}</span>
                  <span class="text-[10px] sm:text-xs">{{ sprintTasks.length }} issues</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Sprint Actions -->
          <div class="flex flex-wrap md:flex-nowrap items-center gap-2 md:space-x-3">
            <button
              v-if="sprint.status === 'backlog'"
              @click="startSprint"
              class="flex-1 md:flex-none inline-flex items-center justify-center px-3 sm:px-4 py-2 border border-transparent rounded-md shadow-sm text-xs sm:text-sm font-medium text-white bg-green-600 hover:bg-green-700"
            >
              <svg class="w-4 h-4 mr-1 sm:mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1m4 0h1m-6 4h.01M12 3v1m0 16v1m9-9h-1M3 12h1m15.364-6.364l-.707.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707"/>
              </svg>
              <span class="hidden xs:inline">Start Sprint</span>
              <span class="xs:hidden">Start</span>
            </button>

            <button
              v-if="sprint.status === 'open'"
              @click="completeSprint"
              class="flex-1 md:flex-none inline-flex items-center justify-center px-3 sm:px-4 py-2 border border-transparent rounded-md shadow-sm text-xs sm:text-sm font-medium text-white bg-red-600 hover:bg-red-700"
            >
              <svg class="w-4 h-4 mr-1 sm:mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              <span class="hidden xs:inline">Complete Sprint</span>
              <span class="xs:hidden">Done</span>
            </button>

            <button class="flex-1 md:flex-none inline-flex items-center justify-center px-3 py-2 border border-gray-300 rounded-md shadow-sm text-xs sm:text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">
              <svg class="w-4 h-4 mr-1 sm:mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4"/>
              </svg>
              <span class="hidden xs:inline">Settings</span>
              <span class="xs:hidden">Opts</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Mobile Scrum Board (Tabs) -->
      <div class="md:hidden p-4 space-y-4 flex-1 overflow-y-auto">
        <div class="flex overflow-x-auto no-scrollbar -mx-1 pb-1" role="tablist">
          <button
            v-for="status in statuses"
            :key="status"
            @click="activeStatus = status"
            class="mx-1 flex-1 whitespace-nowrap inline-flex items-center justify-center px-3 py-2 rounded-md text-xs font-medium border transition-colors"
            :class="activeStatus === status
              ? 'bg-blue-600 text-white border-blue-600 shadow'
              : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'"
          >
            <span>{{ status.toUpperCase() }}</span>
            <span class="ml-2 inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold"
                  :class="activeStatus === status ? 'bg-white/20 text-white' : 'bg-gray-100 text-gray-700'">
              {{ getTasksByStatus(status).length }}
            </span>
          </button>
        </div>

        <div class="space-y-3">
          <div
            v-for="task in getTasksByStatus(activeStatus)"
            :key="task.id"
            class="bg-white rounded-lg border border-gray-200 p-3 shadow-sm"
            @click="openTaskDetail(task)"
          >
            <TaskCard :task="task" />
            <div class="mt-3" @click.stop>
              <label class="block text-[10px] uppercase tracking-wide text-gray-400 mb-1">Status</label>
              <select
                class="w-full bg-white border border-gray-300 rounded-md px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500"
                :value="task.status"
                @change="e => handleMobileStatusChange(task, e.target.value)"
              >
                <option value="To Do">To Do</option>
                <option value="In Progress">In Progress</option>
                <option value="Done">Done</option>
              </select>
            </div>
          </div>
          <div v-if="getTasksByStatus(activeStatus).length === 0" class="text-center py-10 text-gray-400 text-sm">
            No tasks in this column
          </div>
        </div>
      </div>

      <!-- Desktop Scrum Board -->
      <div class="hidden md:block flex-1 p-4 md:p-6 overflow-hidden">
        <div class="flex space-x-4 lg:space-x-6 h-full w-full">
          <!-- To Do Column -->
          <div class="flex-1 min-w-0 bg-gray-50 rounded-lg">
            <div class="p-4 border-b border-gray-200 bg-white rounded-t-lg">
              <div class="flex items-center justify-between">
                <h3 class="font-medium text-gray-900 flex items-center text-sm lg:text-base">
                  <div class="w-3 h-3 bg-gray-400 rounded-full mr-2 lg:mr-3"></div>
                  TO DO
                </h3>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] lg:text-xs font-medium bg-gray-100 text-gray-800">
                  {{ getTasksByStatus('To Do').length }}
                </span>
              </div>
            </div>
            <div
              class="p-3 lg:p-4 space-y-3 h-full overflow-y-auto"
              style="height: calc(100vh - 220px);"
              @drop="onDrop('To Do')"
              @dragover.prevent
              @dragenter.prevent
            >
              <div
                v-for="task in getTasksByStatus('To Do')"
                :key="task.id"
                class="group bg-white rounded-lg border border-gray-200 p-3 lg:p-4 hover:shadow-md transition-all duration-200 cursor-move"
                draggable="true"
                @dragstart="onDragStart(task)"
                @click="openTaskDetail(task)"
              >
                <TaskCard :task="task" />
              </div>

              <div v-if="getTasksByStatus('To Do').length === 0" class="text-center py-12 text-gray-400 text-sm">
                <p class="mt-2">No tasks</p>
              </div>
            </div>
          </div>

          <!-- In Progress Column -->
          <div class="flex-1 min-w-0 bg-blue-50 rounded-lg">
            <div class="p-4 border-b border-blue-200 bg-white rounded-t-lg">
              <div class="flex items-center justify-between">
                <h3 class="font-medium text-gray-900 flex items-center text-sm lg:text-base">
                  <div class="w-3 h-3 bg-blue-400 rounded-full mr-2 lg:mr-3"></div>
                  IN PROGRESS
                </h3>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] lg:text-xs font-medium bg-blue-100 text-blue-800">
                  {{ getTasksByStatus('In Progress').length }}
                </span>
              </div>
            </div>
            <div
              class="p-3 lg:p-4 space-y-3 h-full overflow-y-auto"
              style="height: calc(100vh - 220px);"
              @drop="onDrop('In Progress')"
              @dragover.prevent
              @dragenter.prevent
            >
              <div
                v-for="task in getTasksByStatus('In Progress')"
                :key="task.id"
                class="group bg-white rounded-lg border border-blue-200 p-3 lg:p-4 hover:shadow-md transition-all duration-200 cursor-move"
                draggable="true"
                @dragstart="onDragStart(task)"
                @click="openTaskDetail(task)"
              >
                <TaskCard :task="task" />
              </div>

              <div v-if="getTasksByStatus('In Progress').length === 0" class="text-center py-12 text-blue-300 text-sm">
                <p class="mt-2">No tasks in progress</p>
              </div>
            </div>
          </div>

          <!-- Done Column -->
            <div class="flex-1 min-w-0 bg-green-50 rounded-lg">
              <div class="p-4 border-b border-green-200 bg-white rounded-t-lg">
                <div class="flex items-center justify-between">
                  <h3 class="font-medium text-gray-900 flex items-center text-sm lg:text-base">
                    <div class="w-3 h-3 bg-green-400 rounded-full mr-2 lg:mr-3"></div>
                    DONE
                  </h3>
                  <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] lg:text-xs font-medium bg-green-100 text-green-800">
                    {{ getTasksByStatus('Done').length }}
                  </span>
                </div>
              </div>
              <div
                class="p-3 lg:p-4 space-y-3 h-full overflow-y-auto"
                style="height: calc(100vh - 220px);"
                @drop="onDrop('Done')"
                @dragover.prevent
                @dragenter.prevent
              >
                <div
                  v-for="task in getTasksByStatus('Done')"
                  :key="task.id"
                  class="group bg-white rounded-lg border border-green-200 p-3 lg:p-4 hover:shadow-md transition-all duration-200 cursor-move opacity-75"
                  draggable="true"
                  @dragstart="onDragStart(task)"
                  @click="openTaskDetail(task)"
                >
                  <TaskCard :task="task" />
                </div>

                <div v-if="getTasksByStatus('Done').length === 0" class="text-center py-12 text-green-300 text-sm">
                  <p class="mt-2">No completed tasks</p>
                </div>
              </div>
            </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { listenSprints, listenTasks, updateSprintStatus, updateTaskStatus } from "../../firebase/WorkService";
import SideBar from "../SideBar.vue";
import TaskCard from "./TaskCard.vue";

export default {
  components: { SideBar, TaskCard },
  props: {
    sprintId: String,
    teamId: String
  },
  data() {
    return {
      sprint: {},
      sprints: [],
      tasks: [],
      draggedTask: null,
      isMobile: false,
      activeStatus: 'To Do',
      statuses: ['To Do','In Progress','Done']
    };
  },
  computed: {
    sprintTasks() {
      return this.tasks.filter(task => task.sprintId === this.sprintId);
    }
  },
  methods: {
    formatDate(dateString) {
      if (!dateString) return '';
      return new Date(dateString).toLocaleDateString('en-US');
    },

    getSprintStatusText(status) {
      const statusMap = {
        'backlog': 'Sprint Backlog',
        'open': 'Active Sprint',
        'done': 'Completed Sprint'
      };
      return statusMap[status] || status;
    },

    getTasksByStatus(status) {
      return this.sprintTasks.filter(task => task.status === status);
    },

    async startSprint() {
      await updateSprintStatus(this.teamId, this.sprintId, 'open');
    },

    async completeSprint() {
      await updateSprintStatus(this.teamId, this.sprintId, 'done');
    },

    onDragStart(task) {
      if (this.isMobile) return; // no drag on mobile
      this.draggedTask = task;
    },

    async onDrop(newStatus) {
      if (this.isMobile) return; // no drag on mobile
      if (this.draggedTask && this.draggedTask.status !== newStatus) {
        await updateTaskStatus(this.teamId, this.draggedTask.id, newStatus);
      }
      this.draggedTask = null;
    },

    async handleMobileStatusChange(task, status) {
      if (task.status !== status) {
        await updateTaskStatus(this.teamId, task.id, status);
      }
    },

    openTaskDetail(task) {
      this.$router.push({
        name: 'TaskDetail',
        params: {
          taskId: task.customId || task.id
        }
      });
    },

    handleResize() {
      this.isMobile = window.innerWidth < 768; // md breakpoint
    }
  },
  mounted() {
    this.handleResize();
    window.addEventListener('resize', this.handleResize);

    listenSprints(this.teamId, (sprints) => {
      this.sprints = sprints;
      this.sprint = sprints.find(s => s.id === this.sprintId) || {};
    });

    listenTasks(this.teamId, (tasks) => {
      this.tasks = tasks;
    });
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
  }
};
</script>

<style scoped>
.drag-over {
  @apply ring-2 ring-blue-400 ring-opacity-50 bg-blue-50;
}

.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
