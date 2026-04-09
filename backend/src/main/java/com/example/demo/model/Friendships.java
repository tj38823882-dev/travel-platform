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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Friendships", indexes = {
		@Index(name = "IX_Friendship_Users", columnList = "RequesterID, ReceiverID")
})
public class Friendships {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "FriendshipID")
	private Integer friendshipId;

	// 對應 RequesterID，指向 Users 表
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "RequesterID", nullable = false)
	private User requester;

	// 對應 ReceiverID，同樣指向 Users 表
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ReceiverID", nullable = false)
	private User receiver;

	// 狀態：0: 等待中, 1: 已接受, 2: 拒絕/封鎖
	@Column(name = "Status", nullable = false)
	private Integer status = 0;

	// 更新時間，對應 DEFAULT GETDATE()
	@Column(name = "UpdatedAt", nullable = false)
	private LocalDateTime updatedAt;

	// 生命周期鉤子：在新增或更新時自動填入當前時間
	@PrePersist
	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}