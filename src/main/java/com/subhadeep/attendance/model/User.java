package com.subhadeep.attendance.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {


    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id //marks id as the primary key
    @Column(nullable = false, unique = true)
    private String email;
    private Long id;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name; // 👈 NEW: for name from registration form

    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // STUDENT, TEACHER, ADMIN

    private boolean approved = false;
    private boolean emailVerified;

    @ManyToMany(mappedBy = "teachers")
    private List<Subject> subjects;

    @ManyToMany
    @JoinTable(
            name = "student_subjects",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private List<Subject> enrolledSubjects = new ArrayList<>();


}
