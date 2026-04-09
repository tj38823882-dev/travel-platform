package com.example.demo.requestDto;

import lombok.Data;

@Data
public class CommentCreateDto {
    private String content;
    private Integer userId;
    private Long postId; 
}
