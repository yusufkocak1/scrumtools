<template>
  <div class="space-y-6">
    <!-- Mevcut plan kartı -->
    <div class="rounded-xl border border-gray-200 p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div class="flex items-center gap-2">
            <h2 class="text-lg font-semibold text-gray-900">Mevcut Paket:</h2>
            <span class="px-2.5 py-1 rounded-full text-sm font-medium" :class="planBadgeClass">
              {{ planName }}
            </span>
          </div>
          <p class="text-sm mt-1" :class="statusTextClass">{{ statusText }}</p>
        </div>
        <div class="grid grid-cols-2 gap-6 text-sm">
          <div>
            <p class="text-gray-500 text-xs">Üyeler</p>
            <p class="font-semibold text-gray-900">{{ usage('members') }} / {{ limit('members') ?? '∞' }}</p>
          </div>
          <div>
            <p class="text-gray-500 text-xs">Projeler</p>
            <p class="font-semibold text-gray-900">{{ usage('projects') }} / {{ limit('projects') ?? '∞' }}</p>
          </div>
        </div>
      </div>

      <!-- Bekleyen ödeme -->
      <div
        v-if="pendingTx"
        class="mt-4 rounded-lg bg-amber-50 border border-amber-200 px-4 py-3 flex flex-wrap items-center justify-between gap-2"
      >
        <p class="text-sm text-amber-800">
          <strong>Ödemeniz bekleniyor…</strong>
          Ödemenizi tamamladıysanız paketiniz birkaç dakika içinde otomatik aktive edilir.
        </p>
        <a
          v-if="pendingTx.paymentUrl"
          :href="pendingTx.paymentUrl"
          target="_blank"
          class="text-sm font-medium text-amber-700 underline"
        >Ödeme sayfasını aç</a>
      </div>
    </div>

    <!-- Plan kartları -->
    <div>
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-semibold text-gray-900">Paketler</h3>
        <div class="flex bg-gray-100 rounded-lg p-1 text-sm">
          <button
            @click="cycle = 'MONTHLY'"
            :class="['px-4 py-1.5 rounded-md transition-all', cycle === 'MONTHLY' ? 'bg-white shadow text-indigo-600 font-medium' : 'text-gray-600']"
          >Aylık</button>
          <button
            @click="cycle = 'YEARLY'"
            :class="['px-4 py-1.5 rounded-md transition-all', cycle === 'YEARLY' ? 'bg-white shadow text-indigo-600 font-medium' : 'text-gray-600']"
          >Yıllık</button>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div
          v-for="plan in plans"
          :key="plan.code"
          class="rounded-xl border p-5 flex flex-col"
          :class="plan.code === planCode ? 'border-indigo-500 ring-1 ring-indigo-500' : 'border-gray-200'"
        >
          <div class="flex items-center justify-between">
            <h4 class="font-semibold text-gray-900">{{ plan.name }}</h4>
            <span v-if="plan.code === planCode" class="text-xs text-indigo-600 font-medium">Mevcut</span>
          </div>
          <p class="text-xs text-gray-500 mt-1 min-h-[2rem]">{{ plan.description }}</p>

          <p class="mt-3">
            <span class="text-2xl font-bold text-gray-900">{{ priceFor(plan) }}</span>
            <span class="text-xs text-gray-500"> /{{ cycle === 'YEARLY' ? 'yıl' : 'ay' }} (KDV dahil)</span>
          </p>
          <p v-if="cycle === 'YEARLY' && yearlySavings(plan)" class="text-xs text-green-600 mt-0.5">
            Yıllıkta %{{ yearlySavings(plan) }} avantaj
          </p>

          <ul class="mt-4 space-y-1.5 text-sm text-gray-600 flex-1">
            <li>Üye: <strong>{{ plan.maxMembers ?? 'Sınırsız' }}</strong></li>
            <li>Proje: <strong>{{ plan.maxProjects ?? 'Sınırsız' }}</strong></li>
            <li v-for="f in plan.features" :key="f" class="flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5 text-green-500 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
              </svg>
              {{ featureLabel(f) }}
            </li>
          </ul>

          <button
            v-if="!plan.isDefault"
            @click="buy(plan)"
            :disabled="buying || (plan.code === planCode && status === 'ACTIVE')"
            class="mt-4 w-full py-2.5 rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
            :class="plan.code === planCode
              ? 'bg-indigo-50 text-indigo-600 hover:bg-indigo-100'
              : 'bg-indigo-600 text-white hover:bg-indigo-700'"
          >
            {{ buyLabel(plan) }}
          </button>
          <div v-else class="mt-4 py-2.5 text-center text-sm text-gray-400">Ücretsiz</div>
        </div>
      </div>
    </div>

    <!-- Ödeme geçmişi -->
    <div v-if="transactions.length">
      <h3 class="font-semibold text-gray-900 mb-3">Ödeme Geçmişi</h3>
      <div class="overflow-x-auto rounded-xl border border-gray-200">
        <table class="w-full text-sm">
          <thead class="bg-gray-50">
            <tr>
              <th class="text-left px-4 py-2.5 text-gray-500 font-medium">Paket</th>
              <th class="text-left px-4 py-2.5 text-gray-500 font-medium">Dönem</th>
              <th class="text-left px-4 py-2.5 text-gray-500 font-medium">Tutar</th>
              <th class="text-left px-4 py-2.5 text-gray-500 font-medium">Durum</th>
              <th class="text-left px-4 py-2.5 text-gray-500 font-medium">Tarih</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="tx in transactions" :key="tx.id">
              <td class="px-4 py-2.5 text-gray-900">{{ tx.planName }}</td>
              <td class="px-4 py-2.5 text-gray-500">{{ tx.billingCycle === 'YEARLY' ? 'Yıllık' : 'Aylık' }}</td>
              <td class="px-4 py-2.5 text-gray-900">{{ formatMoney(tx.amount) }}</td>
              <td class="px-4 py-2.5">
                <span class="px-2 py-0.5 rounded-full text-xs font-medium" :class="txBadge(tx.status)">
                  {{ txLabel(tx.status) }}
                </span>
              </td>
              <td class="px-4 py-2.5 text-gray-500 text-xs">{{ formatDate(tx.paidAt || tx.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import BillingApi from '../../api/BillingApi.js'
import { useEntitlements } from '../../composables/useEntitlements.js'

const FEATURE_LABELS = {
  SCRUM_POKER: 'Scrum Poker',
  RETRO: 'Retrospektif',
  WORK_BOARD: 'İş Panosu',
  CODE_SHARE: 'Kod Paylaşımı',
  QUIZ: 'GameBox',
  DOCS: 'Dokümanlar',
  DASHBOARD_REPORTS: 'Raporlar',
  ATTACHMENTS: 'Dosya Ekleri',
  CUSTOM_ROLES: 'Özel Roller',
}

const props = defineProps({
  org: { type: Object, required: true },
})

const orgId = computed(() => props.org?.id)
const {
  planCode, planName, status, isTrial, isPastDue, trialDaysLeft, periodEnd,
  usage, limit, refresh,
} = useEntitlements(orgId)

const plans = ref([])
const transactions = ref([])
const cycle = ref('MONTHLY')
const buying = ref(false)
let pollTimer = null

const pendingTx = computed(() => transactions.value.find(t => t.status === 'PENDING'))

const statusText = computed(() => {
  if (isTrial.value) return `Deneme sürümü — ${trialDaysLeft.value} gün kaldı`
  if (isPastDue.value) return 'Dönem sona erdi — ödeme bekleniyor (ek süre içindesiniz)'
  if (status.value === 'ACTIVE') {
    return periodEnd.value
      ? `Aktif — ${periodEnd.value.toLocaleDateString('tr-TR')} tarihine kadar`
      : 'Aktif abonelik'
  }
  return 'Ücretsiz paket — dilediğiniz zaman yükseltebilirsiniz'
})

const statusTextClass = computed(() => {
  if (isPastDue.value) return 'text-red-600'
  if (isTrial.value) return 'text-amber-600'
  return 'text-gray-500'
})

const planBadgeClass = computed(() => {
  if (planCode.value === 'MAX') return 'bg-purple-100 text-purple-700'
  if (planCode.value === 'PRO') return 'bg-blue-100 text-blue-700'
  return 'bg-gray-100 text-gray-600'
})

function priceFor(plan) {
  const p = cycle.value === 'YEARLY' ? plan.yearlyPriceTry : plan.monthlyPriceTry
  return formatMoney(p)
}

function yearlySavings(plan) {
  if (!plan.monthlyPriceTry || !plan.yearlyPriceTry) return 0
  const monthlyTotal = plan.monthlyPriceTry * 12
  if (monthlyTotal <= plan.yearlyPriceTry) return 0
  return Math.round((1 - plan.yearlyPriceTry / monthlyTotal) * 100)
}

function buyLabel(plan) {
  if (plan.code === planCode.value && status.value === 'ACTIVE') return 'Aktif'
  if (plan.code === planCode.value) return 'Satın Al'
  return 'Yükselt'
}

async function buy(plan) {
  buying.value = true
  try {
    const res = await BillingApi.checkout(orgId.value, { planCode: plan.code, cycle: cycle.value })
    window.open(res.data.paymentUrl, '_blank')
    createToast('Ödeme sayfası açıldı. Ödemeniz onaylanınca paketiniz otomatik aktive edilir.', {
      type: 'info', position: 'top-center', timeout: 8000,
    })
    await loadTransactions()
    startPolling()
  } catch (e) {
    console.error('Satın alma başlatılamadı:', e)
  } finally {
    buying.value = false
  }
}

async function loadPlans() {
  try {
    const res = await BillingApi.getPlans()
    plans.value = res.data
  } catch (e) {
    console.error('Planlar yüklenemedi:', e)
  }
}

async function loadTransactions() {
  if (!orgId.value) return
  try {
    const res = await BillingApi.getTransactions(orgId.value)
    transactions.value = res.data
  } catch {
    transactions.value = []
  }
}

// PENDING ödeme varken 10 sn'de bir durum kontrolü; PAID olunca entitlements yenilenir
function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    const hadPending = !!pendingTx.value
    await loadTransactions()
    if (hadPending && !pendingTx.value) {
      stopPolling()
      await refresh()
      createToast('Ödemeniz alındı — paketiniz aktive edildi! 🎉', { type: 'success', position: 'top-center' })
    }
    if (!pendingTx.value) stopPolling()
  }, 10_000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function featureLabel(f) {
  return FEATURE_LABELS[f] || f
}

function txLabel(status) {
  const map = { PAID: 'Ödendi', PENDING: 'Bekliyor', FAILED: 'Başarısız', EXPIRED: 'Süresi Doldu', REFUNDED: 'İade' }
  return map[status] || status
}

function txBadge(status) {
  const map = {
    PAID: 'bg-green-100 text-green-700',
    PENDING: 'bg-amber-100 text-amber-700',
    FAILED: 'bg-red-100 text-red-700',
    EXPIRED: 'bg-gray-100 text-gray-500',
    REFUNDED: 'bg-blue-100 text-blue-700',
  }
  return map[status] || 'bg-gray-100 text-gray-500'
}

function formatMoney(amount) {
  if (amount == null) return '—'
  return Number(amount).toLocaleString('tr-TR', { style: 'currency', currency: 'TRY', maximumFractionDigits: 0 })
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('tr-TR')
}

watch(orgId, async (id) => {
  if (id) {
    await Promise.all([refresh(), loadTransactions()])
    if (pendingTx.value) startPolling()
  }
}, { immediate: true })

onMounted(loadPlans)
onBeforeUnmount(stopPolling)
</script>
