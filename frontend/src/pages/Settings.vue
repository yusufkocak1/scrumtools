<template>
  <div class="flex flex-row w-full min-h-screen bg-gray-50 pb-20 lg:pb-0">
    <SideBar />
    <div class="flex-1 min-w-0 p-4 sm:p-6">
      <div class="max-w-4xl mx-auto">
        <!-- Header -->
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">Settings</h1>
          <p class="text-gray-600 mt-2">Manage your profile information and security settings</p>
        </div>

        <!-- Çalışma Alanı: aktif organizasyon + takım TEK yerden seçilir.
             Board, Retrospective, Scrum Poker, GameBox, CodeShare ve Dashboard
             bu seçimi merkezi context'ten okur. -->
        <div class="mb-8 bg-white rounded-xl shadow-sm border border-indigo-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200 bg-indigo-50/60 flex items-center justify-between">
            <div>
              <h2 class="text-xl font-semibold text-gray-800">Çalışma Alanı</h2>
              <p class="text-sm text-gray-500 mt-0.5">Tüm modüllerde kullanılacak aktif organizasyon ve takım</p>
            </div>
            <svg class="w-6 h-6 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8"
                d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5m0 0v-4a1 1 0 011-1h2a1 1 0 011 1v4m-4 0h4"/>
            </svg>
          </div>

          <div class="p-6">
            <div v-if="!hasOrganizations && !orgLoading" class="text-sm text-gray-600">
              Henüz bir organizasyonunuz yok.
              <router-link to="/organizations" class="text-indigo-600 font-medium hover:underline">
                Organizasyon oluşturun veya katılın →
              </router-link>
            </div>

            <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <!-- Organizasyon -->
              <div class="space-y-2">
                <label class="block text-sm font-medium text-gray-700">Organizasyon</label>
                <select
                  :value="activeOrgId || ''"
                  :disabled="orgLoading"
                  @change="handleOrgChange($event.target.value)"
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 disabled:bg-gray-50 transition-colors"
                >
                  <option v-for="org in organizations" :key="org.id" :value="org.id">
                    {{ org.name }}
                  </option>
                </select>
              </div>

              <!-- Takım (yalnızca seçili organizasyonun takımları) -->
              <div class="space-y-2">
                <label class="block text-sm font-medium text-gray-700">Takım</label>
                <select
                  v-if="teamsInActiveOrg.length > 0"
                  :value="activeTeamId || ''"
                  :disabled="teamsLoading"
                  @change="handleTeamChange($event.target.value)"
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 disabled:bg-gray-50 transition-colors"
                >
                  <option v-for="team in teamsInActiveOrg" :key="team.id" :value="team.id">
                    {{ team.teamName }}
                  </option>
                </select>
                <p v-else class="px-1 py-3 text-sm text-gray-500">
                  Bu organizasyonda üyesi olduğunuz takım yok.
                  <router-link to="/organizations" class="text-indigo-600 font-medium hover:underline">Takım oluşturun →</router-link>
                </p>
              </div>
            </div>

            <p v-if="activeTeam" class="mt-4 text-xs text-gray-500">
              Aktif seçim: <span class="font-medium text-gray-700">{{ activeOrg?.name }}</span> /
              <span class="font-medium text-gray-700">{{ activeTeam.teamName }}</span> —
              Board, Retrospective, Scrum Poker, GameBox ve diğer tüm modüller bu takımla açılır.
            </p>
          </div>
        </div>

        <!-- Profile Section -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <h2 class="text-xl font-semibold text-gray-800">Profile Information</h2>
          </div>

          <div class="p-6 space-y-6">
            <!-- Email Display -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Email Address
              </label>
              <div class="px-4 py-3 bg-gray-50 rounded-lg border">
                <span class="text-gray-900 font-medium">{{ email?.toLowerCase() }}</span>
              </div>
              <p class="text-xs text-gray-500">Your email address cannot be changed</p>
            </div>

            <!-- Display Name -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Display Name
              </label>
              <div class="flex gap-3">
                <input
                  v-model="displayName"
                  :disabled="isUpdatingDisplayName"
                  class="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-50 disabled:cursor-not-allowed transition-colors"
                  placeholder="Enter your display name"
                  type="text"
                />
                <button
                  :disabled="!displayName.trim() || isUpdatingDisplayName || displayName === originalDisplayName"
                  class="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
                  @click="handleDisplayNameChange"
                >
                  <svg v-if="isUpdatingDisplayName" class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {{ isUpdatingDisplayName ? 'Updating...' : 'Update' }}
                </button>
              </div>
            </div>

            <!-- Password -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Current Password
              </label>
              <input
                v-model="currentPassword"
                type="password"
                :disabled="isUpdatingPassword"
                class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-50 disabled:cursor-not-allowed transition-colors"
                placeholder="Enter your current password"
                autocomplete="current-password"
              />
              <label class="block text-sm font-medium text-gray-700">
                New Password
              </label>
              <div class="flex gap-3">
                <div class="flex-1 relative">
                  <input
                    v-model="password"
                    :type="showPassword ? 'text' : 'password'"
                    :disabled="isUpdatingPassword"
                    class="w-full px-4 py-3 pr-12 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-50 disabled:cursor-not-allowed transition-colors"
                    placeholder="Enter your new password"
                    autocomplete="new-password"
                  />
                  <button
                    type="button"
                    class="absolute inset-y-0 right-0 pr-3 flex items-center"
                    @click="showPassword = !showPassword"
                  >
                    <svg v-if="showPassword" class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    <svg v-else class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
                    </svg>
                  </button>
                </div>
                <button
                  :disabled="!password.trim() || !currentPassword.trim() || isUpdatingPassword"
                  class="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
                  @click="handlePasswordChange"
                >
                  <svg v-if="isUpdatingPassword" class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {{ isUpdatingPassword ? 'Updating...' : 'Update' }}
                </button>
              </div>
              <div class="space-y-1 text-xs text-gray-500">
                <p>• Must be at least 8 characters</p>
                <p>• Must contain uppercase and lowercase letters</p>
                <p>• Must contain at least one number</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Team Information -->
        <div v-if="allTeams.length > 0" class="mt-8 bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <h2 class="text-xl font-semibold text-gray-800">Team Information</h2>
          </div>
          <div class="p-6">
            <div class="space-y-3">
              <p class="text-sm text-gray-600">Teams you are a member of:</p>
              <div class="grid gap-3">
                <div
                  v-for="team in allTeams"
                  :key="team.id"
                  class="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
                >
                  <div>
                    <h3 class="font-medium text-gray-900">{{ team.teamName }}</h3>
                    <p class="text-sm text-gray-500">{{ team.memberEmails?.length || 0 }} members</p>
                  </div>
                  <div class="flex items-center gap-3">
                    <span
                      v-if="team.id === activeTeamId"
                      class="px-3 py-1 bg-green-100 text-green-800 text-xs font-medium rounded-full"
                    >
                      Active
                    </span>
                    <button
                      v-if="team.adminEmail !== email"
                      :disabled="isLeavingTeam === team.id"
                      class="px-3 py-2 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
                      @click="handleLeaveTeam(team)"
                    >
                      <svg v-if="isLeavingTeam === team.id" class="animate-spin h-3 w-3" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      <svg v-else class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                      </svg>
                      {{ isLeavingTeam === team.id ? 'Leaving...' : 'Leave' }}
                    </button>
                    <span
                      v-else
                      class="px-3 py-2 bg-gray-100 text-gray-500 text-sm rounded-lg cursor-not-allowed"
                      title="You cannot leave a team you admin"
                    >
                      Admin
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { changePassword as apiChangePassword, updateName as apiUpdateName, me } from '../api/AuthApi.js'
import { useAuth } from '../composables/useAuth.js'
import { useOrganizationContext } from '../composables/useOrganizationContext.js'
import { useTeamContext } from '../composables/useTeamContext.js'
import { createToast } from 'mosha-vue-toastify'
import { updateDisplayNameAcrossTeams, removeMember } from '../api/TeamApi.js'
import SideBar from '../components/SideBar.vue'

