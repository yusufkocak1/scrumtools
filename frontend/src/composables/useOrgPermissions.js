/**
 * useOrgPermissions.js
 *
 * Aktif organizasyondaki rolün tek yorumlandığı yer. Arayüz "yetkin var mı"
 * sorusunu her ekranda kendi başına cevaplamaya çalışırsa (üye listesini çekip
 * kendini arayarak) hem gereksiz istek atar hem de ekranlar arasında tutarsız
 * davranır — kullanıcı görebildiği bir sekmeye tıklayıp "yetkiniz yok" uyarısı alır.
 *
 * Rol backend'den organizasyon nesnesiyle birlikte gelir (OrganizationResponse.myRole),
 * yani ek istek yoktur ve sunucudaki yetki kontrolüyle aynı kaynağa dayanır.
 * Buradaki bayraklar yalnızca GÖRÜNÜRLÜK içindir; asıl yetki kontrolü her zaman
 * backend'de yapılır.
 */

import { computed } from 'vue'
import { useOrganizationContext } from './useOrganizationContext.js'

export function useOrgPermissions() {
  const { activeOrg } = useOrganizationContext()

  /**
   * Rol bilinmiyorsa (eski bir backend myRole döndürmüyorsa) üye kabul edilir:
   * menü tamamen boş kalıp kullanıcıyı kilitlemek yerine en dar erişimle açılır.
   * Yönetim bölümleri yine gizli kalır ve gerçek kontrol backend'de yapılır.
   */
  const orgRole = computed(() => {
    if (!activeOrg.value) return null
    return activeOrg.value.myRole || 'ORG_MEMBER'
  })

  const isOwner = computed(() => orgRole.value === 'ORG_OWNER')
  const isAdmin = computed(() => isOwner.value || orgRole.value === 'ORG_ADMIN')
  /** Gözlemci: her şeyi görür, hiçbir şeyi değiştiremez. */
  const isViewer = computed(() => orgRole.value === 'ORG_VIEWER')
  const isMember = computed(() => !!orgRole.value)

  /**
   * Ekran/aksiyon bazlı bayraklar. Component'ler rol adlarını değil bu niyetleri
   * kullanır — rol modeli değiştiğinde tek dosya güncellenir.
   */
  const can = computed(() => ({
    viewProjects: isMember.value,
    createProject: isAdmin.value,

    viewTeams: isMember.value,
    manageTeams: isAdmin.value,

    viewMembers: isMember.value,
    inviteMembers: isAdmin.value,
    removeMembers: isAdmin.value,
    changeMemberRole: isOwner.value,

    viewBilling: isAdmin.value,
    manageBilling: isOwner.value,

    viewIntegrations: isAdmin.value,
    manageIntegrations: isAdmin.value,

    viewSettings: isAdmin.value,
    editSettings: isAdmin.value,
  }))

  function roleLabel(role = orgRole.value) {
    return {
      ORG_OWNER: 'Sahip',
      ORG_ADMIN: 'Admin',
      ORG_MEMBER: 'Üye',
      ORG_VIEWER: 'Gözlemci',
    }[role] || role || '—'
  }

  function roleBadgeClass(role = orgRole.value) {
    if (role === 'ORG_OWNER') return 'bg-purple-100 text-purple-700'
    if (role === 'ORG_ADMIN') return 'bg-blue-100 text-blue-700'
    if (role === 'ORG_VIEWER') return 'bg-gray-100 text-gray-600'
    return 'bg-green-100 text-green-700'
  }

  return {
    orgRole,
    isOwner,
    isAdmin,
    isViewer,
    isMember,
    can,
    roleLabel,
    roleBadgeClass,
  }
}

export default useOrgPermissions
