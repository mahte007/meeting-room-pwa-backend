package com.mate.meeting_room_reservation.config;

import com.mate.meeting_room_reservation.entity.*;
import com.mate.meeting_room_reservation.repository.AppUserRepository;
import com.mate.meeting_room_reservation.repository.EmployeeRepository;
import com.mate.meeting_room_reservation.repository.ReservationRepository;
import com.mate.meeting_room_reservation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedDemoData();
        // Runs separately so databases created before user accounts existed still get logins
        seedUsers();
    }

    private void seedUsers() {
        if (appUserRepository.count() > 0) {
            return;
        }

        appUserRepository.save(AppUser.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .employee(null)
                .build());

        seedEmployeeUser("mate", "mate123", "mate@example.com");
        seedEmployeeUser("anna", "anna123", "anna@example.com");
    }

    private void seedEmployeeUser(String username, String password, String employeeEmail) {
        employeeRepository.findByEmail(employeeEmail).ifPresent(employee ->
                appUserRepository.save(AppUser.builder()
                        .username(username)
                        .password(passwordEncoder.encode(password))
                        .role(UserRole.EMPLOYEE)
                        .employee(employee)
                        .build()));
    }

    private void seedDemoData() {
        if (employeeRepository.count() > 0 || roomRepository.count() > 0 || reservationRepository.count() > 0) {
            return;
        }

        Employee mate = employeeRepository.save(Employee.builder()
                .name("Máté Horváth")
                .email("mate@example.com")
                .department("Engineering")
                .role("Developer")
                .active(true)
                .build());

        Employee anna = employeeRepository.save(Employee.builder()
                .name("Anna Kovács")
                .email("anna@example.com")
                .department("HR")
                .role("HR Manager")
                .active(true)
                .build());

        Employee peter = employeeRepository.save(Employee.builder()
                .name("Péter Nagy")
                .email("peter@example.com")
                .department("Sales")
                .role("Sales Specialist")
                .active(true)
                .build());

        Room roomA = roomRepository.save(Room.builder()
                .name("Room A")
                .capacity(8)
                .location("First Floor")
                .hasProjector(true)
                .active(true)
                .build());

        Room roomB = roomRepository.save(Room.builder()
                .name("Room B")
                .capacity(4)
                .location("Second Floor")
                .hasProjector(false)
                .active(true)
                .build());

        Room conferenceRoom = roomRepository.save(Room.builder()
                .name("Conference Room")
                .capacity(20)
                .location("Ground Floor")
                .hasProjector(true)
                .active(true)
                .build());

        reservationRepository.save(Reservation.builder()
                .title("Sprint Planning")
                .description("Weekly planning meeting")
                .startTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0))
                .endTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0))
                .attendeeCount(5)
                .status(ReservationStatus.APPROVED)
                .archived(false)
                .employee(mate)
                .room(roomA)
                .build());

        reservationRepository.save(Reservation.builder()
                .title("HR Interview")
                .description("Interview with a new candidate")
                .startTime(LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0))
                .endTime(LocalDateTime.now().plusDays(2).withHour(15).withMinute(0).withSecond(0).withNano(0))
                .attendeeCount(3)
                .status(ReservationStatus.PLANNED)
                .archived(false)
                .employee(anna)
                .room(roomB)
                .build());

        reservationRepository.save(Reservation.builder()
                .title("Sales Strategy Meeting")
                .description("Quarterly sales planning")
                .startTime(LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0))
                .endTime(LocalDateTime.now().plusDays(3).withHour(10).withMinute(30).withSecond(0).withNano(0))
                .attendeeCount(12)
                .status(ReservationStatus.PLANNED)
                .archived(false)
                .employee(peter)
                .room(conferenceRoom)
                .build());
    }
}