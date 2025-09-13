<template>
  <div class="flex flex-row w-screen">
    <SideBar :team-id="selectedTeam"></SideBar>
    <div class="mx-4 flex flex-row  border w-screen  mt-2">
      <div class="flex flex-col w-full h-screen">
        <div class="flex flex-wrap gap-2 justify-between p-5 items-center border-b">
          <div class="flex justify-center gap-2">
            <TeamList :teamList="teamList" @select="selectTeam"></TeamList>
            <InviteToTheTeam :team-id="selectedTeam"></InviteToTheTeam>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
                class="rounded-md bg-slate-800 py-2 px-4 border border-transparent text-center text-sm text-white transition-all shadow-md hover:shadow-lg focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-slate-700 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none "
                type="button"
                @click="showJoinTeam=!showJoinTeam">
              Join Team
            </button>
            <button
                class="rounded-md bg-slate-800 py-2 px-4 border border-transparent text-center text-sm text-white transition-all shadow-md hover:shadow-lg focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-slate-700 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none "
                type="button"
                @click="showCreateTeam=!showCreateTeam">
              Create Team
            </button>
          </div>
        </div>
        <div v-if="showJoinTeam || showCreateTeam || showCreateRetroBoard"
             class=" fixed inset-0 z-[999] grid h-screen w-screen place-items-center bg-black bg-opacity-60  backdrop-blur-sm transition-opacity duration-300">
          <JoinTeam v-if="showJoinTeam" @close="showJoinTeam = false" @showCreateTeam="showCreateTeam = true"/>
          <CreateTeam v-if="showCreateTeam" @close="closeCreateTeam" @showJoinTeam="showJoinTeam = true"/>
          <CreateRetroBoard v-if="showCreateRetroBoard" :selectedTeam="selectedTeam" @close="closeCreateRetroBoard"/>
        </div>
        <div>
          <RetroBoardList v-if="selectedTeam" :boardList="boardList"
                          :teamId="selectedTeam" @createBoard="showCreateRetroBoard = true"
                          @deleteBoard="deleteBoard"></RetroBoardList>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import JoinTeam from "../components/team/JoinTeam.vue";
import CreateTeam from "../components/team/CreateTeam.vue";
import TeamList from "../components/team/TeamList.vue";
import {getTeams, listenTeams} from "../firebase/TeamService.js";
import {getRetroBoardsFromTeam, removeRetroBoardFromTeam} from "../firebase/RetroBoardService.js";
import RetroBoardList from "../components/retro/RetroBoardList.vue";
import CreateRetroBoard from "../components/retro/CreateRetroBoard.vue";
import SideBar from "../components/SideBar.vue";
import InviteToTheTeam from "../components/team/InviteToTheTeam.vue";

export default {
  name: "Home",
  components: {InviteToTheTeam, SideBar, CreateRetroBoard, RetroBoardList, TeamList, CreateTeam, JoinTeam},
  data: () => ({
    teamList: [],
    selectedTeam: "",
    showJoinTeam: false,
    showCreateTeam: false,
    showCreateRetroBoard: false,
    boardList: [],
  }),
  methods: {
    getRetroBoardsFromTeam,
    selectTeam(teamId) {
      this.selectedTeam = teamId
      localStorage.setItem("selectedTeam", teamId)
    },
    deleteBoard(boardId) {
      removeRetroBoardFromTeam(this.selectedTeam, boardId)
      this.getBoardsByTeamId(this.selectedTeam)
    },
    closeCreateRetroBoard() {
      this.showCreateRetroBoard = false
      this.getBoardsByTeamId(this.selectedTeam)
    },
    getBoardsByTeamId(teamId) {
      getRetroBoardsFromTeam(teamId, (boardList) => {
        this.boardList = boardList.sort((a, b) => a.createdDate.seconds - b.createdDate.seconds);
      })

    },
    closeCreateTeam(){
      this.showCreateTeam = false
      this.getAllTeams();
    },
    getAllTeams(){
      getTeams((teamList) => {
        this.teamList = teamList
        if (teamList && teamList.length > 0) {
          if(localStorage.getItem("selectedTeam")) {
            if(teamList.find(t=>t.id===localStorage.getItem("selectedTeam"))){
              this.selectedTeam = localStorage.getItem("selectedTeam")
            } else {
              this.selectedTeam = teamList[0].id
            }
          }
          this.selectedTeam = teamList[0].id
        } else {
          this.selectedTeam = ""
        }
      })
    }
  },
  created() {
    this.getAllTeams()
  },
  watch: {
    selectedTeam() {
      this.getBoardsByTeamId(this.selectedTeam)
    }
  }
}
</script>