<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import request from '@/utils/request';
import { useAuthStore } from '@/stores/authStore';
import { useToastStore } from '@/stores/toastStore';
import { googleOAuthUrl } from '@/config/appConfig.js';

const authStore = useAuthStore();
const toastStore = useToastStore();
const router = useRouter();
const route = useRoute();

const isLogin = ref(true);
const message = ref('');

const loginForm = reactive({ username: '', password: '' });
const registerForm = reactive({ username: '', password: '', email: '' });

// --- 💡 1. 修改 Google 登入回傳處理 ---
onMounted(async () => {
  // 🌟 現在後端導向過來時，網址會帶的是 ?oauth2=success
  const oauthSuccess = route.query.oauth2 === 'success';
  const errorFromUrl = route.query.error;

  // 處理封鎖邏輯
  if (errorFromUrl === 'blocked') {
    toastStore.addToast('您的 Google 帳號已被封鎖，無法進入系統！', 'danger', '登入限制');
    router.replace({ query: {} });
    return;
  }

  // 處理新註冊/Google 登入需要驗證的情況
  if (route.query.verify === 'pending' && route.query.email) {
    router.replace({ 
      path: '/verify', 
      query: { email: route.query.email } 
    });
    return;
  }

  // 🌟 核心改變：不再拿 token，只要確認成功標籤
  if (oauthSuccess) {
    message.value = "Google 登入成功！正在同步資料...";

    try {
      // 因為 Cookie 已經在瀏覽器裡了，我們直接呼叫 whoami 拿使用者資訊
      // 這一步是為了讓 Pinia Store 更新狀態
      await authStore.fetchUserInfo();

      message.value = "驗證成功！正在跳轉...";
      setTimeout(() => {
        router.replace('/');
      }, 500);
    } catch (err) {
      message.value = "身分驗證失敗，請重新登入";
    }
  }
});

// --- 🌟 新增：處理驗證碼送出 ---
const handleVerifyCode = async () => {
  if (!verifyCode.value || verifyCode.value.length !== 6) {
    toastStore.addToast('請輸入 6 位數驗證碼', 'warning');
    return;
  }

  try {
    message.value = "驗證中...";
    const res = await request.post('/api/public/verify-code', {
      email: emailToVerify.value,
      code: verifyCode.value
    });

    toastStore.addToast('驗證成功！歡迎開啟您的行程規劃', 'success', '開通成功');
    
    // 驗證成功後，後端已經把 JWT 寫進 Cookie 了，我們比照登入成功流程
    await authStore.fetchUserInfo();
    
    setTimeout(() => {
      router.replace('/');
    }, 500);
  } catch (err) {
    message.value = err.response?.data || "驗證失敗，請重新確認驗證碼";
  }
};

// --- 💡 2. 修改一般帳密登入 ---
const handleLogin = async () => {
  try {
    message.value = '';
    // 🌟 現在後端回傳的是 ResponseEntity，不再包含 token 字串
    const res = await request.post('/api/public/login', loginForm);

    // 只要沒進 catch，就代表登入成功（因為 Cookie 已經被瀏覽器收下了）
    message.value = "登入成功！正在跳轉...";

    // 🌟 同步更新 Pinia Store 的狀態
    await authStore.fetchUserInfo();

    setTimeout(() => {
      router.push('/');
    }, 500);

  } catch (err) {
    const status = err.response?.status;
    const backendMsg = err.response?.data;

    if (status === 403) {
      const errorMsg = typeof backendMsg === 'string' ? backendMsg : (backendMsg?.message || backendMsg?.error || "您的帳號已被封鎖");
      toastStore.addToast('⚠️ 登入失敗：' + errorMsg, 'danger', '登入失敗');
      message.value = "帳號已被封鎖";
    } else if (status === 401) {
      message.value = "登入失敗：帳號或密碼錯誤";
    } else {
      const errorMsg = typeof backendMsg === 'string' ? backendMsg : (backendMsg?.message || backendMsg?.error || "連線伺服器失敗");
      message.value = "登入失敗：" + errorMsg;
    }
  }
};
const handleGoogleLogin = () => {
  window.location.href = googleOAuthUrl;
};

