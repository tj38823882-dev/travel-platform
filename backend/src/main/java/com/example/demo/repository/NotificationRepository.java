package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 查詢指定使用者的所有通知，並依時間倒序排列 (支援分頁)
     * 
     * @param userId 接收者 ID
     * @return 通知列表
     */
    Page<Notification> findByReceiver_UserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    /**
     * 計算指定使用者的「未讀」通知數量
     * 用於前端顯示鈴鐺上的紅點數字
     * 
     * @param userId 接收者 ID
     * @return 未讀數量
     */
    Long countByReceiver_UserIdAndIsReadFalse(Integer userId);

    /**
     * 一鍵將指定使用者的所有未讀通知標記為已讀
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Integer userId);

    /**
     * 刪除指定使用者的所有通知
     */
    void deleteAllByReceiver_UserId(Integer userId);

    /**
     * 尋找特定發送者給特定接收者的未讀通知 (用於智慧聚合)
     * 命名規則：Receiver_UserId (接收者ID) + Sender_UserId (發送者ID) + Type (類型) + IsReadFalse (未讀)
     */
    Optional<Notification> findTopByReceiver_UserIdAndSender_UserIdAndTypeAndIsReadFalseOrderByCreatedAtDesc(Integer receiverId, Integer senderId, String type);

    /**
     * 尋找特定類型與 ReferenceId 的未讀通知 (用於按讚聚合，不分發送者)
     */
    Optional<Notification> findTopByReceiver_UserIdAndTypeAndReferenceIdAndIsReadFalseOrderByCreatedAtDesc(
            Integer receiverId, String type, Long referenceId);
}
