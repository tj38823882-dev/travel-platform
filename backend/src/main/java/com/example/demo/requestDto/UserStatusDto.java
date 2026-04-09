package com.example.demo.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusDto {
    private String userId;
    private String status; // 值為: ONLINE, OFFLINE, TYPING, IDLE
}