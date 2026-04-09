package com.example.demo.controller;

import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserStatusService userStatusService;

    @GetMapping("/me")
    public ResponseEntity<LoggedInMemberDto> getCurrentUser() {
        return ResponseEntity.ok(authService.getLoggedInUser());
    }

    /** 回傳目前所有在線使用者的 username 集合 */
    @GetMapping("/online-status")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        return ResponseEntity.ok(userStatusService.getAllOnlineUsers());
    }
}
