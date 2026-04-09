package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;

    private LocalDate startDate;

    private LocalDate endDate;

    private String coverImage;
    // 在資料庫 是 varchar(MAX)

    @Column(columnDefinition = "VARCHAR(500)")
    private String hashTag;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(10)")
    private Status status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "trip")
    private List<TripDay> tripDays;

    public enum Status {
        active, deleted, published
    }
}
