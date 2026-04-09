package com.example.demo.requestDto;

import lombok.Data;

@Data
public class FriendDto {
    private Integer friendshipId; // 資料庫流水號，刪除時使用
    private String friendName;

}