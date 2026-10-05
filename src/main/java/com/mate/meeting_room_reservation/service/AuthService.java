package com.mate.meeting_room_reservation.service;

import com.mate.meeting_room_reservation.dto.auth.LoginRequest;
import com.mate.meeting_room_reservation.dto.auth.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
