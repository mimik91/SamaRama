package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.IndividualUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IndividualUserRepository extends JpaRepository<IndividualUser, Long> {
    Optional<IndividualUser> findByEmail(String email);
    Boolean existsByEmail(String email);
}