<template>
  <div class="max-w-4xl mx-auto bg-white shadow-md rounded-lg p-6 mt-6">
    <h2 class="text-xl font-bold mb-4">{{ sprint.name }} - Sprint Detayları</h2>

    <!-- Sprint Detayları -->
    <p class="text-sm text-gray-500">Başlangıç: {{ sprint.startDate }}</p>
    <p class="text-sm text-gray-500">Bitiş: {{ sprint.endDate }}</p>

    <!-- Sprint'e ait görevleri listele -->
    <SprintDetails :sprintId="sprint.id" />
  </div>
</template>

<script>

import SprintDetails from "./SprintDetails.vue"; // Sprint detayları bileşeni

export default {
  components: { SprintDetails },
  data() {
    return {
      sprint: {},
    };
  },
  mounted() {
    // Parametre olarak gelen sprintId'yi alıp Firestore'dan sprint bilgisini getiriyoruz
    const sprintId = this.$route.params.sprintId; // Vue Router ile gelen parametre
   /* onSnapshot(doc(db, "sprints", sprintId), (docSnap) => {
      if (docSnap.exists()) {
        this.sprint = docSnap.data();
      }
    });*/
  },
};
</script>
