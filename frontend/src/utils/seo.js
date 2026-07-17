// SPA içi SEO yardımcıları: sayfa başına title/meta/canonical/JSON-LD yönetimi.
// Blog gibi giriş gerektirmeyen sayfalarda kullanılır; sayfa değişiminde
// resetSeo() çağrılarak eklenen etiketler temizlenir.

export const SITE_URL = 'https://scrumtools.kocak.net.tr'
export const SITE_NAME = 'ScrumTools'

const MANAGED_ATTR = 'data-seo-managed'
const DEFAULT_TITLE = 'Scrum Tools'

function upsertTag(selector, create) {
    let el = document.head.querySelector(selector)
    if (!el) {
        el = create()
        el.setAttribute(MANAGED_ATTR, '1')
        document.head.appendChild(el)
    }
    return el
}

function setMetaByName(name, content) {
    const el = upsertTag(`meta[name="${name}"]`, () => {
        const m = document.createElement('meta')
        m.setAttribute('name', name)
        return m
    })
    el.setAttribute('content', content)
}

function setMetaByProperty(property, content) {
    const el = upsertTag(`meta[property="${property}"]`, () => {
        const m = document.createElement('meta')
        m.setAttribute('property', property)
        return m
    })
    el.setAttribute('content', content)
}

/**
 * Sayfanın SEO etiketlerini ayarlar.
 * @param {Object} opts
 * @param {string} opts.title - document.title ve og:title
 * @param {string} opts.description - meta description ve og:description
 * @param {string} opts.path - canonical için site kökünden yol (örn. '/blog/scrum-nedir')
 * @param {string} [opts.type] - og:type (varsayılan 'website', makaleler için 'article')
 * @param {Array<Object>} [opts.jsonLd] - sayfaya eklenecek JSON-LD nesneleri
 */
export function setSeo({ title, description, path, type = 'website', jsonLd = [] }) {
    resetSeo()

    document.title = title
    const url = SITE_URL + path

    setMetaByName('description', description)
    setMetaByName('robots', 'index, follow')

    const canonical = upsertTag('link[rel="canonical"]', () => {
        const l = document.createElement('link')
        l.setAttribute('rel', 'canonical')
        return l
    })
    canonical.setAttribute('href', url)

    setMetaByProperty('og:title', title)
    setMetaByProperty('og:description', description)
    setMetaByProperty('og:type', type)
    setMetaByProperty('og:url', url)
    setMetaByProperty('og:site_name', SITE_NAME)
    setMetaByProperty('og:locale', 'tr_TR')
    setMetaByName('twitter:card', 'summary')
    setMetaByName('twitter:title', title)
    setMetaByName('twitter:description', description)

    for (const obj of jsonLd) {
        const script = document.createElement('script')
        script.type = 'application/ld+json'
        script.setAttribute(MANAGED_ATTR, '1')
        script.textContent = JSON.stringify(obj)
        document.head.appendChild(script)
    }
}

/** setSeo ile eklenen tüm etiketleri kaldırır ve varsayılan başlığa döner. */
export function resetSeo() {
    document.title = DEFAULT_TITLE
    document.head.querySelectorAll(`[${MANAGED_ATTR}]`).forEach(el => el.remove())
}
