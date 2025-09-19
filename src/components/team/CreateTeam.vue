<template>
  <div class="relative flex flex-col shadow-sm border border-slate-200 rounded-lg lg:w-96 bg-white p-1 mx-2">
    <div class="flex justify-end border-b py-1">
      <span class="w-full flex items-center ml-2 font-bold">Create Team</span>
      <button @click="$emit('close')" class="rounded-2xl font-extrabold text-md bg-slate-800 py-1 px-2 border border-transparent text-center text-white transition-all shadow-md hover:shadow-lg focus:bg-slate-700 focus:shadow-none active:bg-slate-700 hover:bg-red-800 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none" type="button">
        x
      </button>
    </div>
    <div class="p-5 my-3">
      <div class="w-full max-w-sm min-w-[200px] space-y-4">
        <!-- Team Name Input -->
        <div class="relative">
          <label class="block text-sm font-medium text-slate-700 mb-2">Team Name</label>
          <input
            type="text"
            v-model="teamName"
            @input="generateTeamCode"
            class="w-full bg-transparent placeholder:text-slate-400 text-slate-700 text-sm border border-slate-200 rounded-md px-3 py-2 transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow"
            placeholder="Enter team name"
          />
        </div>

        <!-- Team Code Input -->
        <div class="relative">
          <label class="block text-sm font-medium text-slate-700 mb-2">Team Code</label>
          <input
            type="text"
            v-model="teamCode"
            maxlength="4"
            class="w-full bg-transparent placeholder:text-slate-400 text-slate-700 text-sm border border-slate-200 rounded-md px-3 py-2 transition duration-300 ease focus:outline-none focus:border-slate-400 hover:border-slate-300 shadow-sm focus:shadow"
            placeholder="Team code (2-4 characters)"
          />
          <p class="text-xs text-slate-500 mt-1">2-4 character unique identifier for your team</p>
        </div>

        <!-- Create Button -->
        <button
          class="w-full bg-green-700 py-2 px-4 border border-transparent text-center text-sm text-white transition-all shadow-sm hover:shadow focus:bg-green-800 focus:shadow-none active:bg-green-800 hover:bg-green-800 active:shadow-none disabled:pointer-events-none disabled:opacity-50 disabled:shadow-none rounded-md"
          type="button"
          @click="createTeam()"
          :disabled="!teamName.trim() || !teamCode.trim() || teamCode.length < 2"
        >
          Create Team
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { createTeam } from "../../firebase/TeamService.js";
import { auth } from "../../firebase/Firebase.js";

export default {
  name: "CreateTeam",
  data: () => ({
    teamName: "",
    teamCode: "",
  }),

  methods: {
    generateTeamCode() {
      if (!this.teamName.trim()) {
        this.teamCode = "";
        return;
      }

      const name = this.teamName.trim();
      let code = "";

      // Eğer birden fazla kelime varsa baş harflerini al
      const words = name.split(/\s+/).filter(word => word.length > 0);

      if (words.length > 1) {
        // Birden fazla kelime varsa baş harflerini al
        code = words.map(word => word.charAt(0).toUpperCase()).join('').substring(0, 4);
      } else {
        // Tek kelime varsa ilk 2-4 karakterini al
        code = name.substring(0, Math.min(4, Math.max(2, name.length))).toUpperCase();
      }

      this.teamCode = code;
    },

    createTeam() {
      if (!this.teamName.trim() || !this.teamCode.trim() || this.teamCode.length < 2) {
        return;
      }

      createTeam(this.teamName.trim(), this.teamCode.trim().toUpperCase(), auth.currentUser.email, auth.currentUser.displayName);
      this.$emit('close');
    }
  }
}
</script>
