<template>
  <nav
      class="fixed bottom-0 inset-x-0 z-40 flex flex-row items-center justify-around h-16 px-1 bg-white border-t border-gray-200 shadow-[0_-1px_3px_rgba(0,0,0,0.06)] pb-[env(safe-area-inset-bottom)]
             lg:static lg:inset-auto lg:z-auto lg:h-auto lg:w-20 lg:flex-col lg:items-stretch lg:justify-start lg:gap-2 lg:px-0 lg:py-4 lg:pb-4 lg:border-t-0 lg:border-r lg:shadow-sm">
    <div
        v-for="item in navItems"
        :key="item.label"
        @click="item.action()"
        :class="['group relative flex items-center justify-center p-1.5 rounded-xl transition-all duration-200 cursor-pointer lg:p-3 lg:mx-2', item.hoverClass]">
      <div :class="['w-9 h-9 lg:w-10 lg:h-10 rounded-lg flex items-center justify-center transition-colors', item.iconBgClass]">
        <svg :class="['w-5 h-5', item.iconTextClass]" fill="currentColor" :viewBox="item.viewBox" v-html="item.paths"></svg>
      </div>
      <!-- Tooltip (yalnızca masaüstü) -->
      <div class="hidden lg:block absolute left-full ml-3 px-2 py-1 bg-gray-900 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-10">
        {{ item.label }}
      </div>
    </div>
  </nav>
</template>

<script>
import { useTeamContext } from '../composables/useTeamContext.js'

