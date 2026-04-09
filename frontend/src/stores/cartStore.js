import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { useToastStore } from './toastStore';
import request from '@/utils/request';

// --- 購物車 Store ---
export const useCartStore = defineStore('cart', () => {
    // 購物車內的行程清單
    const cartItems = ref([]);
    const totalAmount = ref(0);
    const purchasedItineraryIds = ref([]);

    /**
     * 從後端獲取購物車內容
     */
    const fetchCart = async () => {
        try {
            const response = await request.get('/api/cart');
            cartItems.value = response.data.items.map(item => ({
                id: item.cartItemId, // 這裡是 cart_items 的 ID，用於移除
                itineraryId: item.itineraryId,
                name: item.title,
                description: item.description,
                price: item.currentPrice,
                priceAtAdd: item.priceAtAdd,
                image: item.coverImage || 'https://images.unsplash.com/photo-1543158266-0066955047b1?q=80&w=2070&auto=format&fit=crop',
            }));
            totalAmount.value = response.data.totalPrice;
        } catch (error) {
            console.error('獲取購物車失敗:', error);
        }
    };

    /**
     * 獲取已購買的行程 ID 列表
     */
    const fetchPurchasedIds = async () => {
        try {
            const response = await request.get('/api/cart/purchased-ids');
            purchasedItineraryIds.value = response.data;
        } catch (error) {
            console.error('獲取已購買列表失敗:', error);
        }
    };

    /**
     * 加入行程至購物車
     * @param {Object} tour - 行程物件
     */
    const addToCart = async (tour) => {
        const { addToast } = useToastStore();
        try {
            const itineraryId = tour.originalItineraryId || tour.id;
            await request.post('/api/cart/add', { itineraryId });
            await fetchCart();
            addToast(`已將「${tour.name}」加入購物車`, 'success', '加入成功');
        } catch (error) {
            const errorMsg = error.response?.data?.error || '加入購物車失敗';
            addToast(errorMsg, 'error', '加入失敗');
        }
    };

    /**
     * 從購物車移除指定項目
     * @param {number} cartItemId - 購物車項目 ID
     */
    const removeFromCart = async (cartItemId) => {
        const { addToast } = useToastStore();
        try {
            await request.delete(`/api/cart/remove/${cartItemId}`);
            await fetchCart();
            addToast('已從購物車移除', 'success', '移除成功');
        } catch (error) {
            addToast('移除失敗', 'error', '操作失敗');
        }
    };

    /**
     * 清空購物車
     */
    const clearCart = () => {
        cartItems.value = [];
        totalAmount.value = 0;
        purchasedItineraryIds.value = [];
    };

    /**
     * 結帳處理
     */
    const checkout = async () => {
        const { addToast } = useToastStore();
        try {
            await request.post('/api/cart/checkout');
            addToast('結帳成功！', 'success', '結帳完成');
            await fetchPurchasedIds(); // 結帳後更新已購買清單
            cartItems.value = [];
            totalAmount.value = 0;
            return true;
        } catch (error) {
            const errorMsg = error.response?.data?.error || '結帳失敗';
            addToast(errorMsg, 'error', '結帳失敗');
            throw error;
        }
    };

    /**
     * 計算購物車總金額 (Computed)
     */
    const totalPrice = computed(() => {
        return totalAmount.value;
    });

    return {
        cartItems,
        totalAmount,
        purchasedItineraryIds,
        fetchCart,
        fetchPurchasedIds,
        addToCart,
        removeFromCart,
        clearCart,
        checkout,
        totalPrice
    };
});
