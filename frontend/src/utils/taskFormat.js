/**
 * taskFormat.js
 *
 * Görev detay panellerinin ortak biçimlendiricileri. Paneller ayrı bileşenlere
 * bölündüğü için tarih/baş harf yardımcıları tek yerde tutulur — aksi hâlde her
 * panel kendi kopyasını taşır ve zamanla birbirinden ayrışır.
 */

/** "ali.veli@x.com" → "AV", "Ali Veli" → "AV" */
export function getInitials(emailOrName) {
  if (!emailOrName) return '?'
  const name = emailOrName.includes('@') ? emailOrName.split('@')[0] : emailOrName
  const parts = name.toUpperCase().replace(/[._-]/g, ' ').split(' ').filter(Boolean)
  if (!parts.length) return '?'
  return parts.length > 1 ? parts[0][0] + parts[1][0] : parts[0][0]
}

/** E-posta ise @ öncesini gösterir — yan kolonda tam adres taşmaya yol açıyor. */
export function displayName(emailOrName) {
  if (!emailOrName) return ''
  return emailOrName.includes('@') ? emailOrName.split('@')[0] : emailOrName
}

export function formatDate(dateString) {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString('tr-TR', {
    day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

export function formatShortDate(dateString) {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString('tr-TR', {
    day: 'numeric', month: 'short', year: 'numeric',
  })
}

/** Bir haftadan eskiler mutlak tarihe döner — "43 gün önce" kimseye bir şey söylemez. */
export function formatRelativeTime(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  const diffMin = Math.floor((Date.now() - date.getTime()) / 60000)

  if (diffMin < 1) return 'az önce'
  if (diffMin < 60) return `${diffMin} dk önce`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour} saat önce`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return `${diffDay} gün önce`
  return formatShortDate(dateString)
}
