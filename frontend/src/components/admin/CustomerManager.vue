<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="font-semibold text-gray-900 dark:text-white">Müşteriler (Organizasyonlar)</h3>
      <input
        v-model="search"
        type="text"
        placeholder="Ara: org, sahip, e-posta..."
        class="text-sm border border-gray-200 dark:border-gray-600 rounded-lg px-3 py-1.5 bg-white dark:bg-gray-700 w-64"
      />
    </div>

    <div v-if="loading" class="text-center py-12 text-gray-500">Yükleniyor...</div>

    <div v-else class="overflow-x-auto rounded-xl border border-gray-200 dark:border-gray-700">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 dark:bg-gray-700/50">
          <tr>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Organizasyon</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Sahip</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Plan</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Durum</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Bitiş</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Üye</th>
            <th class="text-left px-4 py-3 text-gray-500 font-medium">Son Ödeme</th>
            <th class="px-4 py-3"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
          <tr v-if="filtered.length === 0">
            <td colspan="8" class="text-center py-8 text-gray-400">Kayıt bulunamadı.</td>
          </tr>
          <template v-for="org in filtered" :key="org.orgId">
            <tr class="hover:bg-gray-50 dark:hover:bg-gray-700/30">
              <td class="px-4 py-3">
                <p class="font-medium text-gray-900 dark:text-white">{{ org.orgName }}</p>
                <p class="text-xs text-gray-500">{{ org.slug }}</p>
              </td>
              <td class="px-4 py-3">
                <p class="text-gray-900 dark:text-white">{{ org.ownerName }}</p>
                <p class="text-xs text-gray-500">{{ org.ownerEmail }}</p>
              </td>
              <td class="px-4 py-3">
                <span class="px-2 py-1 rounded-full text-xs font-medium" :class="planBadge(org.planCode)">
                  {{ org.planCode }}
                </span>
              </td>
              <td class="px-4 py-3">
                <span class="px-2 py-1 rounded-full text-xs font-medium" :class="statusBadge(org)">
                  {{ statusLabel(org) }}
                </span>
              </td>
              <td class="px-4 py-3 text-xs text-gray-500">
                {{ formatDate(org.trialEndsAt || org.currentPeriodEnd) }}
              </td>
              <td class="px-4 py-3 text-xs text-gray-500">{{ org.memberCount }}</td>
              <td class="px-4 py-3 text-xs text-gray-500">
                <template v-if="org.lastPaymentAt">
                  {{ formatMoney(org.lastPaymentAmount) }}<br/>{{ formatDate(org.lastPaymentAt) }}
                </template>
                <template v-else>—</template>
              </td>
              <td class="px-4 py-3 text-right">
                <button
                  @click="toggleDetail(org)"
                  class="text-indigo-600 hover:text-indigo-800 text-xs font-medium"
                >
                  {{ expandedId === org.orgId ? 'Kapat' : 'Yönet' }}
                </button>
              </td>
            </tr>

            <!-- Detay / aksiyonlar -->
            <tr v-if="expandedId === org.orgId" class="bg-gray-50 dark:bg-gray-700/20">
              <td colspan="8" class="px-6 py-4">
                <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  <!-- Abonelik aksiyonları -->
                  <div class="space-y-3">
                    <p class="text-xs font-semibold text-gray-500 uppercase">Abonelik İşlemleri</p>
                    <div class="flex flex-wrap items-center gap-2">
                      <select v-model="action.planCode" class="input-sm">
                        <option v-for="p in paidPlans" :key="p.code" :value="p.code">{{ p.name }}</option>
                      </select>
                      <select v-model="action.cycle" class="input-sm">
                        <option value="MONTHLY">Aylık</option>
                        <option value="YEARLY">Yıllık</option>
                      </select>
                      <button @click="apply(org, 'ACTIVATE')" class="btn-xs bg-green-600 hover:bg-green-700">
                        Aktive Et
                      </button>
                      <button
                        v-if="org.subscriptionStatus !== 'FREE' && org.subscriptionStatus !== 'TRIAL'"
                        @click="apply(org, 'EXTEND')" class="btn-xs bg-indigo-600 hover:bg-indigo-700">
                        Dönemi Uzat
                      </button>
                    </div>
                    <div class="flex flex-wrap items-center gap-2">
                      <input v-model.number="action.trialDays" type="number" min="1" class="input-sm w-20" />
                      <button @click="apply(org, 'EXTEND_TRIAL')" class="btn-xs bg-amber-500 hover:bg-amber-600">
                        Trial Uzat/Başlat
                      </button>
                      <button
                        v-if="org.subscriptionStatus !== 'FREE'"
                        @click="apply(org, 'CANCEL')" class="btn-xs bg-red-500 hover:bg-red-600">
                        İptal Et
                      </button>
                      <button
                        @click="toggleSuspend(org)"
                        class="btn-xs"
                        :class="org.suspended ? 'bg-green-600 hover:bg-green-700' : 'bg-gray-500 hover:bg-gray-600'">
                        {{ org.suspended ? 'Askıdan Al' : 'Askıya Al' }}
                      </button>
                    </div>
                  </div>

                  <!-- Ödeme linki -->
                  <div class="space-y-3">
                    <p class="text-xs font-semibold text-gray-500 uppercase">Ödeme Linki</p>
                    <p class="text-xs text-gray-500">
                      Seçili plan/dönem için iyzico linki oluşturur ve org sahibine e-posta ile gönderir.
                    </p>
                    <button @click="sendPaymentLink(org)" :disabled="linkSending" class="btn-xs bg-indigo-600 hover:bg-indigo-700">
                      {{ linkSending ? 'Oluşturuluyor...' : 'Ödeme Linki Oluştur ve Gönder' }}
                    </button>
                    <p v-if="lastLink" class="text-xs text-indigo-600 break-all">{{ lastLink }}</p>
                  </div>

                  <!-- Ödeme geçmişi -->
                  <div class="space-y-2">
                    <p class="text-xs font-semibold text-gray-500 uppercase">Ödeme Geçmişi</p>
                    <div v-if="txLoading" class="text-xs text-gray-400">Yükleniyor...</div>
                    <div v-else-if="transactions.length === 0" class="text-xs text-gray-400">Ödeme kaydı yok.</div>
                    <ul v-else class="space-y-1 max-h-40 overflow-y-auto">
                      <li v-for="tx in transactions" :key="tx.id" class="text-xs flex justify-between gap-2">
                        <span class="text-gray-600 dark:text-gray-300">
                          {{ tx.planName }} ({{ tx.billingCycle === 'YEARLY' ? 'yıllık' : 'aylık' }})
                        </span>
                        <span :class="txStatusClass(tx.status)">{{ tx.status }}</span>
                        <span class="text-gray-500">{{ formatMoney(tx.amount) }}</span>
                        <span class="text-gray-400">{{ formatDate(tx.paidAt || tx.createdAt) }}</span>
                      </li>
                    </ul>
                  </div>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import AdminBillingApi from '../../api/AdminBillingApi.js'

