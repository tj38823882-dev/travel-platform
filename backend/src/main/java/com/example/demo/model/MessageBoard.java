package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 留言板主表
 */
@Entity
@Data
@Table(name = "Message_Board")
public class MessageBoard {

    /**
     * 主鍵
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;
    /**
     * 外鍵 使用者ID
     */
    @ManyToOne(fetch = FetchType.LAZY) // 需要用到 User 資料時才去查詢
    @JoinColumn(name = "userId")   
    private User user;

    /**
     * 文章內容
     */
    @Column(columnDefinition = "TEXT")
    private String content;
    /**
     * 創建時間、更新時間
     */ 
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 限時文章的有效日期
     */
    private LocalDateTime expiryDate;
    /**
     * 按讚數
     */
    private Integer likesCount;

    /**
     * 留言數 (為了排序效能而新增的冗餘欄位)
     */
    private Integer commentsCount;

    // 關聯到 Status 表
    @ManyToOne
    @JoinColumn(name = "Status_Id")
    private Status status;

    // 關聯到自己或 Comments 
    private Long commentId;

    // 一對多關聯
    @OneToMany(mappedBy = "messageBoard")
    @JsonIgnore
    private List<PostTag> postTags;

    @OneToMany(mappedBy = "messageBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PostLike> postLikes;

    @OneToMany(mappedBy = "messageBoard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // mappedBy 指向 Comment 類別中定義的變數名稱 "messageBoard"
    private List<Comment> comments = new ArrayList<>();
    
    // 新增圖片網址欄位
    @Column(name = "ImageUrl") // 對應資料庫的欄位名稱
    private String imageUrl;

    // 新增：原始圖片網址欄位
    @Column(name = "OriginalImageUrl")
    private String originalImageUrl;

    // 新增分享貼文欄位 (自我關聯)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_post_id")
    private MessageBoard sharedPost;    

    // 新增：協作者欄位 (自我關聯)
    @ManyToOne
    @JoinColumn(name = "collaborator_id")
    private User collaborator;
}