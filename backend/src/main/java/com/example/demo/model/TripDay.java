package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.hibernate.annotations.BatchSize;

@Data
@Entity
public class TripDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer dayNumber;

    private LocalDate theDate;

    private String title;

    private LocalTime startTime;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(10)")
    private Status status;

    public enum Status {
        active, deleted
    }

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @BatchSize(size = 30)
    @OneToMany(mappedBy = "tripDay", fetch = FetchType.LAZY)
    private List<Stop> stops;
    // 這邊用batchSize 是為了在 TripDayController 裡面 getTripDayWithStops 時，能一次把 stops
    // 全部撈出來，避免 N+1 問題；不過如果是 getTripDaysByTripId 就不會用到 stops，就不會有 N+1 問題，所以就不需要
    // batchSize。

}
