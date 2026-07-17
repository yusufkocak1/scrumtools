<template>
  <div class="flex flex-col">
    <!-- Loading spinner -->
    <div v-if="loading" class="fixed inset-0 bg-white bg-opacity-75 flex items-center justify-center z-[10000]">
      <div class="animate-spin rounded-full h-32 w-32 border-b-2 border-gray-900"></div>
    </div>

    <Navbar
        :isLogged="isLogged"
        :name="name"
        @logout="handleLogout"
    />

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
import UpgradeModal from "./components/billing/UpgradeModal.vue";
import { getMyTeams } from "./api/TeamApi.js";
import './scripts/collapse.js'

export default {
  name: "App",
  components: {
    Navbar,
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

