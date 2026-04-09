package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Comment;


@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // 找出某篇文章下的所有回覆
    List<Comment> findByMessageBoard_PostId(Long postId);
    
}
