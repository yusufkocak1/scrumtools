<template>
  <div class="overflow-x-auto rounded-xl border border-gray-200 dark:border-gray-700">
    <table class="w-full text-xs">
      <thead class="bg-gray-50 dark:bg-gray-700/50">
        <tr>
          <th class="text-left px-4 py-3 text-gray-500 font-medium sticky left-0 bg-gray-50 dark:bg-gray-700/50 min-w-[180px]">İzin</th>
          <th
            v-for="role in roles"
            :key="role.id"
            class="text-center px-3 py-3 font-medium text-gray-700 dark:text-gray-300 min-w-[100px]"
          >
            <div class="flex flex-col items-center gap-1">
              <div class="w-2 h-2 rounded-full" :style="{ backgroundColor: role.color }"></div>
              {{ role.name }}
            </div>
          </th>
        </tr>
      </thead>
      <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
        <template v-for="(perms, group) in groupedPermissions" :key="group">
          <tr class="bg-gray-50/50 dark:bg-gray-700/20">
            <td :colspan="roles.length + 1" class="px-4 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">
              {{ formatGroupName(group) }}
            </td>
          </tr>
          <tr
            v-for="perm in perms"
            :key="perm"
            class="hover:bg-gray-50 dark:hover:bg-gray-700/30"
          >
            <td class="px-4 py-2 text-gray-700 dark:text-gray-300 sticky left-0 bg-white dark:bg-gray-800">
              {{ formatPermission(perm) }}
            </td>
            <td
              v-for="role in roles"
              :key="role.id"
              class="text-center px-3 py-2"
            >
              <svg
                v-if="roleHasPermission(role, perm)"
                class="w-4 h-4 text-green-500 mx-auto"
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
              </svg>
              <span v-else class="text-gray-300">—</span>
            </td>
          </tr>
        </template>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import RoleApi from '../../api/RoleApi.js'

const props = defineProps({
  roles: { type: Array, default: () => [] }
})

const allPermissions = ref([])

const groupedPermissions = computed(() => {
  const groups = {}
  for (const perm of allPermissions.value) {
    const group = perm.split('_')[0]
    if (!groups[group]) groups[group] = []
    groups[group].push(perm)
  }
  return groups
})

function roleHasPermission(role, perm) {
  if (!role.permissions) return false
  return role.permissions.includes('ADMIN_FULL_ACCESS') || role.permissions.includes(perm)
}

function formatPermission(perm) {
  const parts = perm.split('_')
  return parts.slice(1).join(' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())
}

function formatGroupName(group) {
  const map = {
    PROJECT: 'Proje',
    TEAM: 'Takım',
    ISSUE: 'İş Öğesi',
    SPRINT: 'Sprint',
    BOARD: 'Board',
    COMMENT: 'Yorum',
    ATTACHMENT: 'Dosya',
    REPORT: 'Raporlama',
    WORKFLOW: 'Workflow',
    DOCS: 'Dokümanlar',
    ADMIN: 'Admin'
  }
  return map[group] || group
}

onMounted(async () => {
  try {
    const res = await RoleApi.getPermissions()
    allPermissions.value = res.data
  } catch (e) {
    console.error('İzinler yüklenemedi:', e)
  }
})
</script>

