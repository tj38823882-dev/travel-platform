package com.example.demo.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.mapper.MessageBoardMapper;
import com.example.demo.model.MessageBoard;
import com.example.demo.model.PostLikeKey;
import com.example.demo.repository.PostLikeRepository;
import com.example.demo.requestDto.MessageBoardCreateDto;
import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.responseDto.MessageBoardResponseDto;
import com.example.demo.service.FollowService;
import com.example.demo.service.FriendshipsService;
import com.example.demo.service.DrawService;
import com.example.demo.service.ImageUploadService;
import com.example.demo.service.MessageBoardService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/messageboard")
public class MessageBoardController {

    private final MessageBoardService messageBoardService;
    private final ImageUploadService imageUploadService;
    private final PostLikeRepository postLikeRepository;
    private final FollowService followService;
    private final FriendshipsService friendshipsService;
    private final MessageBoardMapper messageBoardMapper;
    private final DrawService drawService;
    
    MessageBoardController(MessageBoardService messageBoardService,
                            ImageUploadService imageUploadService,
                            PostLikeRepository postLikeRepository,
                            FollowService followService,
                            FriendshipsService friendshipsService,
                            MessageBoardMapper messageBoardMapper,
                            DrawService drawService) {
        this.messageBoardService = messageBoardService;
        this.imageUploadService = imageUploadService;
        this.postLikeRepository = postLikeRepository;
        this.followService = followService;
        this.friendshipsService = friendshipsService;
        this.messageBoardMapper = messageBoardMapper;
        this.drawService = drawService;
    }

    /**
     * 取得所有貼文 (支援分頁)
     */
    @GetMapping("/post")
    public ResponseEntity<List<MessageBoardResponseDto>> getAllPosts(
        @RequestParam(defaultValue = "all") String filter, 
        @RequestParam(required = false) String search, // 1. 新增搜尋參數
        @RequestParam(required = false) Integer currentUserId,
        // 預設為第 0 頁
        @RequestParam(defaultValue = "0") int page,
        // 預設一頁抓 5 筆 
        @RequestParam(defaultValue = "5") int size,
        @RequestParam(defaultValue = "0") int seed // 🎲 新增隨機種子
    ) {
        // 2. 將 search 傳入 Service
        List<MessageBoardResponseDto> posts = messageBoardService.getAllPosts(filter, search, currentUserId, page, size, seed);
        return ResponseEntity.ok(posts);
    }

    /**
     * 取得單一貼文詳情
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<MessageBoardResponseDto> getPostById(
        @PathVariable Long postId,
        @RequestParam(required = false) Integer currentUserId
    ) {
        return ResponseEntity.ok(messageBoardService.getPostById(postId, currentUserId));
    }
    
    /**
     * 新增貼文 (支援 Cloudinary 圖片上傳)
     */
    @PostMapping("/post")
    public ResponseEntity<MessageBoardResponseDto> createPost(
        @RequestParam(value = "content", required = false) String content,
        @RequestParam("userId") Integer userId,
        @RequestParam(value = "file", required = false) MultipartFile file,
        @RequestParam(value = "sharedPostId", required = false) Long sharedPostId
    ) throws IOException {
        
        // 1. 處理圖片上傳
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = imageUploadService.uploadImage(file);
        }

        // 2. 組裝 DTO 
        MessageBoardCreateDto dto = new MessageBoardCreateDto();
        dto.setContent(content);
        dto.setUserId(userId);
        dto.setImageUrl(imageUrl); 

