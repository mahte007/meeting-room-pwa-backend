package com.mate.meeting_room_reservation.repository;

import com.mate.meeting_room_reservation.entity.Reservation;
import com.mate.meeting_room_reservation.entity.ReservationStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByArchivedFalse(Sort sort);

    List<Reservation> findByRoomIdAndArchivedFalse(Long roomId, Sort sort);

    List<Reservation> findByEmployeeIdAndArchivedFalse(Long employeeId, Sort sort);

    // Upcoming or ongoing reservations that still occupy a slot
    boolean existsByEmployeeIdAndArchivedFalseAndStatusInAndEndTimeAfter(
            Long employeeId,
            Collection<ReservationStatus> statuses,
            LocalDateTime now
    );

    boolean existsByRoomIdAndArchivedFalseAndStatusInAndEndTimeAfter(
            Long roomId,
            Collection<ReservationStatus> statuses,
            LocalDateTime now
    );

    List<Reservation> findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
            Long roomId,
            ReservationStatus status,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    List<Reservation> findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            Long RoomId,
            ReservationStatus status,
            LocalDateTime endTime,
            LocalDateTime startTime,
            Long id
    );
}
