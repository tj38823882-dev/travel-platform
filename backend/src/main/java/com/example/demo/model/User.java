package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "Users")
public class User implements UserDetails {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer userId;

	@Column(unique = true, nullable = false, length = 50)
	private String username;
	@Column(name = "PasswordHash")
	private String passwordHash; // 存儲加密後的密碼
	@Column(unique = true, length = 100)
	private String email;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "RoleID", nullable = false)
	private Roles role;
	@Column(name = "isActive")
	private Boolean isActive = true;
	@Column(name = "LastLoginAt")
	private LocalDateTime lastLoginAt;

	@Column(name = "CreatedAt", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "ProfilePictureUrl")
	private String profilePictureUrl;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// 👈 3. 將 Roles 物件轉換為 Spring 的權限物件
		if (this.role == null) {
			return Collections.emptyList();
		}
		return List.of(new SimpleGrantedAuthority(this.role.getRoleName()));
	}

	@Override
	public String getPassword() {
		return this.passwordHash;
	}

	@Override
	public String getUsername() {
		return this.username; // 或 return this.email; 看你的登入策略
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Column(name = "Points")
	private Integer points = 0;

	@JsonIgnore
	@OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
	private List<Itinerary> itineraries;

	@JsonIgnore
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	private List<ShoppingCart> shoppingCarts;
	
@Column(name = "verification_code")
private String verificationCode;

	@Override
	public boolean isEnabled() {
		return Boolean.TRUE.equals(this.isActive);
	}

}
