package com.subhadeep.attendance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_email", referencedColumnName = "email")
    private User student;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    private String grade;
    private String feedback;

    // Getters and setters
}
