<template>
    <div class="friend-list-component h-100">
        <div class="modern-card sticky-sidebar h-100 d-flex flex-column animate-fade-up">
            <div
                class="card-header bg-gradient-light border-0 py-3 py-md-4 px-4 d-flex justify-content-between align-items-center rounded-top-4">
                <h5 class="fw-bold mb-0 text-dark d-flex align-items-center">
                    <i class="bi bi-people-fill text-success me-2 fs-4"></i>
                    我的旅伴
                </h5>
                <span class="badge bg-success rounded-pill px-3 py-2 shadow-sm">{{ friends.length }} 人</span>
            </div>

            <!-- 畫畫邀請區塊 (新增) -->
            <div v-if="drawInvites.length > 0" class="pending-requests-section border-bottom"
                style="background-color: #fffbeb;">
                <h6 class="text-muted small fw-bold mb-2 pt-3 px-4 text-warning">
                    <i class="bi bi-palette-fill me-1"></i> 畫畫邀請
                </h6>
                <ul class="list-group list-group-flush">
                    <li v-for="invite in drawInvites" :key="invite.id"
                        class="list-group-item bg-transparent border-0 px-4 py-3 d-flex align-items-center">
                        <div class="position-relative me-3">
                            <img v-if="invite.senderProfilePictureUrl" :src="invite.senderProfilePictureUrl"
                                class="avatar-circle avatar-md shadow-sm border" style="object-fit: cover;">
                            <div v-else class="avatar-circle avatar-md bg-light text-dark shadow-sm border">{{
                                (invite.senderName || '?').charAt(0).toUpperCase() }}</div>
                        </div>
                        <div class="flex-grow-1 fw-bold text-dark fs-6 text-truncate">{{ invite.senderName }}</div>
                        <div class="d-flex gap-2">
                            <button class="btn btn-icon btn-light text-success rounded-circle shadow-sm"
                                @click="acceptDrawInvite(invite)" title="接受">
                                <i class="bi bi-check-lg"></i>
                            </button>
                            <button class="btn btn-icon btn-light text-danger rounded-circle shadow-sm"
                                @click="rejectDrawInvite(invite)" title="拒絕">
                                <i class="bi bi-x-lg"></i>
                            </button>
                        </div>
                    </li>
                </ul>
            </div>

            <!-- 好友邀請區塊 -->
            <div v-if="pendingRequests.length > 0" class="pending-requests-section p-3 border-bottom">
                <h6 class="text-muted small fw-bold mb-2">好友邀請</h6>
                <ul class="list-group list-group-flush">
                    <li v-for="req in pendingRequests" :key="req.friendshipId"
                        class="list-group-item bg-transparent border-0 p-2 d-flex align-items-center">
                        <img v-if="req.profilePictureUrl" :src="req.profilePictureUrl"
                            class="avatar-circle avatar-sm me-2">
                        <div v-else class="avatar-circle avatar-sm bg-light text-dark me-2">{{
                            req.requesterName.charAt(0).toUpperCase() }}</div>
                        <div class="flex-grow-1 fw-bold text-dark small text-truncate">{{ req.requesterName }}</div>
                        <div class="d-flex gap-2">
                            <button class="btn btn-sm btn-success-soft rounded-circle"
                                @click="acceptRequest(req.friendshipId)" title="接受">
                                <i class="bi bi-check-lg"></i>
                            </button>
                            <button class="btn btn-sm btn-danger-soft rounded-circle"
                                @click="rejectRequest(req.friendshipId)" title="拒絕">
                                <i class="bi bi-x-lg"></i>
                            </button>
                        </div>
                    </li>
                </ul>
            </div>

            <div class="card-body p-0 friend-list-container flex-grow-1">
                <div v-if="friends.length === 0" class="empty-state py-5 text-center">
                    <div class="empty-icon bg-light rounded-circle mx-auto mb-3">
                        <i class="bi bi-emoji-smile text-muted fs-1"></i>
                    </div>
                    <h6 class="text-muted fw-bold">還沒有任何好友</h6>
                    <p class="text-secondary small mb-0">趕快去尋找新夥伴吧！</p>
                </div>

                <ul v-else class="list-group list-group-flush custom-list">
                    <li v-for="friend in friends" :key="friend.friendshipId"
                        class="list-group-item border-0 px-4 py-3 d-flex align-items-center friend-item"
                        @click="openChat(friend)">

                        <div class="position-relative me-3">
                            <img v-if="friend.profilePictureUrl" :src="friend.profilePictureUrl"
                                class="avatar-circle avatar-md shadow-sm border" style="object-fit: cover;">
                            <div v-else class="avatar-circle avatar-md bg-light text-dark shadow-sm border">
                                {{ friend.requesterName ? friend.requesterName.charAt(0).toUpperCase() : '?' }}
                            </div>
                            <span :class="getIndicatorClass(friend)"></span>
                        </div>

                        <div class="flex-grow-1">
                            <div class="fw-bold text-dark fs-6">{{ friend.requesterName }}</div>
                            <div :class="getStatusTextClass(friend)" class="small fw-medium mt-1 d-flex align-items-center">
                                <i class="bi bi-circle-fill" style="font-size: 0.4rem; margin-right: 4px;"></i>
                                {{ getStatusLabel(friend) }}
                            </div>
                        </div>

                        <div class="friend-actions">
                            <!-- 畫畫邀請按鈕 -->
                            <button v-if="allowDraw"
                                class="btn btn-icon btn-light text-warning rounded-circle me-2 shadow-sm" title="邀請畫畫"
                                @click.stop="inviteDraw(friend)">
                                <i class="bi bi-palette-fill"></i>
                            </button>
                            <button class="btn btn-icon btn-light text-primary rounded-circle me-2 shadow-sm"
                                title="發送訊息" @click.stop="openChat(friend)">
                                <i class="bi bi-chat-dots-fill"></i>
                            </button>
                            <button v-if="allowDelete"
                                class="btn btn-icon btn-light text-danger rounded-circle shadow-sm" title="刪除好友"
                                @click.stop="deleteFriend(friend.friendshipId)">
                                <i class="bi bi-person-x-fill"></i>
                            </button>
                        </div>
                    </li>
                </ul>
            </div>
        </div>

        <!-- 聊天室浮動視窗 -->
        <div class="chat-container" style="pointer-events: none;">
            <transition-group name="chat-anim" tag="div" class="d-flex flex-row align-items-end gap-3">
                <div v-for="(chat, index) in activeChats" :key="chat.friend.friendshipId" class="chat-popup shadow-lg"
                    :class="{ 'minimized': chat.minimized }">

                    <!-- 標題列 -->
                    <div class="chat-header bg-gradient-primary text-white p-3 d-flex justify-content-between align-items-center rounded-top-4"
                        :class="{ 'has-new-message': chat.hasNewMessage }" @click="toggleMinimize(chat)"
                        style="cursor: pointer;">
                        <div class="d-flex align-items-center" style="min-width: 0;">
                            <template v-if="!chat.minimized">
                                <img v-if="chat.friend.profilePictureUrl" :src="chat.friend.profilePictureUrl"
                                    class="avatar-circle avatar-sm me-2 shadow-sm border-0" style="object-fit: cover;">
                                <div v-else
                                    class="avatar-circle avatar-sm bg-white text-primary me-2 shadow-sm border-0">
                                    {{ (chat.friend.requesterName || chat.friend.username ||
                                        '?').charAt(0).toUpperCase() }}
                                </div>
                            </template>
                            <div class="chat-title-info">
                                <div class="d-flex align-items-center">
                                    <span v-if="chat.unreadCount > 0"
                                        class="badge rounded-pill bg-danger me-2 shadow-sm" style="font-size: 0.75rem;">
                                        {{ chat.unreadCount > 99 ? '99+' : chat.unreadCount }}
                                    </span>
                                    <h6 class="fw-bold mb-0 text-truncate" style="max-width: 120px;">{{
                                        chat.friend.requesterName ||
                                        chat.friend.username }}</h6>
                                </div>
                                <p v-if="chat.minimized && chat.messages.length > 0"
                                    class="last-message-preview text-truncate mb-0">{{
                                        chat.messages[chat.messages.length - 1].sender === myUsername ? '你: ' : '' }}{{
                                        chat.messages[chat.messages.length - 1].content }}</p>
                            </div>
                        </div>
                        <div class="d-flex align-items-center">
                            <button class="btn btn-sm btn-link text-white p-0 border-0 fs-5 me-3"
                                @click.stop="toggleMinimize(chat)" title="縮小/展開">
                                <i class="bi" :class="chat.minimized ? 'bi-box-arrow-in-up-right' : 'bi-dash-lg'"></i>
                            </button>
                            <button class="btn btn-sm btn-link text-white p-0 border-0 fs-5 close-chat-btn"
                                @click.stop="closeChat(index)" title="關閉">
                                <i class="bi bi-x-lg"></i>
                            </button>
                        </div>
                    </div>

                    <!-- 聊天內容 -->
                    <div v-if="!chat.minimized" class="chat-body p-3 bg-white"
                        :ref="el => { if (el) chatBodyRefs[index] = el }">
                        <div class="text-center text-muted mb-3 small">今天</div>
                        <div v-for="(msg, msgIndex) in chat.messages" :key="msgIndex"
                            class="chat-message mb-3 d-flex flex-column"
                            :class="msg.sender === myUsername ? 'align-items-end' : 'align-items-start'">

                            <!-- 新增：未讀訊息分隔線 -->
                            <div v-if="msgIndex === chat.firstUnreadIndex" class="unread-separator mb-2">
                                <span>以下為尚未閱讀的訊息</span>
                            </div>

                            <div class="message-bubble p-2 px-3 rounded-4 shadow-sm"
                                :class="msg.sender === myUsername ? 'bg-primary text-white' : 'bg-light text-dark border'"
                                :style="msg.sender === myUsername ? 'border-bottom-right-radius: 4px !important;' : 'border-bottom-left-radius: 4px !important;'">
                                {{ msg.content }}
                            </div>
                            <small class="text-muted mt-1" style="font-size: 0.7rem;">{{ msg.sendTime }}</small>
                        </div>
                    </div>

                    <!-- 正在輸入提示 -->
                    <div v-if="!chat.minimized && chat.typingUser" class="typing-indicator px-3 py-1">
                        <span class="typing-bubble">
                            <span class="typing-dots"><span></span><span></span><span></span></span>
                            {{ chat.typingUser }} 正在輸入中...
                        </span>
                    </div>

                    <!-- 輸入框 -->
                    <div v-if="!chat.minimized"
                        class="chat-footer p-2 bg-light border-top d-flex align-items-center rounded-bottom-4">
                        <input type="text" class="form-control rounded-pill border-0 shadow-none px-3 ms-1 chat-input"
                            placeholder="輸入訊息..." v-model="chat.newMessage" @keyup.enter="sendMessage(chat)"
                            @input="onTyping(chat)">
                        <button class="btn btn-primary rounded-circle ms-2 shadow-sm send-msg-btn"
                            @click="sendMessage(chat)">
                            <i class="bi bi-send-fill" style="margin-left: -2px;"></i>
                        </button>
                    </div>
                </div>
            </transition-group>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, onBeforeUpdate, watch } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from '@/stores/authStore';
