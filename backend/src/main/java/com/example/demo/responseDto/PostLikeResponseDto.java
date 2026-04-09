package com.example.demo.responseDto;

import lombok.Data;

@Data
public class PostLikeResponseDto {
    private Long postId;
    private Integer userId;
    private Integer likeCount;
    private Boolean isLiked;
}
