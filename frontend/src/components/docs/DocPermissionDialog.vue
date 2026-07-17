<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[80vh] flex flex-col">
      <!-- Header -->
      <div class="px-5 py-4 border-b border-slate-100 flex items-center gap-3">
        <div class="w-9 h-9 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round"
                  d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z"/>
          </svg>
        </div>
        <h2 class="font-semibold text-slate-800">Yetki Yönetimi</h2>
        <button @click="$emit('close')" class="ml-auto p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <!-- Tabs -->
      <div class="mx-5 mt-4 bg-slate-100 rounded-xl p-1 flex gap-1">
        <button @click="tab = 'list'"
                :class="['flex-1 py-1.5 text-sm rounded-lg transition', tab === 'list' ? 'bg-white shadow-sm text-indigo-600 font-medium' : 'text-slate-500 hover:text-slate-700']">
          Mevcut Yetkiler
        </button>
        <button @click="tab = 'add'"
                :class="['flex-1 py-1.5 text-sm rounded-lg transition', tab === 'add' ? 'bg-white shadow-sm text-indigo-600 font-medium' : 'text-slate-500 hover:text-slate-700']">
          Yetki Ekle
        </button>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto p-5">
        <!-- LIST TAB -->
        <div v-if="tab === 'list'">
          <div v-if="permissions.length === 0" class="text-center py-10 text-slate-400 text-sm">
            Henüz tanımlı yetki yok
          </div>
          <div v-else class="space-y-2">
            <div v-for="perm in permissions" :key="perm.id"
                 class="flex items-center justify-between gap-3 border border-slate-200 rounded-xl px-4 py-3 hover:border-slate-300 transition">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-1.5">
                  <span class="text-sm font-medium text-slate-800">{{ perm.targetName }}</span>
                  <span class="text-xs bg-blue-50 text-blue-600 px-2 py-0.5 rounded-full">{{ targetTypeLabels[perm.targetType] }}</span>
                  <span :class="accessBadgeClass(perm.accessLevel)">{{ perm.accessLevel }}</span>
                  <span v-if="perm.canDelegate" class="text-xs bg-amber-50 text-amber-600 px-2 py-0.5 rounded-full">Devredilebilir</span>
                </div>
                <p class="text-xs text-slate-400 mt-1">{{ perm.grantedByName }} tarafından verildi</p>
              </div>
              <button @click="revokeTarget = perm"
                      class="shrink-0 text-sm font-medium text-rose-500 hover:text-rose-600 hover:bg-rose-50 px-2.5 py-1.5 rounded-lg transition">
                Kaldır
              </button>
            </div>
          </div>
        </div>

        <!-- ADD TAB -->
        <div v-if="tab === 'add'" class="space-y-4">
          <!-- Scope: Space veya Page -->
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1.5">Kapsam</label>
            <div class="flex gap-3">
              <label class="flex items-center gap-2 text-sm text-slate-600 cursor-pointer">
                <input type="radio" v-model="form.scope" value="space" class="text-indigo-600"/> Space (tamamı)
              </label>
              <label v-if="pageId" class="flex items-center gap-2 text-sm text-slate-600 cursor-pointer">
                <input type="radio" v-model="form.scope" value="page" class="text-indigo-600"/> Sadece bu sayfa
              </label>
            </div>
          </div>

          <!-- Target Type -->
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1.5">Yetki hedefi</label>
            <select v-model="form.targetType"
                    class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition">
              <option value="USER">Kullanıcı</option>
              <option value="TEAM">Takım</option>
              <option value="ORGANIZATION">Organizasyon</option>
              <option value="PROJECT_MEMBERS">Proje Üyeleri</option>
            </select>
          </div>

          <!-- Target picker — arama ile seçim -->
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1.5">Hedef</label>

            <!-- Seçili hedef -->
            <div v-if="selectedTarget"
                 class="flex items-center justify-between border border-indigo-200 bg-indigo-50 rounded-xl px-3 py-2">
              <div class="min-w-0">
                <span class="text-sm font-medium text-slate-800">{{ selectedTarget.name }}</span>
                <span v-if="selectedTarget.detail" class="text-xs text-slate-500 ml-2">{{ selectedTarget.detail }}</span>
              </div>
              <button @click="clearTarget" class="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-indigo-100 ml-2 shrink-0 transition">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>
            </div>

            <!-- Arama kutusu + sonuçlar -->
            <div v-else class="relative">
              <input v-model="targetSearch" type="text"
                     :placeholder="searchPlaceholder"
                     @focus="onSearchFocus"
                     @blur="showResults = false"
                     class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition placeholder:text-slate-400"/>
              <div v-if="showResults"
                   class="absolute z-10 mt-1 w-full bg-white border border-slate-200 rounded-xl shadow-lg shadow-slate-200/60 max-h-56 overflow-y-auto py-1">
                <div v-if="searching" class="px-3 py-2 text-sm text-slate-400">Aranıyor...</div>
                <div v-else-if="targetResults.length === 0" class="px-3 py-2 text-sm text-slate-400">Sonuç bulunamadı</div>
                <button v-else v-for="result in targetResults" :key="result.id"
                        @mousedown.prevent="selectTarget(result)"
                        class="w-full text-left px-3 py-2 hover:bg-indigo-50 flex items-center justify-between gap-2 transition">
                  <span class="text-sm text-slate-800 truncate">{{ result.name }}</span>
                  <span v-if="result.detail" class="text-xs text-slate-400 truncate">{{ result.detail }}</span>
                </button>
              </div>
            </div>
          </div>

          <!-- Access Level -->
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1.5">Erişim seviyesi</label>
            <select v-model="form.accessLevel"
                    class="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-100 focus:border-indigo-300 transition">
              <option value="READ">Okuma (READ)</option>
              <option value="WRITE">Yazma (WRITE)</option>
              <option value="ADMIN">Yönetim (ADMIN)</option>
            </select>
          </div>

          <!-- Can Delegate -->
          <div class="flex items-center gap-2">
            <input type="checkbox" v-model="form.canDelegate" id="canDelegate"
                   class="rounded border-slate-300 text-indigo-600"/>
            <label for="canDelegate" class="text-sm text-slate-600 cursor-pointer">Yetki devri yapabilsin</label>
          </div>

          <!-- Delegate mode -->
          <div class="flex items-center gap-2">
            <input type="checkbox" v-model="form.useDelegate" id="useDelegate"
                   class="rounded border-slate-300 text-indigo-600"/>
            <label for="useDelegate" class="text-sm text-slate-600 cursor-pointer">Devretme modunda ekle (kendi yetkim üzerinden)</label>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div v-if="tab === 'add'" class="px-5 py-4 bg-slate-50 rounded-b-2xl flex justify-end">
        <button @click="grant" :disabled="!isFormValid"
                class="bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-300 disabled:cursor-not-allowed text-white px-5 py-2 rounded-xl text-sm font-medium shadow-sm shadow-indigo-200 disabled:shadow-none transition">
          Yetki Ver
        </button>
      </div>
    </div>

    <ConfirmDialog
        v-if="revokeTarget"
        title="Yetkiyi kaldır"
        :message="`${revokeTarget.targetName} için tanımlı ${revokeTarget.accessLevel} yetkisi kaldırılacak.`"
        confirmText="Kaldır"
        variant="danger"
        @confirm="revoke"
        @cancel="revokeTarget = null"
    />
  </div>
