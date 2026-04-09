package com.example.demo.model;

import java.io.Serializable;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PostTagKey implements Serializable {
    // 檢查這裡！名稱必須是 postId，不能是 post_id 或 PostId
    private Long postId; 
    private Long tagId;
}