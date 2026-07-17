<template>
  <footer class="bg-gray-900 text-gray-400">
    <div class="max-w-7xl mx-auto px-4 lg:px-6 py-10 flex flex-col md:flex-row items-center justify-between gap-4">
      <router-link to="/" class="flex items-center gap-2 text-white font-bold text-lg">
        <div class="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
          <svg class="w-5 h-5 text-white" fill="currentColor" viewBox="0 0 24 24">
            <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 18V6h16v12H4z"/>
            <path d="M6 8h2v2H6zm0 3h2v2H6zm3-3h2v2H9zm0 3h2v2H9zm3-3h6v2h-6zm0 3h4v2h-4z"/>
          </svg>
        </div>
        ScrumTools
      </router-link>
      <div class="flex items-center gap-6 text-sm">
        <router-link to="/" class="hover:text-white transition-colors">Ana Sayfa</router-link>
        <router-link to="/blog" class="hover:text-white transition-colors">Blog</router-link>
        <router-link to="/login" class="hover:text-white transition-colors">Giriş Yap</router-link>
      </div>
      <p class="text-xs">
        &copy; {{ new Date().getFullYear() }} ScrumTools
      </p>
    </div>
  </footer>
</template>

<script>
export default {
  name: 'BlogFooter'
}
</script>
