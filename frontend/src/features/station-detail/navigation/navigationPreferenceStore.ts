import { defineStore } from 'pinia'
import { ref } from 'vue'
import { readNavigationPreference, saveNavigationPreference, type NavigationApp } from './navigationPreference'
export const useNavigationPreferenceStore = defineStore('navigation-preference', () => {
  const app = ref(readNavigationPreference())
  function selectApp(selected: NavigationApp): void {
    app.value = selected
    saveNavigationPreference(selected)
  }
  return { app, selectApp }
})
