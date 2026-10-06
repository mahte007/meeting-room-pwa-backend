package com.mate.meeting_room_reservation;

import com.jayway.jsonpath.JsonPath;
import com.mate.meeting_room_reservation.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReservationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private Long createAndReadId(MockHttpServletRequestBuilder request, String token, String json) throws Exception {
        String body = mockMvc.perform(request.header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String reservationJson(Long roomId, LocalDateTime start, LocalDateTime end, Long employeeId) {
        return """
                {"title":"Planning","startTime":"%s","endTime":"%s","attendeeCount":2,"roomId":%d,"employeeId":%s}"""
                .formatted(start, end, roomId, employeeId);
    }

    @Test
    void employeeBooksRoomConflictIsRejectedAndAdminApproves() throws Exception {
        String admin = login("admin", "admin123");
        String mate = login("mate", "mate123");
        Long roomId = createAndReadId(post("/api/rooms"), admin, """
                {"name":"Flow test room","capacity":6,"location":"Floor 3","hasProjector":false}""");
        LocalDateTime start = LocalDateTime.now().plusDays(10).truncatedTo(ChronoUnit.DAYS).withHour(10);

        Long reservationId = createAndReadId(post("/api/reservations"), mate,
                reservationJson(roomId, start, start.plusHours(1), null));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", mate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(roomId, start.plusMinutes(30), start.plusHours(2), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Room is already reserved in this time range."));

        mockMvc.perform(patch("/api/reservations/{id}/status", reservationId)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"APPROVED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/reservations/{id}", reservationId).header("Authorization", mate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.employeeName").value("Máté Horváth"));
    }

    @Test
    void deactivatedEmployeeLosesAccessImmediately() throws Exception {
        String admin = login("admin", "admin123");
        Long employeeId = createAndReadId(post("/api/employees"), admin, """
                {"name":"Temp Worker","email":"temp.worker@example.com","department":"Ops","role":"Intern"}""");
        createAndReadId(post("/api/users"), admin, """
                {"username":"tempworker","password":"temp12345","role":"EMPLOYEE","employeeId":%d}"""
                .formatted(employeeId));
        String temp = login("tempworker", "temp12345");

        mockMvc.perform(delete("/api/employees/{id}", employeeId).header("Authorization", admin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/rooms").header("Authorization", temp))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"tempworker","password":"temp12345"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is disabled."));
    }
}
