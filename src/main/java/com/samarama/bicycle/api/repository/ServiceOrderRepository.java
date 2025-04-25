package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}