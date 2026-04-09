<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from '@/stores/authStore';
import { useToastStore } from '@/stores/toastStore';
import request from '@/utils/request';
import { wsUrl } from '@/config/appConfig.js';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const toastStore = useToastStore();

const roomId = route.params.roomId;
const canvasRef = ref(null);
const ctx = ref(null);
// 本地使用者資料 (避免 store 狀態未同步導致 crash)
const currentUser = ref(null);

// 確認框狀態
const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    onConfirm: null // 儲存點擊「確定」後要執行的動作
});

// 遊戲與計時狀態
const gameState = ref('waiting'); // 'waiting' | 'playing' | 'finished'
const timeLeft = ref(10); // 測試用 10 秒
let timerInterval = null;
const showResultModal = ref(false); // 結算視窗
const resultImage = ref(''); // 結算圖片預覽 (Base64)

// 防止重複觸發關閉流程 (例如發布成功後的 10 秒內又收到斷線通知)
const isClosing = ref(false);

// 繪圖狀態
const isDrawing = ref(false);
const brushColor = ref('#000000');
const brushSize = ref(5);

// WebSocket 相關
let stompClient = null;
const connected = ref(false);
const peers = ref(new Map()); // 儲存房間內其他人的資訊 { userId: userData }
const collaboratorId = ref(null); // 鎖定第一位協作者的 ID

const collaboratorName = computed(() => {
    if (peers.value.size > 0) {
        const firstPeer = peers.value.values().next().value;
        return firstPeer.username || '夥伴';
    }
    return '';
});

// 格式化時間 mm:ss
const formattedTime = computed(() => {
    const m = Math.floor(timeLeft.value / 60);
    const s = timeLeft.value % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
});

// 視窗大小 (用於座標換算)
const canvasWidth = ref(0);
const canvasHeight = ref(0);

// 節流控制 (避免 mousemove 發送太頻繁)
let lastSendTime = 0;
const SEND_INTERVAL = 20; // ms

// --- 初始化 ---
onMounted(async () => {
    if (!authStore.isLoggedIn) {
        toastStore.addToast('請先登入', 'error');
        router.push('/loginView');
        return;
    }

    // 🛠️ 新增：確保取得使用者資料
    try {
        const res = await request.post('/api/user/whoami');
        currentUser.value = res.data;
    } catch (e) {
        console.error('無法取得使用者資料', e);
    }

    initCanvas();
    window.addEventListener('resize', resizeCanvas);
    connectWebSocket();
});

onUnmounted(() => {
    if (stompClient) {
        if (timerInterval) clearInterval(timerInterval); // 清除計時器
        // 離開前發送離開訊息 (可選)
        stompClient.deactivate();
    }
    window.removeEventListener('resize', resizeCanvas);
});

// --- Canvas 設定 ---
const initCanvas = () => {
    const canvas = canvasRef.value;
    if (!canvas) return;

    resizeCanvas();
    ctx.value = canvas.getContext('2d', { willReadFrequently: true });

    // 設定線條樣式
    ctx.value.lineCap = 'round';
    ctx.value.lineJoin = 'round';

    // 預設填滿白底 (不然存檔變透明)
    ctx.value.fillStyle = '#ffffff';
    ctx.value.fillRect(0, 0, canvas.width, canvas.height);
};

const resizeCanvas = () => {
    const canvas = canvasRef.value;
    const container = canvas.parentElement;
    if (canvas && container) {
        // 儲存目前的影像內容
        let tempImg = null;
        if (ctx.value) {
            tempImg = ctx.value.getImageData(0, 0, canvas.width, canvas.height);
        }

        canvas.width = container.clientWidth;
        canvas.height = container.clientHeight;
        canvasWidth.value = canvas.width;
        canvasHeight.value = canvas.height;

        // 恢復 context 設定
        if (ctx.value) {
            ctx.value.lineCap = 'round';
            ctx.value.lineJoin = 'round';
            // 如果有舊圖，畫回去 (簡單處理，縮放可能會跑版，這只是防止視窗改變全白)
            if (tempImg) ctx.value.putImageData(tempImg, 0, 0);
        }
    }
};

