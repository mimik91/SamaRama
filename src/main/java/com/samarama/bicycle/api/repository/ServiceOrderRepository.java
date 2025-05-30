package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {

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
}