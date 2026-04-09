<!-- =================================================
DayPlanner.vue — 主頁面

   佈局：
   - 上方：標題
   - 左欄：Google Map（內嵌 Autocomplete 搜尋框）
   - 右欄：行程列（可拖曳排序、編輯時間、rename、刪除）

   職責：
   1. 組合 composable（useGoogleMap、useDirections、usePoints）
   2. 初始化地圖 + Autocomplete
   3. 把 state 傳給 template 渲染
   4. 處理拖曳、編輯等事件
   ================================================= -->

<script setup>
// 前後端傳送用
import { saveTrip } from '@/utils/tripSave'
import { getTripById, getItineraryById } from '@/utils/tripApi'
// 載入前端功能用
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MAP_OPTIONS } from '../config/MapConfig.js'
import { useCategoryStore } from '../stores/CategoryStore.js'
import { usePlannerStore } from '../stores/usePlannerStore.js'
import { useGoogleMap } from '../stores/useGoogleMap.js'
import { useDirections } from '../stores/useDirections.js'
import { usePoints } from '../stores/usePoints.js'

// 載入其他component view
import TripDetail from '@/components/TripDetail.vue'

// 載入router
const router = useRouter()
/* ---- 要使用 Composables 功能，所以相關變數 ---- */
const route = useRoute()
const googleMap = useGoogleMap()
const directions = useDirections(googleMap)
const categoryStore = useCategoryStore()
const {
  points, selectedPoint,
  addFromLatLng, addFromPlace, addEmpty,
  removePoint, selectPoint,
  renamePoint,
  updateTravelMinutes, updateStayMinutes, updateTravelMode,
  updateNote,
  syncMarkersToPoints,
  setIdCounter
} = usePoints(googleMap, directions)


/* ===================================================

定義各式方法 以及相關變數  : 

=================================================== */
// 地圖本身
const mapContainer = ref(null)


// initAutocomplete
const searchInput = ref(null)
function initAutocomplete() {
  const placesLib = googleMap.getPlacesLibrary()
  const map = googleMap.getMap()

  console.log('[Autocomplete] placesLib:', placesLib)
  console.log('[Autocomplete] map:', map)
  console.log('[Autocomplete] searchInput:', searchInput.value)

  if (!placesLib || !map || !searchInput.value) {
    console.warn('[Autocomplete] 初始化條件不足，已退出')
    return
  }

  console.log('[Autocomplete] 開始建立 Autocomplete...')
  const autocomplete = new placesLib.Autocomplete(searchInput.value, {
    fields: ['name', 'geometry', 'formatted_address', 'photos', 'rating', 'place_id']
  })
  console.log('[Autocomplete] Autocomplete 建立成功:', autocomplete)

  /* 使用者從下拉選了一個地點 → 預覽並提供加入行程 */
  autocomplete.addListener('place_changed', () => {
    const place = autocomplete.getPlace()
    console.log('[Autocomplete] place_changed 觸發, place:', place)
    if (!place || !place.geometry || !place.geometry.location) return

    // --- 讓地圖移動到該座標 ---
    if (place.geometry.viewport) {
      map.fitBounds(place.geometry.viewport)
    } else {
      map.panTo(place.geometry.location)
      map.setZoom(17)
    }

    addFromPlace(place)
  })
}

/* 搜尋框文字監控與清除 */
const hasSearchText = ref(false)
function onSearchInput(e) {
  hasSearchText.value = !!e.target.value
}

function clearSearch() {
  if (searchInput.value) {
    searchInput.value.value = ''
    hasSearchText.value = false
    searchInput.value.focus()
  }
}



// 若是要編輯既有行程時的 DB id（新建時為 null）(有id 才可以載入)
const tripId = ref(null)

// 是否為唯讀模式（例如查看已購買行程）
const isReadOnly = computed(() => route.query.readOnly === 'true')


// 儲存每一天的行程資料 { 1: [points...], 2: [points...] }
// 每個陣列上額外掛 dbDayId、dayTitle 屬性
const allDaysItinerary = ref({})


/* ===================================================
   頁面真正載入：初始化地圖 + Autocomplete
   =================================================== */
