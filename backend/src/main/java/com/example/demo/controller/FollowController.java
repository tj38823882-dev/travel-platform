package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.FollowService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final AuthService authService;

    @PostMapping("/{targetId}")
    public ResponseEntity<?> follow(@PathVariable Integer targetId) {
        LoggedInMemberDto me = authService.getLoggedInUser();
        followService.followUser(me.getId(), targetId);
        return ResponseEntity.ok(Map.of("message", "追蹤成功"));
    }

    @DeleteMapping("/{targetId}")
    public ResponseEntity<?> unfollow(@PathVariable Integer targetId) {
        LoggedInMemberDto me = authService.getLoggedInUser();
        followService.unfollowUser(me.getId(), targetId);
        return ResponseEntity.ok(Map.of("message", "已取消追蹤"));
    }
    
    @GetMapping("/check/{targetId}")
    public ResponseEntity<?> checkIsFollowing(@PathVariable Integer targetId) {
        LoggedInMemberDto me = authService.getLoggedInUser();
        boolean isFollowing = followService.isFollowing(me.getId(), targetId);
        return ResponseEntity.ok(Map.of("isFollowing", isFollowing));
    }
}
