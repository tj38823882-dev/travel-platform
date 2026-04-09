package com.example.demo.requestDto;

import lombok.Data;

@Data
public class ItineraryRequestDto {
    private Integer tripId;
    private String title;
    private String description;
    private Integer price;
}
