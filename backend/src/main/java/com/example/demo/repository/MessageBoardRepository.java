package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.model.MessageBoard;

import java.util.List;

@Repository
public interface MessageBoardRepository extends JpaRepository<MessageBoard, Long> {
    
    // 根據狀態 ID 尋找文章 (例如：找出所有「可見」的文章)
    List<MessageBoard> findByStatus_StatusId(Integer statusId);
    
    // 找出特定使用者的所有留言，並按建立時間排序
    List<MessageBoard> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    // 搜尋包含特定內容的文章
    Page<MessageBoard> findByContentContainingOrderByCreatedAtDesc(String keyword, Pageable pageable);
    // 加上 Pageable 參數，回傳改成 Page<MessageBoard>
    Page<MessageBoard> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 改用標準的 IN 查詢，效能更好且能命中索引
    Page<MessageBoard> findByUser_UserIdInOrderByCreatedAtDesc(List<Integer> userIds, Pageable pageable);

    // 新增：找出特定使用者的所有貼文 (支援分頁)
    Page<MessageBoard> findByUser_UserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    // 新增：根據 username 找出該使用者的所有貼文 (管理員搜尋用)
    List<MessageBoard> findByUser_UsernameOrderByCreatedAtDesc(String username);

    @Modifying
    @Query("UPDATE MessageBoard m SET m.likesCount = m.likesCount + 1 WHERE m.postId = :postId")
    void incrementLikes(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE MessageBoard m SET m.likesCount = m.likesCount - 1 WHERE m.postId = :postId AND m.likesCount > 0")
    void decrementLikes(@Param("postId") Long postId);

    /**
     * 熱門演算法排序 (加入隨機因子)
     * Score = BaseScore * RandomFactor
     * RandomFactor = 1 + (ABS(CHECKSUM(m.postId + seed)) % 10000) / 10.0
     * 這會讓分數在 1.0 ~ 1000.0 倍之間浮動，讓隨機性主導排序，避免熱門文章霸榜
     */
    @Query(
        value = "SELECT * FROM Message_Board m ORDER BY ((COALESCE(m.likesCount, 0) * 2 + COALESCE(m.commentsCount, 0) * 3 + 10) / POWER(DATEDIFF(hour, m.createdAt, GETDATE()) + 2, 0.5)) * (1 + (ABS(CHECKSUM(m.postId + :seed)) % 10000) / 10.0) DESC",
        countQuery = "SELECT count(*) FROM Message_Board",
        nativeQuery = true
    )
    Page<MessageBoard> findAllOrderByHotness(@Param("seed") int seed, Pageable pageable);
}