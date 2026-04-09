// usePlannerStore — 暫存「智慧行程規劃 Modal」的資料
// 用途：ToTourList 填完規劃表單後，把資料存到這裡，
// 再 router.push 到 DayPlanner 頁面讀取，避免 URL 過長
import { defineStore } from "pinia";
import { ref } from "vue";

export const usePlannerStore = defineStore("planner", () => {
  const data = ref(null);

  // ToTourList 呼叫：把規劃資料存入 store
  function set(plannerData) {
    data.value = { ...plannerData };
  }

  // DayPlanner 呼叫：取出資料後清空，避免下次誤用舊資料
  function consume() {
    const result = data.value;
    data.value = null;
    return result;
  }

  return { data, set, consume };
});
