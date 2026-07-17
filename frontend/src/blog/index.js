// Blog içerik kaydı — SEO amaçlı statik makaleler.
// Yeni makale eklemek için posts/ altına dosyayı oluşturup buraya import etmek yeterlidir
// (sitemap.xml'e de URL eklenmesi unutulmamalı: frontend/public/sitemap.xml).
import scrumNedir from './posts/scrum-nedir.js'
import scrumPokerNedir from './posts/scrum-poker-nedir.js'
import storyPointNedir from './posts/story-point-nedir.js'
import kanbanScrum from './posts/kanban-ile-scrum-farklari.js'
import burndownNedir from './posts/burndown-grafigi-nedir.js'
import retroNedir from './posts/sprint-retrospektifi-nedir.js'
import backlogOrganizasyonu from './posts/urun-is-listesi-nasil-organize-edilmeli.js'

// Yeniden eskiye sıralı liste
export const posts = [
    scrumNedir,
    scrumPokerNedir,
    storyPointNedir,
    kanbanScrum,
    burndownNedir,
    retroNedir,
    backlogOrganizasyonu,
].sort((a, b) => b.date.localeCompare(a.date))

export function getPost(slug) {
    return posts.find(p => p.slug === slug) || null
}

export function getRelated(post) {
    return (post.related || [])
        .map(slug => getPost(slug))
        .filter(Boolean)
}
