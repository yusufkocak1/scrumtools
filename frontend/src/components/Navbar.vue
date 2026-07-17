<template>
  <div class="relative w-full bg-white border-b border-gray-200 shadow-sm z-[9999]">
    <div class="flex items-center justify-between px-4 lg:px-6 py-3">
      <!-- Logo -->
      <RouterLink to="/" @click="closeAllMenus"
                  class="flex items-center gap-2 lg:gap-3 text-lg lg:text-2xl font-bold text-gray-900 hover:text-blue-600 transition-colors">
        <div class="w-8 h-8 lg:w-10 lg:h-10 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0">
          <svg class="w-5 h-5 lg:w-6 lg:h-6 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
            <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 18V6h16v12H4z"/>
            <path d="M6 8h2v2H6zm0 3h2v2H6zm3-3h2v2H9zm0 3h2v2H9zm3-3h6v2h-6zm0 3h4v2h-4z"/>
            <circle cx="17" cy="14" r="1.5"/>
            <path d="M6 15l2-2 2 2"/>
          </svg>
        </div>
        <span>ScrumTools</span>
      </RouterLink>

      <!-- Desktop: notification bell + profile -->
      <div v-if="isLogged" class="hidden lg:flex items-center gap-2">
        <NotificationBell />

        <!-- Profile Dropdown -->
        <div class="relative">
          <button
              @click="toggleProfileDropdown"
              class="group relative flex items-center gap-3 p-2 rounded-xl transition-all duration-200 hover:bg-purple-50 focus:bg-purple-50 active:bg-purple-100 cursor-pointer"
              type="button">
            <div class="w-9 h-9 bg-purple-100 rounded-lg flex items-center justify-center group-hover:bg-purple-200 transition-colors">
              <span class="text-purple-600 font-bold text-sm">{{ userInitials }}</span>
            </div>
            <div class="flex flex-col items-start">
              <span class="text-sm font-medium text-gray-900">{{ name }}</span>
              <span class="text-xs text-gray-500">Profile</span>
            </div>
            <svg class="w-4 h-4 text-purple-600 transition-transform" :class="{'rotate-180': showProfileDropdown}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
            </svg>
          </button>

          <!-- Desktop Dropdown Menu -->
          <div v-if="showProfileDropdown"
               class="absolute right-0 mt-2 w-56 bg-white rounded-xl shadow-lg border border-gray-200 z-50 overflow-hidden">
            <div class="py-2">
              <!-- Profil Başlığı -->
              <div class="px-4 py-2 border-b border-gray-100">
                <p class="text-xs font-semibold text-gray-900 truncate">{{ name }}</p>
                <p class="text-xs text-gray-500 truncate">{{ auth.userEmail.value }}</p>
              </div>

              <button
                  @click="gotoProfile"
                  class="group flex items-center gap-3 w-full px-4 py-3 text-sm transition-all duration-200 hover:bg-purple-50">
                <div class="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center group-hover:bg-purple-200 transition-colors">
                  <svg class="w-4 h-4 text-purple-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"></path>
                  </svg>
                </div>
                <div class="flex flex-col items-start">
                  <span class="font-medium text-gray-900">Profil</span>
                  <span class="text-xs text-gray-500">Hesap ayarları</span>
                </div>
              </button>

              <button
                  @click="gotoTeams"
                  class="group flex items-center gap-3 w-full px-4 py-3 text-sm transition-all duration-200 hover:bg-blue-50">
                <div class="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center group-hover:bg-blue-200 transition-colors">
                  <svg class="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"></path>
                  </svg>
                </div>
                <div class="flex flex-col items-start">
                  <span class="font-medium text-gray-900">My Team</span>
                  <span class="text-xs text-gray-500">Manage your team</span>
                </div>
              </button>

              <button
                  @click="gotoOrganizations"
                  class="group flex items-center gap-3 w-full px-4 py-3 text-sm transition-all duration-200 hover:bg-indigo-50">
                <div class="w-8 h-8 bg-indigo-100 rounded-lg flex items-center justify-center group-hover:bg-indigo-200 transition-colors">
                  <svg class="w-4 h-4 text-indigo-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-2a1 1 0 00-1-1H9a1 1 0 00-1 1v2a1 1 0 01-1 1H4a1 1 0 110-2V4zm3 1h2v2H7V5zm2 4H7v2h2V9zm2-4h2v2h-2V5zm2 4h-2v2h2V9z" clip-rule="evenodd"></path>
                  </svg>
                </div>
                <div class="flex flex-col items-start">
                  <span class="font-medium text-gray-900">Organizasyonlar</span>
                  <span class="text-xs text-gray-500">Projeler &amp; ekipler</span>
                </div>
              </button>

              <button
                  @click="gotoSettings"
                  class="group flex items-center gap-3 w-full px-4 py-3 text-sm transition-all duration-200 hover:bg-orange-50">
                <div class="w-8 h-8 bg-orange-100 rounded-lg flex items-center justify-center group-hover:bg-orange-200 transition-colors">
                  <svg class="w-4 h-4 text-orange-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"></path>
                  </svg>
                </div>
                <div class="flex flex-col items-start">
                  <span class="font-medium text-gray-900">Settings</span>
                  <span class="text-xs text-gray-500">App preferences</span>
                </div>
              </button>

              <button
                  @click="gotoSupport"
                  class="group flex items-center gap-3 w-full px-4 py-3 text-sm transition-all duration-200 hover:bg-teal-50">
                <div class="w-8 h-8 bg-teal-100 rounded-lg flex items-center justify-center group-hover:bg-teal-200 transition-colors">
                  <svg class="w-4 h-4 text-teal-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z" clip-rule="evenodd"></path>
                  </svg>
                </div>
                <div class="flex flex-col items-start">
                  <span class="font-medium text-gray-900">Destek</span>
                  <span class="text-xs text-gray-500">Sorun &amp; öneri bildir</span>
                </div>
              </button>

              <!-- Admin Paneli — sadece SUPER_ADMIN ve PLATFORM_ADMIN -->
              <button
                  v-if="isSuperAdmin"
                  @click="gotoAdmin"
                  class="group flex items-center gap-3 w-full px-4 py-3 text-sm transition-all duration-200 hover:bg-red-50">
                <div class="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center group-hover:bg-red-200 transition-colors">
                  <svg class="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path>
                  </svg>
                </div>
                <div class="flex flex-col items-start">
                  <span class="font-medium text-red-700">Admin Panel</span>
                  <span class="text-xs text-red-400">Sistem yönetimi</span>
                </div>
              </button>

              <hr class="my-2 border-gray-100">
              <button
                  @click="handleLogout"
                  class="group flex items-center gap-3 w-full px-4 py-3 text-sm transition-all duration-200 hover:bg-red-50">
                <div class="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center group-hover:bg-red-200 transition-colors">
                  <svg class="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 102 0V4a1 1 0 00-1-1zm10.293 9.293a1 1 0 001.414 1.414l3-3a1 1 0 000-1.414l-3-3a1 1 0 10-1.414 1.414L14.586 9H7a1 1 0 100 2h7.586l-1.293 1.293z" clip-rule="evenodd"></path>
                  </svg>
                </div>
                <div class="flex flex-col items-start">
                  <span class="font-medium text-red-600">Logout</span>
                  <span class="text-xs text-red-400">Sign out of account</span>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Mobile: notification bell + hamburger -->
      <div v-if="isLogged" class="lg:hidden flex items-center gap-1">
        <NotificationBell />
        <button
          @click="toggleMobileMenu"
          class="flex items-center justify-center p-2 rounded-md text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-colors"
          type="button">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path v-if="!showMobileMenu" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>
    </div>

    <!-- Mobile Menu (overlay panel — sayfa içeriğini itmez) -->
    <div v-if="isLogged && showMobileMenu"
         class="lg:hidden absolute top-full inset-x-0 bg-white border-b border-gray-200 shadow-lg z-50 max-h-[calc(100vh-4rem)] overflow-y-auto">
      <div class="p-4 space-y-1">
        <div class="flex items-center gap-3 mb-3 p-3 bg-purple-50 rounded-xl">
          <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center flex-shrink-0">
            <span class="text-purple-600 font-bold text-base">{{ userInitials }}</span>
          </div>
          <div class="flex flex-col min-w-0">
            <span class="text-base font-medium text-gray-900 truncate">{{ name }}</span>
            <span class="text-xs text-gray-500 truncate">{{ auth.userEmail.value }}</span>
          </div>
        </div>

        <button @click="gotoProfile" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-purple-50">
          <div class="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center group-hover:bg-purple-200 transition-colors">
            <svg class="w-4 h-4 text-purple-600" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"></path></svg>
          </div>
          <div class="flex flex-col"><span class="font-medium text-gray-900">Profil</span><span class="text-xs text-gray-500">Hesap ayarları</span></div>
        </button>

        <button @click="gotoTeams" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-blue-50">
          <div class="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center group-hover:bg-blue-200 transition-colors">
            <svg class="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"></path></svg>
          </div>
          <div class="flex flex-col"><span class="font-medium text-gray-900">My Team</span><span class="text-xs text-gray-500">Manage your team</span></div>
        </button>

        <button @click="gotoOrganizations" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-indigo-50">
          <div class="w-8 h-8 bg-indigo-100 rounded-lg flex items-center justify-center group-hover:bg-indigo-200 transition-colors">
            <svg class="w-4 h-4 text-indigo-600" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-2a1 1 0 00-1-1H9a1 1 0 00-1 1v2a1 1 0 01-1 1H4a1 1 0 110-2V4zm3 1h2v2H7V5zm2 4H7v2h2V9zm2-4h2v2h-2V5zm2 4h-2v2h2V9z" clip-rule="evenodd"></path></svg>
          </div>
          <div class="flex flex-col"><span class="font-medium text-gray-900">Organizasyonlar</span><span class="text-xs text-gray-500">Projeler & ekipler</span></div>
        </button>

        <button @click="gotoSettings" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-orange-50">
          <div class="w-8 h-8 bg-orange-100 rounded-lg flex items-center justify-center group-hover:bg-orange-200 transition-colors">
            <svg class="w-4 h-4 text-orange-600" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"></path></svg>
          </div>
          <div class="flex flex-col"><span class="font-medium text-gray-900">Settings</span><span class="text-xs text-gray-500">App preferences</span></div>
        </button>

        <button @click="gotoSupport" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-teal-50">
          <div class="w-8 h-8 bg-teal-100 rounded-lg flex items-center justify-center group-hover:bg-teal-200 transition-colors">
            <svg class="w-4 h-4 text-teal-600" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z" clip-rule="evenodd"></path></svg>
          </div>
          <div class="flex flex-col"><span class="font-medium text-gray-900">Destek</span><span class="text-xs text-gray-500">Sorun &amp; öneri bildir</span></div>
        </button>

        <!-- Admin Paneli — sadece SUPER_ADMIN ve PLATFORM_ADMIN -->
        <button v-if="isSuperAdmin" @click="gotoAdmin" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-red-50">
          <div class="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center group-hover:bg-red-200 transition-colors">
            <svg class="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path></svg>
          </div>
          <div class="flex flex-col"><span class="font-medium text-red-700">Admin Panel</span><span class="text-xs text-red-400">Sistem yönetimi</span></div>
        </button>

        <hr class="my-2 border-gray-100">

        <button @click="handleLogout" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-red-50">
          <div class="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center group-hover:bg-red-200 transition-colors">
            <svg class="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 102 0V4a1 1 0 00-1-1zm10.293 9.293a1 1 0 001.414 1.414l3-3a1 1 0 000-1.414l-3-3a1 1 0 10-1.414 1.414L14.586 9H7a1 1 0 100 2h7.586l-1.293 1.293z" clip-rule="evenodd"></path></svg>
          </div>
          <div class="flex flex-col"><span class="font-medium text-red-600">Logout</span><span class="text-xs text-red-400">Sign out of account</span></div>
        </button>
      </div>
    </div>

    <!-- Click-outside backdrop: açık menüleri dışarı tıklamayla kapatır -->
    <div v-if="showProfileDropdown || showMobileMenu" class="fixed inset-0 z-40" @click="closeAllMenus"></div>
  </div>
