<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue';
// 引入子組件
import PostItem from '../components/PostItem.vue';
import FriendList from '../components/FriendList.vue';
import PostCreator from '../components/PostCreator.vue';
import request from '@/utils/request';
import { useToastStore } from '@/stores/toastStore';

// 響應式變數：儲存使用者資料
const user = ref(null);
const loading = ref(true);
const posts = ref([]);
const searchQuery = ref(''); // 搜尋關鍵字
const activeSearchQuery = ref(''); // 實際生效的搜尋關鍵字

const toastStore = useToastStore();

// Canvas 塗鴉相關變數
const isDrawingMode = ref(false);
const canvasRef = ref(null);
const ctx = ref(null);
const isDrawing = ref(false);
const hasDrawn = ref(false); // 🎨 追蹤是否有實際繪製
const drawHistory = ref([]); // 歷史紀錄
const historyStep = ref(-1); // 目前步驟指標
const brushColor = ref('#000000'); // 🎨 畫筆顏色
const brushSize = ref(5); // 🎨 畫筆大小 (預設中)
const drawingCallback = ref(null); // 🎨 編輯貼文時的回呼函數
const sharedPost = ref(null); // 🔄 準備要分享的貼文
const randomSeed = ref(Math.floor(Math.random() * 10000)); // 🎲 隨機種子

// 分頁相關變數
const currentPage = ref(0); // 目前頁碼
const pageSize = 5;         // 每頁抓幾筆
const hasMore = ref(true);  // 是否還有更多貼文
const isFetching = ref(false); // 避免重複觸發載入

// 無限滾動相關
const loadMoreTrigger = ref(null); // 對應 HTML 裡的 ref
let observer = null; // 用來存放觀察者物件
const editingPostId = ref(null); // 儲存目前正在編輯的貼文 ID
// 貼文篩選器
const activeFilter = ref('all'); // 預設為「全部推薦」

const indicatorLeft = computed(() => {
    switch (activeFilter.value) {
        case 'following': return 'calc(25% + 2px)';
        case 'friends': return '50%';
        case 'my': return 'calc(75% - 2px)';
        default: return '4px';
    }
});

const handleGlobalFollowUpdate = ({ userId, isFollowed }) => {
    // 遍歷目前畫面上所有貼文，只要作者 ID 相同，就同步狀態
    posts.value.forEach(p => {
        if (p.userId === userId) {
            p.isFollowed = isFollowed;
        }
        // 同步更新分享貼文中的狀態
        if (p.sharedPost && p.sharedPost.userId === userId) {
            p.sharedPost.isFollowed = isFollowed;
        }
    });
};

/**
 * 切換過濾模式
 */
const switchFilter = (type) => {
    activeFilter.value = type;
    searchQuery.value = ''; // 切換分頁時清空搜尋
    activeSearchQuery.value = ''; // 清空生效的搜尋
    posts.value = [];       // 清空舊貼文
    currentPage.value = 0;   // 重置頁碼
    hasMore.value = true;    // 重置載入狀態
    randomSeed.value = Math.floor(Math.random() * 10000); // 🎲 切換分頁時也重新洗牌
    fetchAllPosts(false);    // 重新抓取資料
};


// 確認框的狀態
const confirmDialog = ref({
    show: false,
    title: '',
    message: '',
    onConfirm: null // 儲存點擊「確定」後要執行的動作
});

/**
 * 顯示吐司訊息的公用函式
 */
const showToast = (msg, type = 'success') => {
    toastStore.addToast(msg, type);
};

/**
 * 顯示自定義確認框
 */
const askConfirm = (title, message, callback) => {
    confirmDialog.value.title = title;
    confirmDialog.value.message = message;
    confirmDialog.value.onConfirm = callback;
    confirmDialog.value.show = true;
};

/**
 * 執行確認動作
 */
const handleDialogConfirm = () => {
    if (confirmDialog.value.onConfirm) {
        confirmDialog.value.onConfirm(); // 執行傳進來的函式
    }
    confirmDialog.value.show = false;
};

/**
 * 從後端 API 取得當前登入使用者的詳細資料
 */
const fetchUserProfile = async () => {
    try {
        loading.value = true;
        const res = await request.post('/api/user/whoami');
        user.value = res.data;
    } catch (err) {
        console.error('獲取個人資料失敗:', err);
    } finally {
        loading.value = false;
    }
};

/**
 * 執行搜尋
 */
