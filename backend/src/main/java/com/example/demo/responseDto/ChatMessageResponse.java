package com.example.demo.responseDto;

import lombok.Data;

@Data
public class ChatMessageResponse {
    private String sender;
    private String receiver;
    private String content;
    private String sendTime;
}