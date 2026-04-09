package com.example.demo.requestDto;

import lombok.Data;

@Data
public class DrawActionDto {
    private String type; // "start", "draw", "end", "clear", "undo", "JOIN", "WELCOME"
    private Double x;    
    private Double y;    
    private String color;
    private Integer size;
    private String roomId;

    // 🛠️ 新增這些欄位以支援 JOIN/WELCOME 訊息
    private Integer userId;
    private String username;
    private Integer senderId;
}
