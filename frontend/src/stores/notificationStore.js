import { defineStore } from 'pinia';
import request from '@/utils/request';

export const useNotificationStore = defineStore('notification', {
    state: () => ({
        notifications: [],
        unreadCount: 0,
        friendListUpdateTrigger: 0, // 新增：用於通知 FriendList 更新的訊號
        currentPage: 0,
        pageSize: 5,
        hasMore: true,
        isLoading: false
    }),

    actions: {
        // 1. 獲取通知列表 (支援載入更多)
        async fetchNotifications(isLoadMore = false) {
            if (this.isLoading) return;
            if (isLoadMore && !this.hasMore) return;

            this.isLoading = true;
            try {
                if (!isLoadMore) {
                    this.currentPage = 0;
                    this.notifications = []; // 重置列表
                    this.hasMore = true;
                } else {
                    this.currentPage++;
                }

                const res = await request.get('/api/user/notifications', {
                    params: {
                        page: this.currentPage,
                        size: this.pageSize
                    }
                });
                
                const pageData = res.data;
                const newItems = pageData.content; // 實際的項目在 content 陣列中
                
                // 判斷是否還有下一頁
                this.hasMore = !pageData.last; // 後端直接告訴我們是不是最後一頁

                if (isLoadMore) {
                    this.notifications.push(...newItems);
                } else {
                    this.notifications = newItems;
                }
            } catch (error) {
                console.error('獲取通知列表失敗:', error);
                if (isLoadMore) this.currentPage--; // 失敗則回退頁碼
            } finally {
                this.isLoading = false;
            }
        },

        // 2. 獲取未讀數量 (用於初始化鈴鐺紅點)
        async fetchUnreadCount() {
            try {
                const res = await request.get('/api/user/notifications/unread-count');
                this.unreadCount = res.data.count;
            } catch (error) {
                console.error('獲取未讀數量失敗:', error);
            }
        },

        // 3. 標記單則已讀
        async markAsRead(id) {
            // 先在前端樂觀更新，讓反應更即時
            const target = this.notifications.find(n => n.id === id);
            if (target && !target.isRead) {
                target.isRead = true;
                this.unreadCount = Math.max(0, this.unreadCount - 1);
            }

            try {
                await request.put(`/api/user/notifications/${id}/read`);
            } catch (error) {
                console.error('標記已讀失敗:', error);
                // 如果失敗可以考慮回復狀態，這裡暫略
            }
        },

        // 4. 全部標為已讀
        async markAllAsRead() {
            // 前端樂觀更新
            this.notifications.forEach(n => n.isRead = true);
            this.unreadCount = 0;

            try {
                await request.put('/api/user/notifications/read-all');
            } catch (error) {
                console.error('全部已讀失敗:', error);
            }
        },

        // 5. 接收 WebSocket 推送的新通知
        // payload 格式應與 NotificationResponseDto 一致
        handleNewNotification(payload) {
            // 智慧聚合前端處理：
            // 如果是貼文通知，且列表中已存在來自同一人的「未讀」貼文通知，先移除舊的
            if (payload.type === 'NEW_POST') {
                const existingIndex = this.notifications.findIndex(n => 
                    n.type === 'NEW_POST' && 
                    n.senderName === payload.senderName && // 或是比對 senderId (若 payload 有提供)
                    !n.isRead
                );

                if (existingIndex !== -1) {
                    this.notifications.splice(existingIndex, 1); // 移除舊的
                    this.unreadCount--; // 扣除舊的未讀數，稍後會再 +1，保持總數正確
                }
            } else if (payload.type === 'LIKE') {
                // ✨ 按讚通知聚合：
                // 檢查是否已存在針對「同一篇貼文」的未讀通知
                const existingIndex = this.notifications.findIndex(n => 
                    n.type === 'LIKE' && 
                    n.referenceId === payload.referenceId && 
                    !n.isRead
                );
                if (existingIndex !== -1) {
                    this.notifications.splice(existingIndex, 1); // 移除舊的聚合通知
                    this.unreadCount--; 
                }
            } else if (payload.type === 'COMMENT') {
                // ✨ 留言通知聚合：
                // 檢查是否已存在針對「同一篇貼文」的未讀通知
                const existingIndex = this.notifications.findIndex(n => 
                    n.type === 'COMMENT' && 
                    n.referenceId === payload.referenceId && 
                    !n.isRead
                );
                if (existingIndex !== -1) {
                    this.notifications.splice(existingIndex, 1); // 移除舊的聚合通知
                    this.unreadCount--; 
                }
            }

            // 將新通知加到列表最前面
            this.notifications.unshift(payload);
            // 未讀數 +1
            this.unreadCount++;
        },

        // 6. 觸發好友列表更新 (給 NotificationList 或其他元件呼叫)
        triggerFriendListUpdate() {
            this.friendListUpdateTrigger++;
        },

        // 7. 刪除單則通知
        async deleteNotification(id) {
            // 樂觀更新：先扣除未讀數(如果是未讀)並移除列表項目
            const target = this.notifications.find(n => n.id === id);
            if (target && !target.isRead) {
                this.unreadCount = Math.max(0, this.unreadCount - 1);
            }
            this.notifications = this.notifications.filter(n => n.id !== id);

            try {
                await request.delete(`/api/user/notifications/${id}`);
            } catch (error) {
                console.error('刪除通知失敗:', error);
            }
        },

        // 8. 清空所有通知
        async deleteAllNotifications() {
            try {
                await request.delete('/api/user/notifications');
                this.notifications = [];
                this.unreadCount = 0;
            } catch (error) {
                console.error('清空通知失敗:', error);
            }
        }
    }
});