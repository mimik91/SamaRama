package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.ServiceUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceUserRepository extends JpaRepository<ServiceUser, Long> {

    /**
     * Znajdź użytkownika serwisu po emailu
     */
    Optional<ServiceUser> findByEmail(String email);

    /**
     * Znajdź użytkownika serwisu po ID serwisu rowerowego
     */
    Optional<ServiceUser> findByBikeServiceId(Long bikeServiceId);


    /**
     * Sprawdź czy istnieje użytkownik serwisu z danym emailem
     */
    boolean existsByEmail(String email);

    /**
     * Sprawdź czy istnieje użytkownik serwisu dla danego bike_service_id
     */
    boolean existsByBikeServiceId(Long bikeServiceId);

    /**
     * Znajdź użytkowników serwisu z join do BikeService po mieście
     */
    @Query("SELECT su FROM ServiceUser su " +
            "JOIN BikeService bs ON su.bikeServiceId = bs.id " +
            "WHERE bs.city = :city")
    List<ServiceUser> findByServiceCity(@Param("city") String city);

    /**
     * Znajdź użytkowników serwisu oferujących transport
     */
    @Query("SELECT su FROM ServiceUser su " +
            "JOIN BikeService bs ON su.bikeServiceId = bs.id " +
            "WHERE bs.transportAvailable = true")
    List<ServiceUser> findByTransportAvailable();
}