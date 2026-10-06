package com.mate.meeting_room_reservation.controller;

import com.mate.meeting_room_reservation.exception.BadRequestException;
import com.mate.meeting_room_reservation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorResponseTest extends WebLayerTestBase {

    private static final String VALID_RESERVATION = """
            {"title":"Planning","startTime":"2099-01-01T10:00:00","endTime":"2099-01-01T11:00:00",
             "attendeeCount":2,"roomId":1}""";

    @Test
    void invalidBodyReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","startTime":"2099-01-01T10:00:00","endTime":"2099-01-01T11:00:00",
                                 "attendeeCount":0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.title").value("Title is required."))
                .andExpect(jsonPath("$.fields.attendeeCount").value("Attendee count must be at least 1."))
                .andExpect(jsonPath("$.fields.roomId").value("Room is required."));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body."));
    }

    @Test
    void invalidPathVariableReturns400() throws Exception {
        mockMvc.perform(get("/api/reservations/abc").header("Authorization", employeeToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'."));
    }

    @Test
    void businessRuleViolationReturns400() throws Exception {
        when(reservationService.createReservation(any()))
                .thenThrow(new BadRequestException("Room is already reserved in this time range."));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RESERVATION))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Room is already reserved in this time range."));
    }

    @Test
    void missingResourceReturns404() throws Exception {
        when(reservationService.loadReservation(99L)).thenThrow(new ResourceNotFoundException("Reservation not found."));

        mockMvc.perform(get("/api/reservations/99").header("Authorization", employeeToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Reservation not found."));
    }

    @Test
    void ownershipViolationReturns403() throws Exception {
        doThrow(new AccessDeniedException("You can only modify your own reservations."))
                .when(reservationService).deleteReservation(5L);

        mockMvc.perform(delete("/api/reservations/5").header("Authorization", employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only modify your own reservations."));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        when(roomService.listAllRooms()).thenThrow(new IllegalStateException("database password is hunter2"));

        mockMvc.perform(get("/api/rooms").header("Authorization", employeeToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected error occurred."));
    }
}
