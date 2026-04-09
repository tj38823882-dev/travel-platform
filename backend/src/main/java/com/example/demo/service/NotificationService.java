package com.example.demo.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.MessageBoard;
import com.example.demo.model.Notification;
import com.example.demo.model.User;
import com.example.demo.repository.FollowRepository;
import com.example.demo.repository.FriendshipsRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final FriendshipsRepository friendshipsRepo;
    private final FollowRepository followRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 發送單則通知 (底層方法)
     */
    public void send(User sender, User receiver, String type, Long referenceId, String message) {
        if (sender.getUserId().equals(receiver.getUserId())) {
            return; // 不用通知自己
        }
        
        Notification notification = new Notification();
        notification.setSender(sender);
        notification.setReceiver(receiver);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setMessage(message);
        notification.setIsRead(false);

        Notification savedNotification = notificationRepo.save(notification);

        // 透過 WebSocket 推送即時通知
        // 為了傳送乾淨的資料，我們手動組裝一個 Map 作為 DTO
        Map<String, Object> payload = Map.of(
            "id", savedNotification.getId(),
            "senderName", savedNotification.getSender().getUsername(),
            "senderProfilePictureUrl", savedNotification.getSender().getProfilePictureUrl(),
            "type", savedNotification.getType(),
            "message", savedNotification.getMessage(),
            "referenceId", savedNotification.getReferenceId(),
            "createdAt", savedNotification.getCreatedAt()
        );

        messagingTemplate.convertAndSendToUser(
            String.valueOf(receiver.getUserId()),
            "/queue/notifications",
            payload);
    }

    /**
     * 當使用者發布新貼文時，通知所有的好友與粉絲
     * @param post 新發布的貼文物件
     */
    @Transactional
    @Async // 建議使用非同步執行，避免發文者卡頓。需在主應用程式加上 @EnableAsync
    public void sendNewPostNotification(MessageBoard post) {
        User author = post.getUser();
        Integer authorId = author.getUserId();

        // 1. 找出所有好友 ID
        List<Integer> friendIds = friendshipsRepo.findFriendIds(authorId);

        // 2. 找出所有粉絲 ID
        List<Integer> followerIds = followRepo.findFollowerIds(authorId);

        // 3. 合併並去重 (因為某人可能既是好友也是粉絲)
        Set<Integer> targetUserIds = new HashSet<>();
        targetUserIds.addAll(friendIds);
        targetUserIds.addAll(followerIds);
        
        // 確保不通知自己
        targetUserIds.remove(authorId);

        if (targetUserIds.isEmpty()) {
            return;
        }

        // 4. 撈出所有接收者的 User 物件
        List<User> targets = userRepo.findAllById(targetUserIds);

        // 5. 逐一處理通知 (加入智慧聚合邏輯)
        // 定義 Regex 用來解析 "發布了 N 則新貼文"
        Pattern countPattern = Pattern.compile("發布了 (\\d+) 則新貼文");

        for (User target : targets) {
            // 檢查是否已存在來自同一人的未讀 NEW_POST 通知
            Optional<Notification> existingOpt = notificationRepo
                .findTopByReceiver_UserIdAndSender_UserIdAndTypeAndIsReadFalseOrderByCreatedAtDesc(
                    target.getUserId(), authorId, "NEW_POST");

            String finalMessage = author.getUsername() + " 發布了一則新貼文";
            
            if (existingOpt.isPresent()) {
                Notification existing = existingOpt.get();
                // 解析舊訊息，計算新的數量
                int count = 1;
                Matcher matcher = countPattern.matcher(existing.getMessage());
                if (matcher.find()) {
                    try {
                        count = Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException e) { /* 忽略 */ }
                }
                count++; // 數量 +1
                
                // 更新訊息內容
                finalMessage = author.getUsername() + " 發布了 " + count + " 則新貼文";
                
                // 刪除舊通知 (為了讓新通知產生新的 ID 與時間，達到置頂效果)
                notificationRepo.delete(existing);
            }

            Notification n = new Notification();
            n.setSender(author);
            n.setReceiver(target);
            n.setType("NEW_POST");
            n.setReferenceId(post.getPostId()); // 連結到最新這篇
            n.setMessage(finalMessage);
            n.setIsRead(false);

            Notification savedN = notificationRepo.save(n);

            // 6. 透過 WebSocket 推送即時通知
            // 為了傳送乾淨的資料，我們手動組裝一個 Map 作為 DTO
            Map<String, Object> payload = Map.of(
                "id", savedN.getId(),
                "senderName", savedN.getSender().getUsername(),
                "senderProfilePictureUrl", savedN.getSender().getProfilePictureUrl(),
                "type", savedN.getType(),
                "message", savedN.getMessage(),
                "referenceId", savedN.getReferenceId(),
                "createdAt", savedN.getCreatedAt()
            );
            
            // 推送到指定使用者的私有頻道 (前端會訂閱 /user/queue/notifications)
            messagingTemplate.convertAndSendToUser(
                String.valueOf(savedN.getReceiver().getUserId()), "/queue/notifications", payload);
        }
    }

    /**
     * 發送按讚通知
     */
    @Transactional
    @Async
    public void sendLikeNotification(Integer likerId, MessageBoard post) {
        User liker = userRepo.findById(likerId).orElse(null);
        if (liker == null) return;

        // 定義接收者集合 (作者 + 協作者)
        Set<User> receivers = new HashSet<>();
        receivers.add(post.getUser());
        if (post.getCollaborator() != null) {
            receivers.add(post.getCollaborator());
        }

        for (User receiver : receivers) {
            if (liker.getUserId().equals(receiver.getUserId())) {
                continue; // 自己按讚自己不用通知
            }

            // 智慧聚合：檢查是否已存在「針對這篇貼文」的未讀 LIKE 通知
            Optional<Notification> existingOpt = notificationRepo
                    .findTopByReceiver_UserIdAndTypeAndReferenceIdAndIsReadFalseOrderByCreatedAtDesc(
                            receiver.getUserId(), "LIKE", post.getPostId());

            // 根據是否為協作貼文調整訊息後綴
            String suffix = (post.getCollaborator() != null) ? " 按讚了你們的共同創作" : " 按讚了你的貼文";
            String message = liker.getUsername() + suffix;

            if (existingOpt.isPresent()) {
                Notification existing = existingOpt.get();
                
                // 判斷是否為同一人重複操作 (防止刷通知)
                boolean isSameLiker = existing.getSender().getUserId().equals(likerId);
                
                int othersCount = 0;
                if (!isSameLiker) {
                    othersCount = 1;
                    // 解析舊訊息，看看原本已經有幾個 "其他人"
                    Pattern countPattern = Pattern.compile("和其他 (\\d+) 人");
                    Matcher matcher = countPattern.matcher(existing.getMessage());
                    if (matcher.find()) {
                        othersCount += Integer.parseInt(matcher.group(1));
                    }
                    message = liker.getUsername() + " 和其他 " + othersCount + " 人" + suffix;
                }
                notificationRepo.delete(existing); // 刪除舊的，以便發送新的
            }

            send(liker, receiver, "LIKE", post.getPostId(), message);
        }
    }

    /**
     * 發送留言通知 (含聚合邏輯)
     */
    @Transactional
    @Async
    public void sendCommentNotification(User commenter, MessageBoard post, String commentContent) {
        User receiver = post.getUser();
        if (commenter.getUserId().equals(receiver.getUserId())) {
            return; // 自己留言自己不用通知
        }

        // 檢查針對這篇貼文是否有未讀的 COMMENT 通知
        Optional<Notification> existingOpt = notificationRepo
                .findTopByReceiver_UserIdAndTypeAndReferenceIdAndIsReadFalseOrderByCreatedAtDesc(
                        receiver.getUserId(), "COMMENT", post.getPostId());

        // 預設單人訊息 (擷取前 15 字作為預覽)
        String preview = commentContent.length() > 15 ? commentContent.substring(0, 15) + "..." : commentContent;
        String message = commenter.getUsername() + " 回覆了你的貼文:「" + preview + "」";

        if (existingOpt.isPresent()) {
            Notification existing = existingOpt.get();
            boolean isSameSender = existing.getSender().getUserId().equals(commenter.getUserId());

            if (!isSameSender) {
                int othersCount = 1;
                Pattern countPattern = Pattern.compile("和其他 (\\d+) 人");
                Matcher matcher = countPattern.matcher(existing.getMessage());
                if (matcher.find()) {
                    othersCount += Integer.parseInt(matcher.group(1));
                }
                message = commenter.getUsername() + " 和其他 " + othersCount + " 人回覆了你的貼文";
            }
            
            notificationRepo.delete(existing); // 刪除舊的以置頂新的
        }

        send(commenter, receiver, "COMMENT", post.getPostId(), message);
    }

    /**
     * 發送好友編輯圖片通知
     */
    @Transactional
    @Async
    public void sendFriendEditNotification(User editor, MessageBoard post) {
        User receiver = post.getUser();
        if (editor.getUserId().equals(receiver.getUserId())) {
            return; // 自己編輯不用通知
        }

        String message = editor.getUsername() + " 突襲了你的圖片！";

        send(editor, receiver, "FRIEND_EDIT", post.getPostId(), message);
    }

    /**
     * 發送好友邀請通知
     * @param requester 發出邀請的人
     * @param addressee 接收邀請的人
     */
    @Transactional
    @Async
    public void sendFriendRequestNotification(User requester, User addressee, Integer friendshipId) {
        String message = requester.getUsername() + " 寄送了好友邀請給你";
        
        // 建立通知物件
        // referenceId 存 friendshipId，方便前端直接接受/拒絕
        send(requester, addressee, "FRIEND_REQUEST", Long.valueOf(friendshipId), message);
    }

    /**
     * 發送接受好友邀請通知
     * @param accepter 接受邀請的人
     * @param requester 原本發出邀請的人 (現在是接收通知的人)
     */
    @Transactional
    @Async
    public void sendFriendRequestAcceptedNotification(User accepter, User requester) {
        String message = accepter.getUsername() + " 接受了你的好友邀請";
        send(accepter, requester, "FRIEND_REQUEST_ACCEPTED", Long.valueOf(accepter.getUserId()), message);
    }

    /**
     * 取得使用者的通知列表
     */
    public Page<Notification> getUserNotifications(Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepo.findByReceiver_UserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 取得未讀數量 (用於顯示紅點)
     */
    public Long getUnreadCount(Integer userId) {
        return notificationRepo.countByReceiver_UserIdAndIsReadFalse(userId);
    }

    /**
     * 標記單則通知為已讀
     */
    public void markAsRead(Long notificationId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepo.save(n);
        });
    }

    /**
     * 標記該使用者所有通知為已讀 (一鍵清除)
     */
    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepo.markAllAsReadByUserId(userId);
    }

    /**
     * 刪除單則通知
     */
    @Transactional
    public void deleteNotification(Long notificationId, Integer userId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            if (n.getReceiver().getUserId().equals(userId)) {
                notificationRepo.delete(n);
            }
        });
    }

    @Transactional
    public void deleteAllNotifications(Integer userId) {
        notificationRepo.deleteAllByReceiver_UserId(userId);
    }

    /**
     * 發送繪圖邀請 (包含 Room ID)
     * 這裡我們將 roomId 放入 WebSocket 推播的 payload 中，
     * 但因為資料庫 Entity 的 referenceId 是 Long 類型，無法存 String 的 UUID，
     * 所以資料庫紀錄僅作為「通知紀錄」，即時跳轉需靠前端接收 WebSocket 訊息當下的處理。
     */
    @Transactional
    @Async
    public void sendDrawInvitation(User sender, User receiver, String roomId) {
        String baseMessage = sender.getUsername() + " 邀請你一起畫畫！";
        // 修改：將 roomId 附加在訊息後方，以便存入資料庫 (用 || 分隔)
        String storedMessage = baseMessage + "||" + roomId;
        
        Long deletedId = null;

        // 1. 檢查是否已有來自同一人的未讀邀請，若有則刪除舊的 (保持最新)
        Optional<Notification> existingOpt = notificationRepo
                .findTopByReceiver_UserIdAndSender_UserIdAndTypeAndIsReadFalseOrderByCreatedAtDesc(
                        receiver.getUserId(), sender.getUserId(), "DRAW_INVITE");

        if (existingOpt.isPresent()) {
            deletedId = existingOpt.get().getId(); // 記下被刪除的 ID
            notificationRepo.delete(existingOpt.get());
        }

        Notification notification = new Notification();
        notification.setSender(sender);
        notification.setReceiver(receiver);
        notification.setType("DRAW_INVITE");
        notification.setReferenceId(0L); // 0 代表特殊事件，或可設為 timestamp
        notification.setMessage(storedMessage); // 存入帶有隱藏 ID 的訊息
        notification.setIsRead(false);

        Notification savedN = notificationRepo.save(notification);

        // 改用 HashMap 以便動態加入 deletedId (Map.of 不支援 null 值或動態put)
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("id", savedN.getId());
        payload.put("senderName", savedN.getSender().getUsername());
        payload.put("senderProfilePictureUrl", savedN.getSender().getProfilePictureUrl());
        payload.put("type", "DRAW_INVITE");
        payload.put("message", baseMessage);
        payload.put("roomId", roomId);
        payload.put("createdAt", savedN.getCreatedAt());
        
        if (deletedId != null) {
            payload.put("deletedId", deletedId);
        }

        messagingTemplate.convertAndSendToUser(
            String.valueOf(receiver.getUserId()),
            "/queue/notifications",
            payload);
    }
}
