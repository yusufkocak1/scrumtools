<template>
  <!-- Modal Arka Planı -->
  <div v-if="isOpen" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[9999]" @click="closeModal">
    <!-- Modal İçeriği -->
    <div class="bg-white shadow-2xl rounded-xl p-0 w-full max-w-4xl h-[90vh] relative overflow-hidden" @click.stop>
      <!-- Header -->
      <div class="flex items-center justify-between p-6 border-b border-gray-200 bg-gray-50">
        <div class="flex items-center space-x-3">
          <div class="w-8 h-8 rounded bg-blue-100 flex items-center justify-center">
            <svg class="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M6 2a2 2 0 00-2 2v12a2 2 0 002 2h8a2 2 0 002-2V4a2 2 0 00-2-2H6zm1 2a1 1 0 000 2h6a1 1 0 100-2H7zm6 7a1 1 0 011 1v3a1 1 0 11-2 0v-3a1 1 0 011-1zm-3 3a1 1 0 100 2h.01a1 1 0 100-2H10zm-4 1a1 1 0 011-1h.01a1 1 0 110 2H7a1 1 0 01-1-1zm1-4a1 1 0 100 2h.01a1 1 0 100-2H7zm2 1a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1zm4-4a1 1 0 100 2h.01a1 1 0 100-2H13z" clip-rule="evenodd"/>
            </svg>
          </div>
          <div>
            <h2 class="text-xl font-semibold text-gray-900">{{ task ? 'Edit Issue' : 'Create New Issue' }}</h2>
            <p class="text-sm text-gray-500">{{ task ? `ID: ${task.id}` : 'Fill in the details below' }}</p>
          </div>
        </div>
        <button
          class="text-gray-400 hover:text-gray-600 p-2 hover:bg-gray-100 rounded-lg transition-colors"
          @click="closeModal"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <!-- Content -->
      <div class="flex h-full">
        <!-- Main Form -->
        <div class="flex-1 p-6 overflow-y-auto">
          <form class="space-y-6" @submit.prevent="submitTask">
            <!-- Issue Type & Status -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Issue Type</label>
                <div class="relative">
                  <select v-model="formData.issueType"
                          class="w-full px-4 py-3 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white">
                    <option v-for="type in issueTypes" :key="type.value" :value="type.value">
                      {{ type.label }}
                    </option>
                  </select>
                </div>
              </div>

              <div v-if="task">
                <label class="block text-sm font-medium text-gray-700 mb-2">Status</label>
                <select v-model="formData.status"
                        class="w-full px-4 py-3 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white">
                  <option value="To Do">To Do</option>
                  <option value="In Progress">In Progress</option>
                  <option value="Done">Done</option>
                </select>
              </div>
            </div>

            <!-- Title -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Title *</label>
              <input
                v-model="formData.title"
                class="w-full px-4 py-3 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="Enter issue title"
                required
                type="text"
              />
            </div>

            <!-- Description -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Description</label>
              <textarea
                v-model="formData.description"
                rows="6"
                class="w-full px-4 py-3 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none"
                placeholder="Describe the issue in detail..."
              ></textarea>
            </div>

            <!-- Priority & Story Points -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Priority</label>
                <select v-model="formData.priority"
                        class="w-full px-4 py-3 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white">
                  <option v-for="priority in priorities" :key="priority.value" :value="priority.value">
                    {{ priority.label }}
                  </option>
                </select>
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Story Points</label>
                <input
                  v-model.number="formData.storyPoints"
                  type="number"
                  min="0"
                  max="100"
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="0"
                />
              </div>
            </div>

            <!-- Team Members -->
            <div class="space-y-4">
              <h3 class="text-lg font-medium text-gray-900 border-b border-gray-200 pb-2">Team Assignment</h3>

              <!-- Assignee -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Assignee</label>
                <AutoCompleteInput
                  v-model="formData.assignee"
                  :options="teamMembers"
                  placeholder="Search and select assignee..."
                  :displayField="'displayName'"
                  :valueField="'email'"
                />
              </div>

              <!-- Developer -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Developer</label>
                <AutoCompleteInput
                  v-model="formData.developer"
                  :options="developers"
                  placeholder="Search and select developer..."
                  :displayField="'name'"
                  :valueField="'email'"
                />
              </div>

              <!-- Analyst -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Analyst</label>
                <AutoCompleteInput
                  v-model="formData.analyst"
                  :options="analysts"
                  placeholder="Search and select analyst..."
                  :displayField="'name'"
                  :valueField="'email'"
                />
              </div>

              <!-- Tester -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Tester</label>
                <AutoCompleteInput
                  v-model="formData.tester"
                  :options="testers"
                  placeholder="Search and select tester..."
                  :displayField="'name'"
                  :valueField="'email'"
                />
              </div>
            </div>

            <!-- Labels -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Labels</label>
              <div class="space-y-2">
                <div class="flex flex-wrap gap-2">
                  <span
                    v-for="(label, index) in formData.labels || []"
                    :key="index"
                    class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-indigo-100 text-indigo-800"
                  >
                    {{ label }}
                    <button
                      type="button"
                      @click="removeLabel(index)"
                      class="ml-2 text-indigo-600 hover:text-indigo-800"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
                    class="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    placeholder="Add label..."
                  />
                  <button
                    type="button"
                    @click="addLabel"
                    :disabled="!newLabel.trim()"
                    class="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Add
                  </button>
                </div>
              </div>
            </div>
          </form>
        </div>

        <!-- Sidebar -->
        <div class="w-80 bg-gray-50 border-l border-gray-200 p-6 overflow-y-auto">
          <h3 class="text-lg font-medium text-gray-900 mb-4">Issue Details</h3>

          <!-- Quick Stats -->
          <div class="space-y-4 mb-6">
            <div class="bg-white rounded-lg p-4 border border-gray-200">
              <div class="text-sm text-gray-500">Created</div>
              <div class="text-sm font-medium">{{ formatDate(task?.createdAt || new Date()) }}</div>
            </div>

            <div v-if="task?.updatedAt" class="bg-white rounded-lg p-4 border border-gray-200">
              <div class="text-sm text-gray-500">Last Updated</div>
              <div class="text-sm font-medium">{{ formatDate(task.updatedAt) }}</div>
            </div>
          </div>

          <!-- Actions -->
          <div class="space-y-3">
            <button
              type="submit"
              @click="submitTask"
              class="w-full bg-blue-600 text-white py-3 px-4 rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors font-medium"
            >
              {{ task ? 'Update Issue' : 'Create Issue' }}
            </button>

            <button
              type="button"
              @click="closeModal"
              class="w-full bg-gray-100 text-gray-700 py-3 px-4 rounded-lg hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 transition-colors font-medium"
            >
              Cancel
            </button>

            <button
              v-if="task"
              type="button"
              @click="deleteTask"
              class="w-full bg-red-100 text-red-700 py-3 px-4 rounded-lg hover:bg-red-200 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 transition-colors font-medium"
            >
              Delete Issue
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>var email;

