package com.mate.meeting_room_reservation.security;

import com.mate.meeting_room_reservation.entity.AppUser;
import com.mate.meeting_room_reservation.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static com.mate.meeting_room_reservation.support.TestData.admin;
import static com.mate.meeting_room_reservation.support.TestData.employee;
import static com.mate.meeting_room_reservation.support.TestData.employeeUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    @BeforeEach
    void authenticateAsAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", null, "ROLE_ADMIN"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsUsernameFromSecurityContext() {
        assertThat(currentUserService.getUsername()).isEqualTo("admin");
    }

    @Test
    void loadsAuthenticatedUser() {
        AppUser adminUser = admin();
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        assertThat(currentUserService.getCurrentUser()).isSameAs(adminUser);
    }

    @Test
    void deniesAccessWhenAuthenticatedUserNoLongerExists() {
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authenticated user not found.");
    }

    @Test
    void isAdminChecksRole() {
        assertThat(currentUserService.isAdmin(admin())).isTrue();
        assertThat(currentUserService.isAdmin(employeeUser(employee(1L)))).isFalse();
    }
}
