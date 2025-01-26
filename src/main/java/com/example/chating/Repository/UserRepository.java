package com.example.chating.Repository;

import com.example.chating.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);
    @Query("SELECT u.name FROM User u WHERE u.id = ?1")
    String getUserNameById(Long id);
}
