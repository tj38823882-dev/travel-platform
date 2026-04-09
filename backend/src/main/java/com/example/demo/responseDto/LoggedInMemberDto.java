package com.example.demo.responseDto;

import java.time.LocalDateTime;
import com.example.demo.model.Roles;

import lombok.Data;

@Data
public class LoggedInMemberDto {
    private Integer id;
    private Roles role;
    private String email;
    private String username;
    private Boolean isActive;
    private Integer points;
    private LocalDateTime createdAt;
    private String profilePictureUrl;
}