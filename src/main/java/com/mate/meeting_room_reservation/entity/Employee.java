package com.mate.meeting_room_reservation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String department;
    private String role;
    private Boolean active;

    @OneToMany(mappedBy = "employee")
    private List<Reservation> reservations = new ArrayList<>();

}
