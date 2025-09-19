<template>
  <div v-if="isVisible" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[10000]" @click.self="close">
    <div class="bg-white rounded-2xl p-6 w-full max-w-2xl mx-4 max-h-[90vh] overflow-y-auto">
      <!-- Modal Header -->
      <div class="flex items-center justify-between mb-6">
        <h3 class="text-2xl font-bold text-gray-900">Donate</h3>
        <button @click="close" class="p-2 hover:bg-gray-100 rounded-lg transition-colors">
          <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>

      <!-- Description -->
      <div class="mb-6">
        <p class="text-gray-600 text-sm leading-relaxed">
          Support us to continue developing ScrumTools with your donations.
          You can donate in TRY or USD using the QR codes below.
        </p>
      </div>

      <!-- Currency Selection -->
      <div class="mb-6">
        <div class="flex bg-gray-100 rounded-xl p-1">
          <button
            @click="selectedCurrency = 'TRY'"
            :class="[
              'flex-1 py-2 px-4 rounded-lg text-sm font-medium transition-all duration-200',
              selectedCurrency === 'TRY'
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            ]">
            TRY (₺)
          </button>
          <button
            @click="selectedCurrency = 'USD'"
            :class="[
              'flex-1 py-2 px-4 rounded-lg text-sm font-medium transition-all duration-200',
              selectedCurrency === 'USD'
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            ]">
            USD ($)
          </button>
        </div>
      </div>

      <!-- QR Code Display -->
      <div class="mb-6">
        <div class="bg-gray-50 rounded-xl p-6 text-center">
          <div class="mb-4">
            <h4 class="text-lg font-semibold text-gray-900 mb-2">
              Donate with {{ selectedCurrency === 'TRY' ? 'Turkish Lira (₺)' : 'US Dollar ($)' }}
            </h4>
            <p class="text-sm text-gray-600">
              Scan the QR code with your banking app to make a donation
            </p>
          </div>

          <!-- QR Code Area -->
          <div class="flex items-center justify-center mb-4">
            <div class="w-48 h-48 bg-white rounded-lg border-2 border-gray-200 flex items-center justify-center">
              <!-- QR code images -->
              <div class="text-center">
                <img
                  :src="qrCodes[selectedCurrency]"
                  :alt="`${selectedCurrency} Donation QR Code`"
                  class="w-40 h-40 rounded-lg mb-2"
                  @error="showPlaceholder = true"
                  v-if="!showPlaceholder"
                />
                <div v-else class="w-40 h-40 bg-gray-100 rounded-lg flex items-center justify-center mb-2">
                  <svg class="w-16 h-16 text-gray-400" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M3 3h8v8H3V3zm10 0h8v8h-8V3zM3 13h8v8H3v-8zm18 0v8h-8v-8h8zM5 5v4h4V5H5zm10 0v4h4V5h-4zM5 15v4h4v-4H5zm10 10h4v-4h-4v4zm0-6h4v-4h-4v4z"/>
                  </svg>
                </div>
                <p class="text-xs text-gray-500">{{ selectedCurrency }} QR Code</p>
              </div>
            </div>
          </div>

          <!-- Alternative Donation Information -->
          <div class="text-xs text-gray-500">
            <p class="mb-1">Alternatively, you can also make a bank transfer</p>
            <p v-if="selectedCurrency === 'TRY'">IBAN: TR49 0006 7010 0000 0077 4518 73</p>
            <p v-if="selectedCurrency === 'USD'">IBAN: TR48 0006 7010 0000 0034 1969 72</p>
          </div>
        </div>
      </div>

      <!-- Information -->
      <div class="bg-blue-50 rounded-xl p-4">
        <div class="flex items-start gap-3">
          <svg class="w-5 h-5 text-blue-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path>
          </svg>
          <div>
            <h5 class="text-sm font-medium text-blue-900 mb-1">How will your donation be used?</h5>
            <p class="text-xs text-blue-700 leading-relaxed">
              Your donations will be used for ScrumTools development, server costs, and adding new features.
            </p>
          </div>
        </div>
      </div>

      <!-- Close Button -->
      <div class="mt-6">
        <button
          @click="close"
          class="w-full bg-gray-100 hover:bg-gray-200 text-gray-800 py-3 px-4 rounded-xl font-medium transition-colors">
          Close
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import TryQrCode from '../assets/qr-codes/TRY.png'
import UsdQrCode from '../assets/qr-codes/USD.png'

export default {
  name: 'DonationModal',
  props: {
    isVisible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close'],
  data() {
    return {
      selectedCurrency: 'TRY',
      showPlaceholder: false,
      qrCodes: {
        TRY: TryQrCode,
        USD: UsdQrCode
      }
    }
  },
  methods: {
    close() {
      this.$emit('close')
    }
  }
}
</script>