import { useNotificationStore } from '@/stores/notificationStore';
import { useToastStore } from '@/stores/toastStore';
import { wsUrl } from '@/config/appConfig.js';

const friends = ref([]);
// 好友狀態對照表：{ [username]: 'ONLINE' | 'OFFLINE' | 'TYPING' }
const friendStatuses = ref({});
// 打字計時器對照表：{ [chatFriendshipId]: timeoutId }
const typingTimers = {};
const pendingRequests = ref([]);

// 💡 改為陣列來儲存多個聊天視窗
const activeChats = ref([]);
const chatBodyRefs = ref([]); // 用來儲存多個視窗的 DOM ref

const authStore = useAuthStore();
const notificationStore = useNotificationStore();
const toastStore = useToastStore();
const router = useRouter();

const props = defineProps({
    allowDraw: {
        type: Boolean,
        default: false // 預設不顯示畫畫 (好友列表頁面用)
    },
    allowDelete: {
        type: Boolean,
        default: true  // 預設顯示刪除 (好友列表頁面用)
    }
});

let stompClient = null;
const myUsername = computed(() => authStore.username || '');

// --- 畫畫邀請相關邏輯 (從 Store 獲取) ---
const drawInvites = computed(() => {
    return notificationStore.notifications.filter(n => n.type === 'DRAW_INVITE' && !n.isRead);
});

