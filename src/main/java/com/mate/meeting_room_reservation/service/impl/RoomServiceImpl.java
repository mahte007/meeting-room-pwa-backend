package com.mate.meeting_room_reservation.service.impl;

import com.mate.meeting_room_reservation.dto.room.RoomDTO;
import com.mate.meeting_room_reservation.dto.room.SaveRoomDTO;
import com.mate.meeting_room_reservation.entity.Reservation;
import com.mate.meeting_room_reservation.entity.ReservationStatus;
import com.mate.meeting_room_reservation.entity.Room;
import com.mate.meeting_room_reservation.exception.BadRequestException;
import com.mate.meeting_room_reservation.exception.ResourceNotFoundException;
import com.mate.meeting_room_reservation.mapper.RoomMapper;
import com.mate.meeting_room_reservation.repository.ReservationRepository;
import com.mate.meeting_room_reservation.repository.RoomRepository;
import com.mate.meeting_room_reservation.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private static final Sort BY_NAME = Sort.by("name");

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final RoomMapper roomMapper;

    @Override
    public List<RoomDTO> listAllRooms() {
        return roomRepository.findAll(BY_NAME)
                .stream()
                .map(roomMapper::toDto)
                .toList();
    }

    @Override
    public List<RoomDTO> listActiveRooms() {
        return roomRepository.findByActiveTrue(BY_NAME)
                .stream()
                .map(roomMapper::toDto)
                .toList();
    }

    @Override
    public RoomDTO loadRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
        return roomMapper.toDto(room);
    }

    @Override
    public RoomDTO createRoom(SaveRoomDTO dto) {
        if (roomRepository.existsByNameIgnoreCase(dto.name())) {
            throw new BadRequestException("Room name already exists.");
        }

        Room room = roomMapper.toEntity(dto);
        room.setId(null);
        room.setActive(true);

        Room savedRoom = roomRepository.save(room);
        return roomMapper.toDto(savedRoom);
    }

    @Override
    public RoomDTO updateRoom(Long id, SaveRoomDTO dto) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));

        if (roomRepository.existsByNameIgnoreCaseAndIdNot(dto.name(), id)) {
            throw new BadRequestException("Room name already exists.");
        }

        roomMapper.updateEntityFromDto(dto, room);

        Room savedRoom = roomRepository.save(room);
        return roomMapper.toDto(savedRoom);
    }

    @Override
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));

        if (reservationRepository.existsByRoomIdAndArchivedFalseAndStatusInAndEndTimeAfter(
                id, ReservationStatus.BLOCKING, LocalDateTime.now())) {
            throw new BadRequestException("Room cannot be deactivated because it has upcoming reservations.");
        }

        room.setActive(false);
        roomRepository.save(room);
    }

    @Override
    public RoomDTO activateRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));

        room.setActive(true);

        Room savedRoom = roomRepository.save(room);
        return roomMapper.toDto(savedRoom);
    }

    @Override
    public List<RoomDTO> listAvailableRooms(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BadRequestException("Start and end time are required.");
        }

        if (!start.isBefore(end)) {
            throw new BadRequestException("Start time must be before end time.");
        }

        List<Room> activeRooms = roomRepository.findByActiveTrue(BY_NAME);

        return activeRooms.stream()
                .filter(room -> {
                    List<Reservation> overlappingReservations =
                            reservationRepository
                                    .findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                                            room.getId(),
                                            ReservationStatus.CANCELLED,
                                            end,
                                            start
                                    );

                    return overlappingReservations.isEmpty();
                })
                .map(roomMapper::toDto)
                .toList();
    }
}
