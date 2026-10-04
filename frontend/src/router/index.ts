import { createRouter, createWebHashHistory } from 'vue-router'
import StationSearchView from '../features/search/views/StationSearchView.vue'
import PageNotFound from '../shared/ui/PageNotFound.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/stations' },
    { path: '/stations', name: 'stations', component: StationSearchView },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: PageNotFound },
  ],
})

export default router