// 追蹤已設定倒數的邀請 ID，避免重複設定 Timer
const pendingInviteTimers = new Set();

// 監聽新的畫畫邀請，設定 10 秒後自動移除 (過期)
watch(drawInvites, (newInvites) => {
    newInvites.forEach(invite => {
        if (!pendingInviteTimers.has(invite.id)) {
            pendingInviteTimers.add(invite.id);
            setTimeout(() => {
                const target = notificationStore.notifications.find(n => n.id === invite.id);
                // 如果通知還在且未讀 (代表使用者沒動作)，就刪除它
                if (target && !target.isRead) {
                    notificationStore.deleteNotification(invite.id);
                }
                pendingInviteTimers.delete(invite.id);
            }, 10000);
        }
    });
}, { deep: true, immediate: true });

const acceptDrawInvite = (notification) => {
    let targetRoomId = notification.roomId;
    // 嘗試從訊息中解析 RoomID (如果不是即時 WS 訊息)
    if (!targetRoomId && notification.message && notification.message.includes('||')) {
        targetRoomId = notification.message.split('||')[1];
    }

    if (targetRoomId) {
        router.push(`/draw/${targetRoomId}`);
        notificationStore.markAsRead(notification.id);
    } else {
        toastStore.addToast('此邀請連結已失效', 'warning');
    }
};

