<script setup>
import { defineProps } from 'vue';

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <div class="animate-fade-in">
    <h4 class="fw-bold mb-4 d-flex align-items-center">
      <i class="bi bi-person-circle me-2 text-primary"></i>
      基本資料
    </h4>

    <div class="row g-4 mb-5">
      <div class="col-md-6">
        <label class="form-label text-muted small fw-bold">使用者名稱</label>
        <div class="info-box p-3 rounded-4 bg-light bg-opacity-50">
          {{ user.username }}
        </div>
      </div>
      <div class="col-md-6">
        <label class="form-label text-muted small fw-bold">電子信箱</label>
        <div class="info-box p-3 rounded-4 bg-light bg-opacity-50">
          {{ user.email }}
        </div>
      </div>
      <div class="col-md-6">
        <label class="form-label text-muted small fw-bold">會員角色</label>
        <div class="info-box p-3 rounded-4 bg-light bg-opacity-50">
          {{ user.role?.roleName || '預設旅客' }}
        </div>
      </div>
      <div class="col-md-6">
        <label class="form-label text-muted small fw-bold">個人 ID</label>
        <div class="info-box p-3 rounded-4 bg-light bg-opacity-50">
          #{{ user.id || user.userId }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.info-box {
  border: 1px solid rgba(0, 0, 0, 0.05);
  font-weight: 500;
}
</style>
