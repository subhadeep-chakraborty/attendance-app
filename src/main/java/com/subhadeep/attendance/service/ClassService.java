package com.subhadeep.attendance.service;


import com.subhadeep.attendance.model.Subject;
import com.subhadeep.attendance.model.User;
import com.subhadeep.attendance.repository.SubjectRepository;
import com.subhadeep.attendance.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassService {
    private final SubjectRepository subjectRepo;
    private final UserRepository userRepo;

    public ClassService(SubjectRepository subjectRepo, UserRepository userRepo) {
        this.subjectRepo = subjectRepo;
        this.userRepo = userRepo;
    }

    public List<Subject> getAllSubjects() {
        return subjectRepo.findAll();
    }

    public List<User> getAllTeachers() {
        return userRepo.findAll().stream()
                .filter(user -> user.getRole().name().equals("TEACHER"))
                .toList();
    }

    public void addSubject(String name, List<String> teacherEmails) {
        List<User> teachers = userRepo.findByEmailIn(
                teacherEmails.stream().map(String::toLowerCase).toList()
        );

        Subject subject = new Subject();
        subject.setName(name);
        subject.setTeachers(teachers);
        subject.setActive(true);
        subjectRepo.save(subject);
    }

    public void deactivateSubject(Long id) {
        subjectRepo.findById(id).ifPresent(sub -> {
            sub.setActive(false);
            subjectRepo.save(sub);
        });
    }

    public void activateSubject(Long id) {
        subjectRepo.findById(id).ifPresent(subject -> {
            subject.setActive(true);
            subjectRepo.save(subject);
        });
    }

    public List<Subject> getSubjectsByTeacherEmail(String email) {
        return subjectRepo.findByTeachersEmail(email);
    }


}
