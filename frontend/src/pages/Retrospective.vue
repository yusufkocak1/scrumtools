<template>
  <div class="flex flex-row w-full pb-20 lg:pb-0">
    <SideBar></SideBar>
    <div class="flex-1 min-w-0 p-4">
      <div class="flex flex-col w-full h-screen">
        <div v-if="showCreateRetroBoard"
             class="fixed inset-0 z-[999] grid h-screen w-screen place-items-center bg-black bg-opacity-60 backdrop-blur-sm transition-opacity duration-300">
          <CreateRetroBoard v-if="showCreateRetroBoard" :selectedTeam="selectedTeam" @close="closeCreateRetroBoard"/>
        </div>
        <!-- Sayfa başlığı — takım merkezi context'ten gelir (Ayarlar > Çalışma Alanı) -->
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 px-2 pt-2 mb-2">
          <div>
            <h1 class="text-2xl font-bold text-gray-900">Retrospective</h1>
            <p class="text-sm text-gray-500">Sprint retrolarını yönetin</p>
          </div>
          <router-link
            v-if="activeTeam"
            to="/settings"
            class="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border border-gray-200 bg-white text-sm text-gray-700 hover:border-indigo-300 hover:text-indigo-700 transition"
            title="Aktif takımı Ayarlar'dan değiştir"
          >
            <span class="w-2 h-2 rounded-full bg-green-500"></span>
            {{ activeTeam.teamName }}
            <span class="text-xs text-gray-400">Değiştir</span>
          </router-link>
        </div>

        <div>
          <RetroBoardList v-if="selectedTeam" :boardList="boardList"
                          :teamId="selectedTeam" @createBoard="showCreateRetroBoard = true"
                          @deleteBoard="deleteBoard" @editBoard="editBoard" @confirmDelete="confirmDelete"
                          @openRetroBoard="openRetroBoard"></RetroBoardList>
          <div v-else class="text-center py-16">
            <div class="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
              </svg>
            </div>
            <h3 class="text-lg font-semibold text-gray-900 mb-1">Takım seçin</h3>
            <p class="text-sm text-gray-500">
              Retro panolarını görmek için
              <router-link to="/settings" class="text-indigo-600 font-medium hover:underline">Ayarlar &gt; Çalışma Alanı</router-link>'ndan
              bir takım seçin
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getBoards, deleteBoard as apiDeleteBoard, renameBoard } from "../api/RetroBoardApi.js";
import RetroBoardList from "../components/retro/RetroBoardList.vue";
import CreateRetroBoard from "../components/retro/CreateRetroBoard.vue";
import SideBar from "../components/SideBar.vue";
import { useTeamContext } from "../composables/useTeamContext.js";

export default {
  name: "Retrospective",
  components: {SideBar, CreateRetroBoard, RetroBoardList},
  setup() {
    // Takım seçimi merkezi: Ayarlar > Çalışma Alanı. Sayfa yalnızca okur.
    const { activeTeamId, activeTeam, loadTeams } = useTeamContext()
    loadTeams()
    return { activeTeamId, activeTeam }
  },
  data: () => ({
    showCreateRetroBoard: false,
    boardList: [],
  }),
  computed: {
    selectedTeam() { return this.activeTeamId || "" }
  },
  methods: {
    deleteBoard(boardId) {
      apiDeleteBoard(this.selectedTeam, boardId).then(() => this.getBoardsByTeamId(this.selectedTeam))
    },
    editBoard(boardData) {
      renameBoard(this.selectedTeam, boardData.id, boardData.name).then(() => this.getBoardsByTeamId(this.selectedTeam))
    },
    confirmDelete(boardId) {
      if (confirm('Are you sure you want to delete this board?')) {
        this.deleteBoard(boardId)
      }
    },
    closeCreateRetroBoard() {
      this.showCreateRetroBoard = false
      this.getBoardsByTeamId(this.selectedTeam)
    },
    getBoardsByTeamId(teamId) {
      if (!teamId) return;
      getBoards(teamId).then(boardList => {
        this.boardList = (boardList || []).sort((a, b) => {
          const aTime = a.createdAt ? new Date(a.createdAt).getTime() : (a.createdDate?.seconds || 0) * 1000;
          const bTime = b.createdAt ? new Date(b.createdAt).getTime() : (b.createdDate?.seconds || 0) * 1000;
          return aTime - bTime;
        });
      }).catch(e => console.error('Board listesi yüklenemedi:', e));
    },
    openRetroBoard(boardId) {
      this.$router.push(`/retroBoard/${this.selectedTeam}/${boardId}`)
    }
  },
  watch: {
    // Aktif takım (context) değişince panolar yeni takıma göre yüklenir
    selectedTeam: {
      immediate: true,
      handler(newTeamId) {
        this.boardList = [];
        if (newTeamId) {
          this.getBoardsByTeamId(newTeamId);
        }
      }
    }
  }
}
</script>
