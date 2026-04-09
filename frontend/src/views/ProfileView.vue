<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import request from '@/utils/request';
import { useAuthStore } from '@/stores/authStore';
import { useToastStore } from '@/stores/toastStore';

// 引入拆分後的 Components
import ProfileInfoTab from '@/components/profile/ProfileInfoTab.vue';
import MyPlansTab from '@/components/profile/MyPlansTab.vue';
import PurchasedTripsTab from '@/components/profile/PurchasedTripsTab.vue';
import TransactionHistoryTab from '@/components/profile/TransactionHistoryTab.vue';
import PublishModal from '@/components/profile/PublishModal.vue';

// 使用 Router
const router = useRouter();
const route = useRoute();

// 使用 Pinia 管理狀態
const authStore = useAuthStore();
const toastStore = useToastStore();
// 響應式變數：儲存使用者資料
const user = ref(null);
// 載入狀態：控制 Spinner 顯示
const loading = ref(true);
// 當前選中的分頁 (profile/trips/settings)
const activeTab = ref('profile');

/**
 * 從後端 API 取得當前登入使用者的詳細資料
 * 使用 AuthController 中的 /whoami 端點
 */
const fetchUserProfile = async (silent = false) => {
  try {
    if (!silent) loading.value = true;
    const res = await request.post('/api/user/whoami');
    user.value = res.data;
  } catch (err) {
    console.error('獲取個人資料失敗:', err);
  } finally {
    if (!silent) loading.value = false;
  }
};

// 交易紀錄數據
const transactions = ref([]);
// 使用者行程數據
const trips = ref([]);
// 好友列表
const friends = ref([]);

/**
 * 從後端 API 取得該使用者的點數交易紀錄
 */
const fetchTransactions = async () => {
  try {
    const res = await request.get('/api/user/payment/history');
    transactions.value = res.data;
  } catch (err) {
    console.error('獲取交易紀錄失敗:', err);
  }
};

/**
 * 從後端 API 取得該使用者的行程列表
 */
const fetchTrips = async () => {
  try {
    const res = await request.get('/api/user/trips');
    trips.value = res.data;
  } catch (err) {
    console.error('獲取行程失敗:', err);
  }
};

/**
 * 編輯行程：跳轉至 DayPlanner 並帶入行程資料
 */
const handleEdit = (trip) => {
  if (trip.status === 'published') {
    toastStore.addToast('此行程已上架販售，暫不開放編輯以確保商品資訊一致性。', 'info', '商品鎖定');
    return;
  }
  router.push({
    name: 'DayPlanner',
    query: { id: trip.id }
  });
};

// 上架行程的表單與 Modal 狀態
const showPublishModal = ref(false);
const publishForm = ref({
  tripId: null,
  title: '',
  description: '',
  categoryId: 1, // 預設分類
  price: 0
});


/**
 * 打開上架 Modal，並帶入行程預設資料
 */
const handlePublish = (trip) => {
  publishForm.value.tripId = trip.id;
  publishForm.value.title = trip.title || '';
  publishForm.value.description = '';
  publishForm.value.categoryId = 1;
  publishForm.value.price = 0;

  showPublishModal.value = true;
};

/**
 * 關閉上架 Modal
 */
const closePublishModal = () => {
  showPublishModal.value = false;
};

/**
 * 送出上架資料
 */
const submitPublish = async () => {
  try {
    const res = await request.post('/api/public/itineraries', publishForm.value);
    toastStore.addToast(`上架成功！即將出售：${res.data.title}，價格：${res.data.price} 點`, 'success', '上架成功');
    closePublishModal();

    // --- 實時刷新 ---
    await Promise.all([
      fetchTrips(),           // 刷新「個人規劃」列表
      fetchpoints(),          // 刷新點數 (全域+本地)
      fetchTransactions()     // 刷新交易紀錄列表
    ]);
  } catch (err) {
    console.error('上架失敗:', err);
    toastStore.addToast('上架時發生錯誤，請稍後再試。', 'danger', '系統錯誤');
  }
};

