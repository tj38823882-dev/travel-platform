<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import PostItem from '../components/PostItem.vue';

const route = useRoute();
const router = useRouter();
const post = ref(null);
const currentUser = ref(null);
const loading = ref(true);
const error = ref('');

// 取得當前使用者 (為了傳給 PostItem 判斷權限/按讚狀態)
const fetchCurrentUser = async () => {
    try {
        const res = await request.post('/api/user/whoami');
        currentUser.value = res.data;
    } catch (err) {
        console.error('無法取得使用者資料', err);
    }
};

// 取得單一貼文
const fetchPost = async () => {
    try {
        loading.value = true;
        const postId = route.params.postId;
        // 呼叫後端 API
        const res = await request.get(`/api/messageboard/post/${postId}`, {
            params: {
                currentUserId: currentUser.value?.id
            }
        });
        post.value = res.data;
    } catch (err) {
        error.value = '找不到該貼文或已被刪除';
    } finally {
        loading.value = false;
    }
};

// 處理按讚 (與主頁面邏輯相同)
const handleLike = async (targetPost) => {
    if (!currentUser.value) return;

    const originalIsLiked = targetPost.isLiked;
    const originalCount = targetPost.likesCount;

    targetPost.isLiked = !targetPost.isLiked;
    targetPost.likesCount += targetPost.isLiked ? 1 : -1;

    try {
        const res = await request.post(`/api/messageboard/like/${targetPost.postId}`, {
            userId: currentUser.value.id
        });
        targetPost.likesCount = res.data.likesCount;
        targetPost.isLiked = res.data.isLiked;
    } catch (err) {
        targetPost.isLiked = originalIsLiked;
        targetPost.likesCount = originalCount;
        alert('操作失敗');
    }
};

// 處理刪除
const handlePostDeleted = () => {
    alert('貼文已刪除');
    router.push('/'); // 刪除後回首頁
};

// 簡單的 Toast 處理 (若需要完整功能可引入 store 或 copy MessageBoardView 的邏輯)
const handleShowToast = (msg) => {
    console.log('Toast:', msg);
};

onMounted(async () => {
    await fetchCurrentUser();
    await fetchPost();
});
</script>

<template>
    <div class="single-post-layout">
        <div v-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>載入中...</p>
        </div>

        <div v-else-if="error" class="error-state">
            <i class="bi bi-exclamation-circle fs-1 mb-3 d-block"></i>
            <p>{{ error }}</p>
            <button @click="router.push('/')" class="btn-back">回首頁</button>
        </div>

        <div v-else-if="post" class="post-container">
            <PostItem :post="post" :user="currentUser || {}" @like="handleLike(post)" @post-deleted="handlePostDeleted"
                @show-toast="handleShowToast" :readonly="false" :default-show-comments="true" />
        </div>
    </div>
</template>

<style scoped>
.single-post-layout {
    min-height: 100vh;
    display: flex;
    justify-content: center;
    padding: 40px 20px;
    background: radial-gradient(circle at 10% 20%, rgba(99, 102, 241, 0.03) 0%, #f0f2f5 50%);
}

.post-container {
    width: 100%;
    max-width: 640px;
}

.loading-state,
.error-state {
    text-align: center;
    margin-top: 100px;
    color: #64748b;
}

.spinner {
    width: 40px;
    height: 40px;
    border: 4px solid rgba(99, 102, 241, 0.1);
    border-top: 4px solid #6366f1;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin: 0 auto 15px;
}

@keyframes spin {
    0% {
        transform: rotate(0deg);
    }

    100% {
        transform: rotate(360deg);
    }
}

.btn-back {
    margin-top: 10px;
    padding: 8px 16px;
    background: #6366f1;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
}
</style>