        // 3. 呼叫 Service
        MessageBoardResponseDto response = messageBoardService.createMessageBoard(dto, sharedPostId);
        return ResponseEntity.ok(response);
    }

    /**
     * 新增雙人協作貼文 (Canvas 繪圖結果)
     */
    @PostMapping("/post/collaborative")
    public ResponseEntity<MessageBoardResponseDto> createCollaborativePost(
            @RequestParam(value = "content", required = false) String content,
            @RequestParam("userId") Integer userId,
            @RequestParam("collaboratorId") Integer collaboratorId,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("協作貼文必須包含圖片");
        }

        String imageUrl = imageUploadService.uploadImage(file);

        MessageBoardResponseDto response = drawService.createCollaborativePost(userId, collaboratorId, content,
                imageUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * 貼文按讚
     */
    @PostMapping("/like/{id}")
    public ResponseEntity<?> likePost(@PathVariable("id") Long postId, HttpSession session) {
        // 從 SecurityContextHolder 取得目前登入的使用者資訊
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // 如果 principal 是 null 或 "anonymousUser"，表示使用者未登入
        if (principal == null || principal.equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
            }
        // 強制轉型為 LoggedInMemberDto，這樣我們就可以取得使用者 ID
        LoggedInMemberDto member = (LoggedInMemberDto) principal;
        System.out.println("點讚用戶 ID: " + member.getId());
        Map<String, Object> result = messageBoardService.likePost(postId, member.getId());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 刪除留言
     */
    @DeleteMapping("/comment/{commentId}")
    public ResponseEntity<?> deleteComment(
        @PathVariable Long commentId,
        @RequestParam Integer userId // 傳入 userId 用於校驗權限
    ) {
        // 呼叫 Service 執行刪除
        messageBoardService.deleteComment(commentId, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 刪除貼文
     */
    @DeleteMapping("/post/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId, @RequestParam Integer userId) {
        messageBoardService.deletePost(postId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 更新貼文內容
     */
    @PutMapping("/post/{postId}")
    public ResponseEntity<?> updatePost(
        @PathVariable Long postId,
        @RequestParam(value = "content", required = false) String content,
        @RequestParam("userId") Integer userId,
        @RequestParam(value = "file", required = false) MultipartFile file
    ) throws IOException {
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = imageUploadService.uploadImage(file);
        }

        MessageBoard updatedPost = messageBoardService.updatePost(postId, content, userId, imageUrl);
        MessageBoardResponseDto responseDto = messageBoardMapper.toDto(updatedPost);
        if (updatedPost.getUser() != null) {
            responseDto.setProfilePictureUrl(updatedPost.getUser().getProfilePictureUrl());
        }

        // Re-populate user-specific fields so the frontend can update correctly
        responseDto.setIsLiked(postLikeRepository.existsById(new PostLikeKey(postId, userId)));

        // These might be inefficient, but for a single post update it's acceptable
        if (updatedPost.getUser() != null && !updatedPost.getUser().getUserId().equals(userId)) {
            responseDto.setIsFollowed(followService.isFollowing(userId, updatedPost.getUser().getUserId()));
            responseDto.setIsFriend(friendshipsService.areFriends(userId, updatedPost.getUser().getUserId()));
        }

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 恢復成原始圖片
     */
    @PutMapping("/post/{postId}/revert-image")
    public ResponseEntity<?> revertImage(@PathVariable Long postId, @RequestParam Integer userId) {
        MessageBoard updatedPost = messageBoardService.revertToOriginalImage(postId, userId);
        MessageBoardResponseDto responseDto = messageBoardMapper.toDto(updatedPost);
        // 重新填充 user-specific 資訊
        responseDto.setIsLiked(postLikeRepository.existsById(new PostLikeKey(postId, userId)));
        // ... 其他 isFriend, isFollowed 邏輯 ...

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 刪除貼文圖片
     */
    @PutMapping("/post/{postId}/delete-image")
    public ResponseEntity<?> deletePostImage(@PathVariable Long postId, @RequestParam Integer userId) {
        MessageBoard updatedPost = messageBoardService.deletePostImage(postId, userId);
        MessageBoardResponseDto responseDto = messageBoardMapper.toDto(updatedPost);
        // 回傳更新後的貼文 (不含圖片)
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/admin/search")
    public ResponseEntity<?> searchUser(@RequestParam String username) {
        return ResponseEntity.ok(messageBoardService.searchUserAndPostsForAdmin(username));
    }
    /**
     * 管理員刪除貼文
     */
    @DeleteMapping("/admin/post/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        messageBoardService.deletePostByAdmin(postId);
        return ResponseEntity.ok().body(Map.of("message", "刪除成功"));
    }
}