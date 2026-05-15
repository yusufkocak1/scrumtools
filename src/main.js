import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from "./router.js"
import 'mosha-vue-toastify/dist/style.css';
import '@material-tailwind/html/scripts/ripple.js';
import { vPermission } from './directives/v-permission.js'

createApp(App)
    .use(router)
    .directive('permission', vPermission)
    .mount('#app')
