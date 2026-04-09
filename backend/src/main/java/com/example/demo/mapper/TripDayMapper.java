package com.example.demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.example.demo.model.TripDay;
import com.example.demo.requestDto.TripDayRequestDto;
import com.example.demo.responseDto.TripDayResponseDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { StopMapper.class })
public interface TripDayMapper {

    TripDay toEntity(TripDayRequestDto dto);

    @Mapping(source = "stops", target = "stops")
    TripDayResponseDto toDto(TripDay entity);

    @Mapping(target = "trip", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "stops", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(TripDayRequestDto dto, @MappingTarget TripDay tripDay);
}