export default {
  name: 'Settings',
  components: {
    SideBar
  },
  setup() {
    // useAuth — merkezi auth kaynağından okuyoruz
    const { user: authUser, userEmail, name: authName } = useAuth()

    // Çalışma alanı: aktif organizasyon + takım burada merkezi olarak seçilir
    const {
      organizations,
      activeOrgId,
      activeOrg,
      hasOrganizations,
      loading: orgLoading,
      loadOrganizations,
      selectOrg,
    } = useOrganizationContext()

    const {
      teams: allTeams,
      teamsInActiveOrg,
      activeTeamId,
      activeTeam,
      loading: teamsLoading,
      loadTeams,
      selectTeam,
    } = useTeamContext()

    return {
      authUser, userEmail, authName,
      organizations, activeOrgId, activeOrg, hasOrganizations, orgLoading, loadOrganizations, selectOrg,
      allTeams, teamsInActiveOrg, activeTeamId, activeTeam, teamsLoading, loadTeams, selectTeam,
    }
  },
  data() {
    return {
      displayName: '',
      originalDisplayName: '',
      email: '',
      password: '',
      currentPassword: '',
      showPassword: false,
      isUpdatingDisplayName: false,
      isUpdatingPassword: false,
      isLeavingTeam: null
    }
  },
  mounted() {
    this.initializeData()
  },
  methods: {
    async initializeData() {
      // Önce useAuth'tan oku — daha hızlı (localStorage zaten sync'lenmiş)
      this.email = this.userEmail.value || localStorage.getItem('user') || ''
      this.displayName = this.authName.value || ''
      this.originalDisplayName = this.authName.value || ''

      // Gerekirse me() ile tazele (token doğrulama için)
      if (!this.displayName) {
        try {
          const user = await me()
          this.displayName = user.name || ''
          this.originalDisplayName = user.name || ''
          if (!this.email) this.email = user.email || ''
        } catch (err) {
          console.warn('Kullanıcı bilgisi alınamadı:', err)
        }
      }

      // Çalışma alanı seçicileri + üyelik listesi için context'leri yükle.
      // Takım listesi güncel kalsın diye force çekilir: organizasyon ekranında
      // yeni takım kurulmuş olabilir.
      this.loadOrganizations()
      this.loadTeams({ force: true })
    },

    handleOrgChange(orgId) {
      if (!orgId || orgId === this.activeOrgId) return
      // Organizasyon değişince useTeamContext watcher'ı aktif takımı o
      // organizasyonun ilk takımına taşır — burada ekstra iş gerekmez.
      this.selectOrg(orgId)
    },

    handleTeamChange(teamId) {
      if (!teamId) return
      this.selectTeam(teamId)
    },

    async handleDisplayNameChange() {
      if (!this.displayName.trim() || this.displayName === this.originalDisplayName) {
        return
      }

      this.isUpdatingDisplayName = true

      try {
        await apiUpdateName(this.displayName.trim())

        createToast('Display name updated successfully', {
          type: 'success',
          position: 'top-center'
        })

        // Tüm takımlardaki display name'i backend üzerinden güncelle
        await updateDisplayNameAcrossTeams(this.displayName.trim())

        this.originalDisplayName = this.displayName.trim()
      } catch (error) {
        createToast('Error updating display name', {
          type: 'error',
          position: 'top-center'
        })
      } finally {
        this.isUpdatingDisplayName = false
      }
    },

    async handlePasswordChange() {
      if (!this.password.trim()) {
        return
      }

      // Basic password validation
      if (this.password.length < 8) {
        createToast('Password must be at least 8 characters', {
          type: 'warning',
          position: 'top-center'
        })
        return
      }

      this.isUpdatingPassword = true

      try {
        await apiChangePassword(this.currentPassword, this.password)
        createToast('Password updated successfully', {
          type: 'success',
          position: 'top-center'
        })
        this.password = ''
        this.currentPassword = ''
      } catch (error) {
        const msg = error.response?.data?.error || 'Error updating password'
        createToast(msg, {
          type: 'error',
          position: 'top-center'
        })
      } finally {
        this.isUpdatingPassword = false
      }
    },

    async handleLeaveTeam(team) {
      if (this.isLeavingTeam === team.id) {
        return
      }

      this.isLeavingTeam = team.id

      try {
        await removeMember(team.id, this.email)

        createToast('Successfully left the team', {
          type: 'success',
          position: 'top-center'
        })

        // Merkezi context tazelenir; ayrılınan takım aktif takımsa context
        // kendi çözümlemesiyle kalan takımlardan birine geçer.
        await this.loadTeams({ force: true })

      } catch (error) {
        createToast('Error leaving team', {
          type: 'error',
          position: 'top-center'
        })
      } finally {
        this.isLeavingTeam = null
      }
    }
  }
}
</script>
