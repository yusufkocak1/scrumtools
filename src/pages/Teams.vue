<template>
  <div class="container">
  <div class="container py-5 border-b">
  <TeamList :teamList="teams" @select="selectTeam"></TeamList>
  </div>
  <div class="container my-2">
    <div
        class="relative flex flex-col w-full h-full overflow-scroll text-gray-700 bg-white shadow-md rounded-xl bg-clip-border">
      <table class="w-full text-left table-auto min-w-max">
        <thead>
        <tr >
          <th class="p-4 border-b border-blue-gray-100 bg-blue-gray-50">
            <p class="block font-sans text-sm antialiased font-normal leading-none text-blue-gray-900 opacity-70">
              Name
            </p>
          </th>
          <th class="p-4 border-b border-blue-gray-100 bg-blue-gray-50">
            <p class="block font-sans text-sm antialiased font-normal leading-none text-blue-gray-900 opacity-70">Email
            </p>
          </th>
          <th class="p-4 border-b border-blue-gray-100 bg-blue-gray-50">
            <p class="block font-sans text-sm antialiased font-normal leading-none text-blue-gray-900 opacity-70"></p>
          </th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(member, email) in selectedTeam.members" :key="member">
          <td class="p-4 border-b border-blue-gray-50">
            <p class="block font-sans text-sm antialiased font-normal leading-normal text-blue-gray-900">
              {{ member.displayName }}
            </p>
          </td>
          <td class="p-4 border-b border-blue-gray-50">
            <p class="block font-sans text-sm antialiased font-normal leading-normal text-blue-gray-900">
              {{ email }}
            </p>
          </td>

          <td class="p-4 border-b border-blue-gray-50">
            <div class="ml-auto grid place-items-center justify-self-end">
              <button :disabled="!isAdmin"  @click="removeMember(email)" class="rounded-md border border-transparent px-2 text-center text-sm transition-all text-slate-600 hover:bg-slate-200 focus:bg-slate-200 active:bg-slate-200 disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none" type="button">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-4 h-4">
                  <path fill-rule="evenodd" d="M16.5 4.478v.227a48.816 48.816 0 0 1 3.878.512.75.75 0 1 1-.256 1.478l-.209-.035-1.005 13.07a3 3 0 0 1-2.991 2.77H8.084a3 3 0 0 1-2.991-2.77L4.087 6.66l-.209.035a.75.75 0 0 1-.256-1.478A48.567 48.567 0 0 1 7.5 4.705v-.227c0-1.564 1.213-2.9 2.816-2.951a52.662 52.662 0 0 1 3.369 0c1.603.051 2.815 1.387 2.815 2.951Zm-6.136-1.452a51.196 51.196 0 0 1 3.273 0C14.39 3.05 15 3.684 15 4.478v.113a49.488 49.488 0 0 0-6 0v-.113c0-.794.609-1.428 1.364-1.452Zm-.355 5.945a.75.75 0 1 0-1.5.058l.347 9a.75.75 0 1 0 1.499-.058l-.346-9Zm5.48.058a.75.75 0 1 0-1.498-.058l-.347 9a.75.75 0 0 0 1.5.058l.345-9Z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
          </td>
        </tr>

        </tbody>
      </table>
    </div>
  </div>
  </div>
</template>
<script>
import {listenTeams, removeUserFromTeam} from "../firebase/TeamService.js";
import TeamList from "../components/TeamList.vue";
import {auth} from "../firebase/Firebase.js";
import {createToast} from "mosha-vue-toastify";

export default {
  name: "Teams",
  components: {TeamList},
  data() {
    return {
      teams: [],
      selectedTeamId: "",
      selectedTeam: {}
    }
  },
  created() {
    listenTeams((teams) => {
      this.teams = teams
      this.selectTeam(teams[0].id)
      console.log(Array.from(teams[0].members))
    })

  },
  methods: {
    selectTeam(teamId) {
      this.selectedTeamId = teamId
      this.selectedTeam = this.teams.find((team) => team.id === teamId)
    },
    removeMember(email) {
      if(this.isAdmin){
        removeUserFromTeam(this.selectedTeamId, this.selectedTeam.members[email])
      }else{
       createToast('Only team admin can remove members. ',{type:'danger',position:'top-center'})
      }
    }
  },
  computed: {
    isAdmin() {
      return auth.currentUser.email === this.selectedTeam.adminEmail
    }
  }
}
</script>