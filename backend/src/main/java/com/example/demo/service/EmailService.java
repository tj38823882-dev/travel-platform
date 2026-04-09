package com.example.demo.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    // 這是 Spring Boot 剛才幫我們準備好的郵差
    private final JavaMailSender mailSender;

    // 設計一個方法：傳入「收件人信箱」跟「6位數驗證碼」
    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        
        // 1. 設定寄給誰
        message.setTo(toEmail);
        
        // 2. 設定信件標題
        message.setSubject("【系統通知】您的登入驗證碼");
        
        // 3. 設定信件內容
        message.setText("歡迎使用本系統！\n\n" +
                        "這是您初次登入的驗證碼：【 " + code + " 】\n\n" +
                        "請在網頁上輸入此 6 位數驗證碼以開通您的帳號。\n" +
                        "（如果您沒有嘗試登入，請忽略此信件）");

        // 4. 把信寄出去！
        mailSender.send(message);
    }
}