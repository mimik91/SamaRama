package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.BicycleEnumeration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BicycleEnumerationRepository extends JpaRepository<BicycleEnumeration, Long> {
    Optional<BicycleEnumeration> findByType(String type);
    boolean existsByType(String type);
}