<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useCartStore } from '../stores/cartStore';
import { useToastStore } from '../stores/toastStore';
import { useAuthStore } from '../stores/authStore';
import { storeToRefs } from 'pinia';

// 引入拆分後的 Components
import CartItemCard from '@/components/cart/CartItemCard.vue';
import ConfirmCheckoutModal from '@/components/cart/ConfirmCheckoutModal.vue';

// 取得 Store 實例
const cartStore = useCartStore();
const toastStore = useToastStore();
const authStore = useAuthStore();
const router = useRouter();
const showConfirmModal = ref(false);
const isCheckoutSuccess = ref(false); // 🌟 新增：結帳成功狀態

// 使用 storeToRefs 保持響應性解構
const { cartItems, totalPrice } = storeToRefs(cartStore);

// 初始化時獲取購物車內容
onMounted(() => {
    cartStore.fetchCart();
});

/**
 * 移除購物車項目
 * @param {number} id - 購物車項目的 ID (cartItemId)
 */
const removeFromCart = (id) => {
    cartStore.removeFromCart(id);
};

/**
 * 開啟結帳確認彈窗
 */
const checkout = () => {
    if (cartItems.value.length === 0) {
        toastStore.addToast('購物車是空的，快去選購行程吧！', 'warning', '結帳提醒');
        return;
    }
    showConfirmModal.value = true;
};

/**
 * 執行結帳
 */
const confirmCheckout = async () => {
    showConfirmModal.value = false;
    try {
        await cartStore.checkout();
        // 結帳成功後刷新使用者點數
        authStore.fetchUserInfo();
        
        // 🌟 切換到成功畫面
        isCheckoutSuccess.value = true;
        toastStore.addToast('結帳成功！已為您解鎖行程', 'success', '恭喜您');
        
    } catch (error) {
        // 錯誤已在 store 中處理並顯示 toast
        console.error('Checkout failed:', error);
    }
};

const goToPurchased = () => {
    router.push({ path: '/profile', query: { tab: 'purchased' } });
};
</script>

<template>
    <div class="container py-5 animate-fade-in relative">
        <!-- 頁面標題 -->
        <div class="cart-header text-center mb-5">
            <h1 class="display-5 fw-bold text-dark">我的行程清單 🛒</h1>
            <p class="text-muted">準備好開啟下一段精彩旅程了嗎？</p>
        </div>

        <!-- 🌟 結帳成功畫面 (Celebration View) -->
        <div v-if="isCheckoutSuccess" class="checkout-success-container py-5 text-center">
            <div class="success-card glass-panel p-5 animate-pop shadow-2xl">
                <div class="celebration-icon mb-4">
                    <div class="confetti-container">
                        <i class="bi bi-patch-check-fill text-success display-1"></i>
                    </div>
                </div>
                <h1 class="fw-black text-dark mb-2">結帳成功！🎉</h1>
                <p class="text-muted mb-4 lead">
                    太棒了！您的下一段精彩旅程已經成功解鎖。<br>
                    可以在您的「個人頁面」中查看完整細節。
                </p>
                <div class="d-flex flex-column flex-sm-row justify-content-center gap-3">
                    <button @click="goToPurchased" class="btn btn-primary btn-lg rounded-pill px-5 py-3 shadow-lg">
                        查看我的行程 <i class="bi bi-map ms-2"></i>
                    </button>
                    <router-link to="/" class="btn btn-outline-secondary btn-lg rounded-pill px-5 py-3">
                        繼續探索商城
                    </router-link>
                </div>
            </div>
        </div>

        <!-- 購物車有內容時顯示列表 (非成功狀態) -->
        <div v-else-if="cartItems.length > 0" class="row justify-content-center">
            <div class="col-lg-9 col-xl-8">
                <!-- 購物車項目列表區域 -->
                <div class="cart-card glass-panel mb-4 p-4">
                    <div class="cart-list">
                        <!-- 遍歷顯示每一個行程項目 -->
                        <CartItemCard 
                            v-for="item in cartItems" 
                            :key="item.id" 
                            :item="item" 
                            @remove="removeFromCart" 
                        />
                    </div>
                </div>

                <!-- 結帳匯總與操作區 (吸底效果) -->
                <div class="checkout-section p-4 glass-panel sticky-bottom-custom shadow-xl">
                    <div class="row align-items-center">
                        <!-- 總計資訊 -->
                        <div class="col-sm-6 mb-3 mb-sm-0">
                            <div class="summary-item">
                                <span class="text-muted me-2">總計行程數:</span>
                                <span class="fw-bold">{{ cartItems.length }} 個項目</span>
                            </div>
                            <div class="summary-total mt-1">
                                <h3 class="fw-bold text-primary mb-0">NT$ {{ totalPrice.toLocaleString() }}</h3>
                            </div>
                        </div>
                        <!-- 操作按鈕 -->
                        <div class="col-sm-6 text-sm-end">
                            <div class="d-grid d-sm-inline-flex gap-2">
                                <router-link to="/" class="btn btn-outline-secondary rounded-pill px-4">
                                    繼續選購
                                </router-link>
                                <button class="btn btn-primary btn-lg rounded-pill px-5 shadow-lg" @click="checkout">
                                    確認結帳 <i class="bi bi-credit-card-2-front ms-2"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 購物車為空時顯示的狀態 -->
        <div v-else class="text-center py-5">
            <div class="empty-cart-container animate-fade-in shadow-lg">
                <div class="empty-cart-graphic mb-4">
                    <i class="bi bi-cart3 text-light-subtle"></i>
                </div>
                <h2 class="fw-bold text-dark mb-3">您的購物車是空的</h2>
                <p class="text-secondary mb-4 mx-auto" style="max-width: 400px;">
                    快去探索我們為您準備的各式精選行程，讓您的假期充滿回憶！
                </p>
                <router-link to="/" class="btn btn-primary btn-lg rounded-pill px-5 py-3 shadow-lg">
                    瀏覽所有行程
                </router-link>
            </div>
        </div>

        <!-- 🌟 自定義結帳確認彈窗 -->
        <ConfirmCheckoutModal 
            :showConfirmModal="showConfirmModal"
            :cartItemsCount="cartItems.length"
            :totalPrice="totalPrice"
            @close="showConfirmModal = false"
            @confirmCheckout="confirmCheckout"
        />
    </div>
</template>

<style scoped>
/* --- 購物車卡片容器 --- */
.cart-card {
    border-radius: var(--radius-lg);
}



/* --- 結帳欄吸底與面板樣式 --- */
.checkout-section {
    border-radius: var(--radius-lg);
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(255, 255, 255, 0.4);
}

.sticky-bottom-custom {
    position: sticky;
    bottom: 2rem;
    z-index: 10;
}

/* --- 結帳成功樣式 --- */
.checkout-success-container {
    max-width: 600px;
    margin: 0 auto;
}

.success-card {
    border-radius: 40px;
    background: rgba(255, 255, 255, 0.95);
    border: 1px solid rgba(255, 255, 255, 0.4);
}

.celebration-icon {
    position: relative;
    display: inline-block;
}

.fw-black { font-weight: 900; }

.shadow-2xl {
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.empty-cart-container {
    background: white;
    padding: 5rem 2rem;
    border-radius: var(--radius-lg);
    border: 1px solid #f1f5f9;
}

.empty-cart-graphic {
    font-size: 8rem;
    line-height: 1;
    background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
}

.animate-pop {
    animation: pop 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes pop {
    from { opacity: 0; transform: scale(0.8) translateY(20px); }
    to { opacity: 1; transform: scale(1) translateY(0); }
}

</style>