const performSearch = () => {
    activeSearchQuery.value = searchQuery.value; // 按下搜尋後才更新生效的關鍵字
    posts.value = [];
    currentPage.value = 0;
    hasMore.value = true;
    fetchAllPosts(false);
};

/**
 * 清除搜尋並回到全部推薦
 */
const clearSearch = () => {
    switchFilter('all'); // switchFilter 內會自動清空 searchQuery 並重新載入
};

/**
 * 從資料庫取得所有貼文
 */
const fetchAllPosts = async (isLoadMore = false) => {
    if (isFetching.value || (!hasMore.value && isLoadMore)) return;

    try {
        isFetching.value = true;

        if (isLoadMore) {
            currentPage.value++;
        } else {
            currentPage.value = 0;
            hasMore.value = true;
        }

        const res = await request.get('/api/messageboard/post', {
            params: {
                currentUserId: user.value?.id,
                page: currentPage.value,
                size: pageSize,
                filter: activeFilter.value || 'all',
                search: activeSearchQuery.value, // 傳送生效的搜尋關鍵字
                seed: randomSeed.value // 🎲 傳送種子
            }
        });

        const newPosts = res.data;

        if (newPosts.length < pageSize) {
            hasMore.value = false;
        }

        if (isLoadMore) {
            posts.value.push(...newPosts);
        } else {
            posts.value = newPosts;
        }
    } catch (err) {
        console.error('取得貼文失敗:', err);
    } finally {
        isFetching.value = false;
        loading.value = false;
    }
};

const handlePostCreated = (newPost) => {
    posts.value.unshift(newPost);
};

/**
 * 處理按讚
 */
const handleLike = async (post) => {
    if (post.likesCount === undefined || post.likesCount === null) {
        post.likesCount = 0;
    }
    const originalIsLiked = post.isLiked;
    const originalCount = post.likesCount;

    post.isLiked = !post.isLiked;
    post.likesCount += post.isLiked ? 1 : -1;

    try {
        const res = await request.post(`/api/messageboard/like/${post.postId}`, {
            userId: user.value.id
        });
        post.likesCount = res.data.likesCount;
        post.isLiked = res.data.isLiked;
    } catch (err) {
        post.isLiked = originalIsLiked;
        post.likesCount = originalCount;
        showToast('操作失敗', 'error');
    }
};

/**
 * 處理貼文刪除後的 UI 更新與吐司顯示 (合併版)
 */
const handlePostDeleted = (deletedPostId) => {
    posts.value = posts.value.filter(p => p.postId !== deletedPostId);
    showToast('貼文已成功吹散成煙霧！', 'success');
};

// 防護罩(?)，提醒使用者先儲存或取消編輯
const warnUnsaved = () => {
    showToast("⚠️ 貼文尚未儲存！請先點擊該貼文的「儲存」或「取消」按鈕。", "error");
};

/**
 * 🎨 更新畫筆設定
 */
const updateCtxStyle = () => {
    if (ctx.value) {
        ctx.value.strokeStyle = brushColor.value;
        ctx.value.lineWidth = brushSize.value;
    }
};

const setBrushSize = (size) => {
    brushSize.value = size;
    updateCtxStyle();
};

/**
 * 開啟塗鴉模式
 */
const openDrawingMode = async (externalImageUrl = null) => {
    let imgObj = null;

    // 重置歷史紀錄
    drawHistory.value = [];
    historyStep.value = -1;

    // 判斷來源：如果是編輯貼文傳來的 URL，或是發新文時選取的檔案
    if (typeof externalImageUrl === 'string' && externalImageUrl) {
        // 🎨 編輯模式：載入外部圖片
        imgObj = new Image();
        imgObj.crossOrigin = "Anonymous"; // 避免跨域汙染畫布
        imgObj.src = externalImageUrl;
        try {
            await new Promise((resolve, reject) => {
                imgObj.onload = resolve;
                imgObj.onerror = reject;
            });
        } catch (e) {
            console.error("無法載入圖片進行塗鴉", e);
            imgObj = null;
        }
    }

    isDrawingMode.value = true;

    await nextTick(); // 等待 DOM 出現
    const canvas = canvasRef.value;
    if (!canvas) return;

    ctx.value = canvas.getContext('2d', { willReadFrequently: true });

    if (imgObj) {
        // --- 圖片模式 ---
        // 計算縮放比例，讓圖片能適應 modal 大小
        const MAX_WIDTH = window.innerWidth * 0.7;
        const MAX_HEIGHT = window.innerHeight * 0.7;
        let w = imgObj.naturalWidth;
        let h = imgObj.naturalHeight;
        const ratio = Math.min(MAX_WIDTH / w, MAX_HEIGHT / h);
        w *= ratio;
        h *= ratio;
        canvas.width = w;
        canvas.height = h;
        // 將圖片畫上去作為背景
        ctx.value.drawImage(imgObj, 0, 0, w, h);
    } else {
        // --- 空白模式 ---
        canvas.width = Math.min(800, window.innerWidth * 0.8);
        canvas.height = Math.min(500, window.innerHeight * 0.7);
        ctx.value.fillStyle = '#ffffff'; // 白底
        ctx.value.fillRect(0, 0, canvas.width, canvas.height);
    }

    ctx.value.strokeStyle = brushColor.value; // 使用設定的顏色
    ctx.value.lineWidth = brushSize.value;    // 使用設定的大小
    ctx.value.lineCap = 'round';
    ctx.value.lineJoin = 'round';

    // 儲存初始狀態 (空白或底圖)
    saveHistoryState();
};

