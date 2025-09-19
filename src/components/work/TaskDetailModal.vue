<template>
  <div class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50" @click="$emit('close')">
    <div class="relative top-20 mx-auto p-5 border w-11/12 max-w-4xl shadow-lg rounded-md bg-white" @click.stop>
      <!-- Header -->
      <div class="flex items-center justify-between border-b border-gray-200 pb-4 mb-6">
        <div class="flex items-center space-x-3">
          <div class="w-8 h-8 rounded bg-blue-100 flex items-center justify-center">
            <svg class="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0 100-2H7zm6 7a1 1 0 011 1v3a1 1 0 11-2 0v-3a1 1 0 011-1zm-3 3a1 1 0 100 2h.01a1 1 0 100-2H10zm-4 1a1 1 0 011-1h.01a1 1 0 110 2H7a1 1 0 01-1-1zm1-4a1 1 0 100 2h.01a1 1 0 100-2H7zm2 1a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1zm4-4a1 1 0 100 2h.01a1 1 0 100-2H13z" clip-rule="evenodd"/>
            </svg>
          </div>
          <div>
            <h3 class="text-lg font-medium text-gray-900">{{ task.title }}</h3>
            <span class="text-sm font-mono text-gray-500">{{ task.id }}</span>
          </div>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Main Content -->
        <div class="lg:col-span-2 space-y-6">
          <!-- Description -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">Description</label>
            <textarea
              v-model="editableTask.description"
              rows="4"
              class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Add a description..."
            ></textarea>
          </div>

          <!-- Comments Section -->
          <div>
            <div class="flex items-center justify-between mb-4">
              <h4 class="text-sm font-medium text-gray-900">Comments</h4>
              <span class="text-xs text-gray-500">{{ task.comments?.length || 0 }} comments</span>
            </div>

            <!-- Add Comment -->
            <div class="mb-4">
              <div class="flex space-x-3">
                <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-xs font-medium">
                  {{ getCurrentUserInitials() }}
                </div>
                <div class="flex-1">
                  <textarea
                    v-model="newComment"
                    rows="2"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-sm"
                    placeholder="Add a comment..."
                  ></textarea>
                  <div class="mt-2 flex justify-end">
                    <button
                      @click="addComment"
                      :disabled="!newComment.trim()"
                      class="px-3 py-1 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Comment
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Comments List -->
            <div class="space-y-3 max-h-60 overflow-y-auto">
              <div
                v-for="comment in task.comments || []"
                :key="comment.id || Math.random()"
                class="flex space-x-3 p-3 bg-gray-50 rounded-lg"
              >
                <div class="w-8 h-8 rounded-full bg-gradient-to-br from-gray-400 to-gray-600 flex items-center justify-center text-white text-xs font-medium">
                  {{ getInitials(comment.author) }}
                </div>
                <div class="flex-1">
                  <div class="flex items-center space-x-2 mb-1">
                    <span class="text-sm font-medium text-gray-900">{{ comment.author }}</span>
                    <span class="text-xs text-gray-500">{{ formatDate(comment.timestamp) }}</span>
                  </div>
                  <p class="text-sm text-gray-700">{{ comment.text }}</p>
                </div>
              </div>

              <div v-if="!task.comments?.length" class="text-center py-4 text-gray-500">
                <svg class="mx-auto h-8 w-8 text-gray-300 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
                </svg>
                <p class="text-sm">No comments yet</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Sidebar -->
        <div class="space-y-6">
          <!-- Status -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">Status</label>
            <select
              v-model="editableTask.status"
              class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="To Do">To Do</option>
              <option value="In Progress">In Progress</option>
              <option value="Done">Done</option>
            </select>
          </div>

          <!-- Assignee -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">Assignee</label>
            <input
              v-model="editableTask.assignee"
              type="text"
              class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Assign to..."
            />
          </div>

          <!-- Priority -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">Priority</label>
            <select
              v-model="editableTask.priority"
              class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="Low">Low</option>
              <option value="Medium">Medium</option>
              <option value="High">High</option>
              <option value="Critical">Critical</option>
            </select>
          </div>

          <!-- Story Points -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">Story Points</label>
            <input
              v-model.number="editableTask.storyPoints"
              type="number"
              min="0"
              max="100"
              class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="0"
            />
          </div>

          <!-- Labels -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">Labels</label>
            <div class="space-y-2">
              <div class="flex flex-wrap gap-1">
                <span
                  v-for="(label, index) in editableTask.labels || []"
                  :key="index"
                  class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-indigo-100 text-indigo-800"
                >
                  {{ label }}
                  <button @click="removeLabel(index)" class="ml-1 text-indigo-600 hover:text-indigo-800">
                    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                    </svg>
                  </button>
                </span>
              </div>
              <div class="flex space-x-2">
                <input
                  v-model="newLabel"
                  @keyup.enter="addLabel"
                  type="text"
                  class="flex-1 px-2 py-1 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  placeholder="Add label..."
                />
                <button
                  @click="addLabel"
                  :disabled="!newLabel.trim()"
                  class="px-2 py-1 bg-indigo-600 text-white text-sm rounded-md hover:bg-indigo-700 disabled:opacity-50"
                >
                  Add
                </button>
              </div>
            </div>
          </div>

          <!-- Created/Updated Info -->
          <div class="pt-4 border-t border-gray-200">
            <div class="space-y-2 text-sm text-gray-500">
              <div>Created: {{ formatDate(task.createdAt) }}</div>
              <div v-if="task.updatedAt">Updated: {{ formatDate(task.updatedAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="flex justify-end space-x-3 mt-6 pt-4 border-t border-gray-200">
        <button
          @click="$emit('close')"
          class="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          Cancel
        </button>
        <button
          @click="saveTask"
          class="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
        >
          Save Changes
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { updateTask } from "../../firebase/WorkService";

export default {
  name: 'TaskDetailModal',
  props: {
    task: {
      type: Object,
      required: true
    },
    teamId: {
      type: String,
      required: true
    }
  },
  emits: ['close', 'update'],
  data() {
    return {
      editableTask: { ...this.task },
      newComment: '',
      newLabel: ''
    };
  },
  methods: {
    async saveTask() {
      try {
        // Güncelleme zamanını ekle
        this.editableTask.updatedAt = new Date().toISOString();

        await updateTask(this.teamId, this.editableTask.id, this.editableTask);
        this.$emit('update', this.editableTask);
      } catch (error) {
        console.error('Error updating task:', error);
      }
    },

    async addComment() {
      if (!this.newComment.trim()) return;

      const comment = {
        id: Date.now().toString(),
        text: this.newComment.trim(),
        author: this.getCurrentUser(),
        timestamp: new Date().toISOString()
      };

      if (!this.editableTask.comments) {
        this.editableTask.comments = [];
      }

      this.editableTask.comments.push(comment);
      this.newComment = '';
    },

    addLabel() {
      if (!this.newLabel.trim()) return;

      if (!this.editableTask.labels) {
        this.editableTask.labels = [];
      }

      if (!this.editableTask.labels.includes(this.newLabel.trim())) {
        this.editableTask.labels.push(this.newLabel.trim());
      }

      this.newLabel = '';
    },

    removeLabel(index) {
      if (this.editableTask.labels) {
        this.editableTask.labels.splice(index, 1);
      }
    },

    formatDate(dateString) {
      if (!dateString) return '';
      return new Date(dateString).toLocaleString('tr-TR');
    },

    getInitials(name) {
      if (!name) return '';
      return name.split(' ').map(word => word[0]).join('').toUpperCase().slice(0, 2);
    },

    getCurrentUser() {
      // Bu gerçek uygulamada authentication service'ten alınmalı
      return 'Current User';
    },

    getCurrentUserInitials() {
      return this.getInitials(this.getCurrentUser());
    }
  }
};
</script>
