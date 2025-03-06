<template>
  <div class="max-w-4xl mx-auto bg-white shadow-md rounded-lg p-6 mt-6">
    <h2 class="text-xl font-bold mb-4">{{ task.title }} - Detaylar</h2>

    <div>
      <p class="text-sm text-gray-500">Durum: {{ task.status }}</p>
      <p class="text-sm text-gray-500">{{ task.description }}</p>
    </div>

    <!-- Rol Atama Alanı -->
    <div class="mt-4">
      <div class="flex space-x-4">
        <div>
          <label class="block text-sm font-medium text-gray-700">Developer</label>
          <select v-model="task.developer" class="p-2 border rounded-md">
            <option disabled value="">Seç</option>
            <option v-for="user in users" :key="user.id" :value="user.name">
              {{ user.name }}
            </option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">Analyst</label>
          <select v-model="task.analyst" class="p-2 border rounded-md">
            <option disabled value="">Seç</option>
            <option v-for="user in users" :key="user.id" :value="user.name">
              {{ user.name }}
            </option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">Tester</label>
          <select v-model="task.tester" class="p-2 border rounded-md">
            <option disabled value="">Seç</option>
            <option v-for="user in users" :key="user.id" :value="user.name">
              {{ user.name }}
            </option>
          </select>
        </div>
      </div>

      <button @click="assignRoles" class="mt-4 px-4 py-2 bg-blue-500 text-white rounded-md">
        Rolleri Kaydet
      </button>
    </div>

    <!-- Statü Güncelleme -->
    <div class="mt-4">
      <button
          v-if="task.status !== 'done'"
          @click="updateStatus(task.id, task.status)"
          class="px-3 py-1 text-white bg-blue-500 hover:bg-blue-600 rounded-md"
      >
        Sonraki Duruma Geç
      </button>
    </div>
  </div>
</template>

<script>
import { db } from "@/firebase";
import { doc, updateDoc } from "firebase/firestore";

export default {
  props: {
    taskId: String,
    users: Array,  // Kullanıcı listesini prop olarak alıyoruz
  },
  data() {
    return {
      task: {
        id: this.taskId,
        title: "",
        description: "",
        status: "backlog",
        developer: "",
        analyst: "",
        tester: "",
      },
    };
  },
  methods: {
    async assignRoles() {
      const taskRef = doc(db, "tasks", this.task.id);
      await updateDoc(taskRef, {
        developer: this.task.developer,
        analyst: this.task.analyst,
        tester: this.task.tester,
      });
    },
    async updateStatus(taskId, currentStatus) {
      const newStatus =
          currentStatus === "backlog" ? "in-progress" : "done";
      const taskRef = doc(db, "tasks", taskId);
      await updateDoc(taskRef, { status: newStatus });
    },
  },
  mounted() {
    // Firestore'dan görev verilerini al
    const taskRef = doc(db, "tasks", this.taskId);
    taskRef.get().then((docSnap) => {
      if (docSnap.exists()) {
        this.task = docSnap.data();
      }
    });
  },
};
</script>
