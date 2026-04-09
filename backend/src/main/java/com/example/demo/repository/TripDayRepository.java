package com.example.demo.repository;

import com.example.demo.model.Trip;
import com.example.demo.model.TripDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripDayRepository extends JpaRepository<TripDay, Integer> {
    List<TripDay> findByTrip(Trip trip);
}
