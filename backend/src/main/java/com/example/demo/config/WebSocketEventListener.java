package com.example.demo.config;

import com.example.demo.requestDto.UserStatusDto;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final UserStatusService userStatusService;
    private final UserRepository userRepository;

    // 數字 userId -> username 的輔助方法
    private String resolveUsername(String numericUserId) {
        try {
            return userRepository.findByUserId(Integer.valueOf(numericUserId))
                    .map(u -> u.getUsername())
                    .orElse(numericUserId); // fallback 避免 null
        } catch (NumberFormatException e) {
            return numericUserId;
        }
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String numericId = sha.getUser().getName();
        String username = resolveUsername(numericId);
        userStatusService.add(username); // 改存 username
        messagingTemplate.convertAndSend("/topic/public-status", new UserStatusDto(username, "ONLINE"));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String numericId = sha.getUser().getName();
        String username = resolveUsername(numericId);
        userStatusService.remove(username); // 改存 username
        messagingTemplate.convertAndSend("/topic/public-status", new UserStatusDto(username, "OFFLINE"));
    }
}