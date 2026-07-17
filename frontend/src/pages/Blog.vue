<template>
  <div class="min-h-screen bg-white text-gray-900 flex flex-col">
    <BlogHeader/>

    <!-- Başlık -->
    <section class="bg-gradient-to-br from-gray-50 to-gray-100 border-b border-gray-200">
      <div class="max-w-4xl mx-auto px-4 lg:px-6 py-14 lg:py-20 text-center">
        <h1 class="text-3xl lg:text-5xl font-bold mb-4">ScrumTools Blog</h1>
        <p class="text-lg text-gray-600 max-w-2xl mx-auto">
          Scrum, çevik pratikler ve takım çalışması üzerine rehberler.
          Sık sorulan soruların güvenilir, uygulamaya dönük yanıtları.
        </p>
      </div>
    </section>

    <!-- Makale listesi -->
    <section class="flex-1 max-w-5xl mx-auto w-full px-4 lg:px-6 py-12 lg:py-16">
      <div class="grid sm:grid-cols-2 gap-6">
        <router-link
            v-for="post in posts"
            :key="post.slug"
            :to="`/blog/${post.slug}`"
            class="group bg-white rounded-2xl border border-gray-200 p-6 shadow-sm hover:shadow-lg hover:-translate-y-1 transition-all duration-200 flex flex-col">
          <div class="flex items-center gap-3 mb-3 text-xs">
            <span class="px-2.5 py-1 rounded-full bg-blue-100 text-blue-700 font-semibold">{{ post.category }}</span>
            <span class="text-gray-400">{{ formatDate(post.date) }}</span>
            <span class="text-gray-400">&middot; {{ post.readingMinutes }} dk okuma</span>
          </div>
          <h2 class="text-lg font-semibold mb-2 group-hover:text-blue-600 transition-colors leading-snug">
            {{ post.title }}
          </h2>
          <p class="text-sm text-gray-600 leading-relaxed flex-1">{{ post.excerpt }}</p>
          <span class="mt-4 text-sm font-medium text-blue-600 inline-flex items-center gap-1">
            Devamını oku
            <svg class="w-4 h-4 group-hover:translate-x-0.5 transition-transform" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M10.293 5.293a1 1 0 011.414 0l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414-1.414L12.586 11H5a1 1 0 110-2h7.586l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
          </span>
        </router-link>
      </div>
    </section>

    <BlogFooter/>
  </div>
</template>

<script>
import BlogHeader from '../components/blog/BlogHeader.vue'
import BlogFooter from '../components/blog/BlogFooter.vue'
import { posts } from '../blog/index.js'
import { setSeo, resetSeo, SITE_URL } from '../utils/seo.js'
import { formatBlogDate } from '../utils/blogDate.js'

export default {
  name: 'Blog',
  components: { BlogHeader, BlogFooter },
  data: () => ({ posts }),
  methods: {
    formatDate: formatBlogDate
  },
  created() {
    setSeo({
      title: 'Blog — Scrum ve Çevik Pratik Rehberleri | ScrumTools',
      description: 'Scrum poker, retrospektif, story point ve daha fazlası: Scrum ile ilgili sık sorulan soruların güvenilir, uygulamaya dönük yanıtları ScrumTools Blog\'da.',
      path: '/blog',
      jsonLd: [{
        '@context': 'https://schema.org',
        '@type': 'Blog',
        name: 'ScrumTools Blog',
        url: SITE_URL + '/blog',
        inLanguage: 'tr',
        blogPost: posts.map(p => ({
          '@type': 'BlogPosting',
          headline: p.title,
          url: `${SITE_URL}/blog/${p.slug}`,
          datePublished: p.date
        }))
      }]
    })
  },
  unmounted() {
    resetSeo()
  }
}
</script>
