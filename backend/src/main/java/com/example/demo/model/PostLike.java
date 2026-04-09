package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Post_Likes")
@Data
public class PostLike {
    @EmbeddedId
    private PostLikeKey id;

    @ManyToOne
    @MapsId("postId")
    @JoinColumn(name = "Post_Id")
    private MessageBoard messageBoard;
    
    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "userId")
    private User user;
}