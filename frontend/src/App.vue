<script setup>
import { onMounted, ref, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import ToastContainer from './components/ToastContainer.vue';
import NotificationList from './components/NotificationList.vue';
import { useCartStore } from './stores/cartStore';
import { useAuthStore } from './stores/authStore';
import { useNotificationStore } from './stores/notificationStore';
import { setupRouterGuard } from './router/permission';
import { Client } from '@stomp/stompjs';
import { wsUrl } from './config/appConfig.js';

// 初始化 Store
const cartStore = useCartStore();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();
const router = useRouter();

// 監聽登入狀態，登入成功就抓購物車
watch(() => authStore.isLoggedIn, (newVal) => {
  if (newVal) {
    cartStore.fetchCart();
    cartStore.fetchPurchasedIds(); // 🌟 同步抓取已購買清單
  }
}, { immediate: true });
// 通知相關狀態
const showNotifications = ref(false);
let stompClient = null;

// WebSocket 連線邏輯
const connectWebSocket = () => {
  // 如果已經連線中，就不重複連線
  if (stompClient && stompClient.active) return;

  stompClient = new Client({
    brokerURL: wsUrl, // 後端 WebSocket 端點
    reconnectDelay: 5000, // 斷線後 5 秒重連
    onConnect: () => {
      // 訂閱使用者的專屬通知頻道
      stompClient.subscribe('/user/queue/notifications', (message) => {
        if (message.body) {
          const notification = JSON.parse(message.body);
          // 將新通知加入 Pinia Store
          notificationStore.handleNewNotification(notification);
          // 您可以在這裡加入提示音效
        }
      });
    },
    onStompError: (frame) => {
      console.error('WebSocket 錯誤:', frame);
    }
  });
  stompClient.activate();
};

// 切換通知選單顯示
const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value;
};

// 點擊外部關閉選單
const closeDropdown = (e) => {
  const wrapper = document.querySelector('.notification-dropdown-wrapper');
  if (wrapper && !wrapper.contains(e.target)) {
    showNotifications.value = false;
  }
};

// 處理登出
const handleLogout = async () => {
  if (stompClient) stompClient.deactivate(); // 登出時斷開 WebSocket
  await authStore.logout();
  cartStore.clearCart(); // 登出時清空
  router.push('/loginView');
};

// 處理儲值 (模擬)
const handleTopUp = (amount) => {
  authStore.addPoints(amount);
};

onMounted(() => {
  // authStore 在初始化時已自動呼叫 fetchUserInfo()，此處無需重複呼叫
  // 如果已登入，則初始化通知與 WebSocket
  if (authStore.isLoggedIn) {
    notificationStore.fetchUnreadCount();
    connectWebSocket();
  }
  window.addEventListener('click', closeDropdown);
});

onUnmounted(() => {
  if (stompClient) stompClient.deactivate();
  window.removeEventListener('click', closeDropdown);
});

// 監聽登入狀態：登入後自動連線，登出後自動斷線
watch(() => authStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn) {
    notificationStore.fetchUnreadCount();
    connectWebSocket();
  } else {
    if (stompClient) stompClient.deactivate();
  }
});
</script>