const rejectDrawInvite = (notification) => {
    notificationStore.deleteNotification(notification.id);
    toastStore.addToast('已忽略邀請', 'info');
};

// 確保在更新前清空 refs 陣列，避免殘留
onBeforeUpdate(() => {
    chatBodyRefs.value = [];
});

// 取得好友名單 & 待確認邀請
const fetchData = async () => {
    try {
        const [friendsRes, pendingRes] = await Promise.all([
            request.get('/api/user/list'),
            request.get('/api/user/friend/requests')
        ]);
        friends.value = friendsRes.data;
        pendingRequests.value = pendingRes.data;
    } catch (error) {
        console.error('獲取好友/邀請列表失敗', error);
    }
};

// 刪除好友
const deleteFriend = async (friendshipId) => {
    if (!confirm('確定要解除好友關係嗎？')) return;
    try {
        await request.delete(`/api/user/friend/delete/${friendshipId}`);
        fetchData();
        // 如果該好友的視窗開著，就關閉它
        const index = activeChats.value.findIndex(c => c.friend.friendshipId === friendshipId);
        if (index !== -1) {
            closeChat(index);
        }
    } catch (error) {
        console.error('Delete failed', error);
        alert('刪除失敗');
    }
};

// 接受好友邀請
const acceptRequest = async (friendshipId) => {
    try {
        await request.put(`/api/user/friend/accept/${friendshipId}`);
        toastStore.addToast('已成為好友！', 'success');
        fetchData(); // 重新整理列表
    } catch (error) {
        console.error('接受邀請失敗', error);
        toastStore.addToast('操作失敗', 'error');
    }
};

// 拒絕好友邀請
const rejectRequest = async (friendshipId) => {
    try {
        await request.delete(`/api/user/friend/delete/${friendshipId}`);
        toastStore.addToast('已拒絕邀請', 'info');
        fetchData(); // 重新整理列表
    } catch (error) {
        console.error('拒絕邀請失敗', error);
        toastStore.addToast('操作失敗', 'error');
    }
};

// 邀請好友畫畫
const inviteDraw = async (friend) => {
    try {
        // 假設後端有一個 API 負責生成 RoomID 並呼叫 NotificationService.sendDrawInvitation
        // POST /api/user/draw/invite
        const res = await request.post('/api/user/draw/invite', {
            receiverId: friend.friendshipId // 注意：這裡需確認後端是要 friendshipId 還是 userId
        });

        const roomId = res.data?.roomId;
        if (!roomId) return;

        // 1. 顯示等待提示
        toastStore.addToast('邀請已發送，等待對方回應 (10秒)...', 'info');

        // 2. 檢查 WS 連線狀態
        if (!stompClient || !stompClient.connected) {
            // 若剛好斷線，只好直接跳轉，避免卡住
            router.push(`/draw/${roomId}`);
            return;
        }

        // 3. 訂閱房間頻道，等待對方加入
        let subscription = null;
        let isAccepted = false;

        // 設定 10 秒超時機制
        const timeoutId = setTimeout(() => {
            if (!isAccepted) {
                if (subscription) subscription.unsubscribe();
                toastStore.addToast('對方沒有回應邀請', 'warning');
            }
        }, 10000);

        subscription = stompClient.subscribe(`/topic/draw/${roomId}`, (message) => {
            const body = JSON.parse(message.body);
            console.log('收到畫布房間訊息:', body); // 🛠️ Debug 用

            // 只要收到 JOIN 訊息 (代表對方進入了房間)
            if (body.type === 'JOIN') {
                isAccepted = true;
                clearTimeout(timeoutId); // 清除倒數
                subscription.unsubscribe(); // 取消這裡的訂閱 (因為 DrawView 會自己再訂閱一次)

                toastStore.addToast('對方已接受！正在進入畫布...', 'success');
                router.push(`/draw/${roomId}`);
            }
        });

    } catch (error) {
        console.error('邀請失敗', error);
        toastStore.addToast('邀請發送失敗', 'error');
    }
};

