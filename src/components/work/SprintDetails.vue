<template>
  <div class="max-w-4xl mx-auto bg-white shadow-md rounded-lg p-6 mt-6">
    <h2 class="text-xl font-bold mb-4">{{ sprint.name }} - Görevler</h2>

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

        <!-- Yorum Bileşeni -->
        <CommentSection :taskId="task.id" />
      </div>
    </div>

    <div v-else class="text-gray-500 text-center">Görev bulunamadı.</div>
  </div>
</template>

<script>

import CommentSection from "./CommentSection.vue"; // Yorum bileşeni

export default {
  components: { CommentSection },
  props: {
    sprintId: String,  // Sprint ID'si, bu bileşene prop olarak gelecek
  },
  data() {
    return {
      sprint: {},
      tasks: [],
    };
  },
  computed: {
    filteredTasks() {
      return this.tasks;
    },
  },
  methods: {
    async updateStatus(taskId, currentStatus) {
      const newStatus =
          currentStatus === "backlog" ? "in-progress" : "done";
      /*
      await updateDoc(doc(db, "tasks", taskId), { status: newStatus });
      */

    },
  },
  mounted() {
    // Sprint bilgisini almak için Firestore'dan verileri çek
    /*
    onSnapshot(doc(db, "sprints", this.sprintId), async (docSnap) => {
      if (docSnap.exists()) {
        this.sprint = docSnap.data();
        // Sprint'e ait görev ID'lerini alıyoruz
        const taskPromises = this.sprint.tasks.map((taskId) =>
            getDoc(doc(db, "tasks", taskId))
        );

        const taskDocs = await Promise.all(taskPromises);
        this.tasks = taskDocs.map((doc) => ({
          id: doc.id,
          ...doc.data(),
        }));
      }
    });
    */

  },
};
</script>
