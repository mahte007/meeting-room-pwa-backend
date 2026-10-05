package com.mate.meeting_room_reservation.dto.auth;

public record LoginResponse(
        String token,
        String username,
        String role
) {}
