package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🌟 關聯發送者 (對應到 User 表)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id") // 資料庫裡會多一個 sender_id 欄位
    private User sender;

    // 🌟 關聯接收者 (對應到 User 表)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id") // 資料庫裡會多一個 receiver_id 欄位
    private User receiver;

    private String content;
    private LocalDateTime sendTime;

    @PrePersist
    protected void onCreate() {
        sendTime = LocalDateTime.now();
    }
}