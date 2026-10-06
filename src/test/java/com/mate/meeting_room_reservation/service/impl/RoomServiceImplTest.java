package com.mate.meeting_room_reservation.service.impl;

import com.mate.meeting_room_reservation.dto.room.RoomDTO;
import com.mate.meeting_room_reservation.dto.room.SaveRoomDTO;
import com.mate.meeting_room_reservation.entity.Reservation;
import com.mate.meeting_room_reservation.entity.ReservationStatus;
import com.mate.meeting_room_reservation.entity.Room;
import com.mate.meeting_room_reservation.exception.BadRequestException;
import com.mate.meeting_room_reservation.exception.ResourceNotFoundException;
import com.mate.meeting_room_reservation.mapper.RoomMapperImpl;
import com.mate.meeting_room_reservation.repository.ReservationRepository;
import com.mate.meeting_room_reservation.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.mate.meeting_room_reservation.support.TestData.employee;
import static com.mate.meeting_room_reservation.support.TestData.reservation;
import static com.mate.meeting_room_reservation.support.TestData.room;
import static com.mate.meeting_room_reservation.support.TestData.tomorrowAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ReservationRepository reservationRepository;

    private RoomServiceImpl roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomServiceImpl(roomRepository, reservationRepository, new RoomMapperImpl());
    }

    private void stubSaveReturnsArgument() {
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static SaveRoomDTO dto(String name) {
        return new SaveRoomDTO(name, 8, "Floor 2", true);
    }

    @Test
    void createRoom_newRoomIsActive() {
        when(roomRepository.existsByNameIgnoreCase("Orion")).thenReturn(false);
        stubSaveReturnsArgument();

        RoomDTO result = roomService.createRoom(dto("Orion"));

        ArgumentCaptor<Room> saved = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
        assertThat(saved.getValue().getActive()).isTrue();
        assertThat(result.name()).isEqualTo("Orion");
        assertThat(result.capacity()).isEqualTo(8);
    }

    @Test
    void createRoom_rejectsDuplicateName() {
        when(roomRepository.existsByNameIgnoreCase("Orion")).thenReturn(true);

        assertThatThrownBy(() -> roomService.createRoom(dto("Orion")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Room name already exists.");
        verify(roomRepository, never()).save(any());
    }

    @Test
    void updateRoom_appliesChanges() {
        Room room = room(10L, 4);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomRepository.existsByNameIgnoreCaseAndIdNot("Orion", 10L)).thenReturn(false);
        stubSaveReturnsArgument();

        RoomDTO result = roomService.updateRoom(10L, dto("Orion"));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Orion");
        assertThat(result.capacity()).isEqualTo(8);
        assertThat(result.hasProjector()).isTrue();
    }

    @Test
    void updateRoom_rejectsNameUsedByAnotherRoom() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room(10L, 4)));
        when(roomRepository.existsByNameIgnoreCaseAndIdNot("Orion", 10L)).thenReturn(true);

        assertThatThrownBy(() -> roomService.updateRoom(10L, dto("Orion")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Room name already exists.");
    }

    @Test
    void updateRoom_failsForUnknownRoom() {
        when(roomRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.updateRoom(10L, dto("Orion")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Room not found.");
    }

    @Test
    void deleteRoom_deactivatesInsteadOfDeleting() {
        Room room = room(10L, 4);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsByRoomIdAndArchivedFalseAndStatusInAndEndTimeAfter(
                eq(10L), eq(ReservationStatus.BLOCKING), any(LocalDateTime.class))).thenReturn(false);

        roomService.deleteRoom(10L);

        assertThat(room.getActive()).isFalse();
        verify(roomRepository).save(room);
        verify(roomRepository, never()).delete(any());
    }

    @Test
    void deleteRoom_blockedByUpcomingReservations() {
        Room room = room(10L, 4);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsByRoomIdAndArchivedFalseAndStatusInAndEndTimeAfter(
                eq(10L), eq(ReservationStatus.BLOCKING), any(LocalDateTime.class))).thenReturn(true);

        assertThatThrownBy(() -> roomService.deleteRoom(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Room cannot be deactivated because it has upcoming reservations.");
        assertThat(room.getActive()).isTrue();
    }

    @Test
    void activateRoom_reactivatesRoom() {
        Room room = room(10L, 4);
        room.setActive(false);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        stubSaveReturnsArgument();

        RoomDTO result = roomService.activateRoom(10L);

        assertThat(result.active()).isTrue();
    }

    @Test
    void listAvailableRooms_excludesRoomsWithOverlappingReservations() {
        LocalDateTime start = tomorrowAt(10);
        LocalDateTime end = tomorrowAt(12);
        Room free = room(1L, 4);
        Room busy = room(2L, 4);
        Reservation overlapping = reservation(5L, employee(1L), busy, start, end);
        when(roomRepository.findByActiveTrue(any())).thenReturn(List.of(free, busy));
        when(reservationRepository.findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                1L, ReservationStatus.CANCELLED, end, start)).thenReturn(List.of());
        when(reservationRepository.findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                2L, ReservationStatus.CANCELLED, end, start)).thenReturn(List.of(overlapping));

        List<RoomDTO> result = roomService.listAvailableRooms(start, end);

        assertThat(result).extracting(RoomDTO::id).containsExactly(1L);
    }

    @Test
    void listAvailableRooms_requiresBothTimes() {
        assertThatThrownBy(() -> roomService.listAvailableRooms(null, tomorrowAt(10)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Start and end time are required.");
        assertThatThrownBy(() -> roomService.listAvailableRooms(tomorrowAt(10), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Start and end time are required.");
        verifyNoInteractions(roomRepository, reservationRepository);
    }

    @Test
    void listAvailableRooms_rejectsInvertedRange() {
        assertThatThrownBy(() -> roomService.listAvailableRooms(tomorrowAt(12), tomorrowAt(10)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Start time must be before end time.");
    }
}
