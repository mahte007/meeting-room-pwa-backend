package com.mate.meeting_room_reservation.mapper;

import com.mate.meeting_room_reservation.dto.user.UserDTO;
import com.mate.meeting_room_reservation.entity.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", source = "employee.name")
    UserDTO toDto(AppUser user);
}
