<template>
  <div class="space-y-4">
    <!-- Başlık & Davet -->
    <div class="flex items-center justify-between">
      <h3 class="text-lg font-semibold text-gray-900">
        Üyeler
        <span class="ml-2 text-sm font-normal text-gray-500">({{ members.length }})</span>
      </h3>
      <button @click="showInviteModal = true" class="btn-primary text-sm flex items-center gap-1">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        Üye Ekle
      </button>
    </div>

    <!-- Tablo -->
    <div class="overflow-x-auto rounded-xl border border-gray-200">
      <table class="w-full text-sm">
        <thead class="bg-gray-50">
          <tr>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Kullanıcı</th>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Rol</th>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Katılım</th>
            <th class="px-4 py-3"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-if="loading">
            <td colspan="4" class="text-center py-8 text-gray-500">Yükleniyor...</td>
          </tr>
          <tr v-else-if="members.length === 0">
            <td colspan="4" class="text-center py-8 text-gray-400">Henüz üye yok.</td>
          </tr>
          <tr
            v-for="member in members"
            :key="member.id"
            class="hover:bg-gray-50 transition-colors"
          >
            <td class="px-4 py-3">
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-full bg-indigo-500 flex items-center justify-center text-white text-xs font-bold">
                  {{ member.userName?.charAt(0)?.toUpperCase() || '?' }}
                </div>
                <div>
                  <p class="font-medium text-gray-900">{{ member.userName }}</p>
                  <p class="text-xs text-gray-500">{{ member.userEmail }}</p>
                </div>
              </div>
            </td>
            <td class="px-4 py-3">
              <span class="px-2 py-1 rounded-full text-xs font-medium" :class="roleBadgeClass(member.orgRole)">
                {{ formatRole(member.orgRole) }}
              </span>
            </td>
            <td class="px-4 py-3 text-gray-500 text-xs">
              {{ formatDate(member.joinedAt) }}
            </td>
            <td class="px-4 py-3 text-right">
              <button
                v-if="member.orgRole !== 'ORG_OWNER'"
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

    <!-- Üye ekleme — hesabı olmayan kullanıcı için hesap oluşturulur, şifre-kurulum maili gider -->
    <div v-if="showInviteModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl p-6 w-full max-w-md shadow-2xl">
        <h4 class="text-lg font-semibold mb-1 text-gray-900">Üye Ekle</h4>
        <p class="text-xs text-gray-500 mb-4">
          Üyenin hesabı yoksa otomatik oluşturulur ve e-postasına şifre belirleme bağlantısı gönderilir.
        </p>
        <div class="space-y-3">
          <div>
            <label class="label">Ad Soyad</label>
            <input v-model="inviteName" type="text" class="input-field" placeholder="Ad Soyad" />
          </div>
          <div>
            <label class="label">E-posta</label>
            <input v-model="inviteEmail" type="email" class="input-field" placeholder="kullanici@example.com" />
          </div>
          <div>
            <label class="label">Rol</label>
            <select v-model="inviteRole" class="input-field">
              <option value="ORG_MEMBER">Üye</option>
              <option value="ORG_ADMIN">Admin</option>
              <option value="ORG_VIEWER">Gözlemci</option>
            </select>
          </div>
        </div>
        <div class="flex gap-2 mt-4 justify-end">
          <button @click="showInviteModal = false" class="btn-secondary">İptal</button>
          <button @click="addMember" :disabled="!inviteEmail || !inviteName || adding" class="btn-primary">
            {{ adding ? 'Ekleniyor...' : 'Ekle' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import OrganizationApi from '../../api/OrganizationApi.js'

const props = defineProps({
  orgId: { type: String, required: true }
})

const members = ref([])
const loading = ref(false)
const showInviteModal = ref(false)
const inviteName = ref('')
const inviteEmail = ref('')
const inviteRole = ref('ORG_MEMBER')
const adding = ref(false)

async function loadMembers() {
  loading.value = true
  try {
    const res = await OrganizationApi.getMembers(props.orgId)
    members.value = res.data
  } catch (e) {
    console.error('Üyeler yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

async function addMember() {
  adding.value = true
  try {
    await OrganizationApi.createMember(props.orgId, {
      email: inviteEmail.value,
      name: inviteName.value,
      orgRole: inviteRole.value,
    })
    createToast('Üye eklendi. Hesabı yoksa şifre kurulum e-postası gönderildi.', {
      type: 'success', position: 'top-center',
    })
    showInviteModal.value = false
    inviteEmail.value = ''
    inviteName.value = ''
    await loadMembers()
  } catch (e) {
    console.error('Üye eklenemedi:', e)
  } finally {
    adding.value = false
  }
}

async function removeMember(member) {
  if (!confirm(`${member.userName} üyeyi çıkarmak istediğinize emin misiniz?`)) return
  try {
    await OrganizationApi.removeMember(props.orgId, member.userId)
    await loadMembers()
  } catch (e) {
    console.error('Üye çıkarılamadı:', e)
  }
}

function formatRole(role) {
  const map = {
    ORG_OWNER: 'Sahip',
    ORG_ADMIN: 'Admin',
    ORG_MEMBER: 'Üye',
    ORG_VIEWER: 'Gözlemci',
  }
  return map[role] || role
}

function roleBadgeClass(role) {
  if (role === 'ORG_OWNER') return 'bg-purple-100 text-purple-700'
  if (role === 'ORG_ADMIN') return 'bg-blue-100 text-blue-700'
  if (role === 'ORG_VIEWER') return 'bg-gray-100 text-gray-600'
  return 'bg-green-100 text-green-700'
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('tr-TR')
}

onMounted(loadMembers)
</script>

<style scoped>
.input-field {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500;
}
.label { @apply block text-sm font-medium text-gray-700 mb-1; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors; }
</style>

