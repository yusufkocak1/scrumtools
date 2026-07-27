<template>
  <div class="space-y-5">
    <!-- Başlık -->
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-gray-900">Üyeler</h2>
        <p class="text-sm text-gray-500 mt-0.5">
          Organizasyon üyeleri ve gönderilen davetler tek yerde
        </p>
      </div>
      <button
        v-if="can.inviteMembers"
        @click="openInviteModal"
        class="btn-primary flex items-center gap-1.5"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        Üye Ekle
      </button>
    </div>

    <!-- Segment: Üyeler / Davetler -->
    <div class="flex gap-1 p-1 bg-gray-100 rounded-lg w-fit text-sm">
      <button
        v-for="seg in segments"
        :key="seg.id"
        @click="segment = seg.id"
        :class="[
          'px-4 py-1.5 rounded-md transition-all font-medium flex items-center gap-2',
          segment === seg.id ? 'bg-white shadow text-indigo-600' : 'text-gray-600 hover:text-gray-900'
        ]"
      >
        {{ seg.label }}
        <span
          v-if="seg.count !== null"
          :class="[
            'text-xs px-1.5 py-0.5 rounded-full',
            segment === seg.id ? 'bg-indigo-50 text-indigo-600' : 'bg-gray-200 text-gray-600'
          ]"
        >{{ seg.count }}</span>
      </button>
    </div>

    <!-- Üyeler -->
    <div v-if="segment === 'members'" class="overflow-x-auto rounded-xl border border-gray-200">
      <table class="w-full text-sm">
        <thead class="bg-gray-50">
          <tr>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Kullanıcı</th>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Rol</th>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Takımlar</th>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Katılım</th>
            <th class="px-4 py-3"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-if="loading">
            <td colspan="5" class="text-center py-8 text-gray-500">Yükleniyor...</td>
          </tr>
          <tr v-else-if="members.length === 0">
            <td colspan="5" class="text-center py-8 text-gray-400">Henüz üye yok.</td>
          </tr>
          <tr v-for="member in members" :key="member.id" class="hover:bg-gray-50 transition-colors">
            <td class="px-4 py-3">
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-full bg-indigo-500 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                  {{ member.userName?.charAt(0)?.toUpperCase() || '?' }}
                </div>
                <div class="min-w-0">
                  <p class="font-medium text-gray-900 truncate">{{ member.userName }}</p>
                  <p class="text-xs text-gray-500 truncate">{{ member.userEmail }}</p>
                </div>
              </div>
            </td>
            <td class="px-4 py-3">
              <!-- Rol değişimi sadece sahibe açık; diğerleri salt okunur rozet görür -->
              <select
                v-if="can.changeMemberRole && member.orgRole !== 'ORG_OWNER'"
                :value="member.orgRole"
                @change="changeRole(member, $event.target.value)"
                class="text-xs border border-gray-200 rounded-lg px-2 py-1 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="ORG_ADMIN">Admin</option>
                <option value="ORG_MEMBER">Üye</option>
                <option value="ORG_VIEWER">Gözlemci</option>
              </select>
              <span v-else class="px-2 py-1 rounded-full text-xs font-medium" :class="roleBadgeClass(member.orgRole)">
                {{ roleLabel(member.orgRole) }}
              </span>
            </td>
            <td class="px-4 py-3">
              <div class="flex flex-wrap gap-1">
                <span
                  v-for="t in teamsOf(member.userEmail)"
                  :key="t.id"
                  class="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full"
                >{{ t.teamName }}</span>
                <span v-if="teamsOf(member.userEmail).length === 0" class="text-xs text-gray-400 italic">—</span>
              </div>
            </td>
            <td class="px-4 py-3 text-gray-500 text-xs whitespace-nowrap">
              {{ formatDate(member.joinedAt) }}
            </td>
            <td class="px-4 py-3 text-right">
              <button
                v-if="can.removeMembers && member.orgRole !== 'ORG_OWNER'"
                @click="removeMember(member)"
                class="text-red-500 hover:text-red-700 text-xs"
              >
                Çıkar
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Davetler -->
    <OrgSentInvitations v-else-if="can.inviteMembers" ref="invitesRef" :orgId="orgId" />

    <!-- Üye ekleme — hesabı yoksa oluşturulur, şifre-kurulum maili gider -->
    <div v-if="showInviteModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-2xl max-h-[90vh] overflow-y-auto">
        <h4 class="text-lg font-semibold mb-1 text-gray-900">Üye Ekle</h4>
        <p class="text-xs text-gray-500 mb-4">
          Üyenin hesabı yoksa otomatik oluşturulur ve e-postasına şifre belirleme bağlantısı gönderilir.
        </p>

        <div class="space-y-3">
          <div>
            <label class="label">Ad Soyad</label>
            <input v-model="form.name" type="text" class="input-field" placeholder="Ad Soyad" />
          </div>
          <div>
            <label class="label">E-posta</label>
            <input v-model="form.email" type="email" class="input-field" placeholder="kullanici@example.com" />
          </div>
          <div>
            <label class="label">Organizasyon Rolü</label>
            <select v-model="form.orgRole" class="input-field">
              <option value="ORG_MEMBER">Üye</option>
              <option value="ORG_ADMIN">Admin</option>
              <option value="ORG_VIEWER">Gözlemci</option>
            </select>
          </div>

          <!-- Takım seçimi: takım bir projeye bağlıysa üye o projeye de düşer -->
          <div>
            <label class="label">Takımlar <span class="font-normal text-gray-400">(opsiyonel)</span></label>
            <div v-if="teams.length === 0" class="text-xs text-gray-400 border border-gray-200 rounded-lg px-3 py-3 text-center">
              Bu organizasyonda henüz takım yok.
            </div>
            <div v-else class="border border-gray-200 rounded-lg divide-y divide-gray-100 max-h-44 overflow-y-auto">
              <label
                v-for="team in teams"
                :key="team.id"
                class="flex items-center gap-2.5 px-3 py-2 cursor-pointer hover:bg-gray-50 transition-colors"
              >
                <input type="checkbox" :value="team.id" v-model="form.teamIds" class="rounded text-indigo-600" />
                <span class="w-6 h-6 rounded bg-indigo-100 flex items-center justify-center text-indigo-700 text-[10px] font-bold flex-shrink-0">
                  {{ team.teamName?.charAt(0)?.toUpperCase() }}
                </span>
                <span class="min-w-0 flex-1">
                  <span class="block text-sm text-gray-800 truncate">{{ team.teamName }}</span>
                  <span class="block text-xs text-gray-400">
                    {{ team.projects?.length || 0 }} proje · {{ team.memberEmails?.length || 0 }} üye
                  </span>
                </span>
              </label>
            </div>
            <p class="text-xs text-gray-500 mt-1.5">
              Seçilen takımlar bir projeye bağlıysa üye o projelere de otomatik eklenir.
            </p>
          </div>
        </div>

        <p v-if="error" class="mt-3 text-sm text-red-600">{{ error }}</p>

        <div class="flex gap-2 mt-5 justify-end">
          <button @click="showInviteModal = false" class="btn-secondary">İptal</button>
          <button @click="addMember" :disabled="!form.email || !form.name || adding" class="btn-primary">
            {{ adding ? 'Ekleniyor...' : 'Ekle' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import OrganizationApi from '../../api/OrganizationApi.js'
import { getTeamsByOrg } from '../../api/TeamApi.js'
import OrgSentInvitations from '../invitation/OrgSentInvitations.vue'
import { useOrgPermissions } from '../../composables/useOrgPermissions.js'

const props = defineProps({
  orgId: { type: String, required: true }
})

const emit = defineEmits(['changed'])

const { can, roleLabel, roleBadgeClass } = useOrgPermissions()

const members = ref([])
const teams = ref([])
const invites = ref([])
const loading = ref(false)
const segment = ref('members')
const invitesRef = ref(null)

const showInviteModal = ref(false)
const adding = ref(false)
const error = ref('')
const form = ref({ name: '', email: '', orgRole: 'ORG_MEMBER', teamIds: [] })

// Davet listesi yalnızca yöneticilere açık — sekme de yalnızca onlara gösterilir,
// üye görebildiği bir sekmeye tıklayıp 403 almamalı.
const segments = computed(() => [
  { id: 'members', label: 'Üyeler', count: members.value.length },
  can.value.inviteMembers && { id: 'invites', label: 'Davetler', count: invites.value.length },
].filter(Boolean))

/** Üyenin hangi takımlarda olduğu — takım listesi zaten yüklü, ek istek atılmaz. */
function teamsOf(email) {
  return teams.value.filter(t => (t.memberEmails || []).includes(email))
}

async function loadAll() {
  if (!props.orgId) return
  loading.value = true
  try {
    const [membersRes, invitesRes, teamList] = await Promise.all([
      OrganizationApi.getMembers(props.orgId),
      // Davet listesi yalnızca yöneticilere açık — üye için sessizce boş kalır
      can.value.inviteMembers
        ? OrganizationApi.getInvites(props.orgId).catch(() => ({ data: [] }))
        : Promise.resolve({ data: [] }),
      getTeamsByOrg(props.orgId).catch(() => []),
    ])
    members.value = membersRes.data
    invites.value = invitesRes.data
    teams.value = teamList
  } catch (e) {
    console.error('Üye verileri yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

function openInviteModal() {
  form.value = { name: '', email: '', orgRole: 'ORG_MEMBER', teamIds: [] }
  error.value = ''
  showInviteModal.value = true
}

async function addMember() {
  adding.value = true
  error.value = ''
  try {
    await OrganizationApi.createMember(props.orgId, {
      email: form.value.email.trim(),
      name: form.value.name.trim(),
      orgRole: form.value.orgRole,
      teamIds: form.value.teamIds,
    })
    const teamNote = form.value.teamIds.length
      ? ` ${form.value.teamIds.length} takıma eklendi.`
      : ''
    createToast(`Üye eklendi.${teamNote} Hesabı yoksa şifre kurulum e-postası gönderildi.`, {
      type: 'success', position: 'top-center',
    })
    showInviteModal.value = false
    await loadAll()
    invitesRef.value?.reload()
    emit('changed')
  } catch (e) {
    error.value = e?.response?.data?.message || 'Üye eklenemedi.'
  } finally {
    adding.value = false
  }
}

async function changeRole(member, role) {
  try {
    await OrganizationApi.updateMemberRole(props.orgId, member.userId, role)
    member.orgRole = role
    createToast('Rol güncellendi.', { type: 'success', position: 'top-center' })
  } catch (e) {
    createToast(e?.response?.data?.message || 'Rol güncellenemedi.', { type: 'danger', position: 'top-center' })
    await loadAll()
  }
}

async function removeMember(member) {
  if (!confirm(
    `${member.userName} organizasyondan çıkarılacak.\n\n` +
    'Kullanıcı bu organizasyondaki tüm takımlardan ve o takımlara bağlı projelerden de düşer. Devam edilsin mi?'
  )) return
  try {
    await OrganizationApi.removeMember(props.orgId, member.userId)
    await loadAll()
    emit('changed')
  } catch (e) {
    createToast(e?.response?.data?.message || 'Üye çıkarılamadı.', { type: 'danger', position: 'top-center' })
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('tr-TR')
}

watch(() => props.orgId, loadAll)
onMounted(loadAll)

defineExpose({ reload: loadAll })
</script>

<style scoped>
.input-field { @apply w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500; }
.label { @apply block text-sm font-medium text-gray-700 mb-1; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 text-sm rounded-lg hover:bg-gray-200 transition-colors font-medium; }
</style>
