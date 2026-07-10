<template>
  <div class="w-full px-4 lg:px-6 py-3 lg:py-4 mx-auto bg-white border-b border-gray-200 shadow-sm z-[9999]">
    <div class="flex items-center justify-between mx-auto">
      <!-- Logo/Home Link -->
      <RouterLink to="/" class="hidden lg:flex items-center gap-3 text-xl lg:text-2xl font-bold text-gray-900 hover:text-blue-600 transition-colors">
        <div class="w-8 h-8 lg:w-10 lg:h-10 bg-blue-100 rounded-lg flex items-center justify-center">
          <svg class="w-5 h-5 lg:w-6 lg:h-6 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
            <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 18V6h16v12H4z"/>
            <path d="M6 8h2v2H6zm0 3h2v2H6zm3-3h2v2H9zm0 3h2v2H9zm3-3h6v2h-6zm0 3h4v2h-4z"/>
            <circle cx="17" cy="14" r="1.5"/>
            <path d="M6 15l2-2 2 2"/>
          </svg>
        </div>
        <span>ScrumTools</span>
      </RouterLink>

      <!-- Mobile Logo with Dropdown -->
      <div v-if="isLogged" class="lg:hidden relative">
        <button
          @click="toggleMobileLogo"
          class="flex items-center gap-2 text-lg font-bold text-gray-900 hover:text-blue-600 transition-colors">
          <div class="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
              <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 18V6h16v12H4z"/>
              <path d="M6 8h2v2H6zm0 3h2v2H6zm3-3h2v2H9zm0 3h2v2H9zm3-3h6v2h-6zm0 3h4v2h-4z"/>
              <circle cx="17" cy="14" r="1.5"/>
              <path d="M6 15l2-2 2 2"/>
            </svg>
          </div>
          <span>ScrumTools</span>
          <svg class="w-4 h-4 text-blue-600 transition-transform" :class="{'rotate-180': showMobileLogo}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
          </svg>
        </button>

        <!-- Mobile Logo Dropdown -->
        <div v-if="showMobileLogo"
             class="absolute left-0 top-full mt-2 w-64 bg-white rounded-xl shadow-lg border border-gray-200 z-50 overflow-hidden">
          <div class="p-4">
            <!-- Home Button -->
            <div class="mb-4">
              <button
                  @click="gotoHome"
                  class="group relative flex items-center justify-center w-full px-4 py-3 rounded-xl transition-all duration-200 hover:bg-gray-50 focus:bg-gray-50 active:bg-gray-100 cursor-pointer border border-gray-200 hover:border-gray-300"
                  type="button">
                <div class="flex items-center gap-2">
                  <div class="w-5 h-5 bg-gray-100 rounded-md flex items-center justify-center group-hover:bg-gray-200 transition-colors">
                    <svg class="w-3 h-3 text-gray-600" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"/>
                    </svg>
                  </div>
                  <span class="text-sm font-medium text-gray-700">Home</span>
                </div>
              </button>
            </div>

            <!-- Team Management Section -->
            <div class="space-y-3 mb-4">
              <div class="w-full">
                <TeamList ref="teamList" :teamList="teamList" @select="handleTeamSelect" class="w-full"></TeamList>
              </div>


            <!-- Action Buttons -->
              <!-- Bağış Butonu -->
              <button
                  class="group relative flex items-center justify-center px-4 pr-6 py-1 rounded-xl transition-all duration-200 bg-white/60 hover:bg-yellow-50 focus:bg-yellow-50 active:bg-yellow-100 cursor-pointer border border-4 border-yellow-700 hover:border-yellow-700 focus:ring-2 focus:ring-yellow-400/60 shadow-sm donate-button-anim"
                  type="button"
                  @click="handleOpenDonationModal">
                <div class="flex items-center gap-2">
                  <div class="w-5 h-5 bg-yellow-400 rounded-md flex items-center justify-center group-hover:bg-yellow-500 transition-colors">
                    <svg class="w-3 h-3 text-yellow-800 donate-icon-anim" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z"/>
                    </svg>
                  </div>
                  <span class="text-sm font-semibold text-yellow-800 whitespace-nowrap">Donate</span>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Mobile Logo without Login -->
      <RouterLink v-if="!isLogged" to="/" class="lg:hidden flex items-center gap-2 text-lg font-bold text-gray-900 hover:text-blue-600 transition-colors">
        <div class="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
          <svg class="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
            <!-- Development Tools Icon -->
            <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 18V6h16v12H4z"/>
            <path d="M6 8h2v2H6zm0 3h2v2H6zm3-3h2v2H9zm0 3h2v2H9zm3-3h6v2h-6zm0 3h4v2h-4z"/>
            <circle cx="17" cy="14" r="1.5"/>
            <path d="M6 15l2-2 2 2"/>
          </svg>
        </div>
        <span>ScrumTools</span>
      </RouterLink>

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

      <!-- Desktop Navigation -->
      <div v-if="isLogged" class="hidden lg:flex items-center gap-6">
        <!-- Team Management Section -->
        <div class="flex items-center gap-3">
          <TeamList ref="teamList" :teamList="teamList" @select="handleTeamSelect"></TeamList>
        </div>

        <div class="flex gap-3">
          <!-- Bağış Butonu -->
          <button
              class="group relative flex items-center justify-center px-4 pr-6 py-1 rounded-xl transition-all duration-200 bg-white/60 hover:bg-yellow-50 focus:bg-yellow-50 active:bg-yellow-100 cursor-pointer border border-4 border-yellow-700 hover:border-yellow-700 focus:ring-2 focus:ring-yellow-400/60 shadow-sm donate-button-anim"
              type="button"
              @click="handleOpenDonationModal">
            <div class="flex items-center gap-2">
              <div class="w-5 h-5 bg-yellow-400 rounded-md flex items-center justify-center group-hover:bg-yellow-500 transition-colors">
                <svg class="w-3 h-3 text-yellow-800 donate-icon-anim" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z"/>
                </svg>
              </div>
              <span class="text-sm font-semibold text-yellow-800 whitespace-nowrap">Donate</span>
            </div>
          </button>
        </div>
      </div>

      <!-- Desktop Profile -->
      <div class="hidden lg:flex items-center gap-2">
        <!-- Bildirim Çanı -->
        <NotificationBell v-if="isLogged" />

        <!-- Profile Dropdown -->
        <div v-if="isLogged" class="relative">
          <button
              @click="toggleProfileDropdown"
              class="group relative flex items-center gap-3 p-3 rounded-xl transition-all duration-200 hover:bg-purple-50 focus:bg-purple-50 active:bg-purple-100 cursor-pointer"
              type="button">
            <!-- Avatar with initials -->
            <div class="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center group-hover:bg-purple-200 transition-colors">
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

    </div>

    <!-- Mobile Menu -->
    <div v-if="isLogged && showMobileMenu" class="lg:hidden mt-4 pb-4 border-t border-gray-200 pt-4">
      <div class="space-y-2">
        <div class="flex items-center gap-3 mb-4 p-3 bg-purple-50 rounded-xl">
          <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
            <span class="text-purple-600 font-bold text-base">{{ userInitials }}</span>
          </div>
          <div class="flex flex-col">
            <span class="text-base font-medium text-gray-900">{{ name }}</span>
            <span class="text-sm text-gray-400 text-xs">{{ auth.userEmail.value }}</span>
          </div>
        </div>

        <button @click="gotoTeams" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-blue-50">
          <div class="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center group-hover:bg-blue-200 transition-colors">
            <svg class="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"></path></svg>
          </div>
          <div class="flex flex-col"><span class="font-medium text-gray-900">My Team</span><span class="text-xs text-gray-500">Manage your team</span></div>
        </button>

        <button @click="gotoProfile" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-purple-50">
          <div class="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center group-hover:bg-purple-200 transition-colors">
            <svg class="w-4 h-4 text-purple-600" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"></path></svg>
          </div>
          <div class="flex flex-col"><span class="font-medium text-gray-900">Profil</span><span class="text-xs text-gray-500">Hesap ayarları</span></div>
        </button>

        <button @click="gotoOrganizations" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-indigo-50">
          <div class="w-8 h-8 bg-indigo-100 rounded-lg flex items-center justify-center group-hover:bg-indigo-200 transition-colors">
            <svg class="w-4 h-4 text-indigo-600" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-2a1 1 0 00-1-1H9a1 1 0 00-1 1v2a1 1 0 01-1 1H4a1 1 0 110-2V4zm3 1h2v2H7V5zm2 4H7v2h2V9zm2-4h2v2h-2V5zm2 4h-2v2h2V9z" clip-rule="evenodd"></path></svg>
          </div>
          <div class="flex flex-col"><span class="font-medium text-gray-900">Organizasyonlar</span><span class="text-xs text-gray-500">Projeler & ekipler</span></div>
        </button>

        <button @click="gotoSettings" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-orange-50">
          <div class="w-8 h-8 bg-orange-100 rounded-lg flex items-center justify-center group-hover:bg-orange-200 transition-colors">
            <svg class="w-4 h-4 text-orange-600" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"></path>
            </svg>
          </div>
          <div class="flex flex-col">
            <span class="font-medium text-gray-900">Settings</span>
            <span class="text-xs text-gray-500">App preferences</span>
          </div>
        </button>

          <!-- Bağış Butonu -->
          <button @click="handleOpenDonationModal" class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 bg-white/60 hover:bg-yellow-50 focus:bg-yellow-50 active:bg-yellow-100 border border-yellow-500 hover:border-yellow-600 focus:ring-2 focus:ring-yellow-400/50 shadow-sm donate-button-anim">
            <div class="w-8 h-8 bg-yellow-300 rounded-lg flex items-center justify-center group-hover:bg-yellow-400 transition-colors">
              <svg class="w-4 h-4 text-yellow-700 donate-icon-anim" fill="currentColor" viewBox="0 0 20 20"><path d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z"/></svg>
            </div>
            <div class="flex flex-col"><span class="font-semibold text-yellow-700">Donate</span><span class="text-xs text-yellow-600">Support the project</span></div>
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
    <DonationModal :isVisible="openDonationModal" @close="()=>openDonationModal = false"></DonationModal>
  </div>
