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
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long>, JpaSpecificationExecutor<ServiceOrder> {

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
    List<ServiceOrder> findByStatus(Order.OrderStatus status);

    /**
     * Znajdź aktywne zamówienia serwisowe
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.status != 'CANCELLED'")
    List<ServiceOrder> findAllActive();

    /**
     * Znajdź zamówienia serwisowe gotowe do rozpoczęcia
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.status IN ('CONFIRMED', 'PICKED_UP')")
    List<ServiceOrder> findReadyToStart();

    /**
     * Znajdź zamówienia serwisowe w trakcie realizacji
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.status = 'IN_SERVICE'")
    List<ServiceOrder> findInProgress();

    // === METODY DO ZARZĄDZANIA SLOTAMI SERWISOWYMI ===

    /**
     * Zlicz liczbę rowerów zaplanowanych do serwisu na określony dzień
     * Wyklucza zamówienia anulowane
     *
     * @param date data odbioru
     * @return liczba rowerów zaplanowanych na dany dzień
     */
    @Query("SELECT COUNT(s) FROM ServiceOrder s WHERE s.pickupDate = :date AND s.status != 'CANCELLED'")
    int countBikesScheduledForDate(@Param("date") LocalDate date);

    /**
     * Zlicz liczbę rowerów zaplanowanych do serwisu na określony dzień według statusu
     *
     * @param date data odbioru
     * @param status status zamówienia
     * @return liczba rowerów zaplanowanych na dany dzień w określonym statusie
     */
    @Query("SELECT COUNT(s) FROM ServiceOrder s WHERE s.pickupDate = :date AND s.status = :status")
    int countBikesScheduledForDateAndStatus(@Param("date") LocalDate date, @Param("status") Order.OrderStatus status);

    /**
     * Znajdź zamówienia serwisowe na określony dzień
     *
     * @param date data odbioru
     * @return lista zamówień na dany dzień
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.pickupDate = :date AND s.status != 'CANCELLED' ORDER BY s.orderDate")
    List<ServiceOrder> findByPickupDate(@Param("date") LocalDate date);

    /**
     * Znajdź zamówienia serwisowe w zakresie dat
     *
     * @param startDate data początkowa
     * @param endDate data końcowa
     * @return lista zamówień w zakresie dat
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.pickupDate BETWEEN :startDate AND :endDate AND s.status != 'CANCELLED' ORDER BY s.pickupDate, s.orderDate")
    List<ServiceOrder> findByPickupDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Sprawdź czy dzień jest przepełniony (więcej zamówień niż dozwolone)
     *
     * @param date data odbioru
     * @param maxBikesPerDay maksymalna liczba rowerów na dzień
     * @return true jeśli dzień jest przepełniony
     */
    @Query("SELECT CASE WHEN COUNT(s) >= :maxBikesPerDay THEN true ELSE false END " +
            "FROM ServiceOrder s WHERE s.pickupDate = :date AND s.status != 'CANCELLED'")
    boolean isDateOverBooked(@Param("date") LocalDate date, @Param("maxBikesPerDay") int maxBikesPerDay);

    /**
     * Znajdź zamówienia serwisowe według pakietu serwisowego na określony dzień
     *
     * @param date data odbioru
     * @param servicePackageCode kod pakietu serwisowego
     * @return lista zamówień
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.pickupDate = :date AND s.servicePackageCode = :servicePackageCode AND s.status != 'CANCELLED'")
    List<ServiceOrder> findByPickupDateAndServicePackageCode(@Param("date") LocalDate date, @Param("servicePackageCode") String servicePackageCode);

    /**
     * Zlicz zamówienia według pakietu serwisowego (statystyki)
     *
     * @param servicePackageCode kod pakietu serwisowego
     * @return liczba zamówień dla danego pakietu
     */
    @Query("SELECT COUNT(s) FROM ServiceOrder s WHERE s.servicePackageCode = :servicePackageCode AND s.status != 'CANCELLED'")
    int countByServicePackageCode(@Param("servicePackageCode") String servicePackageCode);

    /**
     * Znajdź najbliższe zamówienia serwisowe (przydatne do planowania)
     *
     * @param limit maksymalna liczba wyników
     * @return lista najbliższych zamówień
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.pickupDate >= CURRENT_DATE AND s.status IN ('PENDING', 'CONFIRMED') ORDER BY s.pickupDate, s.orderDate LIMIT :limit")
    List<ServiceOrder> findUpcomingServiceOrders(@Param("limit") int limit);

    /**
     * Znajdź wszystkie aktywne zamówienia serwisowe (nieanulowane)
     * Sortowane według daty odbioru i daty zamówienia
     *
     * @return lista wszystkich aktywnych zamówień serwisowych
     */
    @Query("SELECT s FROM ServiceOrder s WHERE s.status != 'CANCELLED' ORDER BY s.pickupDate, s.orderDate")
    List<ServiceOrder> findAllActiveOrders();

    /**
     * Zlicz wszystkie aktywne zamówienia serwisowe (dla statystyk)
     *
     * @return liczba aktywnych zamówień serwisowych
     */
    @Query("SELECT COUNT(s) FROM ServiceOrder s WHERE s.status != 'CANCELLED'")
    long countAllActive();

    // === WYSZUKIWANIE WEDŁUG INFORMACJI O KLIENCIE ===

    /**
     * Wyszukuje zamówienia serwisowe według informacji o kliencie (email lub telefon)
     *
     * @param searchTerm termin wyszukiwania (email lub telefon)
     * @return lista zamówień pasujących do wyszukiwania
     */
    @Query("SELECT s FROM ServiceOrder s WHERE " +
            "LOWER(s.client.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.client.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ServiceOrder> searchByClientInfo(@Param("searchTerm") String searchTerm);

    // === ZLICZANIE W ZAKRESIE DAT ===

    /**
     * Zlicza liczbę rowerów zaplanowanych w zakresie dat
     * Zwraca pary [data, liczba_rowerów] dla każdej daty w zakresie która ma zamówienia
     *
     * @param startDate data początkowa
     * @param endDate data końcowa
     * @return lista par [LocalDate, Long] reprezentujących datę i liczbę rowerów
     */
    @Query("SELECT s.pickupDate, COUNT(s) FROM ServiceOrder s " +
            "WHERE s.pickupDate BETWEEN :startDate AND :endDate " +
            "AND s.status != 'CANCELLED' " +
            "GROUP BY s.pickupDate " +
            "ORDER BY s.pickupDate")
    List<Object[]> countBikesScheduledForDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}