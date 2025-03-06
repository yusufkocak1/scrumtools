<template>
  <div class="container mx-auto">
    <!-- Menü -->
    <div class="flex justify-between mb-4">
      <button class="px-4 py-2 bg-blue-500 text-white rounded-md" @click="showCreateTask = !showCreateTask">
        Create Issue
      </button>

    </div>

    <!-- Yeni Task Oluşturma Formu -->
    <AddTaskForm :is-open="showCreateTask" @close="showCreateTask = false" @addTask="createTask"></AddTaskForm>

    <!-- Yeni Sprint Oluşturma Formu -->
    <div v-if="showCreateSprint" class="mb-4">
      <input v-model="newSprintName" class="p-2 border rounded-md" placeholder="Sprint Adı"/>
      <button class="mt-2 px-4 py-2 bg-green-500 text-white rounded-md" @click="createSprint">
        Add Sprint
      </button>
    </div>

    <!-- Backlog Görevleri -->
    <div class="mb-6"
    >
      <div>
        <div class="w-full border-2 rounded-t-2xl p-4 flex justify-between"
        >
          <h3 class="text-2xl   text-center font-semibold  text-slate-800">Backlog</h3>
          <button class="px-4 py-2 bg-green-500 text-white rounded-md" @click="showCreateSprint = !showCreateSprint">
            Create Sprint
          </button>
        </div>
        <div
            class="relative flex flex-col w-full h-full overflow-scroll text-gray-700 bg-white shadow-md rounded-lg bg-clip-border">
          <table class="w-full text-left table-auto min-w-max"
                 @drop="onDrop(null)"
                 @dragover.prevent
          >

            <thead>
            <tr>
              <th class="p-4 border-b border-slate-300 bg-slate-50">
                <p class="block text-sm font-normal leading-none text-slate-500">
                  Id
                </p>
              </th>
              <th class="p-4 border-b border-slate-300 bg-slate-50">
                <p class="block text-sm font-normal leading-none text-slate-500">
                  Summary
                </p>
              </th>
              <th class="p-4 border-b border-slate-300 bg-slate-50">
                <p class="block text-sm font-normal leading-none text-slate-500">
                  Assignee
                </p>
              </th>
              <th class="p-4 border-b border-slate-300 bg-slate-50">
                <p class="block text-sm font-normal leading-none text-slate-500">
                  Status
                </p>
              </th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="task in tasks.filter(item => !item.sprintId)"
                :key="task.id"
                class="hover:bg-slate-50"
                draggable="true"
                @dragstart="onDragStart(task)"
            >
              <td class="p-4 border-b border-slate-200">
                <p class="block text-sm text-slate-800">
                  {{ task.id }}
                </p>
              </td>
              <td class="p-4 border-b border-slate-200">
                <p class="block text-sm text-slate-800">
                  {{ task.title }}
                </p>
              </td>
              <td class="p-4 border-b border-slate-200">
                <p class="block text-sm text-slate-800">
                  {{ task.assignee }}
                </p>
              </td>
              <td class="p-4 border-b border-slate-200">
                <p class="block text-sm text-slate-800">
                  {{ task.status }}
                </p>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

      </div>
    </div>

    <!-- Sprint Görevleri -->


    <div v-for="sprint in sprints.filter(status => status.status !== 'done')"
         :key="sprint.id"
         class="mb-6"
    >
      <div>
        <div class="w-full border-2 rounded-t-2xl p-4 flex justify-between"
        >
          <h3 class="text-2xl font-semibold  text-slate-800 hover:bg-blue-600 hover:text-white rounded-md p-1 flex items-center justify-center" @click="showSprintDetails(sprint)">{{ sprint.name }}</h3>
          <button v-if="sprint.status !== 'open'" class="mt-2 px-4 py-2 bg-green-500 text-white rounded-md" @click="startSprint(sprint.id)">
           Start Sprint
          </button>
          <button v-else class="mt-2 px-4 py-2 bg-red-800 text-white rounded-md" @click="finishSprint(sprint.id)">Finish Sprint</button>
        </div>
        <div
            class="relative flex flex-col w-full h-full overflow-scroll text-gray-700 bg-white shadow-md rounded-lg bg-clip-border">
          <table class="w-full text-left table-auto min-w-max"
                 @drop="onDrop(sprint.id)"
                 @dragover.prevent
          >
            <thead>
            <tr>
              <th class="p-4 border-b border-slate-300 bg-slate-50">
                <p class="block text-sm font-normal leading-none text-slate-500">
                  Id
                </p>
              </th>
              <th class="p-4 border-b border-slate-300 bg-slate-50">
                <p class="block text-sm font-normal leading-none text-slate-500">
                  Summary
                </p>
              </th>
              <th class="p-4 border-b border-slate-300 bg-slate-50">
                <p class="block text-sm font-normal leading-none text-slate-500">
                  Assignee
                </p>
              </th>
              <th class="p-4 border-b border-slate-300 bg-slate-50">
                <p class="block text-sm font-normal leading-none text-slate-500">
                  Status
                </p>
              </th>
            </tr>
            </thead>
            <tbody>
            <tr
                v-for="task in tasks.filter(item => item.sprintId === sprint.id)"
                :key="task.id"
                class="hover:bg-slate-50"
                draggable="true"
                @dragstart="onDragStart(task)"
            >
              <td class="p-4 border-b border-slate-200">
                <p class="block text-sm text-slate-800">
                  {{ task.id }}
                </p>
              </td>
              <td class="p-4 border-b border-slate-200">
                <p class="block text-sm text-slate-800">
                  {{ task.title }}
                </p>
              </td>
              <td class="p-4 border-b border-slate-200">
                <p class="block text-sm text-slate-800">
                  {{ task.assignee }}
                </p>
              </td>
              <td class="p-4 border-b border-slate-200">
                <p class="block text-sm text-slate-800">
                  {{ task.status }}
                </p>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

      </div>
    </div>

  </div>
</template>

<script>
import {
  addSprint,
  addTask,
  addTaskToSprint,
  listenSprints,
  listenTasks,
  updateSprintStatus
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
    };
  },
  methods: {
    async createTask(newTask) {


      await addTask(this.teamId, newTask);
      this.newTaskTitle = "";
      this.newTaskDescription = "";
      this.showCreateTask = false;
    },

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
      if (sprint.status !== "open") return;
      this.$router.push(`/team/${this.teamId}/sprint/${sprint.sprintId}`);
    },

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
/* Basit stil ayarları */
.container {
  padding: 16px;
}
</style>
