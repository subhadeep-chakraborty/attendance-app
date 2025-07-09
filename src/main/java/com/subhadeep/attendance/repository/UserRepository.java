package com.subhadeep.attendance.repository;


import com.subhadeep.attendance.model.Role;
import com.subhadeep.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    long countByApprovedTrue();
    long countByApprovedFalse();
    List<User> findByApprovedFalse();
    List<User> findByEmailIn(List<String> emails);
    List<User> findByRole(Role role);



}
