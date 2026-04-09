package com.example.demo.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor  
@AllArgsConstructor 
public class PostLikeKey implements Serializable {
    @Column(name = "Post_Id") 
    private Long postId;

    @Column(name = "userId")  
    private Integer userId;
}