/**
 * 繪圖事件處理
 */
const startDrawing = (e) => {
    isDrawing.value = true;
    hasDrawn.value = false; // 重置繪製狀態
    const { offsetX, offsetY } = e;
    ctx.value.beginPath();
    ctx.value.moveTo(offsetX, offsetY);
};

const draw = (e) => {
    if (!isDrawing.value) return;
    hasDrawn.value = true; // 標記為已繪製
    const { offsetX, offsetY } = e;
    ctx.value.lineTo(offsetX, offsetY);
    ctx.value.stroke();
};

const stopDrawing = () => {
    if (!isDrawing.value) return; // 防止重複觸發
    isDrawing.value = false;
    ctx.value.closePath();
    // 只有真的有畫東西才存檔，避免點一下就多一個無意義的歷史紀錄
    if (hasDrawn.value) {
        saveHistoryState();
    }
};

/**
 * 儲存畫布狀態到歷史紀錄
 */
const saveHistoryState = () => {
    historyStep.value++;
    // 如果在中間步驟畫新圖，刪除後面的紀錄 (Redo失效)
    if (historyStep.value < drawHistory.value.length) {
        drawHistory.value.length = historyStep.value;
    }
    // 儲存影像數據 (ImageData)
    drawHistory.value.push(ctx.value.getImageData(0, 0, canvasRef.value.width, canvasRef.value.height));
};

/**
 * 復原 (Undo)
 */
const undoDrawing = () => {
    if (historyStep.value > 0) {
        historyStep.value--;
        ctx.value.putImageData(drawHistory.value[historyStep.value], 0, 0);
    }
};

/**
 * 完成塗鴉：將 Canvas 轉為檔案
 */
const saveDrawing = () => {
    const canvas = canvasRef.value;
    // 轉為 Blob -> File
    canvas.toBlob((blob) => {
        const file = new File([blob], "doodle.png", { type: "image/png" });
        const url = URL.createObjectURL(file);

        if (drawingCallback.value) {
            // 如果是編輯貼文，執行回呼並傳回檔案
            drawingCallback.value(file, url);
            closeDrawingMode();
        }
    });
};

const closeDrawingMode = () => {
    isDrawingMode.value = false;
    drawingCallback.value = null; // 清除回呼，避免汙染下次操作
};

/**
 * 元件掛載後立即執行資料抓取
 */
