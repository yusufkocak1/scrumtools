<template>
  <!-- Modal Arka Planı -->
  <div v-if="isOpen" class="fixed inset-0 bg-gray-900 bg-opacity-50 flex items-center justify-center z-50">
    <!-- Modal İçeriği -->
    <div class="bg-white shadow-lg rounded-lg p-6 w-full max-w-lg relative">

      <!-- Kapat Butonu -->
      <button class="absolute top-3 right-3 text-gray-500 hover:text-gray-800"
              @click="closeModal">
        ✕
      </button>

      <h2 class="text-xl font-bold mb-4">Create New Issue</h2>

      <form class="space-y-4" @submit.prevent="addTask">
        <div>
          <label class="block text-sm font-medium text-gray-700">Issue Type</label>
          <select v-model="issueType"
                  class="w-full p-2 border rounded-md bg-white focus:ring-2 focus:ring-blue-500">
            <option v-for="type in issueTypes" :key="type" :value="type">{{ type }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700">Görev Başlığı</label>
          <input v-model="title" class="w-full p-2 border rounded-md focus:ring-2 focus:ring-blue-500" placeholder="Görev başlığı girin"
                 required
                 type="text"/>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">Açıklama</label>
          <textarea v-model="description"
                    class="w-full p-2 border rounded-md focus:ring-2 focus:ring-blue-500"
                    placeholder="Görev açıklaması girin"></textarea>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700">Assignee</label>
          <input v-model="assigne" class="w-full p-2 border rounded-md focus:ring-2 focus:ring-blue-500"
                 placeholder="Assignee UID"
                 type="text"/>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700">Developer</label>
          <input v-model="developer" class="w-full p-2 border rounded-md focus:ring-2 focus:ring-blue-500"
                 placeholder="Developer UID"
                 type="text"/>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">Analyst</label>
          <input v-model="analyst" class="w-full p-2 border rounded-md focus:ring-2 focus:ring-blue-500"
                 placeholder="Analyst UID"
                 type="text"/>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">Tester</label>
          <input v-model="tester" class="w-full p-2 border rounded-md focus:ring-2 focus:ring-blue-500"
                 placeholder="Tester UID"
                 type="text"/>
        </div>

        <button class="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition"
                type="submit">
          Create
        </button>
      </form>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    isOpen: Boolean, // Modal açık mı değil mi?
  },
  data() {
    return {
      title: "",
      description: "",
      issueType: "task",
      developer: "",
      analyst: "",
      tester: "",
      assigne: "",
      issueTypes: ["task", "story", "bug", "project"]
    };
  },
  methods: {
    async addTask() {
      try {
        this.$emit("addTask", {
          title: this.title,
          description: this.description,
          issueType: this.issueType,
          developer: this.developer || null,
          analyst: this.analyst || null,
          tester: this.tester || null,
          assignee: this.assigne || null,
          status: "Open",
          createdAt: new Date(),
        });

        // Formu temizle
        this.title = "";
        this.description = "";
        this.issueType = "task";
        this.developer = "";
        this.analyst = "";
        this.tester = "";

        // Modal'ı kapat
        this.$emit("close");
      } catch (error) {
        console.error("Görev eklenirken hata oluştu:", error);
      }
    },
    closeModal() {
      this.$emit("close");
    }
  },
};
</script>
