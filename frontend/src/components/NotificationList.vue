<script setup>
import { onMounted, onUnmounted, ref, watch, nextTick } from 'vue';
import { useNotificationStore } from '@/stores/notificationStore';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { useToastStore } from '@/stores/toastStore';

const store = useNotificationStore();
const { notifications, hasMore, isLoading } = storeToRefs(store);
const router = useRouter();
const toastStore = useToastStore();

const emit = defineEmits(['close']);

// 無限捲軸相關變數
const listBodyRef = ref(null);     // 捲動容器的 ref
const loadMoreTrigger = ref(null); // 底部感應元素的 ref
let observer = null;

// 確認框狀態
const showClearConfirm = ref(false);

// 滑動刪除相關變數
const touchStartX = ref(0);
const activeSwipeId = ref(null); // 目前正在滑動的項目 ID
const swipeOffset = ref(0);      // 目前滑動的位移量
const openSwipeId = ref(null);   // 目前處於「已滑開」狀態的項目 ID

onMounted(() => {
    store.fetchNotifications(false); // 初次載入

    // 設定 IntersectionObserver 監聽器
    observer = new IntersectionObserver((entries) => {
        // 當感應元素進入畫面、還有更多資料、且目前不在載入中時 -> 觸發載入
        if (entries[0].isIntersecting && hasMore.value && !isLoading.value) {
            loadMore();
        }
    }, {
        root: listBodyRef.value, // 指定捲動容器 (若為 null 則預設為視窗)
        threshold: 0.1,          // 露出 10% 即觸發
        rootMargin: "50px"       // 預載：快捲到底部前 50px 就觸發
    });

    // 開始觀察
    if (loadMoreTrigger.value) {
        observer.observe(loadMoreTrigger.value);
    }
});

// 監聽 loading 狀態，當載入完成後，如果還有更多資料且感應區仍在視窗內，強制觸發一次檢查
// 解決「初次載入資料不足以產生捲軸，導致無法觸發無限捲動」的問題
watch(isLoading, (newVal) => {
    if (!newVal && hasMore.value && observer && loadMoreTrigger.value) {
        observer.unobserve(loadMoreTrigger.value);
        nextTick(() => {
            if (loadMoreTrigger.value) observer.observe(loadMoreTrigger.value);
        });
    }
});

onUnmounted(() => {
    if (observer) observer.disconnect();
});

const loadMore = () => {
    store.fetchNotifications(true); // 載入更多
};

// 取得動態樣式 (處理滑動位移)
const getItemStyle = (id) => {
    // 如果正在拖曳這個項目
    if (activeSwipeId.value === id) {
        return { transform: `translateX(${swipeOffset.value}px)`, transition: 'none' };
    }
    // 如果這個項目已經被滑開 (顯示刪除按鈕)
    if (openSwipeId.value === id) {
        return { transform: `translateX(-80px)`, transition: 'transform 0.2s ease-out' };
    }
    // 預設狀態
    return { transform: `translateX(0)`, transition: 'transform 0.2s ease-out' };
};

// --- 觸控事件處理 ---
const handleTouchStart = (e, id) => {
    touchStartX.value = e.touches[0].clientX;
    activeSwipeId.value = id;
    // 如果目前這個項目已經是開著的，起始點就是 -80，否則為 0
    swipeOffset.value = openSwipeId.value === id ? -80 : 0;

    // 如果去滑別的項目，就把原本開著的關掉
    if (openSwipeId.value && openSwipeId.value !== id) {
        openSwipeId.value = null;
    }
};

const handleTouchMove = (e, id) => {
    if (activeSwipeId.value !== id) return;

    const currentX = e.touches[0].clientX;
    const diff = currentX - touchStartX.value;

    // 計算目標位移 (如果是已開啟狀態，要加上起始的 -80)
    let newOffset = (openSwipeId.value === id ? -80 : 0) + diff;

    // 限制滑動範圍：
    // 不能往右滑超過 0 (不顯示左邊的東西)
    if (newOffset > 0) newOffset = 0;
    // 不能往左滑超過 -100 (避免滑太遠)
    if (newOffset < -100) newOffset = -100;

    swipeOffset.value = newOffset;
};

const handleTouchEnd = (e, id) => {
    if (activeSwipeId.value !== id) return;

    // 判斷門檻值，決定是彈回還是展開
    // 如果滑動超過 -40px (往左滑)，就認定為展開
    if (swipeOffset.value < -40) {
        openSwipeId.value = id;
        swipeOffset.value = -80; // 定位在按鈕寬度
    } else {
        openSwipeId.value = null;
        swipeOffset.value = 0;
    }

    activeSwipeId.value = null;
};

