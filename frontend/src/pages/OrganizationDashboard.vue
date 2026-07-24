<template>
  <div class="min-h-screen bg-gray-50 p-4 sm:p-6">
    <div class="max-w-6xl mx-auto space-y-6">

      <!-- Başlık -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 bg-indigo-100 rounded-xl flex items-center justify-center">
            <svg class="w-6 h-6 text-indigo-600" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-2a1 1 0 00-1-1H9a1 1 0 00-1 1v2a1 1 0 01-1 1H4a1 1 0 110-2V4zm3 1h2v2H7V5zm2 4H7v2h2V9zm2-4h2v2h-2V5zm2 4h-2v2h2V9z" clip-rule="evenodd"/>
            </svg>
          </div>
          <div>
            <h1 class="text-2xl font-bold text-gray-900">
              {{ currentOrg?.name || 'Organizasyon' }}
            </h1>
            <p class="text-gray-500 text-sm mt-0.5">{{ currentOrg?.slug || 'Organizasyon seçin' }}</p>
          </div>
        </div>
        <OrgSwitcher @create-org="showCreateOrgModal = true" />
      </div>

      <!-- Tab navigator -->
      <div class="bg-white rounded-xl shadow border border-gray-200">
        <div class="flex gap-1 p-2 border-b border-gray-100 overflow-x-auto">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            :class="[
              'flex items-center gap-2 px-4 py-2 text-sm rounded-lg transition-all font-medium whitespace-nowrap',
              activeTab === tab.id
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
            ]"
          >
            <span v-html="tab.icon" class="w-4 h-4"></span>
            {{ tab.label }}
          </button>
        </div>

        <!-- Tab Content -->
        <div class="p-6">

          <!-- Projeler -->
          <div v-if="activeTab === 'projects'">
            <ProjectList
              :projects="projects"
              :loading="projectsLoading"
              @create="showCreateProjectModal = true"
              @select="goToProject"
            />
          </div>

          <!-- Takımlar -->
          <div v-if="activeTab === 'teams' && currentOrg">
            <div class="flex items-center justify-between mb-6">
              <div>
                <h2 class="text-lg font-semibold text-gray-900">Takımlar</h2>
                <p class="text-sm text-gray-500 mt-0.5">Organizasyona ait takımlar</p>
              </div>
              <button
                v-if="isOrgAdmin"
                @click="showCreateTeamModal = true"
                class="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
                Takım Oluştur
              </button>
            </div>

            <!-- Takım Listesi -->
            <div v-if="teamsLoading" class="flex items-center justify-center py-12">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
            </div>

            <div v-else-if="teams.length === 0" class="text-center py-16">
              <div class="w-16 h-16 bg-indigo-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg class="w-8 h-8 text-indigo-400" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"/>
                </svg>
              </div>
              <h3 class="text-base font-semibold text-gray-700 mb-1">Henüz takım yok</h3>
              <p class="text-sm text-gray-500">
                <span v-if="isOrgAdmin">Sağ üstten yeni bir takım oluşturun.</span>
                <span v-else>Bir yönetici tarafından takım oluşturulmasını bekleyin.</span>
              </p>
            </div>

            <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div
                v-for="team in teams"
                :key="team.id"
                class="bg-white border border-gray-200 rounded-xl p-5 hover:shadow-md transition-shadow"
              >
                <!-- Team Header -->
                <div class="flex items-center gap-3 mb-3">
                  <div class="w-10 h-10 bg-gradient-to-r from-green-400 to-blue-500 rounded-lg flex items-center justify-center flex-shrink-0">
                    <span class="text-white font-bold text-sm">{{ team.teamName?.charAt(0)?.toUpperCase() }}</span>
                  </div>
                  <div class="min-w-0">
                    <h3 class="font-semibold text-gray-900 truncate">{{ team.teamName }}</h3>
                    <span class="text-xs font-mono text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">{{ team.teamCode }}</span>
                  </div>
                </div>

                <!-- Members count -->
                <p class="text-sm text-gray-500 mb-4">
                  {{ team.memberEmails?.length || 0 }} üye
                </p>

                <!-- Members Preview -->
                <div v-if="team.memberEmails?.length" class="mb-4">
                  <div class="flex flex-wrap gap-1">
                    <span
                      v-for="email in team.memberEmails.slice(0, 3)"
                      :key="email"
                      class="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full truncate max-w-[120px]"
                      :title="email"
                    >{{ email }}</span>
                    <span v-if="team.memberEmails.length > 3" class="text-xs text-gray-400 px-2 py-0.5">
                      +{{ team.memberEmails.length - 3 }} daha
                    </span>
                  </div>
                </div>

                <!-- Add Member Button -->
                <button
                  v-if="isOrgAdmin || isTeamAdmin(team)"
                  @click="openAddMemberModal(team)"
                  class="w-full flex items-center justify-center gap-2 px-3 py-2 border border-indigo-200 text-indigo-600 text-sm rounded-lg hover:bg-indigo-50 transition-colors"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"/>
                  </svg>
                  Üye Ekle
                </button>
              </div>
            </div>
          </div>

          <!-- Üyeler -->
          <div v-if="activeTab === 'members' && currentOrg">
            <OrgMemberList :orgId="currentOrg.id" />
          </div>

          <!-- Davetler -->
          <div v-if="activeTab === 'invitations'">
            <div class="flex items-center justify-between mb-6">
              <div>
                <h2 class="text-lg font-semibold text-gray-900">Bekleyen Davetler</h2>
                <p class="text-sm text-gray-500 mt-0.5">Organizasyona gönderilen davetleri yönetin</p>
              </div>
              <button @click="showInviteModal = true" class="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
                Davet Gönder
              </button>
            </div>
            <PendingInvitations />
          </div>

          <!-- Abonelik -->
          <div v-if="activeTab === 'billing' && currentOrg">
            <BillingTab v-if="isOrgAdmin" :org="currentOrg" />
            <p v-else class="text-center py-12 text-gray-400">
              Abonelik bilgilerini sadece organizasyon sahibi ve adminleri görüntüleyebilir.
            </p>
          </div>

          <!-- Entegrasyonlar (Git/SCM + CI/CD) -->
          <div v-if="activeTab === 'integrations' && currentOrg">
            <template v-if="isOrgAdmin">
              <!-- Alt sekmeler -->
              <div class="flex gap-1 mb-6 border-b border-gray-200">
                <button
                  v-for="sub in integrationSubTabs"
                  :key="sub.id"
                  @click="integrationSubTab = sub.id"
                  :class="[
                    'px-4 py-2.5 text-sm font-medium -mb-px border-b-2 transition-colors',
                    integrationSubTab === sub.id
                      ? 'border-indigo-600 text-indigo-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                  ]"
                >
                  {{ sub.label }}
                </button>
              </div>
              <ScmConnectionsTab v-if="integrationSubTab === 'scm'" :org-id="currentOrg.id" />
              <CiConnectionsTab v-else-if="integrationSubTab === 'ci'" :org-id="currentOrg.id" />
            </template>
            <p v-else class="text-center py-12 text-gray-400">
              Entegrasyonları sadece organizasyon sahibi ve adminleri yönetebilir.
            </p>
          </div>

          <!-- Ayarlar -->
          <div v-if="activeTab === 'settings' && currentOrg">
            <OrgSettings :org="currentOrg" @updated="upsertOrganization($event)" />
          </div>

          <!-- Org seçilmemişse -->
          <div v-if="!currentOrg && !orgsLoading" class="text-center py-16">
            <div class="w-20 h-20 bg-indigo-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg class="w-10 h-10 text-indigo-400" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-2a1 1 0 00-1-1H9a1 1 0 00-1 1v2a1 1 0 01-1 1H4a1 1 0 110-2V4zm3 1h2v2H7V5zm2 4H7v2h2V9zm2-4h2v2h-2V5zm2 4h-2v2h2V9z" clip-rule="evenodd"/>
            </svg>
            </div>
            <h3 class="text-lg font-semibold text-gray-800 mb-2">Organizasyon Seçin</h3>
            <p class="text-gray-500 mb-6">Devam etmek için sağ üstten bir organizasyon seçin veya yeni oluşturun.</p>
            <button @click="showCreateOrgModal = true" class="px-5 py-2.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-medium">
              Organizasyon Oluştur
            </button>
          </div>

        </div>
      </div>
    </div>

    <!-- Yeni Organizasyon Modal -->
    <div v-if="showCreateOrgModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-2xl border border-gray-200">
        <div class="flex items-center gap-3 mb-5">
          <div class="w-10 h-10 bg-indigo-100 rounded-xl flex items-center justify-center">
            <svg class="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
          </div>
          <h4 class="text-lg font-semibold text-gray-900">Yeni Organizasyon</h4>
        </div>
        <form @submit.prevent="createOrg" class="space-y-4">
          <div>
            <label class="label">Ad</label>
            <input v-model="newOrg.name" type="text" required class="input-field" placeholder="Organizasyon adı" />
          </div>
          <div>
            <label class="label">Slug</label>
            <input v-model="newOrg.slug" type="text" required class="input-field font-mono" placeholder="org-slug" />
          </div>
          <div>
            <label class="label">Açıklama</label>
            <textarea v-model="newOrg.description" rows="2" class="input-field" placeholder="Opsiyonel"></textarea>
          </div>
          <div class="flex gap-2 justify-end pt-2">
            <button type="button" @click="showCreateOrgModal = false" class="btn-secondary">İptal</button>
            <button type="submit" :disabled="creatingOrg" class="btn-primary">
              {{ creatingOrg ? 'Oluşturuluyor...' : 'Oluştur' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Yeni Takım Modal -->
    <div v-if="showCreateTeamModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-2xl border border-gray-200">
        <div class="flex items-center gap-3 mb-5">
          <div class="w-10 h-10 bg-green-100 rounded-xl flex items-center justify-center">
            <svg class="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
          </div>
          <h4 class="text-lg font-semibold text-gray-900">Yeni Takım</h4>
        </div>
        <form @submit.prevent="createTeam" class="space-y-4">
          <div>
            <label class="label">Takım Adı</label>
            <input v-model="newTeam.teamName" @input="generateTeamCode" type="text" required class="input-field" placeholder="Takım adı" />
          </div>
          <div>
            <label class="label">Takım Kodu</label>
            <input v-model="newTeam.teamCode" type="text" required class="input-field font-mono uppercase"
              @input="newTeam.teamCode = $event.target.value.toUpperCase()"
              placeholder="TKM" maxlength="4" minlength="2" />
            <p class="text-xs text-gray-400 mt-1">2-4 karakter kısa kod</p>
          </div>
          <div class="flex gap-2 justify-end pt-2">
            <button type="button" @click="showCreateTeamModal = false" class="btn-secondary">İptal</button>
            <button type="submit" :disabled="creatingTeam || newTeam.teamCode.length < 2" class="btn-primary">
              {{ creatingTeam ? 'Oluşturuluyor...' : 'Oluştur' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Üye Ekleme Modal -->
    <div v-if="showAddMemberModal && selectedTeamForMember" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-2xl border border-gray-200">
        <div class="flex items-center gap-3 mb-5">
          <div class="w-10 h-10 bg-blue-100 rounded-xl flex items-center justify-center">
            <svg class="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
              <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"/>
            </svg>
          </div>
          <div>
            <h4 class="text-lg font-semibold text-gray-900">Üye Ekle</h4>
            <p class="text-sm text-gray-500">{{ selectedTeamForMember.teamName }}</p>
          </div>
        </div>

        <!-- Organizasyon üyelerinden seçim -->
        <div class="space-y-3">
          <label class="label">Organizasyon Üyesi Seç</label>
          <div class="max-h-64 overflow-y-auto border border-gray-200 rounded-lg divide-y divide-gray-100">
            <div
              v-for="member in availableMembersForTeam"
              :key="member.userEmail"
              @click="toggleMemberSelection(member.userEmail)"
              class="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 cursor-pointer transition-colors"
              :class="{ 'bg-indigo-50': selectedMembersToAdd.includes(member.userEmail) }"
            >
              <div class="w-8 h-8 bg-indigo-100 rounded-full flex items-center justify-center flex-shrink-0">
                <span class="text-indigo-600 font-bold text-xs">{{ member.userName?.charAt(0)?.toUpperCase() || '?' }}</span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-gray-900 truncate">{{ member.userName }}</p>
                <p class="text-xs text-gray-400 truncate">{{ member.userEmail }}</p>
              </div>
              <div v-if="selectedMembersToAdd.includes(member.userEmail)" class="text-indigo-600 flex-shrink-0">
                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
              </div>
            </div>
            <div v-if="availableMembersForTeam.length === 0" class="px-4 py-6 text-center text-sm text-gray-500">
              Tüm organizasyon üyeleri zaten bu takımda.
            </div>
          </div>
        </div>

        <div class="flex gap-2 justify-end pt-4">
          <button type="button" @click="closeAddMemberModal" class="btn-secondary">İptal</button>
          <button
            @click="addSelectedMembers"
            :disabled="addingMember || selectedMembersToAdd.length === 0"
            class="btn-primary"
          >
            {{ addingMember ? 'Ekleniyor...' : `Ekle (${selectedMembersToAdd.length})` }}
          </button>
        </div>
      </div>
    </div>

    <!-- Yeni Proje Modal -->
    <div v-if="showCreateProjectModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-2xl border border-gray-200">
        <div class="flex items-center gap-3 mb-5">
          <div class="w-10 h-10 bg-blue-100 rounded-xl flex items-center justify-center">
            <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
          </div>
          <h4 class="text-lg font-semibold text-gray-900">Yeni Proje</h4>
        </div>
        <form @submit.prevent="createProject" class="space-y-4">
          <div>
            <label class="label">Proje Adı</label>
            <input v-model="newProject.name" type="text" required class="input-field" placeholder="Proje adı" />
          </div>
          <div>
            <label class="label">Anahtar (KEY)</label>
            <input v-model="newProject.key" type="text" required class="input-field font-mono uppercase"
              @input="newProject.key = $event.target.value.toUpperCase()"
              placeholder="SCRM" maxlength="10" />
          </div>
          <div>
            <label class="label">Tip</label>
            <select v-model="newProject.projectType" class="input-field">
              <option value="SCRUM">Scrum</option>
              <option value="KANBAN">Kanban</option>
              <option value="BUG_TRACKING">Bug Tracking</option>
              <option value="CUSTOM">Özel</option>
            </select>
          </div>
          <div class="flex gap-2 justify-end pt-2">
            <button type="button" @click="showCreateProjectModal = false" class="btn-secondary">İptal</button>
            <button type="submit" :disabled="creatingProject" class="btn-primary">
              {{ creatingProject ? 'Oluşturuluyor...' : 'Oluştur' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Davet Modal -->
    <InviteModal
      v-if="showInviteModal"
      :default-type="'ORGANIZATION'"
      :default-target-id="currentOrg?.id || ''"
      @close="showInviteModal = false"
      @sent="showInviteModal = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import OrgSwitcher from '../components/organization/OrgSwitcher.vue'
import OrgSettings from '../components/organization/OrgSettings.vue'
import OrgMemberList from '../components/organization/OrgMemberList.vue'
import BillingTab from '../components/billing/BillingTab.vue'
import ScmConnectionsTab from '../components/scm/ScmConnectionsTab.vue'
import CiConnectionsTab from '../components/ci/CiConnectionsTab.vue'
import ProjectList from '../components/project/ProjectList.vue'
import PendingInvitations from '../components/invitation/PendingInvitations.vue'
import InviteModal from '../components/invitation/InviteModal.vue'
import OrganizationApi from '../api/OrganizationApi.js'
import ProjectApi from '../api/ProjectApi.js'
import { getTeamsByOrg, createTeam as apiCreateTeam, addMemberToTeam } from '../api/TeamApi.js'
import { useAuth } from '../composables/useAuth.js'
import { useOrganizationContext } from '../composables/useOrganizationContext.js'
import { useTeamContext } from '../composables/useTeamContext.js'

const router = useRouter()
const auth = useAuth()

// Aktif organizasyon paylaşılan context'te; switcher ve ayarlar ekranı aynı
// seçimi yazar, seçim localStorage'da kalıcıdır.
const {
  activeOrg: currentOrg,
  loading: orgsLoading,
  loadOrganizations,
  selectOrg,
  upsertOrganization,
} = useOrganizationContext()

const activeTab = ref('projects')
const integrationSubTab = ref('scm')
const integrationSubTabs = [
  { id: 'scm', label: 'Git / SCM' },
  { id: 'ci', label: 'CI/CD' },
]
const tabs = [
  { id: 'projects',    label: 'Projeler',  icon: '<svg fill="currentColor" viewBox="0 0 20 20"><path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"/></svg>' },
  { id: 'teams',       label: 'Takımlar',  icon: '<svg fill="currentColor" viewBox="0 0 20 20"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"/></svg>' },
  { id: 'members',     label: 'Üyeler',    icon: '<svg fill="currentColor" viewBox="0 0 20 20"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z"/></svg>' },
  { id: 'invitations', label: 'Davetler',  icon: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/></svg>' },
  { id: 'billing',     label: 'Abonelik',  icon: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"/></svg>' },
  { id: 'integrations', label: 'Entegrasyonlar', icon: '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/></svg>' },
  { id: 'settings',    label: 'Ayarlar',   icon: '<svg fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"/></svg>' },
]

// ─── Projects ────────────────────────────────────────────────────────────────
const projects = ref([])
const projectsLoading = ref(false)
const showCreateProjectModal = ref(false)
const newProject = ref({ name: '', key: '', projectType: 'SCRUM' })
const creatingProject = ref(false)

// ─── Teams ───────────────────────────────────────────────────────────────────
const teams = ref([])
const teamsLoading = ref(false)
const showCreateTeamModal = ref(false)
const newTeam = ref({ teamName: '', teamCode: '' })
const creatingTeam = ref(false)

// ─── Add Member ──────────────────────────────────────────────────────────────
const orgMembers = ref([])
const showAddMemberModal = ref(false)
const selectedTeamForMember = ref(null)
const selectedMembersToAdd = ref([])
const addingMember = ref(false)

// ─── Orgs / Misc ─────────────────────────────────────────────────────────────
const showCreateOrgModal = ref(false)
const showInviteModal = ref(false)
const newOrg = ref({ name: '', slug: '', description: '' })
const creatingOrg = ref(false)

// ─── Computed ─────────────────────────────────────────────────────────────────
const currentUserEmail = computed(() => auth.userEmail.value)

const isOrgAdmin = computed(() => {
  if (!orgMembers.value.length || !currentUserEmail.value) return false
  const me = orgMembers.value.find(m => m.userEmail === currentUserEmail.value)
  return me && (me.orgRole === 'ORG_OWNER' || me.orgRole === 'ORG_ADMIN')
})

const availableMembersForTeam = computed(() => {
  if (!selectedTeamForMember.value) return []
  const teamEmails = new Set(selectedTeamForMember.value.memberEmails || [])
  return orgMembers.value.filter(m => !teamEmails.has(m.userEmail))
})

function isTeamAdmin(team) {
  if (!currentUserEmail.value) return false
  const member = team.members?.[currentUserEmail.value]
  return member?.role === 'admin'
}

// ─── Watchers ─────────────────────────────────────────────────────────────────
// Id'yi izliyoruz, nesneyi değil: ayarlardan yapılan bir ad/logo güncellemesi
// listedeki nesneyi tazeliyor ama aynı organizasyonda kalıyoruz — veriyi yeniden
// çekmeye gerek yok. immediate: seçim context'te önceden hazır olabilir
// (kullanıcı sayfaya geri döndüğünde), o durumda watcher hiç tetiklenmezdi.
watch(() => currentOrg.value?.id, async (orgId) => {
  if (!orgId) return
  await Promise.all([
    loadProjects(orgId),
    loadTeams(orgId),
    loadOrgMembers(orgId),
  ])
}, { immediate: true })

watch(activeTab, async (tab) => {
  if (!currentOrg.value) return
  if (tab === 'teams') await loadTeams(currentOrg.value.id)
})

// UpgradeModal "Paketleri İncele" → Abonelik sekmesini aç
function openBillingTab() {
  activeTab.value = 'billing'
}
onMounted(() => {
  loadOrganizations()
  window.addEventListener('scrumtools:open-billing-tab', openBillingTab)
})
onBeforeUnmount(() => window.removeEventListener('scrumtools:open-billing-tab', openBillingTab))

// ─── Data loaders ─────────────────────────────────────────────────────────────
async function loadProjects(orgId) {
  projectsLoading.value = true
  try {
    const res = await ProjectApi.getByOrg(orgId)
    projects.value = res.data
  } catch (e) {
    console.error('Projeler yüklenemedi:', e)
  } finally {
    projectsLoading.value = false
  }
}

async function loadTeams(orgId) {
  teamsLoading.value = true
  try {
    teams.value = await getTeamsByOrg(orgId)
  } catch (e) {
    console.error('Takımlar yüklenemedi:', e)
  } finally {
    teamsLoading.value = false
  }
}

async function loadOrgMembers(orgId) {
  try {
    const res = await OrganizationApi.getMembers(orgId)
    orgMembers.value = res.data
  } catch (e) {
    console.error('Org üyeler yüklenemedi:', e)
  }
}

// ─── Org actions ──────────────────────────────────────────────────────────────
async function createOrg() {
  creatingOrg.value = true
  try {
    const res = await OrganizationApi.create(newOrg.value)
    // Listeye ekleyip aktif yap — yeni organizasyon switcher'da anında görünür
    upsertOrganization(res.data)
    selectOrg(res.data)
    showCreateOrgModal.value = false
    newOrg.value = { name: '', slug: '', description: '' }
  } catch (e) {
    console.error('Organizasyon oluşturulamadı:', e)
  } finally {
    creatingOrg.value = false
  }
}

// ─── Team actions ─────────────────────────────────────────────────────────────
function generateTeamCode() {
  const name = newTeam.value.teamName.trim()
  if (!name) { newTeam.value.teamCode = ''; return }
  const words = name.split(/\s+/).filter(w => w.length > 0)
  let code = words.length > 1
    ? words.map(w => w.charAt(0).toUpperCase()).join('').substring(0, 4)
    : name.substring(0, Math.min(4, Math.max(2, name.length))).toUpperCase()
  newTeam.value.teamCode = code
}

async function createTeam() {
  if (!currentOrg.value || newTeam.value.teamCode.length < 2) return
  creatingTeam.value = true
  try {
    const team = await apiCreateTeam(
      currentOrg.value.id,
      newTeam.value.teamName.trim(),
      newTeam.value.teamCode.trim().toUpperCase()
    )
    teams.value.push(team)
    showCreateTeamModal.value = false
    newTeam.value = { teamName: '', teamCode: '' }
    // Merkezi takım context'i tazele: yeni takım Ayarlar > Çalışma Alanı
    // seçicisinde ve modüllerde hemen görünsün
    useTeamContext().loadTeams({ force: true })
  } catch (e) {
    console.error('Takım oluşturulamadı:', e)
  } finally {
    creatingTeam.value = false
  }
}

// ─── Add Member actions ───────────────────────────────────────────────────────
function openAddMemberModal(team) {
  selectedTeamForMember.value = team
  selectedMembersToAdd.value = []
  showAddMemberModal.value = true
}

function closeAddMemberModal() {
  showAddMemberModal.value = false
  selectedTeamForMember.value = null
  selectedMembersToAdd.value = []
}

function toggleMemberSelection(email) {
  const idx = selectedMembersToAdd.value.indexOf(email)
  if (idx === -1) selectedMembersToAdd.value.push(email)
  else selectedMembersToAdd.value.splice(idx, 1)
}

async function addSelectedMembers() {
  if (!currentOrg.value || !selectedTeamForMember.value || selectedMembersToAdd.value.length === 0) return
  addingMember.value = true
  try {
    for (const email of selectedMembersToAdd.value) {
      const updated = await addMemberToTeam(currentOrg.value.id, selectedTeamForMember.value.id, email)
      // Takım listesini güncelle
      const idx = teams.value.findIndex(t => t.id === updated.id)
      if (idx !== -1) teams.value[idx] = updated
    }
    closeAddMemberModal()
  } catch (e) {
    console.error('Üye eklenemedi:', e)
    alert(e?.response?.data?.message || 'Üye eklenirken hata oluştu.')
  } finally {
    addingMember.value = false
  }
}

// ─── Project actions ──────────────────────────────────────────────────────────
async function createProject() {
  if (!currentOrg.value) return
  creatingProject.value = true
  try {
    const res = await ProjectApi.create(currentOrg.value.id, newProject.value)
    projects.value.push(res.data)
    showCreateProjectModal.value = false
    newProject.value = { name: '', key: '', projectType: 'SCRUM' }
  } catch (e) {
    console.error('Proje oluşturulamadı:', e)
  } finally {
    creatingProject.value = false
  }
}

function goToProject(project) {
  router.push(`/projects/${project.id}`)
}
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors; }
.label { @apply block text-sm font-medium text-gray-700 mb-1.5; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium text-sm; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium text-sm; }
</style>
