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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Follows", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"FollowerID", "FollowingID"}) // 防止重複追蹤
})
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FollowID")
    private Integer followId;

    // 誰發起追蹤 (粉絲)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FollowerID", nullable = false)
    private User follower;

    // 被追蹤的人 (偶像)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FollowingID", nullable = false)
    private User following;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
