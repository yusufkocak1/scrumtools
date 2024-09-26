<template>
  <div
      :class="['text-slate-800  flex flex-col w-full  rounded-md hover:bg-slate-100 focus:bg-slate-100 active:bg-slate-100 py-1 border-2 mb-2  px-2', getBorderColorByStatus]"
  ><span class="py-2 border-b" @click="openDetail">{{ item.value }}</span>
    <div class=" flex flex-row py-2 justify-between">
      <span class="text-slate-600 text-xs font-bold">{{ ownerName }}</span>
      <div class="flex flex-row">
        <button class="rounded-md border border-transparent px-2 text-center text-sm transition-all text-slate-600 hover:bg-slate-200 focus:bg-slate-200 active:bg-slate-200 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                @click="addVote(1)">
          <svg v-if="!isThereUpVote" class=" w-4 h-4" fill="#000000" viewBox="0 0 56 56"
               xmlns="http://www.w3.org/2000/svg">
            <g id="SVGRepo_bgCarrier" stroke-width="0"></g>
            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"></g>
            <g id="SVGRepo_iconCarrier">
              <path
                  d="M 33.3436 54.4141 L 36.6014 54.4141 C 39.5311 54.4141 42.0624 54.2266 43.7265 53.8281 C 46.9609 53.0313 48.9764 50.7813 48.9764 47.8984 C 48.9764 47.3125 48.8828 46.7500 48.7187 46.2109 C 50.3358 45.0391 51.2497 43.2344 51.2497 41.2891 C 51.2497 40.3281 51.0625 39.3906 50.7107 38.6172 C 51.7887 37.5156 52.4454 35.8984 52.4454 34.1875 C 52.4454 33.0859 52.1641 31.9141 51.6716 31.0703 C 52.3513 30.1094 52.7267 28.8203 52.7267 27.3906 C 52.7267 23.8984 50.0079 21.1328 46.5155 21.1328 L 37.6562 21.1328 C 37.0936 21.1328 36.7187 20.8750 36.7187 20.4063 C 36.7187 17.8516 40.7030 11.9219 40.7030 7.1641 C 40.7030 3.9297 38.4530 1.5859 35.3358 1.5859 C 33.0390 1.5859 31.4921 2.7813 29.9687 5.6875 C 27.1093 11.1719 23.7343 16.0235 18.1562 22.8203 L 13.7030 22.8203 C 7.9140 22.8203 3.2733 29.3594 3.2733 37.3516 C 3.2733 45.2500 8.3827 51.8125 14.5936 51.8125 L 22.3280 51.8125 C 25.4218 53.4297 29.1718 54.4141 33.3436 54.4141 Z M 36.6249 50.8750 L 33.3671 50.8281 C 23.5468 50.7578 16.8905 45.2031 16.8905 37.2344 C 16.8905 32.1719 18.0155 28.9375 21.2030 24.6719 C 24.7421 19.9610 29.6171 14.3125 33.1327 7.3047 C 34.0702 5.4297 34.5624 5.1250 35.3358 5.1250 C 36.4843 5.1250 37.1640 5.8516 37.1640 7.1641 C 37.1640 10.9610 33.1796 16.7266 33.1796 20.4063 C 33.1796 23.0547 35.3827 24.6719 38.1718 24.6719 L 46.5155 24.6719 C 48.0625 24.6719 49.1876 25.8437 49.1876 27.3906 C 49.1876 28.5156 48.8358 29.2422 47.9219 30.1328 C 47.6876 30.3672 47.5700 30.6016 47.5700 30.8594 C 47.5700 31.0469 47.6406 31.2578 47.8047 31.4453 C 48.5780 32.5703 48.9063 33.2735 48.9063 34.1875 C 48.9063 35.3125 48.3673 36.25 47.3123 37.0703 C 46.8671 37.3984 46.6327 37.7735 46.6327 38.1719 C 46.6327 38.3125 46.6562 38.4766 46.7499 38.6406 C 47.4294 39.9297 47.7107 40.4453 47.7107 41.2891 C 47.7107 42.5547 46.9140 43.5156 45.2265 44.3828 C 44.8749 44.5703 44.6640 44.8516 44.6640 45.2031 C 44.6640 45.3438 44.7109 45.4844 44.7811 45.6484 C 45.3671 47.0781 45.4374 47.3125 45.4374 47.8984 C 45.4374 49.0469 44.5936 49.9610 42.8827 50.3828 C 41.4999 50.7344 39.2968 50.8984 36.6249 50.8750 Z M 14.5936 48.2734 C 10.4452 48.2734 6.8124 43.2813 6.8124 37.3516 C 6.8124 31.3047 10.0702 26.3594 13.7030 26.3594 L 15.8593 26.3594 C 14.1014 29.6406 13.3514 33.0859 13.3514 37.1641 C 13.3514 41.5703 14.9452 45.3672 17.7811 48.2734 Z"></path>
            </g>
          </svg>
          <svg v-else class="w-4 h-4" fill="#000000" viewBox="0 0 56 56" xmlns="http://www.w3.org/2000/svg">
            <g id="SVGRepo_bgCarrier" stroke-width="0"></g>
            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"></g>
            <g id="SVGRepo_iconCarrier">
              <path
                  d="M 43.9374 51.7656 C 45.7655 51.3438 47.6171 50.1484 47.6171 47.8984 C 47.6171 46.9844 47.3593 46.3516 47.0546 45.8125 C 46.8671 45.5078 46.8905 45.2734 47.1718 45.1563 C 48.6481 44.5000 49.8673 43.1875 49.8673 41.2891 C 49.8673 40.2344 49.5860 39.2735 49.0470 38.5937 C 48.7887 38.2422 48.8358 37.9610 49.2577 37.7032 C 50.3593 37.0937 51.0625 35.7344 51.0625 34.1875 C 51.0625 33.1094 50.7107 31.9141 50.0545 31.3281 C 49.7031 31.0000 49.7732 30.7656 50.1716 30.4375 C 50.9454 29.8516 51.3673 28.7266 51.3673 27.3906 C 51.3673 25.0937 49.5860 23.2422 47.2421 23.2422 L 38.8749 23.2422 C 36.7655 23.2422 35.3358 22.1406 35.3358 20.4063 C 35.3358 17.1719 39.3436 11.3594 39.3436 7.1641 C 39.3436 4.9844 37.9140 3.6719 36.0624 3.6719 C 34.3749 3.6719 33.5077 4.8437 32.5936 6.6250 C 29.1014 13.5156 24.3671 19.0703 20.7811 23.8281 C 17.7343 27.9063 16.2343 31.3281 16.1640 36.9532 C 16.0467 45.6016 23.0546 52.1875 34.0702 52.2813 L 37.3280 52.3047 C 40.3983 52.3281 42.6483 52.0937 43.9374 51.7656 Z M 4.6327 37.1172 C 4.6327 44.1484 8.9921 50.0313 14.8749 50.0313 L 19.0702 50.0313 C 14.8280 46.9375 12.8827 42.2500 12.9764 36.8828 C 13.0467 30.9297 15.3671 26.6875 17.4296 24.1094 L 13.9843 24.1094 C 8.7108 24.1094 4.6327 29.8281 4.6327 37.1172 Z"></path>
            </g>
          </svg>
        </button>
        <span class="">{{ getVoteCount }}</span>
        <button class="rounded-md border border-transparent px-2 text-center text-sm transition-all text-slate-600 hover:bg-slate-200 focus:bg-slate-200 active:bg-slate-200 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                @click="addVote(-1)">
          <svg v-if="!isThereDownVote" class="w-4 h-4" fill="#000000" viewBox="0 0 56 56"
               xmlns="http://www.w3.org/2000/svg">
            <g id="SVGRepo_bgCarrier" stroke-width="0"></g>
            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"></g>
            <g id="SVGRepo_iconCarrier">
              <path
                  d="M 22.6562 1.5860 L 19.3983 1.5860 C 16.4687 1.5860 13.9609 1.7500 12.2733 2.1719 C 9.0390 2.9687 7.0233 5.2187 7.0233 8.0781 C 7.0233 8.6875 7.1171 9.2266 7.2811 9.7656 C 5.6640 10.9609 4.7499 12.7422 4.7499 14.7109 C 4.7499 15.6484 4.9374 16.5860 5.2890 17.3828 C 4.2109 18.4844 3.5546 20.1016 3.5546 21.7891 C 3.5546 22.9140 3.8358 24.0625 4.3280 24.9297 C 3.6483 25.8672 3.2733 27.1797 3.2733 28.5860 C 3.2733 32.1016 5.9921 34.8672 9.4843 34.8672 L 18.3436 34.8672 C 18.9296 34.8672 19.2811 35.1016 19.2811 35.5938 C 19.2811 38.1484 15.2968 44.0781 15.2968 48.8360 C 15.2968 52.0703 17.5468 54.4140 20.6640 54.4140 C 22.9843 54.4140 24.5077 53.2187 26.0311 50.3125 C 28.8905 44.8281 32.2655 39.9766 37.8436 33.1797 L 42.2968 33.1797 C 48.0860 33.1797 52.7267 26.6406 52.7267 18.625 C 52.7267 10.7500 47.6171 4.1875 41.4062 4.1875 L 33.6718 4.1875 C 30.5780 2.5703 26.8280 1.5860 22.6562 1.5860 Z M 19.3749 5.1250 L 22.6327 5.1484 C 32.4530 5.2422 39.1093 10.7969 39.1093 18.7656 C 39.1093 23.8281 37.9843 27.0625 34.7968 31.3281 C 31.2577 36.0391 26.3827 41.6875 22.8671 48.6953 C 21.9296 50.5469 21.4374 50.8750 20.6640 50.8750 C 19.5155 50.8750 18.8358 50.1484 18.8358 48.8360 C 18.8358 45.0156 22.8202 39.2734 22.8202 35.5938 C 22.8202 32.9453 20.6171 31.3281 17.8280 31.3281 L 9.4843 31.3281 C 7.9374 31.3281 6.8124 30.1562 6.8124 28.5860 C 6.8124 27.4844 7.1640 26.7578 8.0780 25.8672 C 8.3358 25.6328 8.4296 25.3984 8.4296 25.1406 C 8.4296 24.9297 8.3593 24.7422 8.1952 24.5313 C 7.4218 23.4297 7.0936 22.7031 7.0936 21.7891 C 7.0936 20.6875 7.6327 19.7266 8.6874 18.9297 C 9.1562 18.6016 9.3905 18.2266 9.3905 17.8281 C 9.3905 17.6875 9.3436 17.5234 9.2499 17.3594 C 8.5702 16.0703 8.2890 15.5547 8.2890 14.7109 C 8.2890 13.4453 9.0858 12.4844 10.7733 11.6172 C 11.1249 11.4297 11.3358 11.1484 11.3358 10.7969 C 11.3358 10.6562 11.2890 10.5156 11.2187 10.3516 C 10.6327 8.9219 10.5624 8.6875 10.5624 8.0781 C 10.5624 6.9297 11.4062 6.0391 13.1171 5.6172 C 14.4999 5.2422 16.7030 5.1016 19.3749 5.1250 Z M 41.4062 7.7266 C 45.5546 7.7266 49.1876 12.6953 49.1876 18.625 C 49.1876 24.6953 45.9296 29.6406 42.2968 29.6406 L 40.1405 29.6406 C 41.8983 26.3360 42.6483 22.9140 42.6483 18.8360 C 42.6483 14.4297 41.0546 10.6094 38.2421 7.7266 Z"></path>
            </g>
          </svg>
          <svg v-else class="w-4 h-4" fill="#000000" viewBox="0 0 56 56" xmlns="http://www.w3.org/2000/svg">
            <g id="SVGRepo_bgCarrier" stroke-width="0"></g>
            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"></g>
            <g id="SVGRepo_iconCarrier">
              <path
                  d="M 12.0389 4.2344 C 10.2108 4.6797 8.3827 5.8516 8.3827 8.1016 C 8.3827 9.0156 8.6171 9.6719 8.9452 10.1875 C 9.1093 10.4922 9.0858 10.7266 8.8280 10.8437 C 7.3514 11.5 6.1093 12.8359 6.1093 14.7110 C 6.1093 15.7890 6.3905 16.7266 6.9296 17.4063 C 7.2108 17.7578 7.1405 18.0625 6.7186 18.2969 C 5.6405 18.9063 4.9139 20.2656 4.9139 21.8125 C 4.9139 22.9141 5.2655 24.0859 5.9218 24.6719 C 6.2733 25 6.2264 25.2344 5.8046 25.5625 C 5.0546 26.1484 4.6327 27.2734 4.6327 28.6094 C 4.6327 30.9063 6.3905 32.7812 8.7343 32.7812 L 17.1014 32.7812 C 19.2108 32.7812 20.6405 33.8594 20.6405 35.6172 C 20.6405 38.8281 16.6562 44.6406 16.6562 48.8594 C 16.6562 51.0390 18.0858 52.3281 19.9374 52.3281 C 21.6249 52.3281 22.4686 51.1563 23.3827 49.3750 C 26.8749 42.4844 31.6093 36.9531 35.2186 32.1719 C 38.2655 28.1172 39.7421 24.6719 39.8124 19.0703 C 39.9296 10.3984 32.9452 3.8125 21.9062 3.7422 L 18.6718 3.6953 C 15.5780 3.6719 13.3514 3.9063 12.0389 4.2344 Z M 51.3673 18.8828 C 51.3673 11.8516 46.9843 5.9688 41.1014 5.9688 L 36.9296 5.9688 C 41.1483 9.0859 43.1171 13.7734 43.0233 19.1172 C 42.9296 25.0703 40.6093 29.3125 38.5467 31.8906 L 41.9921 31.8906 C 47.2889 31.8906 51.3673 26.1953 51.3673 18.8828 Z"></path>
            </g>
          </svg>
        </button>
        <button v-if="isOwner"
                class="rounded-md border border-transparent px-2 text-center text-sm transition-all text-slate-600 hover:bg-slate-200 focus:bg-slate-200 active:bg-slate-200 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none"
                type="button"
                @click="$emit('removeItem', item.id)">
          <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path clip-rule="evenodd"
                  d="M16.5 4.478v.227a48.816 48.816 0 0 1 3.878.512.75.75 0 1 1-.256 1.478l-.209-.035-1.005 13.07a3 3 0 0 1-2.991 2.77H8.084a3 3 0 0 1-2.991-2.77L4.087 6.66l-.209.035a.75.75 0 0 1-.256-1.478A48.567 48.567 0 0 1 7.5 4.705v-.227c0-1.564 1.213-2.9 2.816-2.951a52.662 52.662 0 0 1 3.369 0c1.603.051 2.815 1.387 2.815 2.951Zm-6.136-1.452a51.196 51.196 0 0 1 3.273 0C14.39 3.05 15 3.684 15 4.478v.113a49.488 49.488 0 0 0-6 0v-.113c0-.794.609-1.428 1.364-1.452Zm-.355 5.945a.75.75 0 1 0-1.5.058l.347 9a.75.75 0 1 0 1.499-.058l-.346-9Zm5.48.058a.75.75 0 1 0-1.498-.058l-.347 9a.75.75 0 0 0 1.5.058l.345-9Z"
                  fill-rule="evenodd"/>
          </svg>
        </button>
      </div>
    </div>
  </div>

