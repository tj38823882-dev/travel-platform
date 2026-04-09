<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useTourStore } from '../stores/useTourStore';
import { useCategoryStore } from '../stores/CategoryStore';
import { useCartStore } from '../stores/cartStore';
import { useAuthStore } from '../stores/authStore';
import { useToastStore } from '../stores/toastStore';
import request from '@/utils/request';
import Pagination from '@/components/common/Pagination.vue';

// 初始化 Store
const router = useRouter();
const tourStore = useTourStore();
const categoryStore = useCategoryStore();
const cartStore = useCartStore();
const authStore = useAuthStore();
const toastStore = useToastStore();

// 響應式狀態
const searchQuery = ref('');    // 搜尋關鍵字
const selectedTour = ref(null); // 當前選中查看詳情的行程
const showModal = ref(false);   // 控制詳情彈窗顯示

/* ---- 智慧行程規劃相關 ---- */
const showPlannerModal = ref(false);
const plannerData = ref({
  coverImage: 'https://images.unsplash.com/photo-1543158266-0066955047b1?q=80&w=2070&auto=format&fit=crop',
  name: '',
  startDate: new Date().toISOString().slice(0, 10),
  endDate: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
  transport: '自訂'
});

const fileInput = ref(null);

const handleImageReplace = () => {
  // 觸發隱藏的檔案選擇器
  if (fileInput.value) {
    fileInput.value.click();
  }
};

const onFileChange = async (e) => {
  const file = e.target.files[0];
  if (file) {
    if (!file.type.startsWith('image/')) {
      toastStore.addToast('請選擇圖片檔案', 'warning', '格式錯誤');
      return;
    }

    // 建立 FormData 用於檔案上傳
    const formData = new FormData();
    formData.append('file', file);

    try {
      // 這裡新增一個正在上傳的提示（可選）
      console.log('上傳中...');

      const res = await request.post('/api/public/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      // 後端會回傳圖片的永久 URL
      plannerData.value.coverImage = res.data;
      console.log('上傳成功，網址為:', res.data);

    } catch (err) {
      console.error('圖片上傳失敗:', err);
      toastStore.addToast('圖片上傳失敗，請稍後再試', 'danger', '上傳失敗');
    }
  }
};
import { usePlannerStore } from '../stores/usePlannerStore'
const plannerStore = usePlannerStore()

const handlePlannerComplete = () => {
  showPlannerModal.value = false;
  plannerStore.set({
    title: plannerData.value.name,
    startDate: plannerData.value.startDate,
    endDate: plannerData.value.endDate,
    transport: plannerData.value.transport,
    coverImage: plannerData.value.coverImage
  })
  router.push({ name: 'DayPlanner' })
};

/**
 * 開啟行程詳情彈窗
 */
const openDetail = (tour) => {
  selectedTour.value = tour;
  showModal.value = true;
};

/**
 * 關閉行程詳情彈窗
 */
const closeModal = () => {
  showModal.value = false;
  selectedTour.value = null;
};

// --- 動態獲取商城商品 ---
const itinerariesList = ref([]);
const loadingTrips = ref(false);

const fetchItineraries = async () => {
  try {
    loadingTrips.value = true;
    const res = await request.get('/api/public/itineraries');
    // 將後端傳來的格式轉換為前端 ToTourList 需要的介面
    itinerariesList.value = res.data
      .map(item => ({
        id: 'pub_' + item.id,
        originalItineraryId: item.id,
        authorId: item.authorId,   // 保留 authorId 供後續 computed 過濾
        tripId: item.tripId,
        name: item.title,
        description: item.description,
        price: item.price,
        image: item.coverImage || 'https://images.unsplash.com/photo-1543158266-0066955047b1?q=80&w=2070&auto=format&fit=crop',
        hashTag: item.hashTag || null,

        location: `由 ${item.authorName} 精心設計`,
        // 將後端回傳的 tripDays 轉換為前端顯示用的格式
        itinerary: item.tripDays && item.tripDays.length > 0
          ? item.tripDays.map(day => ({
              day: day.dayNumber,
              title: day.title || `第 ${day.dayNumber} 天`,
              detail: day.stops && day.stops.length > 0
                ? day.stops.map(s => `[${s.startTime || '未定'}] ${s.name}`).join(' → ')
                : '當天暫無安排景點',
              isLocked: false,
              stops: day.stops || [] // 新增：保留原始站點資訊
            }))
          : [{ day: 1, title: '行程規劃中', detail: item.description, isLocked: true, stops: [] }]
      }));
  } catch (err) {
    console.error('獲取上架商品失敗:', err);
  } finally {
    loadingTrips.value = false;
  }
};

onMounted(() => {
  fetchItineraries();
});

const filteredTours = computed(() => {
  // 1. 先處理基礎列表 (過濾作者)
  const myId = String(authStore.userId);
  const realItems = itinerariesList.value.filter(item => String(item.authorId) !== myId);
  let tours = [...realItems, ...tourStore.tours];

  // 2. 移除「已經購買過」的商品
  const purchasedIds = cartStore.purchasedItineraryIds;
  tours = tours.filter(tour => {
    const idToCheck = tour.originalItineraryId || tour.id;
    return !purchasedIds.includes(idToCheck);
  });

  // 分類篩選
  if (categoryStore.selectedCategory !== '全部行程') {
    tours = tours.filter(tour => {
      if (!tour.hashTag) return false;
      return tour.hashTag.split(',').includes(categoryStore.selectedCategory);
    });
  }

  // 搜尋關鍵字篩選
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase().trim();
    tours = tours.filter(tour =>
      tour.name.toLowerCase().includes(query) ||
      tour.location.toLowerCase().includes(query) ||
      tour.description.toLowerCase().includes(query)
    );
  }
  return tours;
});

