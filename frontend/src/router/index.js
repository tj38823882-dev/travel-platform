import { createRouter, createWebHistory } from 'vue-router'
import { setupRouterGuard } from './permission';
import request from '@/utils/request';
import { useToastStore } from '@/stores/toastStore';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'ToTourList',
      component: () => import('../views/ToTourList.vue'),
    },
    {
      path: '/verify',
      name: 'Verify',
      component: () => import('../components/VerifyCode.vue'),
    },
    {
      path: '/CartView',
      name: 'CartView',
      component: () => import('../views/CartView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/loginView',
      name: 'loginView',
      component: () => import('../views/loginView.vue'),
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/TopUpView',
      name: 'TopUpView',
      component: () => import('../views/TopUpView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/Checkouts',
      name: 'Checkouts',
      component: () => import('../views/Checkouts.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/payment/confirm',
      name: 'PaymentConfirm',
      component: () => import('../views/PaymentConfirm.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/friend',
      name: 'friend',
      component: () => import('../views/friendView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/AdminUserList.vue'),
      meta: {
        requiresAuth: true,
        requiresAdmin: true
      }
    },
    {
      path: '/MessageBoard',
      name: 'MessageBoard',
      component: () => import('../views/MessageBoardView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/DayPlanner',
      name: 'DayPlanner',
      component: () => import('../views/DayPlanner.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/MessageBoard/post/:postId',
      name: 'SinglePost',
      component: () => import('../views/SinglePostView.vue')
    },
    {
      path: '/draw/:roomId',
      name: 'draw',
      component: () => import('../views/DrawView.vue'),
      beforeEnter: async (to, from, next) => {
        try {
          // 進入前先檢查房間是否存在
          const res = await request.get(`/api/user/draw/check/${to.params.roomId}`);
          if (res.data === true) {
            next(); // 房間存在，允許進入
          } else {
            const toastStore = useToastStore();
            toastStore.addToast('此畫布房間已不存在', 'error');
            // 如果是按「上一頁」回來的 (有來源頁面)，則取消導航 (停留在原頁面)
            if (from.name) {
              next(false);
            } else {
              next('/MessageBoard'); // 如果是直接貼網址，導回塗鴉牆
            }
          }
        } catch (error) {
          next('/MessageBoard');
        }
      }
    }
  ],
  
})

setupRouterGuard(router);

export default router