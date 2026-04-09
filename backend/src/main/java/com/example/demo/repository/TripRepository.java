package com.example.demo.repository;

import com.example.demo.model.Trip;
import com.example.demo.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {
    List<Trip> findByUser(User user);

    List<Trip> findByStatus(Trip.Status status);
    // 公開行程查詢：用 status 篩選，目前傳 "deleted" 測試，之後改成 "active"

    @Query("SELECT DISTINCT t FROM Trip t LEFT JOIN FETCH t.tripDays WHERE t.id = :tripId")
    Optional<Trip> findByIdWithDays(@Param("tripId") Integer tripId);
    // 這個使用時機是 TripController 裡面 getTripById，因為要回傳的 TripResponseDto 裡面有
    // tripDays的資訊，所以要一次把 tripDays 撈出來不過如果是 getTripsByUserId 就不會用到 tripDays，就不需要這個
    // query

}
