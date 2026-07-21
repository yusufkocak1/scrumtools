<template>
  <div class="flex flex-col">
    <!-- Loading spinner -->
    <div v-if="loading" class="fixed inset-0 bg-white bg-opacity-75 flex items-center justify-center z-[10000]">
      <div class="animate-spin rounded-full h-32 w-32 border-b-2 border-gray-900"></div>
    </div>

    <!-- Landing gibi kendi header'ı olan sayfalarda global navbar gizlenir -->
    <Navbar
        v-if="!$route.meta.hideNavbar"
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
import { useOrganizationContext } from "./composables/useOrganizationContext.js";
import { useTeamContext } from "./composables/useTeamContext.js";
import Navbar from "./components/Navbar.vue";
import UpgradeModal from "./components/billing/UpgradeModal.vue";
import './scripts/collapse.js'

export default {
  name: "App",
  components: {
    Navbar,
    UpgradeModal
  },
  setup() {
    const auth = useAuth()
    const orgContext = useOrganizationContext()
    const teamContext = useTeamContext()
    return { auth, orgContext, teamContext }
  },
  data: () => ({
    loading: true,
  }),
  computed: {
    isLogged() { return this.auth.isAuthenticated.value },
    name() { return this.auth.name.value }
  },
  methods: {
    handleLogout() {
      apiLogout()
      // Çıkışta login yerine tanıtım sayfasına dön
      this.$router.push('/')
    },
    // Aktif organizasyon + takım seçimi merkezi context'lerde çözülür; sayfalar
    // seçimi oradan okur, burada yalnızca yükleme tetiklenir. Organizasyonlar önce
    // yüklenir ki takım seçimi doğrulanmış organizasyona göre hizalansın.
    async loadWorkspaceContexts() {
      if (!this.isLogged) return
      try {
        await this.orgContext.loadOrganizations()
      } finally {
        this.teamContext.loadTeams()
      }
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
        this.loadWorkspaceContexts()
        this.auth.fetchProfile().catch(() => {})
      } catch {
        // Token geçersiz → oturumu temizle; isLogged watcher'ı landing'e yönlendirir
        this.auth.logout()
      }
    }
    // JWT yoksa yönlendirme yapılmaz: ziyaretçi landing'i (/) görür,
    // korumalı rotalara girişi router guard zaten /login'e yönlendirir.
    this.loading = false
  },
  watch: {
    isLogged(val) {
      if (!val) {
        this.$router.push('/')
      } else {
        this.loadWorkspaceContexts()
      }
    }
  }
}
</script>

