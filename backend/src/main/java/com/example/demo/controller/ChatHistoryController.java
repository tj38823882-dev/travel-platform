package com.example.demo.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.ChatMessage;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.responseDto.ChatMessageResponse;

import com.example.demo.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatMessageRepository chatMessageRepository;
    private final AuthService authService;

    @GetMapping("/history/{friendUsername}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(@PathVariable String friendUsername) {
        String myUsername = authService.getLoggedInUser().getUsername();
        List<ChatMessage> historyEntities = chatMessageRepository.findChatHistory(myUsername, friendUsername);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<ChatMessageResponse> responseList = historyEntities.stream().map(entity -> {
            ChatMessageResponse dto = new ChatMessageResponse();
            dto.setSender(entity.getSender().getUsername());
            dto.setReceiver(entity.getReceiver().getUsername());
            dto.setContent(entity.getContent());
            dto.setSendTime(entity.getSendTime().format(formatter));
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

}