// --- WebSocket 連線 ---
const connectWebSocket = () => {
    stompClient = new Client({
        brokerURL: wsUrl,
        onConnect: () => {
            connected.value = true;
            console.log('【Debug】WebSocket 連線成功！準備發送 JOIN...');
            toastStore.addToast('已連線至畫布房間', 'success');

            // 1. 訂閱房間頻道
            stompClient.subscribe(`/topic/draw/${roomId}`, (message) => {
                const action = JSON.parse(message.body);
                console.log('【Debug】收到廣播訊息:', action);
                handleRemoteAction(action);
            });

            // 2. 發送加入訊息 (讓別人知道我是誰)
            sendAction({
                type: 'JOIN',
                senderId: currentUser.value?.id, // 🛠️ 修正：改用本地 currentUser
                username: currentUser.value?.username || authStore.username,
                roomId: roomId // 🛠️ 修正：補上 roomId
            });
        },
        onStompError: (frame) => {
            console.error('WS Error', frame);
            toastStore.addToast('連線發生錯誤', 'error');
        },
        // 🛠️ 新增：捕捉底層 WebSocket 連線失敗 (例如 403 Forbidden)
        onWebSocketError: (event) => {
            console.error('【Debug】WebSocket 握手失敗 (可能是 Token 過期):', event);
            toastStore.addToast('無法連線 (請嘗試重新登入)', 'error');
        }
    });
    stompClient.activate();
};

// --- 遊戲邏輯 ---
const checkStartGame = () => {
    // 如果還在等待中，且有人加入了，就開始計時
    if (gameState.value === 'waiting' && peers.value.size > 0) {
        startGame();
    }
};

const startGame = () => {
    gameState.value = 'playing';
    timeLeft.value = 10; // 設定倒數時間 (秒)
    toastStore.addToast('挑戰開始！', 'info');

    if (timerInterval) clearInterval(timerInterval);
    timerInterval = setInterval(() => {
        timeLeft.value--;
        if (timeLeft.value <= 0) {
            finishGame();
        }
    }, 1000);
};

const finishGame = () => {
    if (timerInterval) clearInterval(timerInterval);
    gameState.value = 'finished';
    isDrawing.value = false; // 強制停止繪圖

    // 產生預覽圖供結算視窗使用
    if (canvasRef.value) {
        resultImage.value = canvasRef.value.toDataURL("image/png");
    }
    showResultModal.value = true; // 顯示結算視窗
};

// --- 繪圖事件處理 (本地) ---
const getCoords = (e) => {
    const canvas = canvasRef.value;
    const rect = canvas.getBoundingClientRect();
    let clientX = e.clientX;
    let clientY = e.clientY;

    // 支援觸控
    if (e.touches && e.touches.length > 0) {
        clientX = e.touches[0].clientX;
        clientY = e.touches[0].clientY;
    }

    return {
        x: clientX - rect.left,
        y: clientY - rect.top
    };
};

const startDrawing = (e) => {
    if (gameState.value !== 'playing') {
        if (gameState.value === 'finished') toastStore.addToast('時間到！無法再作畫', 'warning');
        return;
    }

    isDrawing.value = true;
    const { x, y } = getCoords(e);

    drawLocal(x, y, 'start');
    sendDrawAction('start', x, y);
};

const draw = (e) => {
    if (gameState.value !== 'playing') return;

    if (!isDrawing.value) return;
    e.preventDefault(); // 防止手機捲動
    const { x, y } = getCoords(e);

    drawLocal(x, y, 'draw');

    // 節流發送
    const now = Date.now();
    if (now - lastSendTime > SEND_INTERVAL) {
        sendDrawAction('draw', x, y);
        lastSendTime = now;
    }
};

const stopDrawing = () => {
    if (!isDrawing.value) return;
    isDrawing.value = false;
    ctx.value.closePath();
    sendDrawAction('end', 0, 0); // 結束座標不重要
};

// 本地繪圖執行 (不透過 WS 回傳才畫，避免延遲)
const drawLocal = (x, y, type) => {
    if (!ctx.value) return;

    ctx.value.strokeStyle = brushColor.value;
    ctx.value.lineWidth = brushSize.value;

    if (type === 'start') {
        ctx.value.beginPath();
        ctx.value.moveTo(x, y);
    } else if (type === 'draw') {
        ctx.value.lineTo(x, y);
        ctx.value.stroke();
    }
};

// --- 發送 WebSocket 訊息 ---
const sendDrawAction = (type, x, y) => {
    if (!stompClient || !connected.value) return;

    // 轉換為百分比座標 (0.0 ~ 1.0)
    const normalizedX = x / canvasWidth.value;
    const normalizedY = y / canvasHeight.value;

    const payload = {
        type,
        x: normalizedX,
        y: normalizedY,
        color: brushColor.value,
        size: brushSize.value,
        senderId: currentUser.value?.id, // 🛠️ 修正：改用本地 currentUser
        roomId
    };

    sendAction(payload);
};

