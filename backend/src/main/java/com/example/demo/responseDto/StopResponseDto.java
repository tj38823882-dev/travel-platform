package com.example.demo.responseDto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StopResponseDto {
    private Integer id;
    private String name;
    private String placeId;
    private Double lat;
    private Double lng;
    private String address;
    private Integer stayMinutes;
    private Integer travelMinutes;
    private String travelMode;
    private String note;
    private LocalDateTime createdAt;
    private Integer orderIndex;
    private String status;
}
