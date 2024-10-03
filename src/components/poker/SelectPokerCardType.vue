<template>
  <div v-if="isAdmin" class="flex m-2 p-4 gap-10">
    <div class="inline-flex items-center" v-for="pokerCard in pokerCardNumbers" :key="pokerCard">
      <label class="relative flex items-center cursor-pointer" >
        <input name="framework" type="radio" v-model="selectedPokerCardType" :value="pokerCard" class="peer h-5 w-5 cursor-pointer appearance-none rounded-full border border-slate-300 checked:border-slate-400 transition-all">
        <span class="absolute bg-slate-800 w-3 h-3 rounded-full opacity-0 peer-checked:opacity-100 transition-opacity duration-200 top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2">
      </span>
      </label>
      <label class="ml-2 text-slate-600 cursor-pointer text-sm" for="html">{{pokerCard.type}}</label>
    </div>
  </div>
</template>

<script>
import {getAuth} from "firebase/auth";

export default {
  name: "SelectPokerCardType",
  props: {
    team: Object,
    selectedPokerCardTypeName: String
  },
  data() {
    return {
      teamPoker:Object,
      selectedPokerCardType: Object,
      pokerCardNumbers: [{
        type: "Fibonacci",
        numbers: ["1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?"]
      },
        {
          type: "Powers of 2",
          numbers: ["2", "4", "8", "16", "32", "64", "128", "256", "512", "1024", "?"]
        },
        {
          type: "tshirts",
          numbers: ["XS", "S", "M", "L", "XL", "XXL", "XXXL", "?"]
        }
      ]}
  },
  mounted() {

  },
  computed: {
    isAdmin() {
      return this.team.adminEmail === getAuth().currentUser.email
    }
  },
  watch: {
    selectedPokerCardType(){
      this.$emit('selectPokerCardType', this.selectedPokerCardType)
    },
    selectedPokerCardTypeName(){
      this.selectedPokerCardType = this.pokerCardNumbers.find(pokerCard => pokerCard.type === this.selectedPokerCardTypeName)
    }
  },

}
</script>