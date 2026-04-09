package com.example.demo.service;

import com.example.demo.mapper.TripMapper;
import com.example.demo.model.Itinerary;
import com.example.demo.model.PurchasedItinerary;
import com.example.demo.model.Trip;
import com.example.demo.model.User;
import com.example.demo.repository.ItineraryRepository;
import com.example.demo.repository.PurchasedItineraryRepository;
import com.example.demo.repository.TripRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.requestDto.ItineraryRequestDto;
import com.example.demo.responseDto.ItineraryResponseDto;
import com.example.demo.responseDto.TripResponseDto;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final PurchasedItineraryRepository purchasedItineraryRepository;
    private final TripMapper tripMapper;

    @Transactional
    public ItineraryResponseDto createItinerary(ItineraryRequestDto dto, Integer authorId) {
        // User 的 PK 是 userId，必須用 findByUserId
        User author = userRepository.findByUserId(authorId)
                .orElseThrow(() -> new RuntimeException("找不到使用者：" + authorId));

        Trip trip = tripRepository.findById(dto.getTripId())
                .orElseThrow(() -> new RuntimeException("找不到原始行程規劃：" + dto.getTripId()));

        Itinerary itinerary = new Itinerary();

        itinerary.setTrip(trip);
        itinerary.setAuthor(author);
        itinerary.setTitle(dto.getTitle());
        itinerary.setDescription(dto.getDescription());
        itinerary.setPrice(dto.getPrice());
        itinerary.setIsActive(true);
        trip.setStatus(Trip.Status.published);
        author.setPoints(author.getPoints() + 10);
        userRepository.save(author);

        Itinerary savedItinerary = itineraryRepository.save(itinerary);

        return toDto(savedItinerary);
    }

    @Transactional(readOnly = true)
    public List<ItineraryResponseDto> getMyPublishedItineraries(Integer authorId) {
        return itineraryRepository.findByAuthorUserId(authorId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ItineraryResponseDto> getPurchasedItineraries(Integer userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("找不到使用者：" + userId));

        return purchasedItineraryRepository.findByUser(user).stream()
                .map(PurchasedItinerary::getItinerary)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ItineraryResponseDto> getAllActiveItineraries(Integer excludeAuthorId) {
        List<Itinerary> itineraries;

        if (excludeAuthorId == null) {
            itineraries = itineraryRepository.findByIsActiveTrue();
        } else {
            itineraries = itineraryRepository.findByIsActiveTrueAndAuthorUserIdNot(excludeAuthorId);
        }

        return itineraries.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ItineraryResponseDto getItineraryById(Integer id, Integer userId) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到此商品：" + id));

        boolean isAuthor = itinerary.getAuthor().getUserId().equals(userId);
        boolean hasPurchased = purchasedItineraryRepository.existsByUserUserIdAndItineraryId(userId, id);

        if (!isAuthor && !hasPurchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限查看此行程");
        }
        return toDto(itinerary);
    }

    // Helper: Entity to DTO Map
    private ItineraryResponseDto toDto(Itinerary itinerary) {
        ItineraryResponseDto dto = new ItineraryResponseDto();
        dto.setId(itinerary.getId());
        dto.setTripId(itinerary.getTrip().getId());
        dto.setAuthorId(itinerary.getAuthor().getUserId()); // 新增：回傳作者 ID
        dto.setAuthorName(itinerary.getAuthor().getUsername());
        dto.setTitle(itinerary.getTitle());
        dto.setDescription(itinerary.getDescription());
        dto.setPrice(itinerary.getPrice());
        dto.setIsActive(itinerary.getIsActive());
        // coverImage 可能為 null，安全處理
        String coverImage = itinerary.getTrip().getCoverImage();
        dto.setCoverImage(coverImage != null ? coverImage : "");
        dto.setCreatedAt(itinerary.getCreatedAt());
        dto.setUpdatedAt(itinerary.getUpdatedAt());

        dto.setHashTag(itinerary.getTrip().getHashTag());
        if (itinerary.getTrip() != null) {
            TripResponseDto tripDto = tripMapper.toDto(itinerary.getTrip());
            dto.setTripDays(tripDto.getTripDays());
        }
        return dto;
    }
}
