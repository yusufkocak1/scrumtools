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
        @logout="handleLogout"
        @team-select="handleTeamSelectRequest"
    />

    <!-- Team Change Confirmation Dialog -->
    <div v-if="showTeamChangeConfirm"
         class="fixed inset-0 z-[999] grid h-screen w-screen place-items-center bg-black bg-opacity-60 backdrop-blur-sm transition-opacity duration-300">
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

    <div class="w-full min-w-0">
      <RouterView/>
    </div>

    <!-- Paket limiti aşıldığında (402) açılan global yükseltme modalı -->
    <UpgradeModal/>
  </div>
</template>

<script>
import { me, logout as apiLogout } from "./api/AuthApi.js";
import { useAuth } from "./composables/useAuth.js";
import Navbar from "./components/Navbar.vue";
import ConfirmationDialog from "./components/ConfirmationDialog.vue";
import UpgradeModal from "./components/billing/UpgradeModal.vue";
import { getMyTeams } from "./api/TeamApi.js";
import './scripts/collapse.js'

export default {
  name: "App",
  components: {
    Navbar,
    ConfirmationDialog,
    UpgradeModal
  },
  setup() {
    const auth = useAuth()
    return { auth }
  },
  data: () => ({
    loading: true,
    teamList: [],
    selectedTeam: "",
    showTeamChangeConfirm: false,
    pendingTeamId: "",
    pendingTeamName: "",
  }),
  computed: {
    isLogged() { return this.auth.isAuthenticated.value },
    name() { return this.auth.name.value }
  },
  methods: {
    handleLogout() {
      apiLogout()
      this.teamList = []
      this.selectedTeam = ''
      this.$router.push('/login')
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
      if (this.selectedTeam === teamId) return;
      if (!this.selectedTeam) {
        this.selectTeam(teamId);
        return;
      }
      const selectedTeam = this.teamList.find(team => team.id === teamId);
      if (selectedTeam) {
        this.pendingTeamId = selectedTeam.id;
        this.pendingTeamName = selectedTeam.teamName || '';
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
      this.$refs.navbar.$refs.teamList.resetToSelected();
    }
  },
  async created() {
    const jwt = localStorage.getItem('jwt')
    if (jwt) {
      try {
        const user = await me()
        this.auth.setUser({
          ...( this.auth.user.value || {} ),
          name: user.name,
          email: user.email,
        })
        this.getAllTeams()
        this.auth.fetchProfile().catch(() => {})
      } catch {
        this.auth.logout()
        this.$router.push('/login')
      }
    } else {
      this.$router.push('/login')
    }
    this.loading = false
  },
  watch: {
    isLogged(val) {
      if (!val) {
        this.$router.push('/login')
      } else {
        this.getAllTeams()
      }
    }
  }
}
</script>

