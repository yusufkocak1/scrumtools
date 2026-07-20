<template>
  <div class="bg-white rounded-xl shadow p-6 space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold text-gray-900">Organizasyon Ayarları</h2>
      <span
        class="text-xs px-2 py-1 rounded-full"
        :class="planBadgeClass"
      >{{ planName }}</span>
    </div>

    <!-- Aktif organizasyon seçimi — birden fazla organizasyona üye olan kullanıcı
         switcher'a gitmeden buradan da geçiş yapabilir -->
    <div v-if="canSwitch" class="rounded-lg border border-gray-200 p-4 space-y-3">
      <div>
        <p class="text-sm font-medium text-gray-700">Aktif Organizasyon</p>
        <p class="text-xs text-gray-500 mt-0.5">
          Aşağıdaki ayarlar seçili organizasyona uygulanır. Seçim tarayıcınızda saklanır.
        </p>
      </div>

      <div class="space-y-1.5">
        <button
          v-for="o in organizations"
          :key="o.id"
          type="button"
          @click="selectOrg(o)"
          class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg border text-left transition-colors"
          :class="o.id === activeOrgId
            ? 'border-indigo-300 bg-indigo-50'
            : 'border-gray-200 hover:bg-gray-50'"
        >
          <div class="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white text-sm font-bold flex-shrink-0">
            {{ o.name?.charAt(0)?.toUpperCase() || '?' }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-gray-900 truncate">{{ o.name }}</p>
            <p class="text-xs text-gray-400 truncate">{{ o.slug }}</p>
          </div>
          <span
            v-if="o.id === activeOrgId"
            class="text-xs font-medium text-indigo-600 flex-shrink-0"
          >Aktif</span>
        </button>
      </div>
    </div>

    <!-- Paket durumu + kullanım göstergeleri -->
    <div v-if="entitlements" class="rounded-lg border border-gray-200 p-4 space-y-3">
      <div class="flex items-center justify-between">
        <p class="text-sm font-medium text-gray-700">Paket Kullanımı</p>
        <p class="text-xs" :class="statusTextClass">{{ statusText }}</p>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <!-- Üyeler -->
        <div>
          <div class="flex justify-between text-xs text-gray-500 mb-1">
            <span>Üyeler</span>
            <span>{{ usage('members') }} / {{ limit('members') ?? '∞' }}</span>
          </div>
          <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full rounded-full transition-all"
              :class="isAtLimit('members') ? 'bg-red-500' : 'bg-indigo-500'"
              :style="{ width: usagePercent('members') + '%' }"
            ></div>
          </div>
        </div>
        <!-- Projeler -->
        <div>
          <div class="flex justify-between text-xs text-gray-500 mb-1">
            <span>Projeler</span>
            <span>{{ usage('projects') }} / {{ limit('projects') ?? '∞' }}</span>
          </div>
          <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full rounded-full transition-all"
              :class="isAtLimit('projects') ? 'bg-red-500' : 'bg-indigo-500'"
              :style="{ width: usagePercent('projects') + '%' }"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <form @submit.prevent="save" class="space-y-4">
      <!-- Logo -->
      <div class="flex items-center gap-4">
        <div class="w-16 h-16 rounded-xl bg-indigo-100 flex items-center justify-center text-2xl font-bold text-indigo-600">
          {{ form.name?.charAt(0)?.toUpperCase() || '?' }}
        </div>
        <div>
          <p class="text-sm font-medium text-gray-700">Logo URL</p>
          <input
            v-model="form.logoUrl"
            type="url"
            placeholder="https://example.com/logo.png"
            class="mt-1 input-field"
          />
        </div>
      </div>

      <!-- Ad -->
      <div>
        <label class="label">Organizasyon Adı</label>
        <input v-model="form.name" type="text" required class="input-field" />
      </div>

      <!-- Açıklama -->
      <div>
        <label class="label">Açıklama</label>
        <textarea v-model="form.description" rows="3" class="input-field"></textarea>
      </div>

      <!-- Slug (read-only) -->
      <div>
        <label class="label">Slug (URL)</label>
        <input :value="org?.slug" disabled class="input-field opacity-60 cursor-not-allowed" />
        <p class="text-xs text-gray-500 mt-1">Slug değiştirilemez.</p>
      </div>

      <div class="flex justify-end">
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? 'Kaydediliyor...' : 'Kaydet' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import OrganizationApi from '../../api/OrganizationApi.js'
import { useEntitlements } from '../../composables/useEntitlements.js'
import { useOrganizationContext } from '../../composables/useOrganizationContext.js'

const props = defineProps({
  org: { type: Object, required: true }
})

const emit = defineEmits(['updated'])

const { organizations, activeOrgId, canSwitch, loadOrganizations, selectOrg } =
  useOrganizationContext()

// Zaten yüklüyse istek atılmaz — composable tekrarlı çağrıları tekilleştirir
onMounted(() => loadOrganizations())

const orgId = computed(() => props.org?.id)
const {
  entitlements, planName, status, isTrial, isPastDue, trialDaysLeft,
  usage, limit, isAtLimit, refresh,
} = useEntitlements(orgId)

watch(orgId, (id) => { if (id) refresh() }, { immediate: true })

const statusText = computed(() => {
  if (isTrial.value) return `Deneme sürümü — ${trialDaysLeft.value} gün kaldı`
  if (isPastDue.value) return 'Ödeme bekleniyor (ek süre)'
  if (status.value === 'ACTIVE') return 'Aktif abonelik'
  return 'Ücretsiz paket'
})

const statusTextClass = computed(() => {
  if (isPastDue.value) return 'text-red-600 font-medium'
  if (isTrial.value) return 'text-amber-600 font-medium'
  return 'text-gray-500'
})

function usagePercent(name) {
  const l = limit(name)
  if (l == null || l === 0) return 0
  return Math.min(Math.round((usage(name) / l) * 100), 100)
}

const saving = ref(false)
const form = ref({
  name: '',
  slug: '',
  description: '',
  logoUrl: '',
})

watch(() => props.org, (val) => {
  if (val) {
    form.value = {
      name: val.name || '',
      slug: val.slug || '',
      description: val.description || '',
      logoUrl: val.logoUrl || '',
    }
  }
}, { immediate: true })

const planBadgeClass = computed(() => {
  const plan = entitlements.value?.planCode || props.org?.plan
  if (plan === 'MAX' || plan === 'ENTERPRISE') return 'bg-purple-100 text-purple-700'
  if (plan === 'PRO') return 'bg-blue-100 text-blue-700'
  return 'bg-gray-100 text-gray-600'
})

async function save() {
  saving.value = true
  try {
    const res = await OrganizationApi.update(props.org.id, form.value)
    emit('updated', res.data)
  } catch (e) {
    console.error('Kayıt hatası:', e)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.input-field {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500;
}
.label {
  @apply block text-sm font-medium text-gray-700 mb-1;
}
.btn-primary {
  @apply px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors;
}
</style>

