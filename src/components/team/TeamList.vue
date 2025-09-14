<template>
  <div class="relative">
    <!-- Compact Team Selector for Navbar -->
    <button
      @click="toggleDropdown"
      class="flex items-center gap-2 px-3 py-2 bg-gray-50 hover:bg-gray-100 border border-gray-200 hover:border-gray-300 rounded-xl transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
      :class="{ 'ring-2 ring-blue-500 border-transparent bg-blue-50': isOpen }"
    >
      <div class="flex items-center gap-2">
        <!-- Team Avatar -->
        <div class="w-7 h-7 bg-gradient-to-r from-green-400 to-blue-500 rounded-lg flex items-center justify-center">
          <span class="text-white text-xs font-medium">
            {{ selectedTeamData?.teamName?.charAt(0)?.toUpperCase() || 'T' }}
          </span>
        </div>

        <!-- Team Info - Show on all screen sizes -->
        <div class="flex-1 min-w-0">
          <div class="text-sm font-medium text-gray-900 truncate">
            {{ selectedTeamData?.teamName || 'Select Team' }}
          </div>
        </div>
      </div>

      <!-- Dropdown Arrow -->
      <svg
        class="w-4 h-4 text-gray-400 transition-transform duration-200"
        :class="{ 'rotate-180': isOpen }"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
      </svg>
    </button>

    <!-- Dropdown Menu -->
    <transition
      enter-active-class="transition ease-out duration-200"
      enter-from-class="opacity-0 translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition ease-in duration-150"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 translate-y-1"
    >
      <div
        v-if="isOpen"
        class="absolute right-0 z-50 w-72 mt-2 bg-white border border-gray-200 rounded-xl shadow-lg overflow-hidden"
      >

        <!-- Teams List -->
        <div class="max-h-60 overflow-y-auto">
          <div
            v-for="team in teamList"
            :key="team.id"
            @click="selectTeam(team)"
            class="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 cursor-pointer transition-colors duration-150"
            :class="{ 'bg-blue-50 border-r-2 border-blue-500': selectedTeam === team.id }"
          >
            <!-- Team Avatar -->
            <div class="w-8 h-8 bg-gradient-to-r from-green-400 to-blue-500 rounded-lg flex items-center justify-center flex-shrink-0">
              <span class="text-white text-sm font-medium">
                {{ team.teamName?.charAt(0)?.toUpperCase() }}
              </span>
            </div>

            <!-- Team Info -->
            <div class="flex-1 min-w-0">
              <div class="font-medium text-gray-900 truncate">{{ team.teamName }}</div>
            </div>

            <!-- Selected Indicator -->
            <div v-if="selectedTeam === team.id" class="text-blue-500 flex-shrink-0">
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
              </svg>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-if="teamList.length === 0" class="px-4 py-8 text-center text-gray-500">
          <svg class="w-8 h-8 mx-auto mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
          </svg>
          <p class="text-sm">No teams available</p>
          <p class="text-xs text-gray-400 mt-1">Create a team to get started</p>
        </div>
      </div>
    </transition>

    <!-- Click Outside Handler -->
    <div v-if="isOpen" class="fixed inset-0 z-40" @click="closeDropdown"></div>
  </div>
</template>

<script>
export default {
  name: "TeamList",
  props: {
    teamList: {
      type: Array,
      default: () => []
    },
  },
  data() {
    return {
      selectedTeam: "",
      previousTeamId: "",
      isOpen: false
    }
  },
  computed: {
    selectedTeamData() {
      return this.teamList.find(team => team.id === this.selectedTeam) || null;
    }
  },
  watch: {
    teamList: {
      handler() {
        this.initializeSelection();
      },
      immediate: true
    }
  },
  created() {
    this.initializeSelection();
  },
  methods: {
    initializeSelection() {
      if (this.teamList.length > 0) {
        const savedTeam = localStorage.getItem("selectedTeam");
        if (savedTeam && this.teamList.find(team => team.id === savedTeam)) {
          this.selectedTeam = savedTeam;
        } else {
          this.selectedTeam = this.teamList[0].id;
        }
        this.previousTeamId = this.selectedTeam;

        // İlk yüklemede emit et
        this.$nextTick(() => {
          this.$emit('select', this.selectedTeam);
        });
      }
    },

    toggleDropdown() {
      this.isOpen = !this.isOpen;
    },

    closeDropdown() {
      this.isOpen = false;
    },

    selectTeam(team) {
      if (this.selectedTeam === team.id) {
        this.closeDropdown();
        return;
      }

      this.selectedTeam = team.id;
      this.closeDropdown();
      this.handleTeamChange();
    },

    handleTeamChange() {
      // Eğer önceki seçim yoksa (ilk kez seçim) direkt emit et
      if (!this.previousTeamId || this.previousTeamId === this.selectedTeam) {
        this.previousTeamId = this.selectedTeam;
        localStorage.setItem("selectedTeam", this.selectedTeam);
        this.$emit('select', this.selectedTeam);
        return;
      }

      // Değişiklik varsa App.vue'ya emit et (onay için)
      localStorage.setItem("selectedTeam", this.selectedTeam);
      this.$emit('select', this.selectedTeam);
      this.previousTeamId = this.selectedTeam;
    },

    resetToSelected() {
      // Onay iptal edildiğinde eski değere geri dön
      this.selectedTeam = this.previousTeamId;
      this.closeDropdown();
    }
  }
}
</script>
