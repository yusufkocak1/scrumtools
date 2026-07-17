import {createRouter, createWebHistory} from 'vue-router'

import HomeView from './pages/Home.vue'
import Landing from './pages/Landing.vue'
import Login from "./pages/Login.vue";
import RetroBoard from "./pages/RetroBoard.vue";
import Retrospective from "./pages/Retrospective.vue";
import ScrumPoker from "./pages/ScrumPoker.vue";
import Teams from "./pages/Teams.vue";
import Settings from "./pages/Settings.vue";
import WorkList from "./pages/WorkList.vue";
import CodeShare from "./pages/CodeShare.vue";
import TaskDetail from "./pages/TaskDetail.vue";
import Quiz from "./pages/Quiz.vue";
// Faz 1 - Yeni sayfalar
import OrganizationDashboard from "./pages/OrganizationDashboard.vue";
import ProjectDashboard from "./pages/ProjectDashboard.vue";
import UserProfile from "./pages/UserProfile.vue";
import AdminPanel from "./pages/AdminPanel.vue";
// Faz 6 - Dashboard & Raporlama
import Dashboard from "./pages/Dashboard.vue";

const routes = [{
    // Tanıtım (landing) sayfası — giriş yapmamış ziyaretçiler burayı görür.
    // Giriş yapmış kullanıcılar uygulama ana sayfasına (/home) yönlendirilir,
    // böylece mevcut push('/') çağrıları çalışmaya devam eder.
    path: '/',
    name: 'Landing',
    component: Landing,
    meta: {requiresAuth: false, hideNavbar: true},
    beforeEnter: () => {
        if (localStorage.getItem('jwt')) {
            return {path: '/home'}
        }
        return true
    }
}, {
    path: '/home',
    name: 'Home',
    component: HomeView,
    meta: {
        requiresAuth: true // Add meta field to indicate protected route
    }
}, {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: {requiresAuth: false}
}, {
    // Üye onboarding + şifre sıfırlama (e-postadaki token linki)
    path: '/set-password',
    name: 'SetPassword',
    component: () => import('./pages/SetPassword.vue'),
    meta: {requiresAuth: false}
}, {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('./pages/ForgotPassword.vue'),
    meta: {requiresAuth: false}
}, {
    path: '/retrospective',
    name: 'Retrospective',
    component: Retrospective,
    meta: {
        requiresAuth: true
    }
}, {
    path: '/retroBoard/:teamId/:boardId',
    name: 'RetroBoard',
    component: RetroBoard,
    props: true,
    meta: {
        requiresAuth: true // Add meta field to indicate protected route
    }
}, {
    path: '/scrumPoker/:teamId',
    name: 'ScrumPoker',
    component: ScrumPoker,
    props: true,
    meta: {
        requiresAuth: true // Add meta field to indicate protected route
    }
}, {
    path: '/teams',
    name: 'Teams',
    component: Teams,
    meta: {
        requiresAuth: true // Add meta field to indicate protected route}
    }
}, {
    path: '/settings',
    name: 'Settings',
    component: Settings,
    meta: {
        requiresAuth: true // Add meta field to indicate protected route
    },
}, {
    path: "/workList/:teamId",
    name: 'WorkList',
    component: WorkList,
    props: true,
    meta: {requiresAuth: true}
}, {
    path: '/codeShare/:teamId',
    name: 'CodeShare',
    component: CodeShare,
    props: true,
    meta: {requiresAuth: true}
}, {
    path: '/task/:taskId',
    name: 'TaskDetail',
    component: TaskDetail,
    props: true,
    meta: {requiresAuth: true}
}, {
    // Eski bildirim URL formatı için redirect
    path: '/teams/:teamId/tasks/:taskId',
    redirect: to => ({ path: `/task/${to.params.taskId}` }),
    meta: {requiresAuth: true}
}, {
    path: '/quiz/:teamId',
    name: 'Quiz',
    component: Quiz,
    props: true,
    meta: {requiresAuth: true}
}, {
    // Faz 1 - Organizasyon & Proje yönetimi
    path: '/organizations',
    name: 'OrganizationDashboard',
    component: OrganizationDashboard,
    meta: {requiresAuth: true}
}, {
    path: '/projects/:id',
    name: 'ProjectDashboard',
    component: ProjectDashboard,
    props: true,
    meta: {requiresAuth: true}
}, {
    path: '/profile',
    name: 'UserProfile',
    component: UserProfile,
    meta: {requiresAuth: true}
}, {
    path: '/admin',
    name: 'AdminPanel',
    component: AdminPanel,
    meta: {requiresAuth: true, requiresSuperAdmin: true}
}, {
    // Faz 6 - Dashboard & Raporlama
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: {requiresAuth: true}
}, {
    // Faz 7 - Docs modülü (projectId olmadan — proje seçim ekranı)
    path: '/docs',
    name: 'DocsHome',
    component: () => import('./pages/Docs.vue'),
    meta: {requiresAuth: true}
}, {
    path: '/projects/:projectId/docs',
    name: 'Docs',
    component: () => import('./pages/Docs.vue'),
    props: true,
    meta: {requiresAuth: true}
}, {
    path: '/projects/:projectId/docs/:spaceId',
    name: 'DocSpace',
    component: () => import('./pages/DocPage.vue'),
    props: true,
    meta: {requiresAuth: true}
}, {
    path: '/projects/:projectId/docs/:spaceId/pages/:pageId',
    name: 'DocPage',
    component: () => import('./pages/DocPage.vue'),
    props: true,
    meta: {requiresAuth: true}
}, {
    // Destek talepleri
    path: '/support',
    name: 'Support',
    component: () => import('./pages/SupportPage.vue'),
    meta: {requiresAuth: true}
}, {
    path: '/support/:ticketId',
    name: 'SupportTicketDetail',
    component: () => import('./pages/SupportTicketDetail.vue'),
    props: true,
    meta: {requiresAuth: true}
}, {
    // SEO blog — giriş gerektirmez, Landing gibi kendi header/footer'ını kullanır
    path: '/blog',
    name: 'Blog',
    component: () => import('./pages/Blog.vue'),
    meta: {requiresAuth: false, hideNavbar: true}
}, {
    path: '/blog/:slug',
    name: 'BlogPost',
    component: () => import('./pages/BlogPost.vue'),
    props: true,
    meta: {requiresAuth: false, hideNavbar: true}
}, {
    // Catch-all: tanımsız URL'leri ana sayfaya yönlendir
    path: '/:pathMatch(.*)*',
    redirect: '/'
}]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL), routes,
})

router.beforeEach((to, from, next) => {
    if (to.meta.requiresAuth) {
        // useAuth'un tuttuğu jwt key'ini oku ('jwt' — AuthApi ile tutarlı)
        const jwt = localStorage.getItem('jwt')
        if (!jwt) {
            next('/login')
            return
        }
        // Client-side kolaylık kontrolü — asıl yetki denetimi backend @PreAuthorize'da
        if (to.meta.requiresSuperAdmin) {
            let role = null
            try {
                role = JSON.parse(localStorage.getItem('userProfile') || 'null')?.systemRole
            } catch { /* bozuk profil → yetkisiz say */ }
            if (role !== 'SUPER_ADMIN') {
                next('/')
                return
            }
        }
        next()
    } else {
        next()
    }
});

export default router
