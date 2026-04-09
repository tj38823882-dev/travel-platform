<script setup>
import { ref } from 'vue';
import request from '@/utils/request';

const props = defineProps({
    user: { type: Object, required: true },
    sharedPost: { type: Object, default: null },
    isDrawingMode: { type: Boolean, default: false }
});

const emit = defineEmits(['post-created', 'update:sharedPost', 'request-drawing', 'show-toast']);

const newPostText = ref('');
const selectedFile = ref(null);
const imagePreview = ref(null);
const fileInput = ref(null);
const postTextarea = ref(null);

const autoResize = () => {
    const el = postTextarea.value;
    if (!el) return;
    el.style.height = '60px';
    const newHeight = Math.min(el.scrollHeight, 300);
    el.style.height = newHeight + 'px';
};

const onFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
        selectedFile.value = file;
        imagePreview.value = URL.createObjectURL(file);
    }
};

const removeImage = () => {
    selectedFile.value = null;
    imagePreview.value = null;
    if (fileInput.value) fileInput.value.value = '';
};

const clearSharedPost = () => {
    emit('update:sharedPost', null);
};

const openDrawingMode = () => {
    // 如果目前已經有選圖片，就傳給塗鴉板當背景
    const currentImg = (selectedFile.value && selectedFile.value.type.startsWith('image/')) ? imagePreview.value : null;

    emit('request-drawing', {
        imageUrl: currentImg,
        onFinish: (file, url) => {
            selectedFile.value = file;
            imagePreview.value = url;
        }
    });
};

const createPost = async () => {
    const hasContent = newPostText.value.trim().length > 0;
    const hasImage = !!selectedFile.value;
    const hasSharedPost = !!props.sharedPost;

    if (!hasContent && !hasImage && !hasSharedPost) {
        emit('show-toast', '請輸入文字內容或選擇一張圖片！', 'error');
        return;
    }

    const formData = new FormData();
    formData.append('content', newPostText.value || "");
    formData.append('userId', props.user.id);

    if (hasImage) {
        formData.append('file', selectedFile.value);
    }

    if (props.sharedPost) {
        formData.append('sharedPostId', props.sharedPost.postId);
    }

    try {
        const res = await request.post('/api/messageboard/post', formData);
        // 通知父層有新貼文
        emit('post-created', res.data);

        // 重置表單
        newPostText.value = '';
        removeImage();
        clearSharedPost();
        if (postTextarea.value) {
            postTextarea.value.style.height = '60px';
        }
        emit('show-toast', '貼文發布成功！');
    } catch (err) {
        console.error('發文失敗:', err);
        emit('show-toast', '發文失敗，請檢查網路或伺服器', 'error');
    }
};
</script>

<template>
    <div class="card post-creator">
        <div class="input-row">
            <img :src="user.profilePictureUrl" class="avatar" />
            <textarea v-if="!isDrawingMode" ref="postTextarea" v-model="newPostText" @input="autoResize"
                placeholder="在想什麼嗎？" style="overflow-y: hidden;"></textarea>
            <!-- 塗鴉時顯示提示文字 -->
            <div v-else class="drawing-hint">正在繪製塗鴉...</div>
        </div>

        <!-- 一般圖片/影片預覽區 -->
        <div v-if="imagePreview" class="preview-container">
            <video v-if="selectedFile?.type?.startsWith('video/')" :src="imagePreview" class="post-preview-img"
                controls>
            </video>

            <img v-else :src="imagePreview" class="post-preview-img" />

            <button class="remove-img-btn" @click="removeImage">✕</button>
        </div>

        <!-- 🔄 分享貼文預覽區 -->
        <div v-if="sharedPost" class="shared-preview-container">
            <div class="shared-preview-header">
                <img :src="sharedPost.profilePictureUrl" class="shared-preview-avatar" />
                <div class="shared-preview-info">
                    <span class="fw-bold">{{ sharedPost.username }}</span>
                    <span class="text-muted small">{{ new Date(sharedPost.createdAt).toLocaleDateString()
                        }}</span>
                </div>
            </div>
            <div class="shared-preview-body">
                <p v-if="sharedPost.content" class="text-truncate">{{ sharedPost.content }}</p>
                <img v-if="sharedPost.imageUrl" :src="sharedPost.imageUrl" class="shared-preview-image" />
            </div>
        </div>

        <div class="action-row">
            <input type="file" ref="fileInput" @change="onFileChange" style="display: none" accept="image/*" />
            <button v-if="!sharedPost" class="btn-secondary" @click="fileInput.click()">📷 相片/影片</button>
            <button v-if="!sharedPost" class="btn-secondary" @click="openDrawingMode">
                {{ selectedFile && selectedFile.type.startsWith('image/') ? '🎨 在圖片上塗鴉' : '🎨 空白塗鴉' }}
            </button>
            <button class="btn-primary" @click="createPost" :disabled="!newPostText && !selectedFile && !sharedPost">
                {{ (!newPostText && sharedPost) ? '立刻發佈' : '發佈' }}
            </button>
        </div>
    </div>
</template>

<style scoped>
.card {
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.4);
    border-radius: 20px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
    margin-bottom: 24px;
    padding: 20px;
    transition: transform 0.3s ease;
}

.post-creator {
    border: 1px solid rgba(99, 102, 241, 0.2);
}

.input-row {
    display: flex;
    gap: 15px;
    margin-bottom: 15px;
}

.avatar {
    width: 48px;
    height: 48px;
    border-radius: 16px;
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

.action-row {
    display: flex;
    justify-content: flex-start;
    gap: 10px;
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

.btn-primary {
    background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
    color: white;
    border: none;
    padding: 10px 24px;
    border-radius: 12px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 8px 15px rgba(99, 102, 241, 0.25);
    transition: all 0.3s ease;
    margin-left: auto;
    min-width: 120px;
    display: flex;
    justify-content: center;
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

.preview-container {
    position: relative;
    margin: 10px 0;
    padding: 0 40px 0 50px;
}

.post-preview-img {
    width: 100%;
    max-height: 300px;
    object-fit: cover;
    border-radius: 8px;
}

/* 為透明圖片加上棋盤格背景 */
.post-preview-img,
.shared-preview-image {
    background-image:
        linear-gradient(45deg, #eee 25%, transparent 25%),
        linear-gradient(-45deg, #eee 25%, transparent 25%),
        linear-gradient(45deg, transparent 75%, #eee 75%),
        linear-gradient(-45deg, transparent 75%, #eee 75%);
    background-size: 20px 20px;
    background-position: 0 0, 0 10px, 10px -10px, -10px 0px;
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

.shared-preview-body {
    margin-top: 4px;
}

.shared-preview-image {
    width: 100%;
    max-height: 150px;
    object-fit: cover;
    border-radius: 12px;
    margin-top: 8px;
}

.drawing-hint {
    flex: 1;
    display: flex;
    align-items: center;
    color: #64748b;
    font-weight: 600;
    padding-left: 10px;
}
</style>