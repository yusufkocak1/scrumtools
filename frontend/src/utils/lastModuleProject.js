/**
 * Docs ve Ortak Çalışma modüllerinin "son kullanılan proje" hatırası.
 *
 * <b>Neden organizasyona göre ayrıldı:</b> önceden tek bir `docs_last_project_id`
 * anahtarı vardı. İki organizasyonu olan bir kullanıcı A'dayken Docs açtığında
 * A'nın projesi bu anahtara yazılıyor, sonra organizasyonunu B'ye çevirip
 * navbar'dan Docs'a bastığında doğrudan <b>A'nın projesine</b> gidiliyordu —
 * tercih değişmiş olmasına rağmen diğer organizasyonun içeriği açılıyordu.
 * Anahtar artık organizasyon kimliğini taşıyor, dolayısıyla her organizasyonun
 * kendi son projesi hatırlanıyor ve biri diğerine sızmıyor.
 *
 * Yalnızca bir kolaylık kaydı: gerçek erişim denetimi backend'de proje bazında
 * yapılıyor, buradaki kayıp/bozuk değer en fazla proje seçme ekranına düşürür.
 */

const KEYS = {
  docs: 'docs_last_project_id',
  collab: 'collab_last_project_id'
}

// Organizasyonsuz eski anahtarlar bir kez temizleniyor: hangi organizasyona ait
// olduklarını bilmenin yolu yok, okunmaya devam etselerdi yukarıdaki hata
// aynen sürerdi.
try {
  Object.values(KEYS).forEach((key) => localStorage.removeItem(key))
} catch {
  // Private mode / kota — kalıcılık zaten best-effort.
}

function keyFor(module, orgId) {
  const base = KEYS[module]
  return base && orgId ? `${base}:${orgId}` : null
}

export function rememberModuleProject(module, orgId, projectId) {
  const key = keyFor(module, orgId)
  if (!key || !projectId) return
  try {
    localStorage.setItem(key, projectId)
  } catch { /* kalıcılık best-effort */ }
}

export function recallModuleProject(module, orgId) {
  const key = keyFor(module, orgId)
  if (!key) return null
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

export function forgetModuleProject(module, orgId) {
  const key = keyFor(module, orgId)
  if (!key) return
  try {
    localStorage.removeItem(key)
  } catch { /* yok sayılır */ }
}
