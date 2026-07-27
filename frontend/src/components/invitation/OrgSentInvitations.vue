<template>
  <div>
    <div v-if="loading" class="text-center py-8 text-gray-500">Yükleniyor...</div>

    <div v-else-if="invites.length === 0" class="text-center py-8 text-gray-400">
      <svg class="w-12 h-12 mx-auto mb-3 opacity-30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
      </svg>
      Henüz davet göndermediniz.
    </div>

    <div v-else class="overflow-x-auto rounded-xl border border-gray-200">
      <table class="w-full text-sm">
        <thead class="bg-gray-50">
          <tr>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Davetli</th>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Davet Durumu</th>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Mail</th>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Etkileşim</th>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Gönderen</th>
            <th class="text-left px-4 py-3 text-gray-600 font-medium">Tarih</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="inv in invites" :key="inv.id" class="hover:bg-gray-50 transition-colors">
            <td class="px-4 py-3">
              <p v-if="inv.name" class="font-medium text-gray-900">{{ inv.name }}</p>
              <p class="text-gray-900" :class="inv.name ? 'text-xs text-gray-500' : ''">{{ inv.email }}</p>
            </td>
            <td class="px-4 py-3">
              <span class="px-2 py-1 rounded-full text-xs font-medium" :class="inviteStatusClass(inv.inviteStatus)">
                {{ formatInviteStatus(inv.inviteStatus) }}
              </span>
            </td>
            <td class="px-4 py-3">
              <!-- mailStatus null → hesabı olan davetliye uygulama içi davet, mail gitmez -->
              <span
                v-if="inv.mailStatus"
                class="px-2 py-1 rounded-full text-xs font-medium"
                :class="mailStatusClass(inv.mailStatus)"
                :title="inv.failureReason || ''"
              >
                {{ formatMailStatus(inv.mailStatus) }}
              </span>
              <span v-else class="text-xs text-gray-400" title="Davetlinin hesabı olduğu için uygulama içi davet gönderildi">
                Uygulama içi
              </span>
            </td>
            <td class="px-4 py-3 text-xs text-gray-600">
              <span v-if="inv.firstClickedAt">
                Tıklandı{{ inv.clickCount > 1 ? ` (${inv.clickCount})` : '' }}
              </span>
              <span v-else-if="inv.openedAt">Açıldı</span>
              <span v-else class="text-gray-400">—</span>
            </td>
            <td class="px-4 py-3 text-xs text-gray-600">{{ inv.invitedByName || '-' }}</td>
            <td class="px-4 py-3 text-xs text-gray-500">{{ formatDate(inv.invitedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import OrganizationApi from '../../api/OrganizationApi.js'

const props = defineProps({
  orgId: { type: String, required: true }
})

const invites = ref([])
const loading = ref(false)

async function loadInvites() {
  if (!props.orgId) return
  loading.value = true
  try {
    const res = await OrganizationApi.getInvites(props.orgId)
    invites.value = res.data
  } catch (e) {
    console.error('Davetler yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

function formatInviteStatus(status) {
  const map = {
    PENDING: 'Bekliyor',
    ACCEPTED: 'Kabul edildi',
    DECLINED: 'Reddedildi',
    EXPIRED: 'Süresi doldu',
  }
  return map[status] || status
}

function inviteStatusClass(status) {
  if (status === 'ACCEPTED') return 'bg-green-100 text-green-700'
  if (status === 'DECLINED') return 'bg-red-100 text-red-700'
  if (status === 'EXPIRED') return 'bg-gray-100 text-gray-600'
  return 'bg-amber-100 text-amber-700'
}

// QUEUED: PostForge isteği aldı ama teslim bildirimi henüz gelmedi —
// "gönderilemedi" demek değil, webhook gecikmiş de olabilir.
function formatMailStatus(status) {
  const map = {
    QUEUED: 'Kuyrukta',
    SENT: 'Gönderildi',
    FAILED: 'Başarısız',
  }
  return map[status] || status
}

function mailStatusClass(status) {
  if (status === 'SENT') return 'bg-green-100 text-green-700'
  if (status === 'FAILED') return 'bg-red-100 text-red-700'
  return 'bg-gray-100 text-gray-600'
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' })
}

watch(() => props.orgId, loadInvites)
onMounted(loadInvites)

defineExpose({ reload: loadInvites })
</script>
