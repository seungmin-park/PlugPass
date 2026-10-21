import './styles/base.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { initializePlatform } from './shared/platform/runtime'

initializePlatform()
const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
