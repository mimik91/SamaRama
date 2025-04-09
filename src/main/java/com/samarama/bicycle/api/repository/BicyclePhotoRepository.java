package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.BicyclePhoto;
import com.samarama.bicycle.api.model.IncompleteBike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BicyclePhotoRepository extends JpaRepository<BicyclePhoto, Long> {
    Optional<BicyclePhoto> findByBicycleId(Long bicycleId);
    Optional<BicyclePhoto> findByIncompleteBikeId(Long incompleteBikeId);
    Optional<BicyclePhoto> findByBicycle(Bicycle bicycle);
    Optional<BicyclePhoto> findByIncompleteBike(IncompleteBike incompleteBike);
    void deleteByBicycleId(Long bicycleId);
    void deleteByIncompleteBikeId(Long incompleteBikeId);
    boolean existsByBicycleId(Long bicycleId);
    boolean existsByIncompleteBikeId(Long incompleteBikeId);
}