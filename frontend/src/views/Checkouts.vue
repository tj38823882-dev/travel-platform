<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { useToastStore } from '@/stores/toastStore';
import request from '@/utils/request';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const toastStore = useToastStore();

const packageId = ref(route.query.packageId);
const points = ref(route.query.points || 0);
const price = ref(route.query.price || 0);
const loading = ref(false);
const paymentMethod = ref('line_pay'); // 預設改為 LINE Pay

const confirmPayment = async () => {
  if (!packageId.value) {
    toastStore.addToast('方案資訊缺失，請重新從儲值頁面進入', 'warning', '提醒');
    return;
  }

  loading.value = true;
  
  try {
    if (paymentMethod.value === 'line_pay') {
      // 呼叫後端發起 LINE Pay 請求
      const response = await request.post('/api/user/payment/request', {
        packageId: Number(packageId.value)
      });
      
      if (response.data && response.data.paymentUrl) {
        // 跳轉到 LINE Pay 支付頁面
        window.location.href = response.data.paymentUrl;
      } else {
        throw new Error('無法取得支付連結');
      }
    } else {
      // 模擬其他支付過程 (之後如需實作可在此擴充)
      setTimeout(() => {
        authStore.addPoints(Number(points.value));
        loading.value = false;
        toastStore.addToast(`支付成功！(模擬) 已存入 ${points.value} 點數。`, 'success', '儲值完成');
        router.push('/');
      }, 1500);
    }
  } catch (error) {
    console.error('支付發起失敗:', error);
    toastStore.addToast('支付發起失敗: ' + (error.response?.data || error.message), 'danger', '支付錯誤');
    loading.value = false;
  }
};

onMounted(() => {
  if (!points.value || !price.value) {
    router.push('/TopUpView');
  }
});
</script>

<template>
  <div class="checkout-page">
    <div class="container py-5">
      <div class="row justify-content-center">
        <div class="col-lg-8">
          <div class="glass-panel p-4 p-md-5 animate-slide-up">
            <h2 class="mb-4 fw-bold d-flex align-items-center">
              <i class="bi bi-shield-check text-success me-2"></i>
              結帳確認
            </h2>
            
            <div class="row g-4">
              <!-- 訂單內容 -->
              <div class="col-md-6">
                <div class="card border-0 bg-light rounded-4 p-4 h-100">
                  <h5 class="fw-bold mb-3">訂單摘要</h5>
                  <div class="d-flex justify-content-between mb-2">
                    <span class="text-muted">儲值點數</span>
                    <span class="fw-bold">{{ points }} Pts</span>
                  </div>
                  <div class="d-flex justify-content-between align-items-center">
                    <span class="fs-5 fw-bold">總計金額</span>
                    <span class="fs-3 fw-bold text-primary">${{ price }}</span>
                  </div>
                </div>
              </div>

              <!-- 支付方式 -->
              <div class="col-md-6">
                <div class="payment-methods">
                  <h5 class="fw-bold mb-3">選擇支付方式</h5>
                  <div 
                    class="payment-option p-3 mb-2 rounded-3 border d-flex align-items-center gap-3"
                    :class="{ active: paymentMethod === 'line_pay' }"
                    @click="paymentMethod = 'line_pay'">
                    <div class="icon fs-4 text-success"><i class="bi bi-chat-fill"></i></div>
                    <div class="flex-grow-1">LINE Pay</div>
                    <div class="check"><i class="bi bi-check-circle-fill text-primary"></i></div>
                </div>
                </div>
              </div>
            </div>

            <hr class="my-4 op-1">

            <!-- 底部操作 -->
            <div class="d-flex flex-column flex-md-row gap-3 align-items-center justify-content-between">
              <button @click="router.back()" class="btn btn-link text-muted text-decoration-none">
                <i class="bi bi-arrow-left me-1"></i> 返回修改方案
              </button>
              <button 
                class="btn btn-primary px-5 py-3 rounded-pill fw-bold shadow-lg btn-pay"
                @click="confirmPayment"
                :disabled="loading"
              >
                <div v-if="loading" class="spinner-border spinner-border-sm me-2"></div>
                {{ loading ? '處理中...' : '確認支付並儲值' }}
              </button>
            </div>
          </div>

          <!-- 安全提示 -->
          <div class="text-center mt-4 text-muted small">
            <i class="bi bi-lock-fill me-1"></i>
            您的支付資訊經過安全加密，請放心支付
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.checkout-page {
  min-height: 100vh;
  background-color: #f8fafc;
  background-image: radial-gradient(at 0% 0%, rgba(99, 102, 241, 0.05) 0, transparent 50%),
                    radial-gradient(at 50% 0%, rgba(16, 185, 129, 0.05) 0, transparent 50%);
}

.glass-panel {
  background: white;
  border-radius: 2rem;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.payment-option {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid #f1f5f9 !important;
}

.payment-option:hover {
  background-color: #f8fafc;
  border-color: #e2e8f0 !important;
}

.payment-option.active {
  border-color: var(--primary) !important;
  background-color: rgba(99, 102, 241, 0.05);
}

.payment-option .check {
  opacity: 0;
  transform: scale(0.5);
  transition: all 0.2s ease;
}

.payment-option.active .check {
  opacity: 1;
  transform: scale(1);
}

.btn-pay {
  min-width: 200px;
  transition: all 0.3s ease;
}

.btn-pay:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(99, 102, 241, 0.4);
}

.animate-slide-up {
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
