<template>
  <div
      class="w-screen px-4 py-2 mx-auto bg-white bg-opacity-90 top-3 shadow lg:px-8 lg:py-3 backdrop-blur-lg backdrop-saturate-150 z-[9999]">
    <div class="flex flex-wrap items-center justify-between mx-auto text-slate-800 font-extrabold text-2xl">
      <RouterLink to="/">Home</RouterLink>

      <!-- Team Management Section -->
      <div v-if="isLogged" class="flex items-center gap-4">
        <div class="flex items-center gap-2">
          <TeamList ref="teamList" :teamList="teamList" @select="handleTeamSelect"></TeamList>
          <InviteToTheTeam v-if="selectedTeam" :team-id="selectedTeam"></InviteToTheTeam>
        </div>

        <div class="flex gap-2">
          <button
              class="rounded-md bg-slate-800 py-1 px-3 border border-transparent text-center text-xs text-white transition-all shadow-md hover:shadow-lg focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-slate-700 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
              type="button"
              @click="handleJoinTeam">
            Join Team
          </button>
          <button
              class="rounded-md bg-slate-800 py-1 px-3 border border-transparent text-center text-xs text-white transition-all shadow-md hover:shadow-lg focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-slate-700 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
              type="button"
              @click="handleCreateTeam">
            Create Team
          </button>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <!-- Profile Dropdown -->
        <div v-if="isLogged" class="relative">
          <button
              @click="toggleProfileDropdown"
              class="flex items-center gap-2 p-2 rounded-lg hover:bg-gray-100 transition-colors"
              type="button">
            <!-- Avatar with initials -->
            <div class="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-sm">
              {{ userInitials }}
            </div>
            <span class="hidden lg:block text-sm font-medium">{{ name }}</span>
            <svg class="w-4 h-4 transition-transform" :class="{'rotate-180': showProfileDropdown}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
            </svg>
          </button>

          <!-- Dropdown Menu -->
          <div v-if="showProfileDropdown"
               class="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg border border-gray-200 z-50">
            <div class="py-1">
              <button
                  @click="gotoTeams"
                  class="flex items-center gap-3 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"></path>
                </svg>
                Teams
              </button>
              <button
                  @click="gotoSettings"
                  class="flex items-center gap-3 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"></path>
                </svg>
                Settings
              </button>
              <hr class="my-1">
              <button
                  @click="handleLogout"
                  class="flex items-center gap-3 w-full px-4 py-2 text-sm text-red-600 hover:bg-red-50">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 102 0V4a1 1 0 00-1-1zm10.293 9.293a1 1 0 001.414 1.414l3-3a1 1 0 000-1.414l-3-3a1 1 0 10-1.414 1.414L14.586 9H7a1 1 0 100 2h7.586l-1.293 1.293z" clip-rule="evenodd"></path>
                </svg>
                Logout
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import TeamList from './team/TeamList.vue'
import InviteToTheTeam from './team/InviteToTheTeam.vue'

export default {
  name: 'Navbar',
  components: {
    TeamList,
    InviteToTheTeam
  },
  props: {
    isLogged: {
      type: Boolean,
      default: false
    },
    name: {
      type: String,
      default: ''
    },
    teamList: {
      type: Array,
      default: () => []
    },
    selectedTeam: {
      type: String,
      default: ''
    }
  },
  emits: ['logout', 'team-select', 'join-team', 'create-team'],
  data() {
    return {
      showProfileDropdown: false
    }
  },
  computed: {
    userInitials() {
      if (!this.name) return '';

      const words = this.name.trim().split(' ').filter(word => word.length > 0);

      if (words.length === 1) {
        // Tek kelime varsa ilk harfi
        return words[0].charAt(0).toUpperCase();
      } else if (words.length === 2) {
        // İki kelime varsa her birinin ilk harfi (örn: Yusuf Koçak -> YK)
        return (words[0].charAt(0) + words[1].charAt(0)).toUpperCase();
      } else if (words.length >= 3) {
        // Üç veya daha fazla kelime varsa ilk ve son kelimenin ilk harfi (örn: Ömer Talha Çim -> ÖÇ)
        return (words[0].charAt(0) + words[words.length - 1].charAt(0)).toUpperCase();
      }

      return '';
    }
  },
  methods: {
    handleLogout() {
      this.showProfileDropdown = false;
      this.$emit('logout')
    },
    handleTeamSelect(teamId) {
      this.$emit('team-select', teamId)
    },
    handleJoinTeam() {
      this.$emit('join-team')
    },
    handleCreateTeam() {
      this.$emit('create-team')
    },
    toggleProfileDropdown() {
      this.showProfileDropdown = !this.showProfileDropdown;
    },
    gotoTeams() {
      this.$router.push('/teams');
      this.showProfileDropdown = false;
    },
    gotoSettings() {
      this.$router.push('/settings');
      this.showProfileDropdown = false;
    }
  }
}
</script>