/**
 * 格式化日期
 */
const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 統計數字（從現有資料計算）
const statTripCount = computed(() => trips.value.length);
const statFootprintCount = computed(() =>
  trips.value.reduce((total, trip) => {
    return total + (trip.tripDays || []).reduce((dayTotal, day) => {
      return dayTotal + (day.stops || []).length;
    }, 0);
  }, 0)
);
const statFriendCount = computed(() => friends.value.length);

/**
 * 從後端 API 取得好友列表（用於統計好友數量）
 */
const fetchFriends = async () => {
  try {
    const res = await request.get('/api/user/list');
    friends.value = res.data;
  } catch (err) {
    console.error('獲取好友列表失敗:', err);
  }
};

// 元件掛載後立即執行資料抓取
onMounted(() => {
  fetchUserProfile();
  fetchTransactions();
  fetchTrips();
  fetchFriends();
  fetchpoints();
  if (route.query.tab) activeTab.value = route.query.tab

});
// 定義側邊欄功能選項與圖標 (Bootstrap Icons)
const tabs = [
  { id: 'profile', name: '個人資料', icon: 'bi-person-badge' },
  { id: 'trips', name: '個人規劃 (可上架)', icon: 'bi-map' },
  { id: 'purchased', name: '已解鎖/購買行程', icon: 'bi-bag-check' },
  { id: 'checkouts', name: '錢包與紀錄', icon: 'bi-wallet2' }
];

// 已購買的行程與上架商品的響應式變數 (未來從後端取得)
const purchasedTrips = ref([]);
const publishedItems = ref([]);

const fetchPurchasedTrips = async () => {
  try {
    const res = await request.get('/api/public/itineraries/purchased');
    purchasedTrips.value = res.data;
  } catch (err) {
    console.error('獲取購買行程失敗:', err);
  }
};

const fetchPublishedItems = async () => {
  // TODO: 串接後端 API 取得上架商品 (例如: GET /api/user/itineraries)
};

/**
 * 重新整理使用者點數
 */
/**
 * 重新整理使用者點數 (同步 Pinia 全域狀態與本地顯示)
 */
const fetchpoints = async () => {
  // 1. 先讓全域 Store 去後端同步資料
  await authStore.fetchUserInfo();
  // 2. 將 Store 最新的點數同步回本地 user ref
  if (user.value) {
    user.value.points = authStore.points;
  }
};
</script>

