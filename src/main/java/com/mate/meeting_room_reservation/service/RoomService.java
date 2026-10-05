package com.mate.meeting_room_reservation.service;

import com.mate.meeting_room_reservation.dto.room.RoomDTO;
import com.mate.meeting_room_reservation.dto.room.SaveRoomDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomService {

    List<RoomDTO> listAllRooms();

    List<RoomDTO> listActiveRooms();

    RoomDTO loadRoom(Long id);

    RoomDTO createRoom(SaveRoomDTO dto);

    RoomDTO updateRoom(Long id, SaveRoomDTO dto);

    void deleteRoom(Long id);

    RoomDTO activateRoom(Long id);

    List<RoomDTO> listAvailableRooms(LocalDateTime start, LocalDateTime end);
}
