package com.mate.meeting_room_reservation.service.impl;

import com.mate.meeting_room_reservation.dto.auth.LoginRequest;
import com.mate.meeting_room_reservation.dto.auth.LoginResponse;
import com.mate.meeting_room_reservation.entity.AppUser;
import com.mate.meeting_room_reservation.repository.AppUserRepository;
import com.mate.meeting_room_reservation.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static com.mate.meeting_room_reservation.support.TestData.admin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_returnsTokenForValidCredentials() {
        AppUser adminUser = admin();
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(jwtService.generateToken(adminUser)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("admin", "admin123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.role()).isEqualTo("ADMIN");
        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("admin", "admin123"));
    }

    @Test
    void login_wrongPasswordIssuesNoToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(appUserRepository, jwtService);
    }
}
