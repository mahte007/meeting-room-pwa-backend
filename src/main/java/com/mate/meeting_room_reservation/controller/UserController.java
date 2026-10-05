package com.mate.meeting_room_reservation.controller;

import com.mate.meeting_room_reservation.dto.user.CreateUserDTO;
import com.mate.meeting_room_reservation.dto.user.ResetPasswordDTO;
import com.mate.meeting_room_reservation.dto.user.UpdateUserDTO;
import com.mate.meeting_room_reservation.dto.user.UserDTO;
import com.mate.meeting_room_reservation.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDTO> listUsers() {
        return userService.listUsers();
    }

    @PostMapping
    public UserDTO createUser(@Valid @RequestBody CreateUserDTO dto) {
        return userService.createUser(dto);
    }

    @PutMapping("/{id}")
    public UserDTO updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        return userService.updateUser(id, dto);
    }

    @PatchMapping("/{id}/password")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
