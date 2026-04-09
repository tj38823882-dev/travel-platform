package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.responseDto.CartResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;
    private final PurchasedItineraryRepository purchasedItineraryRepository;
    private final PointTransactionRepository pointTransactionRepository;

    /**
     * 獲取用戶的購物車（如果不存在則創建一個新的）
     */
    @Transactional
    public ShoppingCart getOrCreateCart(Integer userId) {
        // 透過 Repository 取得 JPA 管理的 User 實體，確保不是 transient 物件
        User managedUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到使用者：" + userId));

        // 尋找該用戶當前有效的購物車（狀態為 0）
        Optional<ShoppingCart> cartOpt = cartRepository.findByUserAndStatus(managedUser, 0);

        if (cartOpt.isPresent()) {
            return cartOpt.get();
        }

        // 如果沒有找到，創建一個新的購物車
        ShoppingCart newCart = new ShoppingCart();
        newCart.setUser(managedUser);
        newCart.setStatus(0); // 0 代表 Active

        return cartRepository.save(newCart);
    }

    /**
     * 將行程加入購物車
     */
    @Transactional
    public CartItem addItemToCart(Integer userId, Integer itineraryId) {
        // 1. 獲取或創建購物車
        ShoppingCart cart = getOrCreateCart(userId);

        // 2. 檢查購物車中是否已存在該行程
        List<CartItem> items = cart.getItems();
        for (CartItem item : items) {
            if (item.getItinerary().getId().equals(itineraryId)) {
                return item;
            }
        }

        // 3. 獲取行程信息（用於獲取價格）
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found"));

        // 4. 創建新的購物車項目
        CartItem newItem = new CartItem();
        newItem.setCart(cart);
        newItem.setItinerary(itinerary);
        newItem.setPriceAtAdd(itinerary.getPrice()); // 記錄當前價格

        // 5. 保存項目並更新購物車
        cart.getItems().add(newItem);
        cartRepository.save(cart);

        return newItem;
    }

    /**
     * 從購物車中移除項目
     */
    @Transactional
    public void removeItemFromCart(Integer userId, Integer cartItemId) {
        ShoppingCart cart = getOrCreateCart(userId);

        // 找到並移除項目
        cart.getItems().removeIf(item -> item.getId().equals(cartItemId));

        cartRepository.save(cart);
    }

    /**
     * 獲取購物車中的所有項目
     */
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(Integer userId) {
        ShoppingCart cart = getOrCreateCart(userId);
        return cart.getItems();
    }

    /**
     * 結算購物車（驗證點數、扣點、並將狀態變更為 1）
     */
    @Transactional
    public ShoppingCart checkoutCart(Integer userId) {
        ShoppingCart cart = getOrCreateCart(userId);

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("購物車是空的");
        }

        // 1. 計算總金額（以加入時的價格為準）
        int totalCost = cart.getItems().stream()
                .mapToInt(CartItem::getPriceAtAdd)
                .sum();

        // 2. 從資料庫取得最新的 User（確保 points 為最新值）
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到使用者：" + userId));

        // 3. 檢查點數是否足夠
        if (user.getPoints() < totalCost) {
            throw new RuntimeException(
                    "點數不足，無法結帳。需要 " + totalCost + " 點，目前僅有 " + user.getPoints() + " 點");
        }

        // 4. 扣除點數 (使用原子操作 UPDATE)
        int updatedRows = userRepository.deductPoints(userId, totalCost);
        if (updatedRows == 0) {
            // 如果更新筆數為 0，代表在查詢與更新之間點數被扣光了，或是並發扣款
            throw new RuntimeException("扣款失敗：點數餘額可能已被其他交易使用，或點數不足");
        }

        // 5. 紀錄到已購買行程表
        for (CartItem item : cart.getItems()) {
            // 檢查是否已經購買過，避免重複紀錄（雖然購物車應該會擋，但後端 double check 比較安全）
            if (!purchasedItineraryRepository.existsByUserAndItinerary(user, item.getItinerary())) {
                PurchasedItinerary purchased = new PurchasedItinerary();
                purchased.setUser(user);
                purchased.setItinerary(item.getItinerary());
                purchased.setPrice(item.getPriceAtAdd());
                purchasedItineraryRepository.save(purchased);

                // 5.5 紀錄點數扣除紀錄
                PointTransaction pt = new PointTransaction();
                pt.setUser(user);
                pt.setType("spend");
                pt.setPoints(item.getPriceAtAdd());
                pt.setDescription("購買行程: " + item.getItinerary().getTitle());
                pointTransactionRepository.save(pt);
            }
        }

        // 6. 將狀態設為已結算 (1=CHECKED_OUT)
        cart.setStatus(1);
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public CartResponseDto getCartDetails(Integer userId) {
        ShoppingCart cart = getOrCreateCart(userId);

        CartResponseDto dto = new CartResponseDto();
        dto.setCartId(cart.getId());
        dto.setStatus(cart.getStatus());

        List<CartResponseDto.CartItemDto> itemDtos = cart.getItems().stream()
                .map(item -> {
                    CartResponseDto.CartItemDto itemDto = new CartResponseDto.CartItemDto();
                    itemDto.setCartItemId(item.getId());
                    itemDto.setItineraryId(item.getItinerary().getId());
                    itemDto.setTitle(item.getItinerary().getTitle());
                    itemDto.setDescription(item.getItinerary().getDescription());
                    itemDto.setPriceAtAdd(item.getPriceAtAdd());
                    itemDto.setCurrentPrice(item.getItinerary().getPrice());
                    itemDto.setCoverImage(item.getItinerary().getTrip().getCoverImage());
                    return itemDto;
                })
                .toList();

        dto.setItems(itemDtos);

        // 計算總價（使用當前價格）
        int totalPrice = itemDtos.stream()
                .mapToInt(CartResponseDto.CartItemDto::getCurrentPrice)
                .sum();
        dto.setTotalPrice(totalPrice);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<Integer> getPurchasedItineraryIds(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到使用者：" + userId));

        // 直接從專用的購買紀錄表查詢，效能更好且邏輯更清晰
        return purchasedItineraryRepository.findByUser(user).stream()
                .map(p -> p.getItinerary().getId())
                .distinct()
                .toList();
    }

    /**
     * 檢查使用者是否已購買某行程
     */
    @Transactional(readOnly = true)
    public boolean isItineraryPurchased(Integer userId, Integer itineraryId) {
        User user = userRepository.findById(userId).orElse(null);
        Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
        if (user == null || itinerary == null)
            return false;

        return purchasedItineraryRepository.existsByUserAndItinerary(user, itinerary);
    }

    // 扣除點數（已改用 JPQL 原子更新，這個方法可移除或留作參考）
    // @Transactional
    // public void deductPoints(User user, int points) {
    // user.setPoints(user.getPoints() - points);
    // userRepository.save(user);
    // }
}
