import { ref, computed } from 'vue'

/**
 * useAuth — Uygulamanın TEK auth kaynağı (single source of truth)
 *
 * AuthApi.js, Login.vue, App.vue, router.js hepsi bu composable'ı kullanır.
 * localStorage anahtarları mevcut yapi ile uyumlu:
 *   - 'jwt'         → JWT token (AuthApi.js ile aynı)
 *   - 'user'        → email string (AuthApi.js backward compat)
 *   - 'userProfile' → tam profil JSON (Faz1'den itibaren)
 */

// Modül seviyesinde reaktif state — tüm composable çağrıları aynı state'i paylaşır
const _jwt = ref(localStorage.getItem('jwt') || null)
const _user = ref(null)

// Sayfa yüklendiğinde mevcut profili yükle
;(function initUser() {
  const profileRaw = localStorage.getItem('userProfile')
  if (profileRaw) {
    try {
      _user.value = JSON.parse(profileRaw)
    } catch {
      localStorage.removeItem('userProfile')
    }
  } else {
    // Eski yapıdan kademeli geçiş: email ve name varsa minimal user oluştur
    const email = localStorage.getItem('user')
    const name = localStorage.getItem('name')
    if (email && _jwt.value) {
      _user.value = { email, name: name || email }
    }
  }
})()

export function useAuth() {
  // ─── Computed ─────────────────────────────────────────────────────────────
  const isAuthenticated = computed(() => !!_jwt.value)
  const isLogged = isAuthenticated // alias — mevcut App.vue ile uyum için

  const name = computed(() => _user.value?.name || '')
  const userEmail = computed(() => _user.value?.email || '')
  const avatarUrl = computed(() => _user.value?.avatarUrl || null)
  const systemRole = computed(() => _user.value?.systemRole || 'USER')
  const userStatus = computed(() => _user.value?.status || 'ACTIVE')

  const isSuperAdmin = computed(() => systemRole.value === 'SUPER_ADMIN')
  const isPlatformAdmin = computed(() =>
    systemRole.value === 'SUPER_ADMIN' || systemRole.value === 'PLATFORM_ADMIN'
  )

  // Kullanıcı aktif mi?
  const isActive = computed(() => userStatus.value === 'ACTIVE')

  // ─── Mutations ────────────────────────────────────────────────────────────

  /**
   * Token'ı güncelle (AuthApi.js login/register'da çağrılır)
   */
  function setToken(token) {
    _jwt.value = token
    if (token) {
      localStorage.setItem('jwt', token)
    } else {
      localStorage.removeItem('jwt')
    }
  }

  /**
   * Kullanıcı verisini güncelle
   * @param {Object|null} userData - { name, email, systemRole, status, avatarUrl, ... }
   */
  function setUser(userData) {
    _user.value = userData
    if (userData) {
      // Eski yapı ile uyum — 'user' ve 'name' anahtarlarını koru
      localStorage.setItem('user', userData.email || '')
      localStorage.setItem('name', userData.name || '')
      // Yeni tam profil
      localStorage.setItem('userProfile', JSON.stringify(userData))
    } else {
      localStorage.removeItem('user')
      localStorage.removeItem('name')
      localStorage.removeItem('userProfile')
    }
  }

  /**
   * Tam logout — token + kullanıcı bilgisi temizle
   */
  function logout() {
    _jwt.value = null
    _user.value = null
    localStorage.removeItem('jwt')
    localStorage.removeItem('user')
    localStorage.removeItem('name')
    localStorage.removeItem('userProfile')
    localStorage.removeItem('selectedTeam')
  }

  /**
   * Profil bilgisini backend'den çek ve güncelle
   * (isteğe bağlı — login sırasında veya sayfa yüklendiğinde çağrılabilir)
   */
  async function fetchProfile() {
    if (!_jwt.value) return null
    try {
      // Lazy import to avoid circular dependency
      const { UserApi } = await import('../api/UserApi.js')
      const res = await UserApi.getProfile()
      setUser(res.data)
      return res.data
    } catch (e) {
      console.warn('[useAuth] Profil yüklenemedi:', e?.response?.status)
      return null
    }
  }

  return {
    // State
    jwt: _jwt,
    user: _user,

    // Computed
    isAuthenticated,
    isLogged,
    name,
    userEmail,
    avatarUrl,
    systemRole,
    userStatus,
    isSuperAdmin,
    isPlatformAdmin,
    isActive,

    // Actions
    setToken,
    setUser,
    logout,
    fetchProfile,
  }
}
