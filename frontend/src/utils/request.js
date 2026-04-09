import axios from 'axios';
import { apiBaseUrl } from '../config/appConfig.js';

const request = axios.create({
    baseURL: apiBaseUrl,
    timeout: 5000,
    // 🌟 1. 這是全場最重要的設定！
    // 開啟它，瀏覽器才會在每次發送 API 時，自動幫你把 HttpOnly Cookie 帶上
    withCredentials: true
});

// --- 1. 請求攔截器 (大瘦身！) ---
// ❌ 以前要辛苦地從 localStorage 拿 Token 手動塞 Header。
// ✅ 現在通通不用了！瀏覽器會在底層幫我們處理好，所以這個攔截器基本上可以空著，
// 但我們保留結構，以防你以後要在發送請求前加個 Loading 動畫之類的。
request.interceptors.request.use(config => {
    // 這裡不用再寫 config.headers.Authorization 了！
    return config;
}, error => {
    return Promise.reject(error);
});

// --- 2. 回應攔截器 (保留 403 捕捉機制) ---
request.interceptors.response.use(
    (response) => {
        // 如果狀態碼是 2xx，就直接回傳資料
        return response;
    },
    (error) => {
        // 🚨 捕捉 403 (Forbidden) 或 401 (Unauthorized)
        // 當後端發現 Cookie 裡的 Token 過期或無效時，會丟出這個錯誤
        if (error.response && (error.response.status === 403 || error.response.status === 401)) {

            // 🌟 避免無限重導向！
            // 如果當前已經在登入頁，或者這是一個身分檢查的請求 (whoami)，就不要再 refresh 頁面
            const isLoginView = window.location.pathname.includes('/loginView');
            const isWhoamiRequest = error.config.url.includes('/api/user/whoami');

            if (!isLoginView && !isWhoamiRequest) {
                console.log('權放不足或 Token 已過期，執行強制登出');

                // 1. 清除前端的「畫面狀態」
                localStorage.removeItem('user');

                // 2. 執行跳轉回登入頁
                window.location.href = '/loginView?sessionExpired=true';
            }
        }

        return Promise.reject(error);
    }
);

export default request;
