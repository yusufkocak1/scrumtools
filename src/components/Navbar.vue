<template>
  <div class="w-screen px-4 lg:px-6 py-4 mx-auto bg-white border-b border-gray-200 shadow-sm z-[9999]">
    <div class="flex items-center justify-between mx-auto">
      <!-- Logo/Home Link -->
      <RouterLink to="/" class="hidden lg:flex items-center gap-3 text-xl lg:text-2xl font-bold text-gray-900 hover:text-blue-600 transition-colors">
        <div class="w-8 h-8 lg:w-10 lg:h-10 bg-blue-100 rounded-lg flex items-center justify-center">
          <svg class="w-5 h-5 lg:w-6 lg:h-6 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
            <!-- Development Tools Icon -->
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
              <!-- Development Tools Icon -->
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
              <div v-if="selectedTeam" class="w-full">
                <InviteToTheTeam :team-id="selectedTeam" class="w-full justify-center"></InviteToTheTeam>
              </div>


            <!-- Action Buttons -->
              <button
                  class="group relative flex items-center justify-center px-4 py-2 rounded-xl transition-all duration-200 hover:bg-green-50 focus:bg-green-50 active:bg-green-100 cursor-pointer border border-green-200 hover:border-green-300"
                  type="button"
                  @click="handleJoinTeam">
                <div class="flex items-center gap-2">
                  <div class="w-5 h-5 bg-green-100 rounded-md flex items-center justify-center group-hover:bg-green-200 transition-colors">
                    <svg class="w-3 h-3 text-green-600" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 016 6H2a6 6 0 016-6zM16 7a1 1 0 10-2 0v1h-1a1 1 0 100 2h1v1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z"></path>
                    </svg>
                  </div>
                  <span class="text-sm font-medium text-green-700 whitespace-nowrap">Join Team</span>
                </div>
              </button>
              <button
                  class="group relative flex items-center justify-center px-4 py-2 rounded-xl transition-all duration-200 hover:bg-blue-50 focus:bg-blue-50 active:bg-blue-100 cursor-pointer border border-blue-200 hover:border-blue-300"
                  type="button"
                  @click="handleCreateTeam">
                <div class="flex items-center gap-2">
                  <div class="w-5 h-5 bg-blue-100 rounded-md flex items-center justify-center group-hover:bg-blue-200 transition-colors">
                    <svg class="w-3 h-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd"></path>
                    </svg>
                  </div>
                  <span class="text-sm font-medium text-blue-700 whitespace-nowrap">Create Team</span>
                </div>
              </button>
              <!-- Bağış Butonu -->
              <button
                  class="group relative flex items-center justify-center px-4 py-2 rounded-xl transition-all duration-200 bg-white/60 hover:bg-yellow-50 focus:bg-yellow-50 active:bg-yellow-100 cursor-pointer border border-yellow-600 hover:border-yellow-700 focus:ring-2 focus:ring-yellow-400/60 shadow-sm"
                  type="button"
                  @click="handleOpenDonationModal">
                <div class="flex items-center gap-2">
                  <div class="w-5 h-5 bg-yellow-400 rounded-md flex items-center justify-center group-hover:bg-yellow-500 transition-colors">
                    <svg class="w-3 h-3 text-yellow-800" fill="currentColor" viewBox="0 0 20 20">
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

      <!-- Mobile Menu Button (only hamburger for profile) -->
      <button
        v-if="isLogged"
        @click="toggleMobileMenu"
        class="lg:hidden flex items-center justify-center p-2 rounded-md text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-colors"
        type="button">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path v-if="!showMobileMenu" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
          <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
        </svg>
      </button>

      <!-- Desktop Navigation -->
      <div v-if="isLogged" class="hidden lg:flex items-center gap-6">
        <!-- Team Management Section -->
        <div class="flex items-center gap-3">
          <TeamList ref="teamList" :teamList="teamList" @select="handleTeamSelect"></TeamList>
          <InviteToTheTeam v-if="selectedTeam" :team-id="selectedTeam"></InviteToTheTeam>
        </div>

        <div class="flex gap-3">
          <button
              class="group relative flex items-center justify-center px-4 py-2 rounded-xl transition-all duration-200 hover:bg-green-50 focus:bg-green-50 active:bg-green-100 cursor-pointer border border-green-200 hover:border-green-300"
              type="button"
              @click="handleJoinTeam">
            <div class="flex items-center gap-2">
              <div class="w-5 h-5 bg-green-100 rounded-md flex items-center justify-center group-hover:bg-green-200 transition-colors">
                <svg class="w-3 h-3 text-green-600" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M8 9a3 3 0 100-6 3 3 0 000 6zM8 11a6 6 0 016 6H2a6 6 0 016-6zM16 7a1 1 0 10-2 0v1h-1a1 1 0 100 2h1v1a1 1 0 102 0v-1h1a1 1 0 100-2h-1V7z"></path>
                </svg>
              </div>
              <span class="text-sm font-medium text-green-700 whitespace-nowrap">Join Team</span>
            </div>
          </button>
          <button
              class="group relative flex items-center justify-center px-4 py-2 rounded-xl transition-all duration-200 hover:bg-blue-50 focus:bg-blue-50 active:bg-blue-100 cursor-pointer border border-blue-200 hover:border-blue-300"
              type="button"
              @click="handleCreateTeam">
            <div class="flex items-center gap-2">
              <div class="w-5 h-5 bg-blue-100 rounded-md flex items-center justify-center group-hover:bg-blue-200 transition-colors">
                <svg class="w-3 h-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd"></path>
                </svg>
              </div>
              <span class="text-sm font-medium text-blue-700 whitespace-nowrap">Create Team</span>
            </div>
          </button>
          <!-- Bağış Butonu -->
          <button
              class="group relative flex items-center justify-center px-4 py-2 rounded-xl transition-all duration-200 bg-white/60 hover:bg-yellow-50 focus:bg-yellow-50 active:bg-yellow-100 cursor-pointer border border-yellow-600 hover:border-yellow-700 focus:ring-2 focus:ring-yellow-400/60 shadow-sm"
              type="button"
              @click="handleOpenDonationModal">
            <div class="flex items-center gap-2">
              <div class="w-5 h-5 bg-yellow-400 rounded-md flex items-center justify-center group-hover:bg-yellow-500 transition-colors">
                <svg class="w-3 h-3 text-yellow-800" fill="currentColor" viewBox="0 0 20 20">
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
      <!-- Mobile Profile Menu -->
      <div class="space-y-2">
        <div class="flex items-center gap-3 mb-4 p-3 bg-purple-50 rounded-xl">
          <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
            <span class="text-purple-600 font-bold text-base">{{ userInitials }}</span>
          </div>
          <div class="flex flex-col">
            <span class="text-base font-medium text-gray-900">{{ name }}</span>
            <span class="text-sm text-gray-500">Profile</span>
          </div>
        </div>

        <button
            @click="gotoTeams"
            class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-blue-50">
          <div class="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center group-hover:bg-blue-200 transition-colors">
            <svg class="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
              <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"></path>
            </svg>
          </div>
          <div class="flex flex-col">
            <span class="font-medium text-gray-900">My Team</span>
            <span class="text-xs text-gray-500">Manage your team</span>
          </div>
        </button>

        <button
            @click="gotoSettings"
            class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-orange-50">
          <div class="w-8 h-8 bg-orange-100 rounded-lg flex items-center justify-center group-hover:bg-orange-200 transition-colors">
            <svg class="w-4 h-4 text-orange-600" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"></path>
            </svg>
          </div>
          <div class="flex flex-col">
            <span class="font-medium text-gray-900">Settings</span>
            <span class="text-xs text-gray-500">App preferences</span>
          </div>
        </button>

        <!-- Bağış Butonu -->
        <button
            @click="handleOpenDonationModal"
            class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 bg-white/60 hover:bg-yellow-50 focus:bg-yellow-50 active:bg-yellow-100 border border-yellow-500 hover:border-yellow-600 focus:ring-2 focus:ring-yellow-400/50 shadow-sm">
          <div class="w-8 h-8 bg-yellow-300 rounded-lg flex items-center justify-center group-hover:bg-yellow-400 transition-colors">
            <svg class="w-4 h-4 text-yellow-700" fill="currentColor" viewBox="0 0 20 20">
              <path d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z"/>
            </svg>
          </div>
          <div class="flex flex-col">
            <span class="font-semibold text-yellow-700">Donate</span>
            <span class="text-xs text-yellow-600">Support the project</span>
          </div>
        </button>

        <hr class="my-2 border-gray-100">

        <button
            @click="handleLogout"
            class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-red-50">
          <div class="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center group-hover:bg-red-200 transition-colors">
            <svg class="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 102 0V4a1 1 0 00-1-1zm10.293 9.293a1 1 0 001.414 1.414l3-3a1 1 0 000-1.414l-3-3a1 1 0 10-1.414 1.414L14.586 9H7a1 1 0 100 2h7.586l-1.293 1.293z" clip-rule="evenodd"></path>
            </svg>
          </div>
          <div class="flex flex-col">
            <span class="font-medium text-red-600">Logout</span>
            <span class="text-xs text-red-400">Sign out of account</span>
          </div>
        </button>
      </div>
    </div>

    <!-- Mobile Profile Dropdown (when opened via profile icon) -->
    <div v-if="isLogged && showProfileDropdown && !showMobileMenu"
         class="lg:hidden absolute left-4 right-4 mt-2 bg-white rounded-xl shadow-lg border border-gray-200 z-50 overflow-hidden">
      <div class="p-4">
        <div class="flex items-center gap-3 mb-4 p-3 bg-purple-50 rounded-xl">
          <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
            <span class="text-purple-600 font-bold text-base">{{ userInitials }}</span>
          </div>
          <div class="flex flex-col">
            <span class="text-base font-medium text-gray-900">{{ name }}</span>
            <span class="text-sm text-gray-500">Profile</span>
          </div>
        </div>

        <div class="space-y-2">
          <button
              @click="gotoTeams"
              class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-blue-50">
            <div class="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center group-hover:bg-blue-200 transition-colors">
              <svg class="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"></path>
              </svg>
            </div>
            <div class="flex flex-col">
              <span class="font-medium text-gray-900">My Team</span>
              <span class="text-xs text-gray-500">Manage your team</span>
            </div>
          </button>

          <button
              @click="gotoSettings"
              class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-orange-50">
            <div class="w-8 h-8 bg-orange-100 rounded-lg flex items-center justify-center group-hover:bg-orange-200 transition-colors">
              <svg class="w-4 h-4 text-orange-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"></path>
            </svg>
            </div>
            <div class="flex flex-col">
              <span class="font-medium text-gray-900">Settings</span>
              <span class="text-xs text-gray-500">App preferences</span>
            </div>
          </button>

          <!-- Bağış Butonu -->
          <button
              @click="handleOpenDonationModal"
              class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 bg-white/60 hover:bg-yellow-50 focus:bg-yellow-50 active:bg-yellow-100 border border-yellow-500 hover:border-yellow-600 focus:ring-2 focus:ring-yellow-400/50 shadow-sm">
            <div class="w-8 h-8 bg-yellow-300 rounded-lg flex items-center justify-center group-hover:bg-yellow-400 transition-colors">
              <svg class="w-4 h-4 text-yellow-700" fill="currentColor" viewBox="0 0 20 20">
                <path d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z"/>
              </svg>
            </div>
            <div class="flex flex-col">
              <span class="font-semibold text-yellow-700">Donate</span>
              <span class="text-xs text-yellow-600">Support the project</span>
            </div>
          </button>

          <hr class="my-2 border-gray-100">

          <button
              @click="handleLogout"
              class="group flex items-center gap-3 w-full px-4 py-3 text-left rounded-xl transition-all duration-200 hover:bg-red-50">
            <div class="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center group-hover:bg-red-200 transition-colors">
              <svg class="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 102 0V4a1 1 0 00-1-1zm10.293 9.293a1 1 0 001.414 1.414l3-3a1 1 0 000-1.414l-3-3a1 1 0 10-1.414 1.414L14.586 9H7a1 1 0 100 2h7.586l-1.293 1.293z" clip-rule="evenodd"></path>
              </svg>
            </div>
            <div class="flex flex-col">
              <span class="font-medium text-red-600">Logout</span>
              <span class="text-xs text-red-400">Sign out of account</span>
            </div>
          </button>
        </div>
      </div>
    </div>
    <DonationModal :isVisible="openDonationModal" @close="()=>openDonationModal = false"></DonationModal>
  </div>
