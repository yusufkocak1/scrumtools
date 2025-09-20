<template>
  <div class="flex flex-col lg:flex-row min-h-screen w-full bg-gray-50"> <!-- mobile layout container -->
    <SideBar :team-id="teamId" class="hidden lg:flex"></SideBar>
    <div class="flex flex-col flex-1 w-full min-w-0"> <!-- using w-full instead of w-screen -->
      <!-- Header -->
      <div class="bg-white border-b border-gray-200 px-4 sm:px-6 py-4 sticky top-0 z-10"> <!-- reduced padding on mobile & sticky -->
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4"> <!-- vertical on mobile -->
          <div class="flex flex-col sm:flex-row sm:items-center gap-4"> <!-- title + back button -->
            <div class="flex items-start sm:items-center space-x-4 flex-wrap"> <!-- wrapping support -->
              <button
                @click="$router.go(-1)"
                class="inline-flex items-center px-3 sm:px-4 py-2 border border-gray-300 rounded-md shadow-sm text-xs sm:text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
              >
                <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
                </svg>
                <span class="hidden xs:inline">Back</span>
                <span class="xs:hidden">Back</span>
              </button>
              <div class="flex items-center space-x-3 min-w-0"> <!-- min-w-0 prevents overflow -->
                <div class="w-8 h-8 rounded bg-blue-100 flex items-center justify-center flex-shrink-0">
                  <svg class="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0 100-2H7zm6 7a1 1 0 011 1v3a1 1 0 11-2 0v-3a1 1 0 011-1zm-3 3a1 1 0 100 2h.01a1 1 0 100-2H10zm-4 1a1 1 0 011-1h.01a1 1 0 110 2H7a1 1 0 01-1-1zm1-4a1 1 0 100 2h.01a1 1 0 100-2H7zm2 1a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1zm4-4a1 1 0 100 2h.01a1 1 0 100-2H13z" clip-rule="evenodd"/>
                  </svg>
                </div>
                <div class="min-w-0"> <!-- truncation support -->
                  <h1 class="text-lg sm:text-xl font-semibold text-gray-900 leading-tight truncate max-w-[240px] sm:max-w-none">{{ task?.title || 'Loading...' }}</h1>
                  <span class="text-[11px] sm:text-sm font-mono text-gray-500">{{ task?.customId || taskId }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex flex-wrap items-center gap-2"> <!-- wrapping actions -->
            <button
              @click="editTask"
              class="inline-flex items-center px-3 sm:px-4 py-2 border border-gray-300 rounded-md shadow-sm text-xs sm:text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
              <span class="hidden xs:inline">Edit</span>
              <span class="xs:hidden">Edit</span>
            </button>
            <button
              @click="cancelTask"
              class="inline-flex items-center px-3 sm:px-4 py-2 border border-transparent rounded-md shadow-sm text-xs sm:text-sm font-medium text-white bg-red-600 hover:bg-red-700"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
              </svg>
              <span class="hidden xs:inline">Cancel</span>
              <span class="xs:hidden">Cancel</span>
            </button>
          </div>
        </div>
      </div>

      <div v-if="loading" class="flex items-center justify-center min-h-[300px] sm:min-h-[400px] px-4">
        <div class="text-gray-500 text-sm sm:text-base">Loading task details...</div>
      </div>

      <div v-else-if="!task" class="flex items-center justify-center min-h-[300px] sm:min-h-[400px] px-4">
        <div class="text-center">
          <h3 class="text-lg font-medium text-gray-900 mb-2">Task not found</h3>
            <p class="text-gray-500 text-sm">The task you're looking for doesn't exist or has been deleted.</p>
        </div>
      </div>

      <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-4 sm:gap-6 px-4 sm:px-6 py-6">
        <!-- Main Content -->
        <div class="lg:col-span-2 space-y-4 sm:space-y-6">
          <!-- Description -->
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 sm:p-6 cursor-text" @click="startEditingDescription">
            <h2 class="text-base sm:text-lg font-medium text-gray-900 mb-3 sm:mb-4">Description</h2>
            <!-- Edit Mode -->
            <div v-if="editingDescription">
              <textarea
                ref="descriptionTextarea"
                v-model="tempDescription"
                rows="6"
                @blur="saveDescription"
                @keydown.esc.prevent="cancelDescriptionEdit"
                @keydown.ctrl.enter.prevent="forceSaveDescription"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-sm resize-y"
                placeholder="Add a description..."
              ></textarea>
              <div class="mt-2 flex flex-col sm:flex-row sm:items-center sm:justify-between text-[11px] sm:text-xs text-gray-500 select-none gap-1">
                <span>Esc: Cancel · Ctrl+Enter: Save</span>
                <div class="flex items-center space-x-3">
                  <span v-if="savingDescription" class="text-blue-500">Saving...</span>
                  <span v-else-if="descriptionSavedFlash" class="text-green-600">Saved</span>
                </div>
              </div>
            </div>
            <!-- View Mode -->
            <div v-else>
              <div v-if="task.description" class="prose prose-sm max-w-none">
                <p class="text-gray-700 whitespace-pre-wrap text-sm sm:text-base">{{ task.description }}</p>
              </div>
              <div v-else class="text-gray-500 italic text-sm">Click to add a description</div>
            </div>
          </div>

          <!-- Activity/Comments -->
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 sm:p-6">
            <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-4 sm:mb-6 gap-2">
              <h2 class="text-base sm:text-lg font-medium text-gray-900">Activity</h2>
              <span class="text-[11px] sm:text-sm text-gray-500">{{ (task.comments?.length || 0) }} comments</span>
            </div>

            <!-- Add Comment -->
            <div class="mb-4 sm:mb-6">
              <div class="flex items-start space-x-3 sm:space-x-4">
                <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-xs font-medium flex-shrink-0">
                  {{ getCurrentUserInitials() }}
                </div>
                <div class="flex-1 min-w-0">
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
                      class="px-3 sm:px-4 py-2 bg-blue-600 text-white text-xs sm:text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Comment
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Comments List -->
            <div class="space-y-3 sm:space-y-4">
              <div
                v-for="comment in task.comments || []"
                :key="comment.id || Math.random()"
                class="flex items-start space-x-3 sm:space-x-4"
              >
                <div class="w-8 h-8 rounded-full bg-gradient-to-br from-gray-400 to-gray-600 flex items-center justify-center text-white text-xs font-medium flex-shrink-0">
                  {{ getInitials(comment.author) }}
                </div>
                <div class="flex-1 min-w-0">
                  <div class="bg-gray-50 rounded-lg p-3 sm:p-4">
                    <div class="flex flex-wrap items-center gap-x-2 gap-y-1 mb-2">
                      <span class="text-sm font-medium text-gray-900">{{ comment.author }}</span>
                      <span class="text-[11px] sm:text-xs text-gray-500">{{ formatDate(comment.timestamp) }}</span>
                    </div>
                    <p class="text-sm text-gray-700 whitespace-pre-wrap">{{ comment.text }}</p>
                  </div>
                </div>
              </div>

              <div v-if="!task.comments?.length" class="text-center py-8 text-gray-500">
                <svg class="mx-auto h-10 w-10 sm:h-12 sm:w-12 text-gray-300 mb-3 sm:mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
                </svg>
                <p class="text-sm">No comments yet</p>
                <p class="text-[11px] sm:text-xs text-gray-400">Be the first to comment</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Sidebar -->
        <div class="space-y-4 sm:space-y-6">
          <!-- Status / Details -->
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 sm:p-6">
            <h3 class="text-sm font-medium text-gray-900 mb-4">Details</h3>

            <div class="space-y-4">
              <!-- Status -->
              <div>
                <label class="block text-[11px] sm:text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Status</label>
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
                <label class="block text-[11px] sm:text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Type</label>
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
                <label class="block text-[11px] sm:text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Priority</label>
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
                <label class="block text-[11px] sm:text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Assignee</label>
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
                <label class="block text-[11px] sm:text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Story Points</label>
                <div class="text-sm text-gray-900">{{ task.storyPoints }}</div>
              </div>
            </div>
          </div>

          <!-- Team Assignment -->
          <div v-if="task.developer || task.analyst || task.tester" class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 sm:p-6">
            <h3 class="text-sm font-medium text-gray-900 mb-4">Team Assignment</h3>

            <div class="space-y-3">
              <div v-if="task.developer">
                <label class="block text-[11px] sm:text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Developer</label>
                <div class="flex items-center space-x-2">
                  <div class="w-5 h-5 rounded-full bg-green-100 flex items-center justify-center">
                    <span class="text-xs">👨‍💻</span>
                  </div>
                  <span class="text-sm text-gray-900">{{ task.developer }}</span>
                </div>
              </div>

              <div v-if="task.analyst">
                <label class="block text-[11px] sm:text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Analyst</label>
                <div class="flex items-center space-x-2">
                  <div class="w-5 h-5 rounded-full bg-blue-100 flex items-center justify-center">
                    <span class="text-xs">📊</span>
                  </div>
                  <span class="text-sm text-gray-900">{{ task.analyst }}</span>
                </div>
              </div>

              <div v-if="task.tester">
                <label class="block text-[11px] sm:text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Tester</label>
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
          <div v-if="task.labels && task.labels.length" class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 sm:p-6">
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
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-4 sm:p-6">
            <h3 class="text-sm font-medium text-gray-900 mb-4">Timestamps</h3>
            <div class="space-y-3">
              <div>
                <label class="block text-[11px] sm:text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Created</label>
                <div class="text-sm text-gray-900">{{ formatDate(task.createdAt) }}</div>
              </div>
              <div v-if="task.updatedAt">
                <label class="block text-[11px] sm:text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Last Updated</label>
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
      showEditForm: false,
      // Inline description edit state
      editingDescription: false,
      tempDescription: '',
      savingDescription: false,
      descriptionSavedFlash: false
    };
  },
  async mounted() {
    await this.loadTask();
  },
  methods: {
    async loadTask() {
      try {
        this.loading = true;
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
    cancelTask(){
      this.updateTaskField('status', "Cancelled");
    },
    // Inline Description Editing
    startEditingDescription() {
      if (!this.task) return;
      if (this.editingDescription) return; // zaten edit modunda
      this.editingDescription = true;
      this.tempDescription = this.task.description || '';
      this.$nextTick(() => {
        if (this.$refs.descriptionTextarea) {
          this.$refs.descriptionTextarea.focus();
        }
      });
    },
    async saveDescription() {
      if (!this.editingDescription) return;
      const newValue = this.tempDescription.trim();
      if (this.task && this.task.description !== newValue) {
        if(!this.teamId || !this.task?.id){
          console.error('Missing teamId or task.id for description update', this.teamId, this.task?.id);
          this.editingDescription = false;
          return;
        }
        this.savingDescription = true;
        const oldValue = this.task.description;
        this.task.description = newValue;
        try {
          await updateTask(this.teamId, this.task.id, { description: newValue, updatedAt: new Date().toISOString() });
          this.descriptionSavedFlash = true;
          setTimeout(() => { this.descriptionSavedFlash = false; }, 1500);
        } catch (e) {
          console.error('Error updating description', e);
          alert('Description could not be saved.');
          this.task.description = oldValue;
        } finally {
          this.savingDescription = false;
        }
      }
      this.editingDescription = false;
    },
    cancelDescriptionEdit() {
      this.editingDescription = false;
      this.tempDescription = '';
    },
    forceSaveDescription() {
      if (this.$refs.descriptionTextarea) {
        this.$refs.descriptionTextarea.blur();
      }
    },
    getMemberByEmail(email) {
      return this.teamData?.members[email] || null;
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
        if(!this.teamId || !this.task?.id){
          console.error('Missing teamId or task.id for adding comment', this.teamId, this.task?.id);
            return;
        }
        await updateTask(this.teamId, this.task.id, { comments: this.task.comments });
        this.newComment = '';
      } catch (error) {
        console.error('Error adding comment:', error);
        alert('Failed to add comment. Please try again.');
      }
    },
    updateTaskField(field, value) {
      if (this.task) {
        this.task[field] = value;
        if(!this.teamId || !this.task?.id){
          console.error('Missing teamId or task.id for updating field', this.teamId, this.task?.id);
          return;
        }
        updateTask(this.teamId, this.task.id, { [field]: value, updatedAt: new Date().toISOString() });
      }
    },
    onTaskUpdate(updatedTask) {
      this.task = { ...this.task, ...updatedTask };
    },
    getCurrentUserName() {
      return localStorage.getItem('user');
    },
    getCurrentUserInitials() {
      return this.getInitials(this.getCurrentUserName());
    },
    getInitials(email) {
      if (!email) return '';
      const member = this.getMemberByEmail(email);
      if(!member || !member.displayName) return '';
      const names = member.displayName.toUpperCase().split(' ');
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
