<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import request from '@/utils/request';

const router = useRouter();
const authStore = useAuthStore();

// 定義儲值方案
const plans = ref([]);

// 當前選擇的方案
const selectedPlan = ref(null);
const loading = ref(false);

// 獲取方案資料
const fetchPackages = async () => {
  try {
    const response = await request.get('/api/user/packages');
    // 後端欄位為 pointsAmount 和 priceCash，對其進行轉換或直接使用
    plans.value = response.data.map(pkg => ({
      ...pkg,
      points: pkg.pointsAmount,
      price: pkg.priceCash
    }));
    
    // 預設選擇第一個方案
    if (plans.value.length > 0) {
      selectedPlan.value = plans.value[0];
    }
  } catch (error) {
    console.error('無法獲取儲值方案:', error);
  }
};

onMounted(() => {
  fetchPackages();
});

// 處理儲值邏輯
const handleTopUp = () => {
  if (!selectedPlan.value) return;
  router.push({
    name: 'Checkouts',
    query: {
      packageId: selectedPlan.value.id,
      points: selectedPlan.value.points,
      price: selectedPlan.value.price
    }
  });
};
</script>

<template>
  <div class="topup-page">
    <div class="container d-flex align-items-center justify-content-center min-vh-100">
      <div class="glass-panel p-5 topup-card animate-fade-in text-center">
        
        <!-- 標題區 -->
        <div class="mb-4">
          <div class="icon-wrapper mb-3 mx-auto">
            <i class="bi bi-credit-card-2-front-fill"></i>
          </div>
          <h2 class="fw-bold mb-2">點數儲值中心</h2>
          <p class="text-muted">選擇您想要儲值的積分方案</p>
        </div>

        <!-- 當前餘額顯示 -->
        <div class="balance-card mb-4 p-3 rounded-4">
          <span class="text-muted small fw-bold text-uppercase">目前持有積分</span>
          <div class="d-flex align-items-center justify-content-center gap-2 mt-1">
            <i class="bi bi-coin text-warning fs-4"></i>
            <span class="fs-2 fw-bold text-dark">{{ authStore.points }}</span>
            <span class="text-muted">Pts</span>
          </div>
        </div>

        <!-- 方案選擇表單 -->
        <div class="form-group text-start mb-4">
          <label class="form-label fw-bold text-muted small ms-1">選擇儲值方案</label>
          <div class="custom-select-wrapper">
            <select v-model="selectedPlan" class="form-select form-select-lg custom-select shadow-sm">
              <option v-for="plan in plans" :key="plan.points" :value="plan">
                {{ plan.points }} 積分 / ${{ plan.price }}
              </option>
            </select>
          </div>
        </div>

        <!-- 訂單摘要 -->
        <div v-if="selectedPlan" class="summary-box p-3 rounded-3 mb-4 text-start">
          <div class="d-flex justify-content-between mb-2">
            <span class="text-muted">儲值積分</span>
            <span class="fw-bold">{{ selectedPlan.points }} Pts</span>
          </div>
          <div class="d-flex justify-content-between align-items-center pt-2 border-top">
            <span class="fw-bold text-dark">應付金額</span>
            <span class="fs-4 fw-bold text-primary">${{ selectedPlan.price }}</span>
          </div>
        </div>

        <!-- 送出按鈕 -->
        <button 
          class="btn btn-primary w-100 py-3 rounded-pill fw-bold shadow-lg btn-confirm"
          @click="handleTopUp"
          :disabled="loading"
        >
          <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
          {{ loading ? '處理中...' : '立即購買' }}
        </button>

        <button @click="router.back()" class="btn btn-link text-muted mt-3 text-decoration-none small">
          取消並返回
        </button>

      </div>
    </div>
  </div>
</template>

<style scoped>
.topup-page {
  background: radial-gradient(circle at 50% 10%, rgba(99, 102, 241, 0.1) 0%, transparent 60%),
              radial-gradient(circle at 10% 90%, rgba(16, 185, 129, 0.05) 0%, transparent 60%);
  min-height: 100vh;
}

.topup-card {
  width: 100%;
  max-width: 480px;
  border-radius: var(--radius-xl);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.icon-wrapper {
  width: 80px;
  height: 80px;
  background: var(--primary-gradient);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  color: white;
  box-shadow: 0 10px 25px rgba(99, 102, 241, 0.3);
  transform: rotate(-5deg);
}

.balance-card {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

/* 自定義下拉選單樣式 */
.custom-select-wrapper {
  position: relative;
}

.custom-select {
  border-radius: var(--radius-md);
  border: 2px solid #e2e8f0;
  padding: 1rem;
  font-weight: 600;
  color: var(--text-main);
  cursor: pointer;
  transition: var(--transition);
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3e%3cpath fill='none' stroke='%23343a40' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='m2 5 6 6 6-6'/%3e%3c/svg%3e");
}

.custom-select:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.summary-box {
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}

.btn-confirm {
  transition: var(--transition);
}

.btn-confirm:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 15px 30px rgba(99, 102, 241, 0.4);
}
</style>
