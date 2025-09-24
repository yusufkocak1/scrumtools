<template>
  <div class="flex flex-row w-screen">
    <SideBar :team-id="currentTeam" class="hidden lg:flex"></SideBar>

    <div class="lg:ml-20 transition-all duration-300 flex-1">
      <div class="px-4 sm:px-6 lg:px-8 py-6">
        <!-- Header Section -->
        <div class="max-w-7xl mx-auto">
          <div class="mb-8">
            <h1 class="text-3xl font-bold text-gray-900 mb-2">Team Management</h1>
            <p class="text-gray-600">Manage your teams and view members</p>
          </div>
          <!-- Team Members Section -->
          <div v-if="selectedTeam && Object.keys(selectedTeam).length > 0"
               class="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">

            <!-- Section Header -->
            <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
              <div class="flex items-center justify-between">
                <div>
                  <h3 class="text-lg font-semibold text-gray-900">Team Members</h3>
                  <p class="text-sm text-gray-600 mt-1">
                    {{ Object.keys(selectedTeam.members || {}).length }} members
                  </p>
                </div>
                <div class="flex items-center space-x-2">
                  <span v-if="isAdmin" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                    <svg class="w-3 h-3 mr-1" fill="currentColor" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path>
                    </svg>
                    Admin
                  </span>
                </div>
              </div>
            </div>

            <!-- Loading State -->
            <div v-if="loading" class="p-8 text-center">
              <div class="inline-flex items-center px-4 py-2 font-semibold leading-6 text-sm shadow rounded-md text-gray-500 bg-white">
                <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-gray-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Loading...
              </div>
            </div>

            <!-- Members Grid/List -->
            <div v-else class="divide-y divide-gray-200">
              <div v-if="Object.keys(selectedTeam.members || {}).length === 0"
                   class="p-8 text-center text-gray-500">
                <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" stroke="currentColor" fill="none" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48">
                  <path d="M34 40h10v-4a6 6 0 00-10.712-3.714M34 40H14m20 0v-4a9.971 9.971 0 00-.712-3.714M14 40H4v-4a6 6 0 0110.713-3.714M14 40v-4c0-1.313.253-2.566.713-3.714m0 0A9.971 9.971 0 0124 30a9.971 9.971 0 019.287 6.286M30 14a6 6 0 11-12 0 6 6 0 0112 0zm12 6a4 4 0 11-8 0 4 4 0 018 0zm-28 0a4 4 0 11-8 0 4 4 0 018 0z" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <p class="text-lg font-medium mb-2">No members yet</p>
                <p class="text-sm">No members have been added to this team yet.</p>
              </div>

              <div v-for="member in sortedMembers"
                   :key="member.email"
                   class="p-6 hover:bg-gray-50 transition-colors duration-200 group border-b border-gray-200">
                <div class="flex items-center justify-between">
                  <div class="flex items-center space-x-4">
                    <!-- Avatar -->
                    <div class="flex-shrink-0">
                      <div class="h-12 w-12 rounded-full bg-gradient-to-r from-blue-500 to-purple-600 flex items-center justify-center text-white font-semibold text-lg">
                        {{ getInitials(member.displayName) }}
                      </div>
                    </div>

                    <!-- Member Info -->
                    <div class="flex-1 min-w-0">
                      <p class="text-lg font-semibold text-gray-900 truncate">
                        {{ member.displayName }}
                        <span v-if="member.email === selectedTeam.adminEmail"
                              class="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                          Admin
                        </span>
                        <span v-else-if="member.role"
                              :class="getRoleBadgeClass(member.role)"
                              class="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium">
                          {{ getRoleDisplayName(member.role) }}
                        </span>
                      </p>
                      <div class="flex items-center mt-1">
                        <p class="text-sm text-gray-600 truncate">{{ member.email }}</p>
                      </div>
                      <!-- Skills -->
                      <div v-if="member.skills && member.skills.length > 0" class="mt-2">
                        <div class="flex flex-wrap gap-1">
                          <span v-for="skill in member.skills" :key="skill"
                                class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                            {{ skill }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Actions -->
                  <div class="flex items-center space-x-2">
                    <!-- Edit Button (for admin and own profile) -->
                    <button v-if="isAdmin"
                            @click="editMember(member)"
                            class="group-hover:opacity-100 transition-opacity duration-200 p-2 rounded-lg text-blue-600 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
                            :title="member.email === selectedTeam.adminEmail ? 'Edit my profile' : 'Edit member'">
                      <svg class="h-5 w-5" fill="none" stroke="currentColor" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                      </svg>
                    </button>

                    <!-- Remove Button (only for admin, not for admin's own profile) -->
                    <button v-if="isAdmin && member.email !== selectedTeam.adminEmail"
                            @click="confirmRemoveMember(member.email, member)"
                            class="group-hover:opacity-100 transition-opacity duration-200 p-2 rounded-lg text-red-600 hover:bg-red-50 hover:text-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
                            title="Remove member">
                      <svg class="h-5 w-5" fill="none" stroke="currentColor" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-else class="text-center py-12">
            <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" stroke="currentColor" fill="none" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48">
              <path d="M34 40h10v-4a6 6 0 00-10.712-3.714M34 40H14m20 0v-4a9.971 9.971 0 00-.712-3.714M14 40H4v-4a6 6 0 0110.713-3.714M14 40v-4c0-1.313.253-2.566.713-3.714m0 0A9.971 9.971 0 0124 30a9.971 9.971 0 019.287 6.286M30 14a6 6 0 11-12 0 6 6 0 0112 0zm12 6a4 4 0 11-8 0 4 4 0 018 0zm-28 0a4 4 0 11-8 0 4 4 0 018 0z" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <h3 class="text-lg font-medium text-gray-900 mb-2">Select a team</h3>
            <p class="text-gray-600">Select a team from above to view members.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Member Edit Modal -->
    <div v-if="showEditModal"
         class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl max-w-md w-full mx-auto transform transition-all">
        <div class="p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-gray-900">
              {{ editingMember?.email === selectedTeam?.adminEmail ? 'Edit My Profile' : 'Edit Member Information' }}
            </h3>
            <button @click="closeEditModal" class="text-gray-400 hover:text-gray-600">
              <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>

          <div class="space-y-4">
            <!-- Member Name (readonly) -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Member Name</label>
              <input type="text"
                     :value="editingMember?.displayName"
                     readonly
                     class="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-gray-600">
            </div>

            <!-- Role Selection -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Role</label>
              <select v-model="editingMember.role"
                      class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500">
                <option value="observer">Observer</option>
                <option value="scrum_master">Scrum Master</option>
                <option value="product_owner">Product Owner</option>
                <option value="developer">Developer</option>
                <option value="analyst">Analyst</option>
                <option value="tester">Tester</option>
                <option value="technical_leader">Technical Leader</option>
              </select>
            </div>

            <!-- Skills -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Skills</label>
              <div class="space-y-2">
                <!-- Skill Selection Dropdown -->
                <div class="flex space-x-2">
                  <select v-model="selectedSkillToAdd"
                          class="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500">
                    <option value="">Select a skill...</option>
                    <option v-for="skill in availableSkills" :key="skill" :value="skill">
                      {{ skill }}
                    </option>
                  </select>
                  <button
                    type="button"
                    @click="addSkillFromDropdown"
                    :disabled="!selectedSkillToAdd || (editingMember.skills && editingMember.skills.includes(selectedSkillToAdd))"
                    class="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Add
                  </button>
                </div>

                <!-- Selected Skills Display -->
                <div v-if="editingMember.skills && editingMember.skills.length > 0" class="flex flex-wrap gap-2">
                  <span v-for="(skill, index) in editingMember.skills" :key="index"
                        class="inline-flex items-center px-2 py-1 rounded-md text-sm bg-blue-100 text-blue-800">
                    {{ skill }}
                    <button @click="removeSkill(index)" class="ml-1 text-blue-600 hover:text-blue-800">
                      <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                      </svg>
                    </button>
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div class="flex space-x-3 mt-6">
            <button @click="closeEditModal"
                    class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 border border-gray-300 rounded-lg hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500 transition-colors">
              Cancel
            </button>
            <button @click="saveMemberChanges"
                    class="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors">
              Save
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Confirmation Modal -->
    <div v-if="showConfirmModal"
         class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl max-w-md w-full mx-auto transform transition-all">
        <div class="p-6">
          <div class="flex items-center mb-4">
            <div class="mx-auto flex-shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-red-100">
              <svg class="h-6 w-6 text-red-600" fill="none" stroke="currentColor" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.268 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
              </svg>
            </div>
          </div>
          <div class="text-center">
            <h3 class="text-lg font-semibold text-gray-900 mb-2">Remove Member</h3>
            <p class="text-gray-600 mb-6">
              Are you sure you want to remove <span class="font-medium">{{ memberToRemove?.displayName }}</span> from the team?
            </p>
            <div class="flex space-x-3">
              <button @click="showConfirmModal = false"
                      class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 border border-gray-300 rounded-lg hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500 transition-colors">
                Cancel
              </button>
              <button @click="removeMemberConfirmed"
                      class="flex-1 px-4 py-2 text-sm font-medium text-white bg-red-600 border border-transparent rounded-lg hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 transition-colors">
                Remove
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {getTeamById, removeUserFromTeam, updateMemberRoleAndSkills} from "../firebase/TeamService.js";
import TeamList from "../components/team/TeamList.vue";
import SideBar from "../components/SideBar.vue";
import {auth} from "../firebase/Firebase.js";
import {createToast} from "mosha-vue-toastify";

export default {
  name: "Teams",
  components: {TeamList, SideBar},
  data() {
    return {
      teams: [],
      selectedTeamId: "",
      selectedTeam: {},
      currentTeam: "",
      loading: false,
      showConfirmModal: false,
      memberToRemove: null,
      emailToRemove: "",
      showEditModal: false,
      editingMember: null,
      newSkill: "",
      selectedSkillToAdd: "",
      availableSkills: ["Development", "Analysis", "Testing", "Reporting"]
    }
  },
  created() {

  },
  mounted() {
    // localStorage'dan selectedTeam'i al
    const storedTeam = localStorage.getItem("selectedTeam");
    if (storedTeam) {
      this.currentTeam = storedTeam;
    }
    this.loading = true;
    const self = this;
    getTeamById(this.currentTeam, (team) => {
      self.selectTeam(team);
      self.loading = false;
    });
  },
  methods: {
    selectTeam(selectedTeam) {
      console.log("Selected team:", selectedTeam);
      this.selectedTeamId = selectedTeam.teamId;
      this.selectedTeam = selectedTeam;
    },
    getInitials(name) {
      if (!name) return '?';
      return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
    },
    confirmRemoveMember(email, member) {
      this.emailToRemove = email;
      this.memberToRemove = member;
      this.showConfirmModal = true;
    },
    async removeMemberConfirmed() {
      if (this.isAdmin) {
        try {
          console.log(`Removing ${this.emailToRemove} from team ${this.selectedTeamId}`);
          await removeUserFromTeam(this.selectedTeamId, this.emailToRemove);

          // Takım verilerini yeniden yükle
          const self = this;
          await getTeamById(this.selectedTeamId, (team) => {
            self.selectTeam(team);
          });

          createToast('Member removed successfully', {
            type: 'success',
            position: 'top-center',
            timeout: 3000
          });
        } catch (error) {
          createToast('Error removing member', {
            type: 'danger',
            position: 'top-center',
            timeout: 3000
          });
        }
      } else {
        createToast('Only team admins can remove members', {
          type: 'danger',
          position: 'top-center',
          timeout: 3000
        });
      }
      this.showConfirmModal = false;
      this.memberToRemove = null;
      this.emailToRemove = "";
    },
    editMember(member) {
      this.editingMember = {
        ...member,
        skills: member.skills ? [...member.skills] : []
      };
      this.showEditModal = true;
    },
    closeEditModal() {
      this.showEditModal = false;
      this.editingMember = null;
      this.newSkill = "";
      this.selectedSkillToAdd = "";
    },
    getRoleBadgeClass(role) {
      switch (role) {
        case 'admin':
          return 'bg-green-100 text-green-800';
        case 'observer':
          return 'bg-yellow-100 text-yellow-800';
        case 'scrum_master':
          return 'bg-purple-100 text-purple-800';
        case 'product_owner':
          return 'bg-red-100 text-red-800';
        case 'developer':
          return 'bg-blue-100 text-blue-800';
        case 'analyst':
          return 'bg-indigo-100 text-indigo-800';
        case 'tester':
          return 'bg-orange-100 text-orange-800';
        case 'technical_leader':
          return 'bg-teal-100 text-teal-800';
        default:
          return 'bg-gray-100 text-gray-800';
      }
    },
    getRoleDisplayName(role) {
      switch (role) {
        case 'admin':
          return 'Admin';
        case 'observer':
          return 'Observer';
        case 'scrum_master':
          return 'Scrum Master';
        case 'product_owner':
          return 'Product Owner';
        case 'developer':
          return 'Developer';
        case 'analyst':
          return 'Analyst';
        case 'tester':
          return 'Tester';
        case 'technical_leader':
          return 'Technical Leader';
        default:
          return '';
      }
    },
    addSkill() {
      if (this.newSkill.trim() !== "") {
        const skill = this.newSkill.trim();
        if (!this.editingMember.skills) {
          this.editingMember.skills = [];
        }
        if (!this.editingMember.skills.includes(skill)) {
          this.editingMember.skills.push(skill);
        }
        this.newSkill = "";
      }
    },
    addSkillFromDropdown() {
      const skill = this.selectedSkillToAdd;
      if (skill && !this.editingMember.skills.includes(skill)) {
        this.editingMember.skills.push(skill);
        this.selectedSkillToAdd = "";
      }
    },
    removeSkill(index) {
      if (this.editingMember.skills) {
        this.editingMember.skills.splice(index, 1);
      }
    },
    async saveMemberChanges() {
      if (!this.isAdmin) {
        createToast('Only team admins can update member information', {
          type: 'danger',
          position: 'top-center',
          timeout: 3000
        });
        return;
      }

      try {
        // TeamService'den rol ve skill güncelleme fonksiyonunu çağır
        const success = await updateMemberRoleAndSkills(
          this.selectedTeamId,
          this.editingMember.email,
          this.editingMember.role,
          this.editingMember.skills || []
        );

        if (success) {
          // Takım verilerini yeniden yükle
          const self = this;
          await getTeamById(this.selectedTeamId, (team) => {
            self.selectTeam(team);
          });

          createToast('Member information updated successfully', {
            type: 'success',
            position: 'top-center',
            timeout: 3000
          });
        } else {
          createToast('Error updating member information', {
            type: 'danger',
            position: 'top-center',
            timeout: 3000
          });
        }
      } catch (error) {
        console.error('Error updating member:', error);
        createToast('Error updating member information', {
          type: 'danger',
          position: 'top-center',
          timeout: 3000
        });
      }
      this.closeEditModal();
    }
  },
  computed: {
    isAdmin() {
      return auth.currentUser?.email === this.selectedTeam?.adminEmail;
    },
    sortedMembers() {
      const membersArray = Object.entries(this.selectedTeam.members || {}).map(([email, member]) => ({ email, ...member }));
      membersArray.sort((a, b) => {
        if (a.email === this.selectedTeam.adminEmail) return -1;
        if (b.email === this.selectedTeam.adminEmail) return 1;
        return a.displayName.localeCompare(b.displayName);
      });
      return membersArray;
    }
  }
}
</script>

<style scoped>
/* Smooth transitions for all interactive elements */
* {
  transition: all 0.2s ease-in-out;
}

/* Animation for modal */
@keyframes fadeIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.transform {
  animation: fadeIn 0.2s ease-out;
}
</style>
