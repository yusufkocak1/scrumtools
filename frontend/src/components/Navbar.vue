<template>
  <!-- pt-safe/px-safe: ana ekrana kurulu uygulamada içerik durum çubuğunun ve
       yatay moddaki çentiğin altında kalmasın -->
  <div class="relative w-full bg-white border-b border-gray-200 shadow-sm z-[9999] pt-safe px-safe">
    <div class="flex items-center gap-2 lg:gap-4 px-3 sm:px-4 lg:px-6 py-2.5">
      <!-- Logo -->
      <RouterLink to="/" @click="closeAllMenus"
                  class="flex items-center gap-2 shrink-0 text-lg lg:text-xl font-bold text-gray-900 hover:text-blue-600 transition-colors">
        <div class="w-8 h-8 lg:w-9 lg:h-9 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0">
          <svg class="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
            <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 18V6h16v12H4z"/>
            <path d="M6 8h2v2H6zm0 3h2v2H6zm3-3h2v2H9zm0 3h2v2H9zm3-3h6v2h-6zm0 3h4v2h-4z"/>
            <circle cx="17" cy="14" r="1.5"/>
            <path d="M6 15l2-2 2 2"/>
          </svg>
        </div>
        <!-- Dar ekranda yazı gizlenir; modül ikonlarına yer açar -->
        <span :class="isLogged ? 'hidden sm:inline' : ''">ScrumTools</span>
      </RouterLink>

      <!-- Modül navigasyonu — eski SideBar'ın yerini alır. md altında gizlenir,
           modüllere hamburger menüsünden ulaşılır; sığmazsa yatay kaydırılır. -->
      <nav v-if="isLogged" class="hidden md:flex flex-1 min-w-0 items-center gap-0.5 overflow-x-auto no-scrollbar">
        <button
          v-for="item in navItems"
          :key="item.label"
          @click="item.action()"
          :title="item.label"
          type="button"
          :class="['flex items-center gap-2 shrink-0 px-2.5 xl:px-3 py-2 rounded-lg text-sm font-medium whitespace-nowrap transition-colors',
                   item.active ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900']">
          <svg class="w-4 h-4 shrink-0" fill="currentColor" :viewBox="item.viewBox" v-html="item.paths"></svg>
          <span class="hidden xl:inline">{{ item.label }}</span>
        </button>
      </nav>

      <!-- Desktop: notification bell + profile -->
      <div v-if="isLogged" class="hidden lg:flex items-center gap-2 ml-auto shrink-0">
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
      <div v-if="isLogged" class="lg:hidden flex items-center gap-1 ml-auto shrink-0">
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

        <!-- Modüller — inline navigasyon md altında gizlendiği için burada listelenir -->
        <div class="md:hidden pb-2 mb-2 border-b border-gray-100">
          <p class="px-1 pb-2 text-[11px] font-semibold uppercase tracking-wide text-gray-400">Modüller</p>
          <div class="grid grid-cols-2 gap-1.5">
            <button
              v-for="item in navItems"
              :key="item.label"
              @click="item.action()"
              type="button"
              :class="['flex items-center gap-2 px-3 py-3 rounded-xl text-sm font-medium text-left transition-colors',
                       item.active ? 'bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-100']">
              <svg class="w-4 h-4 shrink-0" fill="currentColor" :viewBox="item.viewBox" v-html="item.paths"></svg>
              <span class="truncate">{{ item.label }}</span>
            </button>
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
import { useTeamContext } from '../composables/useTeamContext.js'

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
    // Takım seçimi merkezi context'ten okunur; takım gerektiren modül linkleri
    // (board, poker, code share, gamebox) aktif takıma göre yönlenir. Yükleme
    // App.vue'da oturum açıkken tetiklenir — navbar ziyaretçide de render
    // edildiği için burada istek atılmaz.
    const { activeTeamId } = useTeamContext()
    return { auth, activeTeamId }
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
    userAvatarUrl() { return this.auth.avatarUrl.value },
    navItems() {
      const path = this.$route.path
      return [
        {
          label: 'Dashboard',
          action: this.gotoDashboard,
          active: path.startsWith('/dashboard'),
          viewBox: '0 0 20 20',
          paths: '<path d="M2 10a8 8 0 018-8v8h8a8 8 0 11-16 0z"/><path d="M12 2.252A8.014 8.014 0 0117.748 8H12V2.252z"/>'
        },
        {
          label: 'Board',
          action: this.gotoWorkList,
          active: path.startsWith('/workList') || path.startsWith('/task/'),
          viewBox: '0 0 20 20',
          paths: '<path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"></path>'
        },
        {
          label: 'Docs',
          action: this.gotoDocs,
          active: path.includes('/docs'),
          viewBox: '0 0 20 20',
          paths: '<path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/>'
        },
        {
          label: 'Ortak Çalışma',
          action: this.gotoCollab,
          active: path.includes('/collab'),
          viewBox: '0 0 20 20',
          paths: '<path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd"></path>'
        },
        {
          label: 'Retrospective',
          action: this.gotoRetrospective,
          active: path.startsWith('/retrospective') || path.startsWith('/retroBoard'),
          viewBox: '0 0 24 24',
          paths: '<path d="M16 4c0-1.11.89-2 2-2s2 .89 2 2-.89 2-2 2-2-.89-2-2zm4 18v-6h2.5l-2.54-7.63A1.5 1.5 0 0 0 18.54 7H16c-.8 0-1.54.37-2 .95L12.58 9.7c-.35.47-.98.75-1.64.75H9.5a2 2 0 0 0-2 2v1a1 1 0 0 0 1 1H10v8h2v-8h.5c.3 0 .6-.1.85-.29L14.7 12H16l2.05 6H20z"/><path d="M12.5 11.5c.83 0 1.5-.67 1.5-1.5s-.67-1.5-1.5-1.5S11 9.17 11 10s.67 1.5 1.5 1.5z"/><path d="M5.5 6c1.11 0 2-.89 2-2s-.89-2-2-2-2 .89-2 2 .89 2 2 2zm1.5 1h-3c-.83 0-1.54.5-1.84 1.22L.66 12.08c-.11.26-.16.54-.16.82 0 1.11.89 2 2 2h2.5v7h3v-7h1l1.31-3.92C10.69 9.77 9.39 7 5.5 7z"/><path d="M9 14l-2 2h6l-2-2H9z"/>'
        },
        {
          label: 'Scrum Poker',
          action: this.gotoScrumPoker,
          active: path.startsWith('/scrumPoker'),
          viewBox: '0 0 512 512',
          paths: '<path d="M452.279,114.361L298.254,73.25V38.138C298.254,17.109,282.291,0,261.203,0H70.512C49.425,0,31.076,17.114,31.076,38.152 v334.007c0,21.037,18.307,38.151,39.345,38.151h66.145l-1.146,4.276c-2.641,9.855-1.275,20.145,3.845,28.977 c5.112,8.819,13.359,15.129,23.223,17.772l184.192,49.354c3.31,0.886,6.637,1.31,9.911,1.31c16.873,0,32.344-11.246,36.905-28.264 l86.149-322.626C485.088,140.79,472.648,119.819,452.279,114.361z M141.679,391.226H70.421c-10.514,0-20.261-8.553-20.261-19.067 V38.152c0-10.514,9.788-19.068,20.353-19.068h190.691c10.565,0,17.967,8.547,17.967,19.054v29.998l-11.084-3.13 c-20.359-5.448-41.071,6.633-46.518,26.952l-5.493,21.059L182.84,70.456c-1.808-2.311-4.543-3.661-7.477-3.661 s-5.688,1.35-7.496,3.661L81.997,180.19c-2.703,3.455-2.699,8.307,0.005,11.762l84.231,107.626L141.679,391.226z M210.075,135.966 L172.417,276.51l-70.779-90.439L175.4,91.819l33.99,43.432C209.598,135.517,209.846,135.728,210.075,135.966z M461.508,156.171 l-86.447,322.626c-2.72,10.157-13.236,16.192-23.443,13.458l-184.192-49.354c-4.952-1.327-9.089-4.49-11.652-8.91 c-2.557-4.41-3.239-9.548-1.921-14.467l86.447-322.627c2.721-10.155,13.239-16.191,23.443-13.457l184.193,49.354 C458.14,135.529,464.229,146.016,461.508,156.171z"/><path d="M365.115,212.306c-13.618-3.652-28.135-1.539-40.214,5.448c-6.97-12.091-18.484-21.176-32.103-24.826 c-28.343-7.596-57.68,9.658-65.398,38.457c-4.46,16.647,9.303,46.354,21.635,68.343c6.121,10.914,27.125,46.802,40.406,50.361 c1.045,0.28,2.23,0.41,3.535,0.41c15.274,0,46.727-17.931,56.639-23.82c21.675-12.877,48.448-31.722,52.908-48.37 C410.24,249.51,393.46,219.901,365.115,212.306z M384.089,273.37c-1.558,5.817-15.315,19.398-41.834,35.47 c-23.175,14.044-42.023,21.591-47.803,22.552c-4.525-3.722-17.075-19.681-30.122-43.431 c-14.932-27.178-20.054-45.819-18.496-51.636c4.203-15.685,18.224-26.101,33.4-26.101c2.853,0,5.748,0.368,8.625,1.139 c11.634,3.117,20.551,12.044,23.85,23.88c0.906,3.251,3.462,5.781,6.722,6.655c3.255,0.876,6.737-0.04,9.148-2.402 c8.774-8.601,20.962-11.878,32.596-8.755C378.356,235.611,389.082,254.735,384.089,273.37z"/>'
        },
        {
          label: 'GameBox',
          action: this.gotoGameBox,
          active: path.startsWith('/quiz'),
          viewBox: '0 0 20 20',
          paths: '<rect x="3" y="3" width="14" height="14" rx="3" ry="3" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="7" cy="7" r="1.2"/><circle cx="13" cy="7" r="1.2"/><circle cx="10" cy="10" r="1.2"/><circle cx="7" cy="13" r="1.2"/><circle cx="13" cy="13" r="1.2"/>'
        }
      ]
    }
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
    // Takım gerektiren modüller: aktif takım yoksa seçim yapılabilsin diye
    // Ayarlar'daki Çalışma Alanı bölümüne yönlendirilir.
    pushWithTeam(path) {
      this.closeAllMenus();
      if (!this.activeTeamId) {
        this.$router.push('/settings');
        return;
      }
      this.$router.push(`${path}/${this.activeTeamId}`);
    },
    gotoDashboard() { this.$router.push('/dashboard'); this.closeAllMenus(); },
    gotoWorkList() { this.pushWithTeam('/workList'); },
    gotoScrumPoker() { this.pushWithTeam('/scrumPoker'); },
    gotoGameBox() { this.pushWithTeam('/quiz'); },
    gotoRetrospective() { this.$router.push('/retrospective'); this.closeAllMenus(); },
    gotoDocs() {
      this.closeAllMenus();
      const lastProjectId = localStorage.getItem('docs_last_project_id');
      this.$router.push(lastProjectId ? `/projects/${lastProjectId}/docs` : '/docs');
    },
    // Ortak çalışma alanı proje kapsamlıdır (plan D4). Proje hatırlanmıyorsa
    // kullanıcı organizasyon ekranından seçsin — takım seçimiyle eşlenemiyor.
    gotoCollab() {
      this.closeAllMenus();
      const lastProjectId = localStorage.getItem('collab_last_project_id')
          || localStorage.getItem('docs_last_project_id');
      this.$router.push(lastProjectId ? `/projects/${lastProjectId}/collab` : '/organizations');
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
