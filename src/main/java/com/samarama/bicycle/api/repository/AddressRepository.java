package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Znajdź wszystkie aktywne adresy użytkownika
     */
    @Query("SELECT a FROM Address a WHERE a.userId = :userId AND a.active = true ORDER BY a.createdAt DESC")
    List<Address> findByUserIdAndActiveTrue(@Param("userId") Long userId);

    /**
     * Znajdź adres po ID i userId (dla bezpieczeństwa)
     */
    @Query("SELECT a FROM Address a WHERE a.id = :id AND a.userId = :userId AND a.active = true")
    Optional<Address> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);


}