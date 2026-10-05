package com.mate.meeting_room_reservation.dto.user;

import com.mate.meeting_room_reservation.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull UserRole role,
        Long employeeId // required for EMPLOYEE accounts, optional for ADMIN accounts
) {}