/**
 * 分頁邏輯
 */
const currentPage = ref(1);
const pageSize = ref(6); // 每頁顯示 6 筆，也可以改為 9

const totalPages = computed(() => {
  return Math.ceil(filteredTours.value.length / pageSize.value);
});

const paginatedTours = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredTours.value.slice(start, end);
});

const handlePageChange = (page) => {
  currentPage.value = page;
  // 換頁後自動滾動到搜索欄下方，方便查看新結果
  const searchSection = document.querySelector('.search-container');
  if (searchSection) {
    searchSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};

// 當搜尋條件或分類改變時，重設分頁到第一頁
watch([searchQuery, () => categoryStore.selectedCategory], () => {
  currentPage.value = 1;
});

/**
 * 將行程加入購物車 (觸發 Store Action)
 */
const addToCart = (tour) => {
  cartStore.addToCart(tour);
};
</script>
<template>
  <div class="page-container">
    <div class="container py-5 animate-fade-in">
      <!-- 頁面標題區 -->
      <div class="header-section text-center mb-5">
        <h1 class="display-4 fw-extrabold main-title mb-2">探索世界之美</h1>
        <p class="text-muted lead mb-4">為您精選最獨特的旅遊體驗</p>
        <button class="btn btn-primary rounded-pill px-5 py-3 fw-bold shadow-lg animate-bounce"
          @click="showPlannerModal = true">
          <i class="bi bi-magic me-2"></i>規劃專屬智慧行程
        </button>
      </div>

      <!-- 搜尋與分類篩選區 -->
      <div class="row mb-5 justify-content-center">
        <div class="col-md-10 col-lg-8">
          <!-- 搜尋輸入框：包含毛玻璃與陰影效果 -->
          <div class="search-container shadow-lg mb-4">
            <div class="input-group glass-input">
              <span class="input-group-text bg-transparent border-0 ps-4">
                <i class="bi bi-search text-primary"></i>
              </span>
              <input v-model="searchQuery" type="text" class="form-control border-0 py-3" placeholder="搜尋行程名稱、地點或描述...">
            </div>
          </div>

          <!-- 分類按鈕群組：點擊可切換分類 -->
          <div class="d-flex flex-wrap justify-content-center gap-3 category-group">
            <button v-for="cat in categoryStore.categories" :key="cat.id" @click="categoryStore.setCategory(cat.name)"
              class="category-btn" :class="{ active: categoryStore.selectedCategory === cat.name }">
              <span class="btn-icon">{{ cat.icon }}</span>
              <span class="btn-text">{{ cat.name }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 當前搜尋結果統計提示 -->
      <div class="row mb-4" v-if="searchQuery || categoryStore.selectedCategory !== '全部行程'">
        <div class="col-12 text-center text-muted">
          <div class="result-badge animate-fade-in">
            找到 <strong>{{ filteredTours.length }}</strong> 筆
            <span v-if="categoryStore.selectedCategory !== '全部行程'">「{{ categoryStore.selectedCategory }}」</span>
            <span v-if="searchQuery">包含「{{ searchQuery }}」</span>
            的行程
          </div>
        </div>
      </div>

      <!-- 行程卡片列表：包含交錯進入動畫 -->
      <div class="row min-vh-50 g-4">
        <TransitionGroup name="stagger">
          <div v-for="(tour, index) in paginatedTours" :key="tour.id" class="col-md-6 col-lg-4">
            <div class="card h-100 border-0 tour-card">
              <!-- 卡片圖片區：包含分類標籤與滑入式遮罩 -->
              <div class="card-img-wrapper">
                <img :src="tour.image" class="card-img-top" :alt="tour.name" @click="openDetail(tour)">
                <div class="category-pill">{{ tour.category }}</div>
                <div class="img-overlay" @click="openDetail(tour)">
                  <span class="view-btn">查看詳情</span>
                </div>
              </div>

              <!-- 卡片內容區 -->
              <div class="card-body p-4">
                <div class="d-flex justify-content-between align-items-start mb-2">
                  <h5 class="tour-name fw-bold mb-0" @click="openDetail(tour)">
                    {{ tour.name }}
                  </h5>
                </div>

                <div class="location-label mb-3">
                  <i class="bi bi-geo-alt-fill text-accent me-1"></i>
                  {{ tour.location }}
                </div>

                <p class="card-text text-muted clamp-text mb-4">
                  {{ tour.description }}
                </p>

                <!-- 底部按鈕與價格區 -->
                <div class="d-flex justify-content-between align-items-center mt-auto border-top pt-3">
                  <div class="price-tag">
                    <span class="currency">NT$</span>
                    <span class="amount">{{ tour.price.toLocaleString() }}</span>
                  </div>
                  <button class="btn btn-add-cart" @click.stop="addToCart(tour)">
                    <i class="bi bi-cart-plus me-1"></i> 加入
                  </button>
                </div>
              </div>
            </div>
          </div>
        </TransitionGroup>

        <!-- 無結果時的空狀態顯示 -->
        <div v-if="filteredTours.length === 0" class="col-12 text-center py-5">
          <div class="empty-state animate-fade-in">
            <div class="empty-icon">🏖️</div>
            <h3 class="fw-bold text-dark mt-4">找不到相關行程</h3>
            <p class="text-secondary mb-4">嘗試換個關鍵字，或者切換其他分類看看！</p>
            <button class="btn btn-primary rounded-pill px-5 py-3 shadow"
              @click="categoryStore.setCategory('全部行程'); searchQuery = ''">
              重設篩選條件
            </button>
          </div>
        </div>
      </div>

      <!--   分頁元件 -->
      <Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="handlePageChange" />
    </div>

    <!-- 行程詳情彈窗 (Modal)：包含行程時間軸 -->
    <Transition name="fade">
      <div v-if="showModal" class="custom-modal-backdrop" @click.self="closeModal">
        <div class="custom-modal-window shadow-2xl animate-modal">
          <!-- 彈窗頂部大圖 -->
          <div class="modal-header-img" :style="{ backgroundImage: `url(${selectedTour.image})` }">
            <button class="close-float-btn" @click="closeModal">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>

          <!-- 彈窗內容區 -->
          <div class="p-4 p-md-5 modal-content-area">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <div class="badge rounded-pill bg-primary-subtle text-primary border border-primary-subtle px-3 py-2">
                {{ selectedTour.category }}
              </div>
            </div>
            <h2 class="fw-bold display-6 mb-2">{{ selectedTour.name }}</h2>
            <p class="text-muted d-flex align-items-center gap-2 mb-4">
              <i class="bi bi-geo-alt text-primary"></i>
              {{ selectedTour.location }}
            </p>

            <!-- 精采行程時間軸區塊 -->
            <div class="itinerary-section p-4 rounded-4 bg-light mb-4">
              <h5 class="fw-bold d-flex align-items-center gap-2 mb-4">
                <i class="bi bi-calendar-check text-primary"></i> 精採行程
              </h5>
              <div class="timeline">
                <div v-for="item in selectedTour.itinerary" :key="item.day" class="timeline-item mb-4">
                  <div class="timeline-marker"></div>
                  <div class="timeline-content">
                    <span class="day-badge">Day {{ item.day }}</span>
                    <h6 class="fw-bold mt-2 mb-3 text-dark">{{ item.title }}</h6>
                    
                    <!-- 站點列表 -->
                    <div v-if="item.stops && item.stops.length > 0" class="stops-list d-flex flex-column gap-2">
                      <div v-for="(stop, sIdx) in item.stops" :key="sIdx" class="stop-item p-3 rounded-3 bg-white border-start border-4 border-primary shadow-sm animate-fade-in" :style="{ animationDelay: (sIdx * 0.1) + 's' }">
                        <div class="d-flex justify-content-between align-items-center">
                          <div class="d-flex align-items-center gap-2">
                            <span class="badge bg-primary text-white">{{ stop.startTime || '未定' }}</span>
                            <span class="fw-bold">{{ stop.name }}</span>
                          </div>
                          <span v-if="stop.address" class="text-muted small">
                            <i class="bi bi-geo-alt"></i> {{ stop.address.split(' ').pop() }}
                          </span>
                        </div>
                        <p v-if="stop.notes" class="text-secondary small mt-2 mb-0 italic">
                          <i class="bi bi-info-circle me-1"></i>{{ stop.notes }}
                        </p>
                      </div>
                    </div>
                    
                    <p v-else :class="['text-secondary mb-0 p-3 bg-white rounded-3', { 'content-locked': item.isLocked }]">
                      {{ item.detail }}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <!-- 彈窗底部：價格與加入購物車按鈕 -->
            <div class="modal-footer-sticky d-flex justify-content-between align-items-center">
              <div class="modal-price">
                <small class="text-muted d-block">活動價格</small>
                <span class="fw-bold fs-3 text-primary">NT$ {{ selectedTour.price.toLocaleString() }}</span>
              </div>
              <button class="btn btn-primary btn-lg rounded-pill px-5 shadow-lg" @click="addToCart(selectedTour)">
                立即加入購物車
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
    <!-- 智慧行程規劃 Modal -->
    <Transition name="fade">
      <div v-if="showPlannerModal" class="custom-modal-backdrop" @click.self="showPlannerModal = false">
        <div class="modal-content-planner animate-modal">
          <div class="modal-header-planner">
            <h3 class="fw-bold mb-0">行程規劃設定</h3>
            <button class="close-planner-btn" @click="showPlannerModal = false">&times;</button>
          </div>

          <div class="modal-body-planner px-4">
            <!-- 封面照片 -->
            <div class="form-section-planner">
              <label class="section-label-planner">封面照片</label>
              <div class="cover-wrapper-planner">
                <img :src="plannerData.coverImage" class="cover-preview-planner" />
                <button class="replace-btn-planner" @click="handleImageReplace">
                  <i class="bi bi-camera-fill me-1"></i>上傳圖片
                </button>
                <!-- 隱藏的檔案選擇輸入框 -->
                <input type="file" ref="fileInput" style="display: none" accept="image/*" @change="onFileChange" />
              </div>
            </div>

            <!-- 行程名稱 -->
            <div class="form-section-planner mt-3">
              <label class="section-label-planner">行程名稱</label>
              <input type="text" v-model="plannerData.name" placeholder="幫您的專屬旅程命名" class="custom-input-planner" />
            </div>

            <!-- 行程日期 -->
            <div class="form-section-planner mt-3">
              <label class="section-label-planner">日期範圍</label>
              <div class="date-range-planner">
                <input type="date" v-model="plannerData.startDate" class="date-picker-planner" />
                <span class="range-arrow-planner">→</span>
                <input type="date" v-model="plannerData.endDate" class="date-picker-planner" />
              </div>
            </div>

            <!-- 主要交通方式 -->
            <div class="form-section-planner mt-3">
              <label class="section-label-planner">主要交通方式</label>
              <div class="select-wrapper-planner">
                <select v-model="plannerData.transport" class="custom-select-planner">
                  <option value="走路">走路 (步行漫遊)</option>
                  <option value="汽車">汽車 (自行開車)</option>
                  <option value="大眾運輸">大眾運輸 (公車/捷運)</option>
                  <option value="機車">機車 (機動靈巧)</option>
                  <option value="自訂">自訂 (混合模式)</option>
                </select>
              </div>
            </div>
          </div>

          <div class="modal-footer-planner d-flex gap-3 px-4 pb-4 mt-4">
            <button class="btn-cancel-planner flex-fill" @click="showPlannerModal = false">取消</button>
            <button class="btn-complete-planner flex-fill" @click="handlePlannerComplete">開始規劃</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
/* --- 標題漸層效果 --- */
.fw-extrabold {
  font-weight: 800;
}

.main-title {
  background: linear-gradient(135deg, #1e293b 0%, #6366f1 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* --- 搜尋框容器樣式 --- */
.search-container {
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: #fff;
  border: 1px solid #e2e8f0;
  transition: var(--transition);
}

.search-container:focus-within {
  transform: translateY(-2px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.glass-input input:focus {
  box-shadow: none;
}

/* --- 分類選擇按鈕樣式 --- */
.category-btn {
  background: white;
  border: 1px solid #e2e8f0;
  padding: 0.6rem 1.4rem;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: var(--transition);
  font-weight: 600;
  color: #64748b;
}

.category-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  transform: scale(1.05);
}

.category-btn.active {
  background: var(--primary-gradient);
  color: white;
  border-color: transparent;
  box-shadow: 0 10px 20px -5px rgba(99, 102, 241, 0.4);
}

.category-btn .btn-icon {
  font-size: 1.2rem;
}

/* --- 行程卡片基礎樣式與懸停效果 --- */
.tour-card {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.card-img-wrapper {
  position: relative;
  height: 240px;
  overflow: hidden;
}

.card-img-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.tour-card:hover .card-img-wrapper img {
  transform: scale(1.1);
}

/* --- 圖片上的詳情按鈕遮罩 --- */
.img-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: var(--transition);
  cursor: pointer;
}

.tour-card:hover .img-overlay {
  opacity: 1;
}

.view-btn {
  background: white;
  padding: 0.5rem 1.5rem;
  border-radius: var(--radius-xl);
  font-weight: 700;
  color: var(--primary);
  transform: translateY(20px);
  transition: var(--transition);
}

.tour-card:hover .view-btn {
  transform: translateY(0);
}

.category-pill {
  position: absolute;
  top: 15px;
  right: 15px;
  background: rgba(255, 255, 255, 0.95);
  padding: 4px 12px;
  border-radius: 10px;
  font-weight: 800;
  font-size: 0.75rem;
  color: var(--primary);
  z-index: 2;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.tour-name {
  color: #1e293b;
  font-size: 1.25rem;
  transition: color 0.3s;
  cursor: pointer;
}

.tour-name:hover {
  color: var(--primary);
}

.location-label {
  font-size: 0.85rem;
  color: #64748b;
  font-weight: 500;
}

.price-tag .currency {
  font-size: 0.8rem;
  color: var(--text-muted);
  font-weight: 600;
  margin-right: 2px;
}

.price-tag .amount {
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--primary);
}

.btn-add-cart {
  background: #f1f5f9;
  color: #475569;
  border: none;
  font-weight: 700;
  padding: 0.6rem 1.4rem;
  border-radius: 12px;
}

.btn-add-cart:hover {
  background: var(--primary);
  color: white;
  transform: scale(1.05);
}

/* --- 彈窗 (Modal) 自定義樣式 --- */
.custom-modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(8px);
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.custom-modal-window {
  background: white;
  width: 100%;
  max-width: 800px;
  max-height: 90vh;
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header-img {
  height: 300px;
  background-size: cover;
  background-position: center;
  position: relative;
}

.close-float-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  background: white;
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  transition: var(--transition);
}

.close-float-btn:hover {
  background: #f1f5f9;
  transform: rotate(90deg);
}

.modal-content-area {
  overflow-y: auto;
  scrollbar-width: thin;
}

/* --- 行程時間軸 (Timeline) 樣式 --- */
.timeline {
  border-left: 2px dashed #e2e8f0;
  margin-left: 10px;
  padding-left: 20px;
}

.timeline-item {
  position: relative;
}

.timeline-marker {
  position: absolute;
  left: -27px;
  top: 5px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--primary);
  border: 2px solid white;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.2);
}

.day-badge {
  color: var(--primary);
  font-weight: 800;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* --- 未解鎖內容模糊效果 --- */
.content-locked {
  filter: blur(4px);
  user-select: none;
  opacity: 0.6;
}

.italic { font-style: italic; }

.stop-item {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stop-item:hover {
  transform: translateX(5px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08) !important;
}

.italic { font-style: italic; }

.stop-item {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stop-item:hover {
  transform: translateX(5px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08) !important;
}

/* --- Vue Transition 動畫配置 --- */
.stagger-enter-active {
  transition: all 0.5s ease;
}

.stagger-enter-from {
  opacity: 0;
  transform: translateY(30px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.animate-modal {
  animation: modalIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(40px);
  }

  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* --- 空狀態樣式 --- */
.empty-state {
  background: white;
  padding: 4rem;
  border-radius: var(--radius-lg);
  border: 2px dashed #e2e8f0;
}

.empty-icon {
  font-size: 5rem;
}

/* --- 智慧行程規劃 Modal 特有樣式 --- */
.modal-content-planner {
  background: white;
  width: 100%;
  max-width: 460px;
  border-radius: 30px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  position: relative;
  overflow: hidden;
}

.modal-header-planner {
  padding: 1.5rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
}

.close-planner-btn {
  background: none;
  border: none;
  font-size: 2rem;
  color: #94a3b8;
  cursor: pointer;
  line-height: 1;
}

.form-section-planner {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}

.section-label-planner {
  font-weight: 700;
  color: #1e293b;
  font-size: 0.9rem;
  margin-top: 0.5rem;
}

.cover-wrapper-planner {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  height: 180px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}

.cover-preview-planner {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.replace-btn-planner {
  position: absolute;
  bottom: 0.8rem;
  right: 0.8rem;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  border: none;
  color: #0f172a;
  padding: 0.5rem 1rem;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.custom-input-planner {
  padding: 0.8rem 1.2rem;
  border-radius: 15px;
  border: 2px solid #e2e8f0;
  font-size: 1rem;
  outline: none;
  transition: all 0.2s;
}

.custom-input-planner:focus {
  border-color: var(--primary);
  background: #f8fafc;
}

.date-range-planner {
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-picker-planner {
  flex: 1;
  padding: 0.8rem;
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  font-size: 0.9rem;
  color: #475569;
}

.range-arrow-planner {
  color: #94a3b8;
  font-weight: bold;
}

.custom-select-planner {
  width: 100%;
  padding: 0.8rem 1.2rem;
  border-radius: 15px;
  border: 2px solid #e2e8f0;
  font-size: 1rem;
  appearance: none;
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3e%3cpath fill='none' stroke='%23475569' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='m2 5 6 6 6-6'/%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 1.2rem center;
  background-size: 16px 12px;
}

.btn-cancel-planner {
  background: #f1f5f9;
  border: none;
  color: #64748b;
  padding: 0.9rem;
  border-radius: 15px;
  font-weight: 700;
  cursor: pointer;
}

.btn-complete-planner {
  background: var(--primary-gradient);
  border: none;
  color: white;
  padding: 0.9rem;
  border-radius: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.3);
}

.animate-bounce {
  animation: bounce 2s infinite;
}

@keyframes bounce {

  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-5px);
  }
}
</style>
