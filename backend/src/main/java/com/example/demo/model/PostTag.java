package com.example.demo.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 標籤關係表
 */
@Entity
@Table(name = "Post_Tag")
@Data
public class PostTag {

    @EmbeddedId
    private PostTagKey id;

    @ManyToOne
    @MapsId("postId") // 這會去 PostTagKey 找 postId 屬性
    @JoinColumn(name = "Post_Id")
    private MessageBoard messageBoard;

    @ManyToOne
    @MapsId("tagId") // 這會去 PostTagKey 找 tagId 屬性
    @JoinColumn(name = "Tag_Id")
    private TagName tagName;
}