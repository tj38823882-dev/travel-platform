package com.example.demo.responseDto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TopupOrderResponseDto {
    private String id;
    private LocalDateTime date;
    private Integer points;
    private Integer price;
    private String status;
    private String method;
}
