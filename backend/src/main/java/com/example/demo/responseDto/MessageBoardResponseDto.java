package com.example.demo.responseDto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class MessageBoardResponseDto {
    private String content;
    private Integer userId;
    private Integer postId; 
    private String username;
    private LocalDateTime createdAt;
    private Integer likesCount;
    private Boolean isLiked;
    private List<CommentResponseDto> comments;
    private String imageUrl;
    private String originalImageUrl;
    private Boolean isFollowed; 
    private Boolean isFriend;
    private String profilePictureUrl;
    private MessageBoardResponseDto sharedPost;
}