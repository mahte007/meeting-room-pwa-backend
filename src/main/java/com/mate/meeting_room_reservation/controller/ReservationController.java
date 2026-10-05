package com.mate.meeting_room_reservation.controller;

import com.mate.meeting_room_reservation.dto.reservation.ReservationDTO;
import com.mate.meeting_room_reservation.dto.reservation.SaveReservationDTO;
import com.mate.meeting_room_reservation.dto.reservation.UpdateReservationStatusDTO;
import com.mate.meeting_room_reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public List<ReservationDTO> listAllReservations() {
        return reservationService.listAllReservations();
    }

    @GetMapping("/active")
    public List<ReservationDTO> listActiveReservations() {
        return reservationService.listActiveReservations();
    }

    @GetMapping("/{id}")
    public ReservationDTO loadReservation(@PathVariable Long id) {
        return reservationService.loadReservation(id);
    }

    @PostMapping
    public ReservationDTO createReservation(@Valid @RequestBody SaveReservationDTO dto) {
        return reservationService.createReservation(dto);
    }

    @PutMapping("/{id}")
    public ReservationDTO updateReservation(@PathVariable Long id, @Valid @RequestBody SaveReservationDTO dto) {
        return reservationService.updateReservation(id, dto);
    }

    @PatchMapping("/{id}/status")
    public ReservationDTO changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateReservationStatusDTO dto) {
        return reservationService.changeStatus(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
    }

    @PatchMapping("/{id}/restore")
    public ReservationDTO restoreReservation(@PathVariable Long id) {
        return reservationService.restoreReservation(id);
    }

    @GetMapping("/room/{roomId}")
    public List<ReservationDTO> listReservationsByRoom(@PathVariable Long roomId) {
        return reservationService.listReservationsByRoom(roomId);
    }

    @GetMapping("/employee/{employeeId}")
    public List<ReservationDTO> listReservationsByEmployee(@PathVariable Long employeeId) {
        return reservationService.listReservationsByEmployee(employeeId);
    }
}