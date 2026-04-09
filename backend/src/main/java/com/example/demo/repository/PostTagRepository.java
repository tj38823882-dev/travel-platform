package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.PostTag;
import com.example.demo.model.PostTagKey;

import java.util.List;

@Repository
public interface PostTagRepository extends JpaRepository<PostTag, PostTagKey> {
    // 找出這篇文章綁定了哪些標籤
    List<PostTag> findByMessageBoard_PostId(Long postId);
}