onMounted(async () => {
  /* 0. 從 Pinia store 讀取智慧規劃資料（ToTourList Modal 傳過來的） */
  const plannerStore = usePlannerStore()
  const planned = plannerStore.consume()  // 取出後自動清空
  if (planned) {
    tripTitle.value = planned.title || ''
    startDate.value = planned.startDate || startDate.value
    endDate.value = planned.endDate || endDate.value
    coverImage.value = planned.coverImage || ''
    if (planned.hashTag) hashTags.value = planned.hashTag.split(',')
  }

  // 從 URL 參數讀取基本資料 (例如從 ToTourList 跳轉過來時)
  // if (route.query.title) tripTitle.value = route.query.title
  // if (route.query.start) startDate.value = route.query.start
  // if (route.query.end) endDate.value = route.query.end
  // if (route.query.cover) coverImage.value = route.query.cover

  /* 1. 初始化地圖   給地圖 */
  await googleMap.initMap(mapContainer.value, MAP_OPTIONS)

  /* 1. 初始化地圖   讓你可以加點 */
  googleMap.onMapClick(async (latLng) => {
    if (isReadOnly.value) return // 唯讀模式下禁止手動加點
    await addFromLatLng(latLng)
  })

  /* 1. 初始化地圖   給搜尋bar */
  initAutocomplete()



  /* 2. 若有 id，從 DB 載入完整行程資料 */
  // 有id 才有 這是用在查看 不是建立
  if (route.query.id) {
    tripId.value = Number(route.query.id)
    try {
      const res = await getTripById(tripId.value)
      const tripData = res.data
      console.log('[DayPlanner] 從 DB 載入行程:', tripData)


      // 覆蓋基本欄位（以 DB 為準）
      tripTitle.value = tripData.title || ''
      startDate.value = tripData.startDate || startDate.value
      endDate.value = tripData.endDate || endDate.value
      coverImage.value = tripData.coverImage || ''
      if (tripData.hashTag) hashTags.value = tripData.hashTag.split(',')

      // 把每天的 stops 轉成 points 格式，存入 allDaysItinerary
      console.log('[DayPlanner] tripDays:', tripData.tripDays)
      if (tripData.tripDays && tripData.tripDays.length > 0) {
        let localId = 0
        tripData.tripDays.forEach(day => {
          const dayPoints = (day.stops || [])
            .sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0))
            .map(stop => ({
              id: ++localId,
              dbId: stop.id,        // DB 原始 stop id，update 時用來比對
              lat: stop.lat || 0,
              lng: stop.lng || 0,
              address: stop.address || '',
              name: stop.name || '',
              rating: null,
              photos: stop.photoUrl ? [stop.photoUrl] : [],  // 從 DB 回填圖片 URL
              photoUrl: stop.photoUrl || null,
              placeId: stop.placeId || null,
              time: '',
              travelMode: stop.travelMode || 'CUSTOM',
              travelMinutes: stop.travelMinutes || 0,
              stayMinutes: stop.stayMinutes || 0,
              routeSteps: [],
              note: stop.note || ''
            }))
          dayPoints.dbDayId = day.id  // DB 原始 tripDay id，update 時用來比對
          allDaysItinerary.value[day.dayNumber] = dayPoints
          dayTitles.value[day.dayNumber] = day.title || ''
          dayStartTimes.value[day.dayNumber] = day.startTime || '08:00'
        })

        // 載入第 1 天的地點到地圖
        points.value = [...(allDaysItinerary.value[1] || [])]
        console.log('[DayPlanner] 載入後 points:', points.value)
        setIdCounter(localId)
        await nextTick()
        syncMarkersToPoints()

        // 對所有天數中沒有 photoUrl 但有 placeId 的 stop，從 Google Places 重新抓圖
        for (const dayPts of Object.values(allDaysItinerary.value)) {
          for (const pt of dayPts) {
            if (!pt.photoUrl && pt.placeId) {
              try {
                const detail = await googleMap.getPlaceDetails(pt.placeId)
                if (detail?.photos?.length) {
                  const url = detail.photos[0].getUrl({ maxWidth: 400 })
                  pt.photos = [url]
                  pt.photoUrl = url
                }
              } catch (e) {
                console.warn('[DayPlanner] 抓圖失敗:', pt.placeId, e)
              }
            }
          }
        }
        // 重新同步 points（因為 Day 1 的圖可能剛被更新）
        points.value = [...(allDaysItinerary.value[1] || [])]
      } else {
        console.warn('[DayPlanner] tripDays 為空或不存在')
      }
    } catch (err) {
      console.error('載入行程失敗:', err)
    }
  }
  // if (route.query.itineraryId && isReadOnly.value) {
  //   const res = await getItineraryById(route.query.itineraryId)
  //   const tripData = res.data.tripDays ? res.data : res.data // 視 response 結構而定
  //   // 後續 tripData 處理跟現在一樣
  // } else if (route.query.id) {
  //   // 原本的 getTripById 邏輯
  // }
  await nextTick()
  // nextTick() 就是讓程式等到 Vue 把 DOM 更新完再往下執行
})