import AutoCompleteInput from "./AutoCompleteInput.vue";
import {getTeamById} from "../../firebase/TeamService.js";

export default {
  name: 'TaskEditForm',
  components: {
    AutoCompleteInput
  },
  props: {
    isOpen: {
      type: Boolean,
      default: false
    },
    task: {
      type: Object,
      default: null
    },
    teamId: {
      type: String,
      required: true
    }
  },
  emits: ['close', 'addTask', 'updateTask', 'deleteTask'],
  data() {
    return {
      formData: {
        title: "",
        description: "",
        issueType: "task",
        status: "To Do",
        priority: "Medium",
        storyPoints: 0,
        assignee: "",
        developer: "",
        analyst: "",
        tester: "",
        labels: []
      },
      newLabel: "",
      teamMembers: [],
      issueTypes: [
        {value: "task", label: "🎯 Task"},
        {value: "story", label: "📖 Story"},
        {value: "bug", label: "🐛 Bug"},
        {value: "epic", label: "🚀 Epic"}
      ],
      priorities: [
        {value: "Low", label: "🟢 Low"},
        {value: "Medium", label: "🟡 Medium"},
        {value: "High", label: "🟠 High"},
        {value: "Critical", label: "🔴 Critical"}
      ]
    };
  },
  computed: {
    developers() {
      return this.teamMembers.filter(member => {
        // Admin her zaman dahil
        if (member.role === 'admin') return true;

        // Developer rolü olanlar
        if (member.role === 'developer') return true;

        // Development skill'i olanlar
        if (member.skills?.includes('Development')) return true;

        return false;
      }).map(member => ({
        ...member,
        name: member.displayName
      }));
    },
    analysts() {
      return this.teamMembers.filter(member => {
        // Admin her zaman dahil
        if (member.role === 'admin') return true;

        // Product Owner veya analyst rolü olanlar
        if (member.role === 'product_owner' || member.role === 'analyst') return true;

        // Analysis skill'i olanlar
        if (member.skills?.includes('Analysis')) return true;

        return false;
      }).map(member => ({
        ...member,
        name: member.displayName
      }));
    },
    testers() {
      return this.teamMembers.filter(member => {
        // Admin her zaman dahil
        if (member.role === 'admin') return true;

        // Tester rolü olanlar
        if (member.role === 'tester') return true;

        // Testing skill'i olanlar
        if (member.skills?.includes('Testing')) return true;

        return false;
      }).map(member => ({
        ...member,
        name: member.displayName
      }));
    }
  },
  watch: {
    task: {
      immediate: true,
      handler(newTask) {
        if (newTask) {
          this.formData = {...newTask};
        } else {
          this.resetForm();
        }
      }
    },
    isOpen: {
      immediate: true,
      handler(isOpen) {
        if (isOpen && this.teamId) {
          this.loadTeamMembers();
        }
      }
    }
  },
  methods: {
    async loadTeamMembers() {
      try {
        await getTeamById(this.teamId, (team) => {
          this.teamMembers = Object.entries(team.members || {}).map(([email, member]) => ({
            email,
            ...member,
            displayName: member.displayName
          }));
        });
      } catch (error) {
        console.error('Error loading team members:', error);
      }
    },

    submitTask() {
      try {
        const taskData = {
          ...this.formData,
          updatedAt: new Date().toISOString()
        };

        if (this.task) {
          // Edit mode
          this.$emit('updateTask', taskData);
        } else {
          // Create mode
          taskData.createdAt = new Date().toISOString();
          taskData.status = "To Do";
          this.$emit('addTask', taskData);
        }

        this.closeModal();
      } catch (error) {
        console.error('Error submitting task:', error);
      }
    },

    deleteTask() {
      if (confirm('Are you sure you want to delete this issue?')) {
        this.$emit('deleteTask', this.task.id);
        this.closeModal();
      }
    },

    addLabel() {
      if (!this.newLabel.trim()) return;

      if (!this.formData.labels) {
        this.formData.labels = [];
      }

      if (!this.formData.labels.includes(this.newLabel.trim())) {
        this.formData.labels.push(this.newLabel.trim());
      }

      this.newLabel = '';
    },

    removeLabel(index) {
      if (this.formData.labels) {
        this.formData.labels.splice(index, 1);
      }
    },

    resetForm() {
      this.formData = {
        title: "",
        description: "",
        issueType: "task",
        status: "To Do",
        priority: "Medium",
        storyPoints: 0,
        assignee: "",
        developer: "",
        analyst: "",
        tester: "",
        labels: []
      };
      this.newLabel = "";
    },

    closeModal() {
      this.resetForm();
      this.$emit('close');
    },

    formatDate(dateString) {
      if (!dateString) return '';
      return new Date(dateString).toLocaleString('tr-TR');
    }
  }
};
</script>

<style scoped>
/* Custom scrollbar */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}
</style>