export default {
  name: "SideBar",
  props: {
    // Docs navigasyonu için — aktif proje route'a göre sayfadan gelir
    projectId: String
  },
  setup() {
    // Takım seçimi merkezi context'ten okunur; sayfaların teamId prop'u geçmesi
    // gerekmez. Seçim Ayarlar'dan (ya da URL'deki takımın adoption'ı ile) değişir.
    const { activeTeamId, loadTeams } = useTeamContext()
    loadTeams()
    return { activeTeamId }
  },
  computed: {
    navItems() {
      return [
        {
          label: 'Dashboard',
          action: this.gotoDashboard,
          hoverClass: 'hover:bg-indigo-50 focus:bg-indigo-50 active:bg-indigo-100',
          iconBgClass: 'bg-indigo-100 group-hover:bg-indigo-200',
          iconTextClass: 'text-indigo-600',
          viewBox: '0 0 20 20',
          paths: '<path d="M2 10a8 8 0 018-8v8h8a8 8 0 11-16 0z"/><path d="M12 2.252A8.014 8.014 0 0117.748 8H12V2.252z"/>'
        },
        {
          label: 'Board',
          action: this.gotoWorkList,
          hoverClass: 'hover:bg-purple-50 focus:bg-purple-50 active:bg-purple-100',
          iconBgClass: 'bg-purple-100 group-hover:bg-purple-200',
          iconTextClass: 'text-purple-600',
          viewBox: '0 0 20 20',
          paths: '<path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"></path>'
        },
        {
          label: 'Docs',
          action: this.gotoDocs,
          hoverClass: 'hover:bg-teal-50 focus:bg-teal-50 active:bg-teal-100',
          iconBgClass: 'bg-teal-100 group-hover:bg-teal-200',
          iconTextClass: 'text-teal-600',
          viewBox: '0 0 20 20',
          paths: '<path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/>'
        },
        {
          label: 'Retrospective',
          action: this.gotoRetrospective,
          hoverClass: 'hover:bg-blue-50 focus:bg-blue-50 active:bg-blue-100',
          iconBgClass: 'bg-blue-100 group-hover:bg-blue-200',
          iconTextClass: 'text-blue-600',
          viewBox: '0 0 24 24',
          paths: '<path d="M16 4c0-1.11.89-2 2-2s2 .89 2 2-.89 2-2 2-2-.89-2-2zm4 18v-6h2.5l-2.54-7.63A1.5 1.5 0 0 0 18.54 7H16c-.8 0-1.54.37-2 .95L12.58 9.7c-.35.47-.98.75-1.64.75H9.5a2 2 0 0 0-2 2v1a1 1 0 0 0 1 1H10v8h2v-8h.5c.3 0 .6-.1.85-.29L14.7 12H16l2.05 6H20z"/><path d="M12.5 11.5c.83 0 1.5-.67 1.5-1.5s-.67-1.5-1.5-1.5S11 9.17 11 10s.67 1.5 1.5 1.5z"/><path d="M5.5 6c1.11 0 2-.89 2-2s-.89-2-2-2-2 .89-2 2 .89 2 2 2zm1.5 1h-3c-.83 0-1.54.5-1.84 1.22L.66 12.08c-.11.26-.16.54-.16.82 0 1.11.89 2 2 2h2.5v7h3v-7h1l1.31-3.92C10.69 9.77 9.39 7 5.5 7z"/><path d="M9 14l-2 2h6l-2-2H9z"/>'
        },
        {
          label: 'Scrum Poker',
          action: this.gotoScrumPoker,
          hoverClass: 'hover:bg-green-50 focus:bg-green-50 active:bg-green-100',
          iconBgClass: 'bg-green-100 group-hover:bg-green-200',
          iconTextClass: 'text-green-600',
          viewBox: '0 0 512 512',
          paths: '<path d="M452.279,114.361L298.254,73.25V38.138C298.254,17.109,282.291,0,261.203,0H70.512C49.425,0,31.076,17.114,31.076,38.152 v334.007c0,21.037,18.307,38.151,39.345,38.151h66.145l-1.146,4.276c-2.641,9.855-1.275,20.145,3.845,28.977 c5.112,8.819,13.359,15.129,23.223,17.772l184.192,49.354c3.31,0.886,6.637,1.31,9.911,1.31c16.873,0,32.344-11.246,36.905-28.264 l86.149-322.626C485.088,140.79,472.648,119.819,452.279,114.361z M141.679,391.226H70.421c-10.514,0-20.261-8.553-20.261-19.067 V38.152c0-10.514,9.788-19.068,20.353-19.068h190.691c10.565,0,17.967,8.547,17.967,19.054v29.998l-11.084-3.13 c-20.359-5.448-41.071,6.633-46.518,26.952l-5.493,21.059L182.84,70.456c-1.808-2.311-4.543-3.661-7.477-3.661 s-5.688,1.35-7.496,3.661L81.997,180.19c-2.703,3.455-2.699,8.307,0.005,11.762l84.231,107.626L141.679,391.226z M210.075,135.966 L172.417,276.51l-70.779-90.439L175.4,91.819l33.99,43.432C209.598,135.517,209.846,135.728,210.075,135.966z M461.508,156.171 l-86.447,322.626c-2.72,10.157-13.236,16.192-23.443,13.458l-184.192-49.354c-4.952-1.327-9.089-4.49-11.652-8.91 c-2.557-4.41-3.239-9.548-1.921-14.467l86.447-322.627c2.721-10.155,13.239-16.191,23.443-13.457l184.193,49.354 C458.14,135.529,464.229,146.016,461.508,156.171z"/><path d="M365.115,212.306c-13.618-3.652-28.135-1.539-40.214,5.448c-6.97-12.091-18.484-21.176-32.103-24.826 c-28.343-7.596-57.68,9.658-65.398,38.457c-4.46,16.647,9.303,46.354,21.635,68.343c6.121,10.914,27.125,46.802,40.406,50.361 c1.045,0.28,2.23,0.41,3.535,0.41c15.274,0,46.727-17.931,56.639-23.82c21.675-12.877,48.448-31.722,52.908-48.37 C410.24,249.51,393.46,219.901,365.115,212.306z M384.089,273.37c-1.558,5.817-15.315,19.398-41.834,35.47 c-23.175,14.044-42.023,21.591-47.803,22.552c-4.525-3.722-17.075-19.681-30.122-43.431 c-14.932-27.178-20.054-45.819-18.496-51.636c4.203-15.685,18.224-26.101,33.4-26.101c2.853,0,5.748,0.368,8.625,1.139 c11.634,3.117,20.551,12.044,23.85,23.88c0.906,3.251,3.462,5.781,6.722,6.655c3.255,0.876,6.737-0.04,9.148-2.402 c8.774-8.601,20.962-11.878,32.596-8.755C378.356,235.611,389.082,254.735,384.089,273.37z"/>'
        },
        {
          label: 'Code Share',
          action: this.gotoShare,
          hoverClass: 'hover:bg-orange-50 focus:bg-orange-50 active:bg-orange-100',
          iconBgClass: 'bg-orange-100 group-hover:bg-orange-200',
          iconTextClass: 'text-orange-600',
          viewBox: '0 0 20 20',
          paths: '<path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd"></path>'
        },
        {
          label: 'GameBox',
          action: this.gotoGameBox,
          hoverClass: 'hover:bg-yellow-50 focus:bg-yellow-50 active:bg-yellow-100',
          iconBgClass: 'bg-yellow-100 group-hover:bg-yellow-200',
          iconTextClass: 'text-yellow-600',
          viewBox: '0 0 20 20',
          paths: '<rect x="3" y="3" width="14" height="14" rx="3" ry="3" fill="none" stroke="currentColor" stroke-width="1.6"/><circle cx="7" cy="7" r="1.2"/><circle cx="13" cy="7" r="1.2"/><circle cx="10" cy="10" r="1.2"/><circle cx="7" cy="13" r="1.2"/><circle cx="13" cy="13" r="1.2"/>'
        }
      ]
    }
  },
  methods: {
    // Takım gerektiren modüller: aktif takım yoksa seçim yapılabilsin diye
    // Ayarlar'daki Çalışma Alanı bölümüne yönlendirilir.
    pushWithTeam(path) {
      if (!this.activeTeamId) {
        this.$router.push('/settings')
        return
      }
      this.$router.push(`${path}/${this.activeTeamId}`)
    },
    gotoDashboard() {
      this.$router.push('/dashboard')
    },
    gotoSettings() {
      this.$router.push(`/Settings`)
    },
    gotoScrumPoker() {
      this.pushWithTeam('/scrumPoker')
    },
    gotoTeams() {
      this.$router.push(`/teams`)
    },
    gotoWorkList() {
      this.pushWithTeam('/workList')
    },
    gotoShare() {
      this.pushWithTeam('/codeShare')
    },
    gotoGameBox() {
      this.pushWithTeam('/quiz')
    },
    gotoRetrospective() {
      this.$router.push(`/retrospective`)
    },
    gotoDocs() {
      const lastProjectId = this.projectId || localStorage.getItem('docs_last_project_id')
      if (lastProjectId) {
        this.$router.push(`/projects/${lastProjectId}/docs`)
      } else {
        this.$router.push('/docs')
      }
    }
  }
}
</script>
