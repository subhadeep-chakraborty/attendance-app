package com.subhadeep.attendance.service;

import com.subhadeep.attendance.model.User;
import com.subhadeep.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AuthService {

    @Autowired
    private UserRepository userRepo;

    public User authenticate(String email, String password) {
        return userRepo.findByEmail(email)
                .filter(u -> u.getPassword().equals(password)) // plain text for now
                .orElse(null);
    }

    public User getUserByEmail(String email) {
        return userRepo.findByEmail(email).orElse(null);
    }


    public boolean register(User user) {
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            return false; // prevent duplicate registration
        }
        user.setApproved(false);
        user.setEmailVerified(false);
        if (!user.isEmailVerified()) {
            return false; // or throw error
        }

        userRepo.save(user);
        return true;
    }
    public long countApprovedUsers() {
        return userRepo.countByApprovedTrue();
    }

    public long countPendingApprovals() {
        return userRepo.countByApprovedFalse();
    }

    public List<User> getPendingUsers() {
        return userRepo.findByApprovedFalse();
    }

    public void setUserApproval(String email, boolean approved) {
        userRepo.findByEmail(email).ifPresent(user -> {
            user.setApproved(approved);
            userRepo.save(user);
        });
    }

    public void deactivateUser(String email) {
        userRepo.findByEmail(email).ifPresent(user -> {
            user.setApproved(false); // set approved to false
            userRepo.save(user);     // save changes
        });
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }





}
