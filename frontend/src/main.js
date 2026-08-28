import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import './style.css'

const routes = [
  { path: '/', component: () => import('./views/Home.vue') },
  { path: '/explore', component: () => import('./views/Explore.vue') },
  { path: '/merchant/:id', component: () => import('./views/Merchant.vue') },
  { path: '/orders', component: () => import('./views/Orders.vue') },
  { path: '/health', component: () => import('./views/Health.vue') }
]
createApp(App).use(createRouter({ history: createWebHistory(), routes })).mount('#app')
