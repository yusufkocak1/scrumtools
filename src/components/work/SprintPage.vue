<template>
  <div class="flex flex-row w-full h-screen bg-gray-50">
    <SideBar :team-id="teamId" class="hidden lg:flex flex-shrink-0" />

    <div class="flex-1 flex flex-col min-w-0">
      <!-- Header -->
      <div class="bg-white border-b border-gray-200 px-6 py-4 flex-shrink-0">
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-4">
            <button
              @click="$router.go(-1)"
              class="inline-flex items-center px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
              </svg>
              Back to Backlog
            </button>
            <div>
              <h1 class="text-2xl font-semibold text-gray-900">{{ sprint.name || 'Sprint Board' }}</h1>
              <div v-if="sprint.name" class="flex items-center space-x-4 text-sm text-gray-500">
                <span
                  class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                  :class="{
                    'bg-green-100 text-green-800': sprint.status === 'open',
                    'bg-blue-100 text-blue-800': sprint.status === 'backlog',
                    'bg-gray-100 text-gray-800': sprint.status === 'done'
                  }"
                >
                  {{ getSprintStatusText(sprint.status) }}
                </span>
                <span v-if="sprint.startDate">{{ formatDate(sprint.startDate) }} - {{ formatDate(sprint.endDate) }}</span>
                <span>{{ sprintTasks.length }} issues</span>
              </div>
            </div>
          </div>

          <!-- Sprint Actions -->
          <div class="flex items-center space-x-3">
            <button
              v-if="sprint.status === 'backlog'"
              @click="startSprint"
              class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1m4 0h1m-6 4h.01M12 3v1m0 16v1m9-9h-1M3 12h1m15.364-6.364l-.707.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707"/>
              </svg>
              Start Sprint
            </button>

            <button
              v-if="sprint.status === 'open'"
              @click="completeSprint"
              class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              Complete Sprint
            </button>

            <button class="inline-flex items-center px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4"/>
              </svg>
              Settings
            </button>
          </div>
        </div>
      </div>

      <!-- Scrum Board -->
      <div class="flex-1 p-6 overflow-hidden">
        <div class="flex space-x-6 h-full w-full">
          <!-- To Do Column -->
          <div class="flex-1 min-w-0 bg-gray-50 rounded-lg">
            <div class="p-4 border-b border-gray-200 bg-white rounded-t-lg">
              <div class="flex items-center justify-between">
                <h3 class="font-medium text-gray-900 flex items-center">
                  <div class="w-3 h-3 bg-gray-400 rounded-full mr-3"></div>
                  TO DO
                </h3>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                  {{ getTasksByStatus('To Do').length }}
                </span>
              </div>
            </div>
            <div
              class="p-4 space-y-3 h-full overflow-y-auto"
              style="height: calc(100vh - 200px);"
              @drop="onDrop('To Do')"
              @dragover.prevent
              @dragenter.prevent
            >
              <div
                v-for="task in getTasksByStatus('To Do')"
                :key="task.id"
                class="group bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-all duration-200 cursor-move"
                draggable="true"
                @dragstart="onDragStart(task)"
                @click="openTaskDetail(task)"
              >
                <TaskCard :task="task" />
              </div>

              <div v-if="getTasksByStatus('To Do').length === 0" class="text-center py-12 text-gray-400">
                <svg class="mx-auto h-12 w-12 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 48 48">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M20 12H8a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"/>
                </svg>
                <p class="mt-2 text-sm">No tasks</p>
              </div>
            </div>
          </div>

          <!-- In Progress Column -->
          <div class="flex-1 min-w-0 bg-blue-50 rounded-lg">
            <div class="p-4 border-b border-blue-200 bg-white rounded-t-lg">
              <div class="flex items-center justify-between">
                <h3 class="font-medium text-gray-900 flex items-center">
                  <div class="w-3 h-3 bg-blue-400 rounded-full mr-3"></div>
                  IN PROGRESS
                </h3>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                  {{ getTasksByStatus('In Progress').length }}
                </span>
              </div>
            </div>
            <div
              class="p-4 space-y-3 h-full overflow-y-auto"
              style="height: calc(100vh - 200px);"
              @drop="onDrop('In Progress')"
              @dragover.prevent
              @dragenter.prevent
            >
              <div
                v-for="task in getTasksByStatus('In Progress')"
                :key="task.id"
                class="group bg-white rounded-lg border border-blue-200 p-4 hover:shadow-md transition-all duration-200 cursor-move"
                draggable="true"
                @dragstart="onDragStart(task)"
                @click="openTaskDetail(task)"
              >
                <TaskCard :task="task" />
              </div>

              <div v-if="getTasksByStatus('In Progress').length === 0" class="text-center py-12 text-blue-300">
                <svg class="mx-auto h-12 w-12" fill="none" stroke="currentColor" viewBox="0 0 48 48">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M12 8v4l3 3 3-3V8M5 21l3 3 3-3M19 21l3 3 3-3"/>
                </svg>
                <p class="mt-2 text-sm">No tasks in progress</p>
              </div>
            </div>
          </div>

          <!-- Done Column -->
          <div class="flex-1 min-w-0 bg-green-50 rounded-lg">
            <div class="p-4 border-b border-green-200 bg-white rounded-t-lg">
              <div class="flex items-center justify-between">
                <h3 class="font-medium text-gray-900 flex items-center">
                  <div class="w-3 h-3 bg-green-400 rounded-full mr-3"></div>
                  DONE
                </h3>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                  {{ getTasksByStatus('Done').length }}
                </span>
              </div>
            </div>
            <div
              class="p-4 space-y-3 h-full overflow-y-auto"
              style="height: calc(100vh - 200px);"
              @drop="onDrop('Done')"
              @dragover.prevent
              @dragenter.prevent
            >
              <div
                v-for="task in getTasksByStatus('Done')"
                :key="task.id"
                class="group bg-white rounded-lg border border-green-200 p-4 hover:shadow-md transition-all duration-200 cursor-move opacity-75"
                draggable="true"
                @dragstart="onDragStart(task)"
                @click="openTaskDetail(task)"
              >
                <TaskCard :task="task" />
              </div>

              <div v-if="getTasksByStatus('Done').length === 0" class="text-center py-12 text-green-300">
                <svg class="mx-auto h-12 w-12" fill="none" stroke="currentColor" viewBox="0 0 48 48">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                <p class="mt-2 text-sm">No completed tasks</p>
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
      draggedTask: null
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
      return new Date(dateString).toLocaleDateString('tr-TR');
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
      this.draggedTask = task;
    },

    async onDrop(newStatus) {
      if (this.draggedTask && this.draggedTask.status !== newStatus) {
        await updateTaskStatus(this.teamId, this.draggedTask.id, newStatus);
      }
      this.draggedTask = null;
    },

    openTaskDetail(task) {
      // Sadece customId ile yönlendir (teamId olmadan)
      this.$router.push({
        name: 'TaskDetail',
        params: {
          taskId: task.customId || task.id
        }
      });
    },
  },
  mounted() {
    // Sprint bilgilerini dinle
    listenSprints(this.teamId, (sprints) => {
      this.sprints = sprints;
      this.sprint = sprints.find(s => s.id === this.sprintId) || {};
    });

    // Tüm görevleri dinle
    listenTasks(this.teamId, (tasks) => {
      this.tasks = tasks;
    });
  },
};
</script>

<style scoped>
.drag-over {
  @apply ring-2 ring-blue-400 ring-opacity-50 bg-blue-50;
}
</style>