const sendAction = (payload) => {
    stompClient.publish({
        destination: `/app/draw/${roomId}`,
        body: JSON.stringify(payload),
        skipContentLengthHeader: true // 有時候加上這個能避免某些編碼問題，非必須但可嘗試
    });
};

// --- 處理遠端訊息 ---
const handleRemoteAction = (action) => {
    // 處理房間過期錯誤
    if (action.type === 'ERROR') {
        toastStore.addToast('此畫布房間已關閉', 'error');
        setTimeout(() => window.location.href = '/MessageBoard', 1500);
        return;
    }

    // 處理夥伴主動離開 (給予 10 秒緩衝)
    if (action.type === 'LEAVE_ROOM') {
        isClosing.value = true;
        toastStore.addToast('夥伴已放棄創作並離開，房間將在 10 秒後關閉', 'warning');
        setTimeout(() => window.location.href = '/MessageBoard', 10000);
        return;
    }

    // 處理發布成功通知 (給予 10 秒緩衝)
    if (action.type === 'POST_SUCCESS') {
        isClosing.value = true;
        toastStore.addToast('夥伴已發布貼文！房間將在 10 秒後關閉', 'success');
        setTimeout(() => window.location.href = '/MessageBoard', 10000);
        return;
    }

    // 處理房間強制關閉 (有人離開)
    if (action.type === 'ROOM_CLOSED') {
        if (isClosing.value) return; // 如果已經在發布成功或主動離開的倒數中，忽略斷線導致的關閉訊號
        toastStore.addToast('夥伴已離開畫布，房間即將關閉', 'warning');
        setTimeout(() => window.location.href = '/MessageBoard', 1500);
        return;
    }

    // 忽略自己的訊息
    if (action.senderId === currentUser.value?.id) return; // 修正：改用本地 currentUser

    // 處理人員加入
    if (action.type === 'JOIN') {
        if (!peers.value.has(action.senderId)) {
            peers.value.set(action.senderId, action);
            collaboratorId.value = action.senderId; // 鎖定協作者
            toastStore.addToast(`${action.username || '夥伴'} 加入了房間`, 'info');

            // 回覆 WELCOME 讓對方也知道我在這
            sendAction({
                type: 'WELCOME',
                senderId: currentUser.value?.id, // 修正：改用本地 currentUser
                username: currentUser.value?.username,
                roomId: roomId
            });
            checkStartGame(); // 嘗試開始遊戲
        }
        return;
    }

    // 處理 WELCOME (我加入時收到的回覆)
    if (action.type === 'WELCOME') {
        if (!peers.value.has(action.senderId)) { // 🛠️ 修正：統一使用 senderId
            peers.value.set(action.senderId, action);
            collaboratorId.value = action.senderId;
            checkStartGame(); // 嘗試開始遊戲
        }
        return;
    }

    if (action.type === 'CLEAR') {
        clearCanvasLocal();
        toastStore.addToast('對方清空了畫布', 'info');
        return;
    }

    // 處理繪圖動作
    if (!ctx.value) return;

    // 還原座標
    const targetX = action.x * canvasWidth.value;
    const targetY = action.y * canvasHeight.value;

    ctx.value.lineWidth = action.size;
    ctx.value.strokeStyle = action.color;

    if (action.type === 'start') {
        ctx.value.beginPath();
        ctx.value.moveTo(targetX, targetY);
    } else if (action.type === 'draw') {
        ctx.value.lineTo(targetX, targetY);
        ctx.value.stroke();
    } else if (action.type === 'end') {
        ctx.value.closePath();
    }
};

// --- 確認框控制函式 ---
const askConfirm = (title, message, callback) => {
    confirmDialog.value.title = title;
    confirmDialog.value.message = message;
    confirmDialog.value.onConfirm = callback;
    confirmDialog.value.show = true;
};

const handleDialogConfirm = () => {
    if (confirmDialog.value.onConfirm) {
        confirmDialog.value.onConfirm();
    }
    confirmDialog.value.show = false;
};

const confirmLeave = () => {
    askConfirm(
        '離開畫布',
        '確定要離開嗎？對方也將強制離開此畫布。',
        () => {
            // 1. 通知夥伴你要離開了
            sendAction({ type: 'LEAVE_ROOM', roomId, senderId: currentUser.value?.id });
            // 2. 稍微延遲跳轉，確保 WebSocket 訊息有送出去
            setTimeout(() => window.location.href = '/MessageBoard', 500);
        }
    );
};

// --- 功能按鈕 ---
const clearCanvas = () => {
    askConfirm('清空畫布', '確定要清空畫布嗎？此操作無法復原。', () => {
        clearCanvasLocal();
        sendAction({ type: 'CLEAR', roomId, senderId: currentUser.value?.id });
    });
};

