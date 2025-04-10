package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    // Znajdź wszystkie zamówienia serwisowe dla danego klienta
    List<ServiceOrder> findByClient(User client);

    // Znajdź wszystkie zamówienia serwisowe dla danego roweru
    List<ServiceOrder> findByBicycle(Bicycle bicycle);

    // Znajdź wszystkie zamówienia serwisowe dla danego serwisu
    List<ServiceOrder> findByService(BikeService service);

    // Znajdź wszystkie zamówienia serwisowe dla danego klienta o określonym statusie
    List<ServiceOrder> findByClientAndStatus(User client, ServiceOrder.OrderStatus status);

    // Znajdź wszystkie zamówienia serwisowe dla danego serwisu o określonym statusie
    List<ServiceOrder> findByServiceAndStatus(BikeService service, ServiceOrder.OrderStatus status);

    // Znajdź wszystkie zamówienia serwisowe dla danego serwisu na określony dzień odbioru
    List<ServiceOrder> findByServiceAndPickupDate(BikeService service, LocalDate pickupDate);
}