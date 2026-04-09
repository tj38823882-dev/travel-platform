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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class UserLikes {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "LikeID")
	private Integer likeID;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "LikerID", nullable = false)
	private User liker;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "LikedUserID", nullable = false)
	private User likedUser;

	@Column(name = "CreatedAt", updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

}