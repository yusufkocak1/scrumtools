/**
 * PlanFeature (backend enum) → kullanıcıya gösterilen etiket.
 *
 * Tek kaynak: yeni bir PlanFeature eklendiğinde yalnız burası güncellenir.
 * Kullanan yerler: admin/PlanManager, billing/BillingTab, pages/Landing.
 * Sözlükteki sıra aynı zamanda arayüzdeki gösterim sırasıdır.
 */
export const FEATURE_LABELS = {
  SCRUM_POKER: 'Scrum Poker',
  RETRO: 'Retrospektif',
  WORK_BOARD: 'İş Panosu',
  QUIZ: 'GameBox',
  DOCS: 'Dokümanlar',
  DASHBOARD_REPORTS: 'Raporlar',
  ATTACHMENTS: 'Dosya Ekleri',
  CUSTOM_ROLES: 'Özel Roller',
  GIT_INTEGRATION: 'Git Entegrasyonu',
  CI_CD_INTEGRATION: 'CI/CD Entegrasyonu',
  RICH_FILTERS: 'Zengin Filtreler',
  COLLAB_WORKSPACE: 'Ortak Çalışma Alanı',
  COLLAB_SHEET: 'Ortak Hesap Tablosu',
  COLLAB_MACRO: 'Doküman Makroları',
}

/** Gösterim sırası — sözlüğün anahtar sırası. */
export const FEATURE_ORDER = Object.keys(FEATURE_LABELS)

/**
 * Etiketi döndürür; sözlükte yoksa enum adının kendisini verir.
 * Böylece backend'e yeni bir özellik eklenip burası unutulsa da
 * arayüzde satır kaybolmaz, ham adıyla görünür.
 */
export function featureLabel(feature) {
  return FEATURE_LABELS[feature] || feature
}
