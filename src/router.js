import {createMemoryHistory, createRouter} from 'vue-router'

import HomeView from './pages/Home.vue'
import Login from "./pages/Login.vue";
import RetroBoard from "./pages/RetroBoard.vue";
import ScrumPoker from "./pages/ScrumPoker.vue";
import Teams from "./pages/Teams.vue";
import Settings from "./pages/Settings.vue";
import WorkList from "./pages/WorkList.vue";
import {auth} from "./firebase/Firebase.js";

const routes = [
    {
        path: '/', component: HomeView, meta: {
            requiresAuth: true // Add meta field to indicate protected route
        }
    },
    {path: '/login', component: Login, meta: {requiresAuth: false}},
    {
        path: '/retroBoard/:teamId/:boardId', component: RetroBoard, props: true, meta: {
            requiresAuth: true // Add meta field to indicate protected route
        }
    },
    {
        path: '/scrumPoker/:teamId', component: ScrumPoker, props: true, meta: {
            requiresAuth: true // Add meta field to indicate protected route
        }
    },
    {
        path: '/teams', component: Teams, meta: {
            requiresAuth: true // Add meta field to indicate protected route}
        }
    },
    {
        path: '/settings', component: Settings, meta: {
            requiresAuth: true // Add meta field to indicate protected route
        },
    },
    {path:"/workList:teamId",component: WorkList,props:true,meta:{requiresAuth:true}}
]

const router = createRouter({
    history: createMemoryHistory(),
    routes,
})
router.beforeEach((to, from, next) => {
    if (to.meta.requiresAuth) {
        const user = localStorage.getItem("user");
        if (user) {
            // User is authenticated, proceed to the route
            next();
        } else {
            // User is not authenticated, redirect to login
            next('/login');
        }
    } else {
        // Non-protected route, allow access
        next();
    }
});
export default router