const handleRegister = async () => {
  try {
    const res = await request.post('/api/public/registerPost', registerForm);
    message.value = res.data;
    if (res.data === '註冊成功' || res.status === 200) {
      setTimeout(() => {
        isLogin.value = true;
        message.value = "註冊成功，請登入";
      }, 1000);
    }
  } catch (err) {
    message.value = "註冊出錯：" + (err.response?.data || '請檢查網路連線');
  }
};
</script>
<template>
  <div class="login-wrapper">
    <div v-if="route.query.token" class="loading-overlay animate-fade-in">
      <div class="spinner-box">
        <div class="spinner"></div>
        <p>Google 身分驗證中，請稍候...</p>
      </div>
    </div>

    <div v-else class="auth-container glass-panel animate-fade-in">
      <div class="auth-header text-center mb-4">
        <div class="logo-box mb-3">
          <i class="bi bi-geo-alt-fill"></i>
        </div>
        <h2>{{ isLogin ? '歡迎回來' : '加入我們' }}</h2>
        <p class="text-muted">{{ isLogin ? '請登入您的帳號以繼續' : '建立帳號開啟您的旅程' }}</p>
      </div>

      <div class="tabs-container mb-4">
        <div class="tabs-bg">
          <div class="tab-slider" :class="{ 'right': !isLogin }"></div>
          <button @click="isLogin = true" :class="{ active: isLogin }">登入</button>
          <button @click="isLogin = false" :class="{ active: !isLogin }">註冊</button>
        </div>
      </div>

      <div class="form-content">
        <div v-if="isLogin" class="form-box animate-fade-in">
          <div class="input-group-custom mb-3">
            <i class="bi bi-person"></i>
            <input v-model="loginForm.username" placeholder="帳號 / 使用者名稱" />
          </div>
          <div class="input-group-custom mb-4">
            <i class="bi bi-lock"></i>
            <input v-model="loginForm.password" type="password" placeholder="密碼" />
          </div>
          <button class="btn-submit mb-3" @click="handleLogin">
            立即登入
            <i class="bi bi-arrow-right-short"></i>
          </button>

          <div class="divider">
            <span>或使用第三方登入</span>
          </div>

          <button class="btn-google" @click="handleGoogleLogin">
            <div class="google-content">
              <i class="bi bi-google"></i>
              <span>以 Google 帳號登入</span>
            </div>
          </button>
        </div>

        <div v-else class="form-box animate-fade-in">
          <div class="input-group-custom mb-3">
            <i class="bi bi-person"></i>
            <input v-model="registerForm.username" placeholder="設定帳號" />
          </div>
          <div class="input-group-custom mb-3">
            <i class="bi bi-envelope"></i>
            <input v-model="registerForm.email" placeholder="電子信箱" />
          </div>
          <div class="input-group-custom mb-4">
            <i class="bi bi-lock"></i>
            <input v-model="registerForm.password" type="password" placeholder="設定密碼" />
          </div>
          <button class="btn-submit" @click="handleRegister">
            完成註冊
            <i class="bi bi-check-lg"></i>
          </button>
        </div>
      </div>

      <p v-if="message" class="message shadow-sm"
        :class="{ 'error': !message.includes('成功'), 'success': message.includes('成功') }">
        <i class="bi" :class="message.includes('成功') ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill'"></i>
        {{ message }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.loading-overlay {
  width: 100%;
  max-width: 440px;
  height: 450px;
  /* 稍微固定高度讓跳轉感覺更穩定 */
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(15px);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
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

/* 基本佈局 */
.login-wrapper {
  min-height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: radial-gradient(circle at 10% 20%, rgba(99, 102, 241, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 90% 80%, rgba(59, 130, 246, 0.05) 0%, transparent 50%);
}

.auth-container {
  width: 100%;
  max-width: 440px;
  padding: 40px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.logo-box {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #6366f1 0%, #3b82f6 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  color: white;
  font-size: 1.8rem;
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.2);
}

/* 分隔線樣式 */
.divider {
  display: flex;
  align-items: center;
  text-align: center;
  margin: 20px 0;
  color: #888;
  font-size: 0.85rem;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.divider:not(:empty)::before {
  margin-right: 12px;
}

.divider:not(:empty)::after {
  margin-left: 12px;
}

/* 按鈕樣式 */
.btn-submit {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.3s;
}

/* 修改 Google 按鈕樣式 */
.btn-google {
  width: 100%;
  padding: 10px 0;
  /* 縮減上下內距，看起來更精鍊 */
  background: white;
  color: #5f6368;
  /* 使用更接近 Google 規範的灰色 */
  border: 1px solid #dadce0;
  /* 顏色調淺，並讓邊框更細 */
  border-radius: 50px;
  /* 改為全圓角藥丸形狀，如圖所示 */
  font-weight: 500;
  /* 字體稍微減輕一點點 */
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;
}

.google-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  /* 圖標與文字的間距 */
}

/* 修改 Google 按鈕樣式 */
.btn-google {
  width: 100%;
  padding: 10px 0;
  /* 縮減上下內距，看起來更精鍊 */
  background: white;
  color: #5f6368;
  /* 使用更接近 Google 規範的灰色 */
  border: 1px solid #dadce0;
  /* 顏色調淺，並讓邊框更細 */
  border-radius: 50px;
  /* 改為全圓角藥丸形狀，如圖所示 */
  font-weight: 500;
  /* 字體稍微減輕一點點 */
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;
}

.google-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  /* 圖標與文字的間距 */
}

/* 讓 Google 圖標顏色更顯眼 */
.btn-google i {
  color: #4285F4;
  /* 使用 Google Blue */
  font-size: 1.1rem;
}

.btn-google:hover {
  background-color: #f8f9fa;
  border-color: #d2d4d7;
  box-shadow: 0 1px 2px 0 rgba(60, 64, 67, .3), 0 1px 3px 1px rgba(60, 64, 67, .15);
  transform: translateY(-1px);
}

.btn-google:active {
  background-color: #f1f3f4;
  transform: translateY(0);
}

/* 輸入框樣式 */
.input-group-custom {
  position: relative;
  display: flex;
  align-items: center;
}

.input-group-custom i {
  position: absolute;
  left: 16px;
  color: #94a3b8;
}

.input-group-custom input {
  width: 100%;
  padding: 12px 16px 12px 48px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.8);
}

/* Tabs 與 Slider */
.tabs-bg {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  display: flex;
  position: relative;
  padding: 4px;
}

.tab-slider {
  position: absolute;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: white;
  border-radius: 10px;
  transition: 0.3s;
}

.tab-slider.right {
  transform: translateX(100%);
}

.tabs-bg button {
  flex: 1;
  z-index: 1;
  border: none;
  background: none;
  font-weight: 600;
  cursor: pointer;
}

.active {
  color: #6366f1;
}

.tab-slider.right {
  transform: translateX(100%);
}

.tabs-bg button {
  flex: 1;
  position: relative;
  z-index: 2;
  background: none;
  border: none;
  font-weight: 600;
  color: var(--text-muted);
  transition: var(--transition);
}

.tabs-bg button.active {
  color: var(--primary);
}

/* Form Styling */
.input-group-custom {
  position: relative;
  display: flex;
  align-items: center;
}

.input-group-custom i {
  position: absolute;
  left: 16px;
  color: var(--text-muted);
  font-size: 1.1rem;
  transition: var(--transition);
}

.input-group-custom input {
  width: 100%;
  padding: 12px 16px 12px 48px;
  border-radius: 12px;
  border: 1.5px solid rgba(0, 0, 0, 0.05);
  background: rgba(255, 255, 255, 0.5);
  transition: var(--transition);
  font-size: 0.95rem;
}

.input-group-custom input:focus {
  border-color: var(--primary);
  background: white;
  outline: none;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.input-group-custom input:focus+i {
  color: var(--primary);
}

.btn-submit {
  width: 100%;
  padding: 14px;
  background: var(--primary-gradient);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: var(--transition);
  box-shadow: 0 10px 20px -5px rgba(99, 102, 241, 0.3);
}

.btn-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 25px -5px rgba(99, 102, 241, 0.4);
  filter: brightness(1.05);
}

.btn-submit:active {
  transform: translateY(0);
}

/* Message Styling */
.message {
  margin-top: 20px;
  padding: 10px;
  border-radius: 8px;
  text-align: center;
}

.error {
  background: #fee2e2;
  color: #ef4444;
}

.success {
  background: #dcfce7;
  color: #16a34a;
}
</style>
