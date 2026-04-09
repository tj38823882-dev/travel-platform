import request from "@/utils/request";

// 建立旅程
export function createTrip(data) {
  return request.post("/api/user/trips", data);
}

// 拿目前登入者所有旅程
export function getMyTrips() {
  return request.get("/api/user/trips");
}

// 拿單一旅程完整資料
export function getTripById(tripId) {
  return request.get(`/api/user/trips/${tripId}`);
  ㄋ;
}

// 更新旅程
export function updateTrip(tripId, data) {
  return request.put(`/api/user/trips/${tripId}`, data);
}

// 公開行程列表：不需要登入，對應後端 /api/public/tours  目前沒用到
export function getPublicTrips() {
  return request.get("/api/public/tours");
}

export function getItineraryById(itineraryId) {
  return request.get(`/api/public/itineraries/${itineraryId}`);
}
