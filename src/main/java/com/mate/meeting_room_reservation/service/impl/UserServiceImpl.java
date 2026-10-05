package com.mate.meeting_room_reservation.service.impl;

import com.mate.meeting_room_reservation.dto.user.CreateUserDTO;
import com.mate.meeting_room_reservation.dto.user.UserDTO;
import com.mate.meeting_room_reservation.entity.AppUser;
import com.mate.meeting_room_reservation.entity.Employee;
import com.mate.meeting_room_reservation.entity.UserRole;
import com.mate.meeting_room_reservation.exception.BadRequestException;
import com.mate.meeting_room_reservation.exception.ResourceNotFoundException;
import com.mate.meeting_room_reservation.mapper.UserMapper;
import com.mate.meeting_room_reservation.repository.AppUserRepository;
import com.mate.meeting_room_reservation.repository.EmployeeRepository;
import com.mate.meeting_room_reservation.security.CurrentUserService;
import com.mate.meeting_room_reservation.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AppUserRepository appUserRepository;
    private final EmployeeRepository employeeRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    @Override
    public List<UserDTO> listUsers() {
        return appUserRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public UserDTO createUser(CreateUserDTO dto) {
        if (appUserRepository.existsByUsername(dto.username())) {
            throw new BadRequestException("Username already exists.");
        }

        if (dto.role() == UserRole.EMPLOYEE && dto.employeeId() == null) {
            throw new BadRequestException("Employee accounts must be linked to an employee.");
        }

        Employee employee = null;
        if (dto.employeeId() != null) {
            employee = employeeRepository.findById(dto.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

            if (!Boolean.TRUE.equals(employee.getActive())) {
                throw new BadRequestException("Employee is not active.");
            }

            if (appUserRepository.existsByEmployeeId(employee.getId())) {
                throw new BadRequestException("Employee already has a user account.");
            }
        }

        AppUser user = AppUser.builder()
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .role(dto.role())
                .employee(employee)
                .build();

        AppUser savedUser = appUserRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    public void deleteUser(Long id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (user.getUsername().equals(currentUserService.getUsername())) {
            throw new BadRequestException("You cannot delete your own account.");
        }

        appUserRepository.delete(user);
    }
}
