<template>
  <div
    :class="[
      selectedCardNumber === number || !selectable
        ? 'border-green-400 border-2 shadow-lg shadow-green-400/40 scale-105 bg-green-700'
        : 'border-gray-200/50 border hover:border-green-300 hover:scale-102 hover:shadow-xl bg-green-400 hover:bg-green-600',
      'relative rounded-xl p-6 w-20 h-28 sm:w-24 sm:h-32 md:w-28 md:h-36 flex flex-col justify-center items-center cursor-pointer transition-all duration-300 ease-out shadow-lg text-white'
    ]"
    @click="select"
  >
    <!-- Card number -->
    <div class="relative z-10 flex justify-center items-center font-black text-2xl sm:text-3xl md:text-4xl tracking-tight">
      {{ number }}
    </div>

    <!-- Subtle corner decoration -->
    <div class="absolute top-2 right-2 w-2 h-2 bg-white/30 rounded-full"></div>
    <div class="absolute bottom-2 left-2 w-2 h-2 bg-white/30 rounded-full"></div>
  </div>
</template>

<script>
export default {
  name: "pokerCard",
  props: {
    selectable: true,
    number: String,
    selectedCardNumber: String
  },
  methods: {
    select() {
      if (this.selectable) {
        this.$emit('selectPokerCard', this.number)
      }
    }
  }
}
</script>