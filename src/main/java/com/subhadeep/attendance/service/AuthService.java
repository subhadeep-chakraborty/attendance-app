package com.subhadeep.attendance.service;

import com.subhadeep.attendance.model.User;
import com.subhadeep.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class AuthService {

    @Autowired
    private UserRepository userRepo;

    public User authenticate(String email, String password) {
        return userRepo.findByEmail(email)
                .filter(u -> u.getPassword().equals(password)) // plain text for now
                .orElse(null);
    }
}
