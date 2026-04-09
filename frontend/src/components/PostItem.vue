<template>
    <div class="post-item-root">
        <!-- 最外層卡片容器 -->
        <div class="card post" :class="{ 'is-editing-active': props.forceIsEditing }">

            <!-- 貼文頭部 (頭像 + 名字 + 時間) -->
            <div class="post-header">
                <img :src="post.profilePictureUrl" class="avatar" />
                <div class="header-info">

                    <!-- 改成 flex 佈局，讓名字跟追蹤按鈕排在一起 -->
                    <div class="author-row">
                        <div class="author">{{ post.username || '讀取失敗' }}</div>

                        <!-- 如果是我自己，顯示本人標籤 -->
                        <div v-if="post.userId === props.user?.id" class="me-badge">
                            <i class="bi bi-person-fill"></i> 本人
                        </div>

                        <!-- 如果不是我自己，才顯示按鈕 -->
                        <template v-if="post.userId !== props.user?.id && !props.readonly">
                            <div v-if="post.isFriend" class="friend-badge">
                                <i class="bi bi-people-fill"></i> 好友
                            </div>
                            <button v-else class="follow-btn" :class="{ 'following': post.isFollowed }"
                                @click="toggleFollow(post)">
                                {{ post.isFollowed ? '已追蹤' : '＋ 追蹤' }}
                            </button>
                        </template>

                        <div class="header-actions">
                            <!-- 💡 修改：如果是本人 OR 好友，都顯示更多選項 -->
                            <div v-if="(post.userId === props.user?.id || post.isFriend) && !isEditing"
                                class="more-options-container">
                                <button class="menu-trigger" @click="isMenuOpen = !isMenuOpen">
                                    <i class="bi bi-three-dots"></i>
                                </button>

                                <div v-if="isMenuOpen" class="options-menu">
                                    <button class="menu-item" @click="handleEdit">
                                        <i class="bi bi-pencil-square"></i> {{ post.userId === props.user?.id ? '編輯貼文' :
                                            '編輯圖片' }}
                                    </button>
                                    <!-- 只有本人可以使用刪除與更改狀態 -->
                                    <template v-if="post.userId === props.user?.id">
                                        <button class="menu-item" @click="handleChangeStatus">
                                            <i class="bi bi-eye-slash"></i> 更改狀態
                                        </button>
                                        <hr />
                                        <button class="menu-item delete-item" @click="confirmDeletePost">
                                            <i class="bi bi-trash"></i> 刪除貼文
                                        </button>
                                    </template>
                                </div>
                            </div>

                            <button v-if="isSinglePostView" class="close-post-btn" @click="goBackToBoard">
                                <i class="bi bi-x-lg"></i>
                            </button>
                        </div>

                    </div>

                    <div class="time">{{ formatDate(post.createdAt) }}</div>
                </div>
            </div>

            <!-- 貼文內容 (文字 + 圖片) -->
            <div class="post-content">

                <div v-if="isEditing" class="edit-mode">
                    <!-- 💡 修改：若非本人，禁用文字編輯 (:disabled) -->
                    <textarea ref="editTextarea" v-model="editingContent" class="edit-textarea" @input="autoResizeEdit"
                        placeholder="在想什麼嗎？" style="overflow-y: hidden;"
                        :disabled="post.userId !== props.user?.id"></textarea>

                    <!-- 🎨 編輯模式下的圖片預覽與塗鴉按鈕 -->
                    <div class="edit-image-section">
                        <button class="btn-draw" @click="handleDrawing">🎨 {{ (editingPreview || post.imageUrl) ?
                            '在圖片上塗鴉' : '新增塗鴉'
                            }}</button>
                        <!-- 恢復原圖按鈕 -->
                        <button
                            v-if="post.userId === props.user?.id && post.originalImageUrl && post.imageUrl !== post.originalImageUrl"
                            class="btn-revert" @click="revertImage">
                            ↺ 恢復原圖</button>
                    </div>

                    <div class="edit-actions">
                        <button class="btn-cancel" @click="cancelEdit">取消</button>
                        <!-- 💡 修改：允許在「有新圖片」的情況下，即使文字為空也能儲存 -->
                        <button class="btn-save" @click="saveEdit"
                            :disabled="(!editingContent.trim() && !editingFile) || isSaving">
                            {{ isSaving ? '儲存中...' : '儲存' }}
                        </button>
                    </div>
                </div>

                <p v-if="!isEditing">{{ post.content }}</p>

                <!-- 判斷檔案類型 -->
                <div v-if="post.imageUrl || editingPreview" class="image-wrapper">
                    <video
                        v-if="(editingPreview || post.imageUrl).includes('/video/') || (editingPreview || post.imageUrl).endsWith('.mp4')"
                        :src="editingPreview || post.imageUrl" class="post-video" controls>
                    </video>
                    <img v-else :src="editingPreview || post.imageUrl" class="post-image zoom-trigger" alt="點擊放大"
                        :key="editingPreview" @click="isImageZoomed = true" />

                    <!-- 刪除圖片按鈕 (只在編輯模式顯示) -->
                    <button v-if="isEditing && (editingPreview || (post.imageUrl && post.userId === props.user?.id))"
                        class="btn-delete-img" @click.stop="handleDeleteImage" title="刪除圖片">
                        <i class="bi bi-x-lg"></i>
                    </button>
                </div>

                <!-- 💡 顯示被分享的貼文 (引用框) -->
                <div v-if="post.sharedPost" class="shared-post-container" @click="goToPost(post.sharedPost.postId)">
                    <div class="shared-header">
                        <img :src="post.sharedPost.profilePictureUrl" class="shared-avatar" />
                        <div class="shared-info">
                            <div class="shared-author-row">
                                <span class="shared-author">{{ post.sharedPost.username }}</span>
                                <div v-if="post.sharedPost.userId === props.user?.id" class="me-badge shared-badge">
                                    <i class="bi bi-person-fill"></i> 本人
                                </div>
                                <div v-else-if="post.sharedPost.isFriend" class="friend-badge shared-badge">
                                    <i class="bi bi-people-fill"></i> 好友
                                </div>
                                <button v-else-if="!props.readonly" class="follow-btn shared-follow-btn"
                                    :class="{ 'following': post.sharedPost.isFollowed }"
                                    @click.stop="toggleFollow(post.sharedPost)">
                                    {{ post.sharedPost.isFollowed ? '已追蹤' : '＋ 追蹤' }}
                                </button>
                            </div>
                            <span class="shared-time">{{ formatDate(post.sharedPost.createdAt) }}</span>
                        </div>
                    </div>
                    <div class="shared-body">
                        <p>{{ post.sharedPost.content }}</p>
                        <img v-if="post.sharedPost.imageUrl" :src="post.sharedPost.imageUrl" class="shared-image" />
                    </div>
                </div>
            </div>
            <!-- 統計區塊 (讚數、留言數) -->
            <div v-if="!isEditing" class="post-stats">
                <span>👍 {{ post.likesCount || 0 }} </span>
                <span @click="isCommentsVisible = !isCommentsVisible" class="comment-count-btn">
                    {{ (post.comments || []).length }} 則留言
                </span>
            </div>

            <!--按鈕區塊 (讚、留言、分享) -->
            <div v-if="!isEditing && !props.readonly" class="post-actions">

                <!-- 點擊讚，發送 emit 給父層 -->
                <button @click="$emit('like')" :class="{ 'active': post.isLiked }" class="like-button">
                    <div class="icon-wrapper">
                        <i :key="post.isLiked" class="bi"
                            :class="[post.isLiked ? 'bi-heart-fill text-danger heart-animation' : 'bi-heart']"></i>

                        <template v-if="post.isLiked">
                            <span class="particle p1">✨</span>
                            <span class="particle p2">⭐</span>
                            <span class="particle p3">✨</span>
                            <span class="particle p4">⭐</span>
                        </template>
                        <template v-else>
                            <span class="shatter s1"></span>
                            <span class="shatter s2"></span>
                            <span class="shatter s3"></span>
                            <span class="shatter s4"></span>
                        </template>

                    </div>
                    {{ post.likesCount }}
                </button>

                <!-- 點擊留言，切換開關 -->
                <button @click="isCommentsVisible = !isCommentsVisible">
                    💬 {{ isCommentsVisible ? '隱藏留言' : '留言' }}
                </button>

                <div class="share-container">
                    <button class="share-btn" @click="toggleShareMenu">
                        ↪️ 分享
                    </button>
                    <div v-if="isShareMenuOpen" class="share-menu">
                        <button class="menu-item" @click="handleShareToFeed"><i class="bi bi-pencil-square"></i>
                            分享到動態</button>
                        <button class="menu-item" @click="shareToLine"><i class="bi bi-line"></i> 分享到 LINE</button>
                        <button class="menu-item" @click="copyLink"><i class="bi bi-link-45deg"></i> 複製連結</button>
                    </div>
                </div>
            </div>

            <!-- 留言展開區塊 (由 v-if 控制隱藏/顯示) -->
            <div v-if="isCommentsVisible && !isEditing" class="comment-section">
                <TransitionGroup name="smoke" tag="div" class="comment-list">
                    <!-- 跑迴圈顯示每一條留言 -->
                    <div v-for="(comment, index) in post.comments" :key="comment.commentId" class="comment-item">
                        <img :src="comment.profilePictureUrl" class="comment-avatar" />

                        <div class="comment-content-wrapper">
                            <div class="comment-author-row">
                                <div class="comment-meta">
                                    <span class="comment-author">{{ comment.username || '匿名使用者' }}</span>
                                    <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                                </div>
                                <button
                                    v-if="props.user && (comment.userId == props.user.id || post.userId == props.user.id)"
                                    class="delete-comment-btn" @click="deleteComment(comment.commentId, index)"
                                    title="刪除留言">
                                    ✕
                                </button>
                            </div>

                            <div class="comment-bubble">
                                <p>{{ comment.content }}</p>
                            </div>
                        </div>
                    </div>
                </TransitionGroup>
                <!-- 底部輸入框 -->
                <div v-if="!props.readonly" class="comment-input-row">
                    <img :src="props.user?.profilePictureUrl" class="comment-avatar" />

                    <div class="input-wrapper">
                        <input v-model="newCommentText" @keyup.enter="addComment" placeholder="寫下你的留言..." />

                        <button class="send-comment-btn" @click="addComment" :disabled="!newCommentText.trim()">
                            <span class="send-icon">➤</span>
                        </button>
                    </div>
                </div>
            </div>

        </div>
        <Teleport to="body">
            <Transition name="fade">
                <div v-if="isImageZoomed" class="image-lightbox" @click="isImageZoomed = false">
                    <img :src="post.imageUrl" class="zoomed-image" />
                    <button class="close-lightbox" @click="isImageZoomed = false">✕</button>
                    <div class="lightbox-hint">點擊任何地方關閉</div>
                </div>
            </Transition>
            <!-- 🔄 分享貼文的詳細彈窗 (毛玻璃效果) -->
            <Transition name="fade">
                <div v-if="showSharedModal" class="modal-overlay-glass" @click.self="closeSharedModal">
                    <div class="modal-content-glass">
                        <div v-if="isModalLoading" class="loading-spinner">
                            <div class="spinner"></div>
                        </div>
                        <PostItem v-else-if="modalPost" :post="modalPost" :user="user" :readonly="false"
                            @like="handleModalLike" @post-deleted="closeSharedModal" @share="(p) => $emit('share', p)"
                            @request-drawing="(d) => $emit('request-drawing', d)"
                            @show-toast="(m, t) => $emit('show-toast', m, t)"
                            @request-confirm="(d) => $emit('request-confirm', d)"
                            @follow-updated="(d) => $emit('follow-updated', d)" @edit-start="$emit('edit-start')"
                            @edit-end="$emit('edit-end')" />
                    </div>
                </div>
            </Transition>
        </Teleport>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import PostItem from './PostItem.vue'; // 🔄 自我引用，用於在彈窗中顯示貼文

