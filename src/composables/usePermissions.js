import { computed } from 'vue'
import { useAuth } from './useAuth.js'

/**
 * Proje bazlı izin kontrolü için composable.
 *
 * Kullanım:
 * const { can, canAny } = usePermissions(projectMembers)
 *
 * v-if="can('ISSUE_CREATE')"
 * v-if="canAny(['ISSUE_EDIT_ALL', 'ISSUE_EDIT_OWN'])"
 */
export function usePermissions(projectMembers = null) {
  const { user, isSuperAdmin } = useAuth()

  // Mevcut kullanıcının proje rolüne ait izinleri hesapla
  const currentPermissions = computed(() => {
    if (!user.value) return new Set()
    if (isSuperAdmin.value) return new Set(['ADMIN_FULL_ACCESS'])

    if (!projectMembers) return new Set()

    const members = Array.isArray(projectMembers)
      ? projectMembers
      : projectMembers.value || []

    const member = members.find(m => m.userEmail === user.value?.email)
    if (!member || !member.permissions) return new Set()

    return new Set(member.permissions)
  })

  /**
   * Kullanıcının belirli bir izne sahip olup olmadığını kontrol eder
   */
  function can(permission) {
    if (isSuperAdmin.value) return true
    const perms = currentPermissions.value
    return perms.has('ADMIN_FULL_ACCESS') || perms.has(permission)
  }

  /**
   * Kullanıcının verilen izinlerden en az birine sahip olup olmadığını kontrol eder
   */
  function canAny(permissions) {
    return permissions.some(p => can(p))
  }

  /**
   * Kullanıcının verilen izinlerin tamamına sahip olup olmadığını kontrol eder
   */
  function canAll(permissions) {
    return permissions.every(p => can(p))
  }

  return {
    currentPermissions,
    can,
    canAny,
    canAll,
    isSuperAdmin,
  }
}

