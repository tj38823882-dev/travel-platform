package com.example.demo.requestDto;

import lombok.Data;

@Data
public class ChatMessageRequest {

    private String receiver;
    private String content;

}