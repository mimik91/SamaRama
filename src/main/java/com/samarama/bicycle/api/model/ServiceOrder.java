package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ServiceOrder - rozszerzenie TransportOrder o usługi serwisowe
 * ServiceOrder = Transport + Serwis w naszym własnym serwisie
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Entity
@Table(name = "service_orders")
@PrimaryKeyJoinColumn(name = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ServiceOrder extends TransportOrder {

    @Column(name = "service_package_code")
    private String servicePackageCode;

    @Column(name = "service_price")
    private BigDecimal servicePrice; // TYLKO koszt serwisu

    @Column(name = "service_notes", length = 500)
    private String serviceNotes;

    // === DATY SERWISU ===

    @Column(name = "service_start_date")
    private LocalDateTime serviceStartDate;

    @Column(name = "service_completion_date")
    private LocalDateTime serviceCompletionDate;

    // === NADPISANE METODY Z KLASY BAZOWEJ ===

    @Override
    public String getOrderType() {
        return "SERVICE";
    }

    @Override
    public BigDecimal getTotalPrice() {
        BigDecimal transport = getTransportPrice() != null ? getTransportPrice() : BigDecimal.ZERO;
        BigDecimal service = servicePrice != null ? servicePrice : BigDecimal.ZERO;
        return transport.add(service);
    }

    @Override
    public boolean isServiceOrder() {
        return true;
    }

    // === METODY BIZNESOWE SERWISU ===

    /**
     * Sprawdza czy serwis może zostać rozpoczęty
     */
    public boolean canStartService() {
        return getStatus() == OrderStatus.PICKED_UP || getStatus() == OrderStatus.CONFIRMED;
    }

    /**
     * Sprawdza czy serwis jest w trakcie realizacji
     */
    public boolean isServiceInProgress() {
        return getStatus() == OrderStatus.IN_SERVICE;
    }

    /**
     * Sprawdza czy serwis został zakończony
     */
    public boolean isServiceCompleted() {
        return serviceCompletionDate != null;
    }

    /**
     * Zwraca czas trwania serwisu w minutach
     */
    public Long getServiceDurationInMinutes() {
        if (serviceStartDate != null && serviceCompletionDate != null) {
            return java.time.Duration.between(serviceStartDate, serviceCompletionDate).toMinutes();
        }
        return null;
    }

    /**
     * Zwraca czas trwania serwisu jako czytelny string
     */
    public String getServiceDurationDisplay() {
        Long minutes = getServiceDurationInMinutes();
        if (minutes == null) return null;

        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;

        if (hours > 0) {
            return hours + "h " + remainingMinutes + "min";
        } else {
            return minutes + "min";
        }
    }

    /**
     * Rozpoczyna serwis
     */
    public void startService() {
        if (canStartService()) {
            setStatus(OrderStatus.IN_SERVICE);
            this.serviceStartDate = LocalDateTime.now();
        }
    }

    /**
     * Kończy serwis
     */
    public void completeService() {
        if (isServiceInProgress()) {
            this.serviceCompletionDate = LocalDateTime.now();
            setStatus(OrderStatus.ON_THE_WAY_BACK);
        }
    }

    /**
     * Sprawdza czy można anulować (nadpisane - uwzględnia stan serwisu)
     */
    @Override
    public boolean canBeCancelled() {
        // Nie można anulować jeśli serwis już się rozpoczął
        return super.canBeCancelled() && !isServiceInProgress() && serviceStartDate == null;
    }

    /**
     * Zwraca szczegółowy opis statusu z uwzględnieniem serwisu
     */
    public String getDetailedStatusDescription() {
        return switch (getStatus()) {
            case PENDING -> "Zamówienie oczekuje na potwierdzenie";
            case CONFIRMED -> "Zamówienie potwierdzone, oczekuje na odbiór";
            case PICKED_UP -> "Rower odebrany, oczekuje na rozpoczęcie serwisu";
            case IN_SERVICE -> "Rower w serwisie" + (serviceStartDate != null ?
                    " od " + serviceStartDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm")) : "");
            case ON_THE_WAY_BACK -> "Serwis zakończony, rower w drodze powrotnej";
            case FINISHED -> "Rower dostarczony z powrotem do klienta";
            case CANCELLED -> "Zamówienie anulowane";
        };
    }

    // === KONSTRUKTORY ===

    public ServiceOrder(IncompleteBike bicycle, IndividualUser client,
                          BigDecimal transportPrice) {
        // Wywołanie konstruktora bazowego - transport zawsze do serwisu własnego (ID=1)
        super(bicycle, client,
                createOwnService(), // TODO: pobierz z repo lub ustaw stałą
                transportPrice);
    }

    public ServiceOrder(IncompleteBike bicycle, IndividualUser client,
                        String servicePackageCode, BigDecimal servicePrice,
                        BigDecimal transportPrice) {
        super(bicycle, client,
                createOwnService(), // TODO: pobierz z repo lub ustaw stałą
                transportPrice);

        this.servicePackageCode = servicePackageCode;
        this.servicePrice = servicePrice != null ? servicePrice : BigDecimal.ZERO;
    }

    // TODO: To powinno być pobierane z repository lub dependency injection
    private static BikeService createOwnService() {
        BikeService ownService = new BikeService();
        ownService.setId(1L); // ID serwisu własnego
        ownService.setName("Serwis Własny");
        return ownService;
    }

    @PreUpdate
    @Override
    protected void onUpdate() {
        super.onUpdate();

        // Automatyczne ustawienie daty zakończenia serwisu przy zmianie statusu
        if (getStatus() == OrderStatus.ON_THE_WAY_BACK && serviceCompletionDate == null && serviceStartDate != null) {
            serviceCompletionDate = LocalDateTime.now();
        }
    }
}