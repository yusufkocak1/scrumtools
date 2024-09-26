import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from "./router.js"
import 'mosha-vue-toastify/dist/style.css';
import '@material-tailwind/html/scripts/ripple.js';


createApp(App)
    .use(router)
    .mount('#app')
