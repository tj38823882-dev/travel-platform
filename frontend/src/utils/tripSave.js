import { createTrip, updateTrip } from "@/utils/tripApi";

export async function saveTrip({
  tripId,
  tripTitle,
  startDate,
  endDate,
  coverImage,
  hashTag,

  dayStartTimes,
  currentDay,
  allDaysItinerary,
  dayTitles,
  points,
}) {
  // 1. 同步當前天到 allDaysItinerary，保留可能存在的 dbDayId
  const currentPointsArr = [...points];
  currentPointsArr.dbDayId = allDaysItinerary[currentDay]?.dbDayId || null;
  allDaysItinerary[currentDay] = currentPointsArr;

  // 2. 依據日期計算總天數，確保只儲存範圍內的天數
  const startObj = new Date(startDate);
  const endObj = new Date(endDate);
  const totalDays = Math.ceil((endObj - startObj) / (1000 * 60 * 60 * 24)) + 1;

  // 3. 建立 tripDays 陣列 (1 ~ totalDays)
  const tripDaysPayload = [];
  for (let i = 1; i <= totalDays; i++) {
    const pts = allDaysItinerary[i] || [];
    // 取得該天的日期物件並避免 ISO 8601 時區偏移問題
    const dateObj = new Date(startDate);
    dateObj.setDate(dateObj.getDate() + (i - 1));
    const y = dateObj.getFullYear();
    const m = String(dateObj.getMonth() + 1).padStart(2, "0");
    const d = String(dateObj.getDate()).padStart(2, "0");
    const dateStr = `${y}-${m}-${d}`;

    tripDaysPayload.push({
      id: pts.dbDayId || null,
      dayNumber: i,
      theDate: dateStr,
      title: dayTitles?.[i] || null,
      startTime: dayStartTimes?.[i] || "08:00",
      stops: pts.map((p, idx) => ({
        id: p.dbId || null,
        name: p.name,
        placeId: p.placeId,
        lat: p.lat,
        lng: p.lng,
        address: p.address,
        stayMinutes: p.stayMinutes,
        travelMinutes: p.travelMinutes,
        travelMode: p.travelMode,
        note: p.note || null,
        orderIndex: idx,
      })),
    });
  }

  const payload = {
    title: tripTitle,
    startDate: startDate,
    endDate: endDate,
    coverImage: coverImage || null,
    hashTag: hashTag || null,
    tripDays: tripDaysPayload,
  };

  if (tripId) {
    await updateTrip(tripId, payload);
  } else {
    const res = await createTrip(payload);
    return res.data.id;
  }
}