const handleNotificationClick = (notification) => {
    // 如果該項目處於滑開狀態，點擊視為「關閉滑動」，不觸發跳轉
    if (openSwipeId.value === notification.id) {
        openSwipeId.value = null;
        return;
    }

    // 1. 標記為已讀
    if (!notification.isRead) {
        store.markAsRead(notification.id);
    }

    // 2. 根據類型跳轉
    if (['NEW_POST', 'LIKE', 'SHARE', 'COMMENT', 'FRIEND_EDIT'].includes(notification.type)) {
        router.push(`/MessageBoard/post/${notification.referenceId}`);
    } else if (notification.type === 'FRIEND_REQUEST' || notification.type === 'FRIEND_REQUEST_ACCEPTED') {
        router.push(`/friend`); // 跳轉至好友頁面
    } else if (notification.type === 'SYSTEM') {
        // 系統通知通常不跳轉，或跳到公告頁
    }

    // 3. 通知父層關閉選單 (如果是在 Dropdown 裡)
    emit('close');
};

const handleMarkAllRead = () => {
    store.markAllAsRead();
};

const handleDelete = (notification) => {
    // 刪除後重置滑動狀態
    if (openSwipeId.value === notification.id) openSwipeId.value = null;
    store.deleteNotification(notification.id);
};

const handleDeleteAll = () => {
    showClearConfirm.value = true;
};

const confirmClear = () => {
    store.deleteAllNotifications();
    showClearConfirm.value = false;
};

// 接受好友邀請
const acceptRequest = async (notification) => {
    try {
        await request.put(`/api/user/friend/accept/${notification.referenceId}`);
        toastStore.addToast('已接受好友邀請', 'success');
        store.markAsRead(notification.id); // 操作後標記為已讀
        store.triggerFriendListUpdate(); // 🔔 通知 FriendList 更新
    } catch (error) {
        toastStore.addToast(error.response?.data || '操作失敗', 'error');
    }
};

// 拒絕好友邀請
const rejectRequest = async (notification) => {
    try {
        await request.delete(`/api/user/friend/delete/${notification.referenceId}`);
        toastStore.addToast('已拒絕好友邀請', 'info');
        store.markAsRead(notification.id); // 操作後標記為已讀
        store.triggerFriendListUpdate(); // 🔔 通知 FriendList 更新
    } catch (error) {
        toastStore.addToast(error.response?.data || '操作失敗', 'error');
    }
};

// --- 畫畫邀請處理 ---
const acceptDrawInvite = (notification) => {
    console.log('【Debug】點擊接受邀請, 通知物件:', notification);

    let targetRoomId = notification.roomId;

    // 🛠️ 修改：如果沒有 roomId (代表是從 DB 拉的歷史訊息)，嘗試從 message 解析
    if (!targetRoomId && notification.message && notification.message.includes('||')) {
        targetRoomId = notification.message.split('||')[1];
    }

    console.log('【Debug】解析出的目標 RoomID:', targetRoomId);
    if (targetRoomId) {
        router.push(`/draw/${targetRoomId}`);
        store.markAsRead(notification.id);
        emit('close');
    } else {
        toastStore.addToast('此邀請連結已失效 (請對方重新邀請)', 'warning');
    }
};

const rejectDrawInvite = (notification) => {
    store.deleteNotification(notification.id);
    toastStore.addToast('已忽略邀請', 'info');
};

// 🛠️ 新增：過濾顯示訊息 (隱藏後面的 ||roomId)
const getDisplayMessage = (msg) => {
    if (!msg) return '';
    return msg.split('||')[0];
};

// 時間格式化小工具
const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diff = (now - date) / 1000; // seconds

    if (diff < 60) return '剛剛';
    if (diff < 3600) return `${Math.floor(diff / 60)} 分鐘前`;
    if (diff < 86400) return `${Math.floor(diff / 3600)} 小時前`;
    if (diff < 604800) return `${Math.floor(diff / 86400)} 天前`;
    return date.toLocaleDateString();
};
</script>

