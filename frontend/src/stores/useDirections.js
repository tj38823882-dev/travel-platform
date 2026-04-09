/* ===================================================
   useDirections.js — 路線查詢與繪製

   職責：
   - 呼叫 DirectionsService 查詢兩點間路線與預估時間
   - 用 DirectionsRenderer 把路線畫在地圖上
   - 解析 TRANSIT steps 明細（公車、捷運等分段資訊）
   - 每段路線各自有一個 renderer，可獨立清除或重繪
   =================================================== */

export function useDirections(googleMap) {

  // key: `${fromIndex}-${fromIndex+1}`，value: DirectionsRenderer 實例
  const renderers = {}

  /* ===================================================
     _parseSteps — 把 DirectionsResult 的 steps 解析成乾淨格式
     =================================================== */
  function _parseSteps(legs) {
    return legs[0].steps.map(step => {
      const base = {
        mode: step.travel_mode,
        instruction: step.instructions?.replace(/<[^>]*>/g, '') || '',
        duration: Math.ceil(step.duration.value / 60),
        distance: step.distance?.text || ''
      }

      if (step.travel_mode === 'TRANSIT' && step.transit) {
        const t = step.transit
        base.transit = {
          lineName: t.line?.short_name || t.line?.name || '',
          lineColor: t.line?.color || '#6366f1',
          vehicleType: t.line?.vehicle?.type || '',
          vehicleIcon: _vehicleIcon(t.line?.vehicle?.type),
          departureStop: t.departure_stop?.name || '',
          arrivalStop: t.arrival_stop?.name || '',
          numStops: t.num_stops || 0
        }
      }

      return base
    })
  }

  function _vehicleIcon(type) {
    const map = { BUS: '🚌', SUBWAY: '🚇', RAIL: '🚆', TRAM: '🚊', FERRY: '⛴️' }
    return map[type] || '🚌'
  }

  /* ===================================================
     drawRoute — 查詢並畫路線，回傳 { minutes, steps }
     =================================================== */
  async function drawRoute(fromIndex, origin, destination, travelMode) {
    const key = `${fromIndex}-${fromIndex + 1}`

    try {
      const map = googleMap.getMap()

      const result = await new Promise((resolve, reject) => {
        new google.maps.DirectionsService().route({
          origin: { lat: origin.lat, lng: origin.lng },
          destination: { lat: destination.lat, lng: destination.lng },
          travelMode: google.maps.TravelMode[travelMode]
        }, (result, status) => {
          if (status === 'OK') resolve(result)
          else reject(status)
        })
      })

      // 清舊的再畫新的
      if (renderers[key]) renderers[key].setMap(null)

      const renderer = new google.maps.DirectionsRenderer({
        map,
        directions: result,
        suppressMarkers: true,
        polylineOptions: {
          strokeColor: travelMode === 'WALKING' ? '#075310' : travelMode === 'TRANSIT' ? '#170bf5' : '#db0909',
          strokeWeight: 4,
          strokeOpacity: 0.8
        }
      })

            renderers[key] = renderer

            const steps = _parseSteps(result.routes[0].legs)
            const minutes = Math.ceil(result.routes[0].legs[0].duration.value / 60)

            return { minutes, steps }

          } catch (err) {
            console.warn(`drawRoute failed (${key}):`, err)
            return { minutes: null, steps: [] }
          }
        }

  /* ===================================================
     clearRoute — 清除指定段路線
     =================================================== */
  function clearRoute(fromIndex) {
    const key = `${fromIndex}-${fromIndex + 1}`
    if (renderers[key]) {
      renderers[key].setMap(null)
      delete renderers[key]
    }
  }

  /* ===================================================
     clearAllRoutes — 清除所有路線
     =================================================== */
  function clearAllRoutes() {
    Object.values(renderers).forEach(r => r.setMap(null))
    Object.keys(renderers).forEach(k => delete renderers[k])
  }

  /* ===================================================
     redrawAllRoutes — 拖曳換順序後重畫所有路線
     直接修改傳入的 points 陣列的 travelMinutes 和 routeSteps
     =================================================== */
  async function redrawAllRoutes(points) {
    clearAllRoutes()
    for (let i = 0; i < points.length - 1; i++) {
      const from = points[i]
      const to = points[i + 1]
      if (from.lat && from.lng && to.lat && to.lng && from.travelMode !== 'CUSTOM') {
        const { minutes, steps } = await drawRoute(i, from, to, from.travelMode)
        if (minutes !== null) {
          from.travelMinutes = minutes
          from.routeSteps = steps
        }
      }
    }
  }

  return {
    drawRoute,
    clearRoute,
    clearAllRoutes,
    redrawAllRoutes
  }
}