onMounted(() => {
    // console.log('子組件收到的使用者資料:', props.user);
    // console.log(`貼文 ${props.post.postId} 的原始留言內容:`, props.post.comments);
    // 點擊選單以外的地方自動關閉
    window.addEventListener('click', (e) => {
        if (!e.target.closest('.more-options-container')) {
            isMenuOpen.value = false;
        }
        if (!e.target.closest('.share-container')) {
            isShareMenuOpen.value = false;
        }
    });
    if (props.readonly) {
        isCommentsVisible.value = true;
    }

});

const route = useRoute();
const router = useRouter();
const isSinglePostView = computed(() => route.name === 'SinglePost');
const goBackToBoard = () => {
    router.push('/MessageBoard');
};

// --- 彈窗相關邏輯 ---
const showSharedModal = ref(false);
const modalPost = ref(null);
const isModalLoading = ref(false);

const openSharedPostModal = async (postId) => {
    showSharedModal.value = true;
    isModalLoading.value = true;
    try {
        const res = await request.get(`/api/messageboard/post/${postId}`, {
            params: { currentUserId: props.user?.id }
        });
        modalPost.value = res.data;
    } catch (err) {
        emit('show-toast', '無法載入貼文或貼文已刪除', 'error');
        showSharedModal.value = false;
    } finally {
        isModalLoading.value = false;
    }
};

