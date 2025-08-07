package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.BikeService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BikeServiceRepository extends JpaRepository<BikeService, Long> {

    Optional<BikeService> findServiceById(Long id);

    /**
     * Znajdź serwisy które mają współrzędne geograficzne
     */
    @Query("SELECT s FROM BikeService s WHERE s.latitude IS NOT NULL AND s.longitude IS NOT NULL")
    List<BikeService> findServicesWithCoordinates();

    /**
     * Znajdź wszystkie serwisy posortowane według daty utworzenia (najnowsze pierwsze)
     */
    List<BikeService> findAllByOrderByCreatedAtDesc();

    /**
     * Znajdź serwisy posortowane alfabetycznie po nazwie
     */
    List<BikeService> findAllByOrderByNameAsc();

    /**
     * Znajdź serwisy w określonym mieście
     */
    List<BikeService> findByCity(String city);

    List<BikeService> findAllByTransportCost(BigDecimal transportPrice);

    /**
     * Znajdź serwisy po nazwie (zawierającej podany tekst, case-insensitive)
     */
    @Query("SELECT s FROM BikeService s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<BikeService> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Sprawdź czy istnieje serwis o podanej nazwie (case-insensitive)
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM BikeService s WHERE LOWER(s.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);


}