 <template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white">Roller</h3>
      <button @click="showCreateModal = true" class="btn-primary text-sm">+ Yeni Rol</button>
    </div>

    <div class="grid grid-cols-1 gap-3">
      <div
        v-for="role in roles"
        :key="role.id"
        class="flex items-center justify-between bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 hover:border-indigo-300 transition-colors"
      >
        <div class="flex items-center gap-3">
          <div
            class="w-3 h-3 rounded-full"
            :style="{ backgroundColor: role.color || '#6B7280' }"
          ></div>
          <div>
            <p class="font-medium text-gray-900 dark:text-white text-sm">{{ role.name }}</p>
            <p class="text-xs text-gray-500">{{ role.permissions?.length || 0 }} izin · {{ formatScope(role.scope) }}</p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <span v-if="role.isDefault" class="text-xs text-gray-400 italic">Varsayılan</span>
          <button
            v-if="!role.isDefault"
            @click="editRole(role)"
            class="text-indigo-600 hover:text-indigo-800 text-xs"
          >Düzenle</button>
          <button
            v-if="!role.isDefault"
            @click="deleteRole(role)"
            class="text-red-500 hover:text-red-700 text-xs"
          >Sil</button>
        </div>
      </div>

      <div v-if="loading" class="text-center py-8 text-gray-500">Yükleniyor...</div>
      <div v-else-if="roles.length === 0" class="text-center py-8 text-gray-400">Rol bulunamadı.</div>
    </div>

    <!-- Create/Edit Modal -->
    <RoleEditor
      v-if="showCreateModal || editingRole"
      :role="editingRole"
      @saved="onSaved"
      @cancel="closeModal"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import RoleApi from '../../api/RoleApi.js'
import RoleEditor from './RoleEditor.vue'

const roles = ref([])
const loading = ref(false)
const showCreateModal = ref(false)
const editingRole = ref(null)

async function loadRoles() {
  loading.value = true
  try {
    const res = await RoleApi.getAll()
    roles.value = res.data
  } catch (e) {
    console.error('Roller yüklenemedi:', e)
  } finally {
    loading.value = false
  }
}

function editRole(role) {
  editingRole.value = role
}

async function deleteRole(role) {
  if (!confirm(`"${role.name}" rolünü silmek istediğinize emin misiniz?`)) return
  try {
    await RoleApi.delete(role.id)
    await loadRoles()
  } catch (e) {
    console.error('Rol silinemedi:', e)
  }
}

function onSaved() {
  closeModal()
  loadRoles()
}

function closeModal() {
  showCreateModal.value = false
  editingRole.value = null
}

function formatScope(scope) {
  const map = { SYSTEM: 'Sistem', ORGANIZATION: 'Organizasyon', PROJECT: 'Proje', TEAM: 'Takım' }
  return map[scope] || scope
}

onMounted(loadRoles)
</script>

<style scoped>
.btn-primary { @apply px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors text-sm; }
</style>

