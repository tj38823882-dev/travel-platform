package com.example.demo.responseDto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItineraryResponseDto {
    private Integer id;
    private Integer tripId;
    private Integer authorId; // 新增：用於前端過濾自己上架的商品
    private String authorName; // 從 Itinerary.author 取得
    private String title;
    private String description;
    private Integer categoryId;
    private Integer price;
    private Boolean isActive;
    private String coverImage; // 從 Itinerary.trip.coverImage 取得
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String hashTag;
    private List<TripDayResponseDto> tripDays; // 新增：預覽行程天數與站點

}
