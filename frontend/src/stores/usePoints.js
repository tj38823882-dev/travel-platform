/* ===================================================
   usePoints.js — 地點管理的 composable

   職責：管理使用者加入的地點（新增、刪除、清除、選取、排序）。
   它不直接碰 Google Maps API，而是透過 useGoogleMap 提供的方法操作。

   每個 point 物件的結構：
   {
     id,            // 唯一識別碼（遞增整數）
     lat, lng,      // 經緯度
     address,       // 地址字串
     name,          // 商家名稱（可被使用者 rename）
     rating,        // Google 評分
     photos,        // 照片 URL 陣列（最多 3 張）
     placeId,       // Google Place ID
     time,          // 到達時間，格式 "HH:MM"（預設空字串）
     travelMode,    // 到下一個點的交通方式：'WALKING'|'DRIVING'|'TRANSIT'|'CUSTOM'
     travelMinutes, // 交通時間（非 CUSTOM 由 API 填入，CUSTOM 手動輸入）
     stayMinutes,   // 停留時間（純手動）
     routeSteps     // TRANSIT 分段明細陣列（非 TRANSIT 為空陣列）
   }

   到達時間計算：前一個 point 的 travelMinutes + stayMinutes 累加
   =================================================== */

import { ref } from "vue";

export function usePoints(googleMap, directions) {
  const points = ref([]);
  const selectedPoint = ref(null);
  let idCounter = 0;

  /* ===================================================
     _buildPoint — 組裝 point 資料物件（內部用）
     =================================================== */
  function _buildPoint(latLng, place, address) {
    return {
      id: ++idCounter,
      lat: typeof latLng.lat === "function" ? latLng.lat() : latLng.lat,
      lng: typeof latLng.lng === "function" ? latLng.lng() : latLng.lng,
      address: place?.formatted_address || address || "",
      name: place?.name || "",
      rating: place?.rating || null,
      photos: place?.photos
        ? place.photos.slice(0, 3).map((p) => p.getUrl({ maxWidth: 400 }))
        : [],
      placeId: place?.place_id || null,
      time: "",
      travelMode: "WALKING", // 預設步行，新增瞬間自動查路線
      travelMinutes: 0,
      stayMinutes: 0,
      routeSteps: [],
      note: "", // ← 加這行
    };
  }

  /* ===================================================
     _addPointAndMarker — 加入陣列 + 建 marker（內部用）
     =================================================== */
  function _addPointAndMarker(point) {
    const label = points.value.length + 1;

    const marker = googleMap.createMarker(
      { lat: point.lat, lng: point.lng },
      label,
      (m) => {
        selectedPoint.value = point;
        googleMap.showInfoWindow(m, point);
      },
    );

    points.value.push(point);
    selectedPoint.value = point;
    return marker;
  }

  /* ===================================================
     預覽邏輯：點擊地圖或搜尋，先出現預覽標記，點擊「加入」才放入行程
     =================================================== */
  async function _autoRoute(newPoint) {
    const idx = points.value.length - 1; // 新地點的 index
    if (idx === 0) return; // 第一個點不需要查

    const prev = points.value[idx - 1];
    if (!prev.lat || !prev.lng) return;

    // 用前一個點的 travelMode 查路線
    const { minutes, steps } = await directions.drawRoute(
      idx - 1,
      prev,
      newPoint,
      prev.travelMode,
    );
    if (minutes !== null) {
      prev.travelMinutes = minutes;
      prev.routeSteps = steps;
    }
  }

  /* ===================================================
     updateTravelMode — 切換交通方式
     非 CUSTOM 自動查路線更新 travelMinutes；CUSTOM 清路線
     =================================================== */
  async function updateTravelMode(id, mode) {
    const index = points.value.findIndex((p) => p.id === id);
    if (index === -1) return;

    const point = points.value[index];
    point.travelMode = mode;

    if (mode === "CUSTOM") {
      directions.clearRoute(index);
      point.travelMinutes = 0;
      point.routeSteps = [];
      return;
    }

    const next = points.value[index + 1];
    if (!next?.lat || !next?.lng) return;

    const { minutes, steps } = await directions.drawRoute(
      index,
      point,
      next,
      mode,
    );
    if (minutes !== null) {
      point.travelMinutes = minutes;
      point.routeSteps = steps;
    }
  }

  /* ===================================================
     addFromLatLng — 從地圖點擊新增地點
     =================================================== */
  async function addFromLatLng(latLng) {
    try {
      let place = null;
      let address = "";

      try {
        const nearby = await googleMap.nearbySearch(latLng);
        place = await googleMap.getPlaceDetails(nearby.place_id);
      } catch (_) {}

      if (!place) {
        try {
          const geocodeResult = await googleMap.geocode(latLng);
          address = geocodeResult.formatted_address;
        } catch (_) {}
      }

      const point = _buildPoint(latLng, place, address);
      point.placeId = place?.place_id || null;
      // 0311 修改加入 placeId 到資料庫
      const marker = _addPointAndMarker(point);
      googleMap.showInfoWindow(marker, point);

      // 自動查前一段路線
      await _autoRoute(point);

      return point;
    } catch (err) {
      console.warn("addFromLatLng failed:", err);
      return null;
    }
  }

  /* ===================================================
     addFromSearch — 從搜尋文字新增地點
     =================================================== */
  async function addFromSearch(query) {
    if (!query || query.length < 2) return null;

    try {
      const place = await googleMap.findPlaceFromQuery(query);
      const latLng = place.geometry.location;
      const point = _buildPoint(latLng, place, query);
      point.placeId = place.place_id || null;

      const marker = _addPointAndMarker(point);
      googleMap.panTo(point.lat, point.lng, 15);
      googleMap.showInfoWindow(marker, point);

      await _autoRoute(point);

      return point;
    } catch (err) {
      console.warn("addFromSearch failed:", err);
      return null;
    }
  }

  /* ===================================================
     addFromPlace — 從 Google Autocomplete 的 place 物件新增
     =================================================== */
  async function addFromPlace(place) {
    if (!place?.geometry) return null;

    const latLng = place.geometry.location;
    const point = _buildPoint(latLng, place, place.formatted_address);
    point.placeId = place.place_id || null;

    const marker = _addPointAndMarker(point);
    googleMap.panTo(point.lat, point.lng, 15);
    googleMap.showInfoWindow(marker, point);

    await _autoRoute(point);

    return point;
  }

  /* ===================================================
     addEmpty — 新增空的地點（手動新增按鈕用）
     =================================================== */
  function addEmpty() {
    const point = {
      id: ++idCounter,
      lat: 0,
      lng: 0,
      address: "",
      name: "",
      rating: null,
      photos: [],
      placeId: null,
      time: "",
      travelMode: "CUSTOM",
      travelMinutes: 0,
      stayMinutes: 0,
      routeSteps: [],
    };
    points.value.push(point);
    return point;
  }

  /* ===================================================
     removePoint — 移除單一地點
     =================================================== */
  function removePoint(id) {
    const index = points.value.findIndex((p) => p.id === id);
    if (index === -1) return -1;

    directions.clearRoute(index);
    if (index > 0) directions.clearRoute(index - 1);

    if (points.value[index].lat !== 0 || points.value[index].lng !== 0) {
      googleMap.removeMarker(index);
      googleMap.relabelMarkers();
    }

    points.value.splice(index, 1);
    if (selectedPoint.value?.id === id) selectedPoint.value = null;

    return index;
  }

  /* ===================================================
     clearAll — 清除所有地點
     =================================================== */
  function clearAll() {
    directions.clearAllRoutes();
    googleMap.clearMarkers();
    points.value = [];
    selectedPoint.value = null;
  }

  /* ===================================================
     selectPoint — 選取一個地點
     =================================================== */
  function selectPoint(point) {
    selectedPoint.value = point;
    if (point.lat && point.lng) {
      googleMap.panTo(point.lat, point.lng, 13);

      // 找到對應的 marker 顯示 InfoWindow
      const index = points.value.findIndex((p) => p.id === point.id);
      const marker = googleMap.getMarker(index); // ← 需要確認你有沒有這個方法
      if (marker) googleMap.showInfoWindow(marker, point);
    }
  }

  /* ===================================================
     renamePoint — 重新命名地點
     =================================================== */
  function renamePoint(id, newName) {
    const point = points.value.find((p) => p.id === id);
    if (point) point.name = newName;
  }

  /* ===================================================
     updatePointTime — 更新到達時間
     =================================================== */
  function updatePointTime(id, time) {
    const point = points.value.find((p) => p.id === id);
    if (point) point.time = time;
  }

  /* ===================================================
     updateTravelMinutes — CUSTOM 模式手動輸入交通時間
     =================================================== */
  function updateTravelMinutes(id, minutes) {
    const point = points.value.find((p) => p.id === id);
    if (point) point.travelMinutes = minutes;
  }

  /* ===================================================
     updateStayMinutes — 更新停留時間
     =================================================== */
  function updateStayMinutes(id, minutes) {
    const point = points.value.find((p) => p.id === id);
    if (point) point.stayMinutes = minutes;
  }

  /* ===================================================
 
 usePoints.js — 新增 updateNote function
     =================================================== */
  function updateNote(id, note) {
    const point = points.value.find((p) => p.id === id);
    if (point) point.note = note;
  }

  /* ===================================================
     syncMarkersToPoints — 拖曳換順序後同步 markers 和路線
     =================================================== */
  async function syncMarkersToPoints() {
    googleMap.clearMarkers();

    points.value.forEach((point, index) => {
      if (point.lat !== 0 || point.lng !== 0) {
        googleMap.createMarker(
          { lat: point.lat, lng: point.lng },
          index + 1,
          (m) => {
            selectedPoint.value = point;
            googleMap.showInfoWindow(m, point);
          },
        );
      }
    });

    await directions.redrawAllRoutes(points.value);
  }

  /* ===================================================
     setIdCounter — 從 DB 載入行程後，同步 id 計數器
     避免新增地點時 id 與已載入的地點衝突
     =================================================== */
  function setIdCounter(maxId) {
    idCounter = maxId;
  }

  return {
    points,
    selectedPoint,
    addFromLatLng,
    addFromSearch,
    addFromPlace,
    addEmpty,
    removePoint,
    clearAll,
    selectPoint,
    renamePoint,
    updatePointTime,
    updateTravelMinutes,
    updateStayMinutes,
    updateNote,
    updateTravelMode,
    syncMarkersToPoints,
    setIdCounter,
  };
}
