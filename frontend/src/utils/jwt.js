// src/utils/jwt.js

/**
 * 解析 JWT Token 的 Payload
 * @param {string} token - JWT 字串
 * @returns {object|null} - 解析後的 JSON 物件，失敗回傳 null
 */
export const parseToken = (token) => {
    if (!token) return null;

    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(
            window.atob(base64)
                .split('')
                .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        );
        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error('Token 解析失敗:', e);
        return null;
    }
};