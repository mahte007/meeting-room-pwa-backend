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
import com.mate.meeting_room_reservation.mapper.UserMapperImpl;
import com.mate.meeting_room_reservation.repository.AppUserRepository;
import com.mate.meeting_room_reservation.repository.EmployeeRepository;
import com.mate.meeting_room_reservation.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.mate.meeting_room_reservation.support.TestData.admin;
import static com.mate.meeting_room_reservation.support.TestData.employee;
import static com.mate.meeting_room_reservation.support.TestData.employeeUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CurrentUserService currentUserService;

    private UserServiceImpl userService;

    private AppUser adminUser;
    private Employee alice;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                appUserRepository, employeeRepository, new UserMapperImpl(), passwordEncoder, currentUserService);
        adminUser = admin();
        alice = employee(1L);
    }

    private void stubSaveReturnsArgument() {
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createUser_employeeAccountIsLinkedAndPasswordHashed() {
        when(appUserRepository.existsByUsername("alice")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(appUserRepository.existsByEmployeeId(1L)).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        stubSaveReturnsArgument();

        UserDTO result = userService.createUser(new CreateUserDTO("alice", "secret123", UserRole.EMPLOYEE, 1L));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed");
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.role()).isEqualTo("EMPLOYEE");
        assertThat(result.employeeId()).isEqualTo(1L);
        assertThat(result.employeeName()).isEqualTo(alice.getName());
    }

    @Test
    void createUser_adminAccountNeedsNoEmployee() {
        when(appUserRepository.existsByUsername("boss")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        stubSaveReturnsArgument();

        UserDTO result = userService.createUser(new CreateUserDTO("boss", "secret123", UserRole.ADMIN, null));

        assertThat(result.employeeId()).isNull();
        assertThat(result.role()).isEqualTo("ADMIN");
    }

    @Test
    void createUser_rejectsDuplicateUsername() {
        when(appUserRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(new CreateUserDTO("alice", "secret123", UserRole.EMPLOYEE, 1L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username already exists.");
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void createUser_employeeAccountRequiresEmployee() {
        when(appUserRepository.existsByUsername("alice")).thenReturn(false);

        assertThatThrownBy(() -> userService.createUser(new CreateUserDTO("alice", "secret123", UserRole.EMPLOYEE, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Employee accounts must be linked to an employee.");
    }

    @Test
    void createUser_failsForUnknownEmployee() {
        when(appUserRepository.existsByUsername("alice")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(new CreateUserDTO("alice", "secret123", UserRole.EMPLOYEE, 1L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Employee not found.");
    }

    @Test
    void createUser_rejectsInactiveEmployee() {
        alice.setActive(false);
        when(appUserRepository.existsByUsername("alice")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> userService.createUser(new CreateUserDTO("alice", "secret123", UserRole.EMPLOYEE, 1L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Employee is not active.");
    }

    @Test
    void createUser_rejectsSecondAccountForSameEmployee() {
        when(appUserRepository.existsByUsername("alice2")).thenReturn(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(appUserRepository.existsByEmployeeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(new CreateUserDTO("alice2", "secret123", UserRole.EMPLOYEE, 1L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Employee already has a user account.");
    }

    @Test
    void updateUser_changesRoleAndLink() {
        AppUser aliceUser = employeeUser(alice);
        when(appUserRepository.findById(aliceUser.getId())).thenReturn(Optional.of(aliceUser));
        when(currentUserService.getUsername()).thenReturn("admin");
        stubSaveReturnsArgument();

        UserDTO result = userService.updateUser(aliceUser.getId(), new UpdateUserDTO(UserRole.ADMIN, null));

        assertThat(result.role()).isEqualTo("ADMIN");
        assertThat(result.employeeId()).isNull();
    }

    @Test
    void updateUser_keepingOwnEmployeeLinkIsNotAConflict() {
        AppUser aliceUser = employeeUser(alice);
        when(appUserRepository.findById(aliceUser.getId())).thenReturn(Optional.of(aliceUser));
        when(currentUserService.getUsername()).thenReturn("admin");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(appUserRepository.existsByEmployeeIdAndIdNot(1L, aliceUser.getId())).thenReturn(false);
        stubSaveReturnsArgument();

        UserDTO result = userService.updateUser(aliceUser.getId(), new UpdateUserDTO(UserRole.EMPLOYEE, 1L));

        assertThat(result.employeeId()).isEqualTo(1L);
        verify(appUserRepository, never()).existsByEmployeeId(any());
    }

    @Test
    void updateUser_adminCannotChangeOwnRole() {
        when(appUserRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(currentUserService.getUsername()).thenReturn("admin");

        assertThatThrownBy(() -> userService.updateUser(adminUser.getId(), new UpdateUserDTO(UserRole.EMPLOYEE, 1L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You cannot change your own role.");
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void updateUser_adminCanUpdateOwnAccountWithSameRole() {
        when(appUserRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(currentUserService.getUsername()).thenReturn("admin");
        stubSaveReturnsArgument();

        UserDTO result = userService.updateUser(adminUser.getId(), new UpdateUserDTO(UserRole.ADMIN, null));

        assertThat(result.role()).isEqualTo("ADMIN");
    }

    @Test
    void updateUser_failsForUnknownUser() {
        when(appUserRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(5L, new UpdateUserDTO(UserRole.ADMIN, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found.");
    }

    @Test
    void resetPassword_storesHashedPassword() {
        AppUser aliceUser = employeeUser(alice);
        when(appUserRepository.findById(aliceUser.getId())).thenReturn(Optional.of(aliceUser));
        when(passwordEncoder.encode("newSecret1")).thenReturn("hashed-new");

        userService.resetPassword(aliceUser.getId(), new ResetPasswordDTO("newSecret1"));

        assertThat(aliceUser.getPassword()).isEqualTo("hashed-new");
        verify(appUserRepository).save(aliceUser);
    }

    @Test
    void deleteUser_removesOtherAccount() {
        AppUser aliceUser = employeeUser(alice);
        when(appUserRepository.findById(aliceUser.getId())).thenReturn(Optional.of(aliceUser));
        when(currentUserService.getUsername()).thenReturn("admin");

        userService.deleteUser(aliceUser.getId());

        verify(appUserRepository).delete(aliceUser);
    }

    @Test
    void deleteUser_cannotDeleteOwnAccount() {
        when(appUserRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(currentUserService.getUsername()).thenReturn("admin");

        assertThatThrownBy(() -> userService.deleteUser(adminUser.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You cannot delete your own account.");
        verify(appUserRepository, never()).delete(any());
    }

    @Test
    void changeOwnPassword_requiresCorrectCurrentPassword() {
        AppUser aliceUser = employeeUser(alice);
        when(currentUserService.getCurrentUser()).thenReturn(aliceUser);
        when(passwordEncoder.matches("wrong", "encoded-user")).thenReturn(false);

        assertThatThrownBy(() -> userService.changeOwnPassword(new ChangePasswordDTO("wrong", "newSecret1")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current password is incorrect.");
        assertThat(aliceUser.getPassword()).isEqualTo("encoded-user");
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void changeOwnPassword_storesHashedNewPassword() {
        AppUser aliceUser = employeeUser(alice);
        when(currentUserService.getCurrentUser()).thenReturn(aliceUser);
        when(passwordEncoder.matches("current1", "encoded-user")).thenReturn(true);
        when(passwordEncoder.encode("newSecret1")).thenReturn("hashed-new");

        userService.changeOwnPassword(new ChangePasswordDTO("current1", "newSecret1"));

        assertThat(aliceUser.getPassword()).isEqualTo("hashed-new");
        verify(appUserRepository).save(aliceUser);
    }

    @Test
    void loadCurrentUser_returnsLoggedInUser() {
        when(currentUserService.getCurrentUser()).thenReturn(employeeUser(alice));

        UserDTO result = userService.loadCurrentUser();

        assertThat(result.username()).isEqualTo("user1");
        assertThat(result.employeeId()).isEqualTo(1L);
    }
}
