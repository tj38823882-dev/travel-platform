<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  showPublishModal: {
    type: Boolean,
    required: true
  },
  publishForm: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['close', 'submitPublish']);
</script>

<template>
  <template v-if="showPublishModal">
    <div class="modal-backdrop fade show custom-backdrop"></div>
    <div class="modal fade show d-block custom-modal" tabindex="-1" role="dialog" @click.self="emit('close')">
      <div class="modal-dialog modal-dialog-centered" role="document">
        <div class="modal-content shadow-lg border-0 rounded-4">
          <div class="modal-header border-bottom-0 pb-0">
            <h5 class="modal-title fw-bold">
              <i class="bi bi-cloud-upload text-primary me-2"></i>發佈行程為商品
            </h5>
            <button type="button" class="btn-close" @click="emit('close')" aria-label="Close"></button>
          </div>
          <div class="modal-body px-4 py-3">
            <div class="mb-3">
              <label class="form-label text-muted small fw-bold">商品標題</label>
              <input type="text" class="form-control rounded-3" v-model="publishForm.title" placeholder="請輸入吸引人的商品標題" />
            </div>
            <div class="mb-3">
              <label class="form-label text-muted small fw-bold">商品描述</label>
              <textarea class="form-control rounded-3" rows="4" v-model="publishForm.description" placeholder="介紹一下這個行程的特色與亮點..."></textarea>
            </div>
            <div class="mb-3">
              <label class="form-label text-muted small fw-bold">價格 (點數)</label>
              <div class="input-group">
                <span class="input-group-text bg-white border-end-0">
                  <i class="bi bi-coin text-warning"></i>
                </span>
                <input type="number" class="form-control border-start-0 ps-0" v-model="publishForm.price" min="0" placeholder="設定販售點數" />
              </div>
              <div class="form-text mt-1">設定為 0 則代表免費分享。</div>
            </div>
          </div>
          <div class="modal-footer border-top-0 pt-0 px-4 pb-4">
            <button type="button" class="btn btn-light rounded-pill px-4" @click="emit('close')">取消</button>
            <button type="button" class="btn btn-primary rounded-pill px-4 shadow-sm" @click="emit('submitPublish')">
              <i class="bi bi-check2 me-1"></i> 確認上架
            </button>
          </div>
        </div>
      </div>
    </div>
  </template>
</template>

<style scoped>
.custom-backdrop {
  opacity: 0.5;
  z-index: 1040;
}
.custom-modal {
  z-index: 1050;
  background-color: rgba(0, 0, 0, 0.4);
}
</style>
