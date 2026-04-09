package com.example.demo.controller;

import com.example.demo.requestDto.ItineraryRequestDto;
import com.example.demo.responseDto.ItineraryResponseDto;
import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/itineraries") // 網址暫時對應到前端呼叫的寫法
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final AuthService authService;

    // 上架商品
    @PostMapping
    public ResponseEntity<ItineraryResponseDto> createItinerary(@RequestBody ItineraryRequestDto dto) {
        // 取得當前使用者身分
        LoggedInMemberDto me = authService.getLoggedInUser();
        if (me == null) {
            return ResponseEntity.status(401).build(); // 也可以在 Security Config 擋
        }

        ItineraryResponseDto response = itineraryService.createItinerary(dto, me.getId());
        return ResponseEntity.ok(response);
    }

    // 商城首頁取得所有 active 商品（登入者看不到自己的商品）
    @GetMapping
    public ResponseEntity<List<ItineraryResponseDto>> getAllActiveItineraries() {
        // 嘗試取得當前登入者，若未登入則 currentUserId = null
        Integer currentUserId = null;
        try {
            LoggedInMemberDto me = authService.getLoggedInUser();
            if (me != null)
                currentUserId = me.getId();
        } catch (Exception ignored) {
            // 未登入時 getLoggedInUser() 會拋出 401，直接忽略
        }

        List<ItineraryResponseDto> response = itineraryService.getAllActiveItineraries(currentUserId);
        return ResponseEntity.ok(response);
    }

    // 取得我上架的商品
    @GetMapping("/my-published")
    public ResponseEntity<List<ItineraryResponseDto>> getMyPublishedItineraries() {
        LoggedInMemberDto me = authService.getLoggedInUser();
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        List<ItineraryResponseDto> response = itineraryService.getMyPublishedItineraries(me.getId());
        return ResponseEntity.ok(response);
    }

    // 取得我購買過的商品
    @GetMapping("/purchased")
    public ResponseEntity<List<ItineraryResponseDto>> getPurchasedItineraries() {
        LoggedInMemberDto me = authService.getLoggedInUser();
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        List<ItineraryResponseDto> response = itineraryService.getPurchasedItineraries(me.getId());
        return ResponseEntity.ok(response);
    }

    // 商品詳細頁
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ItineraryResponseDto> getItineraryById(@PathVariable Integer id) {
        LoggedInMemberDto me = authService.getLoggedInUser();
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        ItineraryResponseDto response = itineraryService.getItineraryById(id, me.getId());
        return ResponseEntity.ok(response);
    }
}
