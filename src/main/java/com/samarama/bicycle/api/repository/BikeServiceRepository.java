package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.BikeService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BikeServiceRepository extends JpaRepository<BikeService, Long> {
    Optional<BikeService> findByEmail(String email);
    Boolean existsByEmail(String email);
}