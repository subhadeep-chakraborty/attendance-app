package com.subhadeep.attendance.repository;

import com.subhadeep.attendance.model.Grade;
import com.subhadeep.attendance.model.Subject;
import com.subhadeep.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    boolean existsByStudentAndSubject(User student, Subject subject);
    Optional<Grade> findByStudentAndSubject(User student, Subject subject);
    List<Grade> findByStudent_Email(String email);
}
