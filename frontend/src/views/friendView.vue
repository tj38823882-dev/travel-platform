<template>
  <div class="friend-page-container">
    <div class="container py-5 mt-4">
      <!-- 頁面標題 -->
      <div class="row g-4 justify-content-center">
        <!-- 左側：功能區 -->
        <div class="col-lg-7 col-xl-8">

          <!-- 新增好友卡片 -->
          <div class="modern-card mb-4 animate-fade-up" style="animation-delay: 0.1s;">
            <div class="card-body p-4 p-md-5">
              <div class="d-flex align-items-center mb-4">
                <div class="icon-square bg-primary-subtle text-primary me-3">
                  <i class="bi bi-person-plus-fill fs-4"></i>
                </div>
                <h5 class="fw-bold mb-0">尋找新旅伴</h5>
              </div>

              <div class="search-wrapper">
                <div class="input-group input-group-lg search-input-group shadow-sm">
                  <span class="input-group-text bg-white border-0 ps-4">
                    <i class="bi bi-search text-primary"></i>
                  </span>
                  <input type="text" class="form-control border-0 px-3" placeholder="輸入使用者名稱來尋找好友..."
                    v-model="friendUsername" @keyup.enter="addFriend">
                  <button class="btn btn-primary px-4 px-md-5 fw-bold rounded-end-pill send-btn" type="button"
                    @click="addFriend">
                    發送申請 <i class="bi bi-send-fill ms-1 d-none d-md-inline"></i>
                  </button>
                </div>
              </div>

              <!-- 提示訊息 -->
              <transition name="fade">
                <div v-if="addMessage"
                  :class="['alert mt-4 d-flex align-items-center border-0 shadow-sm rounded-4', addMessageClass]"
                  role="alert">
                  <i class="bi"
                    :class="addMessageClass === 'alert-success' ? 'bi-check-circle-fill text-success' : 'bi-exclamation-triangle-fill text-danger'"></i>
                  <span class="ms-2 fw-medium">{{ addMessage }}</span>
                </div>
              </transition>
            </div>
          </div>

          <!-- 待處理申請區塊 -->
          <div class="modern-card animate-fade-up" style="animation-delay: 0.2s;">
            <div
              class="card-header bg-transparent border-0 pt-4 pb-2 px-4 px-md-5 d-flex justify-content-between align-items-center">
              <h5 class="fw-bold mb-0 text-dark d-flex align-items-center">
                <div class="icon-square-sm bg-warning-subtle text-warning me-2">
                  <i class="bi bi-bell-fill"></i>
                </div>
                好友申請
              </h5>
              <span v-if="requests.length > 0"
                class="badge rounded-pill bg-warning text-dark px-3 py-2 shadow-sm pulse-badge">
                {{ requests.length }} 筆待處理
              </span>
            </div>

            <div class="card-body p-0">
              <div v-if="requests.length === 0" class="empty-state py-5 text-center">
                <div class="empty-icon bg-light rounded-circle mx-auto mb-3">
                  <i class="bi bi-inbox text-muted fs-1"></i>
                </div>
                <h6 class="text-muted fw-bold">目前沒有任何申請</h6>
                <p class="text-secondary small mb-0">當有人加您為好友時，會顯示在這裡</p>
              </div>

              <ul v-else class="list-group list-group-flush custom-list">
                <li v-for="request in requests" :key="request.friendshipId"
                  class="list-group-item border-0 px-4 px-md-5 py-3 py-md-4 d-flex flex-column flex-sm-row justify-content-between align-items-sm-center request-item">

                  <div class="d-flex align-items-center mb-3 mb-sm-0">
                    <div class="avatar-circle avatar-lg bg-gradient-primary text-white shadow-sm me-3">
                      {{ request.requesterName.charAt(0).toUpperCase() }}
                    </div>
                    <div>
                      <h5 class="fw-bold mb-1 text-dark">{{ request.requesterName }}</h5>
                      <span class="badge bg-light text-secondary border">想要加您為好友</span>
                    </div>
                  </div>

                  <div class="action-buttons d-flex gap-2">
                    <button class="btn btn-success rounded-pill px-4 shadow-sm"
                      @click="acceptFriend(request.friendshipId)">
                      <i class="bi bi-check-lg me-1"></i> 接受
                    </button>
                    <button class="btn btn-light text-danger rounded-pill px-4 border"
                      @click="deleteRequest(request.friendshipId)">
                      拒絕
                    </button>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 右側：好友列表 -->
        <div class="col-lg-5 col-xl-4">
          <!-- 引用封裝好的好友列表元件 -->
          <FriendList ref="friendListRef" class="sticky-sidebar" style="animation-delay: 0.3s;" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '@/utils/request';
