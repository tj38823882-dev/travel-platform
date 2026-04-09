package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Follow;
import com.example.demo.model.User;
import com.example.demo.repository.FollowRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public void followUser(Integer followerId, Integer targetId) {
        if (followerId.equals(targetId)) {
            throw new RuntimeException("不能追蹤自己");
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("找不到使用者"));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("找不到目標使用者"));

        if (followRepository.existsByFollowerAndFollowing(follower, target)) {
            throw new RuntimeException("已經追蹤過了");
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(target);
        followRepository.save(follow);
    }

    @Transactional
    public void unfollowUser(Integer followerId, Integer targetId) {
        User follower = userRepository.getReferenceById(followerId);
        User target = userRepository.getReferenceById(targetId);

        Follow follow = followRepository.findByFollowerAndFollowing(follower, target)
                .orElseThrow(() -> new RuntimeException("尚未追蹤此使用者"));

        followRepository.delete(follow);
    }
    
    // 檢查我是否追蹤了某人 (用於前端顯示按鈕狀態)
    public boolean isFollowing(Integer myId, Integer targetId) {
        User me = userRepository.getReferenceById(myId);
        User target = userRepository.getReferenceById(targetId);
        return followRepository.existsByFollowerAndFollowing(me, target);
    }
}