<template>
  <div class="app-wrapper">
    <!-- 全域通知容器 -->
    <ToastContainer />

    <!-- 頂部導覽列：包含毛玻璃效果與響應式選單 -->
    <nav class="navbar navbar-expand-lg navbar-dark sticky-top glass-nav">
      <div class="container">
        <RouterLink class="navbar-brand d-flex align-items-center" :to="authStore.isAdmin ? '/admin' : '/'">
          <div class="logo-box me-2">
            <img src="@/assets/aa.png" alt="TravelVibe Logo" class="logo-img">
          </div>
          <div class="brand-info d-none d-sm-block">
            <span class="brand-text">TravelVibe</span>
          </div>
        </RouterLink>

        <!-- 行動裝置選單按鈕 -->
        <button class="navbar-toggler border-0 shadow-none" type="button" data-bs-toggle="collapse"
          data-bs-target="#navbarNav">
          <span class="navbar-toggler-icon"></span>
        </button>

        <!-- 導覽連結與購物車按鈕 -->
        <!-- 導覽連結與系統功能 -->
        <div class="collapse navbar-collapse" id="navbarNav">
          <!-- 左側：主要導覽 -->
          <ul class="navbar-nav me-auto mb-2 mb-lg-0 align-items-center ms-lg-4">
            <li v-if="!authStore.isAdmin" class="nav-item">
              <RouterLink class="nav-link d-flex align-items-center px-3" to="/" active-class="active">
                <i class="bi bi-house-door me-2 fs-5"></i>首頁
              </RouterLink>
            </li>
            <li class="nav-item">
              <RouterLink class="nav-link d-flex align-items-center px-3" to="/MessageBoard" active-class="active">
                <i class="bi bi-chat-square-text me-2 fs-5"></i>動態牆
              </RouterLink>
            </li>
          </ul>

          <!-- 右側：功能清單與會員區 -->
          <ul class="navbar-nav ms-auto align-items-center gap-2 gap-lg-3">
            <!-- 管理員專區 -->
            <li v-if="authStore.isAdmin" class="nav-item">
              <RouterLink class="nav-link feature-btn glass-panel px-3 py-2" to="/admin" active-class="active">
                <i class="bi bi-shield-lock me-2 text-info"></i>管理員介面
              </RouterLink>
            </li>

            <!-- 通知鈴鐺 (登入後顯示) -->
            <li v-if="authStore.isLoggedIn && !authStore.isAdmin" class="nav-item">
              <div class="notification-dropdown-wrapper position-relative">
                <button class="nav-link feature-btn glass-panel px-3 py-2 border-0" @click.stop="toggleNotifications">
                  <i class="bi bi-bell-fill me-2" :class="{ 'text-warning': notificationStore.unreadCount > 0 }"></i>
                  <span v-if="notificationStore.unreadCount > 0" class="cart-badge">
                    {{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}
                  </span>
                </button>
                <!-- 下拉選單 -->
                <Transition name="dropdown">
                  <div v-if="showNotifications" class="notification-dropdown shadow-lg rounded-4">
                    <NotificationList @close="showNotifications = false" />
                  </div>
                </Transition>
              </div>
            </li>

            <!-- 好友系統 -->
            <li v-if="authStore.isLoggedIn && !authStore.isAdmin" class="nav-item">
              <RouterLink class="nav-link feature-btn glass-panel px-3 py-2" to="/friend" active-class="active">
                <i class="bi bi-people-fill me-2 text-primary"></i>好友
              </RouterLink>
            </li>



            <!-- 購物車 -->
            <li v-if="!authStore.isAdmin" class="nav-item">
              <RouterLink class="nav-link feature-btn glass-panel px-3 py-2 position-relative" to="/CartView"
                active-class="active">
                <i class="bi bi-cart3 me-2 text-success"></i>購物車
                <span v-if="cartStore?.cartItems.length > 0" class="cart-badge">
                  {{ cartStore.cartItems.length }}
                </span>
              </RouterLink>
            </li>

            <!-- 點數資訊 (登入後顯示) -->
            <li v-if="authStore.isLoggedIn && !authStore.isAdmin" class="nav-item dropdown">
              <a class="nav-link feature-btn glass-panel px-3 py-2 d-flex align-items-center" href="#" role="button"
                data-bs-toggle="dropdown" aria-expanded="false">
                <i class="bi bi-coin text-warning me-2 fs-5"></i>
                <span class="points-value">{{ authStore.points }}</span>
                <span class="ms-1 small text-white-50">Pts</span>
              </a>
              <ul class="dropdown-menu dropdown-menu-end shadow-lg border-0 animate-fade-in custom-dropdown mt-2">
                <li>
                  <RouterLink class="dropdown-item d-flex align-items-center py-2" to="/TopUpView">
                    <div class="icon-circle-sm bg-warning-subtle text-warning me-3">
                      <i class="bi bi-plus-lg"></i>
                    </div>
                    <span class="fw-bold">前往儲值</span>
                  </RouterLink>
                </li>
              </ul>
            </li>

            <!-- 會員功能 (登入後) -->
            <li v-if="authStore.isLoggedIn" class="nav-item dropdown px-2">
              <a class="nav-link user-profile-btn d-flex align-items-center" href="#" role="button"
                data-bs-toggle="dropdown" aria-expanded="false">
                <div class="user-avatar shadow-sm" :class="{ 'text-bg-light': !authStore.profilePictureUrl }">
                  <img v-if="authStore.profilePictureUrl" :src="authStore.profilePictureUrl" alt="Avatar"
                    class="avatar-img">
                  <i v-else class="bi bi-person-fill"></i>
                </div>
              </a>
              <ul class="dropdown-menu dropdown-menu-end shadow-lg border-0 animate-fade-in custom-dropdown mt-2">
                <li class="px-3 py-2 border-bottom mb-1 bg-light bg-opacity-50">
                  <span class="d-block fw-bold text-dark fs-6">我的帳號</span>
                </li>
                <li>
                  <RouterLink class="dropdown-item py-2 d-flex align-items-center" to="/profile">
                    <i class="bi bi-person-circle me-3 text-secondary fs-5"></i>個人頁面
                  </RouterLink>
                </li>
                <li>
                  <hr class="dropdown-divider my-1">
                </li>
                <li>
                  <button class="dropdown-item py-2 d-flex align-items-center text-danger hover-danger"
                    @click="handleLogout">
                    <i class="bi bi-box-arrow-right me-3 fs-5"></i>登出帳號
                  </button>
                </li>
              </ul>
            </li>

            <!-- 登入按鈕 (未登入) -->
            <li v-else class="nav-item ms-2">
              <RouterLink class="btn btn-light rounded-pill px-4 py-2 fw-bold login-btn shadow-sm" to="/loginView">
                <i class="bi bi-box-arrow-in-right me-2"></i>登入
              </RouterLink>
            </li>
          </ul>
        </div>
      </div>
    </nav>


    <!-- 主內容區：包含路由切換動畫 -->
    <main class="main-content">
      <RouterView v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </RouterView>
    </main>

    <!-- 頁尾 -->
    <footer class="py-5 bg-white border-top mt-auto">
      <div class="container text-center">
        <p class="text-muted mb-0">© 2026 TravelVibe Platform. Crafted for better travel experiences.</p>
      </div>
    </footer>
  </div>
</template>

<style>
/* --- 全域頁面進入/離開動畫 --- */
.logo-img {
  width: 32px;
  /* 根據你的需求調整大小 */
  height: auto;
  display: block;
}

.app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>

<style scoped>
/* --- 導覽列毛玻璃效果 --- */
.glass-nav {
  background: rgba(99, 102, 241, 0.9) !important;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding: 0.8rem 0;
}

/* --- LOGO 容器樣式 --- */
.logo-box {
  width: 40px;
  height: 40px;
  background: white;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  font-size: 1.25rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.brand-text {
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  font-size: 1.5rem;
  letter-spacing: -0.5px;
}

.nav-link {
  font-family: 'Outfit', sans-serif;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8) !important;
  transition: var(--transition);
}

.nav-link:hover,
.nav-link.active {
  color: white !important;
}

.cursor-pointer {
  cursor: pointer;
}

/* --- 特殊功能按鈕 (購物車、好友、點數等) --- */
.feature-btn {
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  border-radius: 50rem !important;
  color: white !important;
  display: flex;
  align-items: center;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1) !important;
  font-weight: 600;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.feature-btn:hover {
  background: rgba(255, 255, 255, 0.25) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.15);
  border-color: rgba(255, 255, 255, 0.4) !important;
}