const orgs = ref([])
const plans = ref([])
const loading = ref(false)
const search = ref('')
const expandedId = ref(null)
const transactions = ref([])
const txLoading = ref(false)
const linkSending = ref(false)
const lastLink = ref('')

const action = ref({ planCode: 'PRO', cycle: 'MONTHLY', trialDays: 14 })

const paidPlans = computed(() => plans.value.filter(p => !p.isDefault && p.active))

const filtered = computed(() => {
  const q = search.value.toLowerCase().trim()
  if (!q) return orgs.value
  return orgs.value.filter(o =>
    [o.orgName, o.slug, o.ownerName, o.ownerEmail, o.planCode]
      .some(v => v?.toLowerCase().includes(q))
  )
})

async function load() {
  loading.value = true
  try {
    const [orgRes, planRes] = await Promise.all([
      AdminBillingApi.getOrganizations(),
      AdminBillingApi.getPlans(),
    ])
    orgs.value = orgRes.data
    plans.value = planRes.data
    if (paidPlans.value.length && !paidPlans.value.some(p => p.code === action.value.planCode)) {
      action.value.planCode = paidPlans.value[0].code
    }
  } catch (e) {
    console.error('Müşteriler yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

async function toggleDetail(org) {
  if (expandedId.value === org.orgId) {
    expandedId.value = null
    return
  }
  expandedId.value = org.orgId
  lastLink.value = ''
  txLoading.value = true
  try {
    const res = await AdminBillingApi.getTransactions(org.orgId)
    transactions.value = res.data
  } catch {
    transactions.value = []
  } finally {
    txLoading.value = false
  }
}

async function apply(org, actionName) {
  try {
    const res = await AdminBillingApi.applySubscriptionAction(org.orgId, {
      action: actionName,
      planCode: action.value.planCode,
      cycle: action.value.cycle,
      trialDays: action.value.trialDays,
    })
    replaceRow(res.data)
    createToast(`İşlem uygulandı: ${actionName}`, { type: 'success', position: 'top-center' })
  } catch (e) {
    console.error('Aksiyon uygulanamadı:', e)
  }
}

async function toggleSuspend(org) {
  try {
    const res = await AdminBillingApi.setSuspended(org.orgId, !org.suspended)
    replaceRow(res.data)
  } catch (e) {
    console.error('Askıya alma değiştirilemedi:', e)
  }
}

async function sendPaymentLink(org) {
  linkSending.value = true
  try {
    const res = await AdminBillingApi.createPaymentLink(org.orgId, {
      planCode: action.value.planCode,
      cycle: action.value.cycle,
    })
    lastLink.value = res.data.paymentUrl
    createToast('Ödeme linki oluşturuldu ve e-posta ile gönderildi.', { type: 'success', position: 'top-center' })
    transactions.value = [res.data, ...transactions.value]
  } catch (e) {
    console.error('Ödeme linki oluşturulamadı:', e)
  } finally {
    linkSending.value = false
  }
}

function replaceRow(row) {
  const idx = orgs.value.findIndex(o => o.orgId === row.orgId)
  if (idx !== -1) orgs.value[idx] = row
}

function planBadge(code) {
  if (code === 'MAX') return 'bg-purple-100 text-purple-700'
  if (code === 'PRO') return 'bg-blue-100 text-blue-700'
  return 'bg-gray-100 text-gray-600'
}

function statusLabel(org) {
  if (org.suspended) return 'ASKIDA'
  const map = { TRIAL: 'Deneme', ACTIVE: 'Aktif', PAST_DUE: 'Ödeme Bekliyor', FREE: 'Free' }
  return map[org.subscriptionStatus] || org.subscriptionStatus
}

function statusBadge(org) {
  if (org.suspended) return 'bg-red-100 text-red-700'
  const map = {
    TRIAL: 'bg-amber-100 text-amber-700',
    ACTIVE: 'bg-green-100 text-green-700',
    PAST_DUE: 'bg-red-100 text-red-700',
    FREE: 'bg-gray-100 text-gray-600',
  }
  return map[org.subscriptionStatus] || 'bg-gray-100 text-gray-600'
}

function txStatusClass(status) {
  if (status === 'PAID') return 'text-green-600 font-medium'
  if (status === 'PENDING') return 'text-amber-600'
  return 'text-gray-400'
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('tr-TR')
}

function formatMoney(amount) {
  if (amount == null) return '—'
  return Number(amount).toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })
}

onMounted(load)
</script>

<style scoped>
.input-sm {
  @apply text-xs border border-gray-200 dark:border-gray-600 rounded px-2 py-1.5 bg-white dark:bg-gray-700;
}
.btn-xs {
  @apply px-3 py-1.5 text-white text-xs rounded-lg transition-colors disabled:opacity-50;
}
</style>
