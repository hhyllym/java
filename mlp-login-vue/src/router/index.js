import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import DashboardView from '../views/DashboardView.vue'
import ShopView from '../views/ShopView.vue'
import MessageCenterView from '../views/MessageCenterView.vue'
import FortuneView from '../views/FortuneView.vue'
import LearnView from '../views/LearnView.vue'
import GameView from '../views/GameView.vue'
import FriendsView from '../views/FriendsView.vue'
import ChatView from '../views/ChatView.vue'
import { getSession } from '../auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: { name: 'home' } },
    { path: '/home', name: 'home', component: HomeView, meta: { requiresAuth: true } },
    { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true } },
    { path: '/shop', name: 'shop', component: ShopView, meta: { requiresAuth: true } },
    { path: '/message', name: 'message', component: MessageCenterView, meta: { requiresAuth: true } },
    { path: '/fortune', name: 'fortune', component: FortuneView, meta: { requiresAuth: true } },
    { path: '/learn', name: 'learn', component: LearnView, meta: { requiresAuth: true } },
    { path: '/game/:id', name: 'game', component: GameView, meta: { requiresAuth: true } },
    { path: '/friends', name: 'friends', component: FriendsView, meta: { requiresAuth: true } },
    { path: '/chat/:id', name: 'chat', component: ChatView, meta: { requiresAuth: true } },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView }
  ]
})

router.beforeEach((to) => {
  const session = getSession()
  if (to.meta.requiresAuth && !session) return { name: 'login' }
  // 已登录的人访问登录/注册页，直接送去首页
  if ((to.name === 'login' || to.name === 'register') && session) return { name: 'home' }
  return true
})

export default router
