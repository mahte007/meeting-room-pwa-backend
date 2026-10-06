package com.mate.meeting_room_reservation.repository;

import com.mate.meeting_room_reservation.entity.Employee;
import com.mate.meeting_room_reservation.entity.Reservation;
import com.mate.meeting_room_reservation.entity.ReservationStatus;
import com.mate.meeting_room_reservation.entity.Room;
import com.mate.meeting_room_reservation.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static com.mate.meeting_room_reservation.support.TestData.employee;
import static com.mate.meeting_room_reservation.support.TestData.reservation;
import static com.mate.meeting_room_reservation.support.TestData.room;
import static com.mate.meeting_room_reservation.support.TestData.tomorrowAt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    private Room room;
    private Employee employee;

    @BeforeEach
    void setUp() {
        room = room(null, 6);
        room.setName("Repository test room");
        room = roomRepository.save(room);
        employee = employee(null);
        employee.setEmail("repository-test@example.com");
        employee = employeeRepository.save(employee);
    }

    private Reservation save(LocalDateTime start, LocalDateTime end, ReservationStatus status, boolean archived) {
        Reservation reservation = reservation(null, employee, room, start, end);
        reservation.setStatus(status);
        reservation.setArchived(archived);
        return reservationRepository.save(reservation);
    }

    private List<Reservation> overlapping(LocalDateTime start, LocalDateTime end) {
        return reservationRepository.findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                room.getId(), ReservationStatus.CANCELLED, end, start);
    }

    @Test
    void overlapQueryFindsIntersectingRangesButNotAdjacentOnes() {
        Reservation existing = save(tomorrowAt(10), tomorrowAt(11), ReservationStatus.APPROVED, false);

        assertThat(overlapping(tomorrowAt(11), tomorrowAt(12))).isEmpty();
        assertThat(overlapping(tomorrowAt(9), tomorrowAt(10))).isEmpty();
        assertThat(overlapping(tomorrowAt(10).plusMinutes(30), tomorrowAt(11).plusMinutes(30))).containsExactly(existing);
        assertThat(overlapping(tomorrowAt(9), tomorrowAt(12))).containsExactly(existing);
        assertThat(overlapping(tomorrowAt(10).plusMinutes(15), tomorrowAt(10).plusMinutes(45))).containsExactly(existing);
    }

    @Test
    void overlapQueryIgnoresCancelledAndArchivedReservations() {
        save(tomorrowAt(10), tomorrowAt(11), ReservationStatus.CANCELLED, false);
        save(tomorrowAt(10), tomorrowAt(11), ReservationStatus.PLANNED, true);

        assertThat(overlapping(tomorrowAt(10), tomorrowAt(11))).isEmpty();
    }

    @Test
    void overlapQueryForUpdateExcludesTheReservationItself() {
        Reservation existing = save(tomorrowAt(10), tomorrowAt(11), ReservationStatus.PLANNED, false);

        assertThat(reservationRepository
                .findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        room.getId(), ReservationStatus.CANCELLED, tomorrowAt(11), tomorrowAt(10), existing.getId()))
                .isEmpty();
    }

    @Test
    void onlyUpcomingBlockingReservationsPreventDeactivation() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        save(yesterday, yesterday.plusHours(1), ReservationStatus.APPROVED, false);
        save(tomorrowAt(10), tomorrowAt(11), ReservationStatus.CANCELLED, false);

        assertThat(reservationRepository.existsByRoomIdAndArchivedFalseAndStatusInAndEndTimeAfter(
                room.getId(), ReservationStatus.BLOCKING, LocalDateTime.now())).isFalse();

        save(tomorrowAt(12), tomorrowAt(13), ReservationStatus.PLANNED, false);

        assertThat(reservationRepository.existsByRoomIdAndArchivedFalseAndStatusInAndEndTimeAfter(
                room.getId(), ReservationStatus.BLOCKING, LocalDateTime.now())).isTrue();
    }
}
