package com.mate.meeting_room_reservation.support;

import com.mate.meeting_room_reservation.entity.AppUser;
import com.mate.meeting_room_reservation.entity.Employee;
import com.mate.meeting_room_reservation.entity.Reservation;
import com.mate.meeting_room_reservation.entity.ReservationStatus;
import com.mate.meeting_room_reservation.entity.Room;
import com.mate.meeting_room_reservation.entity.UserRole;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class TestData {

    private TestData() {
    }

    public static LocalDateTime tomorrowAt(int hour) {
        return LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS).withHour(hour);
    }

    public static Room room(Long id, int capacity) {
        return Room.builder()
                .id(id)
                .name("Room " + id)
                .capacity(capacity)
                .location("Floor 1")
                .hasProjector(false)
                .active(true)
                .build();
    }

    public static Employee employee(Long id) {
        return Employee.builder()
                .id(id)
                .name("Employee " + id)
                .email("employee" + id + "@example.com")
                .department("IT")
                .role("Developer")
                .active(true)
                .build();
    }

    public static AppUser admin() {
        return AppUser.builder()
                .id(1L)
                .username("admin")
                .password("encoded-admin")
                .role(UserRole.ADMIN)
                .build();
    }

    public static AppUser employeeUser(Employee employee) {
        return AppUser.builder()
                .id(100L + employee.getId())
                .username("user" + employee.getId())
                .password("encoded-user")
                .role(UserRole.EMPLOYEE)
                .employee(employee)
                .build();
    }

    public static Reservation reservation(Long id, Employee employee, Room room,
                                          LocalDateTime start, LocalDateTime end) {
        return Reservation.builder()
                .id(id)
                .title("Meeting " + id)
                .description("Description")
                .startTime(start)
                .endTime(end)
                .attendeeCount(2)
                .status(ReservationStatus.PLANNED)
                .archived(false)
                .employee(employee)
                .room(room)
                .build();
    }
}
