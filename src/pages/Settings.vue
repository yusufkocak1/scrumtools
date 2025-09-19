<template>
  <div class="flex flex-row w-screen min-h-screen bg-gray-50">
    <SideBar :team-id="selectedTeam" class="hidden lg:flex" />
    <div class="flex-1 p-6">
      <div class="max-w-4xl mx-auto">
        <!-- Header -->
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">Ayarlar</h1>
          <p class="text-gray-600 mt-2">Profil bilgilerinizi ve güvenlik ayarlarınızı yönetin</p>
        </div>

        <!-- Profile Section -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <h2 class="text-xl font-semibold text-gray-800">Profil Bilgileri</h2>
          </div>

          <div class="p-6 space-y-6">
            <!-- Email Display -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                E-posta Adresi
              </label>
              <div class="px-4 py-3 bg-gray-50 rounded-lg border">
                <span class="text-gray-900 font-medium">{{ email?.toLowerCase() }}</span>
              </div>
              <p class="text-xs text-gray-500">E-posta adresiniz değiştirilemez</p>
            </div>

            <!-- Display Name -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Görünen İsim
              </label>
              <div class="flex gap-3">
                <input
                  v-model="displayName"
                  :disabled="isUpdatingDisplayName"
                  class="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-50 disabled:cursor-not-allowed transition-colors"
                  placeholder="Görünen isminizi girin"
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
                  {{ isUpdatingDisplayName ? 'Güncelleniyor...' : 'Güncelle' }}
                </button>
              </div>
            </div>

            <!-- Password -->
            <div class="space-y-2">
              <label class="block text-sm font-medium text-gray-700">
                Yeni Şifre
              </label>
              <div class="flex gap-3">
                <div class="flex-1 relative">
                  <input
                    v-model="password"
                    :type="showPassword ? 'text' : 'password'"
                    :disabled="isUpdatingPassword"
                    class="w-full px-4 py-3 pr-12 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-50 disabled:cursor-not-allowed transition-colors"
                    placeholder="Yeni şifrenizi girin"
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
                  :disabled="!password.trim() || isUpdatingPassword"
                  class="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
                  @click="handlePasswordChange"
                >
                  <svg v-if="isUpdatingPassword" class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {{ isUpdatingPassword ? 'Güncelleniyor...' : 'Güncelle' }}
                </button>
              </div>
              <div class="space-y-1 text-xs text-gray-500">
                <p>• En az 8 karakter olmalıdır</p>
                <p>• Büyük ve küçük harf içermelidir</p>
                <p>• En az bir rakam içermelidir</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Team Information -->
        <div v-if="teamList.length > 0" class="mt-8 bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <h2 class="text-xl font-semibold text-gray-800">Takım Bilgileri</h2>
          </div>
          <div class="p-6">
            <div class="space-y-3">
              <p class="text-sm text-gray-600">Üye olduğunuz takımlar:</p>
              <div class="grid gap-3">
                <div
                  v-for="team in teamList"
                  :key="team.id"
                  class="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
                >
                  <div>
                    <h3 class="font-medium text-gray-900">{{ team.teamName }}</h3>
                    <p class="text-sm text-gray-500">{{ team.memberEmails?.length || 0 }} üye</p>
                  </div>
                  <div class="flex items-center gap-3">
                    <span
                      v-if="team.id === selectedTeam"
                      class="px-3 py-1 bg-green-100 text-green-800 text-xs font-medium rounded-full"
                    >
                      Aktif
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
                      {{ isLeavingTeam === team.id ? 'Çıkılıyor...' : 'Çık' }}
                    </button>
                    <span
                      v-else
                      class="px-3 py-2 bg-gray-100 text-gray-500 text-sm rounded-lg cursor-not-allowed"
                      title="Admin olduğunuz takımdan çıkamazsınız"
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
import { getAuth } from 'firebase/auth'
import { changePassword, updateDisplayName } from '../firebase/AuthService.js'
import { createToast } from 'mosha-vue-toastify'
import { listenTeams, updateDisplayNameFromTeam, removeUserFromTeam } from '../firebase/TeamService.js'
import SideBar from '../components/SideBar.vue'

export default {
  name: 'Settings',
  components: {
    SideBar
  },
  data() {
    return {
      displayName: '',
      originalDisplayName: '',
      email: '',
      password: '',
      teamList: [],
      selectedTeam: '',
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
    initializeData() {
      const user = getAuth().currentUser
      if (user) {
        this.displayName = user.displayName || ''
        this.originalDisplayName = user.displayName || ''
        this.email = user.email || ''
      }

      // Get selected team from localStorage
      const storedTeam = localStorage.getItem('selectedTeam')
      if (storedTeam) {
        this.selectedTeam = storedTeam
      }

      // Listen to teams
      listenTeams((response) => {
        this.teamList = response || []
      })
    },

    async handleDisplayNameChange() {
      if (!this.displayName.trim() || this.displayName === this.originalDisplayName) {
        return
      }

      this.isUpdatingDisplayName = true

      try {
        await new Promise((resolve, reject) => {
          updateDisplayName(this.displayName.trim(), (response) => {
            if (typeof response === 'string' && !response.includes('error')) {
              createToast('Görünen isim başarıyla güncellendi', {
                type: 'success',
                position: 'top-center'
              })

              // Update display name in all teams
              const user = getAuth().currentUser
              if (user?.email) {
                this.teamList.forEach(team => {
                  updateDisplayNameFromTeam(team.id, this.displayName.trim(), user.email)
                })
              }

              this.originalDisplayName = this.displayName.trim()
              resolve(response)
            } else {
              reject(new Error(response))
            }
          })
        })
      } catch (error) {
        createToast('Görünen isim güncellenirken hata oluştu', {
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
        createToast('Şifre en az 8 karakter olmalıdır', {
          type: 'warning',
          position: 'top-center'
        })
        return
      }

      this.isUpdatingPassword = true

      try {
        await new Promise((resolve, reject) => {
          changePassword(this.password, (response) => {
            if (typeof response === 'string' && !response.includes('error')) {
              createToast('Şifre başarıyla güncellendi', {
                type: 'success',
                position: 'top-center'
              })
              this.password = ''
              resolve(response)
            } else {
              reject(new Error(response))
            }
          })
        })
      } catch (error) {
        createToast('Şifre güncellenirken hata oluştu', {
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
        await removeUserFromTeam(team.id, this.email)

        createToast('Takımdan başarıyla çıkıldı', {
          type: 'success',
          position: 'top-center'
        })

        // If leaving the currently selected team, switch to another team or clear selection
        if (this.selectedTeam === team.id) {
          const remainingTeams = this.teamList.filter(t => t.id !== team.id)
          if (remainingTeams.length > 0) {
            this.selectedTeam = remainingTeams[0].id
            localStorage.setItem('selectedTeam', this.selectedTeam)
          } else {
            this.selectedTeam = ''
            localStorage.removeItem('selectedTeam')
          }
        }

      } catch (error) {
        createToast('Takımdan çıkarken hata oluştu', {
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
