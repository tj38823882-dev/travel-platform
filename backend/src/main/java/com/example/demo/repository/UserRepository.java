package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	List<User> findByUsernameContaining(String keyword);

	Optional<User> findByUserId(Integer userId);

	Optional<User> findByEmail(String email);

	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.data.jpa.repository.Query("UPDATE User u SET u.points = u.points - :cost WHERE u.userId = :userId AND u.points >= :cost")
	int deductPoints(@org.springframework.data.repository.query.Param("userId") Integer userId,
			@org.springframework.data.repository.query.Param("cost") Integer cost);
}