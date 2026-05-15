<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[80vh] flex flex-col">
      <!-- Header -->
      <div class="p-5 border-b flex items-center justify-between">
        <h2 class="text-lg font-bold text-gray-800">🔒 Yetki Yönetimi</h2>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 text-xl">✕</button>
      </div>

      <!-- Tabs -->
      <div class="px-5 pt-3 flex gap-4 border-b">
        <button @click="tab = 'list'"
                :class="['pb-2 text-sm font-medium border-b-2 transition', tab === 'list' ? 'border-indigo-500 text-indigo-600' : 'border-transparent text-gray-500 hover:text-gray-700']">
          Mevcut Yetkiler
        </button>
        <button @click="tab = 'add'"
                :class="['pb-2 text-sm font-medium border-b-2 transition', tab === 'add' ? 'border-indigo-500 text-indigo-600' : 'border-transparent text-gray-500 hover:text-gray-700']">
          Yetki Ekle
        </button>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto p-5">
        <!-- LIST TAB -->
        <div v-if="tab === 'list'">
          <div v-if="permissions.length === 0" class="text-center py-8 text-gray-500 text-sm">
            Henüz tanımlı yetki yok
          </div>
          <div v-else class="space-y-2">
            <div v-for="perm in permissions" :key="perm.id"
                 class="flex items-center justify-between bg-gray-50 rounded-lg px-4 py-3">
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-sm font-medium text-gray-800">{{ perm.targetName }}</span>
                  <span class="text-xs bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded">{{ targetTypeLabels[perm.targetType] }}</span>
                  <span :class="accessBadgeClass(perm.accessLevel)">{{ perm.accessLevel }}</span>
                  <span v-if="perm.canDelegate" class="text-xs bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded">Devredilebilir</span>
                </div>
                <p class="text-xs text-gray-400 mt-1">{{ perm.grantedByName }} tarafından verildi</p>
              </div>
              <button @click="revoke(perm)" class="text-red-500 hover:text-red-700 text-sm">Kaldır</button>
            </div>
          </div>
        </div>

        <!-- ADD TAB -->
        <div v-if="tab === 'add'" class="space-y-4">
          <!-- Scope: Space veya Page -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Kapsam</label>
            <div class="flex gap-3">
              <label class="flex items-center gap-2 text-sm">
                <input type="radio" v-model="form.scope" value="space"/> Space (tamamı)
              </label>
              <label v-if="pageId" class="flex items-center gap-2 text-sm">
                <input type="radio" v-model="form.scope" value="page"/> Sadece bu sayfa
              </label>
            </div>
          </div>

          <!-- Target Type -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Yetki hedefi</label>
            <select v-model="form.targetType" class="w-full border rounded-lg px-3 py-2 text-sm">
              <option value="USER">Kullanıcı</option>
              <option value="TEAM">Takım</option>
              <option value="ORGANIZATION">Organizasyon</option>
              <option value="PROJECT_MEMBERS">Proje Üyeleri</option>
            </select>
          </div>

          <!-- Target ID — basit input (ileride arama/seçim yapılabilir) -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Hedef ID</label>
            <input v-model="form.targetId" type="text" placeholder="UUID..."
                   class="w-full border rounded-lg px-3 py-2 text-sm"/>
            <p class="text-xs text-gray-400 mt-1">Kullanıcı, takım, organizasyon veya proje UUID'si</p>
          </div>

          <!-- Access Level -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Erişim seviyesi</label>
            <select v-model="form.accessLevel" class="w-full border rounded-lg px-3 py-2 text-sm">
              <option value="READ">Okuma (READ)</option>
              <option value="WRITE">Yazma (WRITE)</option>
              <option value="ADMIN">Yönetim (ADMIN)</option>
            </select>
          </div>

          <!-- Can Delegate -->
          <div class="flex items-center gap-2">
            <input type="checkbox" v-model="form.canDelegate" id="canDelegate"
                   class="rounded border-gray-300 text-indigo-600"/>
            <label for="canDelegate" class="text-sm text-gray-700">Yetki devri yapabilsin</label>
          </div>

          <!-- Delegate mode -->
          <div class="flex items-center gap-2">
            <input type="checkbox" v-model="form.useDelegate" id="useDelegate"
                   class="rounded border-gray-300 text-indigo-600"/>
            <label for="useDelegate" class="text-sm text-gray-700">Devretme modunda ekle (kendi yetkim üzerinden)</label>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div v-if="tab === 'add'" class="p-5 border-t flex justify-end">
        <button @click="grant" :disabled="!isFormValid"
                class="bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-300 text-white px-5 py-2 rounded-lg text-sm transition">
          Yetki Ver
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue'
import DocApi from '../../api/DocApi.js'

const props = defineProps({
  projectId: {type: String, required: true},
  spaceId: {type: String, required: true},
  pageId: {type: String, default: null}
})

const emit = defineEmits(['close'])

const tab = ref('list')
const permissions = ref([])
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

const isFormValid = computed(() => form.value.targetId && form.value.accessLevel && form.value.targetType)

onMounted(loadPermissions)

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
  } catch (e) {
    console.error(e)
  }
}

async function revoke(perm) {
  if (!confirm('Bu yetkiyi kaldırmak istediğinize emin misiniz?')) return
  try {
    await DocApi.revokePermission(props.projectId, perm.id)
    permissions.value = permissions.value.filter(p => p.id !== perm.id)
  } catch (e) {
    console.error(e)
  }
}

function accessBadgeClass(level) {
  const base = 'text-xs px-1.5 py-0.5 rounded'
  switch (level) {
    case 'READ': return base + ' bg-green-100 text-green-700'
    case 'WRITE': return base + ' bg-yellow-100 text-yellow-700'
    case 'ADMIN': return base + ' bg-red-100 text-red-700'
    default: return base + ' bg-gray-100 text-gray-700'
  }
}
</script>

