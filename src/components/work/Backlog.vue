<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Header -->
    <div class="bg-white border-b border-gray-200 px-6 py-4">
      <div class="flex justify-between items-center">
        <div class="flex items-center space-x-4">
          <h1 class="text-2xl font-semibold text-gray-900">Backlog</h1>
          <div class="flex items-center space-x-2 text-sm text-gray-500">
            <span>{{ tasks.length }} issues</span>
            <span>•</span>
            <span>{{ sprints.filter(s => s.status !== 'done').length }} sprints</span>
          </div>
        </div>
        <div class="flex items-center space-x-3">
          <button
            class="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            @click="showCreateSprint = !showCreateSprint"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
            </svg>
            Create Sprint
          </button>
          <button
            class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            @click="showCreateTask = !showCreateTask"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
            </svg>
            Create Issue
          </button>
        </div>
      </div>
    </div>

    <div class="px-6 py-6">
      <!-- Task Oluşturma/Düzenleme Formu -->
      <AddTaskForm
        :is-open="showCreateTask || !!editingTask"
        :task="editingTask"
        :teamId="teamId"
        @close="closeTaskForm"
        @updateTask="handleUpdateTask"
        @deleteTask="handleDeleteTask"
      ></AddTaskForm>

      <!-- Sprint Oluşturma Formu -->
      <div v-if="showCreateSprint" class="mb-6 bg-white rounded-lg shadow-sm border border-gray-200 p-4">
        <div class="flex items-center space-x-4">
          <input
            v-model="newSprintName"
            class="flex-1 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            placeholder="Sprint name"
          />
          <button
            class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            @click="createSprint"
          >
            Create Sprint
          </button>
          <button
            class="px-4 py-2 text-gray-600 hover:text-gray-800"
            @click="showCreateSprint = false"
          >
            Cancel
          </button>
        </div>
      </div>

      <!-- Backlog Section -->
      <div class="mb-8">
        <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <!-- Backlog Header -->
          <div class="bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <div class="flex items-center space-x-3">
              <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
              </svg>
              <h3 class="text-lg font-medium text-gray-900">Backlog</h3>
              <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                {{ tasks.filter(item => !item.sprintId).length }} issues
              </span>
            </div>
          </div>

          <!-- Backlog Items -->
          <div
            class="min-h-[200px] p-4 space-y-2"
            @drop="onDrop(null)"
            @dragover.prevent
            @dragenter.prevent
          >
            <div
              v-for="task in tasks.filter(item => !item.sprintId)"
              :key="task.id"
              class="group bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-all duration-200 cursor-pointer"
              draggable="true"
              @dragstart="onDragStart(task)"
              @click="openTaskDetail(task)"
            >
              <div class="flex items-start space-x-3">
                <div class="flex-shrink-0">
                  <div class="w-6 h-6 rounded bg-blue-100 flex items-center justify-center">
                    <svg class="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0 100-2H7zm6 7a1 1 0 011 1v3a1 1 0 11-2 0v-3a1 1 0 011-1zm-3 3a1 1 0 100 2h.01a1 1 0 100-2H10zm-4 1a1 1 0 011-1h.01a1 1 0 110 2H7a1 1 0 01-1-1zm1-4a1 1 0 100 2h.01a1 1 0 100-2H7zm2 1a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1zm4-4a1 1 0 100 2h.01a1 1 0 100-2H13z" clip-rule="evenodd"/>
                    </svg>
                  </div>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center space-x-2">
                    <span class="text-xs font-mono text-gray-500">{{ task.customId || task.id }}</span>
                    <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                      {{ task.status }}
                    </span>
                  </div>
                  <p class="mt-1 text-sm font-medium text-gray-900">{{ task.title }}</p>
                  <div class="mt-2 flex items-center space-x-4 text-xs text-gray-500">
                    <span v-if="task.assignee" class="flex items-center space-x-1">
                      <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/>
                      </svg>
                      <span>{{ task.assignee }}</span>
                    </span>
                  </div>
                </div>
                <div class="opacity-0 group-hover:opacity-100 transition-opacity">
                  <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z"/>
                  </svg>
                </div>
              </div>
            </div>

            <div v-if="tasks.filter(item => !item.sprintId).length === 0" class="text-center py-12 text-gray-500">
              <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 48 48">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M34 40h10v-4a6 6 0 00-10.712-3.714M34 40H14m20 0v-4a9 971 0 00-.712-3.714M14 40H4v-4a6 6 0 0110.713-3.714M14 40v-4c0-1.313.253-2.566.713-3.714m0 0A9.971 9.971 0 0124 24c4.21 0 7.863 2.613 9.288 6.286"/>
              </svg>
              <p class="mt-2 text-sm">No issues in backlog</p>
              <p class="text-xs text-gray-400">Create your first issue to get started</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Sprint Sections -->
      <div
        v-for="sprint in sprints.filter(status => status.status !== 'done')"
        :key="sprint.id"
        class="mb-8"
      >
        <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <!-- Sprint Header -->
          <div
            class="px-6 py-4 border-b border-gray-200 flex items-center justify-between cursor-pointer hover:bg-gray-50 transition-colors"
            :class="{ 'bg-blue-50 border-blue-200': sprint.status === 'open' }"
            @click="showSprintDetails(sprint)"
          >
            <div class="flex items-center space-x-3" @click="showSprintDetails(sprint)">
              <div class="flex-shrink-0">
                <div
                  class="w-8 h-8 rounded-full flex items-center justify-center"
                  :class="sprint.status === 'open' ? 'bg-green-100' : 'bg-gray-100'"
                >
                  <svg
                    class="w-4 h-4"
                    :class="sprint.status === 'open' ? 'text-green-600' : 'text-gray-500'"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                  </svg>
                </div>
              </div>
              <div>
                <h3 class="text-lg font-medium text-gray-900 hover:text-blue-600 transition-colors">{{ sprint.name }}</h3>
                <div class="flex items-center space-x-4 text-sm text-gray-500">
                  <span class="inline-flex items-center">
                    <span
                      class="w-2 h-2 rounded-full mr-2"
                      :class="sprint.status === 'open' ? 'bg-green-400' : 'bg-gray-400'"
                    ></span>
                    {{ sprint.status === 'open' ? 'Active Sprint' : 'Sprint' }}
                  </span>
                  <span>{{ tasks.filter(item => item.sprintId === sprint.id).length }} issues</span>
                </div>
              </div>
            </div>
            <div class="flex items-center space-x-2">
              <button
                v-if="sprint.status !== 'open'"
                class="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                @click.stop="startSprint(sprint.id)"
              >
                <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1m4 0h1m-6 4h.01M12 3v1m0 16v1m9-9h-1M3 12h1m15.364-6.364l-.707.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707"/>
                </svg>
                Start Sprint
              </button>
              <button
                v-else
                class="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                @click.stop="finishSprint(sprint.id)"
              >
                <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                Finish Sprint
              </button>
            </div>
          </div>

          <!-- Sprint Items -->
          <div
            class="min-h-[150px] p-4 space-y-2"
            @drop="onDrop(sprint.id)"
            @dragover.prevent
            @dragenter.prevent
          >
            <div
              v-for="task in tasks.filter(item => item.sprintId === sprint.id)"
              :key="task.id"
              class="group bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-all duration-200 cursor-pointer"
              draggable="true"
              @dragstart="onDragStart(task)"
              @click="openTaskDetail(task)"
            >
              <div class="flex items-start space-x-3">
                <div class="flex-shrink-0">
                  <div class="w-6 h-6 rounded bg-blue-100 flex items-center justify-center">
                    <svg class="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0 100-2H7zm6 7a1 1 0 011 1v3a1 1 0 11-2 0v-3a1 1 0 011-1zm-3 3a1 1 0 100 2h.01a1 1 0 100-2H10zm-4 1a1 1 0 011-1h.01a1 1 0 110 2H7a1 1 0 01-1-1zm1-4a1 1 0 100 2h.01a1 1 0 100-2H7zm2 1a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1zm4-4a1 1 0 100 2h.01a1 1 0 100-2H13z" clip-rule="evenodd"/>
                    </svg>
                  </div>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center space-x-2">
                    <span class="text-xs font-mono text-gray-500">{{ task.customId || task.id }}</span>
                    <span
                      class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
                      :class="{
                        'bg-yellow-100 text-yellow-800': task.status === 'To Do',
                        'bg-blue-100 text-blue-800': task.status === 'In Progress',
                        'bg-green-100 text-green-800': task.status === 'Done',
                        'bg-gray-100 text-gray-800': !['To Do', 'In Progress', 'Done'].includes(task.status)
                      }"
                    >
                      {{ task.status }}
                    </span>
                  </div>
                  <p class="mt-1 text-sm font-medium text-gray-900">{{ task.title }}</p>
                  <div class="mt-2 flex items-center space-x-4 text-xs text-gray-500">
                    <span v-if="task.assignee" class="flex items-center space-x-1">
                      <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/>
                      </svg>
                      <span>{{ task.assignee }}</span>
                    </span>
                  </div>
                </div>
                <div class="opacity-0 group-hover:opacity-100 transition-opacity">
                  <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z"/>
                  </svg>
                </div>
              </div>
            </div>

            <div v-if="tasks.filter(item => item.sprintId === sprint.id).length === 0" class="text-center py-8 text-gray-500">
              <p class="text-sm">No issues planned for this sprint</p>
              <p class="text-xs text-gray-400">Drag issues from the backlog</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Task Detail Modal'ı kaldır -->
    <!-- TaskDetailModal artık kullanılmıyor -->
  </div>
