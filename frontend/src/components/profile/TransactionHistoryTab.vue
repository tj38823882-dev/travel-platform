<script setup>
import { defineProps, ref, computed } from 'vue';
import Pagination from '../common/Pagination.vue';

const props = defineProps({
  transactions: {
    type: Array,
    required: true
  }
});

const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// --- 分頁邏輯 ---
const currentPage = ref(1);
const pageSize = ref(8); // 每頁顯示 8 筆

const totalPages = computed(() => {
  return Math.ceil(props.transactions.length / pageSize.value);
});

const paginatedTransactions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return props.transactions.slice(start, end);
});

const handlePageChange = (page) => {
  currentPage.value = page;
};
</script>

<template>
  <div class="animate-fade-in">
    <h4 class="fw-bold mb-4 d-flex align-items-center">
      <i class="bi bi-clock-history me-2 text-primary"></i>
      點數收支紀錄
    </h4>

    <div class="table-responsive">
      <table class="table table-hover align-middle border-0">
        <thead class="bg-light bg-opacity-50">
          <tr>
            <th class="border-0 rounded-start px-3">日期</th>
            <th class="border-0">交易項目 / 說明</th>
            <th class="border-0">異動點數</th>
            <th class="border-0">金額 (TWD)</th>
            <th class="border-0">支付方式</th>
            <th class="border-0 rounded-end">狀態</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tx in paginatedTransactions" :key="tx.id" class="border-bottom">
            <td class="px-3 small text-muted">{{ formatDate(tx.date) }}</td>
            <td>
              <div class="fw-bold text-dark">{{ tx.description }}</div>
            </td>
            <td>
              <div class="d-flex align-items-center gap-1">
                <i class="bi bi-coin text-warning"></i>
                <span class="fw-bold" :class="tx.type === 'spend' ? 'text-danger' : 'text-success'">
                  {{ tx.type === 'spend' ? '-' : '+' }}{{ tx.points }}
                </span>
              </div>
            </td>
            <td class="fw-bold text-primary">
              {{ tx.price ? '$' + tx.price : '-' }}
            </td>
            <td>
              <span class="badge rounded-pill px-3 py-2" 
                :class="tx.type === 'spend' ? 'bg-danger-subtle text-danger' : 'bg-success-subtle text-success'">
                {{ tx.type === 'spend' ? '支出' : '收入' }}
              </span>
            </td>
            <td>
              <span class="badge bg-secondary-subtle text-secondary border-0 px-2">
                {{ tx.status || '已完成' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 若無資料顯示 -->
    <div v-if="transactions.length === 0" class="text-center py-5">
      <i class="bi bi-clipboard-x text-muted fs-1 mb-2"></i>
      <p class="text-muted">尚無任何訂單紀錄</p>
      <RouterLink to="/TopUpView" class="btn btn-outline-primary btn-sm rounded-pill px-4">
        立即儲值
      </RouterLink>
    </div>

    <!-- 分頁元件 -->
    <Pagination 
      v-if="transactions.length > pageSize"
      :current-page="currentPage" 
      :total-pages="totalPages" 
      @page-change="handlePageChange"
    />
  </div>
</template>

<style scoped>
.extra-small {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}
</style>
