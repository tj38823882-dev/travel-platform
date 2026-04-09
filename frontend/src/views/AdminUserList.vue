<script setup>
import { ref, onMounted } from 'vue';
import request from '@/utils/request'; // 假設你已經封裝好 axios
import PostItem from '../components/PostItem.vue';
// import { useAuthStore } from '@/stores/authStore'; // 如果需要檢查權限

const users = ref([]);
const loading = ref(true);
const message = ref('');
const messageType = ref('success'); // 'success' or 'error'
const searchQuery = ref('');
const searchResult = ref(null);
const previewPostData = ref(null);
const isPreviewOpen = ref(false);

// --- 載入使用者列表 ---
const fetchUsers = async () => {
  loading.value = true;
  try {
    // 呼叫後端 API: GET /api/admin/users
    // 記得確保 request 有帶上 Admin Token
    const res = await request.get('/api/admin/users');
    users.value = res.data.filter(user => user.role?.roleName !== 'ROLE_ADMIN');
    loading.value = false;
  } catch (err) {
    loading.value = false;
    showMessage('無法載入使用者列表：' + (err.response?.data?.message || err.message), 'error');
  }
};

// --- 搜尋使用者與貼文 ---
const searchUser = async () => {
  if (!searchQuery.value.trim()) return;

  loading.value = true;
  searchResult.value = null;

  try {
    const res = await request.get('/api/messageboard/admin/search', {
      params: { username: searchQuery.value }
    });
    searchResult.value = res.data;
  } catch (err) {
    showMessage('搜尋失敗：' + (err.response?.data?.message || '查無此人'), 'error');
  } finally {
    loading.value = false;
  }
};

// --- 清除搜尋 ---
const clearSearch = () => {
  searchQuery.value = '';
  searchResult.value = null;
  // 如果需要可以在這裡重新 fetchUsers()，但因為原本資料還在 users ref 裡，直接切換顯示即可
};

// --- 點擊列表中的使用者直接搜尋 ---
const inspectUser = (username) => {
  searchQuery.value = username;
  searchUser();
};

// --- 切換封鎖狀態 ---
const toggleBlock = async (user) => {
  const newStatus = !user.isActive; // true=解鎖, false=封鎖
  const actionText = newStatus ? '解鎖' : '封鎖';

  if (!confirm(`確定要 ${actionText} 使用者 ${user.username} 嗎？`)) return;

  try {
    // 呼叫後端 API: PUT /api/admin/users/{id}/status
    await request.put(`/api/admin/users/${user.userId}/status`, {
      isActive: newStatus
    });

    // 前端直接更新狀態 (Optimistic UI)，不用重新整理
    user.isActive = newStatus;
    showMessage(`${user.username} 已成功${actionText}`, 'success');
  } catch (err) {
    showMessage(`${actionText}失敗：` + (err.response?.data?.message || '連線錯誤'), 'error');
  }
};

// --- 刪除貼文 (管理員權限) ---
const deletePost = async (postId) => {
  if (!confirm('確定要永久刪除這篇貼文嗎？此操作無法復原。')) return;

  try {
    // 假設後端對應的 Admin API 路徑
    await request.delete(`/api/messageboard/admin/post/${postId}`);

    // 成功後從前端列表中移除
    searchResult.value.posts = searchResult.value.posts.filter(p => p.postId !== postId);
    showMessage('貼文已成功刪除', 'success');
  } catch (err) {
    showMessage('刪除失敗：' + (err.response?.data?.message || '連線錯誤'), 'error');
  }
};

// --- 開啟預覽 ---
const openPreview = (post) => {
  previewPostData.value = post;
  isPreviewOpen.value = true;
};

// --- 顯示訊息 ---
const showMessage = (msg, type) => {
  message.value = msg;
  messageType.value = type;
  // 3秒後自動消失
  setTimeout(() => {
    message.value = '';
  }, 3000);
};

onMounted(() => {
  fetchUsers();
});
</script>

