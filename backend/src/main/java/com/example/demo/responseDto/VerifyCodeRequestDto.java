package com.example.demo.responseDto;

import lombok.Data;

@Data   
public class VerifyCodeRequestDto {
    private String email;
    private String code;
    
}
