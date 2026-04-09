// src/router/permission.js
import { useAuthStore } from '@/stores/authStore';

export function setupRouterGuard(router) {
    router.beforeEach(async (to, from, next) => { // Added 'async' here
        const authStore = useAuthStore();

        // 判斷是否登入 (利用 Store 裡面的 isAuthenticated getter)
        let isAuthenticated = authStore.isAuthenticated;
        // 判斷是否為管理員 (利用 Store 裡面的 isAdmin getter)
        let isAdmin = authStore.isAdmin;

        // 修復：「重新整理時被踢回登入頁」的問題
        // 因為 F5 重新整理時，Pinia 的狀態會被清空 (isAuthenticated = false)
        // 而 App.vue 發送的 whoami 請求是非同步的，還來不及回來，Router Guard 就先執行了！
        // 解法：如果還沒確定登入狀態，且剛重新整理載入了網頁，我們可以試著先同步等待一次 whoami
        if (!authStore.isInitialized) {
            try {
                // 等待 whoami 檢查完成，確保拿到了最新的 Cookie 狀態
                await authStore.fetchUserInfo();
            } catch (error) {
                // 檢查失敗代表真的沒登入或 Cookie 失效
                // console.error("Failed to fetch user info on initial load:", error);
            }
            // 標記為已初始化，避免每次切換路由都去打 API
            authStore.isInitialized = true;
        }

        // 再次獲取最新的狀態 (因為剛剛可能 fetchUserInfo 更新了 Store)
        isAuthenticated = authStore.isAuthenticated;
        isAdmin = authStore.isAdmin;

        // 1. 檢查是否需要登入 (requiresAuth)
        if (to.meta.requiresAuth && !isAuthenticated) {
            // alert('請先登入以繼續訪問！'); // Removed alert as per instruction's implied change
            next('/loginView'); // 重導向到登入頁
            return;
        }
        // 2. 檢查是否需要管理員權限 (requiresAdmin)
        if (to.meta.requiresAdmin && !isAdmin) {
            // alert('權限不足！此區域僅限管理員進入。'); // Removed alert as per instruction's implied change
            // 可以導回首頁，或是導回使用者後台
            next('/');
            return;
        }

        // 3. 通行
        next();
    });
}