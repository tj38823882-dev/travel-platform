package com.example.demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.example.demo.model.Stop;
import com.example.demo.requestDto.StopRequestDto;
import com.example.demo.responseDto.StopResponseDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StopMapper {

    Stop toEntity(StopRequestDto dto);

    StopResponseDto toDto(Stop entity);

    // 用來更新既有的 Stop entity，忽略不應該被 dto 覆蓋的欄位
    @Mapping(target = "tripDay", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(StopRequestDto dto, @MappingTarget Stop stop);
}

// toEntity 用在 create，所有欄位正常對應；updateEntity 用在 update，加上 ignore = true
// 保護那些不該被覆蓋的欄位。兩個方法針對不同情境，@Mapping 的 ignore 只對標注的那個方法生效，不會互相影響。
