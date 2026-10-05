package com.mate.meeting_room_reservation.dto.user;

import com.mate.meeting_room_reservation.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
        @NotBlank(message = "Username is required.")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
        String username,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
        String password,

        @NotNull(message = "Role is required.")
        UserRole role,

        Long employeeId // required for EMPLOYEE accounts, optional for ADMIN accounts
) {}
