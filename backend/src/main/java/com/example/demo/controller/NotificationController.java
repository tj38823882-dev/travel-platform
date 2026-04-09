package com.example.demo.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Notification;
import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.responseDto.NotificationResponseDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    // 1. 獲取我的通知列表
    @GetMapping
    public ResponseEntity<Page<NotificationResponseDto>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        LoggedInMemberDto user = authService.getLoggedInUser();
        Page<Notification> notificationPage = notificationService.getUserNotifications(user.getId(), page, size);
        
        // 使用 Page 的 map 功能直接轉換 DTO，更優雅
        Page<NotificationResponseDto> dtoPage = notificationPage.map(this::toDto);
        
        return ResponseEntity.ok(dtoPage);
    }

    // 2. 獲取未讀數量 (用於顯示鈴鐺紅點)
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        LoggedInMemberDto user = authService.getLoggedInUser();
        Long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    // 3. 標記單則已讀
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    // 4. 全部標為已讀
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        LoggedInMemberDto user = authService.getLoggedInUser();
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok().build();
    }

    // 5. 刪除單則通知
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        LoggedInMemberDto user = authService.getLoggedInUser();
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.ok().build();
    }

    // 6. 清空所有通知
    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications() {
        LoggedInMemberDto user = authService.getLoggedInUser();
        notificationService.deleteAllNotifications(user.getId());
        return ResponseEntity.ok().build();
    }

    // Helper: Entity -> DTO
    private NotificationResponseDto toDto(Notification n) {
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setId(n.getId());
        if (n.getSender() != null) {
            dto.setSenderName(n.getSender().getUsername());
            dto.setSenderProfilePictureUrl(n.getSender().getProfilePictureUrl());
        }
        dto.setType(n.getType());
        dto.setMessage(n.getMessage());
        dto.setReferenceId(n.getReferenceId());
        dto.setIsRead(n.getIsRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}