<template>
    <div class="notification-list-container">
        <!-- 標題列 -->
        <div class="header d-flex justify-content-between align-items-center p-3 border-bottom bg-white sticky-top">
            <h6 class="m-0 fw-bold text-dark">通知</h6>
            <div class="d-flex gap-3">
                <button class="btn btn-sm btn-link text-decoration-none p-0"
                    :disabled="!notifications.some(n => !n.isRead)" @click="handleMarkAllRead">
                    全部已讀
                </button>
                <button class="btn btn-sm btn-link text-danger text-decoration-none p-0" v-if="notifications.length > 0"
                    @click="handleDeleteAll">
                    清空
                </button>
            </div>
        </div>

        <!-- 列表內容 -->
        <div class="list-body custom-scrollbar" ref="listBodyRef">
            <!-- 空狀態 -->
            <div v-if="notifications.length === 0" class="empty-state text-center py-5 text-muted">
                <i class="bi bi-bell-slash fs-1 mb-2 d-block opacity-50"></i>
                <span class="small">暫無新通知</span>
            </div>

            <!-- 通知項目 -->
            <div v-for="notification in notifications" :key="notification.id"
                class="notification-wrapper border-bottom position-relative overflow-hidden">

                <!-- 底層：滑動後顯示的刪除紅色區塊 -->
                <div class="swipe-delete-bg d-flex align-items-center justify-content-end pe-3"
                    @click="handleDelete(notification)">
                    <span class="text-white fw-bold"><i class="bi bi-trash-fill me-1"></i>刪除</span>
                </div>

                <!-- 上層：通知內容卡片 -->
                <div class="notification-item p-3" :class="{ 'unread': !notification.isRead }"
                    :style="getItemStyle(notification.id)" @touchstart="handleTouchStart($event, notification.id)"
                    @touchmove="handleTouchMove($event, notification.id)"
                    @touchend="handleTouchEnd($event, notification.id)" @click="handleNotificationClick(notification)">

                    <div class="d-flex align-items-start">
                        <!-- 頭像 -->
                        <div class="avatar-wrapper me-3">
                            <img :src="notification.senderProfilePictureUrl || 'https://via.placeholder.com/40'"
                                class="avatar rounded-circle border" width="48" height="48" style="object-fit: cover;">
                            <!-- 根據類型顯示小圖示 -->
                            <span class="type-icon" :class="notification.type">
                                <i v-if="notification.type === 'NEW_POST'" class="bi bi-file-text-fill"></i>
                                <i v-else-if="notification.type === 'LIKE'" class="bi bi-heart-fill"></i>
                                <i v-else-if="notification.type === 'FRIEND_REQUEST' || notification.type === 'FRIEND_REQUEST_ACCEPTED'"
                                    class="bi bi-person-plus-fill"></i>
                                <i v-else-if="notification.type === 'COMMENT'" class="bi bi-chat-dots-fill"></i>
                                <i v-else-if="notification.type === 'FRIEND_EDIT'" class="bi bi-brush-fill"></i>
                                <i v-else-if="notification.type === 'DRAW_INVITE'" class="bi bi-palette-fill"></i>
                                <i v-else class="bi bi-bell-fill"></i>
                            </span>
                        </div>

                        <!-- 內容 -->
                        <div class="content flex-grow-1">
                            <div class="message-text mb-1 text-dark">
                                <!-- 🛠️ 修改：使用過濾函數顯示乾淨的文字 -->
                                {{ getDisplayMessage(notification.message) }}
                            </div>

                            <!-- 好友邀請操作按鈕 -->
                            <div v-if="notification.type === 'FRIEND_REQUEST' && !notification.isRead"
                                class="mt-2 mb-1 d-flex gap-2">
                                <button class="btn btn-sm btn-primary py-0 px-3" style="font-size: 0.8rem;"
                                    @click.stop="acceptRequest(notification)">接受</button>
                                <button class="btn btn-sm btn-outline-secondary py-0 px-3" style="font-size: 0.8rem;"
                                    @click.stop="rejectRequest(notification)">拒絕</button>
                            </div>

                            <!-- 畫畫邀請操作按鈕 -->
                            <div v-if="notification.type === 'DRAW_INVITE' && !notification.isRead"
                                class="mt-2 mb-1 d-flex gap-2">
                                <button class="btn btn-sm btn-success py-0 px-3" style="font-size: 0.8rem;"
                                    @click.stop="acceptDrawInvite(notification)">接受</button>
                                <button class="btn btn-sm btn-outline-secondary py-0 px-3" style="font-size: 0.8rem;"
                                    @click.stop="rejectDrawInvite(notification)">拒絕</button>
                            </div>

                            <small class="text-muted">{{ formatDate(notification.createdAt) }}</small>
                        </div>

                        <!-- 未讀藍點 -->
                        <span v-if="!notification.isRead" class="unread-dot rounded-circle bg-primary ms-2"></span>

                        <!-- 桌面版懸浮刪除按鈕 (保留給滑鼠操作者) -->
                        <button class="btn-delete-item" @click.stop="handleDelete(notification)">
                            <i class="bi bi-x"></i>
                        </button>
                    </div>
                </div>
            </div>

            <!-- 無限捲軸感應區 (替代原本的按鈕) -->
            <div v-if="hasMore" ref="loadMoreTrigger" class="p-3 text-center">
                <div v-if="isLoading" class="spinner-border spinner-border-sm text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
                <!-- 隱形佔位，確保有高度讓 Observer 感應到 -->
                <div v-else style="height: 10px;"></div>
            </div>

            <!-- 新增：到底提示 (讓你知道是因為沒資料了才沒按鈕) -->
            <div v-if="!hasMore && notifications.length > 0"
                class="p-3 text-center text-muted small bg-light border-top">
                已經顯示所有通知
            </div>
        </div>

        <!-- 清空確認 Modal (Teleport 到 body 以免被 overflow 裁切) -->
        <Teleport to="body">
            <Transition name="fade">
                <div v-if="showClearConfirm" class="modal-overlay" @click.self="showClearConfirm = false">
                    <div class="modal-content">
                        <h5 class="fw-bold mb-3">清空所有通知</h5>
                        <p class="text-muted mb-4">確定要刪除所有通知嗎？此操作無法復原。</p>
                        <div class="modal-actions">
                            <button class="btn-cancel" @click="showClearConfirm = false">取消</button>
                            <button class="btn-confirm" @click="confirmClear">確定清空</button>
                        </div>
                    </div>
                </div>
            </Transition>
        </Teleport>
    </div>
