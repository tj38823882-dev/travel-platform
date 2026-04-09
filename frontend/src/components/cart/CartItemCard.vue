<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  item: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['remove']);
</script>

<template>
  <div class="cart-item d-flex align-items-center mb-4 pb-4 border-bottom last-no-border">
    <!-- 行程縮圖與分類 -->
    <div class="item-img-wrapper shadow-sm me-4">
      <img :src="item.image" :alt="item.name" class="rounded-3 item-img">
      <div class="item-category-small">{{ item.category }}</div>
    </div>
    
    <!-- 行程詳細內容 -->
    <div class="flex-grow-1">
      <div class="d-flex justify-content-between align-items-start mb-2">
        <h5 class="fw-bold text-dark mb-0">{{ item.name }}</h5>
        <!-- 移除按鈕 -->
        <button class="remove-btn" @click="emit('remove', item.id)" title="移除行程">
          <i class="bi bi-trash3-fill"></i>
        </button>
      </div>
      <p class="mb-3 text-muted small"><i class="bi bi-geo-alt me-1"></i> {{ item.location }}</p>
      
      <div class="d-flex justify-content-between align-items-center">
        <div class="quantity-control glass-panel px-3 py-1 rounded-pill">
          <span class="small text-muted fw-bold">1 個行程</span>
        </div>
        <div class="item-price fw-bold text-primary">
          NT$ {{ item.price.toLocaleString() }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-item {
    transition: var(--transition);
}

.last-no-border:last-child {
    border-bottom: none !important;
    margin-bottom: 0 !important;
    padding-bottom: 0 !important;
}

.item-img-wrapper {
    width: 120px;
    height: 120px;
    position: relative;
    border-radius: var(--radius-md);
    overflow: hidden;
}

.item-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s;
}

.item-img-wrapper:hover .item-img {
    transform: scale(1.1);
}

.item-category-small {
    position: absolute;
    bottom: 0; left: 0; width: 100%;
    background: rgba(99, 102, 241, 0.9);
    color: white;
    font-size: 0.65rem;
    font-weight: 800;
    text-align: center;
    padding: 2px 0;
}

.remove-btn {
    background: transparent;
    border: none;
    color: #e2e8f0;
    font-size: 1.25rem;
    transition: var(--transition);
    padding: 0;
}

.remove-btn:hover {
    color: var(--accent);
    transform: scale(1.1);
}

.item-price {
    font-size: 1.25rem;
    letter-spacing: -0.5px;
}

.quantity-control {
    background: rgba(241, 245, 249, 0.5);
    border: 1px solid rgba(226, 232, 240, 0.5);
}
</style>
