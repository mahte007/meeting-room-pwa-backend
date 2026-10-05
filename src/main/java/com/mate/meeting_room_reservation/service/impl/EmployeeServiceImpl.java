package com.mate.meeting_room_reservation.service.impl;

import com.mate.meeting_room_reservation.dto.employee.EmployeeDTO;
import com.mate.meeting_room_reservation.dto.employee.SaveEmployeeDTO;
import com.mate.meeting_room_reservation.entity.Employee;
import com.mate.meeting_room_reservation.entity.ReservationStatus;
import com.mate.meeting_room_reservation.exception.BadRequestException;
import com.mate.meeting_room_reservation.exception.ResourceNotFoundException;
import com.mate.meeting_room_reservation.mapper.EmployeeMapper;
import com.mate.meeting_room_reservation.repository.EmployeeRepository;
import com.mate.meeting_room_reservation.repository.ReservationRepository;
import com.mate.meeting_room_reservation.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final Sort BY_NAME = Sort.by("name");

    private final EmployeeRepository employeeRepository;
    private final ReservationRepository reservationRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    public List<EmployeeDTO> listAllEmployees() {
        return employeeRepository.findAll(BY_NAME)
                .stream()
                .map(employeeMapper::toDto)
                .toList();
    }

    @Override
    public List<EmployeeDTO> listActiveEmployees() {
        return employeeRepository.findByActiveTrue(BY_NAME)
                .stream()
                .map(employeeMapper::toDto)
                .toList();
    }

    @Override
    public EmployeeDTO loadEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));
        return employeeMapper.toDto(employee);
    }

    @Override
    public EmployeeDTO createEmployee(SaveEmployeeDTO dto) {
        if (employeeRepository.existsByEmail(dto.email())) {
            throw new BadRequestException("Email already exists.");
        }

        Employee employee = employeeMapper.toEntity(dto);
        employee.setId(null);
        employee.setActive(true);

        Employee savedEmployee = employeeRepository.save(employee);
        return employeeMapper.toDto(savedEmployee);
    }

    @Override
    public EmployeeDTO updateEmployee(Long id, SaveEmployeeDTO dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

        employeeRepository.findByEmail(dto.email())
                .ifPresent(existingEmployee -> {
                    if (!existingEmployee.getId().equals(id)) {
                        throw new BadRequestException("Email already exists.");
                    }
                });

        employeeMapper.updateEntityFromDto(dto, employee);

        Employee savedEmployee = employeeRepository.save(employee);
        return employeeMapper.toDto(savedEmployee);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

        if (reservationRepository.existsByEmployeeIdAndArchivedFalseAndStatusInAndEndTimeAfter(
                id, ReservationStatus.BLOCKING, LocalDateTime.now())) {
            throw new BadRequestException("Employee cannot be deactivated because they have upcoming reservations.");
        }

        employee.setActive(false);
        employeeRepository.save(employee);
    }

    @Override
    public EmployeeDTO activateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

        employee.setActive(true);

        Employee savedEmployee = employeeRepository.save(employee);
        return employeeMapper.toDto(savedEmployee);
    }
}