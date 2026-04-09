package com.example.demo.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.example.demo.model.Comment;
import com.example.demo.requestDto.CommentCreateDto;
import com.example.demo.responseDto.CommentResponseDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    Comment toEntity(CommentCreateDto dto);

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.profilePictureUrl", target = "profilePictureUrl")
    CommentResponseDto toDto(Comment entity);

    // 讓 MapStruct 自動把 List<Entity> 轉成 List<DTO>
    List<CommentResponseDto> toDtoList(List<Comment> entities);
}
