package com.mate.meeting_room_reservation.dto.reservation;

import jakarta.validation.constraints.NotBlank;

public record UpdateReservationStatusDTO(
        @NotBlank(message = "Status is required.") String status
) {}
