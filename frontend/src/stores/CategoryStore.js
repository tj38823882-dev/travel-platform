import { defineStore } from "pinia";
import { ref } from "vue";

export const useCategoryStore = defineStore("category", () => {
  // 分類資料庫
  const categories = ref([
    { id: 1, name: "全部行程", icon: "🌎" },
    { id: 2, name: "賞花", icon: "🌸" },
    { id: 3, name: "文化", icon: "🏮" },
    { id: 4, name: "運動", icon: "🧗" },
    { id: 5, name: "美食", icon: "🍜" },
    { id: 6, name: "藝術", icon: "🎨" },
    { id: 7, name: "娛樂", icon: "🔫" },
  ]);

  // 當前選中的分類
  const selectedCategory = ref("全部行程");

  // 設定選中的分類
  const setCategory = (categoryName) => {
    selectedCategory.value = categoryName;
  };

  return {
    categories,
    selectedCategory,
    setCategory,
  };
});
