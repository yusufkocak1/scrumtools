<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="font-semibold text-gray-900 dark:text-white">Paketler</h3>
      <button @click="startCreate" class="btn-primary text-sm">Yeni Paket</button>
    </div>

    <div v-if="loading" class="text-center py-12 text-gray-500">Yükleniyor...</div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <div
        v-for="plan in plans"
        :key="plan.id"
        class="rounded-xl border p-4 space-y-2"
        :class="plan.active ? 'border-gray-200 dark:border-gray-700' : 'border-dashed border-gray-300 opacity-60'"
      >
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="font-semibold text-gray-900 dark:text-white">{{ plan.name }}</span>
            <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600">{{ plan.code }}</span>
            <span v-if="plan.isDefault" class="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700">Varsayılan</span>
            <span v-if="!plan.isPublic" class="text-xs px-2 py-0.5 rounded-full bg-gray-200 text-gray-500">Gizli</span>
          </div>
          <button @click="startEdit(plan)" class="text-indigo-600 hover:text-indigo-800 text-xs font-medium">Düzenle</button>
        </div>
        <p class="text-xs text-gray-500">{{ plan.description }}</p>
        <div class="text-xs text-gray-600 dark:text-gray-300 space-y-1">
          <p>Üye: <strong>{{ plan.maxMembers ?? '∞' }}</strong> · Proje: <strong>{{ plan.maxProjects ?? '∞' }}</strong></p>
          <p>Aylık: <strong>{{ formatMoney(plan.monthlyPriceTry) }}</strong> · Yıllık: <strong>{{ formatMoney(plan.yearlyPriceTry) }}</strong></p>
          <p v-if="plan.trialDays">Trial: <strong>{{ plan.trialDays }} gün</strong></p>
        </div>
        <div class="flex flex-wrap gap-1 pt-1">
          <span
            v-for="f in plan.features"
            :key="f"
            class="text-[10px] px-1.5 py-0.5 rounded bg-indigo-50 text-indigo-600"
          >{{ featureLabel(f) }}</span>
        </div>
      </div>
    </div>

    <!-- Düzenleme / oluşturma modalı -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white dark:bg-gray-800 rounded-xl p-6 w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
        <h4 class="text-lg font-semibold mb-4 text-gray-900 dark:text-white">
          {{ editing ? 'Paketi Düzenle' : 'Yeni Paket' }}
        </h4>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="label">Kod</label>
            <input v-model="form.code" :disabled="editing" class="input-field" placeholder="PRO" />
          </div>
          <div>
            <label class="label">Ad</label>
            <input v-model="form.name" class="input-field" placeholder="Pro" />
          </div>
          <div class="col-span-2">
            <label class="label">Açıklama</label>
            <input v-model="form.description" class="input-field" />
          </div>
          <div>
            <label class="label">Üye Limiti (boş = ∞)</label>
            <input v-model.number="form.maxMembers" type="number" min="1" class="input-field" />
          </div>
          <div>
            <label class="label">Proje Limiti (boş = ∞)</label>
            <input v-model.number="form.maxProjects" type="number" min="1" class="input-field" />
          </div>
          <div>
            <label class="label">Aylık Fiyat (₺, KDV dahil)</label>
            <input v-model.number="form.monthlyPriceTry" type="number" min="0" step="0.01" class="input-field" />
          </div>
          <div>
            <label class="label">Yıllık Fiyat (₺, KDV dahil)</label>
            <input v-model.number="form.yearlyPriceTry" type="number" min="0" step="0.01" class="input-field" />
          </div>
          <div>
            <label class="label">Trial Gün (boş = yok)</label>
            <input v-model.number="form.trialDays" type="number" min="1" class="input-field" />
          </div>
          <div class="flex items-end gap-4 pb-1">
            <label class="flex items-center gap-1 text-sm text-gray-700 dark:text-gray-300">
              <input v-model="form.isPublic" type="checkbox" /> Görünür
            </label>
            <label class="flex items-center gap-1 text-sm text-gray-700 dark:text-gray-300">
              <input v-model="form.active" type="checkbox" /> Aktif
            </label>
          </div>
          <div class="col-span-2">
            <label class="label">Özellikler</label>
            <div class="grid grid-cols-2 gap-1">
              <label
                v-for="f in allFeatures"
                :key="f"
                class="flex items-center gap-1.5 text-xs text-gray-700 dark:text-gray-300"
              >
                <input type="checkbox" :value="f" v-model="form.features" />
                {{ featureLabel(f) }}
              </label>
            </div>
          </div>
        </div>
        <div class="flex gap-2 mt-5 justify-end">
          <button @click="showModal = false" class="btn-secondary">İptal</button>
          <button @click="save" :disabled="saving || !form.code || !form.name" class="btn-primary">
            {{ saving ? 'Kaydediliyor...' : 'Kaydet' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { createToast } from 'mosha-vue-toastify'
import AdminBillingApi from '../../api/AdminBillingApi.js'

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

const allFeatures = Object.keys(FEATURE_LABELS)

const plans = ref([])
const loading = ref(false)
const showModal = ref(false)
const saving = ref(false)
const editing = ref(null) // düzenlenen planın id'si

const emptyForm = () => ({
  code: '', name: '', description: '',
  maxMembers: null, maxProjects: null,
  monthlyPriceTry: 0, yearlyPriceTry: 0,
  trialDays: null, isPublic: true, active: true,
  sortOrder: 99, features: [],
})
const form = ref(emptyForm())

async function load() {
  loading.value = true
  try {
    const res = await AdminBillingApi.getPlans()
    plans.value = res.data
  } catch (e) {
    console.error('Planlar yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

function startCreate() {
  editing.value = null
  form.value = emptyForm()
  showModal.value = true
}

function startEdit(plan) {
  editing.value = plan.id
  form.value = {
    code: plan.code,
    name: plan.name,
    description: plan.description || '',
    maxMembers: plan.maxMembers,
    maxProjects: plan.maxProjects,
    monthlyPriceTry: plan.monthlyPriceTry,
    yearlyPriceTry: plan.yearlyPriceTry,
    trialDays: plan.trialDays,
    isPublic: plan.isPublic,
    active: plan.active,
    sortOrder: plan.sortOrder,
    features: [...(plan.features || [])],
  }
  showModal.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = {
      ...form.value,
      maxMembers: form.value.maxMembers || null,
      maxProjects: form.value.maxProjects || null,
      trialDays: form.value.trialDays || null,
    }
    if (editing.value) {
      await AdminBillingApi.updatePlan(editing.value, payload)
    } else {
      await AdminBillingApi.createPlan(payload)
    }
    createToast('Paket kaydedildi.', { type: 'success', position: 'top-center' })
    showModal.value = false
    await load()
  } catch (e) {
    console.error('Paket kaydedilemedi:', e)
  } finally {
    saving.value = false
  }
}

function featureLabel(f) {
  return FEATURE_LABELS[f] || f
}

function formatMoney(amount) {
  if (amount == null) return '—'
  return Number(amount).toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })
}

onMounted(load)
</script>

<style scoped>
.input-field {
  @apply w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500;
}
.label { @apply block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1; }
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors; }
.btn-secondary { @apply px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors; }
</style>