</template>

<script>
import TeamList from './team/TeamList.vue'
import InviteToTheTeam from './team/InviteToTheTeam.vue'
import DonationModal from "./DonationModal.vue";

export default {
  name: 'Navbar',
  components: {
    DonationModal,
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
      showProfileDropdown: false,
      showMobileMenu: false,
      showMobileLogo: false,
      openDonationModal: false,
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
      this.showMobileMenu = false;
      this.showMobileLogo = false;
      this.$emit('logout')
    },
    handleTeamSelect(teamId) {
      this.$emit('team-select', teamId)
    },
    handleJoinTeam() {
      this.showMobileMenu = false;
      this.showMobileLogo = false;
      this.$emit('join-team')
    },
    handleCreateTeam() {
      this.showMobileMenu = false;
      this.showMobileLogo = false;
      this.$emit('create-team')
    },
    toggleProfileDropdown() {
      this.showProfileDropdown = !this.showProfileDropdown;
      if (this.showProfileDropdown) {
        this.showMobileMenu = false;
        this.showMobileLogo = false;
      }
    },
    toggleMobileMenu() {
      this.showMobileMenu = !this.showMobileMenu;
      if (this.showMobileMenu) {
        this.showProfileDropdown = false;
        this.showMobileLogo = false;
      }
    },
    toggleMobileLogo() {
      this.showMobileLogo = !this.showMobileLogo;
      if (this.showMobileLogo) {
        this.showProfileDropdown = false;
        this.showMobileMenu = false;
      }
    },
    gotoTeams() {
      this.$router.push('/teams');
      this.showProfileDropdown = false;
      this.showMobileMenu = false;
      this.showMobileLogo = false;
    },
    gotoSettings() {
      this.$router.push('/settings');
      this.showProfileDropdown = false;
      this.showMobileMenu = false;
      this.showMobileLogo = false;
    },
    gotoHome() {
      this.$router.push('/');
      this.showProfileDropdown = false;
      this.showMobileMenu = false;
      this.showMobileLogo = false;
    },
    handleOpenDonationModal() {
      this.openDonationModal = true;
      // Diğer menüleri kapat
      this.showProfileDropdown = false;
      this.showMobileMenu = false;
      this.showMobileLogo = false;
    }
  }
}
</script>