// /* 
// ---- Hashtag ---- 
// */
// // 該trip 要存入的hashtag 
const hashTags = ref([])
// 可以選的hashtag (是從另一個 store 引入
const availableTags = computed(() =>
  categoryStore.categories.filter(c => c.name !== '全部行程')
)
// 到時候點選的方法  : 如果已經選了就取消，沒選就加入
function toggleTag(tagName) {
  const idx = hashTags.value.indexOf(tagName)
  if (idx >= 0) hashTags.value.splice(idx, 1)
  else hashTags.value.push(tagName)
}


import { useToastStore } from '@/stores/toastStore'
const tripTitle = ref('')
const toastStore = useToastStore()
const coverImage = ref('')

const dayTitles = ref({})  // { 1: '聖家堂巡禮', 2: '海邊一日' }






/* ===================================================
  日期 相關  
=================================================== */
const startDate = ref(new Date().toISOString().slice(0, 10))
const endDate = ref(new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10))
// 先預設為兩天，所以2*24，60的部分是把天數轉成毫秒;實際會被 DB 或智慧規劃資料覆蓋
/* ---- 總天數計算 ---- */
const totalDays = computed(() => {
  const start = new Date(startDate.value)
  const end = new Date(endDate.value)
  if (isNaN(start) || isNaN(end)) return 1
  const diffTime = end - start
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1
  return diffDays > 0 ? diffDays : 1
})

// 是對每個tripDay來說，有day1、day2....
const theCurrentDay = ref(1)
/* ---- 切換天數時的資料同步 ---- */
function switchDay(day) {
  // 1. 先把目前的 points 存回 allDaysItinerary; 
  // 注意：需要保留掛在陣列上的 dbDayId 屬性
  const currentPoints = [...points.value]
  currentPoints.dbDayId = allDaysItinerary.value[theCurrentDay.value]?.dbDayId || null
  allDaysItinerary.value[theCurrentDay.value] = currentPoints

  // 2. 切換當前天數
  theCurrentDay.value = day

  // 3. 從 allDaysItinerary 載入目標天數的 points
  const targetDayPoints = allDaysItinerary.value[day] || []
  points.value = [...targetDayPoints]

  // 4. 同步地圖標籤
  nextTick(() => {
    syncMarkersToPoints()
  })
}




/* ===================================================
  時間(clock) 相關  
=================================================== */
// 每天起始時間可以不一樣，現在是空的容器之後拿來放每天的 start clock
const dayStartTimes = ref({})

// 更像一個「帶讀寫邏輯的方法」，它不是單純存值的變數，而是透過 get 讀值、set 寫值，動態對應不同天的資料。你操作它時，就像操作變數，但背後其實是呼叫函式去存或取值。
const startTime = computed({
  get: () => dayStartTimes.value[theCurrentDay.value] || '08:00',
  set: (val) => { dayStartTimes.value[theCurrentDay.value] = val }
})

// 這邊也是方法 就是之後可以計算ref型態的 每一個 point 的 ArrivalTimes
const computedArrivalTimes = computed(() => {
  const times = []
  let current = new Date()
  const [h, m] = startTime.value.split(':').map(Number)
  current.setHours(h, m, 0, 0)
  points.value.forEach((point) => {
    times.push(current.toTimeString().slice(0, 5))
    const gap = (point.travelMinutes || 0) + (point.stayMinutes || 0)
    current = new Date(current.getTime() + gap * 60 * 1000)
  })
  return times
})



