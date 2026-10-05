package com.mate.meeting_room_reservation.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordDTO(
        @NotBlank(message = "Current password is required.")
        String currentPassword,

        @NotBlank(message = "New password is required.")
        @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters.")
        String newPassword
) {}
