import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/chat' },
  { path: '/chat', name: 'chat', component: () => import('../views/ChatView.vue') },
  { path: '/zoo', name: 'zoo-list', component: () => import('../views/ZooListView.vue') },
  { path: '/zoo/:id', name: 'zoo-detail', component: () => import('../views/ZooDetailView.vue') },
  { path: '/admin', name: 'admin', component: () => import('../views/ZooAdminView.vue') }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
