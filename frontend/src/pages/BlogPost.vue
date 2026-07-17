<template>
  <div v-if="post" class="min-h-screen bg-white text-gray-900 flex flex-col">
    <BlogHeader/>

    <main class="flex-1 w-full">
      <article class="max-w-3xl mx-auto px-4 lg:px-6 py-10 lg:py-14">
        <!-- Breadcrumb -->
        <nav class="text-sm text-gray-500 mb-6 flex items-center gap-2 flex-wrap" aria-label="breadcrumb">
          <router-link to="/" class="hover:text-blue-600 transition-colors">Ana Sayfa</router-link>
          <span>/</span>
          <router-link to="/blog" class="hover:text-blue-600 transition-colors">Blog</router-link>
          <span>/</span>
          <span class="text-gray-700">{{ post.category }}</span>
        </nav>

        <!-- Başlık ve meta -->
        <header class="mb-8">
          <h1 class="text-3xl lg:text-4xl font-bold leading-tight mb-4">{{ post.title }}</h1>
          <div class="flex items-center gap-3 text-sm text-gray-500 flex-wrap">
            <span class="px-2.5 py-1 rounded-full bg-blue-100 text-blue-700 text-xs font-semibold">{{ post.category }}</span>
            <span>{{ formatDate(post.date) }}</span>
            <span>&middot; {{ post.readingMinutes }} dk okuma</span>
          </div>
        </header>

        <!-- Makale içeriği (statik, repo içinde yazılmış HTML) -->
        <div
            class="prose prose-gray max-w-none prose-headings:font-bold prose-h2:text-2xl prose-h2:mt-10 prose-a:text-blue-600 prose-a:no-underline hover:prose-a:underline prose-li:marker:text-blue-600"
            v-html="post.html"
            @click="onContentClick"
        ></div>

        <!-- SSS -->
        <section v-if="post.faq && post.faq.length" class="mt-12">
          <h2 class="text-2xl font-bold mb-6">Sık Sorulan Sorular</h2>
          <div class="space-y-3">
            <details
                v-for="item in post.faq"
                :key="item.q"
                class="group bg-gray-50 rounded-xl border border-gray-200 px-5 py-4">
              <summary class="cursor-pointer font-medium text-gray-900 flex items-center justify-between gap-4 list-none">
                {{ item.q }}
                <svg class="w-5 h-5 text-gray-400 flex-shrink-0 group-open:rotate-180 transition-transform" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd"/>
                </svg>
              </summary>
              <p class="mt-3 text-gray-600 leading-relaxed text-sm">{{ item.a }}</p>
            </details>
          </div>
        </section>

        <!-- Yumuşak CTA -->
        <aside class="mt-12 bg-gray-50 border border-gray-200 rounded-2xl p-6 lg:p-8">
          <h2 class="text-lg font-semibold mb-2">Okuduklarınızı uygulamak isterseniz</h2>
          <p class="text-sm text-gray-600 leading-relaxed mb-5">
            ScrumTools; sprint panosu, planlama pokeri, retro panoları ve raporlarıyla
            bu yazıda anlatılan pratikleri takımınızla birlikte uygulayabileceğiniz
            ücretsiz başlangıç paketi sunar. Kurulum gerektirmez.
          </p>
          <div class="flex items-center gap-3 flex-wrap">
            <router-link
                :to="{ path: '/login', query: { mode: 'signup' } }"
                class="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-xl shadow-md hover:shadow-lg transition-all duration-200">
              Ücretsiz Deneyin
            </router-link>
            <router-link to="/" class="text-sm font-medium text-gray-600 hover:text-blue-600 transition-colors">
              Özellikleri inceleyin
            </router-link>
          </div>
        </aside>

        <!-- İlgili yazılar -->
        <section v-if="related.length" class="mt-12">
          <h2 class="text-2xl font-bold mb-6">İlgili Yazılar</h2>
          <div class="grid sm:grid-cols-2 gap-4">
            <router-link
                v-for="rp in related"
                :key="rp.slug"
                :to="`/blog/${rp.slug}`"
                class="group bg-white rounded-xl border border-gray-200 p-5 shadow-sm hover:shadow-md transition-all duration-200">
              <span class="text-xs font-semibold text-blue-600">{{ rp.category }}</span>
              <h3 class="font-semibold mt-1 leading-snug group-hover:text-blue-600 transition-colors">{{ rp.title }}</h3>
              <p class="text-sm text-gray-500 mt-2 line-clamp-2">{{ rp.excerpt }}</p>
            </router-link>
          </div>
        </section>
      </article>
    </main>

    <BlogFooter/>
  </div>
</template>

<script>
import BlogHeader from '../components/blog/BlogHeader.vue'
import BlogFooter from '../components/blog/BlogFooter.vue'
import { getPost, getRelated } from '../blog/index.js'
import { setSeo, resetSeo, SITE_URL, SITE_NAME } from '../utils/seo.js'
import { formatBlogDate } from '../utils/blogDate.js'

export default {
  name: 'BlogPost',
  components: { BlogHeader, BlogFooter },
  props: {
    slug: { type: String, required: true }
  },
  data: () => ({
    post: null,
    related: []
  }),
  watch: {
    // İlgili yazılara geçişte aynı bileşen yeniden kullanılır
    slug: { handler: 'load', immediate: true }
  },
  methods: {
    formatDate: formatBlogDate,
    load() {
      this.post = getPost(this.slug)
      if (!this.post) {
        this.$router.replace('/blog')
        return
      }
      this.related = getRelated(this.post)
      window.scrollTo(0, 0)
      this.applySeo()
    },
    // v-html içindeki site içi bağlantıları tam sayfa yenilemeden router ile aç
    onContentClick(e) {
      const link = e.target.closest('a')
      if (!link) return
      const href = link.getAttribute('href')
      if (href && href.startsWith('/')) {
        e.preventDefault()
        this.$router.push(href)
      }
    },
    applySeo() {
      const p = this.post
      const url = `${SITE_URL}/blog/${p.slug}`
      setSeo({
        title: p.metaTitle,
        description: p.description,
        path: `/blog/${p.slug}`,
        type: 'article',
        jsonLd: [
          {
            '@context': 'https://schema.org',
            '@type': 'Article',
            headline: p.title,
            description: p.description,
            datePublished: p.date,
            dateModified: p.date,
            inLanguage: 'tr',
            mainEntityOfPage: url,
            author: { '@type': 'Organization', name: SITE_NAME, url: SITE_URL },
            publisher: { '@type': 'Organization', name: SITE_NAME, url: SITE_URL }
          },
          {
            '@context': 'https://schema.org',
            '@type': 'FAQPage',
            mainEntity: (p.faq || []).map(f => ({
              '@type': 'Question',
              name: f.q,
              acceptedAnswer: { '@type': 'Answer', text: f.a }
            }))
          },
          {
            '@context': 'https://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
              { '@type': 'ListItem', position: 1, name: 'Ana Sayfa', item: SITE_URL + '/' },
              { '@type': 'ListItem', position: 2, name: 'Blog', item: SITE_URL + '/blog' },
              { '@type': 'ListItem', position: 3, name: p.title, item: url }
            ]
          }
        ]
      })
    }
  },
  unmounted() {
    resetSeo()
  }
}
</script>
