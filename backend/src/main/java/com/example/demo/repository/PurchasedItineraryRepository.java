package com.example.demo.repository;

import com.example.demo.model.PurchasedItinerary;
import com.example.demo.model.User;
import com.example.demo.model.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchasedItineraryRepository extends JpaRepository<PurchasedItinerary, Integer> {
    List<PurchasedItinerary> findByUser(User user);

    Optional<PurchasedItinerary> findByUserAndItinerary(User user, Itinerary itinerary);

    boolean existsByUserAndItinerary(User user, Itinerary itinerary);

    // 用在 TripService.getTripById：
    // 前端傳 tripId 進來，查「有沒有人買了某個 itinerary，且那個 itinerary 對應的 trip 是這個 tripId」
    // 關聯路徑：PurchasedItinerary → itinerary → trip.id
    boolean existsByUserUserIdAndItineraryTripId(Integer userId, Integer tripId);

    // 用在 ItineraryService.getItineraryById：
    // 前端傳 itineraryId 進來，查「這個 user 有沒有直接買過這個 itinerary」
    // 關聯路徑：PurchasedItinerary → itinerary.id
    boolean existsByUserUserIdAndItineraryId(Integer userId, Integer itineraryId);

}
