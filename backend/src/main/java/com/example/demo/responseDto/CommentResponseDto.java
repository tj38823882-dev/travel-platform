package com.example.demo.responseDto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CommentResponseDto {
    private Long commentId;
    private String content;
    private Integer userId;
    private Long postId; 
    private String username; // 【 新增 】留言者的名字
    private LocalDateTime createdAt;
    private String profilePictureUrl;

}