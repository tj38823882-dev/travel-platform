<script setup>
defineProps({
  categories: Array,
  modelValue: String // 目前選中的分類名稱
});
defineEmits(['update:modelValue']);
</script>

<template>
  <div class="flex items-center space-x-4 overflow-x-auto py-6 px-2 custom-scrollbar no-scrollbar">
    <button 
      v-for="cat in categories" 
      :key="cat.id"
      @click="$emit('update:modelValue', cat.name)"
      :class="[
        'group flex items-center px-5 py-2.5 rounded-full whitespace-nowrap font-semibold text-sm transition-all duration-500 border-2',
        'active:scale-95 hover:shadow-md', 
        modelValue === cat.name 
          ? 'bg-blue-600 text-white border-blue-600 shadow-xl shadow-blue-200/50' 
          : 'bg-white text-slate-600 border-slate-100 hover:border-blue-200 hover:text-blue-600 shadow-sm'
      ]"
    >
      <span 
        class="inline-block transition-transform duration-300 group-hover:scale-125 mr-2"
        :class="modelValue === cat.name ? 'scale-110' : ''"
      >
        {{ cat.icon }}
      </span> 
      {{ cat.name }}
    </button>
  </div>
</template>

<style scoped>
/* 隱藏捲軸 */
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }

/* 增加左右邊緣的漸層遮罩，暗示還有更多內容可以捲動 */
.custom-scrollbar {
  mask-image: linear-gradient(to right, transparent, black 5%, black 95%, transparent);
  -webkit-mask-image: linear-gradient(to right, transparent, black 5%, black 95%, transparent);
}
</style>