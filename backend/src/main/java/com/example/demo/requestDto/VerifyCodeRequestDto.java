package com.example.demo.requestDto;

import lombok.Data;

@Data
public class VerifyCodeRequestDto {
    private String email;
    private String code;
    
}
