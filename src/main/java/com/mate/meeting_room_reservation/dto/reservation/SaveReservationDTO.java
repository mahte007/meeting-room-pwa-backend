package com.mate.meeting_room_reservation.dto.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record SaveReservationDTO(
        @NotBlank(message = "Title is required.")
        @Size(max = 255, message = "Title must be at most 255 characters.")
        String title,

        @Size(max = 1000, message = "Description must be at most 1000 characters.")
        String description,

        @NotNull(message = "Start time is required.")
        LocalDateTime startTime,

        @NotNull(message = "End time is required.")
        LocalDateTime endTime,

        @NotNull(message = "Attendee count is required.")
        @Min(value = 1, message = "Attendee count must be at least 1.")
        Integer attendeeCount,

        Long employeeId, // only used when an admin books on behalf of an employee

        @NotNull(message = "Room is required.")
        Long roomId
) {}
