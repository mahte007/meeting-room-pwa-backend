package com.mate.meeting_room_reservation.repository;

import com.mate.meeting_room_reservation.entity.Employee;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByEmail(String email);
    Optional<Employee> findByEmail(String email);
    List<Employee> findByActiveTrue(Sort sort);
}