const closeSharedModal = () => {
    showSharedModal.value = false;
    modalPost.value = null;
};

// 修改原本的跳轉邏輯，改為開啟彈窗
const goToPost = (postId) => {
    // router.push(`/MessageBoard/post/${postId}`);
    openSharedPostModal(postId);
};

// 處理彈窗內貼文的按讚 (因為是獨立的物件，需自己處理 API)
const handleModalLike = async () => {
    if (!modalPost.value || !props.user) return;
    const p = modalPost.value;
    const originalIsLiked = p.isLiked;
    const originalCount = p.likesCount;

    p.isLiked = !p.isLiked;
    p.likesCount += p.isLiked ? 1 : -1;

    try {
        const res = await request.post(`/api/messageboard/like/${p.postId}`, {
            userId: props.user.id
        });
        p.likesCount = res.data.likesCount;
        p.isLiked = res.data.isLiked;
    } catch (err) {
        p.isLiked = originalIsLiked;
        p.likesCount = originalCount;
        emit('show-toast', '操作失敗', 'error');
    }
};

const props = defineProps({
    post: { type: Object, required: true },
    user: { type: Object, required: true },
    isAnyEditing: { type: Boolean, default: false }, // 是否有其他人正在編輯
    forceIsEditing: { type: Boolean, default: false }, // 父層強制指定的編輯狀態
    readonly: { type: Boolean, default: false }, // 💡 新增：唯讀模式 (預設關閉)
    defaultShowComments: { type: Boolean, default: false } // 預設是否展開留言
});

const emit = defineEmits(['like', 'post-deleted', 'edit-start', 'edit-end', 'show-toast', 'request-confirm', 'follow-updated', 'request-drawing', 'share']);

const isMenuOpen = ref(false);// 控制選單顯示的狀態
const isShareMenuOpen = ref(false); // 分享選單狀態
const isCommentsVisible = ref(props.readonly || props.defaultShowComments);// 預覽模式或設定預設展開留言
const newCommentText = ref('');// 新留言的文字
const isEditing = ref(false); // 是否處於編輯狀態
const editingContent = ref(''); // 暫存編輯中的文字
const showEditBox = computed(() => props.forceIsEditing);// 判斷畫面上是否顯示編輯框
const editTextarea = ref(null); // 編輯模式的 textarea 
const isImageZoomed = ref(false); // 控制圖片是否放大
const editingFile = ref(null); // 🎨 編輯時的新圖片檔案
const editingPreview = ref(null); // 🎨 編輯時的新圖片預覽
const isSaving = ref(false); // ⏳ 儲存中的載入狀態
/**
 * 編輯框的自動長高邏輯
 */
const autoResizeEdit = () => {
    const el = editTextarea.value;
    if (!el) return;

    el.style.height = 'auto'; // 先重設
    // 根據內容撐開，但不超過 400px（避免編輯長文時失控）
    el.style.height = Math.min(el.scrollHeight, 400) + 'px';
};



// 時間格式化工具
const formatDate = (dateString) => {
    if (!dateString) return '剛剛';

    const now = new Date();
    const postDate = new Date(dateString);
    const diffInSeconds = Math.floor((now - postDate) / 1000);

    // 1. 一分鐘內
    if (diffInSeconds < 60) {
        return '剛剛';
    }

    // 2. 一小時內
    const diffInMinutes = Math.floor(diffInSeconds / 60);
    if (diffInMinutes < 60) {
        return `${diffInMinutes} 分鐘前`;
    }

    // 3. 一天（24小時）內
    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) {
        return `${diffInHours} 小時前`;
    }

    // 4. 一週內
    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 7) {
        return `${diffInDays} 天前`;
    }

    // 5. 超過一週，顯示具體日期 (例如 02-24)
    return postDate.toLocaleDateString('zh-TW', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    });
};

/**
 * 開啟編輯模式
 */
const handleEdit = async () => {
    if (props.isAnyEditing) {
        // ❌ alert("請先完成或取消目前的編輯項目");
        emit('show-toast', '請先完成目前的編輯項目', 'error'); // ✅ 改用吐司
        return;
    }
    editingContent.value = props.post.content; // 把原本的內容塞進暫存
    editingFile.value = null; // 重置編輯檔案
    editingPreview.value = null; // 重置預覽
    isEditing.value = true;
    isMenuOpen.value = false; // 關閉下拉選單
    isCommentsVisible.value = false; // 關閉留言區，專注在編輯
    emit('edit-start'); // 通知父層：我開始編輯了

    // 等待 Vue 渲染完 textarea 後，立刻計算高度
    await nextTick();
    autoResizeEdit();
};

/**
 * 取消編輯
 */
const cancelEdit = () => {
    isEditing.value = false;
    editingContent.value = '';
    editingFile.value = null;
    editingPreview.value = null;
    emit('edit-end'); // 通知父層：我結束了
};

/**
 * 儲存編輯結果
 */