onMounted(async () => {
    window.addEventListener('beforeunload', saveScrollPos);
    await fetchUserProfile();

    // 嘗試恢復狀態 (無限捲軸位置復原)
    const savedPos = sessionStorage.getItem('mb_scroll_pos');
    const savedPage = sessionStorage.getItem('mb_current_page');
    const savedFilter = sessionStorage.getItem('mb_filter');
    const savedSearch = sessionStorage.getItem('mb_search');
    const savedSeed = sessionStorage.getItem('mb_seed'); // 讀取種子

    if (savedPos && savedPage) {
        // 1. 恢復篩選條件
        if (savedFilter) activeFilter.value = savedFilter;
        if (savedSearch !== null) {
            activeSearchQuery.value = savedSearch;
            searchQuery.value = savedSearch;
        }

        try {
            isFetching.value = true;
            const targetPage = parseInt(savedPage);
            // 2. 一次抓取直到該頁的所有資料 (例如原本在第 3 頁，就抓 0~3 頁的量)
            // 這樣才能確保捲軸位置對應的內容存在
            const restoreSize = (targetPage + 1) * pageSize;

            const res = await request.get('/api/messageboard/post', {
                params: {
                    currentUserId: user.value?.id,
                    page: 0,
                    size: restoreSize,
                    filter: activeFilter.value || 'all',
                    search: activeSearchQuery.value,
                    seed: randomSeed.value
                }
            });

            posts.value = res.data;
            currentPage.value = targetPage; // 恢復頁碼計數

            if (res.data.length < restoreSize) {
                hasMore.value = false;
            }

            // 3. 恢復捲軸位置
            await nextTick();
            window.scrollTo({ top: parseInt(savedPos), behavior: 'instant' });
        } catch (err) {
            console.error('恢復狀態失敗，改為重新載入', err);
            await fetchAllPosts();
        } finally {
            isFetching.value = false;
            loading.value = false;
            // 清除暫存
            sessionStorage.removeItem('mb_scroll_pos');
            sessionStorage.removeItem('mb_current_page');
            sessionStorage.removeItem('mb_filter');
            sessionStorage.removeItem('mb_search');
            sessionStorage.removeItem('mb_seed');
        }
    } else {
        // 沒有存檔，正常載入
        await fetchAllPosts();
    }

    observer = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && hasMore.value && !isFetching.value) {
            fetchAllPosts(true);
        }
    }, { threshold: 0.5 });

    if (loadMoreTrigger.value) {
        observer.observe(loadMoreTrigger.value);
    }
});

/**
 * 處理子組件請求塗鴉
 */
const handleRequestDrawing = ({ imageUrl, onFinish }) => {
    openDrawingMode(imageUrl);
    drawingCallback.value = onFinish;
};

/**
 * 處理分享貼文事件
 */
const handleShare = (post) => {
    sharedPost.value = post;
};

const cancelShare = () => {
    sharedPost.value = null;
};

const saveScrollPos = () => {
    sessionStorage.setItem('mb_scroll_pos', window.scrollY);
    sessionStorage.setItem('mb_current_page', currentPage.value);
    sessionStorage.setItem('mb_filter', activeFilter.value);
    sessionStorage.setItem('mb_search', activeSearchQuery.value);
};

onUnmounted(() => {
    window.removeEventListener('beforeunload', saveScrollPos);
    if (observer) observer.disconnect();
});
</script>

