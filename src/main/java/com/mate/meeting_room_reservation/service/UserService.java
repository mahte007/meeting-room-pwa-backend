package com.mate.meeting_room_reservation.service;

import com.mate.meeting_room_reservation.dto.user.CreateUserDTO;
import com.mate.meeting_room_reservation.dto.user.UserDTO;

import java.util.List;

public interface UserService {

    List<UserDTO> listUsers();

    UserDTO createUser(CreateUserDTO dto);

    void deleteUser(Long id);
}