const saveEdit = async () => {
    // 1. 檢查是否變成空白貼文 (無文字、無新圖、無舊圖) -> 觸發刪除
    if (!editingContent.value.trim() && !editingFile.value && !props.post.imageUrl) {
        confirmDeletePost();
        return;
    }

    isSaving.value = true; // 開始儲存，鎖定按鈕
    try {
        // 如果有新圖片，使用 FormData 上傳
        if (editingFile.value) {
            const formData = new FormData();
            formData.append('content', editingContent.value);
            formData.append('userId', props.user?.id);
            formData.append('file', editingFile.value);
            const res = await request.post(`/api/messageboard/post/${props.post.postId}?_method=PUT`, formData);
            // 檔案上傳成功後，後端會回傳包含新圖片 URL 的完整貼文資料
            Object.assign(props.post, res.data);
        } else {
            // 僅更新文字時，後端可能回傳 200 OK 或 204 No Content 但沒有 body
            await request.put(`/api/messageboard/post/${props.post.postId}`,
                { content: editingContent.value },
                { params: { userId: props.user?.id } }
            );
            // 手動更新前端顯示的文字內容
            props.post.content = editingContent.value;
        }
        isEditing.value = false;
        emit('edit-end');
        emit('show-toast', '貼文內容已更新');
    } catch (err) {
        emit('show-toast', '更新失敗', 'error');
    } finally {
        isSaving.value = false; // 🔓 結束儲存，解除鎖定
    }
};

// 改變狀態 (暫時先放著)
const handleChangeStatus = () => {
    alert('狀態切換功能開發中...');
    isMenuOpen.value = false;
};

/**
 * 刪除貼文邏輯
 */
const confirmDeletePost = () => {
    isMenuOpen.value = false; // 1. 先關閉選單

    // 2. 透過 emit 請父組件跳出「霸道確認框」
    emit('request-confirm', {
        title: '永久刪除貼文',
        message: '確定要讓這篇貼文消失嗎？所有的留言與讚都會跟著煙消雲散喔！',
        onConfirm: async () => {
            // 這裡放點擊「確定」後真正執行的 API 邏輯
            try {
                const res = await request.delete(`/api/messageboard/post/${props.post.postId}`, {
                    params: { userId: props.user?.id }
                });

                // 成功後通知父組件從列表移除，觸發「動畫」
                emit('post-deleted', props.post.postId);
            } catch (err) {
                console.error('【刪除失敗】:', err);
                // 失敗時，請父組件彈出錯誤吐司
                emit('show-toast', '刪除失敗：請檢查權限或連線', 'error');
            }
        }
    });
};
/**
 * 新增留言並存入資料庫
 */
const addComment = async () => {
    if (!newCommentText.value.trim()) return;
    console.log("當前貼文 ID:", props.post.postId);

    // 準備要發送的 JSON
    const commentDto = {
        content: newCommentText.value,
        userId: props.user?.id, // 確認user物件是id還是userId
        postId: props.post.postId,
    };
    console.log("Jason:", commentDto);
    try {
        // 發送請求到後端 API
        const res = await request.post('/api/comments/post', commentDto);

        // 成功後，初始化本地 comments 陣列（如果不存在）
        if (!props.post.comments) {
            props.post.comments = [];
        }

        // 將後端回傳的完整物件（包含 commentId, createdAt）塞入
        props.post.comments.push(res.data);

        // 清空輸入框
        newCommentText.value = '';
    } catch (err) {
        console.error('儲存留言失敗:', err);
        emit('show-toast', '留言發送失敗', 'error');
    }
};
// 切換追蹤狀態的函式
const toggleFollow = async (targetPost) => {
    if (!props.user || !props.user.id) {
        emit('show-toast', '請先登入', 'error');
        return;
    }

    const targetId = targetPost.userId;
    const originalState = targetPost.isFollowed;
    const newState = !originalState;

    // 1. 樂觀更新 (Optimistic Update) - 先切換 UI 狀態
    targetPost.isFollowed = newState; // 樂觀更新

    try {
        // 2. 根據原本狀態決定呼叫哪個 API
        const url = `/api/user/follow/${targetId}`;
        originalState ? await request.delete(url) : await request.post(url);
        // 通知父組件，讓所有相同作者的貼文一起更新
        emit('follow-updated', { userId: targetId, isFollowed: newState });
        emit('show-toast', originalState ? '已取消追蹤' : '追蹤成功');
    } catch (err) {
        // 3. 失敗時還原狀態
        targetPost.isFollowed = originalState;
        emit('show-toast', '操作失敗，請稍後再試', 'error');
    }
};

/**
 * 恢復成原始圖片
 */
const revertImage = async () => {
    isSaving.value = true;
    try {
        const res = await request.put(`/api/messageboard/post/${props.post.postId}/revert-image`, null, {
            params: { userId: props.user?.id }
        });
        // 更新前端資料
        Object.assign(props.post, res.data);
        emit('show-toast', '圖片已恢復');
    } catch (err) {
        emit('show-toast', '恢復失敗', 'error');
    } finally {
        isSaving.value = false;
    }
};

/**
 * 刪除圖片邏輯
 */
const handleDeleteImage = () => {
    // 如果是剛選取還沒上傳的預覽圖，直接清除即可
    if (editingPreview.value) {
        editingPreview.value = null;
        editingFile.value = null;
        return;
    }

    // 如果是伺服器上的舊圖，需要確認並呼叫 API
    emit('request-confirm', {
        title: '刪除圖片',
        message: '確定要刪除這張圖片嗎？此操作會同時刪除雲端檔案且無法復原。',
        onConfirm: async () => {
            try {
                const res = await request.put(`/api/messageboard/post/${props.post.postId}/delete-image`, null, {
                    params: { userId: props.user?.id }
                });
                // 更新前端資料
                Object.assign(props.post, res.data);
                // 清除預覽狀態
                editingPreview.value = null;
                editingFile.value = null;
                emit('show-toast', '圖片已刪除');
            } catch (err) {
                emit('show-toast', '刪除失敗', 'error');
            }
        }
    });
};

/**
 * 刪除留言
 */
const deleteComment = (commentId, index) => {
    emit('request-confirm', {
        title: '刪除留言',
        message: '確定要讓這則留言灰飛煙滅嗎？',
        onConfirm: async () => {
            try {
                await request.delete(`/api/messageboard/comment/${commentId}`, {
                    params: { userId: props.user?.id }
                });
                props.post.comments.splice(index, 1);
                emit('show-toast', '留言已化作煙霧散去');
            } catch (err) {
                emit('show-toast', '刪除失敗', 'error');
            }
        }
    });
};

/**
 * 🎨 請求塗鴉
 */
const handleDrawing = () => {
    const currentImg = editingPreview.value || props.post.imageUrl;
    // 簡單檢查是否為影片
    if (currentImg && (currentImg.includes('/video/') || currentImg.endsWith('.mp4'))) {
        emit('show-toast', '影片無法進行塗鴉編輯', 'error');
        return;
    }
    emit('request-drawing', {
        imageUrl: currentImg,
        onFinish: (file, url) => {
            editingFile.value = file;
            editingPreview.value = url;
        }
    });
};

