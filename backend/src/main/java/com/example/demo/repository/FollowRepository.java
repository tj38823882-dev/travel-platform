package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Follow;
import com.example.demo.model.User;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Integer> {
    
    // 檢查是否已追蹤
    boolean existsByFollowerAndFollowing(User follower, User following);

    // 尋找特定的追蹤關係 (用於取消追蹤)
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    // 取得我追蹤的人的 ID 列表 (用於動態牆篩選)
    @Query("SELECT f.following.userId FROM Follow f WHERE f.follower.userId = :followerId")
    List<Integer> findFollowingIds(@Param("followerId") Integer followerId);

    // 新增：取得追蹤我的人 (粉絲) 的 ID 列表 (用於發送通知)
    @Query("SELECT f.follower.userId FROM Follow f WHERE f.following.userId = :followingId")
    List<Integer> findFollowerIds(@Param("followingId") Integer followingId);
}
