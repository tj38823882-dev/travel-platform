package com.example.demo.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.requestDto.LoginRequestDto;
import com.example.demo.requestDto.RegisterRequestDto;
import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.responseDto.VerifyCodeRequestDto;
import com.example.demo.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserRepository userRepository;
	

	@PostMapping("/public/registerPost")
	public ResponseEntity<String> loginPost(@RequestBody RegisterRequestDto registerRequestDto) {
		if (authService.checkUsernameExist(registerRequestDto)) {
			authService.register(registerRequestDto);
			return ResponseEntity.ok("註冊成功");
		}
		return ResponseEntity.badRequest().body("此帳號已經存在，請重新輸入");
	}

	@PostMapping("/public/login")
	public ResponseEntity<?> loginByJwt(@RequestBody LoginRequestDto loginRequestDto) {
		String token = authService.login(loginRequestDto);

		if (token == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("帳號或密碼錯誤");
		}

		ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", token)
				.httpOnly(true)
				.secure(false)
				.path("/")
				.maxAge(24 * 60 * 60)
				.sameSite("Lax")
				.build();

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
				.body("登入成功");
	}

	@PostMapping("/public/logout")
	public ResponseEntity<?> logout() {
		ResponseCookie deleteCookie = ResponseCookie.from("jwt_token", "")
				.httpOnly(true)
				.path("/")
				.maxAge(0)
				.build();

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
				.body("登出成功");
	}

	@PostMapping("/user/whoami")
	public ResponseEntity<LoggedInMemberDto> whoami() {
		LoggedInMemberDto principal = authService.getLoggedInUser();
		return ResponseEntity.ok(principal);
	}
	@PostMapping("/public/verify-code")
public ResponseEntity<?> verifyCode(@RequestBody VerifyCodeRequestDto request) {
    // 1. 去資料庫找這個 email 的使用者
    User user = userRepository.findByEmail(request.getEmail()).orElse(null);
    
    if (user == null) {
        return ResponseEntity.badRequest().body("找不到此帳號");
    }
    
    // 2. 檢查帳號是不是已經開通了
    if (Boolean.TRUE.equals(user.getIsActive())) {
        return ResponseEntity.badRequest().body("此帳號已經驗證過了，請直接登入");
    }
    
    // 3. 比對驗證碼
    if (request.getCode().equals(user.getVerificationCode())) {
        // 🌟 驗證成功！開通帳號，並把驗證碼清空（防止重複使用）
        user.setIsActive(true);
        user.setVerificationCode(null);
        userRepository.save(user);

        // 直接產生 JWT 並回寫 cookie（讓前端可以直接登入）
        String token = authService.createTokenForUser(user);
        ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body("驗證成功！已登入");
    } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("驗證碼錯誤，請重新確認");
    }
}
}