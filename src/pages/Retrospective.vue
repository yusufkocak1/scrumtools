<template>
  <div class="flex flex-row w-screen">
    <SideBar :team-id="selectedTeam" class="hidden lg:flex"></SideBar>
    <div class="flex-1 p-4">
      <div class="flex flex-col w-full h-screen">
        <div v-if="showCreateRetroBoard"
             class="fixed inset-0 z-[999] grid h-screen w-screen place-items-center bg-black bg-opacity-60 backdrop-blur-sm transition-opacity duration-300">
          <CreateRetroBoard v-if="showCreateRetroBoard" :selectedTeam="selectedTeam" @close="closeCreateRetroBoard"/>
        </div>
        <div>
          <RetroBoardList v-if="selectedTeam" :boardList="boardList"
                          :teamId="selectedTeam" @createBoard="showCreateRetroBoard = true"
                          @deleteBoard="deleteBoard" @editBoard="editBoard" @confirmDelete="confirmDelete"
                          @openRetroBoard="openRetroBoard"></RetroBoardList>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {getRetroBoardsFromTeam, removeRetroBoardFromTeam, updateRetroBoard} from "../firebase/RetroBoardService.js";
import RetroBoardList from "../components/retro/RetroBoardList.vue";
import CreateRetroBoard from "../components/retro/CreateRetroBoard.vue";
import SideBar from "../components/SideBar.vue";

export default {
  name: "Retrospective",
  components: {SideBar, CreateRetroBoard, RetroBoardList},
  data: () => ({
    selectedTeam: "",
    showCreateRetroBoard: false,
    boardList: [],
  }),
  methods: {
    deleteBoard(boardId) {
      removeRetroBoardFromTeam(this.selectedTeam, boardId)
      this.getBoardsByTeamId(this.selectedTeam)
    },
    editBoard(boardData) {
      updateRetroBoard(this.selectedTeam, boardData.id, boardData.name)
      this.getBoardsByTeamId(this.selectedTeam)
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
      getRetroBoardsFromTeam(teamId, (boardList) => {
        this.boardList = boardList.sort((a, b) => a.createdDate.seconds - b.createdDate.seconds);
      })
    },
    handleTeamChanged(event) {
      this.selectedTeam = event.detail.teamId;
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
