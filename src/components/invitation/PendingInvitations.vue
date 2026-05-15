<template>
  <div>
    <div v-if="loading" class="text-center py-8 text-gray-500">Yükleniyor...</div>

    <div v-else-if="invitations.length === 0" class="text-center py-8 text-gray-400">
      <svg class="w-12 h-12 mx-auto mb-3 opacity-30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
      </svg>
      Bekleyen davetiniz yok.
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="inv in invitations"
        :key="inv.id"
        class="bg-white rounded-xl border border-gray-200 p-4 flex items-start justify-between gap-4"
      >
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span class="text-xs px-2 py-0.5 rounded-full" :class="typeBadgeClass(inv.type)">
              {{ formatType(inv.type) }}
            </span>
            <span v-if="inv.roleName" class="text-xs text-gray-500">· {{ inv.roleName }}</span>
          </div>
          <p class="text-sm text-gray-900 font-medium">
            <span class="text-indigo-600">{{ inv.invitedByName }}</span> sizi davet etti
          </p>
          <p class="text-xs text-gray-500 mt-1">
            Son geçerlilik: {{ formatDate(inv.expiresAt) }}
          </p>
        </div>
        <div class="flex items-center gap-2 flex-shrink-0">
          <button
            @click="decline(inv)"
            :disabled="processingId === inv.id"
            class="px-3 py-1.5 text-xs border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
          >
            Reddet
          </button>
          <button
            @click="accept(inv)"
            :disabled="processingId === inv.id"
            class="px-3 py-1.5 text-xs bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors"
          >
            {{ processingId === inv.id ? '...' : 'Kabul Et' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import InvitationApi from '../../api/InvitationApi.js'

const invitations = ref([])
const loading = ref(false)
const processingId = ref(null)

async function loadInvitations() {
  loading.value = true
  try {
    const res = await InvitationApi.getPending()
    invitations.value = res.data
  } catch (e) {
    console.error('Davetler yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

async function accept(inv) {
  processingId.value = inv.id
  try {
    await InvitationApi.accept(inv.token)
    invitations.value = invitations.value.filter(i => i.id !== inv.id)
  } catch (e) {
    console.error('Kabul edilemedi:', e)
  } finally {
    processingId.value = null
  }
}

async function decline(inv) {
  processingId.value = inv.id
  try {
    await InvitationApi.decline(inv.token)
    invitations.value = invitations.value.filter(i => i.id !== inv.id)
  } catch (e) {
    console.error('Reddedilemedi:', e)
  } finally {
    processingId.value = null
  }
}

function formatType(type) {
  const map = { ORGANIZATION: 'Organizasyon', PROJECT: 'Proje', TEAM: 'Takım' }
  return map[type] || type
}

function typeBadgeClass(type) {
  if (type === 'ORGANIZATION') return 'bg-purple-100 text-purple-700'
  if (type === 'PROJECT') return 'bg-blue-100 text-blue-700'
  return 'bg-green-100 text-green-700'
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' })
}

onMounted(loadInvitations)
</script>

