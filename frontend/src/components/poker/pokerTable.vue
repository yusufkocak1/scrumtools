<template>
  <div class="w-full">
    <!-- Combined Control Panel & Team Votes -->
    <div class="w-full bg-white border-2 border-gray-200/60 rounded-2xl shadow-lg backdrop-blur-sm relative overflow-hidden">
      <!-- Background Pattern -->
      <div class="absolute inset-0 bg-gradient-to-br from-gray-50/30 to-green-50/30 opacity-60"></div>
      <div class="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-gray-100/20 to-transparent rounded-full -translate-y-12 translate-x-12"></div>
      <div class="absolute bottom-0 left-0 w-20 h-20 bg-gradient-to-tr from-green-100/20 to-transparent rounded-full translate-y-10 -translate-x-10"></div>

      <div class="relative z-10 p-5">
        <!-- Top Section: Button & Average -->
        <div class="flex flex-col lg:flex-row items-center justify-between gap-4 mb-5">
          <!-- Left Side: Button -->
          <Button
              @click="newRound"
              class="select-none rounded-full bg-gradient-to-r from-slate-700 to-slate-800 hover:from-slate-800 hover:to-slate-900 py-3 px-8 text-center font-semibold text-white shadow-lg shadow-slate-500/20 transition-all duration-300 hover:shadow-xl hover:shadow-slate-500/30 hover:scale-105 active:scale-95 transform border border-slate-600/30 relative overflow-hidden"
          >
            <!-- Button shine effect -->
            <div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent -skew-x-12 translate-x-[-100%] hover:translate-x-[100%] transition-transform duration-700"></div>

            <span class="relative flex items-center gap-2">
              <div class="w-6 h-6 bg-white/20 rounded-full flex items-center justify-center">
                <svg v-if="!isVotesVisible" class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <svg v-else class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              </div>
              <span class="text-sm tracking-wide">{{isVotesVisible? 'New Round' : 'Show Votes'}}</span>
            </span>
          </Button>

          <!-- Right Side: Average -->
          <div class="flex items-center gap-3 bg-white/80 backdrop-blur-sm rounded-xl px-5 py-2 border border-gray-200/50 shadow-md">
            <div class="flex items-center gap-2">
              <div class="w-2 h-2 bg-gradient-to-r from-green-500 to-green-600 rounded-full"></div>
              <span class="text-gray-700 font-medium text-sm">Average:</span>
            </div>
            <div class="text-xl font-black">
              <span class="bg-gradient-to-r from-green-600 via-green-500 to-green-700 bg-clip-text text-transparent">
                {{isVotesVisible ? average.toFixed(1) : '?'}}
              </span>
            </div>
          </div>
        </div>

        <!-- Bottom Section: Team Votes -->
        <div>
          <div class="text-center mb-4">
            <h3 class="text-base font-bold text-gray-800 mb-1 flex items-center justify-center gap-1">
              <svg class="w-4 h-4 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              Team Votes
            </h3>
          </div>

          <div class="flex justify-center lg:justify-start flex-wrap gap-3">
            <div v-for="vote in votes" :key="vote" class="group">
              <div class="bg-white/60 backdrop-blur-sm border border-gray-200/60 rounded-lg  flex flex-col gap-1 justify-center items-center shadow-sm  transition-all duration-300  transform relative overflow-hidden">
                <!-- User card background gradient -->
                <div class="absolute inset-0 bg-gradient-to-br from-green-50/20 to-green-100/20 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>

                <div class="relative z-10 flex flex-col items-center gap-2">
                  <pokerCard
                    :key="'vote'+vote"
                    :number="vote.vote==='-' ? '' : isVotesVisible ? vote.vote:'?'"
                    :selectable="false"
                    class="transition-transform duration-300  scale-75"
                  ></pokerCard>

                  <span class="text-center text-xs font-semibold text-gray-700">
                    {{ getName(vote.email).split(" ")[0].toUpperCase() }}
                  </span>
                </div>
              </div>
            </div>
          </div>
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
