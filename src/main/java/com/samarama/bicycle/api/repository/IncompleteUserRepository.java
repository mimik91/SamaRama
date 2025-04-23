package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.IncompleteUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncompleteUserRepository extends JpaRepository<IncompleteUser, Long> {
    Optional<IncompleteUser> findByEmail(String email);
    Boolean existsByEmail(String email);
}