/* --- 購物車數量紅標 --- */
.cart-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ff4757;
  color: white;
  font-size: 0.75rem;
  font-weight: 800;
  min-width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  border: 2px solid rgba(99, 102, 241, 0.9);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  animation: pulseBadge 2s infinite;
}

@keyframes pulseBadge {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 71, 87, 0.7);
  }

  70% {
    box-shadow: 0 0 0 6px rgba(255, 71, 87, 0);
  }

  100% {
    box-shadow: 0 0 0 0 rgba(255, 71, 87, 0);
  }
}

.points-value {
  font-family: 'Outfit', sans-serif;
  font-weight: 800;
  color: #ffca2c;
  letter-spacing: 0.5px;
}

/* --- 會員頭像按鈕 --- */
.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
  border: 2px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  color: var(--primary);
  background-color: #f8f9fa;
  overflow: hidden;
  /* 確保圖片被裁切成圓形 */
}

.user-profile-btn {
  padding: 0 !important;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  /* 保持圖片比例並填滿容器 */
}

.user-profile-btn:hover .user-avatar {
  transform: scale(1.08) translateY(-2px);
  border-color: white;
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.4);
}

/* --- 登入按鈕 --- */
.login-btn {
  color: var(--primary) !important;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(255, 255, 255, 0.3) !important;
  background-color: #f8f9fa !important;
}

/* --- 下拉選單客製化 --- */
.custom-dropdown {
  min-width: 220px;
  border-radius: 16px !important;
  padding: 0.5rem 0 !important;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.05) !important;
}

.icon-circle-sm {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 下拉選單動畫與樣式 */
.dropdown-menu {
  min-width: 180px;
  border-radius: 12px;
  margin-top: 10px;
  animation: dropdownFadeIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes dropdownFadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dropdown-item {
  color: var(--text-main);
  font-weight: 600;
  transition: all 0.2s ease;
  padding: 0.6rem 1rem;
}

.dropdown-item:hover {
  background-color: #f8fafc;
  color: var(--primary);
  transform: translateX(4px);
}

.hover-danger:hover {
  color: #dc3545 !important;
  background-color: #fff5f5;
  transform: translateX(4px);
}

.dropdown-item:active {
  background-color: #eff6ff;
}

.icon-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
}

.letter-spacing-1 {
  letter-spacing: 1px;
}

.hover-lift:hover {
  transform: translateY(-2px);
  background-color: white !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

/* --- 通知選單樣式 --- */
.notification-dropdown {
  position: absolute;
  top: 55px;
  right: 0;
  z-index: 1050;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: white;
  overflow: hidden;
}

@media (max-width: 991.98px) {
  .notification-dropdown {
    position: fixed;
    top: 70px;
    left: 10px;
    right: 10px;
  }
}

.main-content {
  flex: 1;
}
</style>
