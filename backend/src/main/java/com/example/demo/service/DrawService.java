package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.mapper.MessageBoardMapper;
import com.example.demo.model.MessageBoard;
import com.example.demo.model.User;
import com.example.demo.repository.MessageBoardRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.responseDto.MessageBoardResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DrawService {

    private final UserRepository userRepository;
    private final MessageBoardRepository messageBoardRepository;
    private final NotificationService notificationService;
    private final MessageBoardMapper messageBoardMapper;

    /**
     * 建立雙人協作貼文 (Canvas 繪圖結果)
     */
    @Transactional
    public MessageBoardResponseDto createCollaborativePost(Integer userId, Integer collaboratorId, String content, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到發文者"));

        MessageBoard post = new MessageBoard();
        post.setUser(user);
        post.setImageUrl(imageUrl);
        post.setOriginalImageUrl(imageUrl); // 繪圖貼文通常預設原圖即為此圖
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setLikesCount(0);

        if (collaboratorId != null) {
            User collaborator = userRepository.findById(collaboratorId)
                    .orElseThrow(() -> new RuntimeException("找不到協作者"));
            post.setCollaborator(collaborator);

            // 如果沒有提供內容，自動生成包含雙方名字的預設標題
            if (content == null || content.trim().isEmpty()) {
                post.setContent(user.getUsername() + " 與 " + collaborator.getUsername() + " 的共同創作！");
            } else {
                post.setContent(content);
            }
        } else {
            post.setContent(content != null ? content : "與好友的共同創作！");
        }

        MessageBoard savedPost = messageBoardRepository.save(post);
        notificationService.sendNewPostNotification(savedPost); // 通知粉絲與好友

        return messageBoardMapper.toDto(savedPost);
    }
}