package com.mate.meeting_room_reservation.service.impl;

import com.mate.meeting_room_reservation.dto.employee.EmployeeDTO;
import com.mate.meeting_room_reservation.dto.employee.SaveEmployeeDTO;
import com.mate.meeting_room_reservation.entity.Employee;
import com.mate.meeting_room_reservation.entity.ReservationStatus;
import com.mate.meeting_room_reservation.exception.BadRequestException;
import com.mate.meeting_room_reservation.exception.ResourceNotFoundException;
import com.mate.meeting_room_reservation.mapper.EmployeeMapperImpl;
import com.mate.meeting_room_reservation.repository.EmployeeRepository;
import com.mate.meeting_room_reservation.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.mate.meeting_room_reservation.support.TestData.employee;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ReservationRepository reservationRepository;

    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository, reservationRepository, new EmployeeMapperImpl());
    }

    private void stubSaveReturnsArgument() {
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static SaveEmployeeDTO dto(String email) {
        return new SaveEmployeeDTO("Jane Doe", email, "Sales", "Manager");
    }

    @Test
    void createEmployee_newEmployeeIsActive() {
        when(employeeRepository.existsByEmail("jane@example.com")).thenReturn(false);
        stubSaveReturnsArgument();

        EmployeeDTO result = employeeService.createEmployee(dto("jane@example.com"));

        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
        assertThat(saved.getValue().getActive()).isTrue();
        assertThat(result.email()).isEqualTo("jane@example.com");
        assertThat(result.department()).isEqualTo("Sales");
    }

    @Test
    void createEmployee_rejectsDuplicateEmail() {
        when(employeeRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(dto("jane@example.com")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email already exists.");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateEmployee_keepingOwnEmailIsAllowed() {
        Employee jane = employee(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(jane));
        when(employeeRepository.findByEmail(jane.getEmail())).thenReturn(Optional.of(jane));
        stubSaveReturnsArgument();

        EmployeeDTO result = employeeService.updateEmployee(1L, dto(jane.getEmail()));

        assertThat(result.name()).isEqualTo("Jane Doe");
    }

    @Test
    void updateEmployee_rejectsEmailOfAnotherEmployee() {
        Employee other = employee(2L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee(1L)));
        when(employeeRepository.findByEmail(other.getEmail())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> employeeService.updateEmployee(1L, dto(other.getEmail())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email already exists.");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateEmployee_failsForUnknownEmployee() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployee(1L, dto("jane@example.com")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Employee not found.");
    }

    @Test
    void deleteEmployee_deactivatesInsteadOfDeleting() {
        Employee jane = employee(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(jane));
        when(reservationRepository.existsByEmployeeIdAndArchivedFalseAndStatusInAndEndTimeAfter(
                eq(1L), eq(ReservationStatus.BLOCKING), any(LocalDateTime.class))).thenReturn(false);

        employeeService.deleteEmployee(1L);

        assertThat(jane.getActive()).isFalse();
        verify(employeeRepository).save(jane);
        verify(employeeRepository, never()).delete(any());
    }

    @Test
    void deleteEmployee_blockedByUpcomingReservations() {
        Employee jane = employee(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(jane));
        when(reservationRepository.existsByEmployeeIdAndArchivedFalseAndStatusInAndEndTimeAfter(
                eq(1L), eq(ReservationStatus.BLOCKING), any(LocalDateTime.class))).thenReturn(true);

        assertThatThrownBy(() -> employeeService.deleteEmployee(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Employee cannot be deactivated because they have upcoming reservations.");
        assertThat(jane.getActive()).isTrue();
    }

    @Test
    void activateEmployee_reactivatesEmployee() {
        Employee jane = employee(1L);
        jane.setActive(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(jane));
        stubSaveReturnsArgument();

        EmployeeDTO result = employeeService.activateEmployee(1L);

        assertThat(result.active()).isTrue();
    }

    @Test
    void loadEmployee_failsForUnknownId() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.loadEmployee(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
