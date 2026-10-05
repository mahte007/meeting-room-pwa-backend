package com.mate.meeting_room_reservation.controller;

import com.mate.meeting_room_reservation.dto.user.ChangePasswordDTO;
import com.mate.meeting_room_reservation.dto.user.UserDTO;
import com.mate.meeting_room_reservation.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    public UserDTO loadCurrentUser() {
        return userService.loadCurrentUser();
    }

    @PutMapping("/password")
    public void changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changeOwnPassword(dto);
    }
}
