package com.mate.meeting_room_reservation.dto.user;

public record UserDTO(
        Long id,
        String username,
        String role,
        Long employeeId,
        String employeeName
) {}
