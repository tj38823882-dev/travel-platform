package com.example.demo.controller;

import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.requestDto.TripRequestDto;
import com.example.demo.responseDto.TripResponseDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.TripService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final AuthService authService;
    // 注入 AuthService，處理 目前登入的是誰

    @PostMapping
    public ResponseEntity<TripResponseDto> createTrip(@RequestBody TripRequestDto dto) {
        LoggedInMemberDto me = authService.getLoggedInUser();
        TripResponseDto response = tripService.createTrip(dto, me.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TripResponseDto>> getMyTrips() {
        LoggedInMemberDto me = authService.getLoggedInUser();
        List<TripResponseDto> response = tripService.getTripsByUser(me.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponseDto> getTripById(@PathVariable Integer tripId) {
        LoggedInMemberDto me = authService.getLoggedInUser();
        TripResponseDto response = tripService.getTripById(tripId, me.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tripId}")
    public ResponseEntity<TripResponseDto> updateTrip(@PathVariable Integer tripId, @RequestBody TripRequestDto dto) {
        LoggedInMemberDto me = authService.getLoggedInUser();
        TripResponseDto response = tripService.updateTrip(tripId, dto, me.getId());
        return ResponseEntity.ok(response);
    }
}