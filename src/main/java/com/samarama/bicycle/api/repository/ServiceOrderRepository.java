package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.IncompleteBike;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.model.ServicePackage;
import com.samarama.bicycle.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    // Zmieniono z Bicycle na IncompleteBike
    List<ServiceOrder> findByBicycle(IncompleteBike bicycle);
    List<ServiceOrder> findByClient(User client);
    List<ServiceOrder> findByService(BikeService service);
    List<ServiceOrder> findByClientAndStatus(User client, ServiceOrder.OrderStatus status);
    List<ServiceOrder> findByServiceAndStatus(BikeService service, ServiceOrder.OrderStatus status);
    List<ServiceOrder> findByServiceAndPickupDate(BikeService service, LocalDate pickupDate);

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