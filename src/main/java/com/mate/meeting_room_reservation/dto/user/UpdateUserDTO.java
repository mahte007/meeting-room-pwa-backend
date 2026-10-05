package com.mate.meeting_room_reservation.dto.user;

import com.mate.meeting_room_reservation.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserDTO(
        @NotNull(message = "Role is required.") UserRole role,
        Long employeeId // required for EMPLOYEE accounts, optional for ADMIN accounts
) {}
