package com.subhadeep.attendance.repository;

import com.subhadeep.attendance.model.User;
import com.subhadeep.attendance.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    VerificationToken findByToken(String token);
    Optional<VerificationToken> findByUser(User user);
}
