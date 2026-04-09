package com.example.demo.requestDto;

import lombok.Data;

@Data
public class MessageBoardCreateDto {
    private String content;
    private Integer userId;
    private Integer postId; 
    private String imageUrl; 
}

