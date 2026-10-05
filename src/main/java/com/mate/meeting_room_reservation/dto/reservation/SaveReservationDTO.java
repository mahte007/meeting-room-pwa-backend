package com.mate.meeting_room_reservation.dto.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SaveReservationDTO(
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotNull @Min(1) Integer attendeeCount,
        Long employeeId, // only used when an admin books on behalf of an employee
        @NotNull Long roomId
        ) {}