</template>

<style scoped>
.notification-list-container {
    background: white;
    width: 360px;
    max-width: 100%;
    /* 避免手機版爆版 */
    /* 如果放在 Dropdown 可以固定寬度 */
    max-height: 500px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.list-body {
    overflow-y: auto;
    flex: 1;
}

/* Notification Item 改為相對定位，並設定白色背景以遮擋底層按鈕 */
.notification-item {
    cursor: pointer;
    position: relative;
    background-color: white;
    z-index: 2;
    /* 確保在上層 */
    touch-action: pan-y;
    /* 允許垂直捲動，但攔截水平滑動 */
}

.notification-wrapper {
    background-color: #ef4444;
    /* 底色設為紅色 */
}

.notification-item:hover {
    background-color: #f8fafc;
}

.notification-item.unread {
    background-color: #f0f9ff;
    /* 未讀時顯示淡藍色背景 */
}

.unread-dot {
    width: 8px;
    height: 8px;
    margin-top: 8px;
    flex-shrink: 0;
}

.avatar-wrapper {
    position: relative;
}

.type-icon {
    position: absolute;
    bottom: -2px;
    right: -2px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 0.7rem;
    border: 2px solid white;
}

.type-icon.NEW_POST {
    background-color: #6366f1;
}

.type-icon.LIKE {
    background-color: #ef4444;
}

.type-icon.COMMENT {
    background-color: #0ea5e9;
}

.type-icon.FRIEND_EDIT {
    background-color: #a855f7;
    /* 紫色，代表神秘的突襲 */
}

.type-icon.DRAW_INVITE {
    background-color: #f59e0b;
    /* 橘黃色，代表創作與調色盤 */
}

.type-icon.FRIEND_REQUEST,
.type-icon.FRIEND_REQUEST_ACCEPTED {
    background-color: #22c55e;
}

.type-icon.SYSTEM {
    background-color: #f59e0b;
}

.message-text {
    font-size: 0.95rem;
    line-height: 1.4;
    word-break: break-word;
    /* 防止長文字破版 */
}

/* 自定義捲軸 */
.custom-scrollbar::-webkit-scrollbar {
    width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
    background: #f1f1f1;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
    background: #94a3b8;
}

/* 刪除按鈕樣式 */
.btn-delete-item {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: #f1f5f9;
    color: #94a3b8;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    opacity: 0;
    /* 預設隱藏 */
    transition: all 0.2s;
}

.notification-item:hover .btn-delete-item {
    opacity: 1;
    /* Hover 時顯示 */
}

.btn-delete-item:hover {
    background: #fee2e2;
    color: #ef4444;
}

/* 滑動刪除底層樣式 */
.swipe-delete-bg {
    position: absolute;
    top: 0;
    bottom: 0;
    right: 0;
    width: 100%;
    /* 寬度填滿，但被上層遮住，只露出右邊 */
    background-color: #ef4444;
    z-index: 1;
    cursor: pointer;
}

/* Modal 相關樣式 */
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(4px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1060;
    /* 比 dropdown 高 */
}

.modal-content {
    background: white;
    padding: 24px;
    border-radius: 16px;
    width: 90%;
    max-width: 320px;
    text-align: center;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
    animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.modal-actions {
    display: flex;
    gap: 12px;
}

.modal-actions button {
    flex: 1;
    padding: 10px;
    border-radius: 10px;
    border: none;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.2s;
}

.btn-cancel {
    background: #f1f5f9;
    color: #64748b;
}

.btn-cancel:hover {
    background: #e2e8f0;
}

.btn-confirm {
    background: #ef4444;
    color: white;
}

.btn-confirm:hover {
    background: #dc2626;
}

/* 動畫 */
@keyframes popIn {
    from {
        transform: scale(0.9);
        opacity: 0;
    }

    to {
        transform: scale(1);
        opacity: 1;
    }
}
</style>