</template>

<script setup>
import {ref, computed, watch, onMounted, onUnmounted} from 'vue'
import DocApi from '../../api/DocApi.js'
import ConfirmDialog from '../common/ConfirmDialog.vue'

const props = defineProps({
  projectId: {type: String, required: true},
  spaceId: {type: String, required: true},
  pageId: {type: String, default: null}
})

const emit = defineEmits(['close'])

const tab = ref('list')
const permissions = ref([])
const revokeTarget = ref(null)
const form = ref({
  scope: 'space',
  targetType: 'USER',
  targetId: '',
  accessLevel: 'READ',
  canDelegate: false,
  useDelegate: false
})

const targetTypeLabels = {
  USER: 'Kullanıcı',
  TEAM: 'Takım',
  ORGANIZATION: 'Organizasyon',
  PROJECT_MEMBERS: 'Proje Üyeleri'
}

const searchPlaceholders = {
  USER: 'İsim veya e-posta ile ara...',
  TEAM: 'Takım adı ile ara...',
  ORGANIZATION: 'Organizasyon seç...',
  PROJECT_MEMBERS: 'Proje seç...'
}

// ─── Hedef arama ─────────────────────────────────────────────────────────────
const targetSearch = ref('')
const targetResults = ref([])
const selectedTarget = ref(null)
const searching = ref(false)
const showResults = ref(false)
let searchTimer = null

