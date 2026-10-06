package com.mate.meeting_room_reservation.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.mate.meeting_room_reservation.entity.ReservationStatus.APPROVED;
import static com.mate.meeting_room_reservation.entity.ReservationStatus.CANCELLED;
import static com.mate.meeting_room_reservation.entity.ReservationStatus.COMPLETED;
import static com.mate.meeting_room_reservation.entity.ReservationStatus.PLANNED;
import static org.assertj.core.api.Assertions.assertThat;

class ReservationStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed: {2}")
    @CsvSource({
            "PLANNED,   PLANNED,   false",
            "PLANNED,   APPROVED,  true",
            "PLANNED,   CANCELLED, true",
            "PLANNED,   COMPLETED, false",
            "APPROVED,  PLANNED,   false",
            "APPROVED,  APPROVED,  false",
            "APPROVED,  CANCELLED, true",
            "APPROVED,  COMPLETED, true",
            "CANCELLED, PLANNED,   false",
            "CANCELLED, APPROVED,  false",
            "CANCELLED, CANCELLED, false",
            "CANCELLED, COMPLETED, false",
            "COMPLETED, PLANNED,   false",
            "COMPLETED, APPROVED,  false",
            "COMPLETED, CANCELLED, false",
            "COMPLETED, COMPLETED, false"
    })
    void canTransitionTo_allowsOnlyDefinedTransitions(ReservationStatus from, ReservationStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void isFinal_onlyForCancelledAndCompleted() {
        assertThat(PLANNED.isFinal()).isFalse();
        assertThat(APPROVED.isFinal()).isFalse();
        assertThat(CANCELLED.isFinal()).isTrue();
        assertThat(COMPLETED.isFinal()).isTrue();
    }

    @Test
    void blocking_containsOnlyStatusesThatOccupyTheRoom() {
        assertThat(ReservationStatus.BLOCKING).containsExactlyInAnyOrder(PLANNED, APPROVED);
    }
}
