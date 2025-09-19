<template>
  <div class="flex flex-row w-screen">
    <SideBar :team-id="teamId" class="hidden lg:flex"></SideBar>
    <div class="flex flex-col w-screen">
      <!-- Header -->
      <div class="bg-white border-b border-gray-200 px-6 py-4">
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-4">
            <button
              @click="$router.go(-1)"
              class="inline-flex items-center px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
              </svg>
              Back
            </button>
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 rounded bg-blue-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0 100-2H7zm6 7a1 1 0 011 1v3a1 1 0 11-2 0v-3a1 1 0 011-1zm-3 3a1 1 0 100 2h.01a1 1 0 100-2H10zm-4 1a1 1 0 011-1h.01a1 1 0 110 2H7a1 1 0 01-1-1zm1-4a1 1 0 100 2h.01a1 1 0 100-2H7zm2 1a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1zm4-4a1 1 0 100 2h.01a1 1 0 100-2H13z" clip-rule="evenodd"/>
                </svg>
              </div>
              <div>
                <h1 class="text-xl font-semibold text-gray-900">{{ task?.title || 'Loading...' }}</h1>
                <span class="text-sm font-mono text-gray-500">{{ task?.customId || taskId }}</span>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex items-center space-x-3">
            <button
              @click="editTask"
              class="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
              Edit
            </button>
            <button
              @click="deleteTask"
              class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
              </svg>
              Delete
            </button>
          </div>
        </div>
      </div>

      <div v-if="loading" class="flex items-center justify-center min-h-[400px]">
        <div class="text-gray-500">Loading task details...</div>
      </div>

      <div v-else-if="!task" class="flex items-center justify-center min-h-[400px]">
        <div class="text-center">
          <h3 class="text-lg font-medium text-gray-900 mb-2">Task not found</h3>
          <p class="text-gray-500">The task you're looking for doesn't exist or has been deleted.</p>
        </div>
      </div>

      <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-6 p-6">
        <!-- Main Content -->
        <div class="lg:col-span-2 space-y-6">
          <!-- Description -->
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 class="text-lg font-medium text-gray-900 mb-4">Description</h2>
            <div v-if="task.description" class="prose prose-sm max-w-none">
              <p class="text-gray-700 whitespace-pre-wrap">{{ task.description }}</p>
            </div>
            <div v-else class="text-gray-500 italic">No description provided.</div>
          </div>

          <!-- Activity/Comments -->
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-lg font-medium text-gray-900">Activity</h2>
              <span class="text-sm text-gray-500">{{ (task.comments?.length || 0) }} comments</span>
            </div>

            <!-- Add Comment -->
            <div class="mb-6">
              <div class="flex space-x-4">
                <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-xs font-medium">
                  {{ getCurrentUserInitials() }}
                </div>
                <div class="flex-1">
                  <textarea
                    v-model="newComment"
                    rows="3"
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-sm"
                    placeholder="Add a comment..."
                  ></textarea>
                  <div class="mt-2 flex justify-end">
                    <button
                      @click="addComment"
                      :disabled="!newComment.trim()"
                      class="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Comment
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Comments List -->
            <div class="space-y-4">
              <div
                v-for="comment in task.comments || []"
                :key="comment.id || Math.random()"
                class="flex space-x-4"
              >
                <div class="w-8 h-8 rounded-full bg-gradient-to-br from-gray-400 to-gray-600 flex items-center justify-center text-white text-xs font-medium">
                  {{ getInitials(comment.author) }}
                </div>
                <div class="flex-1">
                  <div class="bg-gray-50 rounded-lg p-4">
                    <div class="flex items-center space-x-2 mb-2">
                      <span class="text-sm font-medium text-gray-900">{{ comment.author }}</span>
                      <span class="text-xs text-gray-500">{{ formatDate(comment.timestamp) }}</span>
                    </div>
                    <p class="text-sm text-gray-700">{{ comment.text }}</p>
                  </div>
                </div>
              </div>

              <div v-if="!task.comments?.length" class="text-center py-8 text-gray-500">
                <svg class="mx-auto h-12 w-12 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
                </svg>
                <p class="text-sm">No comments yet</p>
                <p class="text-xs text-gray-400">Be the first to comment</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Sidebar -->
        <div class="space-y-6">
          <!-- Status -->
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h3 class="text-sm font-medium text-gray-900 mb-4">Details</h3>

            <div class="space-y-4">
              <!-- Status -->
              <div>
                <label class="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Status</label>
                <select
                  v-model="task.status"
                  @change="updateTaskField('status', task.status)"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-sm"
                >
                  <option value="To Do">To Do</option>
                  <option value="In Progress">In Progress</option>
                  <option value="Done">Done</option>
                </select>
              </div>

              <!-- Issue Type -->
              <div>
                <label class="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Type</label>
                <div class="flex items-center space-x-2">
                  <div class="w-5 h-5 rounded bg-blue-100 flex items-center justify-center">
                    <svg class="w-3 h-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0 100-2H7zm6 7a1 1 0 011 1v3a1 1 0 11-2 0v-3a1 1 0 011-1zm-3 3a1 1 0 100 2h.01a1 1 0 100-2H10zm-4 1a1 1 0 011-1h.01a1 1 0 110 2H7a1 1 0 01-1-1zm1-4a1 1 0 100 2h.01a1 1 0 100-2H7zm2 1a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1zm4-4a1 1 0 100 2h.01a1 1 0 100-2H13z" clip-rule="evenodd"/>
                    </svg>
                  </div>
                  <span class="text-sm text-gray-900">{{ task.issueType || 'Task' }}</span>
                </div>
              </div>

              <!-- Priority -->
              <div>
                <label class="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Priority</label>
                <select
                  v-model="task.priority"
                  @change="updateTaskField('priority', task.priority)"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-sm"
                >
                  <option value="Low">🟢 Low</option>
                  <option value="Medium">🟡 Medium</option>
                  <option value="High">🟠 High</option>
                  <option value="Critical">🔴 Critical</option>
                </select>
              </div>

              <!-- Assignee -->
              <div>
                <label class="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Assignee</label>
                <div v-if="task.assignee" class="flex items-center space-x-2">
                  <div class="w-6 h-6 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-xs font-medium">
                    {{ getInitials(task.assignee) }}
                  </div>
                  <span class="text-sm text-gray-900">{{ task.assignee }}</span>
                </div>
                <div v-else class="text-sm text-gray-500">Unassigned</div>
              </div>

              <!-- Story Points -->
              <div v-if="task.storyPoints">
                <label class="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Story Points</label>
                <div class="text-sm text-gray-900">{{ task.storyPoints }}</div>
              </div>
            </div>
          </div>

          <!-- Team Assignment -->
          <div v-if="task.developer || task.analyst || task.tester" class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h3 class="text-sm font-medium text-gray-900 mb-4">Team Assignment</h3>

            <div class="space-y-3">
              <div v-if="task.developer">
                <label class="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Developer</label>
                <div class="flex items-center space-x-2">
                  <div class="w-5 h-5 rounded-full bg-green-100 flex items-center justify-center">
                    <span class="text-xs">👨‍💻</span>
                  </div>
                  <span class="text-sm text-gray-900">{{ task.developer }}</span>
                </div>
              </div>

              <div v-if="task.analyst">
                <label class="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Analyst</label>
                <div class="flex items-center space-x-2">
                  <div class="w-5 h-5 rounded-full bg-blue-100 flex items-center justify-center">
                    <span class="text-xs">📊</span>
                  </div>
                  <span class="text-sm text-gray-900">{{ task.analyst }}</span>
                </div>
              </div>

              <div v-if="task.tester">
                <label class="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Tester</label>
                <div class="flex items-center space-x-2">
                  <div class="w-5 h-5 rounded-full bg-purple-100 flex items-center justify-center">
                    <span class="text-xs">🧪</span>
                  </div>
                  <span class="text-sm text-gray-900">{{ task.tester }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Labels -->
          <div v-if="task.labels && task.labels.length" class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h3 class="text-sm font-medium text-gray-900 mb-4">Labels</h3>
            <div class="flex flex-wrap gap-2">
              <span
                v-for="label in task.labels"
                :key="label"
                class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-indigo-100 text-indigo-800"
              >
                {{ label }}
              </span>
            </div>
          </div>

          <!-- Timestamps -->
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h3 class="text-sm font-medium text-gray-900 mb-4">Timestamps</h3>
            <div class="space-y-3">
              <div>
                <label class="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Created</label>
                <div class="text-sm text-gray-900">{{ formatDate(task.createdAt) }}</div>
              </div>
              <div v-if="task.updatedAt">
                <label class="block text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Last Updated</label>
                <div class="text-sm text-gray-900">{{ formatDate(task.updatedAt) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Edit Task Form -->
      <AddTaskForm
        :is-open="showEditForm"
        :task="task"
        :teamId="teamId || ''"
        @close="showEditForm = false"
        @updateTask="onTaskUpdate"
      />
    </div>
  </div>
</template>

<script>
import { updateTask, deleteTask as deleteTaskService, findTaskInUserTeams } from '../firebase/WorkService';
import AddTaskForm from '../components/work/AddTaskForm.vue';
import SideBar from "../components/SideBar.vue";

export default {
  name: 'TaskDetailPage',
  components: {
    SideBar,
    AddTaskForm
  },
  props: {
    taskId: String
  },
  data() {
    return {
      task: null,
      teamId: null,
      teamData: null,
      loading: true,
      newComment: '',
      showEditForm: false
    };
  },
  async mounted() {
    await this.loadTask();
  },
  methods: {
    async loadTask() {
      try {
        this.loading = true;

        // CustomId ile kullanıcının takımları arasında task ara
        console.log('Searching for task with customId:', this.taskId);
        const result = await findTaskInUserTeams(this.taskId);

        if (result) {
          this.task = result.task;
          this.teamId = result.teamId;
          this.teamData = result.teamData;
          console.log('Task found:', this.task);
          console.log('Team found:', this.teamData);
        } else {
          console.error('Task not found with customId:', this.taskId);
          this.task = null;
        }
      } catch (error) {
        console.error('Error loading task:', error);
        this.task = null;
      } finally {
        this.loading = false;
      }
    },
    async editTask() {
      this.showEditForm = true;
    },
    async deleteTask() {
      if (confirm('Are you sure you want to delete this task?')) {
        try {
          await deleteTaskService(this.task.id);
          this.$router.push({ name: 'TeamPage', params: { teamId: this.teamId } });
        } catch (error) {
          console.error('Error deleting task:', error);
          alert('Failed to delete task. Please try again.');
        }
      }
    },
    async addComment() {
      if (!this.newComment.trim()) return;

      try {
        const commentData = {
          text: this.newComment.trim(),
          author: this.getCurrentUserName(),
          timestamp: new Date().toISOString()
        };

        this.task.comments = this.task.comments || [];
        this.task.comments.push(commentData);

        await updateTask(this.task.id, { comments: this.task.comments });

        this.newComment = '';
      } catch (error) {
        console.error('Error adding comment:', error);
        alert('Failed to add comment. Please try again.');
      }
    },
    updateTaskField(field, value) {
      if (this.task) {
        this.task[field] = value;
        updateTask(this.task.id, { [field]: value });
      }
    },
    onTaskUpdate(updatedTask) {
      this.task = { ...this.task, ...updatedTask };
    },
    getCurrentUserName() {
      // Replace with actual logic to get the current user's name
      return 'Current User';
    },
    getCurrentUserInitials() {
      // Replace with actual logic to get the current user's initials
      return 'CU';
    },
    getInitials(name) {
      if (!name) return '';
      const names = name.split(' ');
      return names.length > 1 ? names[0][0] + names[1][0] : names[0][0];
    },
    formatDate(dateString) {
      if (!dateString) return '';
      const options = { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' };
      return new Date(dateString).toLocaleString('en-US', options);
    }
  }
};
</script>

<style scoped>
/* Add any component-specific styles here */
</style>