// --- 聊天室功能 ---

const scrollToBottom = (index) => {
    nextTick(() => {
        const el = chatBodyRefs.value[index];
        if (el) {
            el.scrollTop = el.scrollHeight;
        }
    });
};

const scrollToMessage = (chatIndex, msgIndex) => {
    nextTick(() => {
        const container = chatBodyRefs.value[chatIndex];
        // children[0] 是 "今天" 的日期標籤，所以訊息從 index 1 開始
        if (container && container.children.length > msgIndex + 1) {
            const target = container.children[msgIndex + 1];
            target.scrollIntoView({ block: 'start', behavior: 'auto' });
        }
    });
};

const openChat = async (friend) => {
    // 1. 檢查視窗是否已經開啟
    const existingChat = activeChats.value.find(c => c.friend.friendshipId === friend.friendshipId);
    if (existingChat) {
        // 如果已開啟，則將其展開並將其他視窗最小化
        activeChats.value.forEach(c => {
            c.minimized = c.friend.friendshipId !== friend.friendshipId;
            // 如果其他視窗被強制最小化，重置它們的未讀指標 (視為已讀過)
            if (c.minimized) c.firstUnreadIndex = -1;
        });
        existingChat.hasNewMessage = false; // 點開就取消通知
        existingChat.unreadCount = 0; // 重置未讀數
        // 展開已存在的視窗時，決定捲動位置
        const index = activeChats.value.indexOf(existingChat);
        if (index !== -1) {
            if (existingChat.firstUnreadIndex !== undefined && existingChat.firstUnreadIndex !== -1) {
                scrollToMessage(index, existingChat.firstUnreadIndex);
                // 💡 移除這行：不要立刻清除，讓使用者能看到分隔線
                // existingChat.firstUnreadIndex = -1; 
            } else {
                scrollToBottom(index);
            }
        }
        return;
    }

    // 2. 限制最多開啟視窗數量 (避免塞爆畫面)
    const MAX_CHATS = 3; // 您可以將此數字改為 4 或 5
    if (activeChats.value.length >= MAX_CHATS) {
        activeChats.value.pop(); // 移除最尾端 (最舊) 的一個
    }

    // 3. 將所有現有視窗最小化
    activeChats.value.forEach(c => {
        c.minimized = true;
    });

    // 4. 建立新聊天物件
    const newChat = {
        friend: friend,
        messages: [],
        newMessage: '',
        minimized: false,       // 新視窗預設展開
        hasNewMessage: false,   // 是否有新訊息
        unreadCount: 0,         // 未讀訊息數
        firstUnreadIndex: -1,   // 第一則未讀訊息的索引
        typingUser: ''          // 對方正在輸入時顯示名稱
    };
    activeChats.value.unshift(newChat); // 加到最前面 (最左邊)

    try {
        const friendName = friend.requesterName || friend.username;
        const response = await request.get(`/api/user/chat/history/${friendName}`);
        // 修正：必須更新 activeChats 裡的「響應式物件」，畫面才會更新
        // 同時使用 find 確保在非同步請求回來後，能找到正確的視窗 (避免因開啟多視窗導致 index 變動)
        const targetChat = activeChats.value.find(c => c.friend.friendshipId === friend.friendshipId);
        if (targetChat) {
            targetChat.messages = response.data;
            const index = activeChats.value.indexOf(targetChat);
            if (index !== -1) scrollToBottom(index);
        }
    } catch (error) {
        console.error("無法獲取歷史紀錄", error);
    }
};

