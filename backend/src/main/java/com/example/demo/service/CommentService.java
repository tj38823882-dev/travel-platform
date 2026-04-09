package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.mapper.CommentMapper;
import com.example.demo.model.Comment;
import com.example.demo.model.MessageBoard;
import com.example.demo.model.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.MessageBoardRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.requestDto.CommentCreateDto;
import com.example.demo.responseDto.CommentResponseDto;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class CommentService {
    
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final MessageBoardRepository messageBoardRepository;
    private final NotificationService notificationService;
    @Autowired
    private CommentMapper commentMapper;   

    /**
     * 去 Repository 拿 Entity List
     * 透過 Mapper 轉成 DTO List
     */
    public List<CommentResponseDto> getPosts(CommentCreateDto dto) {
        List<Comment> entities = commentRepository.findByMessageBoard_PostId(dto.getPostId());
        return commentMapper.toDtoList(entities);
    }

    public CommentResponseDto createComment(CommentCreateDto dto) {  
        // 1.檢查內文是否為空
        if (Strings.isBlank(dto.getContent())) {
            throw new RuntimeException("請輸入內容");
        }

        // 2.dto -> entity
        Comment comment = commentMapper.toEntity(dto); 

        // 3. 處理使用者 (User)
        // 因為 DTO 給的是 userId (Long)，去資料庫查出對應的 User Entity
        // 如果沒查到，就拋出例外
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("找不到使用者 ID: " + dto.getUserId()));
        
        comment.setUser(user); // 存 userId

        MessageBoard messageBoard = messageBoardRepository.findById(dto.getPostId())
                .orElseThrow(() -> new RuntimeException("找不到文章 ID: " + dto.getPostId()));

        comment.setMessageBoard(messageBoard); // 存 postId



        // 4. 處理時間
        LocalDateTime now = LocalDateTime.now();
        comment.setCreatedAt(now); 


        // 6. 存入資料庫
        Comment savedComment = commentRepository.save(comment);
        
        // 發送留言通知
        notificationService.sendCommentNotification(user, messageBoard, savedComment.getContent());

        // 7. 回傳轉換後的 DTO
        return commentMapper.toDto(savedComment);
    }

}