/**
 * 分享相關功能
 */
const toggleShareMenu = () => {
    isShareMenuOpen.value = !isShareMenuOpen.value;
};

const handleShareToFeed = () => {
    isShareMenuOpen.value = false;
    emit('share', props.post);
};

const shareToLine = () => {
    const link = `${window.location.origin}/MessageBoard/post/${props.post.postId}`;
    window.open(`https://social-plugins.line.me/lineit/share?url=${encodeURIComponent(link)}`, '_blank');
    isShareMenuOpen.value = false;
};

const copyLink = () => {
    const link = `${window.location.origin}/MessageBoard/post/${props.post.postId}`;
    navigator.clipboard.writeText(link).then(() => emit('show-toast', '連結已複製')).catch(() => emit('show-toast', '複製失敗', 'error'));
    isShareMenuOpen.value = false;
};
</script>

<style scoped>
.post {
    --primary: #6366f1;
    --primary-gradient: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
    --glass-bg: #ffffff;
    --glass-border: rgba(255, 255, 255, 0.4);
    --text-main: #1e293b;
    --text-muted: #64748b;
    --transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    /* 卡片主體樣式 */
    background: var(--glass-bg);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border: 1px solid var(--glass-border);
    border-radius: 24px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.04);
    transition: var(--transition);
}

.post:hover {
    transform: translateY(-4px);
    box-shadow: 0 15px 35px rgba(99, 102, 241, 0.08);
}

/* --- 1. 貼文頭部優化 --- */
/* 1. 確保頭部容器是滿寬的 */
.post-header {
    display: flex;
    align-items: center;
    /* 頭像與文字對齊 */
    width: 100%;
    margin-bottom: 16px;
}

/* 2. 讓文字資訊區塊撐開 */
.header-info {
    flex: 1;
    /* 💡 關鍵：叫它吃掉右邊所有剩餘空間 */
    display: flex;
    flex-direction: column;
    min-width: 0;
    /* 防止長名字撐破佈局 */
}

