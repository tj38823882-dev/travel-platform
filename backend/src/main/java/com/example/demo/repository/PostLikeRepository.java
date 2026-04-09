package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.PostLike;
import com.example.demo.model.PostLikeKey;


@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeKey> {
    
    // 計算某一篇文章的總按讚數
    long countById_PostId(Long postId);
    
    // 檢查某個使用者是否已經對某篇文章點過讚
    boolean existsById_PostIdAndId_UserId(Long postId, Long userId);

    // ✅ JPA 會自動解析為：查找 id 屬性 (PostLikeKey) 裡的 userId 欄位
    List<PostLike> findByIdUserId(Integer userId);
}