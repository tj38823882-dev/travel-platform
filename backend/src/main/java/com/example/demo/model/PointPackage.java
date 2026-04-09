package com.example.demo.model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "PointPackages")
public class PointPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PackageID")
    private Integer id;

    @Column(name = "PointsAmount", nullable = false)
    private Integer pointsAmount; // 提供點數 (>0)

    @Column(name = "PriceCash", nullable = false)
    private Integer priceCash; // 現金售價 (>0)

    @Column(name = "IsActive")
    private Boolean isActive = true;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
