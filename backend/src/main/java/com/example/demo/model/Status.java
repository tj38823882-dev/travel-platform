package com.example.demo.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "Status")
public class Status {
    @Id
    private Integer statusId;

    private String status; // 例如：可見、隱藏、刪除

    @OneToMany(mappedBy = "status")
    @JsonIgnore
    private List<MessageBoard> posts;
}