const toggleMinimize = (chat) => {
    const wasMinimized = chat.minimized;
    chat.minimized = !chat.minimized;

    // 如果我們剛剛展開了一個視窗
    if (wasMinimized) {
        chat.hasNewMessage = false; // 點開就取消通知
        chat.unreadCount = 0; // 重置未讀數
        // 將所有其他視窗最小化
        activeChats.value.forEach(c => {
            if (c.friend.friendshipId !== chat.friend.friendshipId) {
                c.minimized = true;
                c.firstUnreadIndex = -1; // 最小化其他視窗時，清除未讀指標
            }
        });
        // 展開視窗時，決定捲動位置
        const index = activeChats.value.indexOf(chat);
        if (index !== -1) {
            if (chat.firstUnreadIndex !== undefined && chat.firstUnreadIndex !== -1) {
                scrollToMessage(index, chat.firstUnreadIndex);
                // 💡 移除這行：保持分隔線顯示
                // chat.firstUnreadIndex = -1;
            } else {
                scrollToBottom(index);
            }
        }
    } else {
        // 💡 當視窗被縮小時，清除未讀指標 (下次展開就不會看到舊的分隔線)
        chat.firstUnreadIndex = -1;
    }
};

const closeChat = (index) => {
    activeChats.value.splice(index, 1);
};

// ── 狀態輔助函式 ──────────────────────────────────────
const getFriendStatus = (friend) => {
    const name = friend.requesterName || friend.username;
    return friendStatuses.value[name] || 'OFFLINE';
};
const getStatusLabel = (friend) => {
    const s = getFriendStatus(friend);
    if (s === 'TYPING') return '正在輸入...';
    if (s === 'ONLINE') return '線上';
    return '離線';
};
const getStatusTextClass = (friend) => {
    const s = getFriendStatus(friend);
    if (s === 'TYPING') return 'text-primary';
    if (s === 'ONLINE') return 'text-success';
    return 'text-muted';
};
const getIndicatorClass = (friend) => {
    const s = getFriendStatus(friend);
    if (s === 'TYPING') return 'online-indicator indicator-typing';
    if (s === 'ONLINE') return 'online-indicator indicator-online';
    return 'online-indicator indicator-offline';
};

// ── 打字事件 ─────────────────────────────────────────
const onTyping = (chat) => {
    if (!stompClient || !stompClient.connected) return;
    stompClient.publish({ destination: '/app/status.typing', body: 'TYPING' });
    // 清除舊計時器
    const key = chat.friend.friendshipId;
    clearTimeout(typingTimers[key]);
    // 2 秒沒繼續打字，發送 IDLE
    typingTimers[key] = setTimeout(() => {
        if (stompClient && stompClient.connected) {
            stompClient.publish({ destination: '/app/status.typing', body: 'IDLE' });
        }
    }, 2000);
};

