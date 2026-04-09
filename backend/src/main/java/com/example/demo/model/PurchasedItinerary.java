package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchased_itineraries")
@Data
public class PurchasedItinerary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 購買者

    @ManyToOne
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary; // 被購買的行程

    @Column(name = "purchase_date", nullable = false, updatable = false)
    private LocalDateTime purchaseDate; // 購買日期

    @Column(nullable = false)
    private Integer price; // 購買時的價格

    @PrePersist
    protected void onCreate() {
        this.purchaseDate = LocalDateTime.now();
    }
}