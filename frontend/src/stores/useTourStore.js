import { defineStore } from "pinia";

export const useTourStore = defineStore("tour", {
  state: () => ({
    // 擴充後的行程資料
    tours: [
      {
        id: 1,
        name: "東京 5 天 4 夜櫻花季",
        location: "日本, 東京",
        price: 25000,
        category: "賞花",
        image:
          "https://images.unsplash.com/photo-1522383225653-ed111181a951?q=80&w=400", // Ueno Park / Sakura
        description: "漫步上野公園，享受最浪漫的櫻花祭。",
        itinerary: [
          {
            day: 1,
            title: "抵達東京 - 上野公園散策",
            detail:
              "抵達後先前往上野公園，這裡是東京最具代表性的賞櫻勝地，擁有超過一千棵櫻花樹。",
            isLocked: false,
          },
          {
            day: 2,
            title: "新宿御苑與澀谷展望台",
            detail:
              "新宿御苑融合了日式、英式與法式庭園風格。晚上登上 SHIBUYA SKY，從 229 公尺高空俯瞰東京夜景。",
            isLocked: true,
          },
          {
            day: 3,
            title: "千鳥之淵划船體驗",
            detail:
              "在皇居護城河上划船，兩岸櫻花如瀑布般傾瀉而下，是東京最美的風景。",
            isLocked: true,
          },
        ],
      },
      {
        id: 2,
        name: "京都古蹟巡禮",
        location: "日本, 京都",
        price: 18000,
        category: "文化",
        image:
          "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=400", // Yasaka Pagoda (Kyoto)
        description: "造訪金閣寺與伏見稻荷大社，感受千年古都魅力。",
        itinerary: [
          {
            day: 1,
            title: "清水寺與二年坂",
            detail: "參觀宏偉的清水舞台，並在古色古香的街道穿著和服漫步。",
            isLocked: false,
          },
          {
            day: 2,
            title: "金閣寺與龍安寺石庭",
            detail: "欣賞金箔裝飾的舍利殿倒映在鏡湖池中，隨後體驗枯山水禪意。",
            isLocked: true,
          },
        ],
      },
      {
        id: 3,
        name: "北海道滑雪初體驗",
        location: "日本, 北海道",
        price: 32000,
        category: "運動",
        image:
          "https://images.unsplash.com/photo-1551524559-8af4e6624178?q=80&w=400", // Skiing
        description: "入住優質雪場飯店，體驗頂級粉雪。",
        itinerary: [
          {
            day: 1,
            title: "抵達新千歲機場－前往滑雪度假村",
            detail:
              "抵達北海道新千歲機場後，搭乘接駁車前往知名滑雪度假村，辦理入住並熟悉雪場環境。",
            isLocked: false,
          },
          {
            day: 2,
            title: "滑雪基礎教學與自由滑行",
            detail:
              "由專業教練進行滑雪基礎教學，下午可依程度自由滑行，體驗北海道細緻粉雪。",
            isLocked: true,
          },
          {
            day: 3,
            title: "進階雪道挑戰與溫泉放鬆",
            detail: "挑戰中高階雪道，晚上享受日式露天溫泉，舒緩滑雪後的疲勞。",
            isLocked: true,
          },
        ],
      },
      {
        id: 4,
        name: "台北美食探索之旅",
        location: "台灣, 台北",
        price: 5000,
        category: "美食",
        image:
          "https://images.unsplash.com/photo-1534452203293-494d7ddbf7e0?q=80&w=400", // Taipei 101 or Night Market
        description: "從饒河夜市到私廚料理，吃遍台北大街小巷。",
        itinerary: [
          {
            day: 1,
            title: "饒河夜市與米其林小吃",
            detail: "走訪饒河夜市，品嚐胡椒餅、蚵仔煎與米其林推薦街頭美食。",
            isLocked: false,
          },
          {
            day: 2,
            title: "私廚料理與咖啡館巡禮",
            detail:
              "中午安排預約制私廚料理，下午前往大稻埕與中山區特色咖啡館。",
            isLocked: true,
          },
        ],
      },
      {
        id: 5,
        name: "巴黎羅浮宮藝術導覽",
        location: "法國, 巴黎",
        price: 45000,
        category: "藝術",
        image:
          "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?q=80&w=400", // Louvre/Paris
        description: "專業導師深入淺出講解世界三大名畫。",
        itinerary: [
          {
            day: 1,
            title: "羅浮宮經典館藏導覽",
            detail:
              "由專業藝術導師帶領參觀《蒙娜麗莎》、《勝利女神像》與《米洛的維納斯》。",
            isLocked: false,
          },
          {
            day: 2,
            title: "奧賽美術館與塞納河漫步",
            detail: "欣賞印象派名作，傍晚沿塞納河散步，感受巴黎藝術氛圍。",
            isLocked: true,
          },
          {
            day: 3,
            title: "蒙馬特藝術街區",
            detail: "走訪蒙馬特高地、聖心堂與畫家廣場，體驗巴黎藝術家生活。",
            isLocked: true,
          },
        ],
      },
    ],
  }),
  actions: {
    addTour(newTour) {
      this.tours.push({
        id: Date.now(), // 簡單生成一個 ID
        ...newTour,
      });
    },
    // 刪除行程
    removeTour(id) {
      this.tours = this.tours.filter((t) => t.id !== id);
    },
  },
});
