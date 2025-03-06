<template>
  <div class="max-w-4xl mx-auto bg-white shadow-md rounded-lg p-6 mt-6">
    <h2 class="text-xl font-bold mb-4">Spritler</h2>

    <!-- Sprint Listesi -->
    <div v-if="sprints.length" class="space-y-4">
      <div
          v-for="sprint in sprints"
          :key="sprint.id"
          class="p-4 border rounded-md shadow-sm"
      >
        <h3 class="text-lg font-bold">{{ sprint.name }}</h3>
        <p class="text-sm text-gray-500">Başlangıç: {{ sprint.startDate }}</p>
        <p class="text-sm text-gray-500">Bitiş: {{ sprint.endDate }}</p>
      </div>
    </div>
    <p v-else class="text-gray-500">Henüz sprint oluşturulmadı.</p>

    <!-- Yeni Sprint Ekleme -->
    <div class="mt-6">
      <input v-model="sprintName" type="text" placeholder="Sprint adı"
             class="w-full p-2 border rounded-md focus:ring-2 focus:ring-blue-500 mb-2"
      />
      <input v-model="startDate" type="date"
             class="w-full p-2 border rounded-md focus:ring-2 focus:ring-blue-500 mb-2"
      />
      <input v-model="endDate" type="date"
             class="w-full p-2 border rounded-md focus:ring-2 focus:ring-blue-500 mb-2"
      />
      <button @click="addSprint"
              class="w-full px-3 py-2 bg-green-600 text-white rounded-md hover:bg-green-700"
      >
        Sprint Ekle
      </button>
    </div>
  </div>
</template>

<script>
import { db } from "@/firebase";
import { collection, addDoc, onSnapshot } from "firebase/firestore";

export default {
  data() {
    return {
      sprints: [],
      sprintName: "",
      startDate: "",
      endDate: "",
    };
  },
  methods: {
    async addSprint() {
      if (!this.sprintName || !this.startDate || !this.endDate) return;
      await addDoc(collection(db, "sprints"), {
        name: this.sprintName,
        startDate: this.startDate,
        endDate: this.endDate,
        tasks: [],
      });

      this.sprintName = "";
      this.startDate = "";
      this.endDate = "";
    },
  },
  mounted() {
    onSnapshot(collection(db, "sprints"), (snapshot) => {
      this.sprints = snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));
    });
  },
};
</script>
