package com.mate.meeting_room_reservation.entity;

import java.util.Set;

public enum ReservationStatus {
    PLANNED,
    APPROVED,
    CANCELLED,
    COMPLETED;

    // Statuses that occupy the room's time slot
    public static final Set<ReservationStatus> BLOCKING = Set.of(PLANNED, APPROVED);

    public boolean canTransitionTo(ReservationStatus target) {
        return switch (this) {
            case PLANNED -> target == APPROVED || target == CANCELLED;
            case APPROVED -> target == CANCELLED || target == COMPLETED;
            case CANCELLED, COMPLETED -> false;
        };
    }

    public boolean isFinal() {
        return this == CANCELLED || this == COMPLETED;
    }
}