<template>
    <div class="main-layout">

        <main class="feed-container">
            <!-- 發文框元件 -->
            <!-- 1. 一般發文框 (不綁定 sharedPost) -->
            <PostCreator v-if="user" :user="user" :is-drawing-mode="isDrawingMode" @post-created="handlePostCreated"
                @request-drawing="handleRequestDrawing" @show-toast="showToast" />

            <!-- 2. 分享貼文專用的彈窗發文框 -->
            <Teleport to="body">
                <div v-if="sharedPost" class="share-modal-overlay" @click.self="cancelShare">
                    <div class="share-modal-content">
                        <div class="share-modal-header">
                            <h3>分享貼文</h3>
                            <button class="btn-close-modal" @click="cancelShare">✕</button>
                        </div>
                        <PostCreator v-if="user" :user="user" v-model:sharedPost="sharedPost"
                            :is-drawing-mode="isDrawingMode" @post-created="handlePostCreated"
                            @request-drawing="handleRequestDrawing" @show-toast="showToast" />
                    </div>
                </div>
            </Teleport>

            <!-- 貼文篩選器 -->
            <div v-if="!activeSearchQuery" class="feed-filter-tabs">
                <button class="filter-tab" :class="{ active: activeFilter === 'all' }" @click="switchFilter('all')">
                    <i class="bi bi-globe2"></i> 全部推薦
                </button>
                <button class="filter-tab" :class="{ active: activeFilter === 'following' }"
                    @click="switchFilter('following')">
                    <i class="bi bi-person-check-fill"></i> 追蹤動態
                </button>
                <button class="filter-tab" :class="{ active: activeFilter === 'friends' }"
                    @click="switchFilter('friends')">
                    <i class="bi bi-people-fill"></i> 好友動態
                </button>
                <button class="filter-tab" :class="{ active: activeFilter === 'my' }" @click="switchFilter('my')">
                    <i class="bi bi-person-circle"></i> 我的貼文
                </button>
                <div class="tab-indicator" :style="{ left: indicatorLeft }"></div>
            </div>

            <!-- 搜尋狀態顯示 (有搜尋關鍵字時顯示) -->
            <div v-else class="search-status-bar">
                <div class="search-info">
                    <i class="bi bi-search"></i> 搜尋關鍵字: <strong>{{ activeSearchQuery }}</strong>
                </div>
                <button class="btn-clear-search" @click="clearSearch">
                    <i class="bi bi-x-circle-fill"></i> 取消搜尋
                </button>
            </div>

            <div v-if="posts && posts.length > 0">
                <div v-if="posts && posts.length > 0">
                    <TransitionGroup name="dissolve">
                        <PostItem v-for="post in posts" :key="post.postId" :post="post" :user="user"
                            :is-any-editing="editingPostId !== null && editingPostId !== post.postId"
                            :force-is-editing="editingPostId === post.postId" @edit-start="editingPostId = post.postId"
                            @edit-end="editingPostId = null" @like="handleLike(post)" @post-deleted="handlePostDeleted"
                            @request-drawing="handleRequestDrawing" @show-toast="(msg, type) => showToast(msg, type)"
                            @share="handleShare"
                            @request-confirm="(data) => askConfirm(data.title, data.message, data.onConfirm)"
                            @follow-updated="handleGlobalFollowUpdate" />
                    </TransitionGroup>
                </div>

                <div v-if="posts.length === 0 && !loading" class="empty-msg">
                    目前還沒有任何貼文。
                </div>
            </div>
            <!-- 感應器元件 -->
            <div ref="loadMoreTrigger" class="load-more-trigger">
                <span v-if="isFetching">載入中...</span>
                <span v-else-if="!hasMore && posts.length > 0">已經到底囉！</span>
            </div>
        </main>

        <aside class="sidebar-right">
            <div class="search-card">
                <div class="search-wrapper">
                    <input v-model="searchQuery" type="text" placeholder="搜尋貼文內容..." @keyup.enter="performSearch" />
                    <button class="btn-search-icon" :class="{ 'is-cancel': activeSearchQuery }"
                        @click="activeSearchQuery ? clearSearch() : performSearch()">
                        <i class="bi" :class="activeSearchQuery ? 'bi-x-lg' : 'bi-search'"></i>
                    </button>
                </div>
            </div>
            <!-- 塗鴉牆模式：開啟畫畫邀請，隱藏刪除好友 -->
            <FriendList :allow-draw="true" :allow-delete="false" />
        </aside>
        <div v-if="editingPostId" class="global-edit-overlay" @click="warnUnsaved"></div>
        <Teleport to="body">
            <Transition name="fade">
                <div v-if="confirmDialog.show" class="modal-overlay">
                    <div class="modal-content">
                        <h3>{{ confirmDialog.title }}</h3>
                        <p>{{ confirmDialog.message }}</p>
                        <div class="modal-actions">
                            <button class="btn-cancel" @click="confirmDialog.show = false">取消</button>
                            <button class="btn-confirm" @click="handleDialogConfirm">確定刪除</button>
                        </div>
                    </div>
                </div>
            </Transition>
            <!-- 🎨 Canvas 塗鴉 Modal  -->
            <Transition name="fade">
                <div v-if="isDrawingMode" class="drawing-modal-overlay">
                    <div class="drawing-modal-content animate-modal-pop">
                        <!-- 🎨 畫筆工具列 -->
                        <div class="drawing-toolbar">
                            <div class="tool-group">
                                <label>顏色：</label>
                                <input type="color" v-model="brushColor" @input="updateCtxStyle" class="color-input" />
                            </div>
                            <div class="tool-group">
                                <label>筆刷：</label>
                                <button class="size-btn" :class="{ active: brushSize === 2 }"
                                    @click="setBrushSize(2)">小</button>
                                <button class="size-btn" :class="{ active: brushSize === 5 }"
                                    @click="setBrushSize(5)">中</button>
                                <button class="size-btn" :class="{ active: brushSize === 10 }"
                                    @click="setBrushSize(10)">大</button>
                            </div>
                        </div>
                        <div class="canvas-wrapper">
                            <canvas ref="canvasRef" class="drawing-canvas" @mousedown="startDrawing" @mousemove="draw"
                                @mouseup="stopDrawing" @mouseleave="stopDrawing"></canvas>
                        </div>
                        <div class="canvas-actions">
                            <button class="btn-cancel-draw" @click="isDrawingMode = false">取消</button>
                            <button class="btn-secondary" @click="undoDrawing" :disabled="historyStep <= 0">↩️
                                回復上一筆</button>
                            <button class="btn-confirm-draw" @click="saveDrawing">完成繪圖</button>
                        </div>
                    </div>
                </div>
            </Transition>
        </Teleport>
    </div>
