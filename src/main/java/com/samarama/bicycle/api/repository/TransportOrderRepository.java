package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransportOrderRepository extends JpaRepository<TransportOrder, Long>, JpaSpecificationExecutor<TransportOrder> {

    // === PODSTAWOWE ZAPYTANIA ===

    /**
     * Znajdź wszystkie zamówienia transportowe dla klienta
     */
    List<TransportOrder> findByClient(IncompleteUser client);

    /**
     * Znajdź zamówienia transportowe dla roweru
     */
    List<TransportOrder> findByBicycle(IncompleteBike bicycle);

    /**
     * Znajdź zamówienia transportowe według statusu transportu
     */
    List<TransportOrder> findByTransportStatus(TransportOrder.TransportStatus transportStatus);

    /**
     * Znajdź zamówienia transportowe według typu
     */
    List<TransportOrder> findByTransportType(TransportOrder.TransportType transportType);

    /**
     * Znajdź zamówienia transportowe dla konkretnego serwisu docelowego
     */
    List<TransportOrder> findByTargetService(BikeService targetService);

    // === ZAPYTANIA WEDŁUG DAT ===

    /**
     * Znajdź zamówienia transportowe na konkretny dzień
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.pickupDate = :date")
    List<TransportOrder> findByPickupDate(@Param("date") LocalDate date);

    /**
     * Znajdź zamówienia transportowe w zakresie dat
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.pickupDate BETWEEN :startDate AND :endDate")
    List<TransportOrder> findByPickupDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // === ZAPYTANIA DLA KLIENTA ===

    /**
     * Znajdź zamówienia transportowe klienta według statusu
     */
    List<TransportOrder> findByClientAndTransportStatus(
            IncompleteUser client,
            TransportOrder.TransportStatus status
    );

    /**
     * Znajdź aktywne zamówienia transportowe klienta (nieanulowane)
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.client = :client AND t.status != 'CANCELLED'")
    List<TransportOrder> findActiveByClient(@Param("client") IncompleteUser client);

    // === ZAPYTANIA DLA SERWISU ===

    /**
     * Znajdź wszystkie zamówienia dla konkretnego serwisu według statusu
     */
    List<TransportOrder> findByTargetServiceAndTransportStatus(
            BikeService targetService,
            TransportOrder.TransportStatus status
    );

    /**
     * Znajdź zamówienia oczekujące na dostawę do serwisu
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.targetService = :service " +
            "AND t.transportStatus IN ('PICKED_UP', 'IN_TRANSIT')")
    List<TransportOrder> findPendingDeliveriesToService(@Param("service") BikeService service);

    // === STATYSTYKI I ZLICZANIE ===

    /**
     * Zlicz zamówienia transportowe na konkretny dzień
     */
    @Query("SELECT COUNT(t) FROM TransportOrder t WHERE t.pickupDate = :date " +
            "AND t.status != 'CANCELLED'")
    int countByPickupDate(@Param("date") LocalDate date);

    /**
     * Zlicz zamówienia transportowe według statusu transportu
     */
    @Query("SELECT COUNT(t) FROM TransportOrder t WHERE t.transportStatus = :transportStatus")
    int countByTransportStatus(@Param("transportStatus") TransportOrder.TransportStatus transportStatus);

    /**
     * Znajdź zamówienia transportowe które powinny być dostarczone dzisiaj
     * Bezpieczna wersja używająca porównania LocalDateTime
     */
    @Query("SELECT t FROM TransportOrder t WHERE " +
            "t.estimatedDeliveryTime >= :startOfDay " +
            "AND t.estimatedDeliveryTime < :endOfDay " +
            "AND t.transportStatus IN ('PICKED_UP', 'IN_TRANSIT')")
    List<TransportOrder> findDueForDeliveryToday(
            @Param("startOfDay") java.time.LocalDateTime startOfDay,
            @Param("endOfDay") java.time.LocalDateTime endOfDay);

    /**
     * Znajdź zamówienia transportowe które powinny być dostarczone dzisiaj
     * Wrapper metoda bez parametrów (helper w serwisie)
     */
    default List<TransportOrder> findDueForDeliveryToday() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        return findDueForDeliveryToday(startOfDay, endOfDay);
    }

    // === ZAPYTANIA COMBINED (ServiceOrder + TransportOrder) ===

    /**
     * Znajdź wszystkie zamówienia (serwisowe i transportowe) dla klienta
     * UWAGA: To zapytanie nie będzie działać z nową hierarchią - użyj OrderRepository
     */
    @Deprecated
    @Query("SELECT o FROM Order o WHERE o.client = :client")
    List<Order> findAllOrdersByClient(@Param("client") IncompleteUser client);

    /**
     * Sprawdź czy istnieją zamówienia transportowe na dany dzień przekraczające limit
     */
    @Query("SELECT CASE WHEN COUNT(t) > :limit THEN true ELSE false END " +
            "FROM TransportOrder t WHERE t.pickupDate = :date AND t.status != 'CANCELLED'")
    boolean isTransportDateOverBooked(
            @Param("date") LocalDate date,
            @Param("limit") int limit
    );

    // === DODATKOWE METODY DLA ZARZĄDZANIA SLOTAMI ===

    /**
     * Zlicz liczbę rowerów transportowych zaplanowanych na określony dzień
     */
    @Query("SELECT COUNT(t) FROM TransportOrder t WHERE t.pickupDate = :date AND t.status != 'CANCELLED'")
    int countBikesScheduledForDate(@Param("date") LocalDate date);

    /**
     * Znajdź wszystkie aktywne zamówienia transportowe
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.status != 'CANCELLED' ORDER BY t.pickupDate, t.orderDate")
    List<TransportOrder> findAllActiveOrders();

    /**
     * Zlicz wszystkie aktywne zamówienia transportowe
     */
    @Query("SELECT COUNT(t) FROM TransportOrder t WHERE t.status != 'CANCELLED'")
    long countAllActive();

    // === WYSZUKIWANIE WEDŁUG INFORMACJI O KLIENCIE ===

    /**
     * Wyszukuje zamówienia transportowe według informacji o kliencie (email lub telefon)
     *
     * @param searchTerm termin wyszukiwania (email lub telefon)
     * @return lista zamówień pasujących do wyszukiwania
     */
    @Query("SELECT t FROM TransportOrder t WHERE " +
            "LOWER(t.client.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.client.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<TransportOrder> searchByClientInfo(@Param("searchTerm") String searchTerm);

    // === ZLICZANIE W ZAKRESIE DAT ===

    /**
     * Zlicza liczbę rowerów transportowych zaplanowanych w zakresie dat
     * Zwraca pary [data, liczba_rowerów] dla każdej daty w zakresie która ma zamówienia
     *
     * @param startDate data początkowa
     * @param endDate data końcowa
     * @return lista par [LocalDate, Long] reprezentujących datę i liczbę rowerów
     */
    @Query("SELECT t.pickupDate, COUNT(t) FROM TransportOrder t " +
            "WHERE t.pickupDate BETWEEN :startDate AND :endDate " +
            "AND t.status != 'CANCELLED' " +
            "GROUP BY t.pickupDate " +
            "ORDER BY t.pickupDate")
    List<Object[]> countBikesScheduledForDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}