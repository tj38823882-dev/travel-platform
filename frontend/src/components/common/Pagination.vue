<script setup>
import { computed } from 'vue';

const props = defineProps({
  currentPage: {
    type: Number,
    required: true
  },
  totalPages: {
    type: Number,
    required: true
  },
  maxVisibleButtons: {
    type: Number,
    default: 5
  }
});

const emit = defineEmits(['page-change']);

const pageNumbers = computed(() => {
  const pages = [];
  const startPage = Math.max(1, props.currentPage - Math.floor(props.maxVisibleButtons / 2));
  const endPage = Math.min(props.totalPages, startPage + props.maxVisibleButtons - 1);
  
  // Adjust start page if end page is at the limit
  const adjustedStartPage = Math.max(1, Math.min(startPage, props.totalPages - props.maxVisibleButtons + 1));
  
  for (let i = adjustedStartPage; i <= endPage; i++) {
    pages.push(i);
  }
  return pages;
});

const changePage = (page) => {
  if (page < 1 || page > props.totalPages || page === props.currentPage) return;
  emit('page-change', page);
};
</script>

<template>
  <nav v-if="totalPages > 1" class="d-flex justify-content-center mt-5">
    <ul class="pagination custom-pagination shadow-sm p-2 rounded-pill bg-white border">
      <!-- 上一頁 -->
      <li class="page-item" :class="{ disabled: currentPage === 1 }">
        <button class="page-link rounded-circle border-0 me-1" @click="changePage(currentPage - 1)" aria-label="Previous">
          <i class="bi bi-chevron-left"></i>
        </button>
      </li>

      <!-- 第一頁 & 省略號 -->
      <template v-if="pageNumbers[0] > 1">
        <li class="page-item">
          <button class="page-link rounded-circle border-0 mx-1" @click="changePage(1)">1</button>
        </li>
        <li v-if="pageNumbers[0] > 2" class="page-item disabled">
          <span class="page-link bg-transparent border-0">...</span>
        </li>
      </template>

      <!-- 頁碼 -->
      <li v-for="page in pageNumbers" :key="page" class="page-item" :class="{ active: currentPage === page }">
        <button 
          class="page-link rounded-circle border-0 mx-1 transition-all" 
          @click="changePage(page)"
        >
          {{ page }}
        </button>
      </li>

      <!-- 最後一頁 & 省略號 -->
      <template v-if="pageNumbers[pageNumbers.length - 1] < totalPages">
        <li v-if="pageNumbers[pageNumbers.length - 1] < totalPages - 1" class="page-item disabled">
          <span class="page-link bg-transparent border-0">...</span>
        </li>
        <li class="page-item">
          <button class="page-link rounded-circle border-0 mx-1" @click="changePage(totalPages)">{{ totalPages }}</button>
        </li>
      </template>

      <!-- 下一頁 -->
      <li class="page-item" :class="{ disabled: currentPage === totalPages }">
        <button class="page-link rounded-circle border-0 ms-1" @click="changePage(currentPage + 1)" aria-label="Next">
          <i class="bi bi-chevron-right"></i>
        </button>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
.custom-pagination {
  gap: 4px;
}

.page-link {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-weight: 600;
  background: transparent;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.page-link:hover:not(.disabled) {
  background-color: #f1f5f9;
  color: var(--primary);
  transform: scale(1.1);
}

.page-item.active .page-link {
  background: var(--primary-gradient) !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
  transform: scale(1.1);
}

.page-item.disabled .page-link {
  color: #cbd5e1;
  cursor: not-allowed;
}

.transition-all {
  transition: all 0.2s ease-in-out;
}
</style>