// ── WebSocket 連線 ──────────────────────────────────
const connectWebSocket = () => {
    stompClient = new Client({
        // 🌟 核心改變：改用原生 WebSocket 連線，不需要加 /websocket 後綴了
        brokerURL: wsUrl,
        onConnect: () => {
            // 3. 連線成功後，立刻拉取目前所有在線使用者，初始化好友狀態
            //    避免「先連線的人永遠顯示離線」的問題
            request.get('/api/user/online-status').then(res => {
                const onlineUsernames = res.data; // Set<String> -> Array
                onlineUsernames.forEach(username => {
                    friendStatuses.value[username] = 'ONLINE';
                });
            }).catch(() => { /* 靜默失敗，不影響其他功能 */ });

            // 1. 訂閱私訊頻道
            stompClient.subscribe('/user/queue/messages', (message) => {
                const receivedMsg = JSON.parse(message.body);

                // 遍歷所有開啟的視窗，將訊息分發到正確的視窗
                activeChats.value.forEach((chat, index) => {
                    const friendName = chat.friend.requesterName || chat.friend.username;

                    // 判斷訊息是否屬於這個視窗 (對方傳來的 OR 我傳給對方的)
                    const isFromFriend = receivedMsg.sender === friendName;
                    const isToFriend = receivedMsg.sender === authStore.username && receivedMsg.receiver === friendName;

                    if (isFromFriend || isToFriend) {
                        chat.messages.push(receivedMsg);

                        // 如果視窗是縮小的，且是對方傳來的訊息，就亮燈
                        if (chat.minimized && isFromFriend) {
                            chat.hasNewMessage = true;
                            if ((chat.unreadCount || 0) === 0) {
                                chat.firstUnreadIndex = chat.messages.length - 1;
                            }
                            chat.unreadCount = (chat.unreadCount || 0) + 1;
                        } else {
                            scrollToBottom(index);
                        }
                    }
                });
            });

            // 2. 訂閱公開狀態頻道，更新好友的線上/離線/打字狀態
            stompClient.subscribe('/topic/public-status', (res) => {
                const data = JSON.parse(res.body);
                // data = { userId: 'username', status: 'ONLINE' | 'OFFLINE' | 'TYPING' | 'IDLE' }

                // 2a. 更新好友列表圓點顏色
                if (data.status === 'IDLE') {
                    friendStatuses.value[data.userId] = 'ONLINE';
                } else {
                    friendStatuses.value[data.userId] = data.status;
                }

                // 2b. 找出對應的開啟聊天視窗，設定「正在輸入中」提示
                const matchedChat = activeChats.value.find(c => {
                    const name = c.friend.requesterName || c.friend.username;
                    return name === data.userId;
                });
                if (matchedChat) {
                    if (data.status === 'TYPING') {
                        matchedChat.typingUser = data.userId; // 顯示提示
                    } else {
                        matchedChat.typingUser = '';          // IDLE / OFFLINE → 隱藏
                    }
                }

                console.log(`[Status] ${data.userId} → ${data.status}`);
            });
        },
        onStompError: (frame) => {
            console.error('WebSocket 報錯: ', frame.headers['message']);
        }
    });
    stompClient.activate();
};

const sendMessage = (chat) => {
    if (!chat.newMessage.trim() || !stompClient || !stompClient.connected) return;

    const friendName = chat.friend.requesterName || chat.friend.username;
    const chatRequest = {
        receiver: friendName,
        content: chat.newMessage.trim()
    };

    stompClient.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(chatRequest)
    });

    chat.newMessage = '';
};

onMounted(() => {
    fetchData();
    connectWebSocket();
});

// 監聽來自 App.vue 的全域通知
watch(() => notificationStore.notifications.length, (newLength, oldLength) => {
    // 只有當有新通知加入時才觸發
    if (newLength > oldLength) {
        const latestNotification = notificationStore.notifications[0];
        // 如果是好友邀請或接受邀請的通知，就重新整理列表
        if (latestNotification.type === 'FRIEND_REQUEST' || latestNotification.type === 'FRIEND_REQUEST_ACCEPTED') {
            fetchData();
        }
    }
});

// 監聽手動觸發的好友列表更新 (例如從通知列表接受好友邀請後)
watch(() => notificationStore.friendListUpdateTrigger, () => {
    fetchData();
});

// 公開方法讓父組件調用 (例如接受好友申請後刷新列表)
defineExpose({ fetchData });
</script>

<style scoped>
/* 現代卡片設計 */
.modern-card {
    background: #ffffff;
    border-radius: 20px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
    overflow: hidden;
}