</template>

<script>
import {
  addSprint,
  addTaskToSprint,
  listenSprints,
  listenTasks,
  updateSprintStatus,
  updateTask as updateTaskService,
  deleteTask as deleteTaskService
} from "../../firebase/WorkService";
import AddTaskForm from "./AddTaskForm.vue";

export default {
  name: "Backlog",
  components: {
    AddTaskForm
  },
  props: {
    teamId: String
  },
  data() {
    return {
      tasks: [],
      sprints: [],
      showCreateTask: false,
      showCreateSprint: false,
      newTaskTitle: "",
      newTaskDescription: "",
      newSprintName: "",
      editingTask: null,
    };
  },
  methods: {
    async createSprint() {
      const newSprint = {
        name: this.newSprintName,
        startDate: "",
        endDate: "",
        teamId: this.teamId,
        status: "backlog",
        tasks: [],
      };
      await addSprint(this.teamId, newSprint);
      this.newSprintName = "";
      this.showCreateSprint = false;
    },

    async onDragStart(task) {
      this.draggedTask = task;
      console.log("test")
    },

    async onDrop(sprintId) {
      console.log("drop", this.draggedTask.id)
      await addTaskToSprint(this.teamId, this.draggedTask.id, sprintId);
    },

    startSprint(sprintId) {
      updateSprintStatus(this.teamId,sprintId, "open");
    },

    finishSprint(sprintId) {
      updateSprintStatus(this.teamId,sprintId,  "done");
    },

    showSprintDetails(sprint) {
      this.$router.push(`/team/${this.teamId}/sprint/${sprint.id}`);
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

    closeTaskForm() {
      this.showCreateTask = false;
      this.editingTask = null;
    },

    editTask(task) {
      this.editingTask = { ...task };
      this.showCreateTask = true;
    },

    async handleUpdateTask(updatedTask) {
      await updateTaskService(this.teamId, updatedTask.id, { ...updatedTask, updatedAt: new Date().toISOString() });
      this.closeTaskForm();
    },

    async handleDeleteTask(taskId) {
      await deleteTaskService(this.teamId, taskId);
      this.closeTaskForm();
    }
  },
  mounted() {
    listenTasks(this.teamId, (tasks) => {
      this.tasks = tasks;
    });

    listenSprints(this.teamId, (sprints) => {
      this.sprints = sprints;
    });
  },
};
</script>

<style scoped>
.drag-over {
  @apply bg-blue-50 border-blue-300;
}

.drag-target {
  @apply ring-2 ring-blue-300 ring-opacity-50;
}
</style>
