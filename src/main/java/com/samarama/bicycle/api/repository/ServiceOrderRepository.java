package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long>, JpaSpecificationExecutor<ServiceOrder> {

    // === PODSTAWOWE ZAPYTANIA ===

    /**
     * Znajdź zamówienia serwisowe dla klienta
     */
    List<ServiceOrder> findByClient(IncompleteUser client);

    /**
     * Znajdź zamówienia serwisowe dla roweru
     */
    List<ServiceOrder> findByBicycle(IncompleteBike bicycle);

    /**
     * Znajdź zamówienia serwisowe według pakietu
     */
    List<ServiceOrder> findByServicePackage(ServicePackage servicePackage);

    /**
     * Znajdź zamówienia serwisowe według kodu pakietu
     */
    List<ServiceOrder> findByServicePackageCode(String servicePackageCode);

    /**
     * Znajdź zamówienia serwisowe według statusu
     */
    List<ServiceOrder> findByStatus(TransportOrder.OrderStatus status);

    // === ZAPYTANIA SERWISOWE ===

    /**
     * Znajdź aktywne zamówienia serwisowe
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.status != 'CANCELLED'")
    List<ServiceOrder> findAllActive();

    /**
     * Znajdź zamówienia serwisowe gotowe do rozpoczęcia
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.status IN ('CONFIRMED', 'PICKED_UP') AND s.serviceStartDate IS NULL")
    List<ServiceOrder> findReadyToStart();

    /**
     * Znajdź zamówienia serwisowe w trakcie realizacji
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.status = 'IN_SERVICE'")
    List<ServiceOrder> findInProgress();

    /**
     * Znajdź zamówienia serwisowe zakończone ale nie odebrane
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.serviceCompletionDate IS NOT NULL AND s.status != 'ON_THE_WAY_BACK'")
    List<ServiceOrder> findCompletedButNotPickedUp();

    /**
     * Znajdź zamówienia serwisowe rozpoczęte w określonym przedziale czasowym
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.serviceStartDate BETWEEN :startDate AND :endDate")
    List<ServiceOrder> findByServiceStartDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Znajdź zamówienia serwisowe zakończone w określonym przedziale czasowym
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.serviceCompletionDate BETWEEN :startDate AND :endDate")
    List<ServiceOrder> findByServiceCompletionDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // === STATYSTYKI SERWISU ===

    /**
     * Zlicz zamówienia serwisowe na dzień
     */
    @Query("SELECT COUNT(s) FROM ServiceOrder s WHERE s.pickupDate = :date AND s.status != 'CANCELLED'")
    int countByPickupDate(@Param("date") LocalDate date);

    /**
     * Zlicz zamówienia serwisowe według pakietu
     */
    @Query("SELECT COUNT(s) FROM ServiceOrder s WHERE s.servicePackageCode = :packageCode AND s.status != 'CANCELLED'")
    int countByServicePackageCode(@Param("packageCode") String packageCode);

    /**
     * Średni czas serwisu w minutach
     */
    @Query("SELECT AVG(FUNCTION('EXTRACT', EPOCH FROM (s.serviceCompletionDate - s.serviceStartDate)) / 60) " +
            "FROM ServiceOrder s WHERE s.serviceStartDate IS NOT NULL AND s.serviceCompletionDate IS NOT NULL")
    Double getAverageServiceTimeInMinutes();

    /**
     * Najbliższe zamówienia serwisowe do rozpoczęcia
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.pickupDate >= CURRENT_DATE AND s.status IN ('PENDING', 'CONFIRMED') " +
            "ORDER BY s.pickupDate, s.orderDate LIMIT :limit")
    List<ServiceOrder> findUpcomingServiceOrders(@Param("limit") int limit);

    // === WYSZUKIWANIE ===

    /**
     * Wyszukuje zamówienia serwisowe według informacji o kliencie
     */
    @Query("SELECT s FROM ServiceOrder s WHERE " +
            "LOWER(s.client.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.client.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ServiceOrder> searchByClientInfo(@Param("searchTerm") String searchTerm);

    // === ZARZĄDZANIE SLOTAMI ===

    /**
     * Sprawdź czy dzień serwisowy jest przepełniony
     */
    @Query("SELECT CASE WHEN COUNT(s) >= :maxServicesPerDay THEN true ELSE false END " +
            "FROM ServiceOrder s WHERE s.pickupDate = :date AND s.status != 'CANCELLED'")
    boolean isServiceDateOverBooked(@Param("date") LocalDate date, @Param("maxServicesPerDay") int maxServicesPerDay);

    /**
     * Znajdź wszystkie aktywne zamówienia serwisowe
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.status != 'CANCELLED' ORDER BY s.pickupDate, s.orderDate")
    List<ServiceOrder> findAllActiveOrders();

    /**
     * Zlicz wszystkie aktywne zamówienia serwisowe
     */
    @Query("SELECT COUNT(s) FROM ServiceOrder s WHERE s.status != 'CANCELLED'")
    long countAllActive();

    // === ZLICZANIE W ZAKRESIE DAT ===

    /**
     * Zlicza liczbę zamówień serwisowych w zakresie dat
     */
    @Query("SELECT s.pickupDate, COUNT(s) FROM ServiceOrder s " +
            "WHERE s.pickupDate BETWEEN :startDate AND :endDate " +
            "AND s.status != 'CANCELLED' " +
            "GROUP BY s.pickupDate " +
            "ORDER BY s.pickupDate")
    List<Object[]> countServiceOrdersForDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // === ANALITYKA PAKIETÓW SERWISOWYCH ===

    /**
     * Statystyki pakietów serwisowych (najpopularniejsze)
     */
    @Query("SELECT s.servicePackageCode, COUNT(s) as orderCount " +
            "FROM ServiceOrder s WHERE s.status != 'CANCELLED' " +
            "GROUP BY s.servicePackageCode " +
            "ORDER BY orderCount DESC")
    List<Object[]> getServicePackageStatistics();

    /**
     * Przychody z pakietów serwisowych
     */
    @Query("SELECT s.servicePackageCode, SUM(s.servicePrice) as totalRevenue " +
            "FROM ServiceOrder s WHERE s.status NOT IN ('CANCELLED', 'PENDING') " +
            "GROUP BY s.servicePackageCode " +
            "ORDER BY totalRevenue DESC")
    List<Object[]> getServicePackageRevenue();
}