import FriendList from '@/components/FriendList.vue';

const friendUsername = ref('');
const requests = ref([]);
const addMessage = ref('');
const addMessageClass = ref('alert-info');
const friendListRef = ref(null); // 用來呼叫子元件的方法

// 1. 取得好友申請列表 (維持原樣)
const fetchRequests = async () => {
  try {
    const response = await request.get('/api/user/friend/requests');
    requests.value = response.data;
  } catch (error) {
    console.error('Fetch requests failed', error);
  }
};

// 3. 新增好友 (維持原樣)
const addFriend = async () => {
  if (!friendUsername.value) return;
  try {
    const response = await request.post('/api/user/friend/add', {
      receiverName: friendUsername.value
    });
    addMessage.value = response.data;
    addMessageClass.value = 'alert-success';
    friendUsername.value = '';
    fetchRequests();
  } catch (error) {
    addMessage.value = error.response?.data || '發送失敗';
    addMessageClass.value = 'alert-danger';
  }
};

// 4. 接受好友 (維持原樣)
const acceptFriend = async (friendshipId) => {
  try {
    await request.put(`/api/user/friend/accept/${friendshipId}`);
    requests.value = requests.value.filter(r => r.friendshipId !== friendshipId);
    // 🌟 通知子元件刷新好友列表
    friendListRef.value?.fetchFriends();
  } catch (error) {
    alert('接受失敗');
  }
};

// 5. 刪除/拒絕好友 (維持原樣)
const deleteRequest = async (friendshipId) => {
  if (!confirm('確定要解除好友關係嗎？')) return;
  try {
    await request.delete(`/api/user/friend/delete/${friendshipId}`);
    fetchRequests();
    addMessage.value = "已解除好友關係";
    addMessageClass.value = "alert-success";
  } catch (error) {
    console.error('Delete failed', error);
    alert('刪除失敗');
  }
};

onMounted(() => {
  fetchRequests();
});

</script>
<style scoped>
/* 背景與版面配置 */
.friend-page-container {
  background-color: #f4f7fe;
  font-family: 'Inter', 'Outfit', sans-serif;
}

/* 漸層標題 */
.gradient-text {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 現代卡片設計 */
.modern-card {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  overflow: hidden;
}

.modern-card:hover {
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.05);
}

.bg-gradient-light {
  background: linear-gradient(to right, #f8fafc, #ffffff);
}

/* 動畫與過渡 */
.animate-fade-up {
  opacity: 0;
  transform: translateY(20px);
  animation: fadeUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes fadeUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 搜尋框客製化 */
.search-input-group {
  border-radius: 50rem;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.search-input-group .input-group-text,
.search-input-group .form-control {
  border-radius: 50rem 0 0 50rem;
  background: transparent;
}

.search-input-group .form-control:focus {
  box-shadow: none;
}

.send-btn {
  border-radius: 0 50rem 50rem 0 !important;
  transition: all 0.3s;
}

.send-btn:hover {
  transform: translateX(2px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

/* 列表樣式 */
.custom-list .list-group-item {
  transition: all 0.2s ease;
  border-bottom: 1px solid rgba(0, 0, 0, 0.02) !important;
}

.custom-list .list-group-item:last-child {
  border-bottom: none !important;
}

.request-item:hover,
.friend-item:hover {
  background-color: #f8fafc !important;
}

/* 線上狀態圓點 */
.online-indicator {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 14px;
  height: 14px;
  background-color: #22c55e;
  border: 2px solid #ffffff;
  border-radius: 50%;
}

/* 側邊欄固定與滾動 */
.sticky-sidebar {
  top: 100px;
  position: sticky;
}

/* 按鈕樣式 */
.btn-icon {
  width: 36px;
  height: 36px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-icon:hover {
  transform: translateY(-2px);
  background-color: #ffffff !important;
}

/* 空狀態 */
.empty-icon {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 脈衝標籤 */
.pulse-badge {
  animation: pulseWarning 2s infinite;
}

@keyframes pulseWarning {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 193, 7, 0.4);
  }

  70% {
    box-shadow: 0 0 0 6px rgba(255, 193, 7, 0);
  }

  100% {
    box-shadow: 0 0 0 0 rgba(255, 193, 7, 0);
  }
}

/* 淡入動畫 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
</style>