.avatar {
    width: 48px;
    height: 48px;
    border-radius: 14px;
    /* 與 logo 同款方圓角 */
    margin-right: 12px;
    border: 2px solid white;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

.author {
    font-weight: 700;
    color: var(--text-main);
    font-size: 1.05rem;
}

.time {
    font-size: 0.85rem;
    color: var(--text-muted);
}

/* 追蹤按鈕改為藥丸型設計 */
.follow-btn {
    margin-left: 12px;
    background: rgba(99, 102, 241, 0.1);
    border: none;
    color: var(--primary);
    font-weight: 600;
    font-size: 0.85rem;
    padding: 4px 12px;
    border-radius: 50px;
    transition: var(--transition);
}

.follow-btn:hover {
    background: var(--primary-gradient);
    color: white;
}

.follow-btn.following {
    background: #f1f5f9;
    color: var(--text-muted);
}

/* --- 2. 貼文圖片與內容 --- */
.post-content p {
    color: var(--text-main);
    line-height: 1.6;
    margin-bottom: 16px;
    white-space: pre-wrap;
}

.image-wrapper {
    position: relative;
    display: block;
    width: 100%;
    text-align: center;
    /* 讓圖片置中 */
}

.post-image {
    display: block;
    width: 90%;
    max-width: 420px;
    margin: 0 auto 16px auto;
    object-fit: cover;
    border-radius: 16px;
    border: 1px solid var(--glass-border);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
    transition: var(--transition);
}

/* 💡 為透明圖片加上棋盤格背景 */
.post-image,
.shared-image {
    background-image:
        linear-gradient(45deg, #eee 25%, transparent 25%),
        linear-gradient(-45deg, #eee 25%, transparent 25%),
        linear-gradient(45deg, transparent 75%, #eee 75%),
        linear-gradient(-45deg, transparent 75%, #eee 75%);
    background-size: 20px 20px;
    background-position: 0 0, 0 10px, 10px -10px, -10px 0px;
}

/* 圖片右上角的刪除按鈕 */
.btn-delete-img {
    position: absolute;
    top: 10px;
    right: 6%;
    /* 配合 post-image 的 width: 90% (左右各5%)，稍微內縮一點 */
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.6);
    color: white;
    border: 2px solid white;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
    z-index: 10;
}

.btn-delete-img:hover {
    background: #ef4444;
    transform: scale(1.1);
}

.post-image:hover {
    transform: scale(1.02);
}

/* --- 3. 數據統計與按鈕 --- */
.post-stats {
    display: flex;
    justify-content: space-between;
    color: var(--text-muted);
    font-size: 0.9rem;
    padding: 8px 4px;
    border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.post-actions {
    display: flex;
    gap: 8px;
    border-top: 1px solid rgba(0, 0, 0, 0.05);
    padding-top: 4px;
}

.share-container {
    flex: 1;
    position: relative;
}

.post-actions button {
    flex: 1;
    background: transparent;
    border: none;
    color: var(--text-muted);
    font-weight: 600;
    padding: 10px;
    border-radius: 12px;
    cursor: pointer;
    transition: var(--transition);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
}

.share-btn {
    width: 100%;
}

.post-actions button:hover {
    background: rgba(99, 102, 241, 0.08);
    color: var(--primary);
}

.post-actions button.active {
    color: #f43f5e;
}

/* --- 4. 留言區塊精緻化 --- */
.comment-section {
    margin-top: 8px;
    padding: 12px;
    background: rgba(248, 250, 252, 0.5);
    border-radius: 16px;
    border-top: 1px solid rgba(0, 0, 0, 0.03);
}

.comment-list {
    position: relative;
    display: flex;
    flex-direction: column;
}

.comment-item {
    display: flex;
    margin-bottom: 12px;
    background: transparent;
    /* 確保煙霧效果清晰 */
    transition: all 0.4s;
    /* 讓一般位移也順暢 */
}

.comment-avatar {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    margin-right: 10px;
}

.comment-bubble {
    background: white;
    border: 1px solid rgba(0, 0, 0, 0.05);
    border-radius: 0 16px 16px 16px;
    /* 對話氣泡形狀 */
    padding: 8px 14px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);

    white-space: pre-wrap;
}

.comment-author {
    font-weight: 700;
    font-size: 0.85rem;
    color: var(--text-main);
    margin-bottom: 2px;
}

.comment-bubble p {
    margin: 0;
    font-size: 0.9rem;
    color: #475569;
}

/* --- 5. 留言輸入框優化 --- */
.comment-input-row {
    display: flex;
    align-items: center;
    margin-top: 16px;
    gap: 10px;
}

.input-wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    background: white;
    border: 1.5px solid rgba(0, 0, 0, 0.05);
    border-radius: 12px;
    transition: var(--transition);
}

.input-wrapper:focus-within {
    border-color: var(--primary);
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.input-wrapper input {
    flex: 1;
    background: transparent;
    border: none;
    padding: 10px 14px;
    outline: none;
    font-size: 0.9rem;
}

.send-comment-btn .send-icon {
    color: var(--primary);
    font-size: 1.2rem;
    margin-right: 8px;
}

.send-comment-btn:disabled .send-icon {
    color: #cbd5e1;
}

/* 讓名字與按鈕水平排列並置中對齊 */
.author-row {
    display: flex;
    align-items: center;
    width: 100%;
    /* 撐滿 header-info */
    gap: 12px;
}

.author {
    font-weight: 700;
    color: var(--text-main);
    font-size: 1.05rem;
    white-space: nowrap;
    /* 防止名字換行 */
}

/* 追蹤按鈕保持在名字旁邊 */
.follow-btn {
    flex-shrink: 0;
    /* 防止按鈕被壓縮 */
    background: rgba(99, 102, 241, 0.1);
    border: none;
    color: #6366f1;
    font-weight: 600;
    font-size: 0.75rem;
    padding: 2px 10px;
    border-radius: 50px;
    cursor: pointer;
    transition: all 0.2s ease;
}

.follow-btn:hover {
    background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
    color: white;
    transform: scale(1.05);
}

/* 已追蹤狀態：變為低調的灰色 */
.follow-btn.following {
    background: #f1f5f9;
    color: #94a3b8;
}

.follow-btn.following:hover {
    background: #e2e8f0;
    color: #64748b;
    transform: none;
}

/* 好友標籤樣式 (綠色系) */
.friend-badge {
    margin-left: 12px;
    background: #ecfdf5;
    color: #10b981;
    font-weight: 600;
    font-size: 0.85rem;
    padding: 4px 12px;
    border-radius: 50px;
    display: flex;
    align-items: center;
    gap: 4px;
    border: 1px solid #d1fae5;
}

/* 本人標籤樣式 (藍色系) */
.me-badge {
    margin-left: 12px;
    background: #eff6ff;
    color: #3b82f6;
    font-weight: 600;
    font-size: 0.85rem;
    padding: 4px 12px;
    border-radius: 50px;
    display: flex;
    align-items: center;
    gap: 4px;
    border: 1px solid #dbeafe;
}

.shared-follow-btn {
    margin-left: 0;
    padding: 2px 8px;
    font-size: 0.75rem;
}

/* --- 修正留言發送按鈕的「醜框框」 --- */
.send-comment-btn {
    /* 核心：徹底消除按鈕外觀 */
    background: transparent !important;
    border: none !important;
    outline: none !important;
    box-shadow: none !important;

    padding: 0 12px;
    /* 調整與輸入框邊緣的間距 */
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: var(--transition);
}

/* 確保點擊或選取時不會跳出藍色外框 */
.send-comment-btn:focus,
.send-comment-btn:active {
    outline: none;
    border: none;
}

/* 讓發送圖標更有質感 */
.send-icon {
    color: var(--primary);
    /* 使用首頁的靛藍色 */
    font-size: 1.2rem;
    transition: var(--transition);
    display: inline-block;
}

/* 懸浮效果：圖標稍微放大並變亮 */
.send-comment-btn:hover:not(:disabled) .send-icon {
    transform: scale(1.2) translateX(2px);
    /* 往右上方微動，更有「發送」感 */
    filter: drop-shadow(0 0 5px rgba(99, 102, 241, 0.4));
}

/* 點擊時的縮放反饋 */
.send-comment-btn:active:not(:disabled) {
    transform: scale(0.9);
}

/* 禁用狀態：變灰且不反應 */
.send-comment-btn:disabled {
    cursor: not-allowed;
}

.send-comment-btn:disabled .send-icon {
    color: #cbd5e1;
    /* 灰掉的顏色 */
    transform: none;
    filter: none;
}

/* 讓影片預覽與圖片預覽外觀一致 */
.post-video,
.post-preview-img {
    display: block;
    width: 100%;
    max-width: 420px;
    /* 保持與圖片一致的最大寬度 */
    border-radius: 12px;
    background: #000;
    /* 影片載入前顯示黑色背景 */
    margin: 10px 0;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

/* 確保影片在手機上也能正常顯示 */
@media (max-width: 480px) {
    .post-video {
        max-width: 100%;
    }
}

.comment-content-wrapper {
    flex: 1;
}

.comment-author-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2px;
}

.delete-comment-btn {
    background: transparent;
    border: none;
    color: #cbd5e1;
    /* 初始淡淡的灰色 */
    cursor: pointer;
    font-size: 0.8rem;
    padding: 2px 6px;
    transition: all 0.2s;
}

.delete-comment-btn:hover {
    color: #f43f5e;
    /* 懸浮變紅色 */
    transform: scale(1.2);
}

/* 4. 標頭動作區塊 (包含選單與關閉按鈕) */
.header-actions {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 4px;
}

.close-post-btn {
    background: transparent;
    border: none;
    color: #94a3b8;
    cursor: pointer;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    font-size: 1.2rem;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.close-post-btn:hover {
    background: #fee2e2;
    color: #ef4444;
    transform: rotate(90deg);
}

.more-options-container {
    position: relative;
    display: flex;
}

/* 三個點按鈕樣式優化 */
.menu-trigger {
    background: transparent;
    border: none;
    color: #94a3b8;
    cursor: pointer;
    padding: 8px;
    font-size: 1.2rem;
    border-radius: 50%;
    line-height: 1;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
}

.menu-trigger:hover {
    background: #f1f5f9;
    color: var(--primary);
}

/* 漂浮選單主體 (確保選單不會被切掉) */
.options-menu {
    position: absolute;
    right: 0;
    /* 靠容器右邊對齊 */
    top: 100%;
    /* 在觸發按鈕正下方 */
    margin-top: 8px;
    background: white;
    border: 1px solid rgba(0, 0, 0, 0.08);
    border-radius: 14px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
    z-index: 1000;
    /* 提高層級，確保蓋過所有東西 */
    width: 160px;
    overflow: hidden;
    padding: 6px;
    transform-origin: top right;
    animation: menuFadeIn 0.2s ease-out;
    /* 加上微動畫質感更好 */
}

@keyframes menuFadeIn {
    from {
        opacity: 0;
        transform: scale(0.95);
    }

    to {
        opacity: 1;
        transform: scale(1);
    }
}

/* 分享選單樣式 */
.share-menu {
    position: absolute;
    bottom: 100%;
    right: 0;
    background: white;
    border: 1px solid rgba(0, 0, 0, 0.08);
    border-radius: 14px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
    z-index: 1000;
    width: 160px;
    padding: 6px;
    animation: menuFadeIn 0.2s ease-out;
    margin-bottom: 8px;
}

/* 選單項目 */
.menu-item {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    padding: 10px 12px;
    border: none;
    background: transparent;
    color: #334155;
    font-size: 0.9rem;
    cursor: pointer;
    border-radius: 8px;
    transition: background 0.2s;
}

.menu-item:hover {
    background: #f8fafc;
    color: var(--primary);
}

.menu-item i {
    font-size: 1.1rem;
}

.menu-item.delete-item {
    color: #ef4444;
}

.menu-item.delete-item:hover {
    background: #fef2f2;
}

.options-menu hr {
    margin: 6px 0;
    border: none;
    border-top: 1px solid #f1f5f9;
}

/* --- 被分享貼文的樣式 (引用框) --- */
.shared-post-container {
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 12px;
    margin-top: 12px;
    background: #ffffff;
    cursor: pointer;
    transition: background-color 0.2s ease, border-color 0.2s ease;
}

.shared-post-container:hover {
    background-color: #eff6ff;
    /* 淡藍色背景 */
    border-color: #bfdbfe;
}

.shared-header {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
}

.shared-avatar {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    margin-right: 8px;
}

.shared-info {
    display: flex;
    flex-direction: column;
}

.shared-author {
    font-weight: 700;
    font-size: 0.9rem;
    color: var(--text-main);
}

.shared-author-row {
    display: flex;
    align-items: center;
    gap: 6px;
}

.shared-badge {
    margin-left: 0 !important;
    padding: 2px 8px;
    font-size: 0.75rem;
}

.shared-time {
    font-size: 0.75rem;
    color: var(--text-muted);
}

.shared-body p {
    font-size: 0.95rem;
    margin-bottom: 8px;
    color: #334155;
}

.shared-image {
    width: 100%;
    max-height: 200px;
    object-fit: cover;
    border-radius: 8px;
}

.edit-mode {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 16px;
}

.edit-image-section {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 10px;
}

.preview-thumb {
    height: 60px;
    width: auto;
    border-radius: 8px;
    border: 1px solid #e2e8f0;
}

.btn-draw {
    background: #f1f5f9;
    color: #6366f1;
    border: 1px dashed #6366f1;
    padding: 6px 12px;
    border-radius: 8px;
    cursor: pointer;
    font-size: 0.9rem;
    transition: all 0.2s;
}

.btn-revert {
    background: #fefce8;
    color: #ca8a04;
    border: 1px dashed #ca8a04;
    padding: 6px 12px;
    border-radius: 8px;
    cursor: pointer;
    font-size: 0.9rem;
    transition: all 0.2s;
}

.btn-revert:hover {
    background: #fef9c3;
}

.btn-draw:hover {
    background: #e0e7ff;
}

.edit-textarea {
    width: 100%;
    min-height: 100px;
    padding: 12px;
    border: 1.5px solid #e2e8f0;
    border-radius: 12px;
    font-size: 1rem;
    resize: vertical;
    outline: none;
    transition: border-color 0.2s;
}

.edit-textarea:focus {
    border-color: var(--primary);
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.edit-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

.btn-cancel,
.btn-save {
    padding: 6px 16px;
    border-radius: 8px;
    font-size: 0.9rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
}

.btn-cancel {
    background: #f1f5f9;
    color: #64748b;
    border: none;
}

.btn-save {
    background: var(--primary-gradient);
    color: white;
    border: none;
}

.btn-save:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

/* 當這篇貼文正在編輯時，拉到防護罩上方 */
.post.is-editing-active {
    position: relative;
    /* 💡 確保 z-index 生效 */
    z-index: 1000;
    /* 比防護罩的 999 還高 */
    transform: scale(1.02);
    /* 稍微放大，強調正在編輯 */
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
    /* 加深陰影 */
}

/* 1. 定義愛心跳動的動畫 */
@keyframes heart-pop {
    0% {
        transform: scale(1);
    }

    50% {
        transform: scale(1.4);
    }

    /* 💡 放大到 1.4 倍 */
    100% {
        transform: scale(1);
    }
}

/* 2. 當按鈕被點擊（active）時觸發動畫 */
.heart-animation {
    display: inline-block;
    /* 💡 必須是 inline-block 縮放才有效 */
    animation: heart-pop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

/* 3. 讓按讚按鈕更有質感 */
.post-actions button i {
    transition: color 0.2s ease;
}

.text-danger {
    color: #f43f5e !important;
    /* 漂亮的亮紅色 */
}

/* 1. 按鈕基本容器 */
.like-button {
    position: relative;
    /* 讓星星可以相對於按鈕定位 */
    overflow: visible;
    /* 💡 確保星星噴出去時不會被切掉 */
}

.icon-wrapper {
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
}

/* 2. 星星粒子的基礎樣式 */
.particle {
    position: absolute;
    pointer-events: none;
    /* 點不到，不影響功能 */
    font-size: 14px;
    opacity: 0;
    top: 50%;
    left: 50%;
}

/* 3. 定義噴發動畫 */
@keyframes particle-burst {
    0% {
        transform: translate(-50%, -50%) scale(0.5);
        opacity: 1;
    }

    100% {
        /* 💡 讓每顆星星飛往不同方向 (--dx, --dy 是自定義變數) */
        transform: translate(var(--dx), var(--dy)) scale(1.2) rotate(20deg);
        opacity: 0;
    }
}

/* 4. 分別設定四顆星星的飛行路線 */
.p1 {
    --dx: -30px;
    --dy: -35px;
    animation: particle-burst 0.6s ease-out forwards;
}

.p2 {
    --dx: 30px;
    --dy: -35px;
    animation: particle-burst 0.6s ease-out 0.1s forwards;
}

.p3 {
    --dx: -40px;
    --dy: 10px;
    animation: particle-burst 0.6s ease-out 0.05s forwards;
}

.p4 {
    --dx: 40px;
    --dy: 10px;
    animation: particle-burst 0.6s ease-out 0.15s forwards;
}

/* 原本的愛心跳動保持不變 */
.heart-animation {
    display: inline-block;
    animation: heart-pop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

/* 1. 碎片的基礎樣式 (灰色的細長條) */
.shatter {
    position: absolute;
    width: 2px;
    height: 8px;
    background: #94a3b8;
    /* 灰藍色，像灰塵或碎片 */
    border-radius: 2px;
    opacity: 0;
    top: 50%;
    left: 50%;
    pointer-events: none;
}

/* 2. 定義碎裂炸開動畫 */
@keyframes shatter-burst {
    0% {
        transform: translate(-50%, -50%) rotate(var(--angle)) translateY(0);
        opacity: 1;
        height: 8px;
    }

    100% {
        transform: translate(-50%, -50%) rotate(var(--angle)) translateY(20px);
        opacity: 0;
        height: 2px;
        /* 💡 飛出去的過程中變短，增加速度感 */
    }
}

/* 3. 設定四個碎片的飛行角度 */
.s1 {
    --angle: 45deg;
    animation: shatter-burst 0.4s ease-out forwards;
}

.s2 {
    --angle: 135deg;
    animation: shatter-burst 0.4s ease-out forwards;
}

.s3 {
    --angle: 225deg;
    animation: shatter-burst 0.4s ease-out forwards;
}

.s4 {
    --angle: 315deg;
    animation: shatter-burst 0.4s ease-out forwards;
}

/* 4. 取消按讚時的愛心抖動效果 (模擬心碎一下) */
@keyframes heart-shake {
    0% {
        transform: translateX(0);
    }

    25% {
        transform: translateX(-3px) rotate(-5deg);
    }

    50% {
        transform: translateX(3px) rotate(5deg);
    }

    75% {
        transform: translateX(-3px) rotate(-5deg);
    }

    100% {
        transform: translateX(0);
    }
}

.unheart-animation {
    display: inline-block;
    animation: heart-shake 0.3s ease-in-out;
}

/* 煙霧動畫 (Transition) */

/* 1. 留言進入時的動畫 (簡單淡入) */
.smoke-enter-active {
    transition: all 0.4s ease-out;
}

.smoke-enter-from {
    opacity: 0;
    transform: translateY(10px);
}

/* 2. 💡 關鍵：留言刪除時的「煙霧散開」動畫 */
.smoke-leave-active {
    transition: all 0.6s cubic-bezier(0.5, 0, 0.7, 0.4);
    position: absolute;
    /* 讓消失中的元素不影響其他留言排版 */
    width: 100%;
}

.smoke-leave-to {
    opacity: 0;
    filter: blur(10px);
    /* 💡 加上模糊，更有煙霧感 */
    transform: translateY(-30px) scale(1.1);
    /* 向上飄並稍微放大 */
}

/* 3. 當其他留言因為有人被刪除而「移動」時的平滑過度 */
.smoke-move {
    transition: transform 0.4s ease;
}

/* 讓鼠標變成放大鏡 */
.zoom-trigger {
    cursor: zoom-in;
}

/* 燈箱背景 */
.image-lightbox {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.9);
    backdrop-filter: blur(10px);
    /* 霸道的模糊感 */
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 99999;
    /* 絕對要在最上層 */
    cursor: zoom-out;
}

/* 放大後的圖片 */
.zoomed-image {
    max-width: 90%;
    max-height: 90%;
    object-fit: contain;
    border-radius: 12px;
    box-shadow: 0 10px 50px rgba(0, 0, 0, 0.5);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 關閉按鈕 */
.close-lightbox {
    position: absolute;
    top: 30px;
    right: 30px;
    background: rgba(255, 255, 255, 0.2);
    border: none;
    color: white;
    font-size: 2rem;
    width: 50px;
    height: 50px;
    border-radius: 50%;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
}

.close-lightbox:hover {
    background: rgba(255, 255, 255, 0.4);
    transform: rotate(90deg);
}

.lightbox-hint {
    position: absolute;
    bottom: 30px;
    color: rgba(255, 255, 255, 0.5);
    letter-spacing: 2px;
}

/* 淡入淡出動畫 */
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

.comment-time {
    font-size: 0.75rem;
    color: #94a3b8;
    /* 淡淡的灰藍色 */
    margin-left: 8px;
    font-weight: 400;
}

/* 讓作者與時間那一列稍微對齊 */
.comment-author-row div {
    display: flex;
    align-items: baseline;
}

/* 確保名字和時間水平排列，並稍微空開一點距離 */
.comment-meta {
    display: flex;
    align-items: baseline;
    /* 讓文字底部對齊，看起來更整齊 */
    gap: 8px;
    /* 名字與時間的間距 */
}

.comment-time {
    font-size: 0.75rem;
    color: #94a3b8;
    /* 淡淡的灰色 */
    font-weight: 400;
}

/* 確保這一列依然是兩端對齊（左邊是名字時間，右邊是刪除按鈕） */
.comment-author-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2px;
}

/* --- 彈窗樣式 --- */
.modal-overlay-glass {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.4);
    backdrop-filter: blur(8px);
    /* 毛玻璃效果 */
    z-index: 10000;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 20px;
}

.modal-content-glass {
    width: 100%;
    max-width: 640px;
    max-height: 90vh;
    overflow-y: auto;
}

.loading-spinner {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 200px;
}

.spinner {
    width: 40px;
    height: 40px;
    border: 4px solid rgba(255, 255, 255, 0.3);
    border-top: 4px solid #6366f1;
    border-radius: 50%;
    animation: spin 1s linear infinite;
}

@keyframes spin {
    0% {
        transform: rotate(0deg);
    }

    100% {
        transform: rotate(360deg);
    }
}
</style>