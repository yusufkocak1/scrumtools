<template>
  <header class="sticky top-0 z-50 bg-white/90 backdrop-blur border-b border-gray-200">
    <div class="max-w-7xl mx-auto px-4 lg:px-6 py-3 flex items-center justify-between">
      <router-link to="/" class="flex items-center gap-2 lg:gap-3 text-lg lg:text-2xl font-bold text-gray-900">
        <div class="w-8 h-8 lg:w-10 lg:h-10 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0">
          <svg class="w-5 h-5 lg:w-6 lg:h-6 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
            <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 18V6h16v12H4z"/>
            <path d="M6 8h2v2H6zm0 3h2v2H6zm3-3h2v2H9zm0 3h2v2H9zm3-3h6v2h-6zm0 3h4v2h-4z"/>
            <circle cx="17" cy="14" r="1.5"/>
            <path d="M6 15l2-2 2 2"/>
          </svg>
        </div>
        <span>ScrumTools</span>
      </router-link>

      <nav class="hidden md:flex items-center gap-6 text-sm font-medium text-gray-600">
        <router-link to="/" class="hover:text-blue-600 transition-colors">Ana Sayfa</router-link>
        <router-link to="/blog" class="hover:text-blue-600 transition-colors">Blog</router-link>
      </nav>

      <div class="flex items-center gap-2 lg:gap-3">
        <router-link
            to="/login"
            class="px-3 lg:px-4 py-2 text-sm font-medium text-gray-700 hover:text-blue-600 rounded-xl hover:bg-gray-50 transition-all duration-200">
          Giriş Yap
        </router-link>
        <router-link
            :to="{ path: '/login', query: { mode: 'signup' } }"
            class="px-3 lg:px-5 py-2 text-sm font-medium bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-md hover:shadow-lg transition-all duration-200">
          Ücretsiz Başla
        </router-link>
      </div>
    </div>
  </header>
</template>

<script>
export default {
  name: 'BlogHeader'
}
</script>
