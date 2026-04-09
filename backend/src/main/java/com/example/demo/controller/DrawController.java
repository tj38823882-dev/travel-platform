package com.example.demo.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Friendships;
import com.example.demo.model.User;
import com.example.demo.repository.FriendshipsRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/draw")
@RequiredArgsConstructor
public class DrawController {

    private final FriendshipsRepository friendshipsRepo;
    private final NotificationService notificationService;
    private final UserRepository userRepo;
    private final DrawSocketController drawSocketController; // 注入 Socket Controller

    /**
     * 發送畫畫邀請
     * 前端傳入: { "receiverId": friendshipId } 
     */
    @PostMapping("/invite")
    public ResponseEntity<?> inviteDraw(@RequestBody Map<String, Integer> request) {
        Integer friendshipId = request.get("receiverId");
        if (friendshipId == null) {
            return ResponseEntity.badRequest().body("缺少 receiverId (friendshipId)");
        }

        // 1. 取得當前發送者
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            return ResponseEntity.status(401).body("請先登入");
        }
        LoggedInMemberDto me = (LoggedInMemberDto) principal;
        User sender = userRepo.findById(me.getId())
                .orElseThrow(() -> new RuntimeException("找不到使用者"));

        // 取得好友關係
        // 注意：這裡假設你的 Friendships Entity 欄位為 requester 和 friend (或 receiver)
        // 若欄位名稱不同 (例如 user/friend)，請依據實際 Entity 修改 get 方法
        Friendships friendship = friendshipsRepo.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("找不到好友關係"));

        // 3. 判斷接收者 (好友關係中，不是我的那個人就是接收者)
        User receiver;
        if (friendship.getRequester().getUserId().equals(sender.getUserId())) {
            receiver = friendship.getReceiver(); 
        } else {
            receiver = friendship.getRequester();
        }

        // 4. 生成隨機房間 ID
        String roomId = UUID.randomUUID().toString();

        // 🛠️ 關鍵：在發送通知前，先在後端註冊這個房間為「活躍」
        drawSocketController.createRoom(roomId);

        // 5. 發送通知
        notificationService.sendDrawInvitation(sender, receiver, roomId);

        return ResponseEntity.ok(Map.of("message", "邀請已發送", "roomId", roomId));
    }

    /**
     * 檢查房間是否有效 (用於前端路由守衛)
     */
    @GetMapping("/check/{roomId}")
    public ResponseEntity<Boolean> checkRoom(@PathVariable String roomId) {
        return ResponseEntity.ok(drawSocketController.isRoomActive(roomId));
    }
}