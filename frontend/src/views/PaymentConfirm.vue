<script setup>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { useAuthStore } from '@/stores/authStore';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const status = ref('processing'); // processing, success, error
const errorMessage = ref('');

onMounted(async () => {
    const transactionId = route.query.transactionId;
    const orderId = route.query.orderId;

    if (!transactionId || !orderId) {
        status.value = 'error';
        errorMessage.value = '無效的支付參數';
        return;
    }

    try {
        const response = await request.post('/api/user/payment/confirm', {
            transactionId: transactionId,
            orderId: orderId
        });

        if (response.data.status === 'success') {
            status.value = 'success';
            // 重新整理用戶點數資訊
            await authStore.fetchUserInfo(); 
        } else {
            throw new Error(response.data.message || '支付確認失敗');
        }
    } catch (error) {
        console.error('Payment confirmation error:', error);
        status.value = 'error';
        errorMessage.value = error.response?.data?.message || error.message || '支付通訊失敗';
    }
});

const goHome = () => router.push('/');
const retry = () => router.push('/TopUpView');
</script>

<template>
    <div class="confirm-page d-flex align-items-center justify-content-center min-vh-100">
        <div class="glass-panel p-5 text-center shadow-lg" style="max-width: 500px; width: 90%;">
            
            <!-- 處理中 -->
            <div v-if="status === 'processing'" class="animate-fade">
                <div class="spinner-border text-primary mb-4" role="status" style="width: 3rem; height: 3rem;">
                    <span class="visually-hidden">Loading...</span>
                </div>
                <h3 class="fw-bold">正在確認支付結果...</h3>
                <p class="text-muted">請勿關閉視窗，我們正在與 LINE Pay 進行最終核對。</p>
            </div>

            <!-- 成功 -->
            <div v-if="status === 'success'" class="animate-bounce-in">
                <div class="success-icon mb-4">
                    <i class="bi bi-check-circle-fill text-success" style="font-size: 5rem;"></i>
                </div>
                <h2 class="fw-bold mb-3">支付成功！</h2>
                <p class="text-muted mb-4">您的訂單已處理完成，點數已成功存入您的帳戶。</p>
                <div class="points-badge mb-4 py-2 px-4 rounded-pill d-inline-block bg-success bg-opacity-10 text-success fw-bold">
                    <i class="bi bi-coin me-1"></i> 您現在擁有 {{ authStore.points }} 積分
                </div>
                <button @click="goHome" class="btn btn-primary w-100 py-3 rounded-pill fw-bold shadow">
                    返回首頁
                </button>
            </div>

            <!-- 失敗 -->
            <div v-if="status === 'error'" class="animate-fade">
                <div class="error-icon mb-4">
                    <i class="bi bi-exclamation-triangle-fill text-danger" style="font-size: 5rem;"></i>
                </div>
                <h2 class="fw-bold mb-3">支付發生錯誤</h2>
                <p class="text-danger mb-4">{{ errorMessage }}</p>
                <div class="d-grid gap-2">
                    <button @click="retry" class="btn btn-outline-primary py-3 rounded-pill fw-bold">
                        重新嘗試
                    </button>
                    <button @click="goHome" class="btn btn-link text-muted">
                        返回首頁
                    </button>
                </div>
            </div>

        </div>
    </div>
</template>

<style scoped>
.confirm-page {
    background: linear-gradient(135deg, #f0f4ff 0%, #e6fffa 100%);
}

.glass-panel {
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(10px);
    border-radius: 2rem;
    border: 1px solid rgba(255, 255, 255, 0.5);
}

.animate-fade {
    animation: fadeIn 0.5s ease;
}

.animate-bounce-in {
    animation: bounceIn 0.8s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

@keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
}

@keyframes bounceIn {
    0% { transform: scale(0.3); opacity: 0; }
    50% { transform: scale(1.05); opacity: 0.8; }
    70% { transform: scale(0.9); }
    100% { transform: scale(1); opacity: 1; }
}
</style>
