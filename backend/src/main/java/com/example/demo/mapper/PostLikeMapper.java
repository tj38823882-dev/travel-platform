package com.example.demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.example.demo.model.PostLike;
import com.example.demo.requestDto.PostLikeDto;

@Mapper(componentModel = "spring" ,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostLikeMapper {

    @Mapping(source = "userId", target = "id.userId") 
    @Mapping(source = "postId", target = "id.postId") 
    @Mapping(source = "postId", target = "messageBoard.postId")
    PostLike toEntity(PostLikeDto dto);

    @Mapping(source = "id.postId", target = "postId")
    @Mapping(source = "id.userId", target = "userId")
    PostLikeDto toDto(PostLike entity);
}
