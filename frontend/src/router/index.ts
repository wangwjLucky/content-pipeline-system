import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('../views/login/Login.vue'),
    },
    {
      path: '/',
      component: MainLayout,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('../views/dashboard/Dashboard.vue') },
        { path: 'topics', component: () => import('../views/topic/TopicList.vue') },
        { path: 'topics/new', component: () => import('../views/topic/TopicEdit.vue') },
        { path: 'topics/:id/edit', component: () => import('../views/topic/TopicEdit.vue') },
        { path: 'topics/:id', redirect: (to: any) => `/topics/${to.params.id}/edit` },
        { path: 'tasks', component: () => import('../views/task/TaskList.vue') },
        { path: 'tasks/:id', component: () => import('../views/task/TaskDetail.vue') },
        { path: 'scripts', component: () => import('../views/script/ScriptList.vue') },
        { path: 'scripts/:id', component: () => import('../views/script/ScriptReview.vue') },
        { path: 'storyboards/:taskId', component: () => import('../views/storyboard/StoryboardEdit.vue') },
        { path: 'publish', component: () => import('../views/publish/PublishManage.vue') },
        { path: 'publish/calendar', component: () => import('../views/publish/PublishCalendar.vue') },
        { path: 'voice', component: () => import('../views/voice/VoiceManage.vue') },
        { path: 'edits', component: () => import('../views/edit/EditManage.vue') },
        { path: 'analytics', component: () => import('../views/analytics/Analytics.vue') },
        { path: 'templates', component: () => import('../views/template/TemplateList.vue') },
        { path: 'materials', component: () => import('../views/material/MaterialList.vue') },
        { path: 'settings', redirect: '/settings/models' },
        { path: 'settings/models', component: () => import('../views/settings/ModelConfig.vue') },
        { path: 'settings/platform-accounts', component: () => import('../views/settings/PlatformAccountList.vue') },
        { path: 'settings/roles', component: () => import('../views/settings/RoleList.vue') },
        { path: 'settings/users', component: () => import('../views/settings/UserList.vue') },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    return '/login'
  }
})

export default router