/* ---- 交通模式選項  是給 tripDetail 用  ---- */
const TRAVEL_MODES = [
  { value: 'WALKING', label: '🚶', title: '步行' },
  { value: 'DRIVING', label: '🚗', title: '開車' },
  { value: 'TRANSIT', label: '🚏', title: '大眾運輸' },
  { value: 'CUSTOM', label: '✏️', title: '自訂' },
]




/* ===================================================
   最後真的想存進資料庫:

  把前面有建立的變數一一指派值， js 的通用寫法 {tripId: tripId.value,....}，包含跟trip、tripDay有關的，還有跟stop結構對應的 allDaysItinerary，

  有 tripId 會走 updateTrip，沒有走 createTrip 並回傳新 id，因為tripApi.js 裡已經寫好這幾種分類;
   =================================================== */
async function handleSave() {
  try {
    // saveTrip 內部會處理 currentDay 的同步（保留 dbDayId），這裡不需要手動 spread
    const newId = await saveTrip({
      tripId: tripId.value,
      tripTitle: tripTitle.value,
      startDate: startDate.value,
      endDate: endDate.value,
      coverImage: coverImage.value,
      hashTag: hashTags.value.length ? hashTags.value.join(',') : null,
      startTime: startTime.value,
      dayStartTimes: dayStartTimes.value,
      currentDay: theCurrentDay.value,
      dayTitles: dayTitles.value,
      points: points.value,
      allDaysItinerary: allDaysItinerary.value,
    })
    // 如果是第一次建立後把回傳的 id 存起來，之後 Save 就會走 updateTrip
    if (!tripId.value && newId) {
      tripId.value = newId
    }
    toastStore.addToast('儲存行程成功', 'success')
    router.push({ name: 'Profile', query: { tab: 'trips' } })

  } catch (err) {
    console.error(err)
    toastStore.addToast('儲存失敗', 'error')
  }
}


</script>









<template>
  <div class="planner-container">

    <!-- ====== 標題區 ====== -->
    <header class="planner-header">
      <h1 class="planner-title"> 一日行程規劃</h1>
      <p class="planner-subtitle">在地圖上搜尋或點擊加入地點，右邊拖曳可排順序</p>
    </header>

    <!-- ====== 主體 ====== -->
    <div class="planner-body">


      <!-- ===== 左欄：地圖 ===== -->
      <div class="planner-map-col">
        <div class="map-wrapper">
          <div ref="mapContainer" class="map-element"></div>
          <div v-if="!isReadOnly" class="map-search-wrapper">
            <div class="search-box"><input ref="searchInput" type="text" placeholder="想要去哪裡..." class="map-search-input"
                @input="onSearchInput" />
              <div class="search-actions">
                <i v-if="hasSearchText" class="bi bi-x-lg clear-icon" @click="clearSearch"></i>
                <!-- <div class="divider"></div>
                <i class="bi bi-search search-btn-icon"></i> -->
              </div>
            </div>
          </div>
        </div>
      </div>


      <!-- 右欄：行程列 -->
      <div class="planner-side-col">


        <!-- 行程日期範圍與標題 -->
        <div class="trip-settings-card">

          <div class="setting-group full-width ">
            <label class="setting-label">旅程標題</label>
            <input type="text" v-model="tripTitle" class="setting-input" placeholder="輸入旅程名稱" />
          </div>

          <div class="setting-group">
            <label class="setting-label">開始日期</label>
            <input type="date" v-model="startDate" class="setting-input" />
          </div>
          <div class="setting-group">
            <label class="setting-label">結束日期</label>
            <input type="date" v-model="endDate" class="setting-input" />
          </div>



          <div class="setting-group full-width">
            <label class="setting-label">行程標籤</label>
            <div class="hashtag-selector">
              <button v-for="tag in availableTags" :key="tag.id" class="hashtag-btn"
                :class="{ 'is-active': hashTags.includes(tag.name) }" @click="toggleTag(tag.name)">
                {{ tag.icon }} {{ tag.name }}
              </button>
            </div>
          </div>

        </div>

        <!-- 天數切換器 -->
        <div class="day-selector full-width">
          <button v-for="day in totalDays" :key="day" class="day-btn" :class="{ 'active': theCurrentDay === day }"
            @click="switchDay(day)">
            Day {{ day }}
          </button>
        </div>

        <!-- 當天標題 -->
        <div class="day-title-row">
          <input type="text" v-model="dayTitles[theCurrentDay]" class="day-title-input"
            :placeholder="`Day ${theCurrentDay} 標題，例如：聖家堂巡禮`" />
        </div>


        <!-- 行程日期 + 標題
          <div class="trip-start">
            <label class="trip-start-label">行程日期</label>
            <input type="date" v-model="travelDate" class="time-input" />
            <label class="trip-start-label">行程標題</label>
            <input type="text" v-model="tripTitle" class="title-input" placeholder="輸入旅程名稱" />
          </div> -->






        <!-- 行程列表 --> <!-- 這邊是 從 子emit 到父 最後的結果 -->
        <TripDetail v-model:points="points" v-model:startTime="startTime" :selectedPointId="selectedPoint?.id ?? null"
          :arrivalTimes="computedArrivalTimes" :travelModes="TRAVEL_MODES" :readOnly="isReadOnly"
          @reorder="syncMarkersToPoints" @select="selectPoint" @remove="removePoint" @rename="renamePoint"
          @update-stay="updateStayMinutes" @update-travel-mode="updateTravelMode"
          @update-travel-minutes="updateTravelMinutes" @update-note="updateNote" />



        <!-- 底部按鈕 -->
        <div v-if="!isReadOnly" class="bottom-actions">
          <!-- <button class="btn btn-add" @click="addEmpty">＋ 新增</button> -->

          <button class="btn btn-save" @click="handleSave">💾 Save</button>
        </div>
        <div v-else class="bottom-actions">
          <button class="btn btn-secondary w-100 rounded-pill" @click="router.push('/profile')">
            <i class="bi bi-arrow-left me-1"></i> 返回個人資料
          </button>
        </div>


      </div>
    </div>


  </div>
