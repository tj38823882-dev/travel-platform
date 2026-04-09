package com.example.demo.requestDto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TripRequestDto {
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String coverImage;
    private String hashTag;
    private List<TripDayRequestDto> tripDays;
}
