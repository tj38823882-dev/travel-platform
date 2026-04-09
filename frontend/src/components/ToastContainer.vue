<script setup>
import { useToastStore } from '../stores/toastStore';
import { storeToRefs } from 'pinia';

// 初始化通知 Store
const toastStore = useToastStore();
const { toasts } = storeToRefs(toastStore);

/**
 * 根據通知類型返回對應的 CSS 類別
 * @param {string} type - 通知類型 (success, danger, warning, info)
 */
const getToastClass = (type) => {
    switch (type) {
        case 'success': return 'text-bg-success';
        case 'danger': return 'text-bg-danger';
        case 'warning': return 'text-bg-warning';
        case 'info': return 'text-bg-info';
        default: return 'text-bg-primary';
    }
};
</script>

<template>
    <!-- 全域通知容器：固定在右下角 -->
    <div class="toast-container position-fixed bottom-0 end-0 p-4">
        <!-- 列表過渡動畫 -->
        <TransitionGroup name="toast-slide">
            <div v-for="toast in toasts" :key="toast.id" class="premium-toast d-flex align-items-center mb-3"
                :class="getToastClass(toast.type)" role="alert">
                <!-- 根據類型顯示對應圖示 -->
                <div class="toast-icon-box">
                    <i v-if="toast.type === 'success'" class="bi bi-check-circle-fill"></i>
                    <i v-else-if="toast.type === 'warning'" class="bi bi-exclamation-triangle-fill"></i>
                    <i v-else-if="toast.type === 'danger'" class="bi bi-x-circle-fill"></i>
                    <i v-else class="bi bi-info-circle-fill"></i>
                </div>
                <!-- 通知內容區 (標題與訊息) -->
                <div class="toast-content flex-grow-1 mx-3">
                    <div class="toast-title small fw-bold" v-if="toast.title && toast.title !== '通知'">{{ toast.title }}
                    </div>
                    <div class="toast-message small">{{ toast.message }}</div>
                </div>
                <!-- 關閉按鈕 -->
                <button type="button" class="btn-close-custom" @click="toastStore.removeToast(toast.id)">
                    <i class="bi bi-x"></i>
                </button>
            </div>
        </TransitionGroup>
    </div>
</template>

<style scoped>
.toast-container {
    z-index: 100000;
}

/* --- 進階通知外觀樣式 (毛玻璃+陰影) --- */
.premium-toast {
    min-width: 300px;
    max-width: 400px;
    padding: 12px 18px;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(10px);
    border-radius: var(--radius-md);
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
    border-left: 4px solid var(--primary);
    animation: toastScale 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* 根據不同類型調整邊框顏色 */
.text-bg-success {
    border-left-color: #10b981;
}

.text-bg-warning {
    border-left-color: #f59e0b;
}

.text-bg-danger {
    border-left-color: #ef4444;
}

.text-bg-info {
    border-left-color: #3b82f6;
}

.toast-icon-box {
    font-size: 1.25rem;
    display: flex;
    align-items: center;
}

/* 根據不同類型調整圖示顏色 */
.text-bg-success .toast-icon-box {
    color: #10b981;
}

.text-bg-warning .toast-icon-box {
    color: #f59e0b;
}

.text-bg-danger .toast-icon-box {
    color: #ef4444;
}

.text-bg-info .toast-icon-box {
    color: #3b82f6;
}

.toast-title {
    color: #ffffff;
    font-size: 1.1rem;
    /* 加大標題字體 */
}

.toast-message {
    color: #ffffff;
    /* 加深文字顏色 */
    font-size: 1rem;
    /* 加大內容字體 */
}

/* --- 自定義關閉按鈕樣式 --- */
.btn-close-custom {
    background: none;
    border: none;
    color: #94a3b8;
    font-size: 1.25rem;
    padding: 0;
    line-height: 1;
    transition: var(--transition);
}

.btn-close-custom:hover {
    color: #1e293b;
    transform: rotate(90deg);
}

/* --- 滑入過渡動畫 --- */
.toast-slide-enter-active,
.toast-slide-leave-active {
    transition: all 0.4s ease;
}

.toast-slide-enter-from {
    opacity: 0;
    transform: translateX(50px);
}

.toast-slide-leave-to {
    opacity: 0;
    transform: scale(0.9) translateY(-20px);
}

/* --- 縮放進場動畫 --- */
@keyframes toastScale {
    from {
        opacity: 0;
        transform: scale(0.8);
    }

    to {
        opacity: 1;
        transform: scale(1);
    }
}
</style>
