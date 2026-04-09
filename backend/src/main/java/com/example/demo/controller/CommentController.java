package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.requestDto.CommentCreateDto;
import com.example.demo.responseDto.CommentResponseDto;
import com.example.demo.service.CommentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments/post")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public CommentResponseDto createComment(@RequestBody CommentCreateDto dto) {
    return commentService.createComment(dto);

    }

    /**
     * 取得所有留言
     * 對應前端：request.get('/api/comments/post')
     */
    @GetMapping
    public List<CommentResponseDto> getAllComments() {
        return commentService.getPosts(null);
    }

}