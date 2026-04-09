package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.demo.model.PointPackage;
import com.example.demo.repository.PointPackageRepository;
import java.util.Arrays;
import java.util.List;

@Configuration
public class PointPackageInitializer {

    @Bean
    CommandLineRunner initPointPackages(PointPackageRepository pointPackageRepository) {
        return args -> {
            if (pointPackageRepository.count() == 0) {
                List<PointPackage> packages = Arrays.asList(
                        createPackage(100, 150),
                        createPackage(200, 300),
                        createPackage(300, 450),
                        createPackage(400, 600),
                        createPackage(500, 750),
                        createPackage(1000, 1500));
                pointPackageRepository.saveAll(packages);
                System.out.println("Point packages initialized.");
            }
        };
    }

    private PointPackage createPackage(int points, int price) {
        PointPackage pkg = new PointPackage();
        pkg.setPointsAmount(points);
        pkg.setPriceCash(price);
        pkg.setIsActive(true);
        return pkg;
    }
}
