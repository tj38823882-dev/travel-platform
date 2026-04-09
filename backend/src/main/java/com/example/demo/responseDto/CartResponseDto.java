package com.example.demo.responseDto;

import lombok.Data;
import java.util.List;

@Data
public class CartResponseDto {
    private Integer cartId;
    private List<CartItemDto> items;
    private Integer totalPrice;
    private Integer status; // 0=ACTIVE, 1=CHECKED_OUT, 2=ABANDONED

    @Data
    public static class CartItemDto {
        private Integer cartItemId;
        private Integer itineraryId;
        private String title;
        private String description;
        private Integer priceAtAdd; // 加入時的價格
        private Integer currentPrice; // 行程目前的最新價格
        private String coverImage;
    }
}
