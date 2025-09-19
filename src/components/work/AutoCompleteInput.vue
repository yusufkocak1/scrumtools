<template>
  <div class="relative">
    <div class="relative">
      <input
        ref="input"
        v-model="searchTerm"
        type="text"
        :placeholder="placeholder"
        class="w-full px-4 py-3 pr-10 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @focus="showDropdown = true"
        @blur="handleBlur"
        @keydown.down.prevent="navigateDown"
        @keydown.up.prevent="navigateUp"
        @keydown.enter.prevent="selectHighlighted"
        @keydown.escape="hideDropdown"
      />

      <!-- Clear button -->
      <button
        v-if="selectedValue"
        type="button"
        @click="clearSelection"
        class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>

      <!-- Dropdown arrow -->
      <div v-else class="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
        <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
        </svg>
      </div>
    </div>

    <!-- Dropdown -->
    <div
      v-if="showDropdown && filteredOptions.length > 0"
      class="absolute z-50 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg max-h-60 overflow-y-auto"
    >
      <ul class="py-1">
        <li
          v-for="(option, index) in filteredOptions"
          :key="option[valueField] || index"
          :class="[
            'px-4 py-3 cursor-pointer flex items-center space-x-3 hover:bg-blue-50 transition-colors',
            { 'bg-blue-100': index === highlightedIndex }
          ]"
          @mousedown.prevent="selectOption(option)"
          @mouseover="highlightedIndex = index"
        >
          <!-- Avatar -->
          <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-sm font-medium flex-shrink-0">
            {{ getInitials(option[displayField]) }}
          </div>

          <!-- User info -->
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium text-gray-900 truncate">
              {{ option[displayField] }}
            </div>
            <div class="text-xs text-gray-500 truncate">
              {{ option.email }}
            </div>
            <div v-if="option.role" class="text-xs text-blue-600 truncate">
              {{ option.role }}
            </div>
          </div>

          <!-- Role badge -->
          <div v-if="option.role" class="flex-shrink-0">
            <span
              class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium"
              :class="getRoleBadgeClass(option.role)"
            >
              {{ getRoleIcon(option.role) }} {{ option.role }}
            </span>
          </div>
        </li>
      </ul>

      <!-- No results -->
      <div v-if="searchTerm && filteredOptions.length === 0" class="px-4 py-3 text-sm text-gray-500 text-center">
        No results found for "{{ searchTerm }}"
      </div>
    </div>

    <!-- Selected user display -->
    <div v-if="selectedValue && selectedOption" class="mt-2 flex items-center space-x-2 p-2 bg-blue-50 rounded-lg border border-blue-200">
      <div class="w-6 h-6 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-xs font-medium">
        {{ getInitials(selectedOption[displayField]) }}
      </div>
      <div class="flex-1 min-w-0">
        <div class="text-sm font-medium text-gray-900 truncate">{{ selectedOption[displayField] }}</div>
        <div class="text-xs text-gray-500 truncate">{{ selectedOption.email }}</div>
      </div>
      <button
        type="button"
        @click="clearSelection"
        class="text-blue-600 hover:text-blue-800 p-1"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AutoCompleteInput',
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    options: {
      type: Array,
      default: () => []
    },
    displayField: {
      type: String,
      default: 'name'
    },
    valueField: {
      type: String,
      default: 'id'
    },
    placeholder: {
      type: String,
      default: 'Search...'
    }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      searchTerm: '',
      showDropdown: false,
      highlightedIndex: -1,
      selectedValue: this.modelValue,
      blurTimeout: null
    };
  },
  computed: {
    filteredOptions() {
      if (!this.searchTerm) return this.options;

      const term = this.searchTerm.toLowerCase();
      return this.options.filter(option => {
        const name = option[this.displayField]?.toLowerCase() || '';
        const email = option.email?.toLowerCase() || '';
        const role = option.role?.toLowerCase() || '';

        return name.includes(term) || email.includes(term) || role.includes(term);
      });
    },

    selectedOption() {
      if (!this.selectedValue) return null;
      return this.options.find(option => option[this.valueField] === this.selectedValue);
    }
  },
  watch: {
    modelValue: {
      immediate: true,
      handler(newValue) {
        this.selectedValue = newValue;
        if (newValue) {
          const option = this.options.find(opt => opt[this.valueField] === newValue);
          this.searchTerm = option ? option[this.displayField] : '';
        } else {
          this.searchTerm = '';
        }
      }
    },

    searchTerm(newTerm) {
      if (!newTerm && this.selectedValue) {
        // If search term is cleared but we have a selected value, restore it
        const option = this.selectedOption;
        if (option) {
          this.searchTerm = option[this.displayField];
        }
      }
    }
  },
  methods: {
    selectOption(option) {
      this.selectedValue = option[this.valueField];
      this.searchTerm = option[this.displayField];
      this.showDropdown = false;
      this.highlightedIndex = -1;
      this.$emit('update:modelValue', option[this.valueField]);
    },

    clearSelection() {
      this.selectedValue = '';
      this.searchTerm = '';
      this.showDropdown = false;
      this.highlightedIndex = -1;
      this.$emit('update:modelValue', '');
      this.$refs.input.focus();
    },

    navigateDown() {
      if (this.highlightedIndex < this.filteredOptions.length - 1) {
        this.highlightedIndex++;
      }
    },

    navigateUp() {
      if (this.highlightedIndex > 0) {
        this.highlightedIndex--;
      }
    },

    selectHighlighted() {
      if (this.highlightedIndex >= 0 && this.filteredOptions[this.highlightedIndex]) {
        this.selectOption(this.filteredOptions[this.highlightedIndex]);
      }
    },

    hideDropdown() {
      this.showDropdown = false;
      this.highlightedIndex = -1;
    },

    handleBlur() {
      // Delay hiding dropdown to allow click events
      this.blurTimeout = setTimeout(() => {
        this.hideDropdown();
      }, 150);
    },

    getInitials(name) {
      if (!name) return '';
      return name.split(' ').map(word => word[0]).join('').toUpperCase().slice(0, 2);
    },

    getRoleIcon(role) {
      const icons = {
        'developer': '👨‍💻',
        'tester': '🧪',
        'analyst': '📊',
        'designer': '🎨',
        'manager': '👔',
        'admin': '⚙️'
      };
      return icons[role?.toLowerCase()] || '👤';
    },

    getRoleBadgeClass(role) {
      const classes = {
        'developer': 'bg-green-100 text-green-800',
        'tester': 'bg-purple-100 text-purple-800',
        'analyst': 'bg-blue-100 text-blue-800',
        'designer': 'bg-pink-100 text-pink-800',
        'manager': 'bg-orange-100 text-orange-800',
        'admin': 'bg-red-100 text-red-800'
      };
      return classes[role?.toLowerCase()] || 'bg-gray-100 text-gray-800';
    }
  },

  beforeUnmount() {
    if (this.blurTimeout) {
      clearTimeout(this.blurTimeout);
    }
  }
};
</script>

<style scoped>
/* Dropdown animation */
.dropdown-enter-active, .dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from, .dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
