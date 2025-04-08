package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.BicyclePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BicyclePhotoRepository extends JpaRepository<BicyclePhoto, Long> {
    Optional<BicyclePhoto> findByBicycleId(Long bicycleId);
    Optional<BicyclePhoto> findByBicycle(Bicycle bicycle);
    void deleteByBicycleId(Long bicycleId);
    boolean existsByBicycleId(Long bicycleId);
}