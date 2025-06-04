package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.model.TransportOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO dla odpowiedzi z zamówień serwisowych
 * Uproszczone DTO zawierające tylko najważniejsze informacje
 */
public record ServiceOrderResponseDto(
        Long id,
        String orderType,
        Long bicycleId,
        LocalDate pickupDate,
        String pickupAddress,
        String targetServiceName,
        String servicePackageName,
        BigDecimal totalPrice,
        String status,
        String statusDisplayName,
        LocalDateTime orderDate,
        String bicycleDescription
) {
    /**
     * Tworzy DTO z ServiceOrder entity
     */
    public static ServiceOrderResponseDto fromServiceOrder(ServiceOrder entity) {
        // Buduj opis roweru z dostępnych danych
        String bicycleDescription = buildBicycleDescriptionFromTransport(entity);

        // Pobierz nazwę serwisu docelowego
        String targetServiceName = entity.getTargetService() != null ?
                entity.getTargetService().getName() : "Serwis Domyślny";

        // Pobierz nazwę pakietu serwisowego
        String servicePackageName = getServicePackageName(entity);

        // Pobierz cenę całkowitą
        BigDecimal totalPrice = getTotalPrice(entity);

        // Pobierz status display name
        String statusDisplayName = getStatusDisplayName(entity.getStatus());

        return new ServiceOrderResponseDto(
                entity.getId(),
                "SERVICE",
                entity.getBicycle() != null ? entity.getBicycle().getId() : null,
                entity.getPickupDate(),
                entity.getPickupAddress(),
                targetServiceName,
                servicePackageName,
                totalPrice,
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                statusDisplayName,
                entity.getOrderDate(),
                bicycleDescription
        );
    }

    public static ServiceOrderResponseDto fromTransportOrder(TransportOrder entity) {
        // Buduj opis roweru
        String bicycleDescription = buildBicycleDescriptionFromTransport(entity);

        // Dla transportu nie ma pakietu serwisowego
        String servicePackageName = "Transport";

        // Cena całkowita to cena transportu
        BigDecimal totalPrice = entity.getTransportPrice() != null ?
                entity.getTransportPrice() : BigDecimal.ZERO;

        // Nazwa serwisu docelowego
        String targetServiceName = entity.getTargetService() != null ?
                entity.getTargetService().getName() : "Nieznany serwis";

        // Status display name
        String statusDisplayName = getStatusDisplayName(entity.getStatus());

        return new ServiceOrderResponseDto(
                entity.getId(),
                "TRANSPORT", // orderType
                entity.getBicycle() != null ? entity.getBicycle().getId() : null,
                entity.getPickupDate(),
                entity.getPickupAddress(),
                targetServiceName,
                servicePackageName, // Dla transportu = "Transport"
                totalPrice,
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                statusDisplayName,
                entity.getOrderDate(),
                bicycleDescription
        );
    }

    /**
     * Buduje opis roweru z dostępnych danych
     */
    private static String buildBicycleDescriptionFromTransport(TransportOrder entity) {
        StringBuilder description = new StringBuilder();

        // Sprawdź czy mamy dane roweru w encji
        if (entity.getBicycle() != null) {
            if (entity.getBicycle().getBrand() != null) {
                description.append(entity.getBicycle().getBrand());
            }
            if (entity.getBicycle().getModel() != null) {
                if (description.length() > 0) {
                    description.append(" ");
                }
                description.append(entity.getBicycle().getModel());
            }
        }

        // Jeśli nie ma danych w bicycle, sprawdź denormalizowane pola
        if (description.length() == 0) {
            if (entity.getBicycle().getBrand() != null) {
                description.append(entity.getBicycle().getBrand());
            }
            if (entity.getBicycle().getModel() != null) {
                if (description.length() > 0) {
                    description.append(" ");
                }
                description.append(entity.getBicycle().getModel());
            }
        }

        // Jeśli nadal nie ma danych, użyj ID roweru
        if (description.length() == 0) {
            Long bicycleId = entity.getBicycle() != null ? entity.getBicycle().getId() : null;
            if (bicycleId != null) {
                description.append("Rower ID: ").append(bicycleId);
            } else {
                description.append("Nieznany rower");
            }
        }

        return description.toString();
    }

    /**
     * Pobiera nazwę pakietu serwisowego
     */
    private static String getServicePackageName(ServiceOrder entity) {
        // Sprawdź różne źródła nazwy pakietu
        if (entity.getServicePackageName() != null) {
            return entity.getServicePackageName();
        }

        if (entity.getServicePackage() != null && entity.getServicePackage().getName() != null) {
            return entity.getServicePackage().getName();
        }

        if (entity.getServicePackageCode() != null) {
            return entity.getServicePackageCode();
        }

        return "Nieznany pakiet";
    }

    /**
     * Oblicza cenę całkowitą (transport + serwis)
     */
    private static BigDecimal getTotalPrice(ServiceOrder entity) {
        BigDecimal total = BigDecimal.ZERO;

        // Dodaj cenę serwisu
        if (entity.getServicePrice() != null) {
            total = total.add(entity.getServicePrice());
        }

        // Dodaj cenę transportu
        if (entity.getTransportPrice() != null) {
            total = total.add(entity.getTransportPrice());
        }

        return total;
    }

    /**
     * Mapuje status na czytelną nazwę
     */
    private static String getStatusDisplayName(com.samarama.bicycle.api.model.TransportOrder.OrderStatus status) {
        if (status == null) return "Nieznany";

        return switch (status) {
            case PENDING -> "Oczekujące";
            case CONFIRMED -> "Potwierdzone";
            case PICKED_UP -> "Odebrane";
            case IN_SERVICE -> "W serwisie";
            case ON_THE_WAY_BACK -> "W drodze powrotnej";
            case CANCELLED -> "Anulowane";
            default -> status.toString();
        };
    }
}