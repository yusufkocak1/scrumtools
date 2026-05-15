import {createRouter, createWebHistory} from 'vue-router'

import HomeView from './pages/Home.vue'
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
    path: '/',
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
    meta: {requiresAuth: true}
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
    // Catch-all: tanımsız URL'leri ana sayfaya yönlendir
    path: '/:pathMatch(.*)*',
    redirect: '/'
}]

const router = createRouter({
    history: createWebHistory(), routes,
})

router.beforeEach((to, from, next) => {
    if (to.meta.requiresAuth) {
        // useAuth'un tuttuğu jwt key'ini oku ('jwt' — AuthApi ile tutarlı)
        const jwt = localStorage.getItem('jwt')
        if (jwt) {
            next()
        } else {
            next('/login')
        }
    } else {
        next()
    }
});

export default router
