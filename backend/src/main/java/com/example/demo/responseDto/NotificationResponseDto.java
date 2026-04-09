package com.example.demo.responseDto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class NotificationResponseDto {
    private Long id;
    private String senderName;
    private String senderProfilePictureUrl;
    private String type;
    private String message;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}