</template>




<style scoped>
/* ===================================================
   整體佈局 (Modern Premium Design)
   =================================================== */
.planner-container {
  padding: 16px;
  font-family: 'Inter', 'Segoe UI', system-ui, sans-serif;
  max-width: 1400px;
  margin: 0 auto;
  height: calc(100vh - 10px);
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.05);
  box-sizing: border-box;
}

.planner-header {
  margin-bottom: 12px;
  text-align: center;
}

.planner-title {
  font-size: 32px;
  font-weight: 800;
  margin: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  display: inline-block;
  letter-spacing: -0.5px;
}

.planner-subtitle {
  margin: 8px 0 0 0;
  font-size: 16px;
  font-weight: 500;
}

.planner-body {
  display: flex;
  gap: 24px;
  flex: 1;
  min-height: 0;
  position: relative;
}

/* 左欄 */
.planner-map-col {
  flex: 1;
  min-width: 0;
  border-radius: 20px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.map-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 20px;

}

.map-element {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}

/* 搜尋框：保留舊的 */
.map-search-wrapper {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 500;
  /* 高於地圖 UI，但 pac-container 會用 9999 更高 */
  pointer-events: auto;
}

.search-box {
  display: flex;
  align-items: center;
  background: #ffffff;
  border-radius: 28px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(0, 0, 0, 0.05);
  width: 330px;

  padding: 4px 12px 4px 20px;
  transition: box-shadow 0.25s ease;
  border: 1px solid transparent;
}

.search-box:focus-within {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.16);
  border-color: #e2e8f0;
}

.map-search-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 8px 0;
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  outline: none;
}

.search-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #5f6368;
}

.clear-icon {
  font-size: 14px;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: background 0.2s;
}

.clear-icon:hover {
  background: #f1f3f4;
}

.divider {
  width: 1px;
  height: 24px;
  background: #e0e0e0;
}

.search-btn-icon {
  font-size: 18px;
  color: #1a73e8;
  padding: 8px;
}

/* Google Autocomplete 下拉清單美化 */
:global(.pac-container) {
  border-radius: 16px;
  margin-top: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8eaed;
  font-family: 'Inter', system-ui, sans-serif !important;
  z-index: 9999 !important;
  /* 極重要：確保下拉選單不會被地圖或彈窗擋住 */
}

