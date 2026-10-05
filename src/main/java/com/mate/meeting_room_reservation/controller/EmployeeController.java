package com.mate.meeting_room_reservation.controller;

import com.mate.meeting_room_reservation.dto.employee.EmployeeDTO;
import com.mate.meeting_room_reservation.dto.employee.SaveEmployeeDTO;
import com.mate.meeting_room_reservation.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public List<EmployeeDTO> listAllEmployees() {
        return employeeService.listAllEmployees();
    }

    @GetMapping("/active")
    public List<EmployeeDTO> listActiveEmployees() {
        return employeeService.listActiveEmployees();
    }

    @GetMapping("/{id}")
    public EmployeeDTO loadEmployee(@PathVariable Long id) {
        return employeeService.loadEmployee(id);
    }

    @PostMapping
    public EmployeeDTO createEmployee(@Valid @RequestBody SaveEmployeeDTO dto) {
        return employeeService.createEmployee(dto);
    }

    @PutMapping("/{id}")
    public EmployeeDTO updateEmployee(@PathVariable Long id, @Valid @RequestBody SaveEmployeeDTO dto) {
        return employeeService.updateEmployee(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
    }

    @PatchMapping("/{id}/activate")
    public EmployeeDTO activateEmployee(@PathVariable Long id) {
        return employeeService.activateEmployee(id);
    }
}
