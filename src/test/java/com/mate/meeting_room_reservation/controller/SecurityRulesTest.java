package com.mate.meeting_room_reservation.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;

import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityRulesTest extends WebLayerTestBase {

    private static final String ROOM = """
            {"name":"Orion","capacity":8,"location":"Floor 2","hasProjector":true}""";
    private static final String RESERVATION = """
            {"title":"Planning","startTime":"2099-01-01T10:00:00","endTime":"2099-01-01T11:00:00",
             "attendeeCount":2,"roomId":1}""";
    private static final String STATUS = """
            {"status":"APPROVED"}""";

    static Stream<Arguments> accessRules() {
        return Stream.of(
                Arguments.of("EMPLOYEE", HttpMethod.GET, "/api/rooms", null, 200),
                Arguments.of("EMPLOYEE", HttpMethod.POST, "/api/rooms", ROOM, 403),
                Arguments.of("ADMIN", HttpMethod.POST, "/api/rooms", ROOM, 200),
                Arguments.of("EMPLOYEE", HttpMethod.GET, "/api/employees", null, 403),
                Arguments.of("ADMIN", HttpMethod.GET, "/api/employees", null, 200),
                Arguments.of("EMPLOYEE", HttpMethod.GET, "/api/users", null, 403),
                Arguments.of("ADMIN", HttpMethod.GET, "/api/users", null, 200),
                Arguments.of("EMPLOYEE", HttpMethod.GET, "/api/me", null, 200),
                Arguments.of("EMPLOYEE", HttpMethod.POST, "/api/reservations", RESERVATION, 200),
                Arguments.of("EMPLOYEE", HttpMethod.DELETE, "/api/reservations/1", null, 200),
                Arguments.of("EMPLOYEE", HttpMethod.PATCH, "/api/reservations/1/status", STATUS, 403),
                Arguments.of("ADMIN", HttpMethod.PATCH, "/api/reservations/1/status", STATUS, 200),
                Arguments.of("EMPLOYEE", HttpMethod.PATCH, "/api/reservations/1/restore", null, 403)
        );
    }

    @ParameterizedTest(name = "{0} {1} {2} -> {4}")
    @MethodSource("accessRules")
    void roleBasedAccess(String role, HttpMethod method, String url, String body, int expectedStatus) throws Exception {
        var request = request(method, url)
                .header("Authorization", role.equals("ADMIN") ? adminToken : employeeToken);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }

        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    @Test
    void missingTokenReturns401Json() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required."));
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/rooms").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatedUserWithValidTokenReturns401() throws Exception {
        when(userDetailsService.loadUserByUsername("user1")).thenReturn(
                User.withUsername("user1").password("x").roles("EMPLOYEE").disabled(true).build());

        mockMvc.perform(get("/api/rooms").header("Authorization", employeeToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forbiddenReturns403Json() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action."));
    }

    @Test
    void loginIsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}"""))
                .andExpect(status().isOk());
    }
}
