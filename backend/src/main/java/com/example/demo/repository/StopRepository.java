package com.example.demo.repository;

import com.example.demo.model.TripDay;
import com.example.demo.model.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopRepository extends JpaRepository<Stop, Integer> {
    List<Stop> findByTripDay(TripDay tripDay);
}
