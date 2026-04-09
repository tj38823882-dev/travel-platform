package com.example.demo.service;

import java.util.List;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Friendships;
import com.example.demo.model.User;
import com.example.demo.repository.FriendshipsRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.responseDto.FriendRequestResponseDto;

import jakarta.transaction.Transactional;

@Service
public class FriendshipsService {
	@Autowired
	private FriendshipsRepository friendshipsRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NotificationService notificationService;

	@Transactional
	public void sendRequest(Integer myId, User target) {
		User me = userRepository.findByUserId(myId).orElseThrow(() -> new RuntimeException("找不到發送者"));
		if (friendshipsRepository.existsByRequesterAndReceiver(me, target)
				|| friendshipsRepository.existsByRequesterAndReceiver(target, me)) {
			throw new RuntimeException("你們已經是好友，或是已經發送過申請了！");
		}
		Friendships friendships = new Friendships();
		friendships.setRequester(me);
		friendships.setReceiver(target);
		friendships.setStatus(0);
		friendshipsRepository.save(friendships);
		notificationService.sendFriendRequestNotification(me, target, friendships.getFriendshipId());
	}

	public void acceptFriendRequest(Integer currentUserId, Integer friendshipId) {
		// 1. 找不到就直接噴錯
		Friendships friendship = friendshipsRepository.findById(friendshipId)
				.orElseThrow(() -> new RuntimeException("找不到該好友申請"));

		// 2. 檢查權限：如果不符合就噴錯，讓 Controller 的 catch 去抓
		if (!friendship.getReceiver().getUserId().equals(currentUserId)) {
			throw new RuntimeException("你沒有權限接受這筆申請！");
		}

		// 3. 改狀態
		friendship.setStatus(1);
		friendshipsRepository.save(friendship);
		notificationService.sendFriendRequestAcceptedNotification(friendship.getReceiver(), friendship.getRequester());
	}

	// 💡 保留這個「安全版」，並刪除另一個
	@Transactional
	public void removeFriendship(Integer myId, Integer friendshipId) {
		// 1. 找尋該筆紀錄
		Friendships friendship = friendshipsRepository.findById(friendshipId)
				.orElseThrow(() -> new RuntimeException("找不到該好友關係"));

		// 2. 安全檢查：確保刪除者是關係人之一
		Integer requesterId = friendship.getRequester().getUserId();
		Integer receiverId = friendship.getReceiver().getUserId();

		if (!myId.equals(requesterId) && !myId.equals(receiverId)) {
			throw new RuntimeException("你沒有權限刪除此好友關係");
		}

		// 3. 執行刪除
		friendshipsRepository.delete(friendship);
	}

	public List<FriendRequestResponseDto> getPendingRequests(Integer myId) {
		// 1. 找到目前的登入者實體
		User me = userRepository.findById(myId)
				.orElseThrow(() -> new RuntimeException("找不到使用者"));

		// 2. 找出所有「接收者是我」且「狀態為 0」的申請
		return friendshipsRepository.findByReceiverAndStatus(me, 0)
				.stream()
				.map(f -> {
					// 💡 因為不使用 @AllArgsConstructor，所以手動用 Setter 賦值
					FriendRequestResponseDto dto = new FriendRequestResponseDto();
					dto.setFriendshipId(f.getFriendshipId());
					dto.setRequesterName(f.getRequester().getUsername());
					dto.setProfilePictureUrl(f.getRequester().getProfilePictureUrl());
					return dto;
				})
				.collect(Collectors.toList());
	}

	public List<FriendRequestResponseDto> getFriendList(Integer myId) {
		// 1. 找出所有狀態為 1 (好友) 的紀錄
		List<Friendships> list = friendshipsRepository.findAllByStatus(1);

		// 2. 轉換成 DTO 列表
		return list.stream()
				.filter(f -> f.getRequester().getUserId().equals(myId) || f.getReceiver().getUserId().equals(myId))
				.map(f -> {
					FriendRequestResponseDto dto = new FriendRequestResponseDto();
					dto.setFriendshipId(f.getFriendshipId()); // 設定 ID

					// 判斷誰才是「對方」
					User targetUser = f.getRequester().getUserId().equals(myId) ? f.getReceiver() : f.getRequester();

					// 根據你的 DTO 欄位設定：叫 requesterName
					dto.setRequesterName(targetUser.getUsername());
					dto.setProfilePictureUrl(targetUser.getProfilePictureUrl());

					return dto;
				})
				.collect(Collectors.toList());
	}

	public boolean areFriends(Integer userId1, Integer userId2) {
		return friendshipsRepository.findFriendIds(userId1).contains(userId2);
	}

}