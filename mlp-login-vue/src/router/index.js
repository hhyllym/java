import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import DashboardView from '../views/DashboardView.vue'
import ShopView from '../views/ShopView.vue'
import PetView from '../views/PetView.vue'
import MessageCenterView from '../views/MessageCenterView.vue'
import FortuneView from '../views/FortuneView.vue'
import LearnView from '../views/LearnView.vue'
import GameView from '../views/GameView.vue'
import FriendsView from '../views/FriendsView.vue'
import ChatView from '../views/ChatView.vue'
import AdminLoginView from '../views/AdminLoginView.vue'
import AdminView from '../views/AdminView.vue'
import GameHubView from '../views/GameHubView.vue'
import EarnView from '../views/EarnView.vue'
import DiaryView from '../views/DiaryView.vue'
import { getSession } from '../auth'
import { getAdminSession } from '../adminAuth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: { name: 'home' } },
    { path: '/home', name: 'home', component: HomeView, meta: { requiresAuth: true } },
    { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true } },
    { path: '/earn', name: 'earn', component: EarnView, meta: { requiresAuth: true } },
    { path: '/shop', name: 'shop', component: ShopView, meta: { requiresAuth: true, wide: true } },
    { path: '/pet', name: 'pet', component: PetView, meta: { requiresAuth: true } },
    { path: '/message', name: 'message', component: MessageCenterView, meta: { requiresAuth: true } },
    { path: '/fortune', name: 'fortune', component: FortuneView, meta: { requiresAuth: true } },
    { path: '/learn', name: 'learn', component: LearnView, meta: { requiresAuth: true } },
    { path: '/games', name: 'games', component: GameHubView, meta: { requiresAuth: true } },
    // 旧「探险」地址留个重定向，老链接/书签不会 404
    { path: '/adventure', redirect: { name: 'games' } },
    { path: '/diary', name: 'diary', component: DiaryView, meta: { requiresAuth: true } },
    { path: '/game/:id', name: 'game', component: GameView, meta: { requiresAuth: true, mp: true } },
    { path: '/friends', name: 'friends', component: FriendsView, meta: { requiresAuth: true } },
    { path: '/chat/:id', name: 'chat', component: ChatView, meta: { requiresAuth: true } },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/admin/login', name: 'adminLogin', component: AdminLoginView },
    { path: '/admin', name: 'admin', component: AdminView, meta: { requiresAdmin: true, admin: true } }
  ]
})

router.beforeEach((to) => {
  const session = getSession()
  const admin = getAdminSession()
  // 未登录时把原地址带到登录页：四人联机的邀请链接 ?room=XXXX 登录后还能跳回房间
  if (to.meta.requiresAuth && !session) return { name: 'login', query: { redirect: to.fullPath } }
  // 已登录的人访问登录/注册页，直接送去首页
  if ((to.name === 'login' || to.name === 'register') && session) return { name: 'home' }
  // 后台管理页：必须持有管理员令牌
  if (to.meta.requiresAdmin && !admin) return { name: 'adminLogin' }
  // 已是管理员又跑去后台登录页，直接进后台
  if (to.name === 'adminLogin' && admin) return { name: 'admin' }
  return true
})

export default router
