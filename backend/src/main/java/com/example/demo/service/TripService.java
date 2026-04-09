package com.example.demo.service;

import com.example.demo.mapper.TripMapper;
import com.example.demo.mapper.TripDayMapper;
import com.example.demo.mapper.StopMapper;
import com.example.demo.model.Trip;
import com.example.demo.model.TripDay;
import com.example.demo.model.Stop;
import com.example.demo.model.User;
import com.example.demo.repository.TripRepository;
import com.example.demo.repository.TripDayRepository;
import com.example.demo.repository.PurchasedItineraryRepository;
import com.example.demo.repository.StopRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.requestDto.StopRequestDto;
import com.example.demo.requestDto.TripDayRequestDto;
import com.example.demo.requestDto.TripRequestDto;
import com.example.demo.responseDto.TripResponseDto;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final PurchasedItineraryRepository purchasedItineraryRepository;

    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final StopRepository stopRepository;
    private final UserRepository userRepository;
    private final TripMapper tripMapper;
    private final TripDayMapper tripDayMapper;
    private final StopMapper stopMapper;

    @Transactional
    public TripResponseDto createTrip(TripRequestDto dto, Integer userId) {
        System.out.println("createTrip 被呼叫");
        // userId 是從 me.getId(), controller 傳來的，目前登入使用者的 id
        // Service 本身不管登入狀態，它只接受一個 userId 整數，完全不知道 session 或 token

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到使用者：" + userId));

        Trip trip = tripMapper.toEntity(dto);
        // 從 java DTO 物件轉成 可存入 DB 的 java entity (可以想 就是會少了幾個欄位的 Trip
        // 物件，但此時還缺 user、createdAt、status 等欄位
        trip.setUser(user);
        trip.setCreatedAt(LocalDateTime.now());
        // trip.setStatus(Trip.Status.active);
        Trip savedTrip = tripRepository.save(trip);

        if (dto.getTripDays() != null) {
            dto.getTripDays().forEach(dayDto -> {
                TripDay tripDay = tripDayMapper.toEntity(dayDto);
                tripDay.setTrip(savedTrip);
                tripDay.setCreatedAt(LocalDateTime.now());
                // tripDay.setStatus(TripDay.Status.active);
                TripDay savedDay = tripDayRepository.save(tripDay);

                if (dayDto.getStops() != null) {
                    dayDto.getStops().forEach(stopDto -> {
                        Stop stop = stopMapper.toEntity(stopDto);
                        stop.setTripDay(savedDay);
                        stop.setCreatedAt(LocalDateTime.now());
                        // stop.setStatus(Stop.Status.active);
                        stopRepository.save(stop);
                    });
                }
            });
        }

        Trip fullTrip = tripRepository.findById(savedTrip.getId())
                .orElseThrow(() -> new RuntimeException("儲存失敗"));
        System.out.println("createTrip 查看console," + dto);
        return tripMapper.toDto(fullTrip);
    }

    ///////////
    /////////////// 查詢用的 service 方法，沒有寫 @Transactional，因為它們只讀取資料，不修改資料庫，所以不需要開啟交易
    ///////////

    @Transactional(readOnly = true)
    public List<TripResponseDto> getTripsByUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("找不到使用者：" + userId));
        // return tripRepository.findByUser(user).stream()
        // .map(tripMapper::toDto)
        // .collect(Collectors.toList()); 改軟刪除
        return tripRepository.findByUser(user).stream()
                .map(tripMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TripResponseDto getTripById(Integer tripId, Integer userId) {
        Trip trip = tripRepository.findByIdWithDays(tripId)
                .orElseThrow(() -> new RuntimeException("找不到旅程：" + tripId));

        boolean isOwner = trip.getUser().getUserId().equals(userId);
        boolean hasPurchased = purchasedItineraryRepository.existsByUserUserIdAndItineraryTripId(userId, tripId);

        if (!isOwner && !hasPurchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權限查看此旅程");
        }
        return tripMapper.toDto(trip);
    }

    /////
    // 可以做所有的改動
    /////
    @Transactional
    public TripResponseDto updateTrip(Integer tripId, TripRequestDto dto, Integer userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("找不到旅程：" + tripId));

        if (!trip.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("無權限修改此旅程");
        }

        // 更新 Trip 基本欄位
        trip.setTitle(dto.getTitle());
        trip.setStartDate(dto.getStartDate());
        trip.setEndDate(dto.getEndDate());
        trip.setCoverImage(dto.getCoverImage());
        trip.setHashTag(dto.getHashTag());

        ////////// tripDay 部分
        // 前端送來有 id 的 TripDay 集合
        List<TripDay> existingDays = tripDayRepository.findByTrip(trip);
        Set<Integer> incomingDayIds = dto.getTripDays() == null ? new HashSet<>()
                : dto.getTripDays().stream()
                        .filter(d -> d.getId() != null)
                        .map(TripDayRequestDto::getId)
                        .collect(Collectors.toSet());

        // 真刪前端沒傳的 TripDay 和其 stops
        for (TripDay day : existingDays) {
            if (!incomingDayIds.contains(day.getId())) {
                stopRepository.deleteAll(stopRepository.findByTripDay(day));
                tripDayRepository.delete(day);
            }
        }
        if (dto.getTripDays() != null) {
            dto.getTripDays().forEach(dayDto -> {
                TripDay tripDay;
                if (dayDto.getId() != null) {
                    // 更新既有的 TripDay
                    tripDay = tripDayRepository.findById(dayDto.getId())
                            .orElseThrow(() -> new RuntimeException("找不到 TripDay：" + dayDto.getId()));
                    tripDayMapper.updateEntity(dayDto, tripDay);
                } else {
                    // 新增
                    tripDay = tripDayMapper.toEntity(dayDto);
                    tripDay.setTrip(trip);
                    tripDay.setCreatedAt(LocalDateTime.now());
                }
                TripDay savedDay = tripDayRepository.save(tripDay);

                ////////// stop 部分
                // 前端送來有 id 的 Stop 集合
                List<Stop> existingStops = stopRepository.findByTripDay(savedDay);
                Set<Integer> incomingStopIds = dayDto.getStops() == null ? new HashSet<>()
                        : dayDto.getStops().stream()
                                .filter(s -> s.getId() != null)
                                .map(StopRequestDto::getId)
                                .collect(Collectors.toSet());
                // 真刪前端沒傳的 stop
                for (Stop stop : existingStops) {
                    if (!incomingStopIds.contains(stop.getId())) {
                        stopRepository.delete(stop);
                    }
                }
                if (dayDto.getStops() != null) {
                    dayDto.getStops().forEach(stopDto -> {
                        Stop stop;
                        if (stopDto.getId() != null) {
                            // 更新既有的 stop
                            stop = stopRepository.findById(stopDto.getId())
                                    .orElseThrow(() -> new RuntimeException("找不到 Stop：" + stopDto.getId()));
                            stopMapper.updateEntity(stopDto, stop);
                        } else {
                            // 新增
                            stop = stopMapper.toEntity(stopDto);
                            stop.setTripDay(savedDay);
                            stop.setCreatedAt(LocalDateTime.now());
                        }
                        stopRepository.save(stop);
                    });
                }
            });
        }

        tripRepository.save(trip);

        Trip fullTrip = tripRepository.findByIdWithDays(tripId)
                .orElseThrow(() -> new RuntimeException("更新失敗"));
        return tripMapper.toDto(fullTrip);
    }
    // 這個設計的邏輯是「前端送什麼就是最終狀態」，所以只要某個 stop 的 id 沒出現在 incoming 清單裡，就直接刪掉。前端不需要另外發
    // DELETE request，只要在 PUT body 裡把那個 stop 拿掉就等於刪除了，delete 的語意被合併進 update 裡面了。

}
