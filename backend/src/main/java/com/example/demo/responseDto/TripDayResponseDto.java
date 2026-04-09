package com.example.demo.responseDto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TripDayResponseDto {
    private Integer id;
    private Integer dayNumber;
    private LocalDate theDate;
    private String title;
    private LocalTime startTime;
    private LocalDateTime createdAt;
    private String status;
    private List<StopResponseDto> stops;
}
