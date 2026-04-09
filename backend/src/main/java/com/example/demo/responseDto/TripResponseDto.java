package com.example.demo.responseDto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TripResponseDto {
    private Integer id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String coverImage;
    private String hashTag;
    private LocalDateTime createdAt;
    private String status;
    private List<TripDayResponseDto> tripDays;
}
