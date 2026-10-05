import { createRouter, createWebHashHistory } from 'vue-router'
import StationSearchView from '../features/search/views/StationSearchView.vue'
import StationDetailView from '../features/station-detail/views/StationDetailView.vue'
import AlternativeStationsView from '../features/recommendation/views/AlternativeStationsView.vue'
import PageNotFound from '../shared/ui/PageNotFound.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/stations' },
    { path: '/stations', name: 'stations', component: StationSearchView },
    { path: '/stations/:stationId', name: 'station-detail', component: StationDetailView },
    { path: '/alternatives', name: 'alternatives', component: AlternativeStationsView },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: PageNotFound },
  ],
})

export default router
