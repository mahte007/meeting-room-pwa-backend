package com.mate.meeting_room_reservation.controller;

import com.mate.meeting_room_reservation.config.SecurityConfig;
import com.mate.meeting_room_reservation.security.CustomUserDetailsService;
import com.mate.meeting_room_reservation.security.JwtService;
import com.mate.meeting_room_reservation.security.SecurityErrorHandler;
import com.mate.meeting_room_reservation.service.AuthService;
import com.mate.meeting_room_reservation.service.EmployeeService;
import com.mate.meeting_room_reservation.service.ReservationService;
import com.mate.meeting_room_reservation.service.RoomService;
import com.mate.meeting_room_reservation.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.mate.meeting_room_reservation.support.TestData.admin;
import static com.mate.meeting_room_reservation.support.TestData.employee;
import static com.mate.meeting_room_reservation.support.TestData.employeeUser;
import static org.mockito.Mockito.when;

@WebMvcTest
@Import({SecurityConfig.class, SecurityErrorHandler.class, JwtService.class})
abstract class WebLayerTestBase {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    protected CustomUserDetailsService userDetailsService;
    @MockitoBean
    protected AuthService authService;
    @MockitoBean
    protected RoomService roomService;
    @MockitoBean
    protected EmployeeService employeeService;
    @MockitoBean
    protected ReservationService reservationService;
    @MockitoBean
    protected UserService userService;

    protected String adminToken;
    protected String employeeToken;

    @BeforeEach
    void setUpUsers() {
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(
                User.withUsername("admin").password("x").roles("ADMIN").build());
        when(userDetailsService.loadUserByUsername("user1")).thenReturn(
                User.withUsername("user1").password("x").roles("EMPLOYEE").build());

        adminToken = "Bearer " + jwtService.generateToken(admin());
        employeeToken = "Bearer " + jwtService.generateToken(employeeUser(employee(1L)));
    }
}
