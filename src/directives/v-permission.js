/**
 * v-permission direktifi
 *
 * Kullanım:
 * <button v-permission="'ISSUE_CREATE'">Görev Ekle</button>
 * <div v-permission="['ISSUE_EDIT_ALL', 'ISSUE_EDIT_OWN']">Düzenle</div>
 *
 * İzin yoksa element gizlenir (display:none).
 * Tam kontrole ihtiyaç varsa usePermissions composable kullanın.
 */

import { useAuth } from '../composables/useAuth.js'

export const vPermission = {
  mounted(el, binding) {
    checkPermission(el, binding)
  },
  updated(el, binding) {
    checkPermission(el, binding)
  },
}

function checkPermission(el, binding) {
  const { user, isSuperAdmin } = useAuth()

  if (isSuperAdmin.value) {
    el.style.display = ''
    return
  }

  const requiredPermissions = Array.isArray(binding.value)
    ? binding.value
    : [binding.value]

  // User verisi yoksa gizle
  if (!user.value) {
    el.style.display = 'none'
    return
  }

  // User'ın sahip olduğu izinler (localStorage'dan veya composable'dan gelir)
  const userPermissions = user.value?.permissions || []

  const hasPermission =
    userPermissions.includes('ADMIN_FULL_ACCESS') ||
    requiredPermissions.some(p => userPermissions.includes(p))

  el.style.display = hasPermission ? '' : 'none'
}

export default vPermission

