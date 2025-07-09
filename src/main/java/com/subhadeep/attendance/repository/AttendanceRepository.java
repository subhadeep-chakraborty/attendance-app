package com.subhadeep.attendance.repository;


import com.subhadeep.attendance.model.Attendance;
import com.subhadeep.attendance.model.Subject;
import com.subhadeep.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByStudentAndSubjectAndDate(User student, Subject subject, LocalDate date);
    List<Attendance> findByStudentAndSubject(User student, Subject subject);
    List<Attendance> findByStudent(User student);
}

