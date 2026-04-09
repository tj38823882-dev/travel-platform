package com.example.demo.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.time.format.DateTimeFormatter;

import com.example.demo.model.ChatMessage;
import com.example.demo.model.User;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.requestDto.ChatMessageRequest;
import com.example.demo.requestDto.UserStatusDto;
import com.example.demo.responseDto.ChatMessageResponse;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        String senderUserId = principal.getName();
        Integer senderId = Integer.valueOf(senderUserId);
        User senderUser = userRepository.findById(senderId).orElseThrow();
        User receiverUser = userRepository.findByUsername(request.getReceiver()).orElseThrow();
        ChatMessage entity = new ChatMessage();
        entity.setSender(senderUser);
        entity.setReceiver(receiverUser);
        entity.setContent(request.getContent());
        chatMessageRepository.save(entity);
        ChatMessageResponse response = new ChatMessageResponse();
        response.setSender(senderUser.getUsername());
        response.setReceiver(receiverUser.getUsername());
        response.setContent(request.getContent());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        response.setSendTime(entity.getSendTime().format(formatter));
        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverUser.getUserId()),
                "/queue/messages",
                response);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(senderUser.getUserId()),
                "/queue/messages",
                response);

    }

    // 數字 userId 解析為 username
    private String resolveUsername(String numericUserId) {
        try {
            return userRepository.findByUserId(Integer.valueOf(numericUserId))
                    .map(User::getUsername)
                    .orElse(numericUserId);
        } catch (NumberFormatException e) {
            return numericUserId;
        }
    }

    // 在 ChatController 裡面
    @MessageMapping("/status.typing")
    public void handleTyping(Principal principal, @Payload String status) {
        String username = resolveUsername(principal.getName());
        // 廣播詳細訊息：誰正在 TYPING 或 IDLE
        messagingTemplate.convertAndSend("/topic/public-status", new UserStatusDto(username, status));
    }

}
