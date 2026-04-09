import { defineStore } from 'pinia';
import { ref } from 'vue';

// --- 訊息通知 (Toast) Store ---
export const useToastStore = defineStore('toast', () => {
    // 活躍中的通知清單
    const toasts = ref([]);

    /**
     * 新增一則通知訊息
     * @param {string} message - 顯示內容
     * @param {string} type - 類型 ('success', 'danger', 'warning', 'info')
     * @param {string} title - 標題
     */
    const addToast = (message, type = 'info', title = '通知') => {
        const id = Date.now();
        toasts.value.push({ id, message, type, title });

        // Auto remove after 3 seconds
        setTimeout(() => {
            removeToast(id);
        }, 3000);
    };

    /**
     * 移除指定 ID 的通知
     * @param {number} id - 通知 ID
     */
    const removeToast = (id) => {
        toasts.value = toasts.value.filter(t => t.id !== id);
    };

    return {
        toasts,
        addToast,
        removeToast
    };
});
