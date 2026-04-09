package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class NotificationTestController {

        private final NotificationService notificationService;
        private final UserRepository userRepository;

        /**
         * 測試發送通知 API
         * 用法 (Postman):
         * POST
         * http://localhost:8080/api/test/notification?senderId=1&receiverId=2&type=SYSTEM&message=測試訊息
         */
        @PostMapping("/notification")
        public String sendTestNotification(
                        @RequestParam Integer senderId,
                        @RequestParam Integer receiverId,
                        @RequestParam String type,
                        @RequestParam String message,
                        @RequestParam(required = false) Long referenceId) {

                User sender = userRepository.findById(senderId)
                                .orElseThrow(() -> new RuntimeException("找不到發送者 ID: " + senderId));
                User receiver = userRepository.findById(receiverId)
                                .orElseThrow(() -> new RuntimeException("找不到接收者 ID: " + receiverId));

                notificationService.send(sender, receiver, type, referenceId, message);

                return String.format("測試通知已發送！\nFrom: %s (ID: %d)\nTo: %s (ID: %d)\nMessage: %s",
                                sender.getUsername(), senderId, receiver.getUsername(), receiverId, message);
        }
}