</template>

<script>
import TeamList from './team/TeamList.vue'
import DonationModal from "./DonationModal.vue";
import NotificationBell from "./NotificationBell.vue";
import { useAuth } from '../composables/useAuth.js'

export default {
  name: 'Navbar',
  components: {
    DonationModal,
    TeamList,
    NotificationBell,
  },
  props: {
    isLogged: { type: Boolean, default: false },
    name: { type: String, default: '' },
    teamList: { type: Array, default: () => [] },
    selectedTeam: { type: String, default: '' }
  },
  emits: ['logout', 'team-select'],
  setup() {
    const auth = useAuth()
    return { auth }
  },
  data() {
    return {
      showProfileDropdown: false,
      showMobileMenu: false,
      showMobileLogo: false,
      openDonationModal: false,
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
    handleTeamSelect(teamId) {
      this.$emit('team-select', teamId)
    },
    toggleProfileDropdown() {
      this.showProfileDropdown = !this.showProfileDropdown;
      if (this.showProfileDropdown) { this.showMobileMenu = false; this.showMobileLogo = false; }
    },
    toggleMobileMenu() {
      this.showMobileMenu = !this.showMobileMenu;
      if (this.showMobileMenu) { this.showProfileDropdown = false; this.showMobileLogo = false; }
    },
    toggleMobileLogo() {
      this.showMobileLogo = !this.showMobileLogo;
      if (this.showMobileLogo) { this.showProfileDropdown = false; this.showMobileMenu = false; }
    },
    gotoTeams() { this.$router.push('/teams'); this.closeAllMenus(); },
    gotoSettings() { this.$router.push('/settings'); this.closeAllMenus(); },
    gotoProfile() { this.$router.push('/profile'); this.closeAllMenus(); },
    gotoOrganizations() { this.$router.push('/organizations'); this.closeAllMenus(); },
    gotoAdmin() { this.$router.push('/admin'); this.closeAllMenus(); },
    gotoHome() { this.$router.push('/'); this.closeAllMenus(); },
    closeAllMenus() {
      this.showProfileDropdown = false;
      this.showMobileMenu = false;
      this.showMobileLogo = false;
    },
    handleOpenDonationModal() {
      this.openDonationModal = true;
      this.closeAllMenus();
    }
  }
}
</script>

<style scoped>
.donate-icon-anim {
  animation: donate-bounce 1.25s ease-in-out infinite;
  transform-origin: center;
  will-change: transform;
}
@keyframes donate-bounce {
  0%,100% { transform: scale(1); }
  35% { transform: scale(1.28); }
  50% { transform: scale(0.9); }
  65% { transform: scale(1.18); }
  80% { transform: scale(0.97); }
}

.donate-button-anim {
  animation: donate-button-pulse 2.2s ease-in-out infinite;
  transform-origin: center;
  will-change: transform;
}
@keyframes donate-button-pulse {
  0%,100% { transform: scale(1); }
  30% { transform: scale(1.05); }
  45% { transform: scale(0.985); }
  60% { transform: scale(1.04); }
  75% { transform: scale(0.995); }
}
@media (prefers-reduced-motion: reduce) {
  .donate-button-anim, .donate-icon-anim { animation: none !important; }
}
</style>
