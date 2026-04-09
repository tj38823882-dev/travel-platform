package com.example.demo.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.requestDto.FriendRequestDto;
import com.example.demo.responseDto.FriendRequestResponseDto;
import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.FriendshipsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor

public class FriendshipsController {

	private final FriendshipsService friendshipsService;

	private final UserRepository userRepository;

	private final AuthService authService;

	@PostMapping("/friend/add")
	public ResponseEntity<String> addFriend(@RequestBody FriendRequestDto friendRequestDto) {
		LoggedInMemberDto me = authService.getLoggedInUser();
		Integer myId = me.getId();
		String myUsername = me.getUsername();
		if (myUsername.equals(friendRequestDto.getReceiverName())) {
			return ResponseEntity.badRequest().body("你不能發送好友申請給自己");
		}
		Optional<User> byOptionalId = userRepository.findByUsername(friendRequestDto.getReceiverName());
		if (byOptionalId.isEmpty()) {
			return ResponseEntity.badRequest().body("找不到該使用者");
		}

		User targetUser = byOptionalId.get();
		try {
			friendshipsService.sendRequest(myId, targetUser);
			return ResponseEntity.ok("好友申請已送出！");
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("/friend/accept/{id}")
	public ResponseEntity<String> acceptFriend(@PathVariable("id") Integer friendshipId) {
		LoggedInMemberDto me = authService.getLoggedInUser();
		Integer myId = me.getId();
		try {

			friendshipsService.acceptFriendRequest(myId, friendshipId);
			return ResponseEntity.ok("已成為好友！");

		} catch (RuntimeException e) {

			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		}
	}

	@DeleteMapping("/friend/delete/{id}")
	public ResponseEntity<String> deleteFriend(@PathVariable("id") Integer friendshipId) {
		// 1. 取得目前登入者的 ID
		Integer myId = authService.getLoggedInUser().getId();

		try {
			// 2. 呼叫「安全版」的刪除方法，傳入我的 ID 和關係 ID
			friendshipsService.removeFriendship(myId, friendshipId);

			return ResponseEntity.ok("已移除該關係");
		} catch (RuntimeException e) {
			// 3. 如果權限不符或找不到資料，回傳錯誤訊息
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping("friend/requests")
	public ResponseEntity<List<FriendRequestResponseDto>> getPendingRequests() {
		// 使用你已經寫好的 authService 取得 ID
		Integer myId = authService.getLoggedInUser().getId();

		List<FriendRequestResponseDto> list = friendshipsService.getPendingRequests(myId);
		return ResponseEntity.ok(list);
	}

	@GetMapping("/list")
	public ResponseEntity<List<FriendRequestResponseDto>> getFriendList() {
		Integer myId = authService.getLoggedInUser().getId();
		List<FriendRequestResponseDto> friends = friendshipsService.getFriendList(myId);
		return ResponseEntity.ok(friends);
	}
}