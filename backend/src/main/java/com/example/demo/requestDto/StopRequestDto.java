package com.example.demo.requestDto;

import lombok.Data;

@Data
public class StopRequestDto {
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
    private Integer orderIndex;
}
