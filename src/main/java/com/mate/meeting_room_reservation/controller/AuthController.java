package com.mate.meeting_room_reservation.controller;

import com.mate.meeting_room_reservation.dto.auth.LoginRequest;
import com.mate.meeting_room_reservation.dto.auth.LoginResponse;
import com.mate.meeting_room_reservation.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}