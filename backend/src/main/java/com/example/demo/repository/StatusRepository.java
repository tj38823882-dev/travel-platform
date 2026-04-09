package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Status;



@Repository
public interface StatusRepository extends JpaRepository<Status, Integer> {
    
    // 通常用於初始化或根據名稱尋找狀態
    Optional<Status> findByStatus(String statusName);
}