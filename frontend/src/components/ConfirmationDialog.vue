<template>
  <div class="relative m-4 w-full max-w-lg rounded-lg bg-white shadow-lg">
    <div class="flex items-center justify-between p-6 border-b border-gray-200">
      <h3 class="text-xl font-semibold text-gray-900">
        {{ title }}
      </h3>
      <button
        @click="handleCancel"
        class="text-gray-400 hover:text-gray-600 transition-colors duration-200">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
        </svg>
      </button>
    </div>

    <div class="p-6">
      <div class="flex items-center mb-4">
        <div class="flex-shrink-0 w-12 h-12 mx-auto rounded-full flex items-center justify-center"
             :class="iconClass">
          <svg class="w-6 h-6" :class="iconColorClass" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path v-if="type === 'warning'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.082 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
            <path v-else-if="type === 'danger'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
            <path v-else-if="type === 'info'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        </div>
      </div>

      <div class="text-center">
        <h4 class="text-lg font-medium text-gray-900 mb-2">
          {{ message }}
        </h4>
        <p v-if="description" class="text-sm text-gray-500 mb-6">
          {{ description }}
        </p>
      </div>
    </div>

    <div class="flex items-center justify-end gap-3 p-6 border-t border-gray-200">
      <button
        @click="handleCancel"
        class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors duration-200">
        {{ cancelText }}
      </button>
      <button
        @click="handleConfirm"
        class="rounded-md px-4 py-2 text-sm font-medium text-white focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors duration-200"
        :class="confirmButtonClass">
        {{ confirmText }}
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ConfirmationDialog',
  props: {
    title: {
      type: String,
      default: 'Confirm Action'
    },
    message: {
      type: String,
      required: true
    },
    description: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: 'warning', // warning, danger, info, success
      validator: (value) => ['warning', 'danger', 'info', 'success'].includes(value)
    },
    confirmText: {
      type: String,
      default: 'Confirm'
    },
    cancelText: {
      type: String,
      default: 'Cancel'
    }
  },
  emits: ['confirm', 'cancel'],
  computed: {
    iconClass() {
      const classes = {
        warning: 'bg-orange-100',
        danger: 'bg-red-100',
        info: 'bg-blue-100',
        success: 'bg-green-100'
      }
      return classes[this.type] || classes.warning
    },
    iconColorClass() {
      const classes = {
        warning: 'text-orange-600',
        danger: 'text-red-600',
        info: 'text-blue-600',
        success: 'text-green-600'
      }
      return classes[this.type] || classes.warning
    },
    confirmButtonClass() {
      const classes = {
        warning: 'bg-orange-600 hover:bg-orange-700 focus:ring-orange-500',
        danger: 'bg-red-600 hover:bg-red-700 focus:ring-red-500',
        info: 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-500',
        success: 'bg-green-600 hover:bg-green-700 focus:ring-green-500'
      }
      return classes[this.type] || classes.warning
    }
  },
  methods: {
    handleConfirm() {
      this.$emit('confirm')
    },
    handleCancel() {
      this.$emit('cancel')
    }
  }
}
</script>