<template>
  <div class="page-wrapper">
    <div v-if="loading" class="loading-overlay animate-fade-in">
      <div class="spinner-box">
        <div class="spinner"></div>
        <p>資料載入中...</p>
      </div>
    </div>

    <div class="admin-container glass-panel animate-fade-in">

      <div class="auth-header text-center mb-4">
        <div class="logo-box mb-3">
          <i class="bi bi-shield-lock-fill"></i>
        </div>
        <h2>會員管理系統</h2>
        <p class="text-muted">管理使用者權限與帳號狀態</p>
      </div>

      <div class="toolbar mb-3">
        <div class="search-group">
          <input v-model="searchQuery" type="text" placeholder="輸入使用者名稱搜尋貼文..." class="search-input"
            @keyup.enter="searchUser">
          <button class="btn-search" @click="searchUser">
            <i class="bi bi-search"></i>
          </button>
          <button v-if="searchResult" class="btn-clear" @click="clearSearch">
            <i class="bi bi-x-lg"></i> 清除
          </button>
        </div>

        <button class="btn-refresh" @click="fetchUsers" :disabled="loading">
          <i class="bi bi-arrow-clockwise"></i> 重新整理列表
        </button>
      </div>

      <!-- 搜尋結果區塊 -->
      <div v-if="searchResult" class="search-result-section animate-fade-in">
        <div class="result-header mb-3">
          <h4 class="m-0">
            <i class="bi bi-person-lines-fill me-2"></i>
            搜尋結果：{{ typeof searchResult.user === 'object' ? searchResult.user.username : searchQuery }}
          </h4>
          <small class="text-muted" v-if="typeof searchResult.user === 'object'">User ID: {{ searchResult.user.userId
          }}</small>
        </div>

        <div class="table-container">
          <table class="custom-table">
            <thead>
              <tr>
                <th>貼文 ID</th>
                <th>內容摘要</th>
                <th>圖片</th>
                <th>發布時間</th>
                <th>讚數</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="post in searchResult.posts" :key="post.postId">
                <td>#{{ post.postId }}</td>
                <td style="max-width: 250px;" class="text-truncate" :title="post.content">{{ post.content || '(無文字內容)'
                }}</td>
                <td>
                  <img v-if="post.imageUrl" :src="post.imageUrl" class="post-thumb" />
                  <span v-else class="text-muted">-</span>
                </td>
                <td>{{ new Date(post.createdAt).toLocaleString() }}</td>
                <td>{{ post.likesCount }}</td>
                <td>
                  <div class="action-buttons">
                    <button class="btn-action btn-preview" @click="openPreview(post)">
                      <i class="bi bi-eye"></i> 預覽
                    </button>
                    <button class="btn-action btn-delete" @click="deletePost(post.postId)">
                      <i class="bi bi-trash"></i> 刪除
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="searchResult.posts.length === 0">
                <td colspan="6" class="text-center py-4 text-muted">此使用者沒有發布過貼文</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 原本的使用者列表 (沒有搜尋結果時顯示) -->
      <div v-else class="table-container">
        <table class="custom-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>使用者資訊</th>
              <th>角色</th>
              <th>狀態</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.userId">
              <td>#{{ user.userId }}</td>
              <td>
                <div class="user-info clickable" @click="inspectUser(user.username)" title="點擊查看貼文">
                  <span class="user-name">{{ user.username }}</span>
                  <span class="user-email">{{ user.email }}</span>
                </div>
              </td>
              <td>
                <span class="role-badge" :class="user.role?.roleName === 'ROLE_ADMIN' ? 'role-admin' : 'role-user'">
                  {{ user.role?.roleName === 'ROLE_ADMIN' ? '管理員' : '會員' }}
                </span>
              </td>
              <td>
                <span class="status-dot" :class="user.isActive ? 'active' : 'blocked'"></span>
                {{ user.isActive ? '正常' : '已封鎖' }}
              </td>
              <td class="text-right">
                <button class="btn-action" :class="user.isActive ? 'btn-block' : 'btn-unblock'"
                  @click="toggleBlock(user)">
                  <i class="bi" :class="user.isActive ? 'bi-slash-circle' : 'bi-check-circle'"></i>
                  {{ user.isActive ? '封鎖' : '解鎖' }}
                </button>
              </td>
            </tr>
            <tr v-if="users.length === 0 && !loading">
              <td colspan="5" class="text-center py-4 text-muted">
                目前沒有使用者資料
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="message" class="message shadow-sm animate-fade-in" :class="messageType">
        <i class="bi" :class="messageType === 'success' ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill'"></i>
        {{ message }}
      </p>

    </div>

    <!-- 預覽視窗 (Teleport 到 body 避免被遮擋) -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="isPreviewOpen" class="modal-overlay" @click.self="isPreviewOpen = false">
          <div class="preview-modal-content animate-pop">
            <div class="preview-header">
              <h3><i class="bi bi-eye-fill me-2"></i>貼文預覽</h3>
              <button class="btn-close-icon" @click="isPreviewOpen = false">
                <i class="bi bi-x-lg"></i>
              </button>
            </div>
            <div class="preview-body custom-scrollbar">
              <!-- 傳入 dummy user 避免 PostItem 報錯，並隱藏編輯功能 -->
              <PostItem v-if="previewPostData" :post="previewPostData" :user="{ id: -1 }" :is-any-editing="false"
                :readonly="true" />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* --- 繼承自原本的 CSS (背景與容器) --- */
