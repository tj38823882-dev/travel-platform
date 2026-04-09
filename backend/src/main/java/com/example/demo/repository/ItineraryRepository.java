package com.example.demo.repository;

import com.example.demo.model.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Integer> {
    List<Itinerary> findByAuthorUserId(Integer userId);

    List<Itinerary> findByIsActiveTrue();

    List<Itinerary> findByIsActiveTrueAndAuthorUserIdNot(Integer authorId);

}
