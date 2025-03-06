<template>
  <div class="max-w-4xl mx-auto bg-white shadow-md rounded-lg p-6 mt-6">
    <h2 class="text-xl font-bold mb-4">Görevler</h2>

    <!-- Filtreleme -->
    <div class="flex space-x-4 mb-4">
      <button
          v-for="filter in filters"
          :key="filter"
          @click="currentFilter = filter"
          :class="[
            'px-4 py-2 rounded-md',
            currentFilter === filter
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 hover:bg-gray-300'
          ]"
      >
        {{ filter }}
      </button>
    </div>

    <!-- Görev Listesi -->
    <div v-if="tasks.length" class="space-y-4">
      <div
          v-for="task in filteredTasks"
          :key="task.id"
          class="p-4 border rounded-md shadow-sm"
      >
        <h3 class="text-lg font-bold">{{ task.title }}</h3>
        <p class="text-gray-700">{{ task.description }}</p>
        <p class="text-sm text-gray-500">Durum: {{ task.status }}</p>

        <!-- Statü Güncelleme -->
        <div class="mt-2 flex space-x-2">
          <button
              v-if="task.status !== 'done'"
              @click="updateStatus(task.id, task.status)"
              class="px-3 py-1 text-white bg-blue-500 hover:bg-blue-600 rounded-md"
          >
            Sonraki Duruma Geç
          </button>
        </div>

        <!-- Kullanıcı Rolleri Güncelleme -->
        <div class="mt-2 space-y-2">
          <div class="flex space-x-2">
            <span class="text-sm font-semibold">Developer: </span>
            <input v-model="task.developer" @blur="updateRole(task.id, 'developer', task.developer)" class="p-1 border rounded-md w-1/4" placeholder="Developer Rolü" />
          </div>
          <div class="flex space-x-2">
            <span class="text-sm font-semibold">Analyst: </span>
            <input v-model="task.analyst" @blur="updateRole(task.id, 'analyst', task.analyst)" class="p-1 border rounded-md w-1/4" placeholder="Analyst Rolü" />
          </div>
          <div class="flex space-x-2">
            <span class="text-sm font-semibold">Tester: </span>
            <input v-model="task.tester" @blur="updateRole(task.id, 'tester', task.tester)" class="p-1 border rounded-md w-1/4" placeholder="Tester Rolü" />
          </div>
        </div>

        <!-- Sprint Atama -->
        <div class="mt-2 flex space-x-2">
          <select v-model="selectedSprint[task.id]" class="p-2 border rounded-md">
            <option disabled value="">Sprint Seç</option>
            <option v-for="sprint in sprints" :key="sprint.id" :value="sprint.id">
              {{ sprint.name }}
            </option>
          </select>
          <button @click="assignTaskToSprint(task.id, selectedSprint[task.id])"
                  class="px-3 py-1 bg-blue-500 text-white rounded-md hover:bg-blue-600"
          >
            Sprint’e Ekle
          </button>
        </div>

        <!-- Yorum Bileşeni -->
        <CommentSection :taskId="task.id" />
      </div>
    </div>

    <div v-else class="text-gray-500 text-center">Görev bulunamadı.</div>
  </div>
</template>

<script>
import { db } from "@/firebase";
import { collection, doc, updateDoc, onSnapshot, getDoc } from "firebase/firestore";
import CommentSection from "./CommentSection.vue"; // Yorum bileşeni

export default {
  components: { CommentSection },
  data() {
    return {
      tasks: [],
      sprints: [],
      selectedSprint: {},
      filters: ["Tümü", "backlog", "in-progress", "done"],
      currentFilter: "Tümü",
    };
  },
  computed: {
    filteredTasks() {
      if (this.currentFilter === "Tümü") {
        return this.tasks;
      }
      return this.tasks.filter((task) => task.status === this.currentFilter);
    },
  },
  methods: {
    async updateStatus(taskId, currentStatus) {
      const newStatus =
          currentStatus === "backlog" ? "in-progress" : "done";
      await updateDoc(doc(db, "tasks", taskId), { status: newStatus });
    },
    async assignTaskToSprint(taskId, sprintId) {
      if (!sprintId) return;
      const sprintRef = doc(db, "sprints", sprintId);

      // Firestore'daki mevcut sprint'i güncelle
      const sprintDoc = await getDoc(sprintRef);
      const currentTasks = sprintDoc.exists() ? sprintDoc.data().tasks || [] : [];

      if (!currentTasks.includes(taskId)) {
        await updateDoc(sprintRef, {
          tasks: [...currentTasks, taskId],
        });
      }
    },
    async updateRole(taskId, role, value) {
      const taskRef = doc(db, "tasks", taskId);
      const updateData = {};
      updateData[role] = value;
      await updateDoc(taskRef, updateData);
    },
  },
  mounted() {
    // Firestore’dan görevleri çek
    onSnapshot(collection(db, "tasks"), (snapshot) => {
      this.tasks = snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));
    });

    // Firestore’dan sprintleri çek
    onSnapshot(collection(db, "sprints"), (snapshot) => {
      this.sprints = snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));
    });
  },
};
</script>