</template>
<style scoped>
/* 1. 外層大容器：背景改為首頁同款的微漸層 */
.main-layout {
    display: flex;
    justify-content: center;
    align-items: flex-start;
    gap: 24px;
    /* 與首頁一致的微漸層背景 */
    background: radial-gradient(circle at 10% 20%, rgba(99, 102, 241, 0.03) 0%, #f0f2f5 50%);
    min-height: 100vh;
    padding: 30px 20px;
    box-sizing: border-box;
}

/* 2. 動態牆容器 */
.feed-container {
    flex: 1;
    max-width: 640px;
    width: 100%;
}

/* 3. 右側好友列表：保持 sticky */
.sidebar-right {
    width: 300px;
    position: sticky;
    top: 90px;
    /* 設定最大高度為視窗高度扣除上下邊距，確保側邊欄固定在視窗內 */
    max-height: calc(100vh - 120px);
    overflow-y: auto;
}

/* 搜尋框樣式 */
.search-card {
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.4);
    border-radius: 16px;
    padding: 15px;
    margin-bottom: 20px;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.search-wrapper {
    display: flex;
    align-items: center;
    /* 垂直置中 */
    background: white;
    border-radius: 10px;
    padding: 6px 8px 6px 16px;
    /* 調整內距讓按鈕更好看 */
    border: 1px solid #e2e8f0;
    transition: all 0.3s;
}

.search-wrapper:focus-within {
    border-color: #6366f1;
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.search-wrapper input {
    border: none;
    outline: none;
    flex: 1;
    font-size: 0.95rem;
    background: transparent;
}

/* 搜尋按鈕美化 */
.btn-search-icon {
    background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
    color: white;
    border: none;
    width: 32px;
    height: 32px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.2s ease;
    box-shadow: 0 2px 6px rgba(99, 102, 241, 0.3);
    margin-left: 8px;
}

.btn-search-icon:hover {
    transform: scale(1.05);
    box-shadow: 0 4px 10px rgba(99, 102, 241, 0.4);
}

.btn-search-icon:active {
    transform: scale(0.95);
}

/* 搜尋按鈕取消狀態 (紅色) */
.btn-search-icon.is-cancel {
    background: linear-gradient(135deg, #ef4444 0%, #f87171 100%);
    box-shadow: 0 2px 6px rgba(239, 68, 68, 0.3);
}

.btn-search-icon.is-cancel:hover {
    box-shadow: 0 4px 10px rgba(239, 68, 68, 0.4);
}

/* 4. 玻璃擬態卡片設計 (與首頁 glass-panel 一致) */
.card {
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.4);
    border-radius: 20px;
    /* 圓角加大 */
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
    margin-bottom: 24px;
    /* 間距拉開更清爽 */
    padding: 20px;
    transition: transform 0.3s ease;
}

.post-creator {
    border: 1px solid rgba(99, 102, 241, 0.2);
    /* 淡淡的紫色邊框強調 */
}

/* 5. 輸入區域 */
.input-row {
    display: flex;
    gap: 15px;
    margin-bottom: 15px;
}

.avatar {
    width: 48px;
    height: 48px;
    border-radius: 16px;
    /* 改為與 logo 一致的方圓角 */
    object-fit: cover;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
    border: 2px solid white;
}

textarea {
    flex: 1;
    border: 1.5px solid rgba(0, 0, 0, 0.05);
    background: rgba(255, 255, 255, 0.5);
    border-radius: 15px;
    padding: 12px 18px;
    resize: none;
    outline: none;
    font-size: 15px;
    height: 60px;
    transition: all 0.3s ease;
}

textarea:focus {
    background: white;
    border-color: #6366f1;
    box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

/* 6. 按鈕樣式：採用首頁漸層色 */
.action-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid rgba(0, 0, 0, 0.05);
    padding-top: 15px;
}

.btn-secondary {
    background: rgba(0, 0, 0, 0.03);
    border: none;
    color: #64748b;
    font-weight: 600;
    cursor: pointer;
    padding: 10px 16px;
    border-radius: 10px;
    transition: all 0.2s;
    font-size: 0.9rem;
}

.btn-secondary:hover {
    background-color: rgba(99, 102, 241, 0.1);
    color: #6366f1;
}

/* 🎨 畫筆工具列樣式 */
.drawing-toolbar {
    display: flex;
    justify-content: center;
    gap: 20px;
    margin-bottom: 10px;
    width: 100%;
}

.tool-group {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    color: #475569;
}

.size-btn {
    padding: 4px 12px;
    border: 1px solid #e2e8f0;
    background: white;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;
    font-size: 0.9rem;
}

.size-btn.active {
    background: #6366f1;
    color: white;
    border-color: #6366f1;
}

.color-input {
    border: none;
    width: 40px;
    height: 30px;
    cursor: pointer;
    background: transparent;
    padding: 0;
}

/* 🎨 Canvas 相關樣式 */
.drawing-canvas {
    background: white;
    border: 2px dashed #cbd5e1;
    border-radius: 8px;
    cursor: crosshair;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
    max-width: 100%;
    /* 響應式 */
}

.btn-confirm-draw {
    background: #10b981;
    color: white;
    border: none;
    padding: 6px 16px;
    border-radius: 8px;
    cursor: pointer;
}

.btn-cancel-draw {
    background: #ef4444;
    color: white;
    border: none;
    padding: 6px 16px;
    border-radius: 8px;
    cursor: pointer;
}

.drawing-hint {
    flex: 1;
    display: flex;
    align-items: center;
    color: #64748b;
    font-weight: 600;
    padding-left: 10px;
}

.btn-secondary:hover {
    background-color: rgba(99, 102, 241, 0.1);
    color: #6366f1;
}

/* 塗鴉 Modal 樣式 */
.drawing-modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.7);
    backdrop-filter: blur(8px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 12000;
    /* 比確認框更高 */
}

.drawing-modal-content {
    background: #f8fafc;
    padding: 20px;
    border-radius: 20px;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
    display: flex;
    flex-direction: column;
    gap: 15px;
}

.animate-modal-pop {
    animation: modal-pop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.canvas-wrapper {
    /* 讓 canvas 容器有自己的背景和邊框 */
    background: white;
    padding: 8px;
    border-radius: 12px;
    box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.1);
    display: flex;
    /* 確保 canvas 置中 */
    align-items: center;
    justify-content: center;
}

.canvas-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding: 10px;
    background: rgba(255, 255, 255, 0.5);
    border-radius: 12px;
    border: 1px solid #e2e8f0;
}

