package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.ServiceSlotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceSlotConfigRepository extends JpaRepository<ServiceSlotConfig, Long> {

    /**
     * Znajduje konfigurację slotów dla określonej daty
     * @param date data, dla której szukamy konfiguracji
     * @return konfigurację, jeśli istnieje
     */
    @Query("SELECT c FROM ServiceSlotConfig c WHERE " +
            "c.startDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date) " +
            "ORDER BY c.startDate DESC LIMIT 1")
    Optional<ServiceSlotConfig> findConfigForDate(@Param("date") LocalDate date);

    /**
     * Znajduje wszystkie konfiguracje, które są aktywne w danym momencie
     * (aktualna data zawiera się między startDate a endDate)
     * @return lista aktywnych konfiguracji
     */
    @Query("SELECT c FROM ServiceSlotConfig c WHERE " +
            "c.startDate <= CURRENT_DATE AND (c.endDate IS NULL OR c.endDate >= CURRENT_DATE)")
    List<ServiceSlotConfig> findCurrentlyActiveConfigs();

    /**
     * Znajduje wszystkie przyszłe konfiguracje (startDate jest w przyszłości)
     * @return lista przyszłych konfiguracji
     */
    @Query("SELECT c FROM ServiceSlotConfig c WHERE c.startDate > CURRENT_DATE ORDER BY c.startDate")
    List<ServiceSlotConfig> findFutureConfigs();

    /**
     * Znajduje wszystkie konfiguracje, które nakładają się na podany zakres dat
     * @param startDate data początkowa
     * @param endDate data końcowa (może być null)
     * @return lista nakładających się konfiguracji
     */
    @Query("SELECT c FROM ServiceSlotConfig c WHERE " +
            "c.startDate <= :endDate AND (c.endDate IS NULL OR c.endDate >= :startDate)")
    List<ServiceSlotConfig> findOverlappingConfigsWithEndDate(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM ServiceSlotConfig c WHERE " +
            "c.endDate IS NULL OR c.endDate >= :startDate")
    List<ServiceSlotConfig> findOverlappingConfigsWithoutEndDate(
            @Param("startDate") LocalDate startDate);



    /**
     * Znajduje konfiguracje z datą startu po podanej dacie
     * @param date data graniczna
     * @return lista konfiguracji z datą startu po podanej dacie
     */
    @Query("SELECT c FROM ServiceSlotConfig c WHERE c.startDate > :date ORDER BY c.startDate")
    List<ServiceSlotConfig> findConfigsWithStartDateAfter(@Param("date") LocalDate date);
}