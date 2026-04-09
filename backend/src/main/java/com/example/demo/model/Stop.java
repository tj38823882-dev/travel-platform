package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class Stop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String placeId;

    private Double lat;

    private Double lng;

    private String address;

    private Integer stayMinutes;

    private Integer travelMinutes;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(10)")
    private TravelMode travelMode;

    public enum TravelMode {
        WALKING, DRIVING, TRANSIT, CUSTOM
    }

    private String note;

    // // Google Places 取得的第一張圖片 URL，用來在列表/卡片顯示縮圖
    // @Column(columnDefinition = "VARCHAR(1000)")
    // private String photoUrl;

    private LocalDateTime createdAt;

    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(10)")
    private Status status;

    public enum Status {
        active, deleted
    }

    @ManyToOne
    @JoinColumn(name = "trip_day_id")
    private TripDay tripDay;

}
