<template>
  <div class="flex flex-row w-full pb-20 lg:pb-0">
    <SideBar :team-id="selectedTeam"></SideBar>
    <div class="flex-1 min-w-0 p-4">
      <div class="flex flex-col w-full h-screen">
        <div v-if="showCreateRetroBoard"
             class="fixed inset-0 z-[999] grid h-screen w-screen place-items-center bg-black bg-opacity-60 backdrop-blur-sm transition-opacity duration-300">
          <CreateRetroBoard v-if="showCreateRetroBoard" :selectedTeam="selectedTeam" @close="closeCreateRetroBoard"/>
        </div>
        <!-- Sayfa başlığı + takım seçici -->
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 px-2 pt-2 mb-2">
          <div>
            <h1 class="text-2xl font-bold text-gray-900">Retrospective</h1>
            <p class="text-sm text-gray-500">Sprint retrolarını yönetin</p>
          </div>
          <TeamList :teamList="teams" :selectedTeamId="selectedTeam" align="left" @select="handleTeamSelect"></TeamList>
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
            <p class="text-sm text-gray-500">Retro panolarını görmek için yukarıdan bir takım seçin</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getBoards, deleteBoard as apiDeleteBoard, renameBoard } from "../api/RetroBoardApi.js";
import { getMyTeams } from "../api/TeamApi.js";
import RetroBoardList from "../components/retro/RetroBoardList.vue";
import CreateRetroBoard from "../components/retro/CreateRetroBoard.vue";
import TeamList from "../components/team/TeamList.vue";
import SideBar from "../components/SideBar.vue";

export default {
  name: "Retrospective",
  components: {SideBar, CreateRetroBoard, RetroBoardList, TeamList},
  data: () => ({
    selectedTeam: "",
    showCreateRetroBoard: false,
    boardList: [],
    teams: [],
  }),
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
    handleTeamChanged(event) {
      this.selectedTeam = event.detail.teamId;
    },
    handleTeamSelect(teamId) {
      if (this.selectedTeam === teamId) return;
      this.selectedTeam = teamId;
      localStorage.setItem("selectedTeam", teamId);
      window.dispatchEvent(new CustomEvent('teamChanged', {
        detail: {
          teamId: teamId,
          teamName: this.teams.find(t => t.id === teamId)?.teamName || ''
        }
      }));
    },
    openRetroBoard(boardId) {
      this.$router.push(`/retroBoard/${this.selectedTeam}/${boardId}`)
    }
  },
  mounted() {
    // localStorage'dan selectedTeam'i al
    const storedTeam = localStorage.getItem("selectedTeam");
    if (storedTeam) {
      this.selectedTeam = storedTeam;
    }

    // Takım seçici için takım listesini yükle
    getMyTeams().then(teams => {
      this.teams = teams || [];
    }).catch(e => console.error('Takımlar yüklenemedi:', e));

    // Team değişikliklerini dinle
    window.addEventListener('teamChanged', this.handleTeamChanged);
  },
  beforeUnmount() {
    window.removeEventListener('teamChanged', this.handleTeamChanged);
  },
  watch: {
    selectedTeam(newTeamId) {
      if (newTeamId) {
        this.getBoardsByTeamId(newTeamId);
      }
    }
  }
}
</script>
