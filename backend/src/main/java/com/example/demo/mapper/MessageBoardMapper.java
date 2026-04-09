package com.example.demo.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.example.demo.model.MessageBoard;
import com.example.demo.requestDto.MessageBoardCreateDto;
import com.example.demo.responseDto.MessageBoardResponseDto;

@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CommentMapper.class} // 叫 MapStruct 去看 CommentMapper 怎麼轉留言
)
public interface MessageBoardMapper {

    MessageBoard toEntity(MessageBoardCreateDto dto);

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.profilePictureUrl", target = "profilePictureUrl")
    // MapStruct 會自動比對 MessageBoard 裡的 comments 
    // 並呼叫 CommentMapper.toDto 來轉成 MessageBoardResponseDto 裡的 comments
    @Mapping(source = "sharedPost", target = "sharedPost", qualifiedByName = "toSharedPostDto")
    MessageBoardResponseDto toDto(MessageBoard entity);

    List<MessageBoardResponseDto> toDtoList(List<MessageBoard> entities);

    @Named("toSharedPostDto")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.profilePictureUrl", target = "profilePictureUrl")
    @Mapping(target = "comments", ignore = true) // 預覽時不顯示留言
    @Mapping(target = "sharedPost", ignore = true) // 避免巢狀分享 (只顯示一層)
    MessageBoardResponseDto toSharedPostDto(MessageBoard entity);
}