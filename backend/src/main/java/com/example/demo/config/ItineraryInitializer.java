package com.example.demo.config;

import com.example.demo.repository.ItineraryRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ItineraryInitializer {

        @Bean
        CommandLineRunner initItineraries(
        ItineraryRepository itineraryRepository,
        UserRepository userRepository) {
                return args -> {
                // 商品(Itinerary)現在需要關聯到真實的 Trip（trip_id NOT NULL）
                // 假資料已停用 — 上架商品改由使用者在個人頁面操作「上架」產生
                // 若資料庫舊有 itineraries 資料（沒有 trip_id），請先手動清除：
                // DELETE FROM itineraries;
                System.out.println("[ItineraryInitializer] 假資料初始化已停用，商品由使用者上架產生。");
                };
        }
}