const clearCanvasLocal = () => {
    ctx.value.fillStyle = '#ffffff';
    ctx.value.fillRect(0, 0, canvasRef.value.width, canvasRef.value.height);
};

const saveAndPost = () => {
    // 透過結算視窗發布，不再需要額外 confirm，直接上傳
    // 關閉結算視窗，避免重複點擊
    showResultModal.value = false;

    canvasRef.value.toBlob(async (blob) => {
        const formData = new FormData();
        formData.append('file', blob, 'collaboration.png');
        formData.append('userId', currentUser.value?.id);

        // 如果有協作者 ID 就傳，沒有就傳自己或空 (後端需容錯)
        if (collaboratorId.value) {
            formData.append('collaboratorId', collaboratorId.value);
        } else {
            // 為了測試方便，若無人則不傳或傳自己，視後端邏輯而定
            // 這裡假設後端允許單人測試，或你自己扮演兩人
            formData.append('collaboratorId', currentUser.value?.id);
        }

        formData.append('content', ''); // 讓後端自動生成標題

        try {
            await request.post('/api/messageboard/post/collaborative', formData);

            // 1. 通知夥伴發布成功 (讓他有 10 秒緩衝)
            sendAction({ type: 'POST_SUCCESS', roomId, senderId: currentUser.value?.id });

            toastStore.addToast('協作貼文發布成功！', 'success');

            // 2. 稍微延遲跳轉，確保 WebSocket 訊息有送出去
            setTimeout(() => window.location.href = '/MessageBoard', 500);
        } catch (error) {
            console.error(error);
            toastStore.addToast('發布失敗', 'error');
        }
    });
};
</script>

<template>
    <div class="draw-view-container">
        <div class="glass-panel main-panel">
            <!-- 頂部工具列 -->
            <div class="toolbar">
                <div class="left-tools">
                    <button class="btn-back" @click="confirmLeave">
                        <i class="bi bi-arrow-left"></i> 離開
                    </button>
                    <span class="room-info">
                        <i class="bi bi-people-fill"></i>
                        {{ peers.size > 0 ? `與 ${collaboratorName} 協作中` : '等待夥伴加入...' }}
                    </span>
                </div>

                <div class="center-tools">
                    <input type="color" v-model="brushColor" class="color-picker" title="顏色" />
                    <input type="range" v-model="brushSize" min="1" max="20" class="size-slider" title="筆刷大小" />
                    <button class="btn-tool" @click="brushColor = '#ffffff'" title="橡皮擦">
                        <i class="bi bi-eraser-fill"></i>
                    </button>
                </div>

                <div class="right-tools">
                    <button class="btn-tool text-danger" @click="clearCanvas" title="清空">
                        <i class="bi bi-trash"></i>
                    </button>
                    <!-- 計時器顯示 -->
                    <div class="timer-display" :class="{ 'urgent': timeLeft <= 5 }">
                        <i class="bi bi-stopwatch"></i> {{ formattedTime }}
                    </div>
                    <!-- 原本的發布按鈕隱藏，改由時間到後彈窗觸發 -->
                </div>
            </div>

            <!-- 畫布區域 -->
            <div class="canvas-container">
                <canvas ref="canvasRef" @mousedown="startDrawing" @mousemove="draw" @mouseup="stopDrawing"
                    @mouseleave="stopDrawing" @touchstart="startDrawing" @touchmove="draw"
                    @touchend="stopDrawing"></canvas>
            </div>
        </div>
    </div>

    <!-- 自定義確認框 (Teleport 到 body 以免被 overflow 裁切) -->
    <Teleport to="body">
        <Transition name="fade">
            <div v-if="confirmDialog.show" class="modal-overlay confirm-z-index">
                <div class="modal-content">
                    <h3>{{ confirmDialog.title }}</h3>
                    <p>{{ confirmDialog.message }}</p>
                    <div class="modal-actions">
                        <button class="btn-cancel" @click="confirmDialog.show = false">取消</button>
                        <button class="btn-confirm" @click="handleDialogConfirm">確定</button>
                    </div>
                </div>
            </div>
        </Transition>
    </Teleport>

    <!-- 結算預覽視窗 -->
    <Teleport to="body">
        <Transition name="fade">
            <div v-if="showResultModal" class="modal-overlay">
                <div class="modal-content result-modal">
                    <h3>⏰ 時間到！</h3>
                    <p>創作結束，請預覽你們的共同作品</p>

                    <div class="result-preview">
                        <img :src="resultImage" alt="Result Preview" />
                    </div>

                    <div class="modal-actions">
                        <button class="btn-cancel" @click="confirmLeave">放棄並離開</button>
                        <button class="btn-confirm" @click="saveAndPost">發布貼文</button>
                    </div>
                </div>
            </div>
        </Transition>
    </Teleport>
