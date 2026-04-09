package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.mapper.MessageBoardMapper;
import com.example.demo.model.Comment;
import com.example.demo.model.MessageBoard;
import com.example.demo.model.PostLike;
import com.example.demo.model.PostLikeKey;
import com.example.demo.model.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FollowRepository;
import com.example.demo.repository.FriendshipsRepository;
import com.example.demo.repository.MessageBoardRepository;
import com.example.demo.repository.PostLikeRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.requestDto.MessageBoardCreateDto;
import com.example.demo.responseDto.MessageBoardResponseDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageBoardService {

    private final UserRepository userRepository;
    private final MessageBoardRepository messageBoardRepository;
    private final CommentRepository commentRepository;
    private final FriendshipsRepository friendshipsRepository;
    private final PostLikeRepository postLikeRepository;
    private final FollowRepository followRepository;
    private final NotificationService notificationService;

    @Autowired
    private MessageBoardMapper messageBoardMapper;

    @Autowired
    private ImageUploadService imageUploadService;

    @Transactional
    public Map<String, Object> likePost(Long postId, Integer userId) {
        // 1. 找貼文
        MessageBoard post = messageBoardRepository.findById(postId.longValue())
                .orElseThrow(() -> new RuntimeException("找不到貼文"));

        // 2. 建立複合主鍵
        PostLikeKey key = new PostLikeKey(postId.longValue(), userId);

        // 3. 判斷是「新增讚」還是「取消讚」
        boolean isLiked;
        if (postLikeRepository.existsById(key)) {
            // 已存在 -> 移除讚
            postLikeRepository.deleteById(key);
            // 使用原子操作更新 DB
            messageBoardRepository.decrementLikes(postId);

            // 更新記憶體中的物件數值以便回傳給前端 (不存回 DB)
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            isLiked = false;
        } else {
            // 不存在 -> 加入讚
            PostLike newLike = new PostLike();
            newLike.setId(key);
            newLike.setMessageBoard(post);
            User user = userRepository.getReferenceById(userId);
            newLike.setUser(user);
            postLikeRepository.save(newLike);

            // 使用原子操作更新 DB
            messageBoardRepository.incrementLikes(postId);

            // 更新記憶體中的物件數值以便回傳給前端 (不存回 DB)
            post.setLikesCount(post.getLikesCount() + 1);
            isLiked = true;

            // 發送按讚通知
            notificationService.sendLikeNotification(userId, post);
        }

        // 4. 回傳物件給前端，好讓 Vue 更新 UI
        return Map.of(
                "likesCount", post.getLikesCount(),
                "isLiked", isLiked);
    }

    /**
     * 去 Repository 拿 Entity List
     * 透過 Mapper 轉成 DTO List
     */
    public List<MessageBoardResponseDto> getAllPosts(String filter, String search, Integer currentUserId, int page,
            int size, int seed) {

        // 1. 建立分頁請求 (PageRequest 會自動幫我們換算 SQL 的 OFFSET 和 FETCH)
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageBoard> entityPage;
        // 2. 判斷要撈「全部」還是「好友」
        if (search != null && !search.trim().isEmpty()) {
            // 如果有搜尋關鍵字，優先進行搜尋 (忽略 filter)
            entityPage = messageBoardRepository.findByContentContainingOrderByCreatedAtDesc(search.trim(), pageable);
        } else if ("friends".equals(filter) && currentUserId != null) {
            // filter=friends，只抓好友 (不包含自己，因為有獨立的「我的貼文」)
            // 1. 先找出所有好友 ID
            List<Integer> userIds = friendshipsRepository.findFriendIds(currentUserId);

            if (userIds.isEmpty()) {
                entityPage = Page.empty(pageable);
            } else {
                entityPage = messageBoardRepository.findByUser_UserIdInOrderByCreatedAtDesc(userIds, pageable);
            }
        } else if ("my".equals(filter) && currentUserId != null) {
            // filter=my，只抓自己的貼文
            entityPage = messageBoardRepository.findByUser_UserIdOrderByCreatedAtDesc(currentUserId, pageable);
        } else if ("following".equals(filter) && currentUserId != null) {
            // filter=following，只抓「我追蹤的人」 (不包含自己)
            List<Integer> followingIds = followRepository.findFollowingIds(currentUserId);

            // 如果沒有追蹤任何人，直接回傳空分頁，避免錯誤
            if (followingIds.isEmpty()) {
                entityPage = Page.empty(pageable);
            } else {
                entityPage = messageBoardRepository.findByUser_UserIdInOrderByCreatedAtDesc(followingIds, pageable);
            }
        } else { // filter=all 或其他未定義情況
            // filter=all，改用熱門演算法排序
            entityPage = messageBoardRepository.findAllOrderByHotness(seed, pageable);
        }
        // 3. 從 Page 物件中把 List 萃取出來
        List<MessageBoard> entities = entityPage.getContent();

        // 4. 轉換成 DTO 列表
        List<MessageBoardResponseDto> dtos = messageBoardMapper.toDtoList(entities);

        // 5. 如果有登入，標記該使用者點過讚的貼文
        if (currentUserId != null) {
            // 從 PostLikeRepository 找出該使用者所有點讚紀錄
            // 建議在 Repository 寫一個：List<PostLike> findByIdUserId(Integer userId);
            List<PostLike> userLikes = postLikeRepository.findByIdUserId(currentUserId);

            // 提取出所有被點讚的 postId 集合，方便比對
            Set<Long> likedPostIds = userLikes.stream()
                    .map(like -> like.getId().getPostId())
                    .collect(Collectors.toSet());

            // 6. 找出該使用者追蹤的人 (用於標記 isFollowed)
            List<Integer> myFollowingIds = followRepository.findFollowingIds(currentUserId);
            Set<Integer> followingSet = myFollowingIds.stream().collect(Collectors.toSet());

            // 7. 找出該使用者的好友 (用於標記 isFriend)
            List<Integer> myFriendIds = friendshipsRepository.findFriendIds(currentUserId);
            Set<Integer> friendSet = myFriendIds.stream().collect(Collectors.toSet());

            // 遍歷 DTO，如果 postId 在集合中，就設為 true
            dtos.forEach(dto -> {
                if (dto.getPostId() != null) {
                    dto.setIsLiked(likedPostIds.contains(dto.getPostId().longValue()));
                }
                if (dto.getUserId() != null) {
                    dto.setIsFollowed(followingSet.contains(dto.getUserId()));
                    dto.setIsFriend(friendSet.contains(dto.getUserId()));
                }
                // 處理分享貼文的狀態 (引文)
                if (dto.getSharedPost() != null && dto.getSharedPost().getUserId() != null) {
                    dto.getSharedPost().setIsFollowed(followingSet.contains(dto.getSharedPost().getUserId()));
                    dto.getSharedPost().setIsFriend(friendSet.contains(dto.getSharedPost().getUserId()));
                }
            });
        }

        return dtos;
    }

    /**
     * 取得單一貼文詳情
     */
    public MessageBoardResponseDto getPostById(Long postId, Integer currentUserId) {
        MessageBoard post = messageBoardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("找不到該貼文"));

        MessageBoardResponseDto dto = messageBoardMapper.toDto(post);

        if (currentUserId != null) {
            dto.setIsLiked(postLikeRepository.existsById(new PostLikeKey(postId, currentUserId)));

            // 預先取得關聯清單，供主貼文與分享貼文使用
            List<Integer> followingIds = followRepository.findFollowingIds(currentUserId);
            List<Integer> friendIds = friendshipsRepository.findFriendIds(currentUserId);

            if (!post.getUser().getUserId().equals(currentUserId)) {
                dto.setIsFollowed(followingIds.contains(post.getUser().getUserId()));
                dto.setIsFriend(friendIds.contains(post.getUser().getUserId()));
            }
            // 處理分享貼文的狀態
            if (dto.getSharedPost() != null && dto.getSharedPost().getUserId() != null) {
                dto.getSharedPost().setIsFollowed(followingIds.contains(dto.getSharedPost().getUserId()));
                dto.getSharedPost().setIsFriend(friendIds.contains(dto.getSharedPost().getUserId()));
            }
        }
        return dto;
    }

    public MessageBoardResponseDto createMessageBoard(MessageBoardCreateDto dto, Long sharedPostId) {
        // 修正：只有當「內容為空」且「圖片也為空」時才報錯
        boolean noContent = Strings.isBlank(dto.getContent());
        boolean noImage = Strings.isBlank(dto.getImageUrl());
        boolean noSharedPost = (sharedPostId == null);

        // 1.檢查內文和圖片是否為空
        if (noContent && noImage && noSharedPost) {
            throw new RuntimeException("發文內容或圖片不能同時為空");
        }

        // 2.dto -> entity
        MessageBoard messageBoard = messageBoardMapper.toEntity(dto);
        // 確保圖片網址有存進去 (假設 Entity 也有 imageUrl 欄位)
        messageBoard.setImageUrl(dto.getImageUrl());
        // 1. 修正：第一次建立貼文時，若有圖片，將其設為原始圖片
        if (dto.getImageUrl() != null) {
            messageBoard.setOriginalImageUrl(dto.getImageUrl());
        }
        // 3. 處理使用者 (User)
        // 因為 DTO 給的是 userId (Long)，去資料庫查出對應的 User Entity
        // 如果沒查到，就拋出例外
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("找不到使用者 ID: " + dto.getUserId()));

        messageBoard.setUser(user); // 存 userId

        // 4. 處理時間與預設值
        LocalDateTime now = LocalDateTime.now();
        messageBoard.setCreatedAt(now);
        messageBoard.setUpdatedAt(now);
        messageBoard.setLikesCount(0);

        // 5. 處理分享貼文 (Shared Post)
        if (sharedPostId != null) {
            // 假設 MessageBoard Entity 有 sharedPost 欄位 (ManyToOne)
            MessageBoard sharedPost = messageBoardRepository.findById(sharedPostId).orElse(null);
            messageBoard.setSharedPost(sharedPost);
        }

        // 6. 存入資料庫
        MessageBoard savedMessageBoard = messageBoardRepository.save(messageBoard);

        // 新增：發送通知給粉絲與好友
        notificationService.sendNewPostNotification(savedMessageBoard);

        // 7. 回傳轉換後的 DTO
        return messageBoardMapper.toDto(savedMessageBoard);
    }

    /**
     * 刪除留言邏輯
     */
    @Transactional
    public void deleteComment(Long commentId, Integer userId) {
        // 1. 找到該留言
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("找不到該留言"));

        // 2. 權限檢查：留言者本人 OR 貼文作者 可以刪除
        boolean isCommentAuthor = comment.getUser().getUserId().equals(userId);
        boolean isPostAuthor = comment.getMessageBoard().getUser().getUserId().equals(userId);

        if (!isCommentAuthor && !isPostAuthor) {
            throw new RuntimeException("你沒有權限刪除此留言");
        }

        // 3. 執行刪除
        commentRepository.delete(comment);
    }

    /**
     * 刪除貼文邏輯
     */
    @Transactional
    public void deletePost(Long postId, Integer userId) {
        // 1. 查找貼文 (這裡用你的 MessageBoardRepository)
        MessageBoard message = messageBoardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("找不到該貼文"));

        // 2. 權限檢查：只有原發文者能刪除
        if (!message.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("你沒有權限刪除此貼文");
        }

        // 3. 處理 Cloudinary 檔案刪除 (如果你有存 imageUrl)
        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            imageUploadService.deleteFileFromCloudinary(message.getImageUrl());
        }

        // 4. 執行刪除
        messageBoardRepository.delete(message);
    }

    /**
     * 管理員刪除貼文 (不檢查是否為本人)
     */
    @Transactional
    public void deletePostByAdmin(Long postId) {
        MessageBoard message = messageBoardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("找不到該貼文"));

        // 處理 Cloudinary 檔案刪除
        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            imageUploadService.deleteFileFromCloudinary(message.getImageUrl());
        }

        messageBoardRepository.delete(message);
    }

    /**
     * 更新貼文內容邏輯
     */
    @Transactional
    public MessageBoard updatePost(Long postId, String newContent, Integer userId, String imageUrl) {
        // 1. 查找貼文
        MessageBoard post = messageBoardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("找不到該貼文"));

        // 2. 權限檢查：原作者 OR 好友 可以編輯
        boolean isAuthor = post.getUser().getUserId().equals(userId);
        if (!isAuthor) {
            // 如果不是作者，檢查是否為好友
            List<Integer> friendIds = friendshipsRepository.findFriendIds(post.getUser().getUserId());
            if (!friendIds.contains(userId)) {
                throw new RuntimeException("你沒有權限編輯此貼文");
            }
        }

        // 3. 更新內容 (只有作者本人可以修改文字)
        if (isAuthor && newContent != null) {
            post.setContent(newContent);
        }

        // 4. 更新圖片 (如果有新圖片)
        if (imageUrl != null) {
            // 作者本人上傳新圖時，只更新 ImageUrl
            if (isAuthor) {
                post.setImageUrl(imageUrl);
            } else {
                // 如果是好友塗鴉，只更新目前圖片，不刪除舊圖
                if (post.getOriginalImageUrl() == null && post.getImageUrl() != null) {
                    post.setOriginalImageUrl(post.getImageUrl());
                }
                // 只有好友塗鴉時才更新 imageUrl
                post.setImageUrl(imageUrl);

                // 新增：發送好友編輯通知
                User editor = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("找不到編輯者 ID: " + userId));
                notificationService.sendFriendEditNotification(editor, post);
            }
        }

        // 5. 更新時間 (如果你的 Entity 有這個欄位)
        post.setUpdatedAt(java.time.LocalDateTime.now());

        // 因為有 @Transactional，你不需要呼叫 .save()。
        // 當方法結束時，JPA 會自動比對物件狀態並發送 SQL Update。
        // 我們直接回傳這個 post，它已經是最新狀態。
        return post;
    }
    
    /**
     * 恢復成原始圖片
     */
    @Transactional
    public MessageBoard revertToOriginalImage(Long postId, Integer userId) {
        MessageBoard post = messageBoardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("找不到該貼文"));

        // 權限檢查：只有作者本人可以恢復
        if (!post.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("你沒有權限執行此操作");
        }

        // 將目前圖片 URL 設為原始圖片 URL
        post.setImageUrl(post.getOriginalImageUrl());
        return post;
    }

    /**
     * 刪除貼文圖片
     */
    @Transactional
    public MessageBoard deletePostImage(Long postId, Integer userId) {
        MessageBoard post = messageBoardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("找不到該貼文"));

        if (!post.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("你沒有權限執行此操作");
        }

        if (post.getImageUrl() != null) {
            imageUploadService.deleteFileFromCloudinary(post.getImageUrl());
        }

        post.setImageUrl(null);
        post.setOriginalImageUrl(null);
        return post;
    }

    /**
     * 管理員功能：根據 username 搜尋使用者資料與所有貼文
     */
    public Map<String, Object> searchUserAndPostsForAdmin(String username) {
        // 1. 搜尋該使用者的所有貼文
        List<MessageBoard> posts = messageBoardRepository.findByUser_UsernameOrderByCreatedAtDesc(username);

        // 2. 轉成 DTO
        List<MessageBoardResponseDto> postDtos = messageBoardMapper.toDtoList(posts);

        // 3. 嘗試取得使用者基本資料 (從第一篇貼文拿，或是回傳空)
        // 注意：如果該使用者完全沒發過文，這裡會拿不到資料。
        // 若需完整支援，請在 UserRepository 新增 findByUsername(String username) 並在此呼叫。
        Map<String, Object> userInfo = null;
        if (!posts.isEmpty()) {
            User user = posts.get(0).getUser();
            userInfo = Map.of("userId", user.getUserId(), "username", user.getUsername());
        }

        return Map.of(
                "user", userInfo != null ? userInfo : "User not found or no posts",
                "posts", postDtos);
    }
}