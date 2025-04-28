package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    // Zmieniono z Bicycle na IncompleteBike
    List<ServiceOrder> findByBicycle(IncompleteBike bicycle);
    List<ServiceOrder> findByClient(IncompleteUser client);
    List<ServiceOrder> findByClientAndStatus(IncompleteUser client, ServiceOrder.OrderStatus status);

    // Nowe metody do znajdowania zamówień po pakiecie
    List<ServiceOrder> findByServicePackage(ServicePackage servicePackage);
    List<ServiceOrder> findByServicePackageId(Long servicePackageId);
    List<ServiceOrder> findByServicePackageCode(String servicePackageCode);

    // Pobierz wszystkie aktywne zamówienia (nieanulowane)
    @Query("SELECT o FROM ServiceOrder o WHERE o.status != 'CANCELLED'")
    List<ServiceOrder> findAllActiveOrders();

    // Pobierz wszystkie zamówienia o określonym statusie
    List<ServiceOrder> findByStatus(ServiceOrder.OrderStatus status);

    /**
     * Zlicza liczbę rowerów zaplanowanych na serwis w danym dniu (zamówienia aktywne)
     * @param date data, dla której zliczamy rowery
     * @return liczba rowerów zaplanowanych na serwis
     */
    @Query("SELECT COUNT(o) FROM ServiceOrder o WHERE " +
            "o.pickupDate = :date AND o.status != 'CANCELLED'")
    int countBikesScheduledForDate(@Param("date") LocalDate date);

    /**
     * Zlicza liczbę rowerów zaplanowanych na serwis dla zakresu dat (zamówienia aktywne)
     * @param startDate data początkowa (włącznie)
     * @param endDate data końcowa (włącznie)
     * @return mapa z datami i liczbą rowerów na każdy dzień
     */
    @Query("SELECT o.pickupDate as date, COUNT(o) as bikeCount FROM ServiceOrder o " +
            "WHERE o.pickupDate BETWEEN :startDate AND :endDate " +
            "AND o.status != 'CANCELLED' " +
            "GROUP BY o.pickupDate ORDER BY o.pickupDate")
    List<Object[]> countBikesScheduledForDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Pobiera wszystkie zamówienia na dany dzień (zamówienia aktywne)
     * @param date data
     * @return lista zamówień
     */
    @Query("SELECT o FROM ServiceOrder o WHERE " +
            "o.pickupDate = :date AND o.status != 'CANCELLED'")
    List<ServiceOrder> findOrdersForDate(@Param("date") LocalDate date);

    /**
     * Pobiera daty, na które są zaplanowane zamówienia w określonym zakresie
     * @param startDate data początkowa (włącznie)
     * @param endDate data końcowa (włącznie)
     * @return lista dat
     */
    @Query("SELECT DISTINCT o.pickupDate FROM ServiceOrder o " +
            "WHERE o.pickupDate BETWEEN :startDate AND :endDate " +
            "AND o.status != 'CANCELLED' " +
            "ORDER BY o.pickupDate")
    List<LocalDate> findDatesWithOrders(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sprawdza, czy istnieją jakiekolwiek zamówienia na dany dzień (zamówienia aktywne)
     * @param date data
     * @return true, jeśli istnieją zamówienia
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM ServiceOrder o " +
            "WHERE o.pickupDate = :date AND o.status != 'CANCELLED'")
    boolean existsOrdersForDate(@Param("date") LocalDate date);

    /**
     * Sprawdza, czy na dany dzień liczba zaplanowanych rowerów przekracza podany limit
     * @param date data
     * @param limit limit rowerów
     * @return true, jeśli liczba rowerów przekracza limit
     */
    @Query("SELECT CASE WHEN COUNT(o) > :limit THEN true ELSE false END FROM ServiceOrder o " +
            "WHERE o.pickupDate = :date AND o.status != 'CANCELLED'")
    boolean isDateOverBooked(@Param("date") LocalDate date, @Param("limit") int limit);
}