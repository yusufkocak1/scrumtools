<template>
  <div class="flex flex-col">
    <!-- Loading spinner -->
    <div v-if="loading" class="fixed inset-0 bg-white bg-opacity-75 flex items-center justify-center z-[10000]">
      <div class="animate-spin rounded-full h-32 w-32 border-b-2 border-gray-900"></div>
    </div>

    <Navbar
        ref="navbar"
        :isLogged="isLogged"
        :name="name"
        :selectedTeam="selectedTeam"
        :teamList="teamList"
        @logout="logout"
        @team-select="handleTeamSelectRequest"
        @join-team="showJoinTeam = true"
        @create-team="showCreateTeam = true"
    />

    <!-- Modal overlay for team operations -->
    <div v-if="showJoinTeam || showCreateTeam || showTeamChangeConfirm"
         class="fixed inset-0 z-[999] grid h-screen w-screen place-items-center bg-black bg-opacity-60 backdrop-blur-sm transition-opacity duration-300">
      <JoinTeam v-if="showJoinTeam" @close="closeJoinTeam" @showCreateTeam="showCreateTeam = true"/>
      <CreateTeam v-if="showCreateTeam" @close="closeCreateTeam" @showJoinTeam="showJoinTeam = true"/>

      <!-- Team Change Confirmation Dialog -->
      <ConfirmationDialog
          v-if="showTeamChangeConfirm"
          :message="`Are you sure you want to switch to ${pendingTeamName} team?`"
          cancel-text="Cancel"
          confirm-text="Yes, Switch"
          description="You will be redirected to the homepage and your current work may be lost."
          title="Switch Team"
          type="warning"
          @cancel="cancelTeamChange"
          @confirm="confirmTeamChange"
      />
    </div>


    <div class="flex justify-center">
      <RouterView/>
    </div>
  </div>
</template>
<script>
import { me, logout as apiLogout } from "./api/AuthApi.js";
import Navbar from "./components/Navbar.vue";
import JoinTeam from "./components/team/JoinTeam.vue";
import CreateTeam from "./components/team/CreateTeam.vue";
import ConfirmationDialog from "./components/ConfirmationDialog.vue";
import { getMyTeams } from "./api/TeamApi.js";
import './scripts/collapse.js'

export default {
  name: "App",
  components: {
    Navbar,
    JoinTeam,
    CreateTeam,
    ConfirmationDialog
  },
  data: () => ({
    isLogged: false,
    name: "",
    loading: true,
    showJoinTeam: false,
    showCreateTeam: false,
    teamList: [],
    selectedTeam: "",
    showTeamChangeConfirm: false,
    pendingTeamId: "",
    pendingTeamName: "",
  }),
  methods: {
    logout() {
      apiLogout()
      this.isLogged = false
      this.name = ""
      this.$router.push('/login')
    },
    getUserName() {
      // Artık name, me() çağrısından geliyor — ayrı bir çağrıya gerek yok
    },
    closeJoinTeam() {
      this.showJoinTeam = false;
      this.getAllTeams();
    },
    closeCreateTeam() {
      this.showCreateTeam = false;
      this.getAllTeams();
    },
    selectTeam(teamId) {
      this.selectedTeam = teamId;
      localStorage.setItem("selectedTeam", teamId);
      window.dispatchEvent(new CustomEvent('teamChanged', {
        detail: {
          teamId: teamId,
          teamName: this.teamList.find(t => t.id === teamId)?.teamName || ''
        }
      }));

      // Home sayfasına yönlendir
      this.$router.push('/');
    },
    getAllTeams() {
      if (!this.isLogged) return;
      getMyTeams().then(teamList => {
        this.teamList = teamList
        if (teamList && teamList.length > 0) {
          if (localStorage.getItem("selectedTeam")) {
            if (teamList.find(t => t.id === localStorage.getItem("selectedTeam"))) {
              this.selectedTeam = localStorage.getItem("selectedTeam")
            } else {
              this.selectTeam(teamList[0].id)
            }
          } else {
            this.selectTeam(teamList[0].id)
          }
        } else {
          this.selectedTeam = ""
        }
      }).catch(err => console.error('Takımlar yüklenemedi:', err))
    },
    handleTeamSelectRequest(teamId) {
      // Eğer aynı takım seçildiyse hiçbir şey yapma
      if (this.selectedTeam === teamId) {
        return;
      }

      // Eğer selectedTeam boşsa (ilk seçim) direkt değiştir
      if (!this.selectedTeam) {
        this.selectTeam(teamId);
        return;
      }

      // Onay popup'ı göster
      const selectedTeam = this.teamList.find(team => team.id === teamId);
      if (selectedTeam) {
        this.pendingTeamId = selectedTeam.id;
        this.showTeamChangeConfirm = true;
      }
    },
    confirmTeamChange() {
      this.showTeamChangeConfirm = false;
      this.selectTeam(this.pendingTeamId);
      this.pendingTeamId = "";
      this.pendingTeamName = "";
    },
    cancelTeamChange() {
      this.showTeamChangeConfirm = false;
      this.pendingTeamId = "";
      this.pendingTeamName = "";

      // TeamList component'ine eski değere dönmesini söyle
      this.$refs.navbar.$refs.teamList.resetToSelected();
    }
  },
  async created() {
    const jwt = localStorage.getItem('jwt')

    if (jwt) {
      try {
        // Token geçerliliğini backend'de doğrula
        const user = await me()
        this.name = user.name
        this.isLogged = true
        this.getAllTeams()
      } catch {
        // Token geçersiz veya süresi dolmuş
        localStorage.removeItem('jwt')
        localStorage.removeItem('user')
        this.isLogged = false
        this.$router.push('/login')
      }
    } else {
      this.isLogged = false
      this.$router.push('/login')
    }

    this.loading = false
  },
  watch: {
    isLogged() {
      if (!this.isLogged) {
        this.$router.push('/login')
      } else {
        this.getAllTeams()
      }
    }
  }
}
</script>