</template>

<script>
import NotificationBell from "./NotificationBell.vue";
import { useAuth } from '../composables/useAuth.js'

export default {
  name: 'Navbar',
  components: {
    NotificationBell,
  },
  props: {
    isLogged: { type: Boolean, default: false },
    name: { type: String, default: '' }
  },
  emits: ['logout'],
  setup() {
    const auth = useAuth()
    return { auth }
  },
  data() {
    return {
      showProfileDropdown: false,
      showMobileMenu: false,
    }
  },
  computed: {
    userInitials() {
      const n = this.name
      if (!n) return '';
      const words = n.trim().split(' ').filter(word => word.length > 0);
      if (words.length === 1) return words[0].charAt(0).toUpperCase();
      if (words.length === 2) return (words[0].charAt(0) + words[1].charAt(0)).toUpperCase();
      return (words[0].charAt(0) + words[words.length - 1].charAt(0)).toUpperCase();
    },
    isSuperAdmin() { return this.auth.isSuperAdmin.value },
    userAvatarUrl() { return this.auth.avatarUrl.value }
  },
  methods: {
    handleLogout() {
      this.closeAllMenus();
      this.$emit('logout')
    },
    toggleProfileDropdown() {
      this.showProfileDropdown = !this.showProfileDropdown;
      if (this.showProfileDropdown) { this.showMobileMenu = false; }
    },
    toggleMobileMenu() {
      this.showMobileMenu = !this.showMobileMenu;
      if (this.showMobileMenu) { this.showProfileDropdown = false; }
    },
    gotoTeams() { this.$router.push('/teams'); this.closeAllMenus(); },
    gotoSettings() { this.$router.push('/settings'); this.closeAllMenus(); },
    gotoProfile() { this.$router.push('/profile'); this.closeAllMenus(); },
    gotoSupport() { this.$router.push('/support'); this.closeAllMenus(); },
    gotoOrganizations() { this.$router.push('/organizations'); this.closeAllMenus(); },
    gotoAdmin() { this.$router.push('/admin'); this.closeAllMenus(); },
    closeAllMenus() {
      this.showProfileDropdown = false;
      this.showMobileMenu = false;
    }
  }
}
</script>
