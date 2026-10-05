package com.mate.meeting_room_reservation.service.impl;

import com.mate.meeting_room_reservation.dto.reservation.ReservationDTO;
import com.mate.meeting_room_reservation.dto.reservation.SaveReservationDTO;
import com.mate.meeting_room_reservation.dto.reservation.UpdateReservationStatusDTO;
import com.mate.meeting_room_reservation.entity.*;
import com.mate.meeting_room_reservation.exception.BadRequestException;
import com.mate.meeting_room_reservation.exception.ResourceNotFoundException;
import com.mate.meeting_room_reservation.mapper.ReservationMapper;
import com.mate.meeting_room_reservation.repository.AppUserRepository;
import com.mate.meeting_room_reservation.repository.EmployeeRepository;
import com.mate.meeting_room_reservation.repository.ReservationRepository;
import com.mate.meeting_room_reservation.repository.RoomRepository;
import com.mate.meeting_room_reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final EmployeeRepository employeeRepository;
    private final RoomRepository roomRepository;
    private final ReservationMapper reservationMapper;
    private final AppUserRepository appUserRepository;

    @Override
    public List<ReservationDTO> listAllReservations() {
        return reservationRepository.findAll()
                .stream()
                .map(reservationMapper::toDto)
                .toList();
    }

    @Override
    public List<ReservationDTO> listActiveReservations() {
        return reservationRepository.findByArchivedFalse()
                .stream()
                .map(reservationMapper::toDto)
                .toList();
    }

    @Override
    public ReservationDTO loadReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found."));
        return reservationMapper.toDto(reservation);
    }

    @Override
    public ReservationDTO createReservation(SaveReservationDTO dto) {
        validateTimeRange(dto.startTime(), dto.endTime());

        Employee employee = employeeRepository.findById(dto.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new BadRequestException("Employee is not active.");
        }

        Room room = roomRepository.findById(dto.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));

        if (!Boolean.TRUE.equals(room.getActive())) {
            throw new BadRequestException("Room is not active.");
        }

        validateCapacity(dto.attendeeCount(), room);
        validateNoOverlapForCreate(room.getId(), dto.startTime(), dto.endTime());

        Reservation reservation = Reservation.builder()
                .title(dto.title())
                .description(dto.description())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .attendeeCount(dto.attendeeCount())
                .status(ReservationStatus.PLANNED)
                .archived(false)
                .employee(employee)
                .room(room)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        return reservationMapper.toDto(savedReservation);
    }

    @Override
    public ReservationDTO updateReservation(Long id, SaveReservationDTO dto) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found."));

        validateReservationOwnershipForEmployee(reservation);

        if (Boolean.TRUE.equals(reservation.getArchived())) {
            throw new BadRequestException("Archived reservation cannot be modified.");
        }

        validateTimeRange(dto.startTime(), dto.endTime());

        Employee employee = employeeRepository.findById(dto.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new BadRequestException("Employee is not active.");
        }

        Room room = roomRepository.findById(dto.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));

        if (!Boolean.TRUE.equals(room.getActive())) {
            throw new BadRequestException("Room is not active.");
        }

        validateCapacity(dto.attendeeCount(), room);
        validateNoOverlapForUpdate(room.getId(), dto.startTime(), dto.endTime(), id);

        reservation.setTitle(dto.title());
        reservation.setDescription(dto.description());
        reservation.setStartTime(dto.startTime());
        reservation.setEndTime(dto.endTime());
        reservation.setAttendeeCount(dto.attendeeCount());
        reservation.setEmployee(employee);
        reservation.setRoom(room);

        Reservation savedReservation = reservationRepository.save(reservation);
        return reservationMapper.toDto(savedReservation);
    }

    @Override
    public ReservationDTO changeStatus(Long id, UpdateReservationStatusDTO dto) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found."));

        if (Boolean.TRUE.equals(reservation.getArchived())) {
            throw new BadRequestException("Archived reservation status cannot be modified.");
        }

        ReservationStatus newStatus;
        try {
            newStatus = ReservationStatus.valueOf(dto.status().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid reservation status.");
        }

        reservation.setStatus(newStatus);

        Reservation savedReservation = reservationRepository.save(reservation);
        return reservationMapper.toDto(savedReservation);
    }

    @Override
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found."));

        validateReservationOwnershipForEmployee(reservation);

        reservation.setArchived(true);
        reservationRepository.save(reservation);
    }

    @Override
    public List<ReservationDTO> listReservationsByRoom(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("Room not found.");
        }

        return reservationRepository.findByRoomIdAndArchivedFalse(roomId)
                .stream()
                .map(reservationMapper::toDto)
                .toList();
    }

    @Override
    public List<ReservationDTO> listReservationsByEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found.");
        }

        return reservationRepository.findByEmployeeIdAndArchivedFalse(employeeId)
                .stream()
                .map(reservationMapper::toDto)
                .toList();
    }

    private void validateTimeRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new BadRequestException("Start time must be before end time.");
        }
    }

    private void validateCapacity(Integer attendeeCount, Room room) {
        if (attendeeCount > room.getCapacity()) {
            throw new BadRequestException("Attendee count exceeds room capacity.");
        }
    }

    private void validateNoOverlapForCreate(Long roomId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        boolean hasOverlap = !reservationRepository
                .findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                        roomId,
                        ReservationStatus.CANCELLED,
                        end,
                        start
                )
                .isEmpty();

        if (hasOverlap) {
            throw new BadRequestException("Room is already reserved in this time range.");
        }
    }

    private void validateNoOverlapForUpdate(Long roomId, java.time.LocalDateTime start, java.time.LocalDateTime end, Long reservationId) {
        boolean hasOverlap = !reservationRepository
                .findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        roomId,
                        ReservationStatus.CANCELLED,
                        end,
                        start,
                        reservationId
                )
                .isEmpty();

        if (hasOverlap) {
            throw new BadRequestException("Room is already reserved in this time range.");
        }
    }

    private void validateReservationOwnershipForEmployee(Reservation reservation) {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        AppUser currentUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

        if (currentUser.getRole() == UserRole.ADMIN) {
            return;
        }

        if (currentUser.getEmployee() == null) {
            throw new BadRequestException("Authenticated user is not linked to an employee.");
        }

        if (!reservation.getEmployee().getId().equals(currentUser.getEmployee().getId())) {
            throw new BadRequestException("You can only modify your own reservations.");
        }
    }
}