.page-wrapper {
  min-height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  /* 相同的漸層背景 */
  background: radial-gradient(circle at 10% 20%, rgba(99, 102, 241, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 90% 80%, rgba(59, 130, 246, 0.05) 0%, transparent 50%);
}

.admin-container {
  width: 100%;
  max-width: 1200px;
  padding: 40px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.75);
  /* 稍微不透明一點以增加文字閱讀性 */
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

/* --- 標題與 LOGO (繼承並微調) --- */
.logo-box {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #ef4444 0%, #f97316 100%);
  /* 💡 修改：使用橘紅色系代表管理區 */
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  color: white;
  font-size: 1.8rem;
  box-shadow: 0 10px 20px rgba(239, 68, 68, 0.2);
}

.auth-header h2 {
  margin: 0;
  color: #1e293b;
  font-weight: 700;
}

/* --- 工具列 --- */
.toolbar {
  display: flex;
  justify-content: space-between;
  /* 改為左右分佈 */
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.btn-refresh {
  background: white;
  border: 1px solid #e2e8f0;
  color: #64748b;
  padding: 8px 16px;
  border-radius: 50px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.9rem;
}

.btn-refresh:hover {
  background: #f8fafc;
  color: #334155;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* --- 搜尋框樣式 --- */
.search-group {
  display: flex;
  gap: 8px;
}

.search-input {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  outline: none;
  font-size: 0.9rem;
  width: 200px;
}

.search-input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
}

.btn-search,
.btn-clear {
  border: none;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: 0.2s;
}

.btn-search {
  background: #6366f1;
  color: white;
}

.btn-search:hover {
  background: #4f46e5;
}

.btn-clear {
  background: #f1f5f9;
  color: #64748b;
}

.btn-clear:hover {
  background: #e2e8f0;
}

/* --- 表格樣式 (符合 Glassmorphism) --- */
.table-container {
  overflow-x: auto;
  /* 支援手機橫向捲動 */
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  padding: 4px;
}

.custom-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  text-align: left;
}

.custom-table th {
  padding: 16px;
  font-weight: 600;
  color: #64748b;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.custom-table td {
  padding: 16px;
  vertical-align: middle;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
  color: #334155;
  font-size: 0.95rem;
}

.custom-table tr:last-child td {
  border-bottom: none;
}

.custom-table tr:hover td {
  background: rgba(255, 255, 255, 0.6);
}

/* --- 使用者資訊欄位 --- */
.user-info {
  display: flex;
  flex-direction: column;
}

.user-info.clickable {
  cursor: pointer;
  transition: opacity 0.2s;
}

.user-info.clickable:hover {
  opacity: 0.7;
}

.user-name {
  font-weight: 600;
  color: #0f172a;
}

.user-email {
  font-size: 0.8rem;
  color: #94a3b8;
}

/* --- 角色標籤 --- */
.role-badge {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
}

.role-admin {
  background: #fff1f2;
  color: #e11d48;
  border: 1px solid #ffe4e6;
}

.role-user {
  background: #f0f9ff;
  color: #0ea5e9;
  border: 1px solid #e0f2fe;
}

/* --- 狀態燈號 --- */
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}

.status-dot.active {
  background: #22c55e;
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.2);
}

.status-dot.blocked {
  background: #94a3b8;
  box-shadow: 0 0 0 2px rgba(148, 163, 184, 0.2);
}

/* --- 動作按鈕 --- */
.btn-action {
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  /* 按鈕之間的間距 */
}

.btn-block {
  background: #fee2e2;
  color: #ef4444;
}

.btn-block:hover {
  background: #fecaca;
  transform: scale(1.05);
}

.btn-unblock {
  background: #dcfce7;
  color: #16a34a;
}

.btn-unblock:hover {
  background: #bbf7d0;
  transform: scale(1.05);
}

.btn-delete {
  background: #fee2e2;
  color: #ef4444;
}

.btn-delete:hover {
  background: #fecaca;
  transform: scale(1.05);
}

.btn-preview {
  background: #e0e7ff;
  color: #6366f1;
}

.btn-preview:hover {
  background: #c7d2fe;
  transform: scale(1.05);
}

/* --- Loading Overlay (沿用原版) --- */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(5px);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.spinner-box {
  text-align: center;
  color: #6366f1;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 5px solid rgba(99, 102, 241, 0.1);
  border-top: 5px solid #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

/* --- 訊息提示 (沿用原版) --- */
.message {
  margin-top: 20px;
  padding: 12px;
  border-radius: 12px;
  text-align: center;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.error {
  background: #fee2e2;
  color: #ef4444;
  border: 1px solid #fecaca;
}

.success {
  background: #dcfce7;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.animate-fade-in {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.text-right {
  text-align: right;
}

.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
}

/* --- 預覽視窗樣式 --- */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.preview-modal-content {
  background: #f8fafc;
  /* 淺灰背景，讓 PostItem 的白色卡片突顯 */
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  border-radius: 20px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-header {
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #334155;
  font-weight: 700;
}

.btn-close-icon {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  color: #94a3b8;
  cursor: pointer;
  transition: 0.2s;
  padding: 4px;
  border-radius: 50%;
}

.btn-close-icon:hover {
  background: #f1f5f9;
  color: #ef4444;
}

.preview-body {
  padding: 20px;
  overflow-y: auto;
}

@keyframes pop {
  0% {
    transform: scale(0.95);
    opacity: 0;
  }

  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.animate-pop {
  animation: pop 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
</style>