package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Entity
@Table(name = "service_orders")
@PrimaryKeyJoinColumn(name = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ServiceOrder extends Order {

    // === INFORMACJE O SERWISIE ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_package_id")
    private ServicePackage servicePackage;

    @Column(name = "service_package_code")
    private String servicePackageCode;

    @Column(name = "service_notes", length = 500)
    private String serviceNotes;

    // === IMPLEMENTACJA METOD ABSTRAKCYJNYCH ===

    @Override
    public String getOrderType() {
        return "SERVICE";
    }

    // === METODY POMOCNICZE ===

    /**
     * Sprawdza czy zamówienie ma przypisany pakiet serwisowy
     */
    public boolean hasServicePackage() {
        return servicePackage != null ||
                (servicePackageCode != null && !servicePackageCode.trim().isEmpty());
    }

    /**
     * Zwraca nazwę pakietu serwisowego
     */
    public String getServicePackageName() {
        return servicePackage != null ? servicePackage.getName() : servicePackageCode;
    }

    /**
     * Sprawdza czy serwis może zostać rozpoczęty
     */
    public boolean canStartService() {
        return getStatus() == OrderStatus.PICKED_UP || getStatus() == OrderStatus.CONFIRMED;
    }

    /**
     * Sprawdza czy serwis jest w trakcie realizacji
     */
    public boolean isInService() {
        return getStatus() == OrderStatus.IN_SERVICE;
    }

    // === KONSTRUKTORY ===

    public ServiceOrder(IncompleteBike bicycle, IncompleteUser client, ServicePackage servicePackage) {
        super();
        setBicycle(bicycle);
        setClient(client);
        this.servicePackage = servicePackage;
        this.servicePackageCode = servicePackage != null ? servicePackage.getCode() : null;
        setPrice(servicePackage != null ? servicePackage.getPrice() : null);
    }
}