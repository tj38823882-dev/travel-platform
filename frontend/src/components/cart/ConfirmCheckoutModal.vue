<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  showConfirmModal: {
    type: Boolean,
    required: true
  },
  cartItemsCount: {
    type: Number,
    required: true
  },
  totalPrice: {
    type: Number,
    required: true
  }
});

const emit = defineEmits(['close', 'confirmCheckout']);
</script>

<template>
  <Teleport to="body">
    <Transition name="fade-scale">
      <div v-if="showConfirmModal" class="confirm-modal-overlay" @click.self="emit('close')">
        <div class="confirm-modal-content glass-panel p-5 animate-pop">
          <div class="text-center mb-4">
            <div class="confirm-icon mb-3">
              <i class="bi bi-bag-check-fill"></i>
            </div>
            <h3 class="fw-bold text-dark">確認結帳</h3>
            <p class="text-muted">即將為您安排這份精彩行程</p>
          </div>
          
          <div class="confirm-summary bg-light p-4 rounded-4 mb-4">
            <div class="d-flex justify-content-between mb-2">
              <span class="text-muted">行程總計</span>
              <span class="fw-bold">{{ cartItemsCount }} 個項目</span>
            </div>
            <div class="d-flex justify-content-between align-items-center pt-2 border-top">
              <span class="fw-bold">應付金額</span>
              <span class="fs-4 fw-black text-primary">NT$ {{ totalPrice.toLocaleString() }}</span>
            </div>
          </div>

          <div class="d-flex gap-3 mt-4">
            <button class="btn btn-outline-secondary flex-grow-1 rounded-pill py-3 fw-bold" @click="emit('close')">
              暫時不要
            </button>
            <button class="btn btn-primary flex-grow-1 rounded-pill py-3 fw-bold shadow-lg" @click="emit('confirmCheckout')">
              確定結帳
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* 🌟 確認彈窗樣式 🌟 */
.confirm-modal-overlay {
    position: fixed;
    top: 0; left: 0; width: 100%; height: 100%;
    background: rgba(15, 23, 42, 0.6);
    backdrop-filter: blur(8px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 10000;
}

.confirm-modal-content {
    width: 90%;
    max-width: 480px;
    background: white;
    border-radius: 32px;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.confirm-icon {
    width: 80px;
    height: 80px;
    background: var(--primary-gradient);
    border-radius: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto;
    color: white;
    font-size: 2.5rem;
    box-shadow: 0 10px 20px rgba(99, 102, 241, 0.3);
}

.fw-black {
    font-weight: 900;
}

.animate-pop {
    animation: pop 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes pop {
    from { transform: scale(0.8); opacity: 0; }
    to { transform: scale(1); opacity: 1; }
}

/* 動畫過渡 */
.fade-scale-enter-active, .fade-scale-leave-active {
    transition: all 0.3s ease;
}

.fade-scale-enter-from, .fade-scale-leave-to {
    opacity: 0;
    transform: scale(1.05);
}
</style>