.btn-primary {
    /* 使用首頁登入按鈕的漸層色 */
    background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
    color: white;
    border: none;
    padding: 10px 24px;
    border-radius: 12px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 8px 15px rgba(99, 102, 241, 0.25);
    transition: all 0.3s ease;
}

.btn-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 20px rgba(99, 102, 241, 0.35);
    filter: brightness(1.05);
}

.btn-primary:disabled {
    background: #e2e8f0;
    box-shadow: none;
    color: #94a3b8;
    cursor: not-allowed;
    transform: none;
}

/* 7. 空訊息樣式 */
.empty-msg {
    text-align: center;
    color: #94a3b8;
    margin-top: 40px;
    font-size: 1.1rem;
    font-weight: 500;
}

/* 回應式設計保持 */
@media (max-width: 960px) {
    .sidebar-right {
        display: none;
    }

    .feed-container {
        max-width: 600px;
    }
}

/* 新增預覽圖相關 CSS */
.preview-container {
    position: relative;
    margin: 10px 0;
    padding: 0 40px 0 50px;
    /* 對齊頭像後的間距 */
}

.post-preview-img {
    width: 100%;
    max-height: 300px;
    object-fit: cover;
    border-radius: 8px;
}

.remove-img-btn {
    position: absolute;
    top: 5px;
    right: 45px;
    background: rgba(0, 0, 0, 0.5);
    color: white;
    border: none;
    border-radius: 50%;
    width: 25px;
    height: 25px;
    cursor: pointer;
}

/* 分享預覽樣式 */
.shared-preview-container {
    margin: 10px 20px;
    padding: 12px;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.6);
    position: relative;
}

.shared-preview-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
}

.shared-preview-avatar {
    width: 32px;
    height: 32px;
    border-radius: 8px;
}

.shared-preview-info {
    display: flex;
    flex-direction: column;
    line-height: 1.2;
}

.remove-shared-btn {
    margin-left: auto;
    border: none;
    background: transparent;
    color: #94a3b8;
    cursor: pointer;
}