<template>
  <div class="profile-page h-100">
    <!-- 數據讀取中的遮罩層 -->
    <div v-if="loading" class="loading-overlay">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <!-- 資料抓取成功且使用者已登入 -->
    <div v-else-if="user" class="container py-5 animate-fade-in">
      <div class="row g-4">

        <!-- 左側側邊欄：使用者基本簡介與導航按鈕 -->
        <div class="col-lg-4">
          <div class="glass-panel profile-sidebar p-4 sticky-top">
            <div class="text-center mb-4">
              <!-- 大頭貼區域：使用 DiceBear 隨機生成頭像 -->
              <div class="avatar-wrapper mb-3">
                <img :src="user.profilePictureUrl" alt="Avatar" class="profile-avatar shadow-lg">
                <button class="edit-avatar-btn" title="更換頭像">
                  <i class="bi bi-camera-fill"></i>
                </button>
              </div>
              <h3 class="fw-bold mb-1">{{ user.username }}</h3>
              <p class="text-muted small mb-2">加入時間：{{ formatDate(user.createdAt) }}</p>
              <div class="d-flex align-items-center justify-content-center gap-2 mb-3">
              </div>
              <div class="badge bg-primary-subtle text-primary border border-primary-subtle px-3 py-2 rounded-pill">
                {{ user.role?.roleName || 'Explorer' }}
              </div>
            </div>

            <!-- 數據統計區 -->
            <div class="stats-row d-flex justify-content-around py-3 mb-4 border-top border-bottom">
              <div class="stat-item text-center">
                <div class="fw-bold fs-5">{{ statTripCount }}</div>
                <div class="text-muted extra-small">行程</div>
              </div>
              <div class="stat-item text-center">
                <div class="fw-bold fs-5">{{ statFriendCount }}</div>
                <div class="text-muted extra-small">好友</div>
              </div>
            </div>

            <!-- 分頁導航按鈕群組 -->
            <div class="nav flex-column nav-pills custom-nav-pills">
              <button v-for="tab in tabs" :key="tab.id" class="nav-link text-start mb-2"
                :class="{ active: activeTab === tab.id }" @click="activeTab = tab.id;
                if (tab.id === 'checkouts') fetchTransactions();
                if (tab.id === 'trips') fetchTrips();
                if (tab.id === 'purchased') fetchPurchasedTrips();
                if (tab.id === 'published') fetchPublishedItems();">
                <i :class="['bi', tab.icon, 'me-3']"></i>
                {{ tab.name }}
              </button>
            </div>
          </div>
        </div>

        <!-- 右側主內容區 -->
        <div class="col-lg-8">
          <div class="glass-panel content-area p-4 min-vh-75">

            <!-- 分頁 1: 基本資料 (Profile) -->
            <ProfileInfoTab v-if="activeTab === 'profile'" :user="user" />

            <!-- 分頁 2: 我的行程 (Trips) -->
            <MyPlansTab v-if="activeTab === 'trips'" :trips="trips" @edit="handleEdit" @publish="handlePublish" />

            <!-- 分頁 3: 已購買行程 (Purchased) -->
            <PurchasedTripsTab v-if="activeTab === 'purchased'" :purchasedTrips="purchasedTrips" />

            <!-- 分頁 4: 訂單紀錄 (Checkouts) -->
            <TransactionHistoryTab v-if="activeTab === 'checkouts'" :transactions="transactions" />
          </div>
        </div>
      </div>
    </div>

    <!-- 未登入狀態的提示 -->
    <div v-else class="container py-5 text-center">
      <div class="glass-panel p-5 d-inline-block">
        <h3>尚未登入</h3>
        <p class="text-muted">請先登入後再查看個人頁面</p>
        <RouterLink to="/loginView" class="btn btn-primary rounded-pill mt-3">前往登入</RouterLink>
      </div>
    </div>

    <!-- 上架商品 Modal -->
    <PublishModal :showPublishModal="showPublishModal" :publishForm="publishForm" @close="closePublishModal"
      @submitPublish="submitPublish" />
  </div>
</template>

<style scoped>
.profile-page {
  background: radial-gradient(circle at 10% 20%, rgba(99, 102, 241, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 90% 80%, rgba(168, 85, 247, 0.03) 0%, transparent 50%);
  min-height: 90vh;
}

.profile-sidebar {
  border-radius: var(--radius-lg);
  top: 100px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
}

.profile-avatar {
  width: 120px;
  height: 120px;
  border-radius: 40px;
  object-fit: cover;
  background: white;
  padding: 5px;
  border: 4px solid var(--glass-border);
}

.edit-avatar-btn {
  position: absolute;
  bottom: 0;
  right: -5px;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: var(--primary-gradient);
  color: white;
  border: 2px solid white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  transition: var(--transition);
}

.edit-avatar-btn:hover {
  transform: scale(1.1);
}

.extra-small {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.custom-nav-pills .nav-link {
  border-radius: 12px;
  color: var(--text-muted);
  font-weight: 600;
  transition: var(--transition);
}

.custom-nav-pills .nav-link:hover {
  background: rgba(99, 102, 241, 0.05);
  color: var(--primary);
}

.custom-nav-pills .nav-link.active {
  background: var(--primary-gradient);
  color: white;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.2);
}

.content-area {
  border-radius: var(--radius-lg);
}



.loading-overlay {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 60vh;
}

.min-vh-75 {
  min-height: 75vh;
}

@media (max-width: 991.98px) {
  .profile-sidebar {
    position: static;
    margin-bottom: 2rem;
  }
}
</style>
