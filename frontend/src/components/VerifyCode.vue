<template>
  <div class="verify-page">
    <div class="verify-container glass-panel animate-fade-in">
      <div class="verify-header text-center mb-4">
        <div class="icon-box mb-3">
          <i class="bi bi-envelope-check-fill"></i>
        </div>
        <h2 class="fw-bold">身分驗證</h2>
        <p class="text-muted">
          我們已將 6 位數驗證碼寄送至：<br />
          <span class="text-primary fw-bold">{{ targetEmail }}</span>
        </p>
      </div>

      <div class="input-group-custom mb-4">
        <i class="bi bi-shield-lock"></i>
        <input 
          v-model="code" 
          placeholder="請輸入 6 位數驗證碼" 
          maxlength="6" 
          class="code-input"
          @keyup.enter="handleVerify"
        />
      </div>

      <button 
        class="btn-submit mb-3" 
        @click="handleVerify" 
        :disabled="!isValidCode || loading"
      >
        <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
        {{ loading ? '驗證中...' : '確認驗證並登入' }}
        <i v-if="!loading" class="bi bi-arrow-right-short"></i>
      </button>

      <div v-if="message" class="message-box" :class="{ 'error': isError, 'success': !isError }">
        <i class="bi" :class="isError ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill'"></i>
        {{ message }}
      </div>

      <div class="footer-links text-center mt-4">
        <p class="small text-muted mb-2">沒收到郵件？請檢查垃圾信箱</p>
        <button class="btn btn-link btn-sm text-decoration-none" @click="goBack">
          返回登入頁面
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import request from '@/utils/request';
import { useAuthStore } from '@/stores/authStore';
import { useToastStore } from '@/stores/toastStore';

const props = defineProps({
  email: {
    type: String,
    default: ''
  }
});

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const toastStore = useToastStore();

const code = ref('');
const message = ref('');
const isError = ref(false);
const loading = ref(false);

const targetEmail = computed(() => props.email || route.query.email || '');

const isValidCode = computed(() => code.value.length === 6 && /^\d{6}$/.test(code.value));

onMounted(() => {
  if (!targetEmail.value) {
    toastStore.addToast('無效的驗證請求，請重新登入', 'warning');
    router.replace('/loginView');
  }
});

watch(code, (newValue) => {
  code.value = newValue.replace(/\D/g, '');
});

const handleVerify = async () => {
  if (!isValidCode.value || loading.value) return;

  try {
    loading.value = true;
    message.value = '';
    isError.value = false;

    const res = await request.post('/api/public/verify-code', {
      email: targetEmail.value,
      code: code.value
    });

    message.value = "驗證成功！正在為您導向...";
    toastStore.addToast('驗證成功！歡迎開啟您的探索之旅', 'success', '開通完成');

    // 1. 同步 Pinia Store 狀態
    await authStore.fetchUserInfo();

    // 2. 跳轉回首頁
    setTimeout(() => {
      router.replace('/');
    }, 800);

  } catch (err) {
    isError.value = true;
    message.value = err.response?.data || '驗證碼錯誤，請重新確認';
    toastStore.addToast(message.value, 'danger', '驗證失敗');
  } finally {
    loading.value = false;
  }
};

const goBack = () => {
  router.push('/loginView');
};
</script>

<style scoped>
.verify-page {
  min-height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: radial-gradient(circle at 10% 20%, rgba(99, 102, 241, 0.05) 0%, transparent 50%),
              radial-gradient(circle at 90% 80%, rgba(168, 85, 247, 0.05) 0%, transparent 50%);
}

.verify-container {
  width: 100%;
  max-width: 440px;
  padding: 40px;
  border-radius: 24px;
}

.icon-box {
  width: 64px;
  height: 64px;
  background: var(--primary-gradient);
  color: white;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  font-size: 1.8rem;
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.2);
}

.code-input {
  letter-spacing: 0.5rem;
  font-weight: bold;
  text-align: center;
  font-size: 1.2rem;
}

.input-group-custom {
  position: relative;
}

.input-group-custom i {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  font-size: 1.2rem;
  z-index: 5;
}

.input-group-custom input {
  width: 100%;
  padding: 14px 16px 14px 48px;
  border-radius: 12px;
  border: 1.5px solid rgba(0, 0, 0, 0.05);
  background: rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
}

.input-group-custom input:focus {
  border-color: var(--primary);
  background: white;
  outline: none;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.btn-submit {
  width: 100%;
  padding: 14px;
  background: var(--primary-gradient);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 10px 20px -5px rgba(99, 102, 241, 0.3);
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 15px 25px -5px rgba(99, 102, 241, 0.4);
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.message-box {
  padding: 12px;
  border-radius: 10px;
  text-align: center;
  font-size: 0.9rem;
  margin-top: 15px;
}

.message-box.error {
  background: #fee2e2;
  color: #ef4444;
  border: 1px solid #fecaca;
}

.message-box.success {
  background: #dcfce7;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.animate-fade-in {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>