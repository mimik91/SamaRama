package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.BicyclePhoto;
import com.samarama.bicycle.api.model.IncompleteBike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BicyclePhotoRepository extends JpaRepository<BicyclePhoto, Long> {

    // Znajdź zdjęcie po ID roweru (niezależnie od typu)
    @Query("SELECT p FROM BicyclePhoto p WHERE p.bike.id = :bikeId")
    Optional<BicyclePhoto> findByBikeId(@Param("bikeId") Long bikeId);

    // Znajdź zdjęcie dla konkretnego roweru
    Optional<BicyclePhoto> findByBike(IncompleteBike bike);

    // Te metody możemy zaimplementować, aby zachować zgodność wsteczną:

    // Znajdź zdjęcie po ID roweru - dla Bicycle
    default Optional<BicyclePhoto> findByBicycleId(Long bicycleId) {
        return findByBikeId(bicycleId);
    }

    // Znajdź zdjęcie po ID roweru - dla IncompleteBike
    default Optional<BicyclePhoto> findByIncompleteBikeId(Long incompleteBikeId) {
        return findByBikeId(incompleteBikeId);
    }

    // Sprawdź czy istnieje zdjęcie dla roweru o podanym ID
    boolean existsByBikeId(Long bikeId);
}