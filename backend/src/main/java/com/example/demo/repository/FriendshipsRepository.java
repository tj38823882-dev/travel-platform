package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Friendships;
import com.example.demo.model.User;

public interface FriendshipsRepository extends JpaRepository<Friendships, Integer> {
	List<Friendships> findByReceiverAndStatus(User receiver, Integer status);

	List<Friendships> findByRequesterAndStatusOrReceiverAndStatus(User u1, Integer s1, User u2, Integer s2);

	boolean existsByRequesterAndReceiver(User requester, User receiver);

	List<Friendships> findAllByStatus(Integer status);

	// 使用 Native Query 與 UNION 快速取得所有好友 ID
	@Query(value = "SELECT ReceiverID FROM Friendships WHERE RequesterID = :userId AND Status = 1 " +
			"UNION " +
			"SELECT RequesterID FROM Friendships WHERE ReceiverID = :userId AND Status = 1", 
			nativeQuery = true)
	List<Integer> findFriendIds(@Param("userId") Integer userId);
}