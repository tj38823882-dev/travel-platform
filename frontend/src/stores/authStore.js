import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import request from '@/utils/request';

export const useAuthStore = defineStore('auth', () => {
    // --- 💡 狀態 (State) ---
    // 不再存 token，改存登入狀態
    const isLoggedIn = ref(false);
    const userRole = ref('');
    const points = ref(0);
    const username = ref('');
    const profilePictureUrl = ref('');
    const userId = ref('');

    const isInitialized = ref(false); // 🌟 新增：用來記錄是否已經完成初始化的狀態檢查

    // --- 💡 Getters ---
    const isAuthenticated = computed(() => isLoggedIn.value);
    const isAdmin = computed(() => userRole.value === 'ROLE_ADMIN');

    // --- 💡 Actions ---

    /**
     * 核心功能：從後端獲取目前登入者的資訊
     * 只要 Cookie 有效，這個 API 就會成功
     */
    async function fetchUserInfo() {
        try {
            // 這裡呼叫我們剛改好的 whoami API
            const response = await request.post('/api/user/whoami');
            const userData = response.data;

            if (userData) {
                userId.value = userData.userId;
                username.value = userData.username || userData.name; // 根據你的 DTO 欄位調整
                userRole.value = userData.role?.roleName || '';
                points.value = userData.points || 0;
                profilePictureUrl.value = userData.profilePictureUrl || '';
                isLoggedIn.value = true;
            }
        } catch (error) {
            console.error('尚未登入或 Token 已失效');
            clearAuth(); // 失敗就清空前端狀態
        }
    }

    /**
     * 登入後的處理：
     * 傳統登入或 Google 登入後，呼叫此 function 來同步資料
     */
    async function login() {
        await fetchUserInfo(); // 直接去問後端：「我是誰？」
    }

    /**
     * 登出：必須呼叫後端 API 來刪除 HttpOnly Cookie
     */
    async function logout() {
        try {
            await request.post('/api/public/logout');
        } catch (error) {
            console.error('後端登出失敗，但仍清除前端狀態');
        } finally {
            clearAuth();
        }
    }

    /**
     * 清除前端所有的狀態
     */
    function clearAuth() {
        isLoggedIn.value = false;
        userRole.value = '';
        points.value = 0;
        username.value = '';
        profilePictureUrl.value = '';
        userId.value = '';
    }

    function addPoints(amount) {
        points.value += amount;
    }

    // --- 💡 初始化邏輯 ---
    // 網頁重新整理時，Pinia 會執行這個 fetch
    // 如果 Cookie 還在，使用者就不用重新登入

    // 🧹 清除舊架構殘留的冗餘 Token，避免開發者在 F12 看到產生誤會
    localStorage.removeItem('token');
    localStorage.removeItem('loggedInUsername');

    fetchUserInfo();

    return {
        isLoggedIn,
        isInitialized, // 🌟 匯出
        userRole,
        points,
        username,
        userId,
        profilePictureUrl,
        isAuthenticated,
        isAdmin,
        login,
        logout,
        fetchUserInfo,
        fetchCurrentUser: fetchUserInfo, // 🌟 新增別名，確保相容性
        addPoints
    };
});