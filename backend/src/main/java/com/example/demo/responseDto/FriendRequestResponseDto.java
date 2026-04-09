package com.example.demo.responseDto;

import lombok.Data;

@Data
public class FriendRequestResponseDto {
    private Integer friendshipId;
    private String requesterName;
    private String profilePictureUrl;
    // 如果需要顯示是否上線，可加 private Boolean isOnline;
}