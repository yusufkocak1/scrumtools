import {createRouter, createWebHistory} from 'vue-router'

import HomeView from './pages/Home.vue'
import Login from "./pages/Login.vue";
import RetroBoard from "./pages/RetroBoard.vue";
import Retrospective from "./pages/Retrospective.vue";
import ScrumPoker from "./pages/ScrumPoker.vue";
import Teams from "./pages/Teams.vue";
import Settings from "./pages/Settings.vue";
import WorkList from "./pages/WorkList.vue";
import SprintPage from "./components/work/SprintPage.vue";
import {authService} from "./firebase/AuthService.js";
import {isTeamMember} from "./firebase/TeamService.js";
import {createToast} from "mosha-vue-toastify";
import CodeShare from "./pages/CodeShare.vue";
import TaskDetail from "./pages/TaskDetail.vue";

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
    path: '/team/:teamId/sprint/:sprintId',
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
}, {
    path: '/task/:taskId',
    name: 'TaskDetail',
    component: TaskDetail,
    props: true,
    meta: {requiresAuth: true}
}]

const router = createRouter({
    history: createWebHistory(), routes,
})
const getCurrentUser = () => new Promise((resolve) => {
    const unsubscribe = authService.onAuthStateChanged((user) => {
        unsubscribe(); // Bir defaya mahsus dinlemeyi sonlandırıyoruz.
        resolve(user);
    });
});

router.beforeEach(async (to, from, next) => {
    if (!to.meta.requiresAuth) {
        // Non-protected route, allow access
        return next();
    }

    const user = await getCurrentUser();
    if (!user) {
        // Kullanıcı oturum açmamış, login sayfasına yönlendir.
        return next('/login');
    }

    // Takım bazlı sayfalarda üyelik kontrolü:
    // paylaşılan linkle gelen kullanıcı takım üyesi değilse içeri alma.
    const teamId = to.params.teamId;
    if (teamId) {
        const hasAccess = await isTeamMember(teamId, user.email);
        if (!hasAccess) {
            createToast('Bu takıma erişim yetkiniz yok', {type: 'danger', position: 'top-center'});
            return next('/teams');
        }
    }

    next();
});
export default router