</template>

<script>
import {getUserFromDB} from "../../firebase/AuthService.js";
import {getVotes, listenRetroItemVotes, removeVote} from "../../firebase/RetroBoardService.js";

export default {
  name: "RetroItem",
  data() {
    return {
      ownerName: "",
    }
  },
  props: {
    item: Object,
    isAdmin: false,
    teamId: String,
    boardId: String,
    column: String,
  },
  methods: {
    openDetail() {
      this.$emit('openDetail', this.item)
    },
    addVote(voteValue) {
      const vote = {
        value: voteValue,
        owner: localStorage.getItem('user')
      }
      if (voteValue === 1 && this.isThereUpVote) {
        removeVote(this.teamId, this.boardId, this.column, this.item.id, localStorage.getItem('user'))
      } else if (voteValue === -1 && this.isThereDownVote) {
        removeVote(this.teamId, this.boardId, this.column, this.item.id, localStorage.getItem('user'))
      } else {
        this.$emit('addVote', this.item.id, vote)
      }
    }
  },
  computed: {
    isOwner() {
      return this.item.owner === localStorage.getItem('user') || this.isAdmin
    },
    getVoteCount() {
      return this.item?.votes?.map(vote => vote.value).reduce((a, b) => a + b, 0)
    },
    isThereUpVote() {
      return this.item?.votes?.filter(vote => vote.value === 1 && vote.owner === localStorage.getItem('user')).length > 0
    },
    isThereDownVote() {
      return this.item?.votes?.filter(vote => vote.value === -1 && vote.owner === localStorage.getItem('user')).length > 0
    },
    getBorderColorByStatus() {
      if (!this.item.status) {
        return "border-slate-200"
      } else if (this.item.status === "ACCEPTED") {
        return " border-l-4 border-l-green-700"
      } else if (this.item.status === "REJECTED") {
        return "border-l-4 border-l-red-700"
      } else if (this.item.status === "CONFLICT") {
        return "border-l-4 border-l-amber-700"
      } else {
        return "border-slate-200"
      }
    }
  },
  created() {
    if (this.item.owner === "Anonymous") {
      this.ownerName = "Anonymous"
    }
    listenRetroItemVotes(this.teamId, this.boardId, this.column, this.item.id, (votes) => {
      this.item.votes = votes
    })

    getUserFromDB(this.item.owner, (user) => {
      this.ownerName = user.name
    })
  },
  unmounted() {
  }
}
</script>
