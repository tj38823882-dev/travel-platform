package com.example.demo.controller;

import com.example.demo.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/public/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("檔案不能為空");
        }

        try {
            // 使用現有的 ImageUploadService 上傳到 Cloudinary
            String imageUrl = imageUploadService.uploadImage(file);
            return ResponseEntity.ok(imageUrl);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("上傳失敗: " + e.getMessage());
        }
    }
}
