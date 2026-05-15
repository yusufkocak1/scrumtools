<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 p-6">
    <div class="max-w-6xl mx-auto space-y-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Admin Paneli</h1>

      <!-- Tabs -->
      <div class="flex gap-1 bg-gray-100 dark:bg-gray-800 rounded-xl p-1 w-fit">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          @click="activeTab = tab.id"
          :class="[
            'px-4 py-2 text-sm rounded-lg transition-all font-medium',
            activeTab === tab.id
              ? 'bg-white dark:bg-gray-700 text-indigo-600 shadow-sm'
              : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
          ]"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Kullanıcılar -->
      <div v-if="activeTab === 'users'">
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow overflow-hidden">
          <div class="p-4 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
            <h3 class="font-semibold text-gray-900 dark:text-white">Tüm Kullanıcılar</h3>
            <span class="text-sm text-gray-500">{{ users.length }} kullanıcı</span>
          </div>

          <div v-if="usersLoading" class="text-center py-12 text-gray-500">Yükleniyor...</div>

          <table v-else class="w-full text-sm">
            <thead class="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th class="text-left px-4 py-3 text-gray-500 font-medium">Kullanıcı</th>
                <th class="text-left px-4 py-3 text-gray-500 font-medium">Sistem Rolü</th>
                <th class="text-left px-4 py-3 text-gray-500 font-medium">Durum</th>
                <th class="text-left px-4 py-3 text-gray-500 font-medium">Kayıt</th>
                <th class="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
              <tr
                v-for="user in users"
                :key="user.id"
                class="hover:bg-gray-50 dark:hover:bg-gray-700/30"
              >
                <td class="px-4 py-3">
                  <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-full bg-indigo-500 flex items-center justify-center text-white text-xs font-bold">
                      {{ user.name?.charAt(0)?.toUpperCase() }}
                    </div>
                    <div>
                      <p class="font-medium text-gray-900 dark:text-white">{{ user.name }}</p>
                      <p class="text-xs text-gray-500">{{ user.email }}</p>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-3">
                  <select
                    :value="user.systemRole"
                    @change="updateRole(user, $event.target.value)"
                    class="text-xs border border-gray-200 dark:border-gray-600 rounded px-2 py-1 bg-white dark:bg-gray-700"
                  >
                    <option value="USER">USER</option>
                    <option value="PLATFORM_ADMIN">PLATFORM_ADMIN</option>
                    <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                  </select>
                </td>
                <td class="px-4 py-3">
                  <select
                    :value="user.status"
                    @change="updateStatus(user, $event.target.value)"
                    class="text-xs border border-gray-200 dark:border-gray-600 rounded px-2 py-1 bg-white dark:bg-gray-700"
                    :class="user.status === 'ACTIVE' ? 'text-green-600' : 'text-red-500'"
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="INACTIVE">INACTIVE</option>
                    <option value="SUSPENDED">SUSPENDED</option>
                  </select>
                </td>
                <td class="px-4 py-3 text-gray-500 text-xs">
                  {{ formatDate(user.createdAt) }}
                </td>
                <td class="px-4 py-3 text-xs text-gray-400">
                  {{ user.lastLoginAt ? formatDate(user.lastLoginAt) : 'Giriş yok' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Roller -->
      <div v-if="activeTab === 'roles'">
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow p-6">
          <RoleManager />
        </div>
      </div>

      <!-- İzin Matrisi -->
      <div v-if="activeTab === 'permissions'">
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow p-6">
          <h3 class="font-semibold text-gray-900 dark:text-white mb-4">İzin Matrisi</h3>
          <PermissionMatrix :roles="defaultRoles" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import UserApi from '../api/UserApi.js'
import RoleApi from '../api/RoleApi.js'
import RoleManager from '../components/roles/RoleManager.vue'
import PermissionMatrix from '../components/roles/PermissionMatrix.vue'

const activeTab = ref('users')
const tabs = [
  { id: 'users', label: 'Kullanıcılar' },
  { id: 'roles', label: 'Roller' },
  { id: 'permissions', label: 'İzin Matrisi' },
]

const users = ref([])
const defaultRoles = ref([])
const usersLoading = ref(false)

async function loadUsers() {
  usersLoading.value = true
  try {
    const res = await UserApi.getAllUsers()
    users.value = res.data
  } catch (e) {
    console.error('Kullanıcılar yüklenemedi:', e)
  } finally {
    usersLoading.value = false
  }
}

async function loadRoles() {
  try {
    const res = await RoleApi.getDefaults()
    defaultRoles.value = res.data
  } catch (e) {
    console.error('Roller yüklenemedi:', e)
  }
}

async function updateRole(user, newRole) {
  try {
    const res = await UserApi.updateSystemRole(user.id, newRole)
    const idx = users.value.findIndex(u => u.id === user.id)
    if (idx !== -1) users.value[idx] = res.data
  } catch (e) {
    console.error('Rol güncellenemedi:', e)
  }
}

async function updateStatus(user, newStatus) {
  try {
    const res = await UserApi.updateUserStatus(user.id, newStatus)
    const idx = users.value.findIndex(u => u.id === user.id)
    if (idx !== -1) users.value[idx] = res.data
  } catch (e) {
    console.error('Durum güncellenemedi:', e)
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('tr-TR')
}

onMounted(() => {
  loadUsers()
  loadRoles()
})
</script>

