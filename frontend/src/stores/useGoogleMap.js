/* ===================================================
   useGoogleMap.js — Google Maps 底層操作的 composable

   職責：所有跟 Google Maps API 直接互動的事情都在這裡。
   包含地圖初始化、marker 操作、geocode、places 查詢、InfoWindow。
   =================================================== */

import { ref } from "vue";
import { setOptions, importLibrary } from "@googlemaps/js-api-loader";
import {
  API_KEY,
  MARKER_LABEL_STYLE,
  MARKER_ICON_URL,
  MARKER_ICON_SIZE,
} from "../config/MapConfig.js";

export function useGoogleMap() {
  const mapReady = ref(false);
  // 預設 false，等 initMap 完成後設 true，保護依賴地圖的操作不會提前執行

  let map = null; // Google Map 實例
  let placesService = null; // PlacesService，做地點搜尋
  let mapsLibrary = null; // 載入後的 maps 模組
  let placesLibrary = null; // 載入後的 places 模組
  let geocoderLibrary = null; // 載入後的 geocoding 模組
  let infoWindow = null; // InfoWindow 實例（全域只建一個，重複使用）
  const markers = []; // 收集所有 marker，用陣列管理

  /* ===================================================
     initMap — 初始化地圖
     =================================================== */
  async function initMap(el, mapOptions = {}) {
    // 設定 API key、版本、語言（zh-TW 讓所有回傳資料和 UI 顯示繁體中文）
    setOptions({ key: API_KEY, v: "weekly", language: "zh-TW" });

    // 平行載入三個模組
    const [maps, places, geocoding] = await Promise.all([
      importLibrary("maps"), // Map class
      importLibrary("places"), // PlacesService、Autocomplete
      importLibrary("geocoding"), // Geocoder
    ]);

    mapsLibrary = maps;
    placesLibrary = places;
    geocoderLibrary = geocoding;

    map = new maps.Map(el, { ...mapOptions, gestureHandling: "greedy" }); // 建立地圖，掛到 DOM
    placesService = new places.PlacesService(map); // 建立地點查詢服務

    mapReady.value = true;
  }

  /* ===================================================
     Getter — 讓外部拿到內部實例
     =================================================== */

  // Autocomplete 元件需要 map 實例來把搜尋框塞進 controls
  function getMap() {
    return map;
  }

  // 建 Autocomplete 時需要 new placesLib.Autocomplete()
  function getPlacesLibrary() {
    return placesLibrary;
  }

  /* ===================================================
     地圖操作
     =================================================== */

  // panTo — 平移地圖到指定座標
  function panTo(lat, lng, zoom = 16) {
    if (!map) return;
    map.panTo({ lat, lng });
    map.setZoom(zoom);
  }

  // onMapClick — 監聽地圖點擊事件
  function onMapClick(callback) {
    if (!map) return;
    map.addListener("click", (event) => callback(event.latLng));
  }

  /* ===================================================
     Places 查詢
     =================================================== */

  // geocode — 經緯度轉地址
  function geocode(latLng) {
    return new Promise((resolve, reject) => {
      const geocoder = new geocoderLibrary.Geocoder();
      geocoder.geocode({ location: latLng }, (results, status) => {
        if (status === "OK" && results[0]) resolve(results[0]);
        else reject(status);
      });
    });
  }

  // nearbySearch — 用座標搜附近最近的商家
  // rankBy DISTANCE 會按距離排序，type 'establishment' 只找商家不找路名
  // 注意：rankBy DISTANCE 不能同時帶 radius 參數
  function nearbySearch(latLng) {
    return new Promise((resolve, reject) => {
      const req = {
        location: latLng, // 搜尋中心點
        rankBy: google.maps.places.RankBy.DISTANCE, // 按距離排序，最近的排第一
        type: "establishment", // 只找商家（餐廳、店家等），排除路名和行政區
      };
      placesService.nearbySearch(req, (results, status) => {
        if (
          status === placesLibrary.PlacesServiceStatus.OK &&
          results.length > 0
        ) {
          resolve(results[0]); // 回傳最近的一間商家
        } else {
          reject(status); // 附近沒商家
        }
      });
    });
  }

  // getPlaceDetails — 用 placeId 查商家詳細資訊
  function getPlaceDetails(
    placeId,
    fields = [
      "name",
      "formatted_address",
      "photos",
      "rating",
      "geometry",
      "place_id",
    ],
  ) {
    return new Promise((resolve, reject) => {
      placesService.getDetails({ placeId, fields }, (place, status) => {
        if (status === placesLibrary.PlacesServiceStatus.OK) resolve(place);
        else reject(status);
      });
    });
  }

  // findPlaceFromQuery — 用文字搜尋地點
  function findPlaceFromQuery(query) {
    return new Promise((resolve, reject) => {
      const request = {
        query,
        fields: [
          "name",
          "geometry",
          "formatted_address",
          "photos",
          "rating",
          "place_id",
        ],
      };
      placesService.findPlaceFromQuery(request, (results, status) => {
        if (status === placesLibrary.PlacesServiceStatus.OK && results[0])
          resolve(results[0]);
        else reject(status);
      });
    });
  }

  /* ===================================================
     Marker 相關操作
     =================================================== */

  // createMarker — 在地圖上建立一個標記點
  function createMarker(latLng, label, onClick) {
    const marker = new google.maps.Marker({
      position: latLng, // 座標
      map, // 放在哪個地圖上
      label: { text: String(label), ...MARKER_LABEL_STYLE }, // 數字標籤
      icon: {
        url: MARKER_ICON_URL, // 圖片 URL（從 MapConfig）
        scaledSize: new google.maps.Size( // 圖片大小
          MARKER_ICON_SIZE.width,
          MARKER_ICON_SIZE.height,
        ),
      },
      animation: google.maps.Animation.DROP, // 掉落動畫
    });

    // 點擊 marker 時把 marker 自己傳給 callback，讓 usePoints 能拿去彈 InfoWindow
    if (onClick) marker.addListener("click", () => onClick(marker));

    markers.push(marker); // 存進陣列方便管理
    return marker;
  }

  // removeMarker — 移除指定 index 的 marker
  function removeMarker(index) {
    if (!markers[index]) return;
    markers[index].setMap(null); // 從地圖上移除
    markers.splice(index, 1); // 從陣列移除
  }

  // clearMarkers — 清除所有 marker
  function clearMarkers() {
    markers.forEach((m) => m.setMap(null)); // 每個都從地圖移除
    markers.length = 0; // 清空陣列
  }

  // relabelMarkers — 重新編號所有 marker（刪除或拖曳換順序後呼叫）
  function relabelMarkers() {
    markers.forEach((m, i) => {
      m.setLabel({ text: String(i + 1), ...MARKER_LABEL_STYLE });
    });
  }

  // reorderMarkers — 根據新的 index 順序重排 markers 內部陣列
  function reorderMarkers(newOrder) {
    const reordered = newOrder.map((i) => markers[i]);
    markers.length = 0;
    markers.push(...reordered);
    relabelMarkers();
  }

  /* ===================================================
     InfoWindow — 商家資訊彈窗

     全域只建一個 InfoWindow，每次呼叫時更新內容。
     同一時間只會有一個彈窗，點別的 marker 舊的自動關掉。
     =================================================== */
  function showInfoWindow(marker, point) {
    // 第一次呼叫才建立（lazy init）
    if (!infoWindow) infoWindow = new google.maps.InfoWindow();

    // 有照片就生成 img 標籤，沒有就空字串
    const photoHtml = point.photos?.length
      ? `<img src="${point.photos[0]}" style="width:100%;max-width:220px;border-radius:6px;margin-bottom:6px;" />`
      : "";

    // 組裝 HTML 內容
    infoWindow.setContent(`
      <div style="font-family:sans-serif;max-width:240px;">
        ${photoHtml}
        <div style="font-weight:700;font-size:14px;margin-bottom:4px;">${point.name || "未命名地點"}</div>
        <div style="color:#666;font-size:12px;margin-bottom:4px;">${point.address || ""}</div>
        ${point.rating ? `<div style="font-size:13px;">⭐ ${point.rating}</div>` : ""}
      </div>
    `);

    // 定位在 marker 上方並打開
    infoWindow.open(map, marker);
  }

  function getMarker(index) {
    return markers[index] || null;
  }

  /* ===================================================
     回傳公開介面
     =================================================== */
  return {
    mapReady,
    initMap,
    getMap,
    getPlacesLibrary,
    showInfoWindow,
    createMarker,
    removeMarker,
    clearMarkers,
    relabelMarkers,
    reorderMarkers,
    panTo,
    onMapClick,
    geocode,
    nearbySearch,
    getPlaceDetails,
    findPlaceFromQuery,
    getMarker,
  };
}
