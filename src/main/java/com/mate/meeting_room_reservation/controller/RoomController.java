package com.mate.meeting_room_reservation.controller;

import com.mate.meeting_room_reservation.dto.room.RoomDTO;
import com.mate.meeting_room_reservation.dto.room.SaveRoomDTO;
import com.mate.meeting_room_reservation.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public List<RoomDTO> listAllRooms() {
        return roomService.listAllRooms();
    }

    @GetMapping("/active")
    public List<RoomDTO> listActiveRooms() {
        return roomService.listActiveRooms();
    }

    @GetMapping("/{id}")
    public RoomDTO loadRoom(@PathVariable Long id) {
        return roomService.loadRoom(id);
    }

    @PostMapping
    public RoomDTO createRoom(@Valid @RequestBody SaveRoomDTO dto) {
        return roomService.createRoom(dto);
    }

    @PutMapping("/{id}")
    public RoomDTO updateRoom(@PathVariable Long id, @Valid @RequestBody SaveRoomDTO dto) {
        return roomService.updateRoom(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
    }

    @PatchMapping("/{id}/activate")
    public RoomDTO activateRoom(@PathVariable Long id) {
        return roomService.activateRoom(id);
    }

    @GetMapping("/available")
    public List<RoomDTO> listAvailableRooms(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {
        return roomService.listAvailableRooms(start, end);
    }
}
