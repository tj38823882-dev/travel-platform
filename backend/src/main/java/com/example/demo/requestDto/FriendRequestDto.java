package com.example.demo.requestDto;

import lombok.Data;

@Data
public class FriendRequestDto {
    private Integer friendshipId;// 好友申請id
    private String receiverName;// 接收者名稱

}