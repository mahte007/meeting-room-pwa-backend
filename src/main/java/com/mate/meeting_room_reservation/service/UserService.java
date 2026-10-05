package com.mate.meeting_room_reservation.service;

import com.mate.meeting_room_reservation.dto.user.ChangePasswordDTO;
import com.mate.meeting_room_reservation.dto.user.CreateUserDTO;
import com.mate.meeting_room_reservation.dto.user.ResetPasswordDTO;
import com.mate.meeting_room_reservation.dto.user.UpdateUserDTO;
import com.mate.meeting_room_reservation.dto.user.UserDTO;

import java.util.List;

public interface UserService {

    List<UserDTO> listUsers();

    UserDTO createUser(CreateUserDTO dto);

    UserDTO updateUser(Long id, UpdateUserDTO dto);

    void resetPassword(Long id, ResetPasswordDTO dto);

    void deleteUser(Long id);

    UserDTO loadCurrentUser();

    void changeOwnPassword(ChangePasswordDTO dto);
}
