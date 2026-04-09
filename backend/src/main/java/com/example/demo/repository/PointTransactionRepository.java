package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.PointTransaction;
import com.example.demo.model.User;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Integer> {
    List<PointTransaction> findByUserOrderByCreatedAtDesc(User user);
}
