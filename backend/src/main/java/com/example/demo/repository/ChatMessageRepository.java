package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    @Query("SELECT m FROM ChatMessage m WHERE " +
            "(m.sender.username = :user1 AND m.receiver.username = :user2) OR " +
            "(m.sender.username = :user2 AND m.receiver.username = :user1) " +
            "ORDER BY m.sendTime ASC")
    List<ChatMessage> findChatHistory(@Param("user1") String user1, @Param("user2") String user2);
}
