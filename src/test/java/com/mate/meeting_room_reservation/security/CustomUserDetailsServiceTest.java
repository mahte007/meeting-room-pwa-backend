package com.mate.meeting_room_reservation.security;

import com.mate.meeting_room_reservation.entity.AppUser;
import com.mate.meeting_room_reservation.entity.Employee;
import com.mate.meeting_room_reservation.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static com.mate.meeting_room_reservation.support.TestData.admin;
import static com.mate.meeting_room_reservation.support.TestData.employee;
import static com.mate.meeting_room_reservation.support.TestData.employeeUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void activeEmployeeIsEnabledWithEmployeeRole() {
        AppUser user = employeeUser(employee(1L));
        when(appUserRepository.findByUsername("user1")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("user1");

        assertThat(details.getUsername()).isEqualTo("user1");
        assertThat(details.getPassword()).isEqualTo("encoded-user");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void deactivatedEmployeeCannotLogIn() {
        Employee employee = employee(1L);
        employee.setActive(false);
        when(appUserRepository.findByUsername("user1")).thenReturn(Optional.of(employeeUser(employee)));

        UserDetails details = userDetailsService.loadUserByUsername("user1");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void adminWithoutEmployeeIsEnabled() {
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(admin()));

        UserDetails details = userDetailsService.loadUserByUsername("admin");

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    void unknownUserIsRejected() {
        when(appUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
