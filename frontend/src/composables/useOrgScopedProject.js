/**
 * useOrgScopedProject.js
 *
 * Proje kapsamlı sayfaları (Docs, Ortak Çalışma) aktif organizasyona bağlar.
 *
 * <b>Çözdüğü sorun:</b> Docs ve Ortak Çalışma proje kapsamlı, proje de bir
 * organizasyona ait. Ama sayfa yalnızca URL'deki `projectId`'ye bakıyordu:
 * kullanıcı organizasyon tercihini değiştirdiğinde ekranda <b>önceki
 * organizasyonun</b> dokümanı açık kalmaya devam ediyordu. Board ve iş listesi
 * bu sorunu yaşamıyor çünkü onlar takım/organizasyon üzerinden adresleniyor.
 *
 * İki yönlü çalışır:
 *  - <b>Bağlantı takibi:</b> başka bir organizasyonun projesine ait bir link
 *    açıldığında (paylaşılan doküman adresi) ve kullanıcı o organizasyonun da
 *    üyesiyse, aktif organizasyon o tarafa taşınır. Linkin kırılmasındansa
 *    tercihi içeriğe uydurmak doğru davranış: iki taraf da aynı organizasyonu
 *    gösterdiği sürece karışma olmaz.
 *  - <b>Tercih takibi:</b> kullanıcı organizasyonunu değiştirdiğinde, ekrandaki
 *    proje yeni organizasyona ait değilse `onLeave` çağrılır ve sayfa oradan
 *    çıkarılır.
 *
 * Üye olunmayan bir organizasyonun projesi zaten backend tarafından
 * reddediliyor (403); burada yapılan iş yetkilendirme değil, <b>bağlam
 * tutarlılığı</b>.
 */

import { onMounted, unref, watch } from 'vue'
import ProjectApi from '../api/ProjectApi.js'
import useOrganizationContext from './useOrganizationContext.js'

/**
 * projectId → organizationId. Aynı proje içinde sayfa değiştirmek (space →
 * page → ortak düzenleme) tek bir istekle yetinsin diye önbellekleniyor;
 * bir projenin organizasyonu değişmez.
 */
const orgIdByProject = new Map()

export async function resolveProjectOrgId(projectId) {
  if (!projectId) return null
  if (orgIdByProject.has(projectId)) return orgIdByProject.get(projectId)

  const { data } = await ProjectApi.getById(projectId)
  const orgId = data?.organizationId ?? null
  orgIdByProject.set(projectId, orgId)
  return orgId
}

/**
 * @param projectIdRef izlenecek proje kimliği (ref, getter ya da düz değer)
 * @param onLeave      proje aktif organizasyona ait değilken çağrılır — sayfa
 *                     kendi çıkışını (yönlendirme) burada yapar
 */
export function useOrgScopedProject(projectIdRef, { onLeave } = {}) {
  const { organizations, activeOrgId, loadOrganizations, selectOrg } = useOrganizationContext()

  const currentProjectId = () =>
      typeof projectIdRef === 'function' ? projectIdRef() : unref(projectIdRef)

  function leave() {
    if (typeof onLeave === 'function') onLeave()
  }

  /**
   * Projenin organizasyonu — okunamıyorsa `null`.
   *
   * <b>Neden hata sessiz geçiliyor:</b> proje üstverisi <i>proje üyeliği</i>
   * ister, oysa Docs bir space'i ya da tek bir sayfayı proje üyesi olmayan bir
   * kişiyle de paylaşabiliyor (DocPermission USER/TEAM hedefleri). Böyle bir
   * okuyucu için çağrı 403 döner; bunu "başka organizasyon" sayıp kullanıcıyı
   * sayfadan atmak, kendisiyle paylaşılmış dokümanı erişilemez kılardı. Bilgi
   * yoksa sayfaya dokunulmuyor — erişim denetimi zaten backend'de.
   */
  async function projectOrgIdOrNull() {
    const projectId = currentProjectId()
    if (!projectId) return null
    try {
      return await resolveProjectOrgId(projectId)
    } catch {
      return null
    }
  }

  /** Bağlantı takibi — yalnızca sayfa açılışında ve proje değiştiğinde. */
  async function adopt() {
    if (!currentProjectId()) return

    await loadOrganizations()
    const projectOrgId = await projectOrgIdOrNull()
    if (!projectOrgId || projectOrgId === activeOrgId.value) return

    if (organizations.value.some((org) => org.id === projectOrgId)) {
      selectOrg(projectOrgId)
      return
    }
    leave()
  }

  /** Tercih takibi — kullanıcı switcher'dan organizasyon değiştirdiğinde. */
  watch(activeOrgId, async (orgId, previous) => {
    if (!orgId || orgId === previous) return
    if (!currentProjectId()) return

    const projectOrgId = await projectOrgIdOrNull()
    if (projectOrgId && projectOrgId !== orgId) leave()
  })

  watch(() => currentProjectId(), (projectId, previous) => {
    if (projectId && projectId !== previous) adopt()
  })

  onMounted(adopt)

  return { activeOrgId, adopt }
}

export default useOrgScopedProject
