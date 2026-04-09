<script setup>
import { defineProps, defineEmits, ref, computed } from 'vue';
import { RouterLink } from 'vue-router';
import Pagination from '../common/Pagination.vue';

const props = defineProps({
  trips: {
    type: Array,
    required: true
  }
});

const emit = defineEmits(['edit', 'publish']);

const handleEdit = (trip) => {
  emit('edit', trip);
};

const handlePublish = (trip) => {
  emit('publish', trip);
};

// --- 分頁邏輯 ---
const currentPage = ref(1);
const pageSize = ref(4); // 每頁顯示 4 筆

const totalPages = computed(() => {
  return Math.ceil(props.trips.length / pageSize.value);
});

const paginatedTrips = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return props.trips.slice(start, end);
});

const handlePageChange = (page) => {
  currentPage.value = page;
};
</script>

<template>
  <div class="animate-fade-in">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="fw-bold mb-0">我的行程</h4>
      <RouterLink to="/DayPlanner" class="btn btn-primary btn-sm rounded-pill px-3">
        <i class="bi bi-plus-lg me-1"></i> 新增行程
      </RouterLink>
    </div>
    
    <div class="trip-grid row g-3">
      <div v-for="trip in paginatedTrips" :key="trip.id" class="col-md-6">
        <div class="trip-card h-100 p-0 rounded-4 border overflow-hidden shadow-sm hover-shadow transition-all">
          <div class="trip-image-container position-relative cursor-pointer" style="height: 160px;" @click="handleEdit(trip)">
            <img :src="trip.coverImage || 'https://images.unsplash.com/photo-1543158266-0066955047b1'" 
                 class="w-100 h-100 object-fit-cover" 
                 alt="Trip Cover">
            <div class="trip-status-badge position-absolute top-0 end-0 m-2">
              <span :class="['badge', 
                trip.status === 'active' ? 'bg-success' : 
                trip.status === 'published' ? 'bg-info' : 'bg-secondary']">
                {{ trip.status === 'active' ? '正常' : trip.status === 'published' ? '已上架' : trip.status }}
              </span>
            </div>
          </div>
          <div class="p-3">
            <h5 class="fw-bold text-truncate mb-1 cursor-pointer" @click="handleEdit(trip)">{{ trip.title || '未命名行程' }}</h5>
            <p class="text-muted small mb-3">
              <i class="bi bi-calendar3 me-1"></i>
              {{ trip.startDate }} ~ {{ trip.endDate }}
            </p>
            <div class="d-flex gap-2">
              <!-- 已上架則禁用編輯 -->
              <button class="btn btn-sm flex-grow-1 rounded-pill" 
                :class="trip.status === 'published' ? 'btn-outline-secondary' : 'btn-outline-primary'"
                @click="handleEdit(trip)"
                :disabled="trip.status === 'published'">
                <i class="bi" :class="trip.status === 'published' ? 'bi-lock-fill' : 'bi-pencil-square'"></i> 
                {{ trip.status === 'published' ? '已鎖定' : '編輯' }}
              </button>
              
              <!-- 已上架則切換按鈕 -->
              <button v-if="trip.status !== 'published'" class="btn btn-primary btn-sm flex-grow-1 rounded-pill" @click="handlePublish(trip)">
                <i class="bi bi-cloud-upload me-1"></i> 上架
              </button>
              <button v-else class="btn btn-secondary btn-sm flex-grow-1 rounded-pill" disabled>
                <i class="bi bi-check-circle me-1"></i> 販售中
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 若無資料顯示 -->
      <div v-if="trips.length === 0" class="col-12 text-center py-5">
        <i class="bi bi-map text-muted fs-1 mb-2"></i>
        <p class="text-muted">尚無任何規劃紀錄</p>
        <RouterLink to="/DayPlanner" class="btn btn-outline-primary btn-sm rounded-pill px-4">
          立即規劃
        </RouterLink>
      </div>
    </div>

    <!-- 分頁元件 -->
    <Pagination 
      v-if="trips.length > pageSize"
      :current-page="currentPage" 
      :total-pages="totalPages" 
      @page-change="handlePageChange"
    />
  </div>
</template>

<style scoped>
.trip-card {
  transition: all 0.3s ease;
}
.hover-shadow:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1) !important;
}
.transition-all {
  transition: all 0.3s ease;
}
.object-fit-cover {
  object-fit: cover;
}
.cursor-pointer {
  cursor: pointer;
}
</style>
