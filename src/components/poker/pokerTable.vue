<template>
  <div class="container flex flex-col">

    <div class="flex flex-col justify-center items-center font-extrabold text-4xl  py-12 border-4 gap-4">
      <Button
          @click="newRound"
          class="select-none mb-2 rounded-lg bg-amber-700 py-3.5 px-7 text-center align-middle font-sans text-sm font-bold uppercase text-white shadow-md shadow-gray-900/10 transition-all hover:shadow-lg hover:shadow-gray-900/20 focus:opacity-[0.85] focus:shadow-none active:opacity-[0.85] active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
      >{{isVotesVisible? 'New Round' : 'Show Votes'}}
      </Button>
      <span>Average : {{isVotesVisible ? average:'?' }}</span></div>
    <div class="p-2 border-4 flex justify-center lg:justify-start flex-wrap gap-2 ">
      <div v-for="vote in votes" :key="vote">
        <div class="border-4 border-slate-200 rounded-lg  p-4 flex  flex-col gap-2 justify-center items-center">
          <pokerCard :key="'vote'+vote" :number="vote.vote==='-' ? '' : isVotesVisible ? vote.vote:'?'" :selectable="false"></pokerCard>
          <span class="text-center text-2xl font-bold">{{ getName(vote.email) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import PokerCard from "./pokerCard.vue";

export default {
  name: "pokerTable",
  components: {PokerCard},
  props: {
    votes: Array,
    members: Object,
    isVotesVisible: Boolean
  },
  methods: {
    getName(email) {
      return this.members[email].displayName
    },
    newRound() {
      console.log("buarada")
      this.$emit('newRound')
    }
  }, computed: {
    average() {
      let sum = 0;
      let validVotes = this.votes.filter((vote) => !isNaN(vote.vote) && vote.vote !== "-" && vote.vote !== '?');

      if (validVotes.length === 0) {
        return 0; // or any default value you prefer
      }

      sum = validVotes.reduce((acc, cur) => acc + parseFloat(cur.vote), 0);
      return sum / validVotes.length;
    }
}}
</script>
