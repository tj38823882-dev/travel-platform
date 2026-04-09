package com.example.demo.service;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Roles;
import com.example.demo.model.User;
import com.example.demo.repository.RolesRepository;
import com.example.demo.repository.UserRepository;

@Service
public class OAuth2UserService {
    
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RolesRepository roleRepository;
    
    // 🌟 1. 把我們剛才寫好的寄信服務叫進來
    @Autowired
    private EmailService emailService;

    public User processPostLogin(String email, String name, String pictureUrl) {
        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    // 舊用戶：更新資訊
                    existingUser.setUsername(name); 
                    if (pictureUrl != null && !pictureUrl.isEmpty()) {
                        existingUser.setProfilePictureUrl(pictureUrl); 
                    }
                    existingUser.setLastLoginAt(java.time.LocalDateTime.now());
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // 新用戶：建立帳號
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setUsername(email); 
                    newUser.setCreatedAt(java.time.LocalDateTime.now());
                    newUser.setLastLoginAt(java.time.LocalDateTime.now());
                    
                    // 🌟 2. 鎖住帳號：預設改為 false，等他輸入正確的驗證碼才打開
                    newUser.setIsActive(false);

                    if (pictureUrl != null && !pictureUrl.isEmpty()) {
                        newUser.setProfilePictureUrl(pictureUrl);
                    } else {
                        newUser.setProfilePictureUrl("https://api.dicebear.com/9.x/avataaars/svg?seed=" + email);
                    }

                    Roles userRole = roleRepository.findByRoleName("ROLE_USER")
                            .orElseThrow(() -> new RuntimeException("找不到預設角色"));
                    newUser.setRole(userRole);
                    
                    // 🌟 3. 產生 6 位數密碼，並存進資料庫
                    String code = String.format("%06d", new Random().nextInt(999999));
                    newUser.setVerificationCode(code); // 把這 6 個數字記在資料庫裡

                    // 先把這個尚未開通的帳號存進資料庫
                    User savedUser = userRepository.save(newUser);
                    
                    // 🌟 4. 呼叫郵差！把驗證碼寄到他的 Gmail
                    emailService.sendVerificationCode(email, code);

                    return savedUser;
                });
    }
}