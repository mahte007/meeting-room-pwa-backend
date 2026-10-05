package com.mate.meeting_room_reservation.service;

import com.mate.meeting_room_reservation.dto.reservation.ReservationDTO;
import com.mate.meeting_room_reservation.dto.reservation.SaveReservationDTO;
import com.mate.meeting_room_reservation.dto.reservation.UpdateReservationStatusDTO;

import java.util.List;

public interface ReservationService {

    List<ReservationDTO> listAllReservations();

    List<ReservationDTO> listActiveReservations();

    ReservationDTO loadReservation(Long id);

    ReservationDTO createReservation(SaveReservationDTO dto);

    ReservationDTO updateReservation(Long id, SaveReservationDTO dto);

    ReservationDTO changeStatus(Long id, UpdateReservationStatusDTO dto);

    void deleteReservation(Long id);

    ReservationDTO restoreReservation(Long id);

    List<ReservationDTO> listReservationsByRoom(Long roomId);

    List<ReservationDTO> listReservationsByEmployee(Long employeeId);
}