const searchPlaceholder = computed(() => searchPlaceholders[form.value.targetType])

const isFormValid = computed(() => form.value.targetId && form.value.accessLevel && form.value.targetType)

watch(() => form.value.targetType, () => {
  clearTarget()
})

watch(targetSearch, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(searchTargets, 300)
})

async function searchTargets() {
  searching.value = true
  showResults.value = true
  try {
    const res = await DocApi.searchPermissionTargets(props.projectId, form.value.targetType, targetSearch.value)
    targetResults.value = res.data
  } catch (e) {
    console.error(e)
    targetResults.value = []
  } finally {
    searching.value = false
  }
}

function onSearchFocus() {
  searchTargets()
}

function selectTarget(target) {
  selectedTarget.value = target
  form.value.targetId = target.id
  showResults.value = false
}

function clearTarget() {
  selectedTarget.value = null
  form.value.targetId = ''
  targetSearch.value = ''
  targetResults.value = []
  showResults.value = false
}

onMounted(loadPermissions)
onUnmounted(() => clearTimeout(searchTimer))

async function loadPermissions() {
  try {
    const params = {}
    if (props.pageId) params.pageId = props.pageId
    else params.spaceId = props.spaceId
    const res = await DocApi.getPermissions(props.projectId, params)
    permissions.value = res.data
  } catch (e) {
    console.error(e)
  }
}

async function grant() {
  const data = {
    spaceId: form.value.scope === 'space' ? props.spaceId : null,
    pageId: form.value.scope === 'page' ? props.pageId : null,
    targetType: form.value.targetType,
    targetId: form.value.targetId,
    accessLevel: form.value.accessLevel,
    canDelegate: form.value.canDelegate
  }

  try {
    if (form.value.useDelegate) {
      await DocApi.delegatePermission(props.projectId, data)
    } else {
      await DocApi.grantPermission(props.projectId, data)
    }
    await loadPermissions()
    tab.value = 'list'
    form.value = {scope: 'space', targetType: 'USER', targetId: '', accessLevel: 'READ', canDelegate: false, useDelegate: false}
    clearTarget()
  } catch (e) {
    console.error(e)
  }
}

async function revoke() {
  const perm = revokeTarget.value
  revokeTarget.value = null
  if (!perm) return
  try {
    await DocApi.revokePermission(props.projectId, perm.id)
    permissions.value = permissions.value.filter(p => p.id !== perm.id)
  } catch (e) {
    console.error(e)
  }
}

function accessBadgeClass(level) {
  const base = 'text-xs px-2 py-0.5 rounded-full'
  switch (level) {
    case 'READ': return base + ' bg-emerald-50 text-emerald-600'
    case 'WRITE': return base + ' bg-amber-50 text-amber-600'
    case 'ADMIN': return base + ' bg-rose-50 text-rose-600'
    default: return base + ' bg-slate-100 text-slate-600'
  }
}
</script>

