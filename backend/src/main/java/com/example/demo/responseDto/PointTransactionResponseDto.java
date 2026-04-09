package com.example.demo.responseDto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PointTransactionResponseDto {
    private Integer id;
    private LocalDateTime date;
    private Integer points;
    private Integer price; // Only for topup
    private String type; // topup/spend
    private String status; // Completed (for historical points)
    private String method; // LINE Pay (for topup)
    private String orderId; // Internal ID or LINE Pay transaction ID
    private String description;
}
