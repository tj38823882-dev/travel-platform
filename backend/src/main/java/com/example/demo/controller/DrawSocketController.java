package com.example.demo.controller;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.example.demo.requestDto.DrawActionDto;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DrawSocketController {

    // 記錄活躍的房間及其參與者 Session ID
    // Map<RoomID, Set<SessionID>>
    private final Map<String, Set<String>> activeRooms = new ConcurrentHashMap<>();
    
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 開啟新房間 (由 DrawController 呼叫)
     */
    public void createRoom(String roomId) {
        activeRooms.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
    }

    /**
     * 檢查房間是否活躍 (供 API 查詢用)
     */
    public boolean isRoomActive(String roomId) {
        return activeRooms.containsKey(roomId);
    }

    /**
     * 監聽：使用者訂閱頻道 (代表加入房間)
     */
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();

        // 解析路徑 /topic/draw/{roomId}
        if (destination != null && destination.startsWith("/topic/draw/")) {
            String roomId = destination.substring("/topic/draw/".length());
            
            // 如果房間存在，加入 Session ID
            if (activeRooms.containsKey(roomId)) {
                activeRooms.get(roomId).add(sessionId);
            }
        }
    }

    /**
     * 監聽：使用者斷線 (代表離開)
     */
    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        // 尋找該 Session 所在的房間
        activeRooms.entrySet().stream()
            .filter(entry -> entry.getValue().contains(sessionId))
            .map(Map.Entry::getKey)
            .findFirst()
            .ifPresent(roomId -> {
                // 透過原子操作 remove，確保只有第一個觸發的執行緒會成功並發送通知
                if (activeRooms.remove(roomId) != null) {
                    DrawActionDto closeMsg = new DrawActionDto();
                    closeMsg.setType("ROOM_CLOSED");
                    closeMsg.setRoomId(roomId);
                    messagingTemplate.convertAndSend("/topic/draw/" + roomId, closeMsg);
                    System.out.println("Session " + sessionId + " disconnected. Room " + roomId + " closed immediately.");
                }
            });
    }

    /**
     * 處理繪圖動作轉發
     * 前端發送至: /app/draw/{roomId}
     * 後端轉發至: /topic/draw/{roomId}
     */
    @MessageMapping("/draw/{roomId}")
    @SendTo("/topic/draw/{roomId}")
    public DrawActionDto handleDrawAction(@DestinationVariable String roomId, @Payload DrawActionDto action) {
        // 🛠️ 驗證：如果房間不存在 (已被刪除)，回傳錯誤指令
        if (!activeRooms.containsKey(roomId)) {
            System.out.println("拒絕繪圖：房間 " + roomId + " 已關閉");
            action.setType("ERROR"); // 前端需要處理這個類型
            // 不設 roomId，這樣前端可能就不會處理，或者我們可以利用這個回傳告訴前端跳轉
            return action; 
        }

        // 🛠️ Debug: 印出收到的資訊，確認 WebSocket 有通
        System.out.println("【後端收到 WS 訊息】RoomID: " + roomId + ", Type: " + action.getType() + ", Sender: " + action.getUsername());
        
        // 🛠️ 強制設定 roomId，確保前端收到時欄位正確
        action.setRoomId(roomId);
        return action;
    }
}
