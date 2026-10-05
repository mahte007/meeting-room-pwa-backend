package com.mate.meeting_room_reservation.dto.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveRoomDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 255, message = "Name must be at most 255 characters.")
        String name,

        @NotNull(message = "Capacity is required.")
        @Min(value = 1, message = "Capacity must be at least 1.")
        Integer capacity,

        @NotBlank(message = "Location is required.")
        @Size(max = 255, message = "Location must be at most 255 characters.")
        String location,

        @NotNull(message = "Projector availability is required.")
        Boolean hasProjector
) {}
