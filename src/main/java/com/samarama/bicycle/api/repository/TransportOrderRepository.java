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
     * Znajdź zamówienia transportowe według statusu
     */
    List<TransportOrder> findByStatus(TransportOrder.OrderStatus status);

    /**
     * Znajdź zamówienia transportowe dla konkretnego serwisu docelowego
     */
    List<TransportOrder> findByTargetService(BikeService targetService);

    /**
     * Znajdź TYLKO czyste zamówienia transportowe (nie serwisowe)
     * Wykluczamy ServiceOrder używając TYPE
     */
    @Query("SELECT t FROM TransportOrder t WHERE TYPE(t) = TransportOrder")
    List<TransportOrder> findPureTransportOrders();

    /**
     * Znajdź wszystkie zamówienia (transport + serwis)
     */
    @Query("SELECT t FROM TransportOrder t ORDER BY t.orderDate DESC")
    List<TransportOrder> findAllOrders();

    // === ZAPYTANIA WEDŁUG DAT ===

    /**
     * Znajdź zamówienia transportowe na konkretny dzień
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.pickupDate = :date AND t.status != 'CANCELLED'")
    List<TransportOrder> findByPickupDate(@Param("date") LocalDate date);

    /**
     * Znajdź zamówienia transportowe w zakresie dat
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.pickupDate BETWEEN :startDate AND :endDate AND t.status != 'CANCELLED'")
    List<TransportOrder> findByPickupDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // === ZAPYTANIA DLA KLIENTA ===

    /**
     * Znajdź zamówienia transportowe klienta według statusu
     */
    List<TransportOrder> findByClientAndStatus(IncompleteUser client, TransportOrder.OrderStatus status);

    /**
     * Znajdź aktywne zamówienia transportowe klienta (nieanulowane)
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.client = :client AND t.status != 'CANCELLED'")
    List<TransportOrder> findActiveByClient(@Param("client") IncompleteUser client);

    // === ZAPYTANIA DLA SERWISU ===

    /**
     * Znajdź wszystkie zamówienia dla konkretnego serwisu według statusu
     */
    List<TransportOrder> findByTargetServiceAndStatus(BikeService targetService, TransportOrder.OrderStatus status);

    /**
     * Znajdź zamówienia oczekujące na dostawę do serwisu
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.targetService = :service " +
            "AND t.status IN ('PICKED_UP')")
    List<TransportOrder> findPendingDeliveriesToService(@Param("service") BikeService service);

    /**
     * Znajdź zamówienia w serwisie
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.targetService = :service " +
            "AND t.status = 'IN_SERVICE'")
    List<TransportOrder> findInServiceAtService(@Param("service") BikeService service);

    /**
     * Znajdź zamówienia gotowe do zwrotu
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.targetService = :service " +
            "AND t.status = 'ON_THE_WAY_BACK'")
    List<TransportOrder> findReadyForReturnFromService(@Param("service") BikeService service);

    // === STATYSTYKI I ZLICZANIE ===

    /**
     * Zlicz zamówienia transportowe na konkretny dzień (wszystkie typy)
     */
    @Query("SELECT COUNT(t) FROM TransportOrder t WHERE t.pickupDate = :date AND t.status != 'CANCELLED'")
    int countByPickupDate(@Param("date") LocalDate date);

    /**
     * Zlicz TYLKO czyste zamówienia transportowe na dzień
     */
    @Query("SELECT COUNT(t) FROM TransportOrder t WHERE TYPE(t) = TransportOrder AND t.pickupDate = :date AND t.status != 'CANCELLED'")
    int countPureTransportByPickupDate(@Param("date") LocalDate date);

    /**
     * Zlicz zamówienia serwisowe na dzień
     */
    @Query("SELECT COUNT(t) FROM TransportOrder t WHERE TYPE(t) = ServiceOrder AND t.pickupDate = :date AND t.status != 'CANCELLED'")
    int countServiceOrdersByPickupDate(@Param("date") LocalDate date);

    /**
     * Zlicz zamówienia transportowe według statusu
     */
    @Query("SELECT COUNT(t) FROM TransportOrder t WHERE t.status = :status")
    int countByStatus(@Param("status") TransportOrder.OrderStatus status);

    /**
     * Znajdź zamówienia które powinny być dostarczone dzisiaj
     */
    @Query("SELECT t FROM TransportOrder t WHERE " +
            "t.pickupDate = CURRENT_DATE " +
            "AND t.status IN ('CONFIRMED', 'PICKED_UP')")
    List<TransportOrder> findDueForPickupToday();

    // === WYSZUKIWANIE WEDŁUG INFORMACJI O KLIENCIE ===

    /**
     * Wyszukuje zamówienia według informacji o kliencie (email lub telefon)
     */
    @Query("SELECT t FROM TransportOrder t WHERE " +
            "LOWER(t.client.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.client.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<TransportOrder> searchByClientInfo(@Param("searchTerm") String searchTerm);

    // === SPRAWDZANIE DOSTĘPNOŚCI SLOTÓW ===

    /**
     * Sprawdź czy dzień jest przepełniony
     */
    @Query("SELECT CASE WHEN COUNT(t) >= :maxOrdersPerDay THEN true ELSE false END " +
            "FROM TransportOrder t WHERE t.pickupDate = :date AND t.status != 'CANCELLED'")
    boolean isDateOverBooked(@Param("date") LocalDate date, @Param("maxOrdersPerDay") int maxOrdersPerDay);

    /**
     * Zlicz rowery na dzień (jeden rower = jedno zamówienie w nowej strukturze)
     */
    @Query("SELECT COUNT(t) FROM TransportOrder t WHERE t.pickupDate = :date AND t.status != 'CANCELLED'")
    int countBikesScheduledForDate(@Param("date") LocalDate date);

    /**
     * Znajdź wszystkie aktywne zamówienia
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.status != 'CANCELLED' ORDER BY t.pickupDate, t.orderDate")
    List<TransportOrder> findAllActiveOrders();

    /**
     * Zlicz wszystkie aktywne zamówienia
     */
    @Query("SELECT COUNT(t) FROM TransportOrder t WHERE t.status != 'CANCELLED'")
    long countAllActive();

    // === ZLICZANIE W ZAKRESIE DAT ===

    /**
     * Zlicza liczbę zamówień w zakresie dat (grupowane po dniach)
     */
    @Query("SELECT t.pickupDate, COUNT(t) FROM TransportOrder t " +
            "WHERE t.pickupDate BETWEEN :startDate AND :endDate " +
            "AND t.status != 'CANCELLED' " +
            "GROUP BY t.pickupDate " +
            "ORDER BY t.pickupDate")
    List<Object[]> countOrdersForDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // === ZAPYTANIA SPECYFICZNE DLA TRANSPORTU ===

    /**
     * Znajdź zamówienia transportowe do zewnętrznych serwisów
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.targetService.id != 1 AND TYPE(t) = TransportOrder")
    List<TransportOrder> findExternalTransportOrders();

    /**
     * Znajdź zamówienia z określonym oknem czasowym odbioru
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.pickupTimeFrom IS NOT NULL AND t.pickupTimeTo IS NOT NULL")
    List<TransportOrder> findOrdersWithPickupTimeWindow();

    /**
     * Znajdź zamówienia bez okna czasowego odbioru
     */
    @Query("SELECT t FROM TransportOrder t WHERE t.pickupTimeFrom IS NULL OR t.pickupTimeTo IS NULL")
    List<TransportOrder> findOrdersWithoutPickupTimeWindow();
}