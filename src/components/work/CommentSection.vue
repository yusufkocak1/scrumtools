<template>
  <div class="mt-4 p-4 border-t">
    <h3 class="text-lg font-bold mb-2">Yorumlar</h3>

    <!-- Yorum Listesi -->
    <div v-if="comments.length" class="space-y-2">
      <div v-for="comment in comments" :key="comment.id" class="p-2 bg-gray-100 rounded-md">
        <p class="text-gray-700">{{ comment.text }}</p>
        <p class="text-sm text-gray-500">{{ formatDate(comment.createdAt) }}</p>
      </div>
    </div>
    <p v-else class="text-gray-500">Henüz yorum yok.</p>

    <!-- Yorum Ekleme -->
    <div class="mt-3 flex space-x-2">
      <input v-model="newComment" type="text"
             class="w-full p-2 border rounded-md focus:ring-2 focus:ring-blue-500"
             placeholder="Yorum yaz..."
      />
      <button @click="addComment"
              class="px-3 py-2 bg-green-600 text-white rounded-md hover:bg-green-700"
      >
        Ekle
      </button>
    </div>
  </div>
</template>

<script>
export default {
  props: ["taskId"],
  data() {
    return {
      newComment: "",
      comments: [],
    };
  },
  methods: {
    async addComment() {
      if (!this.newComment.trim()) return;

     /* await addDoc(collection(db, "tasks", this.taskId, "comments"), {
        text: this.newComment,
        createdAt: serverTimestamp(),
      });
*/
      this.newComment = "";
    },
    formatDate(timestamp) {
      if (!timestamp) return "";
      return new Date(timestamp.seconds * 1000).toLocaleString();
    },
  },
  mounted() {
   /* const commentsRef = query(
        collection(db, "tasks", this.taskId, "comments"),
        orderBy("createdAt", "desc")
    );
    onSnapshot(commentsRef, (snapshot) => {
      this.comments = snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));
    });*/
  },
};
</script>
