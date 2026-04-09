package com.example.demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.example.demo.model.Trip;
import com.example.demo.requestDto.TripRequestDto;
import com.example.demo.responseDto.TripResponseDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { TripDayMapper.class })
public interface TripMapper {

    Trip toEntity(TripRequestDto dto);

    @Mapping(source = "tripDays", target = "tripDays")
    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? entity.getStatus(   ).name() : null)")
    TripResponseDto toDto(Trip entity);
}