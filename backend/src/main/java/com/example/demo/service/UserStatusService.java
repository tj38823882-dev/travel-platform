package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStatusService {
    // 建立一個執行緒安全的 Set 來存 userId
    private final Set<String> onlineUsers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void add(String userId) {
        onlineUsers.add(userId);
    }

    public void remove(String userId) {
        onlineUsers.remove(userId);
    }

    public Set<String> getAllOnlineUsers() {
        return onlineUsers;
    }
}