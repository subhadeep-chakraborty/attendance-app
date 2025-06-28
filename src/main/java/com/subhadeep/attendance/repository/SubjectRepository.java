package com.subhadeep.attendance.repository;

import com.subhadeep.attendance.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository  extends JpaRepository<Subject, Long> {

    List<Subject> findByTeachersEmail(String email);

    List<Subject> findAll();
}

