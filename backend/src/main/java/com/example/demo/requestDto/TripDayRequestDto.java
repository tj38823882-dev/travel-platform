package com.example.demo.requestDto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class TripDayRequestDto {
    private Integer id;
    // 一個 trip 裡面有多個 tripDay，後端不知道你送來的這個 tripDay 是要更新哪一筆，所以每個 tripDay 物件裡要帶自己的
    // id，後端才能比對「這個 id 對應到 tripDay DB 裡面的 哪一筆」然後去更新它。
    private Integer dayNumber;
    private LocalDate theDate;
    private String title;
    private LocalTime startTime;
    private List<StopRequestDto> stops;
}
