package com.example.demo.requestDto;

import lombok.Data;

@Data
public class MessageBoardUpdateDto {
    private String content;
    private Integer userId;
}