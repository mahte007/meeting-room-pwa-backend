package com.mate.meeting_room_reservation.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveEmployeeDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 255, message = "Name must be at most 255 characters.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be a valid email address.")
        @Size(max = 255, message = "Email must be at most 255 characters.")
        String email,

        @NotBlank(message = "Department is required.")
        @Size(max = 255, message = "Department must be at most 255 characters.")
        String department,

        @NotBlank(message = "Role is required.")
        @Size(max = 255, message = "Role must be at most 255 characters.")
        String role
) {}
