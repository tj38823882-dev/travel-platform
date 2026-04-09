package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 接收通知的人 (例如：粉絲或好友)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // 觸發通知的人 (例如：發文者)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // 通知類型 (建議使用常數管理，例如: "NEW_POST", "SHARE", "LIKE", "FRIEND_REQUEST")
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    // 關聯的目標 ID (例如 postId, friendshipId)，方便前端點擊後跳轉
    @Column(name = "reference_id")
    private Long referenceId;

    // 附加訊息 (選填，例如預覽文字)
    @Column(name = "message", length = 255)
    private String message;

    // 是否已讀
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isRead == null) {
            this.isRead = false;
        }
    }
}
