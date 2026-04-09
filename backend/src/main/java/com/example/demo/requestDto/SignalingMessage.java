package com.example.demo.requestDto;

import lombok.Data;

@Data
public class SignalingMessage {
    private String type;     // "offer", "answer", 或 "candidate"
    private String sender;   // 發送者帳號
    private String receiver; // 接收者帳號
    private Object data;     // SDP 或是 ICE Candidate 的詳細資料
}