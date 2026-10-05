package com.mate.meeting_room_reservation.service;

import com.mate.meeting_room_reservation.dto.employee.EmployeeDTO;
import com.mate.meeting_room_reservation.dto.employee.SaveEmployeeDTO;

import java.util.List;

public interface EmployeeService {

    List<EmployeeDTO> listAllEmployees();

    List<EmployeeDTO> listActiveEmployees();

    EmployeeDTO loadEmployee(Long id);

    EmployeeDTO createEmployee(SaveEmployeeDTO dto);

    EmployeeDTO updateEmployee(Long id, SaveEmployeeDTO dto);

    void deleteEmployee(Long id);

    EmployeeDTO activateEmployee(Long id);
}
