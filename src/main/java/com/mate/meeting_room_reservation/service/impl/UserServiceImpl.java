package com.mate.meeting_room_reservation.service.impl;

import com.mate.meeting_room_reservation.dto.user.ChangePasswordDTO;
import com.mate.meeting_room_reservation.dto.user.CreateUserDTO;
import com.mate.meeting_room_reservation.dto.user.ResetPasswordDTO;
import com.mate.meeting_room_reservation.dto.user.UpdateUserDTO;
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
import org.springframework.data.domain.Sort;
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
        return appUserRepository.findAll(Sort.by("username"))
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public UserDTO createUser(CreateUserDTO dto) {
        if (appUserRepository.existsByUsername(dto.username())) {
            throw new BadRequestException("Username already exists.");
        }

        Employee employee = resolveLinkedEmployee(dto.role(), dto.employeeId(), null);

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
    public UserDTO updateUser(Long id, UpdateUserDTO dto) {
        AppUser user = findUser(id);

        // Prevents admins from locking themselves out of the admin pages
        if (isCurrentUser(user) && dto.role() != user.getRole()) {
            throw new BadRequestException("You cannot change your own role.");
        }

        user.setRole(dto.role());
        user.setEmployee(resolveLinkedEmployee(dto.role(), dto.employeeId(), user.getId()));

        AppUser savedUser = appUserRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    public void resetPassword(Long id, ResetPasswordDTO dto) {
        AppUser user = findUser(id);

        user.setPassword(passwordEncoder.encode(dto.password()));
        appUserRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        AppUser user = findUser(id);

        if (isCurrentUser(user)) {
            throw new BadRequestException("You cannot delete your own account.");
        }

        appUserRepository.delete(user);
    }

    @Override
    public UserDTO loadCurrentUser() {
        return userMapper.toDto(currentUserService.getCurrentUser());
    }

    @Override
    public void changeOwnPassword(ChangePasswordDTO dto) {
        AppUser user = currentUserService.getCurrentUser();

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        appUserRepository.save(user);
    }

    private AppUser findUser(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private boolean isCurrentUser(AppUser user) {
        return user.getUsername().equals(currentUserService.getUsername());
    }

    // userId is the account being updated (null when creating), so it doesn't count as a conflicting link
    private Employee resolveLinkedEmployee(UserRole role, Long employeeId, Long userId) {
        if (role == UserRole.EMPLOYEE && employeeId == null) {
            throw new BadRequestException("Employee accounts must be linked to an employee.");
        }

        if (employeeId == null) {
            return null;
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new BadRequestException("Employee is not active.");
        }

        boolean linkedElsewhere = userId == null
                ? appUserRepository.existsByEmployeeId(employeeId)
                : appUserRepository.existsByEmployeeIdAndIdNot(employeeId, userId);

        if (linkedElsewhere) {
            throw new BadRequestException("Employee already has a user account.");
        }

        return employee;
    }
}