.load-more-trigger {
    height: 50px;
    display: flex;
    justify-content: center;
    align-items: center;
    color: var(--text-muted);
    font-size: 0.9rem;
    margin-top: 10px;
}

/* 霸道防護罩樣式 */
.global-edit-overlay {
    position: fixed;
    /* 固定定位，覆蓋全螢幕 */
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.4);
    /* 💡 稍微暗一點，讓背景變模糊的感覺 */
    backdrop-filter: blur(4px);
    /* 加上毛玻璃效果，更有質感 */
    z-index: 999;
    /* 確保這層比所有東西都高 */
    cursor: not-allowed;
    /* 游標變成禁止符號 */
}

/* 確認框遮罩 */
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(8px);
    /* 💡 背景模糊，非常有質感 */
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 11000;
}

/* 對話盒主體 */
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

.modal-actions {
    display: flex;
    gap: 12px;
    margin-top: 24px;
}

.modal-actions button {
    flex: 1;
    padding: 12px;
    border-radius: 12px;
    font-weight: 600;
    cursor: pointer;
    border: none;
}

.btn-confirm {
    background: #ef4444;
    color: white;
}

.btn-cancel {
    background: #f1f5f9;
    color: #64748b;
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

/* 分享彈窗樣式 */
.share-modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(5px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 11000;
    /* 介於確認框與塗鴉框之間 */
}

.share-modal-content {
    width: 90%;
    max-width: 600px;
    background: transparent;
    /* 讓 PostCreator 的卡片樣式主導 */
    animation: modal-pop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.share-modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    padding: 0 10px;
}

.share-modal-header h3 {
    color: white;
    margin: 0;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.btn-close-modal {
    background: rgba(255, 255, 255, 0.2);
    border: none;
    color: white;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    cursor: pointer;
    transition: all 0.2s;
}

.btn-close-modal:hover {
    background: rgba(255, 255, 255, 0.4);
    transform: rotate(90deg);
}

/* 1. 定義瓦解動畫 (Dissolve) */
.dissolve-leave-active {
    transition: all 0.7s cubic-bezier(0.4, 0, 0.2, 1);
    position: absolute;
    /* 💡 確保消失時不卡住其他貼文 */
    width: 100%;
    max-width: 640px;
}

.dissolve-leave-to {
    opacity: 0;
    transform: scale(0.8) rotateX(15deg) translateY(-20px);
    /* 向後倒並縮小 */
    filter: blur(15px) brightness(1.5);
    /* 💡 產生發光瓦解的感覺 */
}

/* 2. 平滑移動其他貼文 */
.dissolve-move {
    transition: transform 0.6s ease;
}

/* 讓 TransitionGroup 運作順暢的基礎設定 */
.feed-container>div {
    position: relative;
    /* 關鍵：讓 absolute 的子元素能正確對齊 */
}

/* --- FB 式過濾標籤樣式 --- */
.feed-filter-tabs {
    display: flex;
    position: relative;
    background: rgba(255, 255, 255, 0.5);
    backdrop-filter: blur(8px);
    border-radius: 16px;
    padding: 4px;
    margin-bottom: 24px;
    border: 1px solid rgba(255, 255, 255, 0.4);
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
}

.filter-tab {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px;
    border: none;
    background: transparent;
    font-weight: 600;
    color: #64748b;
    cursor: pointer;
    z-index: 2;
    transition: color 0.3s;
}

.filter-tab.active {
    color: #6366f1;
}

.filter-tab i {
    font-size: 1.1rem;
}

/* 滑動指示器 */
.tab-indicator {
    position: absolute;
    top: 4px;
    bottom: 4px;
    width: calc((100% - 8px) / 4);
    background: white;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    z-index: 1;
}

/* 搜尋狀態列樣式 */
.search-status-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(12px);
    border-radius: 16px;
    padding: 15px 20px;
    margin-bottom: 24px;
    border: 1px solid rgba(99, 102, 241, 0.3);
    box-shadow: 0 4px 15px rgba(99, 102, 241, 0.1);
    animation: fadeIn 0.3s ease;
}

.search-info {
    font-size: 1.05rem;
    color: #475569;
}

.search-info strong {
    color: #6366f1;
    margin-left: 4px;
}

.btn-clear-search {
    background: #fee2e2;
    color: #ef4444;
    border: none;
    padding: 8px 16px;
    border-radius: 50px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    gap: 6px;
}

.btn-clear-search:hover {
    background: #fecaca;
    transform: translateY(-1px);
}
</style>