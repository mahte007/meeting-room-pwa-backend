package com.mate.meeting_room_reservation.service.impl;

import com.mate.meeting_room_reservation.dto.reservation.ReservationDTO;
import com.mate.meeting_room_reservation.dto.reservation.SaveReservationDTO;
import com.mate.meeting_room_reservation.dto.reservation.UpdateReservationStatusDTO;
import com.mate.meeting_room_reservation.entity.AppUser;
import com.mate.meeting_room_reservation.entity.Employee;
import com.mate.meeting_room_reservation.entity.Reservation;
import com.mate.meeting_room_reservation.entity.ReservationStatus;
import com.mate.meeting_room_reservation.entity.Room;
import com.mate.meeting_room_reservation.exception.BadRequestException;
import com.mate.meeting_room_reservation.exception.ResourceNotFoundException;
import com.mate.meeting_room_reservation.mapper.ReservationMapperImpl;
import com.mate.meeting_room_reservation.repository.EmployeeRepository;
import com.mate.meeting_room_reservation.repository.ReservationRepository;
import com.mate.meeting_room_reservation.repository.RoomRepository;
import com.mate.meeting_room_reservation.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.mate.meeting_room_reservation.support.TestData.admin;
import static com.mate.meeting_room_reservation.support.TestData.employee;
import static com.mate.meeting_room_reservation.support.TestData.employeeUser;
import static com.mate.meeting_room_reservation.support.TestData.reservation;
import static com.mate.meeting_room_reservation.support.TestData.room;
import static com.mate.meeting_room_reservation.support.TestData.tomorrowAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    private static final LocalDateTime START = tomorrowAt(10);
    private static final LocalDateTime END = tomorrowAt(11);

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private CurrentUserService currentUserService;

    private ReservationServiceImpl reservationService;

    private Room room;
    private Employee alice;
    private Employee bob;
    private AppUser adminUser;
    private AppUser aliceUser;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationServiceImpl(
                reservationRepository, employeeRepository, roomRepository,
                new ReservationMapperImpl(), currentUserService);

        room = room(10L, 6);
        alice = employee(1L);
        bob = employee(2L);
        adminUser = admin();
        aliceUser = employeeUser(alice);
    }

    private void loginAs(AppUser user) {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(currentUserService.isAdmin(user)).thenReturn(user == adminUser);
    }

    private void stubSaveReturnsArgument() {
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubNoOverlapOnCreate() {
        when(reservationRepository.findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                room.getId(), ReservationStatus.CANCELLED, END, START)).thenReturn(List.of());
    }

    private static SaveReservationDTO dto(LocalDateTime start, LocalDateTime end, int attendees, Long employeeId, Long roomId) {
        return new SaveReservationDTO("Sprint planning", "Weekly planning", start, end, attendees, employeeId, roomId);
    }

    @Nested
    class CreateReservation {

        @Test
        void adminBooksForChosenEmployee() {
            loginAs(adminUser);
            when(employeeRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            stubNoOverlapOnCreate();
            stubSaveReturnsArgument();

            ReservationDTO result = reservationService.createReservation(dto(START, END, 4, 2L, 10L));

            assertThat(result.employeeId()).isEqualTo(2L);
            assertThat(result.roomId()).isEqualTo(10L);
            assertThat(result.status()).isEqualTo("PLANNED");
            assertThat(result.archived()).isFalse();
            assertThat(result.title()).isEqualTo("Sprint planning");
            assertThat(result.startTime()).isEqualTo(START);
            assertThat(result.endTime()).isEqualTo(END);
            assertThat(result.attendeeCount()).isEqualTo(4);
        }

        @Test
        void employeeAlwaysBooksForThemselves_requestedEmployeeIsIgnored() {
            loginAs(aliceUser);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            stubNoOverlapOnCreate();
            stubSaveReturnsArgument();

            ReservationDTO result = reservationService.createReservation(dto(START, END, 4, bob.getId(), 10L));

            assertThat(result.employeeId()).isEqualTo(alice.getId());
            verify(employeeRepository, never()).findById(anyLong());
        }

        @Test
        void attendeeCountEqualToCapacityIsAllowed() {
            loginAs(aliceUser);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            stubNoOverlapOnCreate();
            stubSaveReturnsArgument();

            ReservationDTO result = reservationService.createReservation(dto(START, END, room.getCapacity(), null, 10L));

            assertThat(result.attendeeCount()).isEqualTo(room.getCapacity());
        }

        @Test
        void rejectsStartAfterEnd() {
            assertThatThrownBy(() -> reservationService.createReservation(dto(END, START, 2, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Start time must be before end time.");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        void rejectsZeroLengthReservation() {
            assertThatThrownBy(() -> reservationService.createReservation(dto(START, START, 2, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Start time must be before end time.");
        }

        @Test
        void rejectsStartInThePast() {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

            assertThatThrownBy(() -> reservationService.createReservation(dto(yesterday, yesterday.plusHours(1), 2, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Reservation cannot start in the past.");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        void adminMustChooseAnEmployee() {
            loginAs(adminUser);

            assertThatThrownBy(() -> reservationService.createReservation(dto(START, END, 2, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Employee is required.");
        }

        @Test
        void adminChoosingUnknownEmployeeFails() {
            loginAs(adminUser);
            when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.createReservation(dto(START, END, 2, 99L, 10L)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Employee not found.");
        }

        @Test
        void inactiveEmployeeCannotBook() {
            alice.setActive(false);
            loginAs(aliceUser);

            assertThatThrownBy(() -> reservationService.createReservation(dto(START, END, 2, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Employee is not active.");
        }

        @Test
        void accountWithoutLinkedEmployeeIsDenied() {
            AppUser unlinked = employeeUser(alice);
            unlinked.setEmployee(null);
            loginAs(unlinked);

            assertThatThrownBy(() -> reservationService.createReservation(dto(START, END, 2, null, 10L)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Your account is not linked to an employee.");
        }

        @Test
        void unknownRoomFails() {
            loginAs(aliceUser);
            when(roomRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.createReservation(dto(START, END, 2, null, 10L)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Room not found.");
        }

        @Test
        void inactiveRoomCannotBeBooked() {
            room.setActive(false);
            loginAs(aliceUser);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

            assertThatThrownBy(() -> reservationService.createReservation(dto(START, END, 2, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Room is not active.");
        }

        @Test
        void rejectsMoreAttendeesThanCapacity() {
            loginAs(aliceUser);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

            assertThatThrownBy(() -> reservationService.createReservation(dto(START, END, room.getCapacity() + 1, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Attendee count exceeds room capacity.");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        void rejectsOverlappingReservation() {
            loginAs(aliceUser);
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            Reservation existing = reservation(50L, bob, room, START.minusMinutes(30), START.plusMinutes(30));
            when(reservationRepository.findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                    room.getId(), ReservationStatus.CANCELLED, END, START)).thenReturn(List.of(existing));

            assertThatThrownBy(() -> reservationService.createReservation(dto(START, END, 2, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Room is already reserved in this time range.");
            verify(reservationRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateReservation {

        private Reservation existing;

        @BeforeEach
        void setUp() {
            existing = reservation(5L, alice, room, START, END);
        }

        private void stubValidUpdate() {
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            when(reservationRepository.findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                    any(), any(), any(), any(), any())).thenReturn(List.of());
            stubSaveReturnsArgument();
        }

        @Test
        void ownerCanUpdateTheirReservation() {
            loginAs(aliceUser);
            stubValidUpdate();

            ReservationDTO result = reservationService.updateReservation(5L, dto(START.plusHours(2), END.plusHours(2), 3, null, 10L));

            assertThat(result.startTime()).isEqualTo(START.plusHours(2));
            assertThat(result.endTime()).isEqualTo(END.plusHours(2));
            assertThat(result.attendeeCount()).isEqualTo(3);
            assertThat(result.title()).isEqualTo("Sprint planning");
        }

        @Test
        void overlapCheckExcludesTheReservationItself() {
            loginAs(aliceUser);
            stubValidUpdate();

            reservationService.updateReservation(5L, dto(START, END, 3, null, 10L));

            verify(reservationRepository).findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                    room.getId(), ReservationStatus.CANCELLED, END, START, 5L);
        }

        @Test
        void employeeCannotHandReservationToSomeoneElse() {
            loginAs(aliceUser);
            stubValidUpdate();

            ReservationDTO result = reservationService.updateReservation(5L, dto(START, END, 3, bob.getId(), 10L));

            assertThat(result.employeeId()).isEqualTo(alice.getId());
        }

        @Test
        void employeeCannotUpdateSomeoneElsesReservation() {
            existing.setEmployee(bob);
            loginAs(aliceUser);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> reservationService.updateReservation(5L, dto(START, END, 3, null, 10L)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You can only modify your own reservations.");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        void adminCanUpdateAnyReservationAndReassignIt() {
            loginAs(adminUser);
            stubValidUpdate();
            when(employeeRepository.findById(2L)).thenReturn(Optional.of(bob));

            ReservationDTO result = reservationService.updateReservation(5L, dto(START, END, 3, 2L, 10L));

            assertThat(result.employeeId()).isEqualTo(bob.getId());
        }

        @Test
        void unknownReservationFails() {
            when(reservationRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.updateReservation(5L, dto(START, END, 3, null, 10L)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Reservation not found.");
        }

        @Test
        void archivedReservationCannotBeModified() {
            existing.setArchived(true);
            loginAs(aliceUser);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> reservationService.updateReservation(5L, dto(START, END, 3, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Archived reservation cannot be modified.");
        }

        @ParameterizedTest
        @EnumSource(value = ReservationStatus.class, names = {"CANCELLED", "COMPLETED"})
        void finalReservationCannotBeModified(ReservationStatus status) {
            existing.setStatus(status);
            loginAs(aliceUser);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> reservationService.updateReservation(5L, dto(START, END, 3, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cancelled or completed reservation cannot be modified.");
        }

        @Test
        void ongoingReservationCanBeEditedIfStartTimeIsUnchanged() {
            LocalDateTime startedAnHourAgo = LocalDateTime.now().minusHours(1).withNano(0);
            existing.setStartTime(startedAnHourAgo);
            existing.setEndTime(startedAnHourAgo.plusHours(2));
            loginAs(aliceUser);
            stubValidUpdate();

            ReservationDTO result = reservationService.updateReservation(5L,
                    dto(startedAnHourAgo, startedAnHourAgo.plusHours(3), 3, null, 10L));

            assertThat(result.endTime()).isEqualTo(startedAnHourAgo.plusHours(3));
        }

        @Test
        void cannotMoveReservationIntoThePast() {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            loginAs(aliceUser);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> reservationService.updateReservation(5L, dto(yesterday, yesterday.plusHours(1), 3, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Reservation cannot start in the past.");
        }

        @Test
        void rejectsOverlapWithAnotherReservation() {
            loginAs(aliceUser);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
            when(reservationRepository.findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                    room.getId(), ReservationStatus.CANCELLED, END, START, 5L))
                    .thenReturn(List.of(reservation(6L, bob, room, START, END)));

            assertThatThrownBy(() -> reservationService.updateReservation(5L, dto(START, END, 3, null, 10L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Room is already reserved in this time range.");
            verify(reservationRepository, never()).save(any());
        }
    }

    @Nested
    class ChangeStatus {

        private Reservation existing;

        @BeforeEach
        void setUp() {
            existing = reservation(5L, alice, room, START, END);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "PLANNED,  APPROVED",
                "PLANNED,  CANCELLED",
                "APPROVED, CANCELLED",
                "APPROVED, COMPLETED"
        })
        void appliesAllowedTransition(ReservationStatus from, ReservationStatus to) {
            existing.setStatus(from);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));
            stubSaveReturnsArgument();

            ReservationDTO result = reservationService.changeStatus(5L, new UpdateReservationStatusDTO(to.name()));

            assertThat(result.status()).isEqualTo(to.name());
        }

        @Test
        void statusIsCaseInsensitive() {
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));
            stubSaveReturnsArgument();

            ReservationDTO result = reservationService.changeStatus(5L, new UpdateReservationStatusDTO("approved"));

            assertThat(result.status()).isEqualTo("APPROVED");
        }

        @Test
        void rejectsForbiddenTransition() {
            existing.setStatus(ReservationStatus.COMPLETED);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> reservationService.changeStatus(5L, new UpdateReservationStatusDTO("PLANNED")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Status cannot be changed from COMPLETED to PLANNED.");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        void rejectsUnknownStatus() {
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> reservationService.changeStatus(5L, new UpdateReservationStatusDTO("DONE")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Invalid reservation status.");
        }

        @Test
        void archivedReservationStatusCannotChange() {
            existing.setArchived(true);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> reservationService.changeStatus(5L, new UpdateReservationStatusDTO("APPROVED")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Archived reservation status cannot be modified.");
        }

        @Test
        void unknownReservationFails() {
            when(reservationRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.changeStatus(5L, new UpdateReservationStatusDTO("APPROVED")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class RestoreReservation {

        private Reservation archived;

        @BeforeEach
        void setUp() {
            archived = reservation(5L, alice, room, START, END);
            archived.setArchived(true);
        }

        @Test
        void restoresBlockingReservationWhenSlotIsStillFree() {
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(archived));
            when(reservationRepository.findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                    room.getId(), ReservationStatus.CANCELLED, END, START, 5L)).thenReturn(List.of());
            stubSaveReturnsArgument();

            ReservationDTO result = reservationService.restoreReservation(5L);

            assertThat(result.archived()).isFalse();
        }

        @Test
        void rejectsRestoreWhenSomeoneBookedTheSlotMeanwhile() {
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(archived));
            when(reservationRepository.findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                    room.getId(), ReservationStatus.CANCELLED, END, START, 5L))
                    .thenReturn(List.of(reservation(6L, bob, room, START, END)));

            assertThatThrownBy(() -> reservationService.restoreReservation(5L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Room is already reserved in this time range.");
            verify(reservationRepository, never()).save(any());
        }

        @Test
        void rejectsRestoreIntoInactiveRoom() {
            room.setActive(false);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(archived));

            assertThatThrownBy(() -> reservationService.restoreReservation(5L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Room is not active.");
        }

        @Test
        void rejectsRestoreForInactiveEmployee() {
            alice.setActive(false);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(archived));

            assertThatThrownBy(() -> reservationService.restoreReservation(5L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Employee is not active.");
        }

        @Test
        void nonBlockingReservationIsRestoredWithoutChecks() {
            archived.setStatus(ReservationStatus.CANCELLED);
            room.setActive(false);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(archived));
            stubSaveReturnsArgument();

            ReservationDTO result = reservationService.restoreReservation(5L);

            assertThat(result.archived()).isFalse();
            verify(reservationRepository, never())
                    .findByRoomIdAndArchivedFalseAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                            any(), any(), any(), any(), any());
        }

        @Test
        void rejectsReservationThatIsNotArchived() {
            archived.setArchived(false);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(archived));

            assertThatThrownBy(() -> reservationService.restoreReservation(5L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Reservation is not archived.");
        }
    }

    @Nested
    class DeleteReservation {

        private Reservation existing;

        @BeforeEach
        void setUp() {
            existing = reservation(5L, alice, room, START, END);
        }

        @Test
        void ownerArchivesTheirReservation() {
            loginAs(aliceUser);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));

            reservationService.deleteReservation(5L);

            ArgumentCaptor<Reservation> saved = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(saved.capture());
            assertThat(saved.getValue().getArchived()).isTrue();
            verify(reservationRepository, never()).delete(any());
        }

        @Test
        void adminCanArchiveAnyReservation() {
            existing.setEmployee(bob);
            loginAs(adminUser);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));

            reservationService.deleteReservation(5L);

            assertThat(existing.getArchived()).isTrue();
        }

        @Test
        void employeeCannotArchiveSomeoneElsesReservation() {
            existing.setEmployee(bob);
            loginAs(aliceUser);
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> reservationService.deleteReservation(5L))
                    .isInstanceOf(AccessDeniedException.class);
            assertThat(existing.getArchived()).isFalse();
            verify(reservationRepository, never()).save(any());
        }
    }

    @Nested
    class Listing {

        @Test
        void listByRoomFailsForUnknownRoom() {
            when(roomRepository.existsById(10L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.listReservationsByRoom(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Room not found.");
            verifyNoInteractions(reservationRepository);
        }

        @Test
        void listByRoomReturnsMappedReservations() {
            when(roomRepository.existsById(10L)).thenReturn(true);
            when(reservationRepository.findByRoomIdAndArchivedFalse(any(), any()))
                    .thenReturn(List.of(reservation(5L, alice, room, START, END)));

            List<ReservationDTO> result = reservationService.listReservationsByRoom(10L);

            assertThat(result).singleElement().satisfies(r -> {
                assertThat(r.id()).isEqualTo(5L);
                assertThat(r.roomName()).isEqualTo(room.getName());
                assertThat(r.employeeName()).isEqualTo(alice.getName());
            });
        }

        @Test
        void listByEmployeeFailsForUnknownEmployee() {
            when(employeeRepository.existsById(1L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.listReservationsByEmployee(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Employee not found.");
        }

        @Test
        void loadReservationFailsForUnknownId() {
            when(reservationRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.loadReservation(5L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Reservation not found.");
        }
    }
}
