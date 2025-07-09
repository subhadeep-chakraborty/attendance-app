package com.subhadeep.attendance.repository;

import com.subhadeep.attendance.model.Subject;
import com.subhadeep.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository  extends JpaRepository<Subject, Long> {

    List<Subject> findByTeachersEmail(String email);

    List<Subject> findAll();

    @Query("SELECT s FROM Subject s JOIN s.teachers t WHERE t = :teacher")
    List<Subject> findSubjectsByTeacher(@Param("teacher") User teacher);
    Optional<Subject> findByName(String name);
}

