package com.example.demo.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor; // 這是 Lombok，幫我們寫建構子
import lombok.extern.slf4j.Slf4j; // 這是 Lombok，幫我們寫日誌 log

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final Cloudinary cloudinary;

    /**
     * 第一部分：上傳
     */
    public String uploadImage(MultipartFile file) throws IOException {
        // 1. 執行上傳動作
        // cloudinary.uploader().upload() 會回傳一個 Map
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("resource_type", "auto"));

        // 2. 從 Map 中把 "secure_url" 拿出來
        String url = uploadResult.get("secure_url").toString();

        log.info("圖片上傳成功！網址為: {}", url);
        return url;
    }

    /**
     * 第二部分：刪除 (當你要更新行程換照片時，這很重要)
     */
    public void deleteFileFromCloudinary(String url) {
        try {
            String publicId = extractPublicId(url);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary 檔案已刪除: {}", publicId);
        } catch (Exception e) {
            log.error("刪除失敗: {}", e.getMessage());
        }
    }

    /**
     * 第三部分：私有工具方法 (解析 URL)
     */
    private String extractPublicId(String url) {
        // 目標：從網址中切出檔案 ID
        try {
            // 範例：.../upload/v1234/my_folder/photo.jpg -> my_folder/photo
            String part = url.split("/upload/")[1];
            String pathWithExt = part.substring(part.indexOf("/") + 1);
            return pathWithExt.substring(0, pathWithExt.lastIndexOf("."));
        } catch (Exception e) {
            return ""; // 解析失敗回傳空值
        }
    }
}