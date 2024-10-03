<template>
<div class="border-2 bg-slate-800 text-white border-slate-200 rounded-lg  p-4 hover:bg-slate-200 hover:text-slate-800 w-24 h-32  flex flex-col justify-center items-center" @click="select">
  <div class="flex justify-center items-center font-extrabold text-4xl">{{ number }}</div>
</div>
</template>
<script>
export default {
  name: "pokerCard",
  props: {
    selectable:true,
    number: String
  },
  methods: {
    select() {
      if(this.selectable) {
        this.$emit('selectPokerCard', this.number)
      }
    }
  }
}
</script>