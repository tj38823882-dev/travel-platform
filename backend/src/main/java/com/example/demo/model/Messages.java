package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "Messages", indexes = {
        // 對應 SQL: CREATE INDEX IX_Message_History ON Messages (SenderID, ReceiverID,
        // SentAt)
        @Index(name = "IX_Message_History", columnList = "SenderID, ReceiverID, SentAt")
})
public class Messages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MessageID")
    private Long messageID; // BIGINT 對應 Long

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SenderID", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReceiverID", nullable = false)
    private User receiver;

    @Column(name = "Content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "IsRead")
    private Boolean isRead = false;

    @Column(name = "SentAt", updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }

}