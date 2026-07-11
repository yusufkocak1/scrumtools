import { ref, computed, unref } from 'vue'
import { BillingApi } from '../api/BillingApi.js'

/**
 * useEntitlements — organizasyonun paket haklarını (plan, limit, kullanım) yönetir.
 *
 * Plan/limit bilgisi ASLA localStorage/JWT'de saklanmaz; her zaman backend'den
 * taze çekilir (org değişiminde ve ödeme sonrası refresh çağrılmalı).
 *
 * Kullanım:
 * const { planCode, isTrial, trialDaysLeft, hasFeature, isAtLimit, refresh } = useEntitlements(orgId)
 * await refresh()
 * v-if="hasFeature('DOCS')"
 * :disabled="isAtLimit('members')"
 */

// Modül seviyesinde cache — tüm bileşenler aynı state'i paylaşır (orgId -> entitlements)
const _byOrg = ref({})

export function useEntitlements(orgId = null) {
  const currentOrgId = () => unref(orgId)

  const entitlements = computed(() => {
    const id = currentOrgId()
    return (id && _byOrg.value[id]) || null
  })

  /**
   * Entitlement'ları backend'den yeniden yükle.
   * @param {string|null} targetOrgId - verilmezse composable'ın orgId'si kullanılır
   */
  async function refresh(targetOrgId = null) {
    const id = targetOrgId || currentOrgId()
    if (!id) return null
    try {
      const res = await BillingApi.getEntitlements(id)
      _byOrg.value = { ..._byOrg.value, [id]: res.data }
      return res.data
    } catch (e) {
      console.warn('[useEntitlements] Yüklenemedi:', e?.response?.status)
      return null
    }
  }

  // ─── Plan bilgisi ─────────────────────────────────────────────────────────
  const planCode = computed(() => entitlements.value?.planCode || 'FREE')
  const planName = computed(() => entitlements.value?.planName || 'Free')
  // TRIAL | ACTIVE | PAST_DUE | FREE
  const status = computed(() => entitlements.value?.status || 'FREE')
  const isTrial = computed(() => status.value === 'TRIAL')
  const isPastDue = computed(() => status.value === 'PAST_DUE')

  const trialDaysLeft = computed(() => {
    const end = entitlements.value?.trialEndsAt
    if (!end) return null
    return Math.max(Math.ceil((new Date(end) - Date.now()) / 86400000), 0)
  })

  const periodEnd = computed(() =>
    entitlements.value?.periodEnd ? new Date(entitlements.value.periodEnd) : null
  )

  // ─── Özellik ve limit kontrolleri ────────────────────────────────────────
  function hasFeature(feature) {
    return entitlements.value?.features?.includes(feature) ?? false
  }

  /** @param {'members'|'projects'} name — null dönerse sınırsız */
  function limit(name) {
    if (!entitlements.value) return null
    return name === 'members' ? entitlements.value.maxMembers : entitlements.value.maxProjects
  }

  /** @param {'members'|'projects'} name */
  function usage(name) {
    if (!entitlements.value) return 0
    return name === 'members' ? entitlements.value.memberCount : entitlements.value.projectCount
  }

  /** @param {'members'|'projects'} name */
  function isAtLimit(name) {
    const l = limit(name)
    return l != null && usage(name) >= l
  }

  return {
    entitlements,
    planCode,
    planName,
    status,
    isTrial,
    isPastDue,
    trialDaysLeft,
    periodEnd,
    hasFeature,
    limit,
    usage,
    isAtLimit,
    refresh,
  }
}
