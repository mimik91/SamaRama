package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BicycleRepository extends JpaRepository<Bicycle, Long> {
    List<Bicycle> findByOwner(User owner);
    Optional<Bicycle> findByFrameNumber(String frameNumber);
    Boolean existsByFrameNumber(String frameNumber);
}
