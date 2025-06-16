package com.subhadeep.attendance.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id //marks id as the primary key
    @GeneratedValue
    private Long id;

    private String email;

    private String password;

    private String role; // STUDENT, TEACHER, ADMIN

    private boolean approved = false;
    private boolean emailVerified;
}
