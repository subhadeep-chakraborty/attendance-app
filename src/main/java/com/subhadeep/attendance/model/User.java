package com.subhadeep.attendance.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
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


    @Id
    @Column(nullable = false, unique = true)
    private String email;

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

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "student_subjects",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private List<Subject> enrolledSubjects = new ArrayList<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Attendance> attendanceRecords = new ArrayList<>();



}