</template>

<style scoped>
.draw-view-container {
    min-height: 100vh;
    padding: 20px;
    background: radial-gradient(circle at 10% 20%, rgba(99, 102, 241, 0.05) 0%, #f0f2f5 50%);
    display: flex;
    justify-content: center;
    align-items: center;
}

.main-panel {
    width: 100%;
    max-width: 1000px;
    height: 85vh;
    display: flex;
    flex-direction: column;
    padding: 0;
    overflow: hidden;
    background: white;
    border-radius: 20px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.toolbar {
    padding: 15px 20px;
    background: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
}

.left-tools,
.center-tools,
.right-tools {
    display: flex;
    align-items: center;
    gap: 12px;
}

.room-info {
    font-size: 0.9rem;
    color: #64748b;
    font-weight: 600;
}

.color-picker {
    width: 36px;
    height: 36px;
    border: none;
    border-radius: 50%;
    cursor: pointer;
    overflow: hidden;
    padding: 0;
}

.size-slider {
    width: 100px;
    cursor: pointer;
}

.btn-back {
    height: 36px;
    padding: 0 16px;
    border: 1px solid #e2e8f0;
    background: white;
    border-radius: 18px;
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    color: #64748b;
    font-weight: 600;
    font-size: 0.9rem;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-back:hover {
    background: #eef2ff;
    color: #6366f1;
    border-color: #c7d2fe;
    transform: translateX(-3px);
}

.btn-tool {
    width: 36px;
    height: 36px;
    border: 1px solid #e2e8f0;
    background: white;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #475569;
    transition: all 0.2s;
}

.btn-tool:hover {
    background: #f1f5f9;
    color: #6366f1;
}

.btn-save {
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    color: white;
    border: none;
    padding: 8px 16px;
    border-radius: 8px;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 6px;
    transition: all 0.2s;
}

.btn-save:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.canvas-container {
    flex: 1;
    position: relative;
    background: #e2e8f0;
    /* 外框背景 */
    cursor: crosshair;
    overflow: hidden;
}

canvas {
    display: block;
    width: 100%;
    height: 100%;
    background: white;
}

/* --- Modal 樣式 (與 MessageBoardView 一致) --- */
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(8px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 11000;
}

/* 讓確認框層級高於結算視窗 (避免被遮擋無法點擊) */
.confirm-z-index {
    z-index: 12000 !important;
}

.modal-content {
    background: white;
    padding: 30px;
    border-radius: 24px;
    width: 90%;
    max-width: 400px;
    text-align: center;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    animation: modal-pop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.modal-content h3 {
    margin-top: 0;
    margin-bottom: 15px;
    color: #1e293b;
}

.modal-content p {
    color: #64748b;
    margin-bottom: 25px;
}

.modal-actions {
    display: flex;
    gap: 12px;
}

.modal-actions button {
    flex: 1;
    padding: 12px;
    border-radius: 12px;
    font-weight: 600;
    cursor: pointer;
    border: none;
    font-size: 1rem;
}

.btn-confirm {
    background: #ef4444;
    color: white;
}

.btn-confirm:hover {
    background: #dc2626;
}

.btn-cancel {
    background: #f1f5f9;
    color: #64748b;
}

.btn-cancel:hover {
    background: #e2e8f0;
}

@keyframes modal-pop {
    from {
        transform: scale(0.9);
        opacity: 0;
    }

    to {
        transform: scale(1);
        opacity: 1;
    }
}

.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

/* 計時器樣式 */
.timer-display {
    font-family: 'Courier New', Courier, monospace;
    font-size: 1.2rem;
    font-weight: bold;
    color: #475569;
    background: #e2e8f0;
    padding: 6px 12px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    gap: 6px;
    border: 2px solid transparent;
    transition: all 0.3s;
}

.timer-display.urgent {
    color: #ef4444;
    background: #fee2e2;
    border-color: #ef4444;
    animation: blink 1s infinite;
}

@keyframes blink {
    50% {
        opacity: 0.5;
    }
}

.result-modal {
    max-width: 500px;
}

.result-preview img {
    width: 100%;
    height: auto;
    border-radius: 12px;
    border: 1px solid #e2e8f0;
    margin-bottom: 20px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}
</style>