:global(.pac-item) {
  padding: 10px 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  border-top: none;
  font-size: 14px;
}

:global(.pac-item:hover) {
  background-color: #f8f9fa;
}

:global(.pac-item-query) {
  font-size: 15px;
  color: #1a1a1a;
  font-weight: 500;
}

:global(.pac-matched) {
  color: #4285f4;
}

:global(.pac-icon) {
  margin-top: 0;
  margin-right: 12px;
}

/* ===================================================
   右欄：行程列 (Glassmorphism)
   =================================================== */
/* ===================================================
   右欄：行程列 (Modern Aesthetic)
   =================================================== */
.planner-side-col {
  width: 460px;
  height: 100%;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  border-radius: 20px;
  padding: 9px 14px 14px 14px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  border: 1px solid #eef2f6;
}

.trip-settings-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 2px 12px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px;
  border: 1px solid #e2e8f0;
}

@media (max-width: 400px) {
  .trip-settings-card {
    grid-template-columns: 1fr;
  }
}

.full-width {
  grid-column: span 2;
}

@media (max-width: 400px) {
  .full-width {
    grid-column: span 1;
  }
}

.setting-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-label {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  padding-top: 6px;

}

.setting-input {
  width: 100%;
  padding: 4px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 15px;
  color: #1e293b;
  outline: none;
  background: #ffffff;
  transition: all 0.2s;
  font-family: inherit;
}

.setting-input:focus {
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

.itinerary-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding-right: 8px;
}

.itinerary-list::-webkit-scrollbar {
  width: 3px;
}

.itinerary-list::-webkit-scrollbar-track {
  background: transparent;
}

.itinerary-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}








/* 當天標題 */
.day-title-row {
  padding: 0 4px;
}

.day-title-input {
  width: 100%;
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  outline: none;
  background: #ffffff;
  transition: all 0.2s;
  font-family: inherit;
}

.day-title-input:focus {
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

.day-title-input::placeholder {
  color: #cbd5e1;
  font-weight: 400;
}

/* Hashtag 選擇器 */
.hashtag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px 0 8px;
}

.hashtag-btn {
  padding: 5px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: #f8fafc;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.hashtag-btn:hover {
  border-color: #7c3aed;
  color: #7c3aed;
  background: #f5f3ff;
}

.hashtag-btn.is-active {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
  color: white;
  border-color: #7c3aed;
  box-shadow: 0 4px 10px rgba(124, 58, 237, 0.25);
}

.day-selector {
  display: flex;
  gap: 9px;
  overflow-x: auto;
  padding: 0px 0px 10px 10px;
  grid-column: span 2;
}

.day-selector::-webkit-scrollbar {
  height: 10px;
  margin: 4px;
}


.day-btn {
  padding: 8px 15px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
  color: #64748b;
  font-weight: 700;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.02);
}

.day-btn:hover {
  border-color: #7c3aed;
  color: #7c3aed;
  background: #f5f3ff;
  transform: translateY(-2px);
}

.day-btn.active {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
  color: white;
  border-color: #7c3aed;
  box-shadow: 0 8px 15px rgba(124, 58, 237, 0.3);
}

.bottom-actions {
  display: flex;
  gap: 16px;
  padding-top: 16px;
  margin-top: auto;
}

.btn {
  padding: 14px 24px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn:active {
  transform: translateY(2px);
}

.btn-add {
  background: #ffffff;
  color: #475569;
  flex: 1;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

.btn-add:hover {
  background: #f8fafc;
  color: #1e293b;
  border-color: #cbd5e1;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.04);
}

.btn-save {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
  color: white;
  flex: 1;
  box-shadow: 0 4px 15px rgba(124, 58, 237, 0.35);
}

.btn-save:hover {
  filter: brightness(1.1);
  box-shadow: 0 8px 25px rgba(109, 40, 217, 0.45);
  transform: translateY(-3px);
}

/* ===================================================
   拖曳時的 ghost 樣式
   =================================================== */
.drag-ghost {
  opacity: 0.5;
  background: #f8fafc;
  transform: scale(0.98);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  border-color: #7c3aed !important;
}
</style>