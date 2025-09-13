import {createRouter, createWebHistory} from 'vue-router'

import HomeView from './pages/Home.vue'
import Login from "./pages/Login.vue";
import RetroBoard from "./pages/RetroBoard.vue";
import ScrumPoker from "./pages/ScrumPoker.vue";
import Teams from "./pages/Teams.vue";
import Settings from "./pages/Settings.vue";
import WorkList from "./pages/WorkList.vue";
import SprintPage from "./components/work/SprintPage.vue";
import {authService} from "./firebase/AuthService.js";
import CodeShare from "./pages/CodeShare.vue";

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
    path: '/sprint/:sprintId',
    name: 'SprintPage',
    component: SprintPage,
    props: true,
    meta: {requiresAuth: true}
}, {
    path: '/codeShare/:teamId',
    name: 'CodeShare',
    component: CodeShare,
    props: true,
    meta: {requiresAuth: true}
}]

const router = createRouter({
    history: createWebHistory(), routes,
})
router.beforeEach((to, from, next) => {
    if (to.meta.requiresAuth) {
        const unsubscribe = authService.onAuthStateChanged((user) => {
            unsubscribe(); // Bir defaya mahsus dinlemeyi sonlandırıyoruz.
            if (user) {
                // Kullanıcı oturum açmış, route'a devam et.
                next();
            } else {
                // Kullanıcı oturum açmamış, login sayfasına yönlendir.
                next('/login');
            }
        });
    } else {
        // Non-protected route, allow access
        next();
    }
});
export default router