package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.model.Roles;
import com.example.demo.model.User;
import com.example.demo.repository.RolesRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.requestDto.LoginRequestDto;
import com.example.demo.requestDto.RegisterRequestDto;
import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.utils.JwtUtil;

import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RolesRepository rolesRepository;
	private final JwtUtil jwtUtil;

	@Transactional
	public User register(RegisterRequestDto registerRequestDto) {
		User user = new User();
		user.setUsername(registerRequestDto.getUsername());
		user.setEmail(registerRequestDto.getEmail());
		user.setPasswordHash(passwordEncoder.encode(registerRequestDto.getPassword()));
		user.setIsActive(true);
		user.setProfilePictureUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + registerRequestDto.getEmail());
		Optional<Roles> byId = rolesRepository.findById(2);
		if (byId.isPresent()) {
			user.setRole(byId.get());
		}

		return userRepository.save(user);
	}

	public boolean checkUsernameExist(RegisterRequestDto registerRequestDto) {
		Optional<User> optional = userRepository.findByUsername(registerRequestDto.getUsername());
		Optional<User> optional2 = userRepository.findByEmail(registerRequestDto.getEmail());
		if (optional.isPresent() || optional2.isPresent()) {
			return false;
		}

		return true;
	}

	public String login(LoginRequestDto loginRequestDto) {
		Optional<User> byUsername = userRepository.findByUsername(loginRequestDto.getUsername());

		if (byUsername.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤");
		}

		if (!passwordEncoder.matches(loginRequestDto.getPassword(), byUsername.get().getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤");
		}
		if (!Boolean.TRUE.equals(byUsername.get().getIsActive())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您的帳號已被封鎖，請聯繫管理員");
		}

		User user = byUsername.get();
		user.setLastLoginAt(LocalDateTime.now());
		userRepository.save(user);

		return jwtUtil.generateToken(byUsername.get().getUserId(), byUsername.get().getRole());
	}

	public String createTokenForUser(User user) {
		// 更新最後登入時間
		user.setLastLoginAt(LocalDateTime.now());
		userRepository.save(user);
		return jwtUtil.generateToken(user.getUserId(), user.getRole());
	}

	public LoggedInMemberDto createLoggedInUserDto(Integer Userid) {
		Optional<User> byUserId = userRepository.findByUserId(Userid);
		if (byUserId.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到該使用者資料");
		}
		LoggedInMemberDto loggedInMemberDto = new LoggedInMemberDto();
		loggedInMemberDto.setId(byUserId.get().getUserId());
		loggedInMemberDto.setRole(byUserId.get().getRole());
		loggedInMemberDto.setEmail(byUserId.get().getEmail());
		loggedInMemberDto.setUsername(byUserId.get().getUsername());
		loggedInMemberDto.setIsActive(byUserId.get().getIsActive());
		loggedInMemberDto.setPoints(byUserId.get().getPoints());
		loggedInMemberDto.setCreatedAt(byUserId.get().getCreatedAt());
		loggedInMemberDto.setProfilePictureUrl(byUserId.get().getProfilePictureUrl());
		return loggedInMemberDto;

	}

	public LoggedInMemberDto createLoggedInUserDto(String Userid) {
		return this.createLoggedInUserDto(Integer.valueOf(Userid));
	}

	// 取得當前登入者
	public LoggedInMemberDto getLoggedInUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入");
		}
		LoggedInMemberDto loggedInMemberDto = (LoggedInMemberDto) authentication.getPrincipal();
		// 重新從資料庫查詢完整資料，確保最新的資料
		return createLoggedInUserDto(loggedInMemberDto.getId());

	}

	public void updateUserStatus(Integer userId, Boolean isActive) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("找不到使用者"));

		user.setIsActive(isActive);
		userRepository.save(user);
	}

	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

}