.bg-gradient-light {
    background: linear-gradient(to right, #f8fafc, #ffffff);
}

/* 動畫 */
.animate-fade-up {
    animation: fadeUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes fadeUp {
    from {
        opacity: 0;
        transform: translateY(20px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* 列表樣式 */
.custom-list .list-group-item {
    transition: all 0.2s ease;
    border-bottom: 1px solid rgba(0, 0, 0, 0.02) !important;
    cursor: pointer;
}

.friend-item:hover {
    background-color: #f8fafc !important;
}

/* 頭像 */
.avatar-circle {
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
}

.avatar-md {
    width: 46px;
    height: 46px;
    font-size: 1.25rem;
}

.avatar-sm {
    width: 40px;
    height: 40px;
    font-size: 1.1rem;
}

.bg-gradient-primary {
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
}

.online-indicator {
    position: absolute;
    bottom: 0;
    right: 0;
    width: 14px;
    height: 14px;
    border: 2px solid #ffffff;
    border-radius: 50%;
    transition: background-color 0.3s ease;
}

.indicator-online {
    background-color: #22c55e;
}

.indicator-offline {
    background-color: #94a3b8;
}

.indicator-typing {
    background-color: #6366f1;
    animation: pulse-typing 1s infinite;
}

@keyframes pulse-typing {
    0%, 100% { transform: scale(1); opacity: 1; }
    50% { transform: scale(1.3); opacity: 0.7; }
}

.friend-list-container {
    overflow-y: auto;
    /* 確保在父容器高度限制下可捲動 */
}

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

.empty-icon {
    width: 80px;
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* --- 多視窗容器 --- */
.chat-container {
    position: fixed;
    bottom: 0;
    right: 20px;
    display: flex;
    flex-direction: row;
    align-items: flex-end;
    gap: 15px;
    z-index: 1050;
    pointer-events: none;
    /* 讓點擊穿透容器空白處 */
    flex-direction: row;
    align-items: flex-end;
    gap: 15px;
}

.chat-popup {
    width: 320px;
    background: white;
    border-radius: 16px 16px 0 0;
    display: flex;
    flex-direction: column;
    border: 1px solid rgba(0, 0, 0, 0.05);
    pointer-events: auto;
    /* 恢復視窗的可點擊性 */
    box-shadow: 0 5px 20px rgba(0, 0, 0, 0.15);
}

/* 好友邀請按鈕 */
.btn-success-soft {
    background-color: #dcfce7;
    color: #166534;
    border: none;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.btn-success-soft:hover {
    background-color: #bbf7d0;
}

.btn-danger-soft {
    background-color: #fee2e2;
    color: #991b1b;
    border: none;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.btn-danger-soft:hover {
    background-color: #fecaca;
}

/* 新訊息閃爍動畫 */
.chat-header.has-new-message {
    animation: pulse-yellow 1.5s infinite;
}

@keyframes pulse-yellow {
    0% {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    }

    50% {
        background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
        transform: scale(1.02);
        box-shadow: 0 0 15px rgba(245, 158, 11, 0.7);
    }

    100% {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    }
}

/* 縮小狀態樣式 */
.chat-popup.minimized {
    width: 200px;
    /* 縮小寬度 */
}

.chat-title-info {
    line-height: 1.2;
    /* 讓兩行文字更緊湊 */
}

.last-message-preview {
    font-size: 0.8rem;
    font-weight: 400;
    color: rgba(255, 255, 255, 0.75);
    max-width: 160px;
    /* 確保文字會被截斷 */
}

/* 未讀訊息分隔線 */
.unread-separator {
    width: 100%;
    text-align: center;
    align-self: center;
    /* 讓它在 flex-column 中置中，不受 align-items 影響 */
}

.unread-separator span {
    background-color: rgba(0, 0, 0, 0.05);
    color: #64748b;
    font-size: 0.75rem;
    padding: 4px 12px;
    border-radius: 12px;
    font-weight: 500;
}

.chat-body {
    height: 350px;
    overflow-y: auto;
    background-color: #f8fafc !important;
}

.message-bubble {
    max-width: 85%;
    font-size: 0.95rem;
    line-height: 1.5;
    word-break: break-word;
}

.chat-input {
    background-color: #f1f5f9;
}

.send-msg-btn {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* 正在輸入提示 */
.typing-indicator {
    background-color: #f8fafc;
    border-top: 1px solid rgba(0, 0, 0, 0.04);
}

.typing-bubble {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: #fff;
    border: 1px solid rgba(0, 0, 0, 0.07);
    padding: 5px 12px;
    border-radius: 16px;
    font-size: 0.78rem;
    color: #64748b;
    font-weight: 500;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.typing-dots {
    display: inline-flex;
    align-items: center;
    gap: 3px;
}

.typing-dots span {
    width: 5px;
    height: 5px;
    background-color: #6366f1;
    border-radius: 50%;
    animation: bounce-dot 1s infinite ease-in-out;
}

.typing-dots span:nth-child(2) { animation-delay: 0.16s; }
.typing-dots span:nth-child(3) { animation-delay: 0.32s; }

@keyframes bounce-dot {
    0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
    40%            { transform: translateY(-4px); opacity: 1; }
}

/* 列表動畫 */
.chat-anim-enter-active,
.chat-anim-leave-active {
    transition: all 0.3s ease;
}

.chat-anim-enter-from,
.chat-anim-leave-to {
    opacity: 0;
    transform: translateY(20px);
}
</style>
