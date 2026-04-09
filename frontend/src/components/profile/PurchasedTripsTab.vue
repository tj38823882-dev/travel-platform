<script setup>
import { defineProps } from 'vue';
import { useRouter } from 'vue-router';

const props = defineProps({
  purchasedTrips: {
    type: Array,
    required: true
  }
});

const router = useRouter();
</script>

<template>
  <div class="animate-fade-in">
    <h4 class="fw-bold mb-4 d-flex align-items-center">
      <i class="bi bi-bag-check me-2 text-primary"></i>
      已解鎖/購買的行程
    </h4>
    <p class="text-muted small mb-4">這裡存放您使用點數購買的行程，可以直接進入觀看完整天數與站點。</p>
<<<<<<< HEAD

=======
    
>>>>>>> origin/666
    <div class="row g-3">
      <div v-for="itinerary in purchasedTrips" :key="itinerary.id" class="col-md-6">
        <div class="trip-card h-100 p-0 rounded-4 border overflow-hidden shadow-sm hover-shadow transition-all">
          <div class="trip-image-container position-relative" style="height: 160px;">
<<<<<<< HEAD
            <img :src="itinerary.coverImage || 'https://images.unsplash.com/photo-1543158266-0066955047b1'"
              class="w-100 h-100 object-fit-cover" alt="Trip Cover">
=======
            <img :src="itinerary.coverImage || 'https://images.unsplash.com/photo-1543158266-0066955047b1'" 
                 class="w-100 h-100 object-fit-cover" 
                 alt="Trip Cover">
>>>>>>> origin/666
          </div>
          <div class="p-3">
            <h5 class="fw-bold text-truncate mb-1">{{ itinerary.title || '未命名行程' }}</h5>
            <p class="text-muted small mb-1">
              <i class="bi bi-person me-1"></i> 作者：{{ itinerary.authorName }}
            </p>
            <p class="text-muted small mb-3">
              <i class="bi bi-coin me-1"></i> 購買價格：{{ itinerary.price }} 點
            </p>
            <div class="d-flex gap-2">
              <button class="btn btn-primary btn-sm flex-grow-1 rounded-pill"
                @click="router.push({ name: 'DayPlanner', query: { id: itinerary.tripId, readOnly: true } })">
                <i class=" bi bi-eye-fill me-1"></i> 查看詳情 (唯讀)
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 若無資料顯示 -->
      <div v-if="purchasedTrips.length === 0" class="col-12 text-center py-5">
        <i class="bi bi-bag-x text-muted fs-1 mb-2"></i>
        <p class="text-muted">尚未購買任何行程</p>
        <RouterLink to="/" class="btn btn-outline-primary btn-sm rounded-pill px-4">
          前往商城探索
        </RouterLink>
      </div>
    </div>
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
</style>
