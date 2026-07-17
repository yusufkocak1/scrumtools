// 'YYYY-MM-DD' biçimindeki blog tarihlerini Türkçe uzun biçime çevirir.
export function formatBlogDate(isoDate) {
    const [y, m, d] = isoDate.split('-').map(Number)
    return new Date(y, m - 1, d).toLocaleDateString('tr-TR', {
        day: 'numeric', month: 'long', year: 'numeric'
    })
}
